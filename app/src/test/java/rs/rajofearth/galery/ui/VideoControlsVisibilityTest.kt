package rs.rajofearth.galery.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoControlsVisibilityTest {
    @Test
    fun `controls stay visible just after an interaction`() {
        assertTrue(VideoControlsVisibility(lastInteractionMs = 1_000L, nowMs = 1_000L).isVisible)
        assertTrue(VideoControlsVisibility(lastInteractionMs = 1_000L, nowMs = 3_999L).isVisible)
    }

    @Test
    fun `controls fade after three seconds without interaction`() {
        assertFalse(VideoControlsVisibility(lastInteractionMs = 1_000L, nowMs = 4_000L).isVisible)
    }

    @Test
    fun `controls fade after two seconds when the video is paused`() {
        assertTrue(
            VideoControlsVisibility(
                lastInteractionMs = 1_000L,
                nowMs = 2_999L,
                isPlaying = false,
            ).isVisible,
        )
        assertFalse(
            VideoControlsVisibility(
                lastInteractionMs = 1_000L,
                nowMs = 3_000L,
                isPlaying = false,
            ).isVisible,
        )
    }

    @Test
    fun `a new interaction brings the controls back`() {
        val faded = VideoControlsVisibility(lastInteractionMs = 1_000L, nowMs = 4_000L)
        assertFalse(faded.isVisible)

        assertTrue(VideoControlsVisibility(lastInteractionMs = 4_000L, nowMs = 5_000L).isVisible)
    }
}
