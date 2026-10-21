package com.flickshare.app

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.flickshare.app.discovery.MdnsDiscoveryManager
import com.flickshare.app.discovery.ReceiverState
import com.flickshare.app.gesture.ThrowAim
import com.flickshare.app.gesture.ThrowCompass
import com.flickshare.app.gesture.ThrowSensorController
import com.flickshare.app.gesture.WristFlick
import com.flickshare.app.network.FileTransferClient
import com.flickshare.app.network.StagedFile
import com.flickshare.app.network.UploadProgress
import com.flickshare.app.ui.FlickShareScreen
import com.flickshare.app.ui.FlickShareTheme
import com.flickshare.app.ui.TransferUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var mdnsDiscoveryManager: MdnsDiscoveryManager
    private lateinit var throwSensorController: ThrowSensorController
    private lateinit var throwCompass: ThrowCompass
    private val fileTransferClient = FileTransferClient()

    private val stagedFiles = mutableStateListOf<StagedFile>()
    private var transferUiState by mutableStateOf<TransferUiState>(TransferUiState.Idle)
    private var activeUploadJob: Job? = null
    private var aimCalibrationVersion by mutableIntStateOf(0)

    private val wristFlickChannel = Channel<WristFlick>(capacity = Channel.CONFLATED)

    private val localNetworkPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(
                    this,
                    "Allow \"Nearby devices\" so FlickShare can reach your PC on Wi-Fi",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (!hasLocalNetworkPermission()) {
            localNetworkPermissionLauncher.launch(LOCAL_NETWORK_PERMISSION)
        }

        mdnsDiscoveryManager = MdnsDiscoveryManager(applicationContext)
        mdnsDiscoveryManager.startDiscovery()
        throwSensorController = ThrowSensorController(applicationContext) { flick ->
            wristFlickChannel.trySend(flick)
        }
        throwCompass = ThrowCompass(applicationContext)

        // Extract any files or text shared via Android Share Sheet on initial launch
        handleIncomingShareIntent(intent)

        setContent {
            FlickShareTheme {
                val receiverState by mdnsDiscoveryManager.receiverState.collectAsState()
                val discoveredEndpoints by mdnsDiscoveryManager.discoveredEndpoints.collectAsState()
                val connectedEndpoint = (receiverState as? ReceiverState.Connected)?.endpoint
                val isAimCalibrated = remember(connectedEndpoint, aimCalibrationVersion) {
                    connectedEndpoint != null && throwCompass.isCalibrated(connectedEndpoint)
                }

                FlickShareScreen(
                    receiverState = receiverState,
                    discoveredEndpoints = discoveredEndpoints,
                    stagedFiles = stagedFiles,
                    transferState = transferUiState,
                    isSensorAvailable = throwSensorController.isSensorAvailable,
                    isBumpAvailable = throwSensorController.isBumpAvailable,
                    isCompassAvailable = throwCompass.isAvailable,
                    isAimCalibrated = isAimCalibrated,
                    onCalibrateAim = { calibrateAim() },
                    onClearAim = { clearAim() },
                    wristFlickEvents = wristFlickChannel.receiveAsFlow(),
                    onSensorArmChanged = { arming ->
                        throwSensorController.setArmed(arming)
                    },
                    onTriggerTouchHaptics = {
                        throwSensorController.triggerThrowHaptics()
                    },
                    onFilesPicked = { uris ->
                        stageUris(uris, clearExisting = false)
                    },
                    onClearFiles = {
                        stagedFiles.clear()
                        transferUiState = TransferUiState.Idle
                    },
                    onStartTransfer = { launchModeLabel, gestureAim ->
                        startFileUpload(launchModeLabel, gestureAim)
                    },
                    onResetTransferState = {
                        transferUiState = TransferUiState.Idle
                    },
                    onRescanReceiver = {
                        mdnsDiscoveryManager.restartDiscovery()
                    },
                    onSelectEndpoint = { endpoint ->
                        mdnsDiscoveryManager.selectEndpoint(endpoint)
                    },
                    onSetManualEndpoint = { host, port ->
                        mdnsDiscoveryManager.setManualEndpoint(host, port)
                    },
                    onForgetDeviceAndRescan = {
                        mdnsDiscoveryManager.forgetSavedDeviceAndRescan()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingShareIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        throwSensorController.startListening()
        throwCompass.startListening()
    }

    override fun onPause() {
        throwSensorController.stopListening()
        throwCompass.stopListening()
        super.onPause()
    }

    private fun calibrateAim() {
        val endpoint = (mdnsDiscoveryManager.receiverState.value as? ReceiverState.Connected)?.endpoint ?: return
        val saved = throwCompass.calibrate(endpoint)
        if (saved) aimCalibrationVersion++
        Toast.makeText(
            this,
            if (saved) "Aim saved: throws now land from the side you stand on" else "Compass not ready, move the phone a bit and try again",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun clearAim() {
        val endpoint = (mdnsDiscoveryManager.receiverState.value as? ReceiverState.Connected)?.endpoint ?: return
        throwCompass.clearCalibration(endpoint)
        aimCalibrationVersion++
    }

    override fun onDestroy() {
        mdnsDiscoveryManager.stopDiscovery()
        super.onDestroy()
    }

    private fun handleIncomingShareIntent(incomingIntent: Intent?) {
        if (incomingIntent == null) return

        when (incomingIntent.action) {
            Intent.ACTION_SEND -> {
                val streamUri = getParcelableExtraCompat(incomingIntent, Intent.EXTRA_STREAM, Uri::class.java)
                    ?: incomingIntent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri

                if (streamUri != null) {
                    stageUris(listOf(streamUri), clearExisting = true)
                } else {
                    val sharedText = incomingIntent.getStringExtra(Intent.EXTRA_TEXT)
                    if (!sharedText.isNullOrBlank()) {
                        stagedFiles.clear()
                        stagedFiles.add(FileTransferClient.createVirtualTextFile(sharedText))
                        transferUiState = TransferUiState.Idle
                    }
                }
            }

            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = mutableListOf<Uri>()
                val extraUris = getParcelableArrayListExtraCompat(
                    incomingIntent,
                    Intent.EXTRA_STREAM,
                    Uri::class.java
                )
                if (!extraUris.isNullOrEmpty()) {
                    uris.addAll(extraUris)
                } else {
                    val clip = incomingIntent.clipData
                    if (clip != null) {
                        for (i in 0 until clip.itemCount) {
                            clip.getItemAt(i)?.uri?.let { uris.add(it) }
                        }
                    }
                }
                if (uris.isNotEmpty()) {
                    stageUris(uris, clearExisting = true)
                }
            }
        }
    }

    private fun stageUris(uris: List<Uri>, clearExisting: Boolean) {
        if (clearExisting) {
            stagedFiles.clear()
        }
        val existingUris = stagedFiles.map { it.uri }.toSet()
        for (uri in uris) {
            if (uri in existingUris) continue
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Ephemeral share sheet URIs may not support persistable permissions; ignore
            }
            val resolved = FileTransferClient.resolveStagedFile(contentResolver, uri)
            if (resolved != null) {
                stagedFiles.add(resolved)
            }
        }
        transferUiState = TransferUiState.Idle
    }

    private fun startFileUpload(launchModeLabel: String, gestureAim: ThrowAim?) {
        if (activeUploadJob?.isActive == true) return
        val currentReceiver = mdnsDiscoveryManager.receiverState.value
        if (currentReceiver !is ReceiverState.Connected) {
            transferUiState = TransferUiState.Failed("Desktop receiver is not connected.")
            return
        }
        val filesToUpload = stagedFiles.toList()
        if (filesToUpload.isEmpty()) return
        if (!hasLocalNetworkPermission()) {
            transferUiState = TransferUiState.Failed(
                "Local network access is off. Allow \"Nearby devices\" for FlickShare, then throw again."
            )
            localNetworkPermissionLauncher.launch(LOCAL_NETWORK_PERMISSION)
            return
        }

        val endpoint = currentReceiver.endpoint
        // A calibrated compass knows where you stand relative to the PC; otherwise use the gesture's own angle.
        val aim = throwCompass.aimAt(endpoint) ?: gestureAim
        val label = if (aim != null) "$launchModeLabel · ${aim.describe()}" else launchModeLabel
        transferUiState = TransferUiState.Uploading(
            UploadProgress(
                bytesSent = 0L,
                totalBytes = filesToUpload.sumOf { it.sizeBytes.coerceAtLeast(0L) },
                fraction = 0f,
                speedMbps = 0f,
                currentFileName = filesToUpload.first().name
            )
        )

        activeUploadJob = lifecycleScope.launch {
            val result = fileTransferClient.uploadFiles(
                endpoint = endpoint,
                files = filesToUpload,
                contentResolver = contentResolver,
                aim = aim,
                onProgress = { progress ->
                    lifecycleScope.launch {
                        if (transferUiState is TransferUiState.Uploading) {
                            transferUiState = TransferUiState.Uploading(progress)
                        }
                    }
                }
            )

            transferUiState = if (result.success) {
                TransferUiState.CaughtOnDesktop(result, label)
            } else {
                TransferUiState.Failed(result.errorMessage ?: "Upload failed")
            }
        }
    }

    private fun hasLocalNetworkPermission(): Boolean =
        Build.VERSION.SDK_INT < LOCAL_NETWORK_PERMISSION_SDK ||
            checkSelfPermission(LOCAL_NETWORK_PERMISSION) == PackageManager.PERMISSION_GRANTED

    private companion object {
        const val LOCAL_NETWORK_PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"
        const val LOCAL_NETWORK_PERMISSION_SDK = 37
    }

    private fun <T : android.os.Parcelable> getParcelableExtraCompat(
        intent: Intent,
        name: String,
        clazz: Class<T>
    ): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(name, clazz)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(name)
        }
    }

    private fun <T : android.os.Parcelable> getParcelableArrayListExtraCompat(
        intent: Intent,
        name: String,
        clazz: Class<T>
    ): ArrayList<T>? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(name, clazz)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra(name)
        }
    }
}
