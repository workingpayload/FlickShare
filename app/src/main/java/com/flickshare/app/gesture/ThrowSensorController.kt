package com.flickshare.app.gesture

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlin.math.sqrt

/**
 * "Grab and Throw" physical wrist-flick detector.
 *
 * Listens to [Sensor.TYPE_LINEAR_ACCELERATION] (with automatic high-pass filtered fallback to
 * [Sensor.TYPE_ACCELEROMETER]) at [SensorManager.SENSOR_DELAY_GAME].
 *
 * Samples are ignored unless armed via [setArmed]: either while the user's finger holds the staged
 * file card, or hands-free (stricter rules, see [FlickDetector]) when the user enabled that option.
 *
 * "Bump to send" ([BumpDetector]) listens to raw [Sensor.TYPE_ACCELEROMETER] at 200 Hz, registered
 * only while bump is armed. Whichever gesture fires first disarms both, so one motion sends once.
 */
/**
 * A detected throw gesture: its strength and, when the motion had a clear direction, where it aims.
 * [isBump] is true for a double knock against the PC instead of a wrist flick.
 */
data class WristFlick(val magnitude: Float, val aim: ThrowAim?, val isBump: Boolean = false)

/** Which throw gestures should currently be listened for. */
data class SensorArming(
    val flick: Boolean = false,
    val handsFree: Boolean = false,
    val bump: Boolean = false,
)

class ThrowSensorController(
    context: Context,
    private val onWristFlickThrow: (WristFlick) -> Unit
) : SensorEventListener {

    companion object {
        private const val TAG = "ThrowSensorController"
        private const val GRAVITY_FILTER_ALPHA = 0.82f
        // 200 Hz: fast enough to catch a knock's impact spike, and within the Android 12+ cap
        // that applies without the HIGH_SAMPLING_RATE_SENSORS permission.
        private const val BUMP_SAMPLING_PERIOD_US = 5_000
    }

    private val appContext = context.applicationContext
    private val sensorManager: SensorManager? =
        appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private val linearAccelSensor: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    private val fallbackAccelSensor: Sensor? =
        if (linearAccelSensor == null) sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) else null

    private val activeSensor: Sensor? = linearAccelSensor ?: fallbackAccelSensor
    val isSensorAvailable: Boolean = activeSensor != null

    private val bumpSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    val isBumpAvailable: Boolean = bumpSensor != null

    private val detector = FlickDetector()
    private val bumpDetector = BumpDetector()
    private var isRegistered = false
    private var isListening = false
    private var isBumpRegistered = false

    private val bumpListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event == null || !bumpDetector.isArmed) return
            val nowMs = event.timestamp / 1_000_000L
            if (bumpDetector.onSample(event.values[0], event.values[1], event.values[2], nowMs)) {
                val peak = bumpDetector.lastPeak
                Log.i(TAG, "Bump detected! peak=${"%.2f".format(peak)} m/s^2")
                disarmAll()
                triggerThrowHaptics()
                onWristFlickThrow(WristFlick(peak, aim = null, isBump = true))
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    // High-pass filter state when falling back to raw TYPE_ACCELEROMETER
    private val gravity = FloatArray(3)
    private var hasGravityBaseline = false

    /**
     * Arms or disarms flick detection. [handsFree] means the card is not being held, so the
     * stricter hands-free rules apply.
     */
    fun setArmed(arming: SensorArming) {
        val armed = arming.flick && isSensorAvailable
        if (armed && !detector.isArmed) {
            hasGravityBaseline = false
        }
        detector.arm(armed, arming.handsFree, SystemClock.elapsedRealtime())
        bumpDetector.arm(arming.bump && isBumpAvailable)
        updateBumpRegistration()
    }

    fun startListening() {
        isListening = true
        updateBumpRegistration()
        if (isRegistered || sensorManager == null || activeSensor == null) return
        val registered = sensorManager.registerListener(
            this,
            activeSensor,
            SensorManager.SENSOR_DELAY_GAME
        )
        isRegistered = registered
        Log.d(TAG, "Sensor listener registered=$registered (sensor=${activeSensor.name})")
    }

    fun stopListening() {
        isListening = false
        disarmAll()
        if (!isRegistered || sensorManager == null) return
        sensorManager.unregisterListener(this)
        isRegistered = false
        hasGravityBaseline = false
    }

    private fun disarmAll() {
        detector.arm(armed = false, handsFree = false, nowMs = SystemClock.elapsedRealtime())
        bumpDetector.arm(false)
        updateBumpRegistration()
    }

    private fun updateBumpRegistration() {
        val manager = sensorManager ?: return
        val sensor = bumpSensor ?: return
        val wanted = isListening && bumpDetector.isArmed
        if (wanted && !isBumpRegistered) {
            isBumpRegistered = manager.registerListener(bumpListener, sensor, BUMP_SAMPLING_PERIOD_US)
            Log.d(TAG, "Bump listener registered=$isBumpRegistered")
        } else if (!wanted && isBumpRegistered) {
            manager.unregisterListener(bumpListener)
            isBumpRegistered = false
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !detector.isArmed) return

        val lx: Float
        val ly: Float
        val lz: Float

        if (event.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION) {
            lx = event.values[0]
            ly = event.values[1]
            lz = event.values[2]
        } else {
            // High-pass filter on TYPE_ACCELEROMETER to isolate linear acceleration from gravity
            if (!hasGravityBaseline) {
                gravity[0] = event.values[0]
                gravity[1] = event.values[1]
                gravity[2] = event.values[2]
                hasGravityBaseline = true
                return
            }
            gravity[0] = GRAVITY_FILTER_ALPHA * gravity[0] + (1f - GRAVITY_FILTER_ALPHA) * event.values[0]
            gravity[1] = GRAVITY_FILTER_ALPHA * gravity[1] + (1f - GRAVITY_FILTER_ALPHA) * event.values[1]
            gravity[2] = GRAVITY_FILTER_ALPHA * gravity[2] + (1f - GRAVITY_FILTER_ALPHA) * event.values[2]

            lx = event.values[0] - gravity[0]
            ly = event.values[1] - gravity[1]
            lz = event.values[2] - gravity[2]
        }

        val magnitude = sqrt(lx * lx + ly * ly + lz * lz)
        val handsFree = detector.isHandsFree

        if (detector.onSample(magnitude, SystemClock.elapsedRealtime())) {
            Log.i(TAG, "Wrist-flick detected! magnitude=${"%.2f".format(magnitude)} m/s^2 handsFree=$handsFree")
                        disarmAll()
                        triggerThrowHaptics()
            onWristFlickThrow(WristFlick(magnitude, ThrowAim.fromWrist(lx, ly, lz)))
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    /**
     * Plays a crisp double-pulse haptic confirmation when a throw gesture launches the file(s).
     */
    fun triggerThrowHaptics() {
        try {
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager =
                    appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = try {
                        VibrationEffect.createWaveform(
                            longArrayOf(0L, 30L, 25L, 55L),
                            intArrayOf(0, 180, 0, 255),
                            -1
                        )
                    } catch (_: Exception) {
                        VibrationEffect.createOneShot(75L, VibrationEffect.DEFAULT_AMPLITUDE)
                    }
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(75L)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to trigger haptics", e)
        }
    }
}
