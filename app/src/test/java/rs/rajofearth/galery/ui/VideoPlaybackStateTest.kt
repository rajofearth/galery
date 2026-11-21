package rs.rajofearth.galery.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoPlaybackStateTest {
    @Test
    fun `a video opens muted and playing`() {
        val state = VideoPlaybackState()

        assertTrue(state.isPlaying)
        assertTrue(state.isMuted)
    }

    @Test
    fun `tapping the surface pauses and resumes playback`() {
        val paused = VideoPlaybackState().togglePlay()

        assertFalse(paused.isPlaying)
        assertTrue(paused.togglePlay().isPlaying)
    }

    @Test
    fun `the sound toggle enables and disables audio`() {
        val unmuted = VideoPlaybackState().toggleMute()

        assertFalse(unmuted.isMuted)
        assertTrue(unmuted.toggleMute().isMuted)
    }

    @Test
    fun `scrubbing past the duration clamps to the end`() {
        val state = VideoPlaybackState(durationMs = 90_000L)

        assertEquals(90_000L, state.onPositionChange(120_000L).positionMs)
    }

    @Test
    fun `scrubbing before the start clamps to zero`() {
        val state = VideoPlaybackState(durationMs = 90_000L)

        assertEquals(0L, state.onPositionChange(-1L).positionMs)
    }

    @Test
    fun `progress updates stay inside the known duration`() {
        val state = VideoPlaybackState().onDurationAvailable(90_000L)

        assertEquals(45_000L, state.onPositionChange(45_000L).positionMs)
        assertEquals(0L, state.onPositionChange(-5L).positionMs)
        assertEquals(90_000L, state.onPositionChange(100_000L).positionMs)
    }

    @Test
    fun `an unset duration does not break position updates`() {
        val state = VideoPlaybackState().onDurationAvailable(-1L)

        assertEquals(0L, state.positionMs)
        assertEquals(0L, state.onPositionChange(50L).positionMs)
    }

    @Test
    fun `reaching the end stops the video instead of restarting it`() {
        val state = VideoPlaybackState(durationMs = 90_000L, positionMs = 90_000L, isPlaying = true)

        val ended = state.onPlaybackEnded()

        assertFalse(ended.isPlaying)
        assertEquals(90_000L, ended.positionMs)
    }
}
