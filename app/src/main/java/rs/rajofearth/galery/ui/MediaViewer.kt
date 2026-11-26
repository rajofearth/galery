package rs.rajofearth.galery.ui

import android.content.pm.ActivityInfo
import android.os.SystemClock
import android.view.TextureView
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import rs.rajofearth.galery.ui.theme.GalleryIcon
import rs.rajofearth.galery.ui.theme.GalleryText
import rs.rajofearth.galery.ui.theme.GalleryPalette
import rs.rajofearth.galery.ui.theme.GalleryTypography
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem as PlayerMediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import rs.rajofearth.galery.R
import rs.rajofearth.galery.data.MediaItem
import rs.rajofearth.galery.data.formatDuration
import kotlin.math.abs

@Composable
internal fun MediaViewer(
    items: List<MediaItem>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    visible: Boolean,
    onGlassSource: ((() -> GlassFrame?)?) -> Unit = {},
    onPageChanged: (MediaItem) -> Unit = {},
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    onShare: (MediaItem) -> Unit,
    onRestore: ((MediaItem) -> Unit)? = null,
    onDetails: (MediaItem) -> Unit,
    onTrash: ((MediaItem) -> Unit)? = null,
) {
    if (items.isEmpty()) return
    BackHandler(enabled = visible, onBack = onDismiss)
    val pager = rememberPagerState(initialIndex.coerceIn(items.indices), pageCount = { items.size })
    val pageIndex = pager.settledPage.coerceIn(items.indices)
    val currentItem = items[pageIndex]
    val scope = rememberCoroutineScope()
    val backdrop = rememberGlassBackdrop(enabled = !currentItem.isVideo)
    val videoBackdrop = remember(currentItem.key) { mutableStateOf<GlassFrame?>(null) }
    val videoControlsVisible = remember(currentItem.key) { mutableStateOf(true) }
    var photoControlsVisible by remember { mutableStateOf(true) }
    val mediaFrame: () -> GlassFrame? = remember(backdrop, videoBackdrop, currentItem.isVideo) {
        if (currentItem.isVideo) ({ videoBackdrop.value }) else ({ backdrop.frame })
    }
    val publishSource by rememberUpdatedState(onGlassSource)
    DisposableEffect(mediaFrame) {
        publishSource(mediaFrame)
        onDispose { publishSource(null) }
    }
    val activity = LocalActivity.current
    DisposableEffect(activity, visible && currentItem.isVideo) {
        if (activity == null || !visible || !currentItem.isVideo) return@DisposableEffect onDispose {}
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
        onDispose { activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
    }
    LaunchedEffect(currentItem.key) { onPageChanged(currentItem) }
    CompositionLocalProvider(LocalGalleryGlass provides backdrop) {
    BoxWithConstraints(Modifier.fillMaxSize().background(GalleryPalette.background)
        .windowInsetsPadding(WindowInsets.systemBars)) {
        val landscapeVideo = currentItem.isVideo && maxWidth > maxHeight
        val wideVideo = landscapeVideo && maxWidth >= 600.dp
        val showViewerChrome = when {
            landscapeVideo -> videoControlsVisible.value
            currentItem.isVideo -> true
            else -> photoControlsVisible
        }
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxSize().then(if (currentItem.isVideo) Modifier else Modifier.glassBackdropSource(backdrop)),
            key = { items[it].key },
            userScrollEnabled = visible,
            beyondViewportPageCount = 1,
        ) { page ->
            val item = items[page]
            val selected = pager.settledPage == page
            if (item.isVideo) {
                ViewerVideo(
                    item,
                    active = selected && visible,
                    actionBackdrop = videoBackdrop.takeIf { selected },
                    wideVideo = wideVideo,
                    controlsVisible = videoControlsVisible.takeIf { selected },
                )
            } else {
                ViewerPhoto(item, onDismiss, onTap = { photoControlsVisible = !photoControlsVisible })
            }
        }
        AnimatedVisibility(showViewerChrome, enter = fadeIn(GalleryDesign.effects()), exit = fadeOut(GalleryDesign.effects())) {
            ViewerHeader(currentItem, onDismiss, mediaFrame)
        }
        AnimatedVisibility(
            visible = showViewerChrome,
            modifier = Modifier.align(if (wideVideo) Alignment.BottomEnd else Alignment.BottomCenter),
            enter = fadeIn(GalleryDesign.effects()),
            exit = fadeOut(GalleryDesign.effects()),
        ) {
        Column(
            Modifier.then(if (wideVideo) Modifier.width(240.dp) else Modifier)
                .padding(bottom = GalleryDesign.gap),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (!currentItem.isVideo && items.size > 1) {
                PhotoFilmstrip(items, pageIndex) { index ->
                    scope.launch { pager.animateScrollToPage(index, animationSpec = GalleryDesign.spatial()) }
                }
            }
            val tint = GalleryPalette.surfaceContainer.copy(alpha = GalleryDesign.viewerTintOpacity)
            Row(
                Modifier.widthIn(max = 440.dp).fillMaxWidth()
                    .padding(horizontal = GalleryDesign.inset),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val shareIcon = if (onRestore == null) Icons.Filled.Share else Icons.Filled.Restore
                val shareLabel = stringResource(if (onRestore == null) R.string.share_selection else R.string.restore_media)
                GalleryIconButton(
                    shareIcon, shareLabel, { if (onRestore == null) onShare(currentItem) else onRestore(currentItem) },
                    Modifier.sampledGlass(CircleShape, tint, mediaFrame),
                )
                if (onToggleFavorite != null || onTrash != null) {
                    Row(Modifier.sampledGlass(CircleShape, tint, mediaFrame).padding(horizontal = 4.dp)) {
                        if (onToggleFavorite != null) {
                            GalleryIconButton(
                                if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                stringResource(if (isFavorite) R.string.viewer_remove_favorite else R.string.viewer_add_favorite),
                                onToggleFavorite,
                                Modifier.semantics { selected = isFavorite },
                                tint = if (isFavorite) GalleryPalette.primary else GalleryPalette.onSurface,
                            )
                        }
                        if (onRestore == null && onTrash != null) {
                            GalleryIconButton(Icons.Filled.DeleteOutline, stringResource(R.string.trash_album), { onTrash(currentItem) })
                        }
                    }
                }
                GalleryIconButton(
                    Icons.Filled.Info, stringResource(R.string.selection_details), { onDetails(currentItem) },
                    Modifier.sampledGlass(CircleShape, tint, mediaFrame),
                )
            }
        }
        }
    }
    }
}

