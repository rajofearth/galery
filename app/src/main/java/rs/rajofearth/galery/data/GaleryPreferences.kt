package rs.rajofearth.galery.data

import android.content.Context
import androidx.core.content.edit

class GaleryPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("galery", Context.MODE_PRIVATE)

    var layout: MediaLayout
        get() = MediaLayout.entries.firstOrNull { it.name == preferences.getString(LAYOUT, null) }
            ?: MediaLayout.Mosaic
        set(value) {
            preferences.edit { putString(LAYOUT, value.name) }
        }

    var sort: MediaSort
        get() {
            val fieldName = preferences.getString(SORT_FIELD, null)
            val field = MediaSortField.entries.firstOrNull { it.name == fieldName } ?: MediaSortField.Date
            return MediaSort(field, preferences.getBoolean(SORT_DESCENDING, true))
        }
        set(value) {
            preferences.edit {
                putString(SORT_FIELD, value.field.name)
                putBoolean(SORT_DESCENDING, value.descending)
            }
        }

    val favoriteKeys: Set<String>
        get() = preferences.getStringSet(FAVORITES, null).orEmpty().toSet()

    fun toggleFavorite(key: String): Set<String> = toggleFavorites(setOf(key))

    fun toggleFavorites(keys: Set<String>): Set<String> = synchronized(preferences) {
        val current = favoriteKeys
        val updated = if (current.containsAll(keys)) current - keys else current + keys
        if (updated != current) {
            preferences.edit { putStringSet(FAVORITES, updated) }
        }
        updated
    }

    fun pruneFavorites(items: List<MediaItem>): Set<String> = synchronized(preferences) {
        val current = favoriteKeys
        if (current.isEmpty()) return@synchronized current
        val remaining = items.mapNotNullTo(mutableSetOf()) { it.key.takeIf(current::contains) }
        if (remaining != current) {
            preferences.edit { putStringSet(FAVORITES, remaining) }
        }
        remaining
    }

    private companion object {
        const val LAYOUT = "media_layout"
        const val SORT_FIELD = "media_sort_field"
        const val SORT_DESCENDING = "media_sort_descending"
        const val FAVORITES = "favorite_keys"
    }
}
