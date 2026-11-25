package rs.rajofearth.galery.data

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.MediaStore
import android.provider.MediaStore.MediaColumns
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.coroutines.coroutineContext

class MediaStoreMediaRepository(private val context: Context) {
    suspend fun loadMedia(trashed: Boolean = false): List<MediaItem> = withContext(Dispatchers.IO) {
        if (trashed && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return@withContext emptyList()
        val nomediaByDirectory = mutableMapOf<File, Boolean>()
        val volumes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.getExternalVolumeNames(context)
        } else {
            setOf("external")
        }
        buildList {
            for (volume in volumes) {
                val volumeDirectory = volumeDirectory(volume)
                for (isVideo in listOf(false, true)) {
                    val collection = mediaCollectionUri(volume, isVideo).toUri()
                    readCollection(context.contentResolver, collection, volume, volumeDirectory, isVideo, trashed) { item ->
                        if (trashed || MediaVisibility.isVisible(item.displayName, item.relativePath, emptySet()) &&
                            !hasNomediaAncestor(item, volumeDirectory, nomediaByDirectory)
                        ) {
                            add(item)
                        }
                    }
                }
            }
        }.sortedBy(MediaSort())
    }

    private fun volumeDirectory(volume: String): File? {
        if (volume == "external" || volume == "external_primary") {
            return Environment.getExternalStorageDirectory()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return context.getSystemService(StorageManager::class.java)
                ?.storageVolumes
                ?.firstOrNull { it.mediaStoreVolumeName == volume }
                ?.directory
        }
        return null
    }
}

private suspend fun readCollection(
    resolver: ContentResolver,
    collection: Uri,
    volumeName: String,
    volumeDirectory: File?,
    isVideo: Boolean,
    trashed: Boolean,
    onItem: (MediaItem) -> Unit,
) {
    val durationColumn = MediaStore.Video.Media.DURATION
    val usesRelativePath = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    val projection = buildList {
        add(MediaColumns._ID)
        add(MediaColumns.DISPLAY_NAME)
        add(MediaColumns.DATE_ADDED)
        add(MediaStore.Images.ImageColumns.DATE_TAKEN)
        add(MediaColumns.SIZE)
        add(MediaColumns.MIME_TYPE)
        add(MediaColumns.WIDTH)
        add(MediaColumns.HEIGHT)
        add(MediaColumns.DATA)
        if (usesRelativePath) add(MediaColumns.RELATIVE_PATH)
        if (isVideo) add(durationColumn)
    }.toTypedArray()

    val cursor = if (trashed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val arguments = android.os.Bundle().apply {
            putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
        }
        resolver.query(collection, projection, arguments, null)
    } else {
        resolver.query(collection, projection, null, null, null)
    }
    val rows = cursor ?: throw IOException("MediaStore could not read $collection")
    rows.use { cursor ->
        val columns = projection.associateWith(cursor::getColumnIndex)
        val idIndex = cursor.getColumnIndexOrThrow(MediaColumns._ID)
        val nameIndex = cursor.getColumnIndexOrThrow(MediaColumns.DISPLAY_NAME)
        fun index(column: String) = columns[column] ?: -1

        while (cursor.moveToNext()) {
            coroutineContext.ensureActive()
            val filePath = cursor.text(index(MediaColumns.DATA))
            val relativePath = if (usesRelativePath) cursor.text(index(MediaColumns.RELATIVE_PATH)) else null
            onItem(
                MediaItem(
                    id = cursor.getLong(idIndex),
                    displayName = cursor.getString(nameIndex).orEmpty(),
                    isVideo = isVideo,
                    dateAdded = cursor.number(index(MediaColumns.DATE_ADDED)),
                    width = cursor.number(index(MediaColumns.WIDTH)).toInt(),
                    height = cursor.number(index(MediaColumns.HEIGHT)).toInt(),
                    durationMs = cursor.number(index(durationColumn)),
                    dateTaken = cursor.number(index(MediaStore.Images.ImageColumns.DATE_TAKEN)),
                    sizeBytes = cursor.number(index(MediaColumns.SIZE), default = -1L),
                    relativePath = relativePath ?: folderPath(filePath, volumeDirectory),
                    filePath = filePath,
                    mimeType = cursor.text(index(MediaColumns.MIME_TYPE)),
                    volumeName = volumeName,
                ),
            )
        }
    }
}

private fun Cursor.number(index: Int, default: Long = 0L): Long =
    if (index >= 0 && !isNull(index)) getLong(index) else default

private fun Cursor.text(index: Int): String? =
    if (index >= 0) getString(index) else null

private fun folderPath(filePath: String?, volumeDirectory: File?): String {
    val parent = filePath?.let { File(it).parentFile } ?: return ""
    return if (volumeDirectory != null && parent.startsWith(volumeDirectory)) {
        parent.relativeTo(volumeDirectory).path
    } else {
        parent.path
    }
}

private fun hasNomediaAncestor(
    item: MediaItem,
    volumeDirectory: File?,
    cache: MutableMap<File, Boolean>,
): Boolean {
    val directory = item.filePath?.let { File(it).parentFile }
        ?: volumeDirectory?.let { File(it, item.relativePath.trim('/')) }
    return directory?.let { MediaVisibility.hasNomediaAncestor(it, cache) } ?: false
}
