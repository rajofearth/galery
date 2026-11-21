package rs.rajofearth.galery.data

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore

data class MediaItem(
    val id: Long,
    val displayName: String,
    val isVideo: Boolean,
    val dateAdded: Long,
    val width: Int = 0,
    val height: Int = 0,
    val durationMs: Long = 0L,
) {
    val aspectRatio: Float
        get() = if (width > 0 && height > 0) width.toFloat() / height else 1f

    val hasDurationBadge: Boolean
        get() = isVideo

    val durationLabel: String
        get() = if (durationMs > 0L) formatDuration(durationMs) else UNKNOWN_DURATION

    val key: String
        get() = "${if (isVideo) "v" else "i"}-$id"

    val contentUri: Uri
        get() = ContentUris.withAppendedId(
            if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            else MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            id,
        )

    companion object {
        const val UNKNOWN_DURATION = "--:--"
    }
}
