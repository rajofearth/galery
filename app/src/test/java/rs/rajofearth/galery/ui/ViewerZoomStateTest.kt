package rs.rajofearth.galery.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewerZoomStateTest {
    @Test
    fun `pinching out zooms in up to the maximum scale`() {
        val state = ViewerZoomState()

        val zoomed = state.zoomBy(10f)

        assertEquals(ViewerZoomState.MAX_SCALE, zoomed.scale, 0.0001f)
    }

    @Test
    fun `pinching back to the base scale drops the pan offset`() {
        val zoomed = ViewerZoomState().zoomBy(3f).panBy(40f, 30f)

        val restored = zoomed.zoomBy(0.1f)

        assertEquals(ViewerZoomState.MIN_SCALE, restored.scale, 0.0001f)
        assertEquals(0f, restored.offsetX, 0.0001f)
        assertEquals(0f, restored.offsetY, 0.0001f)
    }

    @Test
    fun `a zoomed photo can be panned`() {
        val panned = ViewerZoomState().zoomBy(2f).panBy(40f, -30f)

        assertEquals(40f, panned.offsetX, 0.0001f)
        assertEquals(-30f, panned.offsetY, 0.0001f)
    }

    @Test
    fun `a photo at base scale only accumulates downward drag for dismissal`() {
        val state = ViewerZoomState()

        val dragged = state.panBy(80f, -60f).panBy(0f, 90f)

        assertEquals(0f, dragged.offsetX, 0.0001f)
        assertEquals(0f, dragged.offsetY, 0.0001f)
        assertEquals(90f, dragged.dismissY, 0.0001f)
    }

    @Test
    fun `double tap toggles between the base and zoomed scale`() {
        val zoomed = ViewerZoomState().panBy(0f, 50f).toggleDoubleTap()

        assertEquals(ViewerZoomState.DOUBLE_TAP_SCALE, zoomed.scale, 0.0001f)
        assertEquals(0f, zoomed.dismissY, 0.0001f)

        val restored = zoomed.panBy(30f, 30f).toggleDoubleTap()

        assertEquals(ViewerZoomState.MIN_SCALE, restored.scale, 0.0001f)
        assertEquals(0f, restored.offsetX, 0.0001f)
        assertEquals(0f, restored.offsetY, 0.0001f)
    }

    @Test
    fun `pinching sets an absolute scale instead of multiplying the current scale`() {
        val zoomed = ViewerZoomState().zoomTo(2f)

        assertEquals(3f, zoomed.zoomTo(zoomed.scale * 1.5f).scale, 0.0001f)
    }

    @Test
    fun `pinching out beyond the maximum clamps the scale`() {
        assertEquals(ViewerZoomState.MAX_SCALE, ViewerZoomState().zoomTo(20f).scale, 0.0001f)
    }

    @Test
    fun `pinching back to base scale drops the pan offset`() {
        val panned = ViewerZoomState().zoomTo(3f).panBy(40f, 30f)

        val restored = panned.zoomTo(1f)

        assertEquals(0f, restored.offsetX, 0.0001f)
        assertEquals(0f, restored.offsetY, 0.0001f)
    }

    @Test
    fun `the Viewer dismisses only after dragging down past the threshold`() {
        assertFalse(ViewerZoomState().panBy(0f, ViewerZoomState.DISMISS_THRESHOLD - 1f).isDismissed)
        assertTrue(ViewerZoomState().panBy(0f, ViewerZoomState.DISMISS_THRESHOLD).isDismissed)
    }
}
