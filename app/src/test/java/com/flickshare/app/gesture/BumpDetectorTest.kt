package com.flickshare.app.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sin

/** Simulates a phone held still at 200 Hz and feeds [BumpDetector] with knocks or motion. */
class BumpDetectorTest {

    private class Phone(private val detector: BumpDetector) {
        var timeMs = 0L
        var fired = 0

        private fun sample(extraZ: Float, extraX: Float = 0f) {
            if (detector.onSample(extraX, 0f, 9.81f + extraZ, timeMs)) fired++
            timeMs += 5
        }

        fun still(ms: Long) = repeat((ms / 5).toInt()) { sample(0f) }

        /** A sharp impact: a 10 ms spike, then a short ringing that settles. */
        fun knock(strength: Float = 25f) {
            sample(strength)
            sample(-strength * 0.6f)
            sample(strength * 0.2f)
            sample(-1f)
        }

        /** Continuous swinging motion such as a flick, walking, or shaking. */
        fun move(ms: Long, amplitude: Float) {
            repeat((ms / 5).toInt()) { i -> sample(0f, extraX = amplitude * sin(i / 6.0).toFloat()) }
        }
    }

    private fun armedPhone(): Pair<BumpDetector, Phone> {
        val detector = BumpDetector().apply { arm(true) }
        return detector to Phone(detector).apply { still(BumpDetector.ARM_DELAY_MS + 100) }
    }

    @Test
    fun doubleKnockFiresOnceAndDisarms() {
        val (detector, phone) = armedPhone()

        phone.knock()
        phone.still(200)
        phone.knock()
        phone.still(300)

        assertEquals(1, phone.fired)
        assertFalse(detector.isArmed)
        assertTrue(detector.lastPeak >= 25f)
    }

    @Test
    fun singleKnockLikeSettingPhoneDownDoesNotFire() {
        val (_, phone) = armedPhone()

        phone.knock(40f)
        phone.still(1_000)

        assertEquals(0, phone.fired)
    }

    @Test
    fun tripleKnockDoesNotFire() {
        val (_, phone) = armedPhone()

        phone.knock()
        phone.still(150)
        phone.knock()
        phone.still(100)
        phone.knock()
        phone.still(500)

        assertEquals(0, phone.fired)
    }

    @Test
    fun knocksTooFarApartDoNotFire() {
        val (_, phone) = armedPhone()

        phone.knock()
        phone.still(BumpDetector.MAX_GAP_MS + 100)
        phone.knock()
        phone.still(BumpDetector.CONFIRM_MS + 100)

        assertEquals(0, phone.fired)
    }

    @Test
    fun laterKnockAfterTimeoutStartsANewDoubleKnock() {
        val (_, phone) = armedPhone()

        phone.knock()
        phone.still(BumpDetector.MAX_GAP_MS + 200)
        phone.knock()
        phone.still(200)
        phone.knock()
        phone.still(300)

        assertEquals(1, phone.fired)
    }

    @Test
    fun motionBetweenKnocksDoesNotFire() {
        val (_, phone) = armedPhone()

        phone.knock()
        phone.move(150, amplitude = 8f)
        phone.knock()
        phone.still(300)

        assertEquals(0, phone.fired)
    }

    @Test
    fun vigorousShakingDoesNotFire() {
        val (_, phone) = armedPhone()

        phone.move(2_000, amplitude = 30f)
        phone.still(300)

        assertEquals(0, phone.fired)
    }

    @Test
    fun ignoresKnocksRightAfterArming() {
        val detector = BumpDetector().apply { arm(true) }
        val phone = Phone(detector)

        phone.still(50)
        phone.knock()
        phone.still(150)
        phone.knock()
        phone.still(300)

        assertEquals(0, phone.fired)
    }

    @Test
    fun disarmedDetectorIgnoresKnocks() {
        val phone = Phone(BumpDetector())

        phone.still(BumpDetector.ARM_DELAY_MS + 100)
        phone.knock()
        phone.still(200)
        phone.knock()
        phone.still(300)

        assertEquals(0, phone.fired)
    }
}
