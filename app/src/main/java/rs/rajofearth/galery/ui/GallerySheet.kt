package rs.rajofearth.galery.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import rs.rajofearth.galery.ui.theme.GalleryPalette
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import kotlin.math.roundToInt

@Composable
internal fun GallerySheet(
    onDismissRequest: () -> Unit,
    frameAtDraw: (() -> GlassFrame?)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val onDismiss by rememberUpdatedState(onDismissRequest)
    val visibility = remember { MutableTransitionState(false).apply { targetState = true } }
    val transition = rememberTransition(visibility, label = "Gallery sheet")
    var closing by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val offset by animateFloatAsState(
        targetValue = dragOffset,
        animationSpec = if (dragging || closing) snap() else GalleryDesign.spatial(),
        label = "Sheet drag",
    )
    val density = LocalDensity.current
    val dragThreshold = with(density) { 56.dp.toPx() }
    val flingThreshold = with(density) { 750.dp.toPx() }
    val sheetTint = GalleryPalette.surfaceContainerLow
    val requestDismiss = {
        closing = true
        visibility.targetState = false
    }
    LaunchedEffect(closing, visibility.isIdle, visibility.currentState) {
        if (closing && visibility.isIdle && !visibility.currentState) onDismiss()
    }

    Dialog(
        onDismissRequest = requestDismiss,
        properties = DialogProperties(
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.setDimAmount(0f) }
        Box(Modifier.fillMaxSize()) {
            transition.AnimatedVisibility(
                visible = { it },
                enter = fadeIn(GalleryDesign.spatial()),
                exit = fadeOut(GalleryDesign.effects()),
            ) {
                Box(
                    Modifier.fillMaxSize()
                        .background(Color.Black.copy(alpha = if (frameAtDraw == null) 0.64f else 0.28f))
                        .clearAndSetSemantics {}
                        .clickable(interactionSource = null, indication = null, onClick = requestDismiss),
                )
            }
            transition.AnimatedVisibility(
                visible = { it },
                modifier = Modifier.align(Alignment.BottomCenter).statusBarsPadding(),
                enter = slideInVertically(GalleryDesign.spatial()) { it },
                exit = slideOutVertically(GalleryDesign.effects()) { it },
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth()
                        .offset { IntOffset(0, offset.roundToInt()) }
                        .then(
                            if (frameAtDraw == null) Modifier.clip(GalleryDesign.sheet).background(sheetTint)
                            else Modifier.sampledGlass(GalleryDesign.sheet, sheetTint.copy(alpha = 0.78f), frameAtDraw),
                        )
                        .pointerInput(Unit) {
                            // Keep taps on the sheet from reaching the scrim behind it.
                            awaitPointerEventScope {
                                while (true) awaitPointerEvent()
                            }
                        }
                        .semantics {
                            dismiss {
                                requestDismiss()
                                true
                            }
                        }
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                            .draggable(
                                state = rememberDraggableState {
                                    dragOffset = (dragOffset + it).coerceAtLeast(0f)
                                },
                                orientation = Orientation.Vertical,
                                enabled = !closing,
                                onDragStarted = { dragging = true },
                                onDragStopped = { velocity ->
                                    dragging = false
                                    if (dragOffset > dragThreshold || velocity > flingThreshold) {
                                        requestDismiss()
                                    } else {
                                        dragOffset = 0f
                                    }
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        MediaSheetHandle()
                    }
                    content()
                }
            }
        }
    }
}

@Composable
internal fun MediaSheetHandle() {
    Box(
        Modifier.size(32.dp, 4.dp)
            .background(GalleryPalette.onSurfaceVariant.copy(alpha = 0.48f), CircleShape),
    )
}
