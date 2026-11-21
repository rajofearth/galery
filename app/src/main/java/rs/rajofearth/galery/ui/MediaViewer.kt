package rs.rajofearth.galery.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import rs.rajofearth.galery.R
import rs.rajofearth.galery.data.MediaItem
import kotlin.math.abs

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MediaViewer(
    items: List<MediaItem>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    scopes: MediaTransitionScopes,
) {
    BackHandler(onBack = onDismiss)
    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { items.size })
    val sharedKey = items[initialIndex].key

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(state = pagerState) { page ->
            ViewerPhoto(
                item = items[page],
                applySharedElement = items[page].key == sharedKey,
                onDismiss = onDismiss,
                scopes = scopes,
            )
        }

        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(16.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.viewer_close),
                tint = Color.White,
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ViewerPhoto(
    item: MediaItem,
    applySharedElement: Boolean,
    onDismiss: () -> Unit,
    scopes: MediaTransitionScopes,
) {
    var zoom by remember { mutableStateOf(ViewerZoomState()) }
    val dismissOffset by animateFloatAsState(
        targetValue = zoom.dismissY,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "Viewer dismiss",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                handlePhotoGestures(
                    zoom = { zoom },
                    onZoomChange = { zoom = it },
                    onDismiss = onDismiss,
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { zoom = zoom.toggleDoubleTap() })
            },
    ) {
        var imageModifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = zoom.scale
                scaleY = zoom.scale
                translationX = zoom.offsetX
                translationY = zoom.offsetY + dismissOffset
                alpha = (1f - dismissOffset / ViewerZoomState.DISMISS_FADE_DISTANCE).coerceIn(0f, 1f)
            }

        if (applySharedElement) {
            imageModifier = with(scopes.sharedTransition) {
                imageModifier.sharedElement(
                    rememberSharedContentState(key = "media-${item.key}"),
                    animatedVisibilityScope = scopes.visibility,
                )
            }
        }

        AsyncImage(
            model = rememberMediaImageModel(item),
            contentDescription = item.displayName,
            contentScale = ContentScale.Fit,
            modifier = imageModifier,
        )
    }
}

private suspend fun PointerInputScope.handlePhotoGestures(
    zoom: () -> ViewerZoomState,
    onZoomChange: (ViewerZoomState) -> Unit,
    onDismiss: () -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var lastPositions = mapOf(down.id to down.position)
        var pinchStartDistance = 0f
        var pinchStartScale = ViewerZoomState.MIN_SCALE
        var dismissing = false
        var dismissed = false

        while (true) {
            val event = awaitPointerEvent()
            val active = event.changes.filter { it.pressed }
            if (active.isEmpty()) break

            if (active.size >= 2) {
                val distance = (active[1].position - active[0].position).getDistance()
                if (pinchStartDistance == 0f) {
                    pinchStartDistance = distance
                    pinchStartScale = zoom().scale
                } else if (pinchStartDistance > 0f) {
                    onZoomChange(zoom().zoomTo(pinchStartScale * distance / pinchStartDistance))
                }
                active.forEach { it.consume() }
                lastPositions = active.associate { it.id to it.position }
            } else {
                pinchStartDistance = 0f
                val change = active.single()
                val previous = lastPositions[change.id] ?: change.position
                val delta = change.position - previous
                lastPositions = mapOf(change.id to change.position)

                val current = zoom()
                when {
                    current.scale > ViewerZoomState.MIN_SCALE -> {
                        onZoomChange(current.panBy(delta.x, delta.y))
                        change.consume()
                    }

                    dismissing || (delta.y > 0f && abs(delta.y) > abs(delta.x)) -> {
                        dismissing = true
                        val next = current.panBy(0f, delta.y)
                        onZoomChange(next)
                        change.consume()
                        if (next.isDismissed) {
                            dismissed = true
                            onDismiss()
                            break
                        }
                    }
                }
            }
        }

        if (!dismissed) onZoomChange(zoom().copy(dismissY = 0f))
    }
}
