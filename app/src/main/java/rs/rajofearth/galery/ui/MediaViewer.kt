package rs.rajofearth.galery.ui

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as PlayerMediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import rs.rajofearth.galery.R
import rs.rajofearth.galery.data.MediaItem
import rs.rajofearth.galery.data.formatDuration
import kotlin.math.abs

private const val PROGRESS_INTERVAL_MS = 250L

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
    val currentPageIsVideo = items.getOrNull(pagerState.currentPage)?.isVideo == true

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(state = pagerState) { page ->
            val item = items[page]
            if (item.isVideo) {
                ViewerVideo(
                    item = item,
                    isActive = pagerState.currentPage == page,
                    onDismiss = onDismiss,
                )
            } else {
                ViewerPhoto(
                    item = item,
                    applySharedElement = item.key == sharedKey,
                    onDismiss = onDismiss,
                    scopes = scopes,
                )
            }
        }

        if (!currentPageIsVideo) {
            ViewerCloseButton(onDismiss = onDismiss)
        }
    }
}

@Composable
private fun ViewerVideo(
    item: MediaItem,
    isActive: Boolean,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var state by remember { mutableStateOf(VideoPlaybackState()) }
    var lastInteractionMs by remember { mutableLongStateOf(SystemClock.uptimeMillis()) }
    var nowMs by remember { mutableLongStateOf(lastInteractionMs) }
    var isScrubbing by remember { mutableStateOf(false) }

    val exoPlayer = remember(item.key) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            volume = 0f
            playWhenReady = true
            setMediaItem(PlayerMediaItem.fromUri(item.contentUri))
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    state = state.onPlaybackEnded()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                state = state.copy(isPlaying = isPlaying)
            }
        }
        exoPlayer.addListener(listener)
        exoPlayer.prepare()
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(exoPlayer, isActive) {
        exoPlayer.playWhenReady = isActive
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            if (!isScrubbing) {
                state = state
                    .onDurationAvailable(exoPlayer.duration)
                    .onPositionChange(exoPlayer.currentPosition)
            }
            nowMs = SystemClock.uptimeMillis()
            delay(PROGRESS_INTERVAL_MS)
        }
    }

    fun commit(next: VideoPlaybackState) {
        state = next
        lastInteractionMs = SystemClock.uptimeMillis()
        nowMs = lastInteractionMs
    }

    fun togglePlay() {
        val next = state.togglePlay()
        exoPlayer.playWhenReady = next.isPlaying
        commit(next)
    }

    fun toggleMute() {
        val next = state.toggleMute()
        exoPlayer.volume = if (next.isMuted) 0f else 1f
        commit(next)
    }

    fun seek(positionMs: Long) {
        val next = state.onPositionChange(positionMs)
        exoPlayer.seekTo(next.positionMs)
        isScrubbing = true
        commit(next)
    }

    val controlsVisible = VideoControlsVisibility(
        lastInteractionMs = lastInteractionMs,
        nowMs = nowMs,
        isPlaying = state.isPlaying,
    ).isVisible
    val scrubState = rememberSliderState(value = 0f, trackRange = 0f..1f)
    LaunchedEffect(state.positionMs, state.durationMs) {
        scrubState.value = if (state.durationMs > 0L) {
            state.positionMs.toFloat() / state.durationMs.toFloat()
        } else {
            0f
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { togglePlay() })
            },
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    player = exoPlayer
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        Box(Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()),
                exit = fadeOut(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()),
            ) {
                Box(Modifier.fillMaxSize()) {
                    ViewerCloseButton(onDismiss = onDismiss)

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.systemBars)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BlurredControlButton(
                            onClick = ::togglePlay,
                            imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = stringResource(
                                if (state.isPlaying) R.string.viewer_pause else R.string.viewer_play,
                            ),
                        )
                        BlurredControlButton(
                            onClick = ::toggleMute,
                            imageVector = if (state.isMuted) {
                                Icons.AutoMirrored.Filled.VolumeOff
                            } else {
                                Icons.AutoMirrored.Filled.VolumeUp
                            },
                            contentDescription = stringResource(
                                if (state.isMuted) R.string.viewer_unmute else R.string.viewer_mute,
                            ),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = formatDuration(state.positionMs),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Slider(
                            state = scrubState,
                            onValueChange = { fraction ->
                                seek((fraction * state.durationMs).toLong())
                            },
                            onValueChangeFinished = { isScrubbing = false },
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = formatDuration(state.durationMs),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.ViewerCloseButton(onDismiss: () -> Unit) {
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

@Composable
private fun BlurredControlButton(
    onClick: () -> Unit,
    imageVector: ImageVector,
    contentDescription: String,
) {
    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Spacer(
            Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.55f),
                            Color.White.copy(alpha = 0.35f),
                            Color.Transparent,
                        ),
                    ),
                )
                .blur(8.dp),
        )
        IconButton(onClick = onClick) {
            Icon(imageVector, contentDescription, tint = Color.White)
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
