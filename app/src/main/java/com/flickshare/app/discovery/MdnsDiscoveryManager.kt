package com.flickshare.app.discovery

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL
import java.util.Collections

data class ReceiverEndpoint(
    val serviceName: String,
    val hostAddress: String,
    val port: Int,
    val isRemembered: Boolean = true
) {
    val uploadUrl: String
        get() = "http://$hostAddress:$port/upload"

    val healthUrl: String
        get() = "http://$hostAddress:$port/health"
}

sealed interface ReceiverState {
    data object Scanning : ReceiverState
    data class Connected(val endpoint: ReceiverEndpoint) : ReceiverState
    data class Error(val message: String) : ReceiverState
}

/**
 * Silent, zero-prompt LAN discovery & persistent receiver manager.
 *
 * Avoids Android 16/17's NsdManager system device-picker modal (NsdPickerActivity)
 * by combining:
 * 1. Persistent SharedPreferences storage of the connected desktop receiver (instant 0ms lock).
 * 2. Silent parallel HTTP `/info` subnet probing on the local Wi-Fi `/24` network when
 *    initial discovery or a manual rescan is needed.
 */
class MdnsDiscoveryManager(context: Context) {

    companion object {
        private const val TAG = "MdnsDiscoveryManager"
        const val SERVICE_TYPE = "_flickshare._tcp."
        const val DEFAULT_PORT = 8765

        private const val PREFS_NAME = "flickshare_receiver_prefs"
        private const val KEY_SERVICE_NAME = "saved_service_name"
        private const val KEY_HOST_ADDRESS = "saved_host_address"
        private const val KEY_PORT = "saved_port"
    }

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var scanJob: Job? = null

    private val _receiverState = MutableStateFlow<ReceiverState>(
        loadSavedEndpoint()?.let { ReceiverState.Connected(it) } ?: ReceiverState.Scanning
    )
    val receiverState: StateFlow<ReceiverState> = _receiverState.asStateFlow()

    private val _discoveredEndpoints = MutableStateFlow<List<ReceiverEndpoint>>(
        loadSavedEndpoint()?.let { listOf(it) } ?: emptyList()
    )
    val discoveredEndpoints: StateFlow<List<ReceiverEndpoint>> = _discoveredEndpoints.asStateFlow()

    /**
     * Starts silent background discovery ONLY if no receiver is remembered yet,
     * or silently verifies the remembered receiver without popping any OS dialogs.
     */
    @Synchronized
    fun startDiscovery(forceFullScan: Boolean = false) {
        val saved = loadSavedEndpoint()
        if (saved != null && _receiverState.value !is ReceiverState.Connected) {
            _receiverState.value = ReceiverState.Connected(saved)
            addOrUpdateDiscovered(saved)
        }

        if (scanJob?.isActive == true && !forceFullScan) {
            return
        }

        scanJob?.cancel()
        scanJob = scope.launch {
            // 1. If we already have a remembered receiver and aren't forcing a full scan,
            //    verify it silently in the background. If it responds, we're done!
            if (saved != null && !forceFullScan) {
                val refreshed = probeHost(saved.hostAddress, saved.port, timeoutMs = 600)
                if (refreshed != null) {
                    if (refreshed.serviceName != saved.serviceName) {
                        selectEndpoint(refreshed)
                    }
                    return@launch
                }
                // Remembered IP didn't respond (e.g., DHCP IP changed); silently scan subnet
                // to update its IP while keeping the current state non-intrusive.
            }

            if (_receiverState.value !is ReceiverState.Connected) {
                _receiverState.value = ReceiverState.Scanning
            }

            val prefix = getLocalIpv4Prefix()
            if (prefix == null) {
                if (_receiverState.value !is ReceiverState.Connected) {
                    _receiverState.value = ReceiverState.Error("Connect to Wi-Fi or set Desktop IP manually")
                }
                return@launch
            }

            val semaphore = Semaphore(48)
            val foundAny = coroutineScope {
                (1..254).map { hostByte ->
                    async {
                        val candidateIp = "$prefix$hostByte"
                        semaphore.withPermit {
                            val endpoint = probeHost(candidateIp, DEFAULT_PORT, timeoutMs = 380)
                            if (endpoint != null) {
                                addOrUpdateDiscovered(endpoint)
                                val current = _receiverState.value
                                if (current !is ReceiverState.Connected) {
                                    Log.i(TAG, "Auto-locked & saved receiver: ${endpoint.serviceName} at ${endpoint.uploadUrl}")
                                    selectEndpoint(endpoint)
                                } else if (
                                    current.endpoint.serviceName.equals(endpoint.serviceName, ignoreCase = true) &&
                                    current.endpoint.hostAddress != endpoint.hostAddress
                                ) {
                                    Log.i(TAG, "Updated saved receiver IP: ${endpoint.serviceName} -> ${endpoint.uploadUrl}")
                                    selectEndpoint(endpoint)
                                }
                            }
                            endpoint
                        }
                    }
                }.awaitAll().filterNotNull()
            }

            if (foundAny.isEmpty() && _receiverState.value !is ReceiverState.Connected) {
                _receiverState.value = ReceiverState.Error("No desktop receiver found on $prefix*:$DEFAULT_PORT")
            }
        }
    }

