package com.example.npucourse

import com.example.npucourse.overlay.OverlayGesture
import com.example.npucourse.overlay.OverlayGestureClassifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OverlayGestureClassifierTest {
    @Test fun resolvesAllFourDirections() {
        assertEquals(OverlayGesture.SWIPE_LEFT, detect(-100f, 8f))
        assertEquals(OverlayGesture.SWIPE_RIGHT, detect(100f, -8f))
        assertEquals(OverlayGesture.SWIPE_UP, detect(7f, -100f))
        assertEquals(OverlayGesture.SWIPE_DOWN, detect(-7f, 100f))
    }

    @Test fun rejectsShortAndDiagonalMovement() {
        assertNull(OverlayGestureClassifier.detect(20f, 0f, 20f, 60f, 900f, 360f))
        assertNull(detect(80f, 70f))
    }

    private fun detect(dx: Float, dy: Float): String? =
        OverlayGestureClassifier.detect(dx, dy, kotlin.math.hypot(dx, dy), 60f, 500f, 360f)
}
