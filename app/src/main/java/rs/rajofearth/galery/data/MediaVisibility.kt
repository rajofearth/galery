package rs.rajofearth.galery.data

import java.io.File

object MediaVisibility {
    fun isVisible(
        displayName: String,
        relativePath: String,
        nomediaFolders: Set<String>,
    ): Boolean {
        if (displayName.startsWith(".")) return false

        val path = relativePath.trim('/')
        if (path.split('/').any { it.startsWith(".") }) return false
        return nomediaFolders.none { folder ->
            val normalized = folder.trim('/')
            normalized.isEmpty() || path == normalized || path.startsWith("$normalized/")
        }
    }

    internal fun hasNomediaAncestor(directory: File, cache: MutableMap<File, Boolean>): Boolean =
        cache.getOrPut(directory) {
            File(directory, ".nomedia").isFile ||
                directory.parentFile?.let { hasNomediaAncestor(it, cache) } == true
        }
}
