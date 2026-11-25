package rs.rajofearth.galery.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoControlsVisibilityTest {
    @Test
    fun `playing controls use the accessibility timeout`() {
        assertEquals(8_000L, VideoControlsVisibility.hideAfter(isPlaying = true, scrubbing = false, timeoutMs = 8_000L))
    }

    @Test
    fun `paused controls stay visible indefinitely`() {
        assertNull(VideoControlsVisibility.hideAfter(isPlaying = false, scrubbing = false, timeoutMs = 3_000L))
    }

    @Test
    fun `scrubbing keeps playing controls visible`() {
        assertNull(VideoControlsVisibility.hideAfter(isPlaying = true, scrubbing = true, timeoutMs = 3_000L))
    }
}
