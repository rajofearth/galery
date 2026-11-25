package rs.rajofearth.galery.ui

import android.os.Build
import android.os.CancellationSignal
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil3.Uri
import coil3.asImage
import coil3.request.ImageRequest
import coil3.compose.AsyncImagePainter
import coil3.decode.DataSource
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.size.Dimension
import coil3.size.Precision
import coil3.toAndroidUri
import coil3.video.VideoFrameDecoder
import java.io.IOException
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import rs.rajofearth.galery.data.MediaItem

@Composable
internal fun galleryImageReady(): ((AsyncImagePainter.State.Success) -> Unit)? {
    val backdrop = LocalGalleryGlass.current ?: return null
    return remember(backdrop) { { backdrop.invalidate() } }
}

@Composable
fun rememberMediaImageModel(item: MediaItem, thumbnail: Boolean = false): Any {
    val context = LocalContext.current
    return remember(context, item.key, thumbnail) {
        if (item.isVideo || thumbnail) {
            ImageRequest.Builder(context)
                .data(item.contentUri)
                .apply {
                    if (thumbnail) {
                        precision(Precision.INEXACT)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            fetcherFactory(MediaStoreThumbnailFetcher)
                        }
                    }
                    if (item.isVideo) {
                        decoderFactory { result, options, _ -> VideoFrameDecoder(result.source, options) }
                    }
                }
                .build()
        } else {
            item.contentUri
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
private val MediaStoreThumbnailFetcher = Fetcher.Factory<Uri> { uri, options, _ ->
    val width = (options.size.width as? Dimension.Pixels)?.px ?: return@Factory null
    val height = (options.size.height as? Dimension.Pixels)?.px ?: return@Factory null
    val resolver = options.context.contentResolver
    Fetcher {
        suspendCancellableCoroutine { continuation ->
            val signal = CancellationSignal()
            continuation.invokeOnCancellation { signal.cancel() }
            val bitmap = if (continuation.isActive) {
                try {
                    resolver.loadThumbnail(uri.toAndroidUri(), android.util.Size(width, height), signal)
                } catch (_: IOException) {
                    null
                } catch (_: SecurityException) {
                    null
                }
            } else null
            continuation.resume(bitmap?.let { ImageFetchResult(it.asImage(), true, DataSource.DISK) })
        }
    }
}
