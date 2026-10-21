package com.flickshare.app.gesture

/**
 * Decides when linear-acceleration samples count as a deliberate throw.
 *
 * Two arming modes:
 * - **Hold**: the finger is on the staged card, so intent is clear; one sample over
 *   [HOLD_THRESHOLD_MPS2] fires.
 * - **Hands-free**: nothing is touched, so everyday motion must not send files. A flick must be
 *   stronger ([HANDS_FREE_THRESHOLD_MPS2]) and last [HANDS_FREE_MIN_SAMPLES] samples in a row
 *   (a bump or setting the phone down is a single-sample spike), and nothing fires for
 *   [HANDS_FREE_ARM_DELAY_MS] after arming (e.g. while picking files or releasing the card).
 *
 * The detector disarms itself after firing so one flick sends once.
 */
class FlickDetector {

    companion object {
        const val HOLD_THRESHOLD_MPS2 = 12.0f
        const val HANDS_FREE_THRESHOLD_MPS2 = 17.0f
        const val HANDS_FREE_MIN_SAMPLES = 2
        const val HANDS_FREE_ARM_DELAY_MS = 800L
        const val COOLDOWN_MS = 600L
    }

    var isArmed: Boolean = false
        private set
    var isHandsFree: Boolean = false
        private set

    private var armedAtMs = 0L
    private var strongSamples = 0
    private var lastThrowMs: Long? = null

    fun arm(armed: Boolean, handsFree: Boolean, nowMs: Long) {
        if (armed && (!isArmed || handsFree != isHandsFree)) {
            armedAtMs = nowMs
        }
        strongSamples = 0
        isArmed = armed
        isHandsFree = armed && handsFree
    }

    /** Returns `true` when this sample completes a throw. */
    fun onSample(magnitude: Float, nowMs: Long): Boolean {
        if (!isArmed) return false
        if (isHandsFree && nowMs - armedAtMs < HANDS_FREE_ARM_DELAY_MS) return false

        val threshold = if (isHandsFree) HANDS_FREE_THRESHOLD_MPS2 else HOLD_THRESHOLD_MPS2
        if (magnitude < threshold) {
            strongSamples = 0
            return false
        }
        strongSamples++

        val needed = if (isHandsFree) HANDS_FREE_MIN_SAMPLES else 1
        val last = lastThrowMs
        if (strongSamples < needed || (last != null && nowMs - last < COOLDOWN_MS)) return false

        lastThrowMs = nowMs
        isArmed = false
        isHandsFree = false
        strongSamples = 0
        return true
    }
}
