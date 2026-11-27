package rs.rajofearth.galery.data

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore.MediaColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

internal data class AlbumFolder(val volume: String, val path: String) {
    val name: String get() = path.substringAfterLast('/')
    val key: String get() = "$volume/$path"

    fun accepts(items: List<MediaItem>): Boolean =
        items.all { it.volumeName == volume } && when (path.substringBefore('/')) {
            "DCIM", "Pictures" -> true
            "Movies" -> items.all { it.isVideo }
            else -> false
        }
}

internal val MediaAlbum.folder: AlbumFolder
    get() = items.first().let { AlbumFolder(it.volumeName, it.relativePath.trim('/')) }

internal val AlbumFolder.canRename: Boolean get() = '/' in path

internal fun albumName(input: String): String? =
    input.trim().takeIf { name ->
        name.isNotEmpty() && !name.startsWith('.') && !name.endsWith('.') &&
            name.toByteArray().size <= 255 &&
            name.none { it.isISOControl() || it in "/\\:*?\"<>|" }
    }

private const val MAX_WRITE_REQUEST = 2_000

internal data class AlbumMoveResult(val moved: Int, val cancelled: Boolean)

internal suspend fun moveAlbumMedia(
    resolver: ContentResolver,
    items: List<MediaItem>,
    folder: AlbumFolder,
    requestWrite: suspend (List<Uri>) -> Boolean,
): AlbumMoveResult {
    require(folder.accepts(items))
    var moved = 0
    for (batch in items.chunked(MAX_WRITE_REQUEST)) {
        coroutineContext.ensureActive()
        val uris = batch.map { it.contentUri }
        val direct = writeAlbumPaths(resolver, uris, folder.path)
        moved += direct.moved
        if (direct.needsPermission.isNotEmpty()) {
            if (!requestWrite(direct.needsPermission)) return AlbumMoveResult(moved, cancelled = true)
            moved += writeAlbumPaths(resolver, direct.needsPermission, folder.path).moved
        }
    }
    return AlbumMoveResult(moved, cancelled = false)
}

private data class AlbumPathWrites(val moved: Int, val needsPermission: List<Uri>)

private suspend fun writeAlbumPaths(
    resolver: ContentResolver,
    uris: List<Uri>,
    path: String,
): AlbumPathWrites = withContext(Dispatchers.IO) {
    val values = ContentValues(1).apply { put(MediaColumns.RELATIVE_PATH, "$path/") }
    val needsPermission = ArrayList<Uri>()
    var moved = 0
    for (uri in uris) {
        coroutineContext.ensureActive()
        try {
            if (resolver.update(uri, values, null, null) == 1) moved++
        } catch (_: SecurityException) {
            needsPermission.add(uri)
        } catch (_: RuntimeException) {
            // A missing item or unwritable destination must not stop the remaining moves.
        }
    }
    AlbumPathWrites(moved, needsPermission)
}
