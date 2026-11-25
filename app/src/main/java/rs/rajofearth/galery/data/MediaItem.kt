package rs.rajofearth.galery.data

import android.net.Uri
import androidx.core.net.toUri

data class MediaItem(
    val id: Long,
    val displayName: String,
    val isVideo: Boolean,
    val dateAdded: Long,
    val width: Int = 0,
    val height: Int = 0,
    val durationMs: Long = 0L,
    val dateTaken: Long = 0L,
    val sizeBytes: Long = -1L,
    val relativePath: String = "",
    val filePath: String? = null,
    val mimeType: String? = null,
    val volumeName: String = "external",
) {
    val aspectRatio: Float
        get() = if (width > 0 && height > 0) width.toFloat() / height else 1f

    val hasDurationBadge: Boolean
        get() = isVideo

    val durationLabel: String
        get() = if (durationMs > 0L) formatDuration(durationMs) else UNKNOWN_DURATION

    val key: String
        get() = "${mediaCollectionUri(volumeName, isVideo)}/$id"

    val contentUri: Uri
        get() = key.toUri()

    val dateMillis: Long
        get() = if (dateTaken > 0L) dateTaken else dateAdded * 1_000L

    val albumKey: String
        get() = "$volumeName/${relativePath.trim('/')}"

    val albumName: String
        get() = relativePath.trim('/').substringAfterLast('/')

    companion object {
        const val UNKNOWN_DURATION = "--:--"
    }
}

internal fun mediaCollectionUri(volumeName: String, isVideo: Boolean): String =
    "content://media/$volumeName/${if (isVideo) "video" else "images"}/media"
