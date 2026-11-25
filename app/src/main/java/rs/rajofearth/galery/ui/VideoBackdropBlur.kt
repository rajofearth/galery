package rs.rajofearth.galery.ui

import android.graphics.Bitmap
import androidx.core.graphics.createBitmap

import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlin.math.exp

private const val BLUR_FRAME_WIDTH = 80
private const val BLUR_FRAME_INTERVAL_MS = 120L
private const val BLUR_RADIUS = 4

object GaussianBlur {
    fun blur(source: Bitmap, radius: Int): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        val horizontal = IntArray(pixels.size)
        val output = IntArray(pixels.size)
        blurPass(pixels, horizontal, width, height, radius, horizontal = true)
        blurPass(horizontal, output, width, height, radius, horizontal = false)

        return createBitmap(width, height).apply {
            setPixels(output, 0, width, 0, 0, width, height)
        }
    }

    private fun blurPass(
        source: IntArray,
        destination: IntArray,
        width: Int,
        height: Int,
        radius: Int,
        horizontal: Boolean,
    ) {
        val kernel = FloatArray(radius * 2 + 1)
        val sigma = (radius / 2f).coerceAtLeast(0.5f)
        var total = 0f
        for (offset in -radius..radius) {
            val weight = exp(-(offset * offset) / (2f * sigma * sigma))
            kernel[offset + radius] = weight
            total += weight
        }
        for (index in kernel.indices) kernel[index] /= total

        for (y in 0 until height) {
            for (x in 0 until width) {
                val center = if (horizontal) x else y
                val limit = if (horizontal) width else height
                var red = 0f
                var green = 0f
                var blue = 0f
                for (offset in -radius..radius) {
                    val sample = (center + offset).coerceIn(0, limit - 1)
                    val index = if (horizontal) y * width + sample else sample * width + x
                    val pixel = source[index]
                    val weight = kernel[offset + radius]
                    red += ((pixel shr 16) and 0xFF) * weight
                    green += ((pixel shr 8) and 0xFF) * weight
                    blue += (pixel and 0xFF) * weight
                }
                destination[y * width + x] =
                    (0xFF000000.toInt()) or
                        (red.toInt() shl 16) or
                        (green.toInt() shl 8) or
                        blue.toInt()
            }
        }
    }
}

@Composable
fun rememberBlurredVideoFrame(
    textureView: TextureView?,
    aspectRatio: Float,
    enabled: Boolean,
    playing: Boolean,
    positionMs: Long,
): ImageBitmap? {
    var frame by remember(textureView) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(textureView, enabled, aspectRatio, playing, if (playing) 0L else positionMs) {
        if (!enabled || textureView == null) return@LaunchedEffect
        val ratio = aspectRatio.coerceIn(0.05f, 20f)
        val width = (BLUR_FRAME_WIDTH * minOf(1f, ratio)).toInt().coerceAtLeast(1)
        val height = (BLUR_FRAME_WIDTH / maxOf(1f, ratio)).toInt().coerceAtLeast(1)
        val bitmap = createBitmap(width, height)
        while (true) {
            if (textureView.isAvailable) {
                textureView.getBitmap(bitmap)
                frame = withContext(Dispatchers.Default) {
                    GaussianBlur.blur(bitmap, BLUR_RADIUS).asImageBitmap()
                }
                if (!playing) break
            }
            delay(BLUR_FRAME_INTERVAL_MS)
        }
    }
    return frame
}
