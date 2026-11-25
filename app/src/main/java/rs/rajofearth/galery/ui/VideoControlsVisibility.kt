package rs.rajofearth.galery.ui

internal object VideoControlsVisibility {
    const val VISIBLE_DURATION_MS = 3_000L

    fun hideAfter(isPlaying: Boolean, scrubbing: Boolean, timeoutMs: Long): Long? =
        if (isPlaying && !scrubbing) timeoutMs else null
}
