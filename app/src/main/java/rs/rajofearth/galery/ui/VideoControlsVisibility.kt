package rs.rajofearth.galery.ui

data class VideoControlsVisibility(
    val lastInteractionMs: Long,
    val nowMs: Long,
    val isPlaying: Boolean = true,
) {
    val isVisible: Boolean
        get() = (nowMs - lastInteractionMs) < timeoutMs

    private val timeoutMs: Long
        get() = if (isPlaying) VISIBLE_DURATION_MS else PAUSED_VISIBLE_DURATION_MS

    companion object {
        const val VISIBLE_DURATION_MS = 3_000L
        const val PAUSED_VISIBLE_DURATION_MS = 2_000L
    }
}
