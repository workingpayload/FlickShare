package com.flickshare.app.gesture

import kotlin.math.sqrt

/**
 * Detects "bump to send": the phone tapped twice against the PC (a double knock).
 *
 * Works on raw [android.hardware.Sensor.TYPE_ACCELEROMETER] samples (~200 Hz) because the fused
 * linear-acceleration sensor smooths away the short impact spike of a knock. Gravity is removed
 * with a time-based low-pass filter.
 *
 * A knock is a spike of at least [SPIKE_MPS2] that settles within [RING_MS]. Two knocks
 * [MIN_GAP_MS]..[MAX_GAP_MS] apart, with the phone still between them and for [CONFIRM_MS]
 * after the second, count as a bump. One knock (e.g. setting the phone on a desk), a third knock,
 * or continuous motion (a flick, walking, shaking) does not.
 */
class BumpDetector {

    companion object {
        const val SPIKE_MPS2 = 14.0f
        const val QUIET_MPS2 = 4.5f
        const val RING_MS = 60L
        const val MIN_GAP_MS = 90L
        const val MAX_GAP_MS = 600L
        const val CONFIRM_MS = 150L
        const val ARM_DELAY_MS = 500L
        private const val GRAVITY_TAU_MS = 100f
    }

    private enum class Stage { IDLE, KNOCKED_ONCE, KNOCKED_TWICE }

    var isArmed: Boolean = false
        private set

    /** Peak spike of the last detected bump, for the throw label. */
    var lastPeak: Float = 0f
        private set

    private val gravity = FloatArray(3)
    private var hasBaseline = false
    private var startMs = 0L
    private var lastSampleMs = 0L
    private var stage = Stage.IDLE
    private var knockMs = 0L
    private var peak = 0f

    fun arm(armed: Boolean) {
        if (armed && !isArmed) {
            hasBaseline = false
            stage = Stage.IDLE
        }
        isArmed = armed
    }

    /** Feeds one raw accelerometer sample. Returns `true` when it completes a double knock. */
    fun onSample(x: Float, y: Float, z: Float, timeMs: Long): Boolean {
        if (!isArmed) return false
        if (!hasBaseline) {
            gravity[0] = x; gravity[1] = y; gravity[2] = z
            hasBaseline = true
            startMs = timeMs
            lastSampleMs = timeMs
            return false
        }

        val hx = x - gravity[0]
        val hy = y - gravity[1]
        val hz = z - gravity[2]
        val magnitude = sqrt(hx * hx + hy * hy + hz * hz)

        val dt = (timeMs - lastSampleMs).coerceIn(1L, 100L).toFloat()
        lastSampleMs = timeMs
        // Learn gravity slowly during spikes so a knock doesn't leak into the baseline.
        var alpha = dt / (GRAVITY_TAU_MS + dt)
        if (magnitude >= QUIET_MPS2) alpha *= 0.1f
        gravity[0] += alpha * hx
        gravity[1] += alpha * hy
        gravity[2] += alpha * hz

        if (timeMs - startMs < ARM_DELAY_MS) return false

        when (stage) {
            Stage.IDLE -> if (magnitude >= SPIKE_MPS2) startKnock(timeMs, magnitude)

            Stage.KNOCKED_ONCE -> {
                val sinceKnock = timeMs - knockMs
                when {
                    sinceKnock <= RING_MS -> peak = maxOf(peak, magnitude)
                    magnitude >= SPIKE_MPS2 -> when {
                        sinceKnock < MIN_GAP_MS -> stage = Stage.IDLE
                        sinceKnock <= MAX_GAP_MS -> {
                            stage = Stage.KNOCKED_TWICE
                            knockMs = timeMs
                            peak = maxOf(peak, magnitude)
                        }
                        else -> startKnock(timeMs, magnitude)
                    }
                    magnitude >= QUIET_MPS2 || sinceKnock > MAX_GAP_MS -> stage = Stage.IDLE
                }
            }

            Stage.KNOCKED_TWICE -> {
                val sinceKnock = timeMs - knockMs
                when {
                    sinceKnock <= RING_MS -> peak = maxOf(peak, magnitude)
                    magnitude >= QUIET_MPS2 -> stage = Stage.IDLE
                    sinceKnock >= CONFIRM_MS -> {
                        stage = Stage.IDLE
                        isArmed = false
                        lastPeak = peak
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun startKnock(timeMs: Long, magnitude: Float) {
        stage = Stage.KNOCKED_ONCE
        knockMs = timeMs
        peak = magnitude
    }
}
