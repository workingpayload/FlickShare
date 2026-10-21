package com.flickshare.app.gesture

import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Which side of the PC screen a throw arrives from, sent to the receiver as
 * `X-FlickShare-Direction` so the desktop liquid glass can enter from that side.
 *
 * [degrees] is in -90..90: 0 = straight on (glass rises from the bottom centre),
 * negative = from the left, positive = from the right. [source] says how it was measured.
 */
data class ThrowAim(val degrees: Float, val source: String) {

    fun describe(): String {
        val side = when {
            abs(degrees) < 8f -> "straight on"
            degrees < 0f -> String.format(Locale.US, "%.0f° left", -degrees)
            else -> String.format(Locale.US, "%.0f° right", degrees)
        }
        return "$side · $source"
    }

    companion object {
        const val SOURCE_FLING = "fling"
        const val SOURCE_WRIST = "wrist"
        const val SOURCE_COMPASS = "compass"

        private const val MIN_FLING_PX_PER_S = 1f
        private const val MIN_DRAG_PX = 40f
        private const val DRAG_WEIGHT = 2f
        private const val MIN_WRIST_MPS2 = 2f

        /**
         * Touch fling (px and px/s, +y = down). Flicking up and to the right lands on the right.
         *
         * The release velocity alone is unreliable: a thumb curls at the end of a flick, so a throw dragged
         * up-left can let go moving up-right. The card's whole drag path ([dragX], [dragY]) carries the intent,
         * so it is weighted over the release velocity when the card actually moved.
         */
        fun fromFling(vx: Float, vy: Float, dragX: Float = 0f, dragY: Float = 0f): ThrowAim? {
            val speed = hypot(vx, vy)
            val drag = hypot(dragX, dragY)
            if (speed < MIN_FLING_PX_PER_S && drag < MIN_DRAG_PX) return null
            var x = if (speed >= MIN_FLING_PX_PER_S) vx / speed else 0f
            var y = if (speed >= MIN_FLING_PX_PER_S) vy / speed else 0f
            if (drag >= MIN_DRAG_PX) {
                x += DRAG_WEIGHT * dragX / drag
                y += DRAG_WEIGHT * dragY / drag
            }
            return ThrowAim(clamp(degrees(atan2(x, -y))), SOURCE_FLING)
        }

        /**
         * Wrist flick from linear acceleration in device axes. "Forward" is the phone's top edge (+y)
         * or its back (-z), so it works whether the phone is held flat or upright; +x is sideways right.
         */
        fun fromWrist(ax: Float, ay: Float, az: Float): ThrowAim? {
            val forward = ay - az
            if (hypot(ax, forward) < MIN_WRIST_MPS2) return null
            return ThrowAim(clamp(degrees(atan2(ax, forward))), SOURCE_WRIST)
        }

        /**
         * Compass aim: [headingDeg] now vs [calibratedDeg], recorded while pointing at the PC from straight
         * in front of it. Standing to the right of the PC you turn left to face it, which gives a positive
         * angle, so the glass enters from the right.
         */
        fun fromCompass(headingDeg: Float, calibratedDeg: Float): ThrowAim =
            ThrowAim(clamp(wrap180(calibratedDeg - headingDeg)), SOURCE_COMPASS)

        /** Wraps any angle into [-180, 180). */
        fun wrap180(deg: Float): Float {
            var x = (deg + 180f) % 360f
            if (x < 0f) x += 360f
            return x - 180f
        }

        private fun clamp(deg: Float): Float = deg.coerceIn(-90f, 90f)

        private fun degrees(rad: Float): Float = Math.toDegrees(rad.toDouble()).toFloat()
    }
}
