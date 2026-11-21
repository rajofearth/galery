package rs.rajofearth.galery.data

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaStoreMediaRepository(private val context: Context) {
    suspend fun loadMedia(): List<MediaItem> = withContext(Dispatchers.IO) {
        val candidates = query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, isVideo = false) +
            query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, isVideo = true)

        val nomediaFolders = candidates
            .map { it.folder }
            .toSet()
            .filter { hasNomediaFile(it) }
            .toSet()

        candidates
            .filter { MediaVisibility.isVisible(it.item.displayName, it.folder, nomediaFolders) }
            .map { it.item }
            .sortedByDescending { it.dateAdded }
    }

    private data class Candidate(val item: MediaItem, val folder: String)

    private val usesRelativePath: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    private fun query(contentUri: Uri, isVideo: Boolean): List<Candidate> {
        val pathColumn = pathColumn()
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_ADDED,
            pathColumn,
        )
        val candidates = mutableListOf<Candidate>()
        context.contentResolver.query(
            contentUri,
            projection,
            null,
            null,
            "${MediaStore.MediaColumns.DATE_ADDED} DESC",
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val dateIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val pathIndex = cursor.getColumnIndexOrThrow(pathColumn)

            while (cursor.moveToNext()) {
                val folder = folderOf(cursor.getString(pathIndex).orEmpty())
                candidates += Candidate(
                    item = MediaItem(
                        id = cursor.getLong(idIndex),
                        displayName = cursor.getString(nameIndex).orEmpty(),
                        isVideo = isVideo,
                        dateAdded = cursor.getLong(dateIndex),
                    ),
                    folder = folder,
                )
            }
        }
        return candidates
    }

    private fun hasNomediaFile(folder: String): Boolean {
        val directory = if (usesRelativePath) {
            File(Environment.getExternalStorageDirectory(), folder)
        } else {
            File(folder)
        }
        return File(directory, ".nomedia").exists()
    }

    private fun pathColumn(): String =
        if (usesRelativePath) {
            MediaStore.MediaColumns.RELATIVE_PATH
        } else {
            MediaStore.MediaColumns.DATA
        }

    private fun folderOf(path: String): String =
        if (usesRelativePath) {
            path
        } else {
            path.substringBeforeLast('/', "") + "/"
        }
}
