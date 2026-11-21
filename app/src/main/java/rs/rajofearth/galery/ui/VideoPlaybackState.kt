package rs.rajofearth.galery.ui

data class VideoPlaybackState(
    val isPlaying: Boolean = true,
    val isMuted: Boolean = true,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
) {
    fun onDurationAvailable(durationMs: Long): VideoPlaybackState =
        copy(durationMs = durationMs).clampPosition()

    fun onPositionChange(positionMs: Long): VideoPlaybackState =
        copy(positionMs = positionMs).clampPosition()

    fun togglePlay(): VideoPlaybackState = copy(isPlaying = !isPlaying)

    fun toggleMute(): VideoPlaybackState = copy(isMuted = !isMuted)

    fun onPlaybackEnded(): VideoPlaybackState =
        copy(isPlaying = false, positionMs = durationMs).clampPosition()

    private fun clampPosition(): VideoPlaybackState =
        copy(positionMs = positionMs.coerceIn(0L, durationMs.coerceAtLeast(0L)))
}
