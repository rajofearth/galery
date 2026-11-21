package rs.rajofearth.galery.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil3.request.ImageRequest
import coil3.video.VideoFrameDecoder
import rs.rajofearth.galery.data.MediaItem

@Composable
fun rememberMediaImageModel(item: MediaItem): Any {
    val context = LocalContext.current
    return remember(item.key) {
        if (item.isVideo) {
            ImageRequest.Builder(context)
                .data(item.contentUri)
                .decoderFactory { result, options, _ -> VideoFrameDecoder(result.source, options) }
                .build()
        } else {
            item.contentUri
        }
    }
}
