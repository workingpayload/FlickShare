package com.flickshare.app.gesture

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.flickshare.app.discovery.ReceiverEndpoint
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Tracks which way the phone points (compass heading, degrees clockwise from north) and remembers,
 * per desktop receiver, the heading recorded by "Point at PC" calibration. With that, a throw from any
 * spot in the room gets a [ThrowAim] relative to the PC.
 */
class ThrowCompass(context: Context) : SensorEventListener {

    private val sensorManager =
        context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val rotationSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val rotation = FloatArray(9)
    private var isRegistered = false

    val isAvailable: Boolean = rotationSensor != null

    @Volatile
    var heading: Float? = null
        private set

    fun startListening() {
        if (isRegistered || sensorManager == null || rotationSensor == null) return
        isRegistered = sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI)
    }

    fun stopListening() {
        if (!isRegistered) return
        sensorManager?.unregisterListener(this)
        isRegistered = false
        heading = null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        SensorManager.getRotationMatrixFromVector(rotation, event.values)
        // World axes: x = east, y = north. Point with the top edge (device +y) plus the back (device -z)
        // so the heading stays stable whether the phone is held flat or upright.
        val east = rotation[1] - rotation[2]
        val north = rotation[4] - rotation[5]
        if (hypot(east, north) < 0.2f) return // pointing at the floor or ceiling
        heading = Math.toDegrees(atan2(east, north).toDouble()).toFloat()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    fun isCalibrated(endpoint: ReceiverEndpoint): Boolean = prefs.contains(key(endpoint))

    /** Saves the current heading as "straight at this PC". Returns false if no heading yet. */
    fun calibrate(endpoint: ReceiverEndpoint): Boolean {
        val now = heading ?: return false
        prefs.edit().putFloat(key(endpoint), now).apply()
        return true
    }

    fun clearCalibration(endpoint: ReceiverEndpoint) {
        prefs.edit().remove(key(endpoint)).apply()
    }

    /** The compass aim for [endpoint], or null when it isn't calibrated or no heading is available. */
    fun aimAt(endpoint: ReceiverEndpoint): ThrowAim? {
        val calibrated = if (isCalibrated(endpoint)) prefs.getFloat(key(endpoint), 0f) else return null
        val now = heading ?: return null
        return ThrowAim.fromCompass(now, calibrated)
    }

    // Keyed by the PC's LAN address: service names differ between mDNS, probe and manual entry.
    private fun key(endpoint: ReceiverEndpoint): String = "heading:" + endpoint.hostAddress.lowercase()

    private companion object {
        const val PREFS = "flickshare_aim"
    }
}
