package rs.rajofearth.galery.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import rs.rajofearth.galery.data.MediaLayout

internal class MediaTileTargets {
    var viewport: LayoutCoordinates? = null
    private val tiles = mutableMapOf<String, LayoutCoordinates>()

    @Composable
    fun track(key: String): Modifier {
        DisposableEffect(this, key) {
            onDispose { tiles.remove(key) }
        }
        return Modifier.onGloballyPositioned { tiles[key] = it }
    }

    fun at(position: Offset, nearest: Boolean): String? {
        val parent = viewport?.takeIf { it.isAttached } ?: return null
        var best: String? = null
        var distance = Float.POSITIVE_INFINITY
        for ((key, tile) in tiles) {
            if (!tile.isAttached) continue
            val bounds = parent.localBoundingBoxOf(tile)
            if (bounds.isEmpty) continue
            if (bounds.contains(position)) return key
            if (!nearest) continue
            val dx = position.x - position.x.coerceIn(bounds.left, bounds.right)
            val dy = position.y - position.y.coerceIn(bounds.top, bounds.bottom)
            val squared = dx * dx + dy * dy
            if (squared < distance) {
                best = key
                distance = squared
            }
        }
        return best
    }
}

@Composable
internal fun mediaRangeGesture(
    state: MediaTilesState,
    targets: MediaTileTargets,
    mediaKeys: () -> List<String>,
    selectedKeys: Set<String>,
    onChange: (Set<String>) -> Unit,
    padding: PaddingValues,
): Modifier {
    val ordered by rememberUpdatedState(mediaKeys)
    val selection by rememberUpdatedState(selectedKeys)
    val change by rememberUpdatedState(onChange)
    val insets by rememberUpdatedState(padding)
    val haptic = LocalHapticFeedback.current
    return Modifier.onGloballyPositioned { targets.viewport = it }.pointerInput(state, targets, haptic) {
        val edge = 56.dp.toPx()
        val maxSpeed = 980.dp.toPx()
        val scrollable = if (state.layout == MediaLayout.Grid) state.gridState else state.listState
        coroutineScope {
            val jobs = this
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val anchor = targets.at(down.position, nearest = false) ?: return@awaitEachGesture
                val held = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
                val range = MediaDragSelection.start(ordered(), anchor, selection) ?: return@awaitEachGesture
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                range.moveTo(anchor)?.let(change)
                var finger = held.position
                var dragging = false
                fun updateEndpoint() {
                    targets.at(finger, nearest = true)?.let(range::moveTo)?.let(change)
                }
                fun scrollVelocity(): Float {
                    val height = size.height.toFloat()
                    val upper = (insets.calculateTopPadding().toPx() + edge).coerceAtMost(height / 2)
                    val lower = (height - insets.calculateBottomPadding().toPx() - edge).coerceAtLeast(height / 2)
                    return when {
                        finger.y < upper -> -((upper - finger.y) / edge).coerceAtMost(1f) * maxSpeed
                        finger.y > lower -> ((finger.y - lower) / edge).coerceAtMost(1f) * maxSpeed
                        else -> 0f
                    }
                }
                var frameJob: Job? = null
                fun updateAutoScroll() {
                    if (scrollVelocity() == 0f) {
                        frameJob?.cancel()
                        return
                    }
                    if (frameJob?.isActive == true) return
                    frameJob = jobs.launch {
                        var previous = withFrameNanos { it }
                        while (true) {
                            val now = withFrameNanos { it }
                            val seconds = ((now - previous) / 1e9f).coerceIn(0f, 0.05f)
                            previous = now
                            val speed = scrollVelocity()
                            if (speed == 0f || speed < 0f && !scrollable.canScrollBackward ||
                                speed > 0f && !scrollable.canScrollForward
                            ) break
                            scrollable.scrollBy(speed * seconds)
                            updateEndpoint()
                        }
                    }
                }
                try {
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val pointer = event.changes.firstOrNull { it.id == held.id } ?: break
                        if (pointer.isConsumed) break
                        finger = pointer.position
                        if (!dragging && (finger - held.position).getDistance() >= viewConfiguration.touchSlop) {
                            dragging = true
                        }
                        if (dragging) {
                            updateEndpoint()
                            if (pointer.pressed) updateAutoScroll()
                        }
                        event.changes.forEach { it.consume() }
                    } while (pointer.pressed)
                } finally {
                    frameJob?.cancel()
                }
            }
        }
    }
}