@Composable
private fun ViewerHeader(
    item: MediaItem,
    onDismiss: () -> Unit,
    frame: () -> GlassFrame?,
) {
    val colors = GalleryPalette
    val tint = colors.surfaceContainer.copy(alpha = GalleryDesign.viewerTintOpacity)
    Row(
        Modifier.fillMaxWidth()
            .height(GalleryDesign.dock)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GalleryIconButton(
            Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.viewer_close), onDismiss,
            Modifier.sampledGlass(CircleShape, tint, frame), size = GalleryDesign.button,
        )
        Column(
            Modifier.weight(1f, fill = false).sampledGlass(CircleShape, tint, frame)
                .padding(horizontal = GalleryDesign.inset, vertical = 4.dp),
        ) {
            val timestamp = item.dateMillis
            if (timestamp > 0L) {
                val date = remember(timestamp) { galleryDayLabel(timestamp) }
                val time = remember(timestamp) { DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(timestamp)) }
                GalleryText(date, style = GalleryTypography.titleSmall)
                GalleryText(time, style = GalleryTypography.labelSmall, color = colors.onSurfaceVariant)
            } else {
                GalleryText(item.displayName, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun PhotoFilmstrip(
    items: List<MediaItem>,
    page: Int,
    onPage: (Int) -> Unit,
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = page)
    LaunchedEffect(page) { listState.animateScrollToItem(page) }
    BoxWithConstraints(Modifier.fillMaxWidth().height(52.dp)) {
        val side = ((maxWidth - 44.dp) / 2).coerceAtLeast(0.dp)
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = side),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            itemsIndexed(items, key = { _, item -> item.key }) { index, item ->
                val shape = RoundedCornerShape(6.dp)
                val selected = page == index
                AsyncImage(
                    rememberMediaImageModel(item, thumbnail = true),
                    onSuccess = galleryImageReady(),
                    contentDescription = item.displayName,
                    modifier = Modifier.size(44.dp).clip(shape)
                        .then(if (selected) Modifier.border(2.dp, Color.White, shape) else Modifier)
                        .semantics { this.selected = selected }
                        .clickable(role = Role.Button) { onPage(index) },
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

private data class FrameBounds(val origin: Offset, val size: IntSize)

@Composable
private fun ViewerVideo(
    item: MediaItem, active: Boolean,
    actionBackdrop: MutableState<GlassFrame?>?,
    wideVideo: Boolean,
    controlsVisible: MutableState<Boolean>?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var foreground by remember(owner) { mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, _ ->
            foreground = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val playingPage = active && foreground
    var state by remember(item.key) { mutableStateOf(VideoPlaybackState(durationMs = item.durationMs)) }
    var texture by remember { mutableStateOf<TextureView?>(null) }
    var bounds by remember { mutableStateOf(FrameBounds(Offset.Zero, IntSize.Zero)) }
    var aspect by remember(item.key) { mutableFloatStateOf(item.aspectRatio) }
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var rendered by remember { mutableStateOf(false) }
    var scrubbing by remember { mutableStateOf(false) }
    var ended by remember { mutableStateOf(false) }
    var lastInteraction by remember { mutableLongStateOf(SystemClock.uptimeMillis()) }
    val pageControlsVisible = remember(item.key) { mutableStateOf(true) }
    val controlsState = controlsVisible ?: pageControlsVisible
    var showControls by controlsState
    val accessibility = LocalAccessibilityManager.current
    val timeout = accessibility?.calculateRecommendedTimeoutMillis(
        VideoControlsVisibility.VISIBLE_DURATION_MS,
        containsIcons = true, containsText = true, containsControls = true,
    ) ?: VideoControlsVisibility.VISIBLE_DURATION_MS
    LaunchedEffect(lastInteraction, timeout, scrubbing, playingPage, state.isPlaying) {
        showControls = true
        if (!playingPage) return@LaunchedEffect
        val hideAfter = VideoControlsVisibility.hideAfter(state.isPlaying, scrubbing, timeout)
            ?: return@LaunchedEffect
        delay(hideAfter)
        showControls = false
    }
    DisposableEffect(item.key, playingPage) {
        if (!playingPage) return@DisposableEffect onDispose {}
        val exo = ExoPlayer.Builder(context).build()
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() { rendered = true }
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.height > 0 && videoSize.width > 0) {
                    aspect = videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
                }
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    state = state.onPlaybackEnded()
                    ended = true
                    lastInteraction = SystemClock.uptimeMillis()
                }
            }
        }
        exo.addListener(listener)
        exo.setMediaItem(PlayerMediaItem.fromUri(item.contentUri), state.positionMs)
        exo.volume = if (state.isMuted) 0f else 1f
        exo.playWhenReady = state.isPlaying
        exo.prepare()
        player = exo
        onDispose {
            state = state.onPositionChange(exo.currentPosition)
            exo.removeListener(listener)
            exo.release()
            player = null
            rendered = false
        }
    }
    DisposableEffect(player, texture) {
        val exo = player
        val view = texture
        exo?.setVideoTextureView(view)
        onDispose { if (exo != null && player === exo) exo.clearVideoTextureView(view) }
    }
    LaunchedEffect(player, scrubbing) {
        val exo = player ?: return@LaunchedEffect
        while (!scrubbing) {
            if (exo.duration > 0) state = state.onDurationAvailable(exo.duration)
            state = state.onPositionChange(exo.currentPosition)
            delay(250)
        }
    }
    fun togglePlay() {
        if (ended) {
            player?.seekTo(0)
            state = state.onPositionChange(0)
            ended = false
        }
        state = state.togglePlay()
        player?.playWhenReady = state.isPlaying
        lastInteraction = SystemClock.uptimeMillis()
    }
    val frame = rememberBlurredVideoFrame(texture, aspect, playingPage && rendered && bounds.size != IntSize.Zero,
        state.isPlaying, state.positionMs)
    SideEffect {
        actionBackdrop?.value = frame?.let { GlassFrame(it, bounds.origin, bounds.size) }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(modifier.aspectRatio(aspect), contentAlignment = Alignment.Center) {
            if (playingPage) {
                AndroidView(
                    factory = { ctx -> TextureView(ctx).also { texture = it } },
                    modifier = Modifier.fillMaxSize().graphicsLayer { alpha = if (rendered) 1f else 0f }.onGloballyPositioned { bounds = FrameBounds(it.positionOnScreen(), it.size) },
                    onRelease = { texture = null },
                )
            }
            if (!rendered || !playingPage) {
                AsyncImage(rememberMediaImageModel(item), item.displayName, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
        }
        Box(Modifier.fillMaxSize().pointerInput(item.key, controlsState) {
            detectTapGestures {
                lastInteraction = SystemClock.uptimeMillis()
                showControls = true
            }
        })
        AnimatedVisibility(visible = showControls && active,
            enter = fadeIn(GalleryDesign.effects()),
            exit = fadeOut(GalleryDesign.effects())) {
            Box(Modifier.fillMaxSize()) {

                val colors = GalleryPalette
                val frameAtDraw = { actionBackdrop?.value }
                Row(
                    Modifier.align(if (wideVideo) Alignment.BottomStart else Alignment.BottomCenter)
                        .then(if (wideVideo) Modifier.padding(end = 240.dp) else Modifier)
                        .padding(horizontal = GalleryDesign.inset)
                        .padding(bottom = if (wideVideo) GalleryDesign.gap else GalleryDesign.target + GalleryDesign.separation)
                        .widthIn(max = 440.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GalleryDesign.gap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val playIcon = if (ended) Icons.Filled.Replay else if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow
                    val playLabel = if (ended) R.string.viewer_replay else if (state.isPlaying) R.string.viewer_pause else R.string.viewer_play
                    GalleryIconButton(
                        playIcon, stringResource(playLabel), ::togglePlay,
                        Modifier.sampledGlass(CircleShape, colors.surfaceContainer, frameAtDraw),
                    )
                    Row(
                        Modifier.weight(1f).sampledGlass(CircleShape, colors.surfaceContainer, frameAtDraw)
                            .padding(horizontal = GalleryDesign.separation),
                        horizontalArrangement = Arrangement.spacedBy(GalleryDesign.gap),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        GalleryText(formatDuration(state.positionMs), style = GalleryTypography.labelSmall)
                        VideoSeekBar(
                            fraction = if (state.durationMs > 0) state.positionMs.toFloat() / state.durationMs else 0f,
                            enabled = state.durationMs > 0,
                            modifier = Modifier.weight(1f),
                            onValueChange = {
                                scrubbing = true
                                ended = false
                                state = state.onPositionChange((it * state.durationMs).toLong())
                                lastInteraction = SystemClock.uptimeMillis()
                            },
                            onFinished = {
                                player?.seekTo(state.positionMs)
                                scrubbing = false
                                lastInteraction = SystemClock.uptimeMillis()
                            },
                        )
                        GalleryText(if (state.durationMs > 0) formatDuration(state.durationMs) else item.durationLabel,
                            style = GalleryTypography.labelSmall)
                    }
                    GalleryIconButton(
                        if (state.isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        stringResource(if (state.isMuted) R.string.viewer_unmute else R.string.viewer_mute),
                        {
                            state = state.toggleMute()
                            player?.volume = if (state.isMuted) 0f else 1f
                            lastInteraction = SystemClock.uptimeMillis()
                        },
                        Modifier.sampledGlass(CircleShape, colors.surfaceContainer, frameAtDraw),
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoSeekBar(
    fraction: Float,
    enabled: Boolean,
    modifier: Modifier,
    onValueChange: (Float) -> Unit,
    onFinished: () -> Unit,
) {
    val label = stringResource(R.string.viewer_seek)
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val change by rememberUpdatedState(onValueChange)
    val finish by rememberUpdatedState(onFinished)
    val value = fraction.coerceIn(0f, 1f)
    Canvas(
        modifier.height(48.dp)
            .semantics {
                contentDescription = label
                progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f)
                if (enabled) {
                    setProgress {
                        change(it.coerceIn(0f, 1f))
                        finish()
                        true
                    }
                }
            }
            .pointerInput(enabled, isRtl) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val edge = 6.dp.toPx()
                    val width = (size.width - 2 * edge).coerceAtLeast(1f)
                    fun seek(x: Float) {
                        val position = ((x - edge) / width).coerceIn(0f, 1f)
                        change(if (isRtl) 1f - position else position)
                    }
                    down.consume()
                    seek(down.position.x)
                    try {
                        do {
                            val pointer = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                            seek(pointer.position.x)
                            pointer.consume()
                        } while (pointer.pressed)
                    } finally {
                        finish()
                    }
                }
            },
    ) {
        val edge = 6.dp.toPx()
        val length = (size.width - edge * 2).coerceAtLeast(0f)
        val height = 2.dp.toPx()
        val y = center.y - height / 2
        val filled = length * value
        val x = if (isRtl) size.width - edge - filled else edge + filled
        drawRoundRect(
            Color.White.copy(alpha = 0.3f), Offset(edge, y), Size(length, height), CornerRadius(height / 2),
        )
        if (filled > 0f) {
            drawRoundRect(
                Color.White, Offset(if (isRtl) x else edge, y), Size(filled, height), CornerRadius(height / 2),
            )
        }
        if (enabled) drawCircle(Color.White, radius = 4.dp.toPx(), center = Offset(x, center.y))
    }
}

@Composable
private fun ViewerPhoto(
    item: MediaItem, onDismiss: () -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var zoom by remember(item.key) { mutableStateOf(ViewerZoomState()) }
    val backdrop = LocalGalleryGlass.current
    val dismiss by rememberUpdatedState(onDismiss)
    val toggleChrome by rememberUpdatedState(onTap)
    val dragY by animateFloatAsState(zoom.dismissY,
        animationSpec = if (zoom.dismissY == 0f) GalleryDesign.spatial() else snap(),
        label = "Photo drag")

    Box(
        modifier.fillMaxSize().clipToBounds()
            .pointerInput(item.key) {
                fun bounded(value: ViewerZoomState) = value.boundPan(size.width, size.height, item.aspectRatio)
                handlePhotoGestures({ zoom }, { zoom = bounded(it) }, { dismiss() })
            }
            .pointerInput(item.key) {
                detectTapGestures(onTap = { toggleChrome() }, onDoubleTap = { tap ->
                    val next = zoom.toggleDoubleTap()
                    val delta = Offset(size.width / 2f, size.height / 2f) - tap
                    zoom = if (next.scale > 1f) next.panBy(delta.x * (next.scale - 1), delta.y * (next.scale - 1))
                        .boundPan(size.width, size.height, item.aspectRatio) else next
                })
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(rememberMediaImageModel(item), item.displayName,
            onSuccess = galleryImageReady(),
            modifier = Modifier.graphicsLayer {
                scaleX = zoom.scale
                scaleY = zoom.scale
                translationX = zoom.offsetX
                translationY = zoom.offsetY + dragY
                backdrop?.invalidate()
            }.aspectRatio(item.aspectRatio),
            contentScale = ContentScale.Fit)
    }
}

private suspend fun PointerInputScope.handlePhotoGestures(
    zoom: () -> ViewerZoomState, onZoomChange: (ViewerZoomState) -> Unit, onDismiss: () -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var claimed = false
        var dismissing = false
        var pinched = false
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val active = event.changes.filter { it.pressed }
            if (active.isEmpty()) break
            val current = zoom()
            if (active.size >= 2) {
                pinched = true
                claimed = true
                dismissing = false
                val previous = event.calculateCentroid(useCurrent = false)
                if (previous != Offset.Unspecified) {
                    val next = current.zoomBy(event.calculateZoom()).copy(dismissY = 0f)
                    val factor = next.scale / current.scale
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val previousOffset = Offset(current.offsetX, current.offsetY)
                    val offset = previousOffset * factor + (previous - center) * (1f - factor) + event.calculatePan()
                    onZoomChange(next.copy(offsetX = offset.x, offsetY = offset.y))
                }
            } else {
                val change = active.first()
                val travel = change.position - down.position
                if (!claimed && travel.getDistance() >= viewConfiguration.touchSlop) {
                    dismissing = current.scale == 1f && travel.y > abs(travel.x)
                    claimed = dismissing || current.scale > 1f
                    if (!claimed) break
                }
                if (claimed && (dismissing || current.scale > 1f)) {
                    val delta = change.positionChange()
                    onZoomChange(current.panBy(delta.x, delta.y))
                }
            }
            if (claimed) event.changes.forEach { it.consume() }
        }
        if (!pinched && dismissing && zoom().dismissY >= 92.dp.toPx()) onDismiss()
        else if (zoom().dismissY != 0f) onZoomChange(zoom().copy(dismissY = 0f))
    }
}
