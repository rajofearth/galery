package rs.rajofearth.galery.ui

import android.os.Build
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.channels.Channel
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private const val SAMPLE_EDGE_PX = 96
private const val SAMPLE_INTERVAL_MS = 96L
private const val SAMPLE_BLUR_RADIUS = 3

internal class GlassBackdrop(internal val source: GraphicsLayer) {
    internal var sourceOrigin = Offset.Unspecified
    internal val updates = Channel<Unit>(Channel.CONFLATED)

    fun invalidate() { updates.trySend(Unit) }
    internal var frame by mutableStateOf<GlassFrame?>(null)
    internal val scrollObserver = object : NestedScrollConnection {
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (consumed != Offset.Zero) invalidate()
            return Offset.Zero
        }
    }
}

internal data class GlassFrame(val bitmap: ImageBitmap, val origin: Offset, val size: IntSize)

@Composable
internal fun rememberGlassBackdrop(enabled: Boolean = true): GlassBackdrop {
    val source = rememberGraphicsLayer()
    val sample = rememberGraphicsLayer()
    val backdrop = remember(source) { GlassBackdrop(source) }
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    LaunchedEffect(backdrop, sample, enabled, density, direction) {
        if (!enabled) return@LaunchedEffect
        backdrop.invalidate()
        for (update in backdrop.updates) {
            withFrameNanos {}
            val size = source.size
            val origin = backdrop.sourceOrigin
            if (size.width > 0 && size.height > 0 && origin != Offset.Unspecified) {
                val scale = minOf(1f, SAMPLE_EDGE_PX.toFloat() / maxOf(size.width, size.height))
                val small = IntSize(
                    (size.width * scale).roundToInt().coerceAtLeast(1),
                    (size.height * scale).roundToInt().coerceAtLeast(1),
                )
                sample.record(density, direction, small) {
                    drawRect(Color.Black)
                    scale(small.width.toFloat() / size.width, small.height.toFloat() / size.height, Offset.Zero) {
                        drawLayer(source)
                    }
                }
                val blurred = try {
                    val capture = sample.toImageBitmap().asAndroidBitmap()
                    withContext(Dispatchers.Default) {
                        val pixels = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                            capture.config == Bitmap.Config.HARDWARE
                        ) capture.copy(Bitmap.Config.ARGB_8888, false) else capture
                        pixels?.let { GaussianBlur.blur(it, SAMPLE_BLUR_RADIUS).asImageBitmap() }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: RuntimeException) {
                    Log.w("GaleryGlass", "Could not capture backdrop", error)
                    backdrop.frame = null
                    return@LaunchedEffect
                }
                backdrop.frame = blurred?.let { GlassFrame(it, origin, size) }
            }
            delay(SAMPLE_INTERVAL_MS)
        }
    }
    return backdrop
}

internal fun Modifier.glassBackdropSource(backdrop: GlassBackdrop): Modifier =
    nestedScroll(backdrop.scrollObserver)
        .graphicsLayer()
        .onGloballyPositioned {
            val nextOrigin = it.positionOnScreen()
            if (backdrop.sourceOrigin != nextOrigin) {
                backdrop.sourceOrigin = nextOrigin
                backdrop.invalidate()
            }
        }
        .drawWithContent {
            backdrop.source.record { this@drawWithContent.drawContent() }
            drawLayer(backdrop.source)
            backdrop.invalidate()
        }

@Composable
internal fun Modifier.glassBackdrop(
    backdrop: GlassBackdrop,
    shape: Shape,
    tint: Color,
): Modifier {
    return sampledGlass(shape, tint) { backdrop.frame }
}

@Composable
internal fun Modifier.sampledGlass(shape: Shape, tint: Color, frameAtDraw: () -> GlassFrame?): Modifier {
    var origin by remember { mutableStateOf(Offset.Unspecified) }
    return clip(shape)
        .onGloballyPositioned { origin = it.positionOnScreen() }
        .drawWithContent {
            val frame = frameAtDraw()
            drawRect(tint)
            if (frame != null && frame.origin != Offset.Unspecified && origin != Offset.Unspecified) {
                val offset = frame.origin - origin
                drawImage(
                    image = frame.bitmap,
                    dstOffset = IntOffset(offset.x.roundToInt(), offset.y.roundToInt()),
                    dstSize = frame.size,
                    filterQuality = FilterQuality.Low,
                    alpha = 1f - GalleryDesign.glassTint,
                )
            }
            drawContent()
        }
}
