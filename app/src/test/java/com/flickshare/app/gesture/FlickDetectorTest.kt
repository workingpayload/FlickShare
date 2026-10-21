package com.flickshare.app.gesture

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlickDetectorTest {

    private val afterArmDelay = FlickDetector.HANDS_FREE_ARM_DELAY_MS

    @Test
    fun holdModeFiresOnSingleSampleOverThreshold() {
        val detector = FlickDetector().apply { arm(armed = true, handsFree = false, nowMs = 0L) }

        assertFalse(detector.onSample(11.9f, 10L))
        assertTrue(detector.onSample(12.0f, 20L))
    }

    @Test
    fun handsFreeIgnoresSingleSpike() {
        val detector = FlickDetector().apply { arm(armed = true, handsFree = true, nowMs = 0L) }

        assertFalse(detector.onSample(30f, afterArmDelay))
        assertFalse(detector.onSample(2f, afterArmDelay + 10))
        assertFalse(detector.onSample(30f, afterArmDelay + 20))
    }

    @Test
    fun handsFreeIgnoresHoldStrengthFlick() {
        val detector = FlickDetector().apply { arm(armed = true, handsFree = true, nowMs = 0L) }

        assertFalse(detector.onSample(14f, afterArmDelay))
        assertFalse(detector.onSample(14f, afterArmDelay + 10))
    }

    @Test
    fun handsFreeFiresOnConsecutiveStrongSamples() {
        val detector = FlickDetector().apply { arm(armed = true, handsFree = true, nowMs = 0L) }

        assertFalse(detector.onSample(18f, afterArmDelay))
        assertTrue(detector.onSample(18f, afterArmDelay + 10))
    }

    @Test
    fun handsFreeIgnoresMotionRightAfterArming() {
        val detector = FlickDetector().apply { arm(armed = true, handsFree = true, nowMs = 1_000L) }

        assertFalse(detector.onSample(25f, 1_100L))
        assertFalse(detector.onSample(25f, 1_110L))
    }

    @Test
    fun rearmingWithSameModeKeepsArmDelayStart() {
        val detector = FlickDetector().apply { arm(armed = true, handsFree = true, nowMs = 0L) }
        detector.arm(armed = true, handsFree = true, nowMs = afterArmDelay - 1)

        assertFalse(detector.onSample(20f, afterArmDelay))
        assertTrue(detector.onSample(20f, afterArmDelay + 10))
    }

    @Test
    fun disarmsAfterFiring() {
        val detector = FlickDetector().apply { arm(armed = true, handsFree = false, nowMs = 0L) }

        assertTrue(detector.onSample(20f, 10L))
        assertFalse(detector.isArmed)
        assertFalse(detector.onSample(20f, 2_000L))
    }

    @Test
    fun respectsCooldownAfterThrow() {
        val detector = FlickDetector().apply { arm(armed = true, handsFree = false, nowMs = 0L) }
        assertTrue(detector.onSample(20f, 10L))

        detector.arm(armed = true, handsFree = false, nowMs = 20L)
        assertFalse(detector.onSample(20f, 10L + FlickDetector.COOLDOWN_MS - 1))
        assertTrue(detector.onSample(20f, 10L + FlickDetector.COOLDOWN_MS))
    }

    @Test
    fun ignoresSamplesWhenDisarmed() {
        val detector = FlickDetector()

        assertFalse(detector.onSample(50f, 10_000L))
    }
}
