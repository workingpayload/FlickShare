package com.flickshare.app.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThrowAimTest {

    @Test
    fun flingUpAndRightLandsOnTheRight() {
        assertEquals(0f, ThrowAim.fromFling(0f, -2000f)!!.degrees, 0.01f)
        assertEquals(45f, ThrowAim.fromFling(2000f, -2000f)!!.degrees, 0.01f)
        assertEquals(-45f, ThrowAim.fromFling(-2000f, -2000f)!!.degrees, 0.01f)
        assertEquals(90f, ThrowAim.fromFling(2000f, 500f)!!.degrees, 0.01f) // sideways-down clamps
        assertNull(ThrowAim.fromFling(0f, 0f))
    }

    @Test
    fun flingFollowsTheDragPathWhenTheThumbCurlsOnRelease() {
        // Dragged up-left, but the thumb let go moving up and slightly right.
        assertTrue(ThrowAim.fromFling(800f, -2500f, dragX = -300f, dragY = -400f)!!.degrees < -10f)
        assertTrue(ThrowAim.fromFling(-800f, -2500f, dragX = 300f, dragY = -400f)!!.degrees > 10f)
        // A tiny drag (a pure flick) still uses the release velocity.
        assertEquals(45f, ThrowAim.fromFling(2000f, -2000f, dragX = 5f, dragY = -10f)!!.degrees, 0.01f)
    }

    @Test
    fun wristFlickWorksFlatOrUpright() {
        assertEquals(0f, ThrowAim.fromWrist(0f, 15f, 0f)!!.degrees, 0.01f) // flat, pushed toward the top edge
        assertEquals(0f, ThrowAim.fromWrist(0f, 0f, -15f)!!.degrees, 0.01f) // upright, pushed out the back
        assertEquals(-45f, ThrowAim.fromWrist(-10f, 10f, 0f)!!.degrees, 0.01f)
        assertNull(ThrowAim.fromWrist(0.5f, 0.5f, 0f))
    }

    @Test
    fun compassTurningLeftToFaceThePcMeansYouStandOnTheRight() {
        assertEquals(30f, ThrowAim.fromCompass(headingDeg = 60f, calibratedDeg = 90f).degrees, 0.01f)
        assertEquals(-20f, ThrowAim.fromCompass(headingDeg = 10f, calibratedDeg = 350f).degrees, 0.01f) // wraps north
        assertEquals(90f, ThrowAim.fromCompass(headingDeg = 0f, calibratedDeg = 170f).degrees, 0.01f)
        assertEquals("compass", ThrowAim.fromCompass(0f, 0f).source)
    }
}