    @Synchronized
    fun stopDiscovery() {
        scanJob?.cancel()
        scanJob = null
    }

    /**
     * Rescans the network for available receivers while keeping the current saved device
     * connected unless a new one is chosen.
     */
    fun restartDiscovery() {
        if (_receiverState.value !is ReceiverState.Connected) {
            _receiverState.value = ReceiverState.Scanning
        }
        startDiscovery(forceFullScan = true)
    }

    /**
     * Clears the remembered device and scans fresh for any receiver on LAN.
     */
    fun forgetSavedDeviceAndRescan() {
        prefs.edit().clear().apply()
        _discoveredEndpoints.value = emptyList()
        stopDiscovery()
        _receiverState.value = ReceiverState.Scanning
        startDiscovery(forceFullScan = true)
    }

    /**
     * Locks onto a chosen endpoint and saves it persistently so the app connects
     * automatically every time without asking again.
     */
    fun selectEndpoint(endpoint: ReceiverEndpoint) {
        val remembered = endpoint.copy(isRemembered = true)
        saveEndpoint(remembered)
        addOrUpdateDiscovered(remembered)
        _receiverState.value = ReceiverState.Connected(remembered)
    }

    /**
     * Allows manual IPv4/hostname and port configuration and persists it as the default device.
     */
    fun setManualEndpoint(
        host: String,
        port: Int = DEFAULT_PORT,
        serviceName: String = "Desktop ($host)"
    ) {
        val sanitizedHost = host.trim()
            .removePrefix("http://")
            .removePrefix("https://")
            .substringBefore("/")
            .substringBefore(":")
        if (sanitizedHost.isEmpty() || port !in 1..65535) {
            _receiverState.value = ReceiverState.Error("Invalid IP address or port")
            return
        }
        val cleanName = serviceName.trim().ifEmpty { "Desktop ($sanitizedHost)" }
        val endpoint = ReceiverEndpoint(
            serviceName = cleanName,
            hostAddress = sanitizedHost,
            port = port,
            isRemembered = true
        )
        selectEndpoint(endpoint)

        // Silently fetch real hostname in background if reachable
        scope.launch {
            val verified = probeHost(sanitizedHost, port, timeoutMs = 800)
            if (verified != null) {
                selectEndpoint(verified)
            }
        }
    }

    private fun probeHost(ip: String, port: Int, timeoutMs: Int): ReceiverEndpoint? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL("http://$ip:$port/info")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                useCaches = false
            }
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                if (json.optString("status") == "ok") {
                    val hostname = json.optString("hostname").takeIf { it.isNotBlank() } ?: "Desktop ($ip)"
                    val reportedPort = json.optInt("port", port).takeIf { it in 1..65535 } ?: port
                    ReceiverEndpoint(
                        serviceName = "FlickShare-$hostname",
                        hostAddress = ip,
                        port = reportedPort,
                        isRemembered = true
                    )
                } else {
                    null
                }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun getLocalIpv4Prefix(): String? {
        return try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress && addr.isSiteLocalAddress) {
                        val ip = addr.hostAddress ?: continue
                        val lastDot = ip.lastIndexOf('.')
                        if (lastDot > 0) {
                            return ip.substring(0, lastDot + 1)
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to determine local IPv4 subnet prefix", e)
            null
        }
    }

    private fun loadSavedEndpoint(): ReceiverEndpoint? {
        val host = prefs.getString(KEY_HOST_ADDRESS, null)?.trim().orEmpty()
        val port = prefs.getInt(KEY_PORT, DEFAULT_PORT)
        if (host.isEmpty() || port !in 1..65535) return null
        val name = prefs.getString(KEY_SERVICE_NAME, null)
            ?.takeIf { it.isNotBlank() }
            ?: "Desktop ($host)"
        return ReceiverEndpoint(
            serviceName = name,
            hostAddress = host,
            port = port,
            isRemembered = true
        )
    }

    private fun saveEndpoint(endpoint: ReceiverEndpoint) {
        prefs.edit()
            .putString(KEY_SERVICE_NAME, endpoint.serviceName)
            .putString(KEY_HOST_ADDRESS, endpoint.hostAddress)
            .putInt(KEY_PORT, endpoint.port)
            .apply()
    }

    private fun addOrUpdateDiscovered(endpoint: ReceiverEndpoint) {
        val currentList = _discoveredEndpoints.value.toMutableList()
        val existingIdx = currentList.indexOfFirst {
            it.hostAddress.equals(endpoint.hostAddress, ignoreCase = true) ||
                it.serviceName.equals(endpoint.serviceName, ignoreCase = true)
        }
        if (existingIdx >= 0) {
            currentList[existingIdx] = endpoint
        } else {
            currentList.add(0, endpoint)
        }
        _discoveredEndpoints.value = currentList
    }
}
