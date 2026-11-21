package rs.rajofearth.galery.ui

data class ViewerZoomState(
    val scale: Float = MIN_SCALE,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val dismissY: Float = 0f,
) {
    val isDismissed: Boolean
        get() = dismissY >= DISMISS_THRESHOLD

    fun zoomBy(factor: Float): ViewerZoomState = zoomTo(scale * factor)

    fun zoomTo(nextScale: Float): ViewerZoomState {
        val clampedScale = nextScale.coerceIn(MIN_SCALE, MAX_SCALE)
        return if (clampedScale == MIN_SCALE) {
            copy(scale = clampedScale, offsetX = 0f, offsetY = 0f)
        } else {
            copy(scale = clampedScale)
        }
    }

    fun panBy(dx: Float, dy: Float): ViewerZoomState =
        if (scale > MIN_SCALE) {
            copy(offsetX = offsetX + dx, offsetY = offsetY + dy)
        } else {
            copy(dismissY = (dismissY + dy).coerceAtLeast(0f))
        }

    fun toggleDoubleTap(): ViewerZoomState =
        if (scale > MIN_SCALE) {
            copy(scale = MIN_SCALE, offsetX = 0f, offsetY = 0f, dismissY = 0f)
        } else {
            copy(scale = DOUBLE_TAP_SCALE, dismissY = 0f)
        }

    companion object {
        const val MIN_SCALE = 1f
        const val MAX_SCALE = 5f
        const val DOUBLE_TAP_SCALE = 2.5f
        const val DISMISS_THRESHOLD = 220f
        const val DISMISS_FADE_DISTANCE = 700f
    }
}
