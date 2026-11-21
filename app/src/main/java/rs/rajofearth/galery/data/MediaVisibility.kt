package rs.rajofearth.galery.data

object MediaVisibility {
    fun isVisible(
        displayName: String,
        relativePath: String,
        nomediaFolders: Set<String>,
    ): Boolean {
        if (displayName.startsWith(".")) return false

        val path = relativePath.trim('/')
        return nomediaFolders.none { folder ->
            val normalized = folder.trim('/')
            path == normalized || path.startsWith("$normalized/")
        }
    }
}
