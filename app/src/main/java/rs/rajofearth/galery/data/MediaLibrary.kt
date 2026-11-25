package rs.rajofearth.galery.data

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

enum class MediaLayout { Mosaic, Grid }

enum class MediaSortField { Date, Name, Size }

data class MediaSort(
    val field: MediaSortField = MediaSortField.Date,
    val descending: Boolean = true,
)

data class MediaAlbum(
    val key: String,
    val name: String,
    val items: List<MediaItem>,
)

fun List<MediaItem>.toAlbums(): List<MediaAlbum> =
    groupBy { it.albumKey }.map { (key, items) ->
        MediaAlbum(key, items.first().albumName, items)
    }

fun List<MediaItem>.sortedBy(sort: MediaSort): List<MediaItem> {
    val primary = when (sort.field) {
        MediaSortField.Date -> compareBy<MediaItem> { it.dateMillis }
        MediaSortField.Name -> compareBy<MediaItem, String>(String.CASE_INSENSITIVE_ORDER) { it.displayName }
        MediaSortField.Size -> compareBy<MediaItem> { it.sizeBytes }
    }
    val comparator = if (sort.descending) primary.reversed() else primary
    return sortedWith(comparator.thenBy { it.key })
}

fun List<MediaItem>.filteredBy(query: String): List<MediaItem> {
    val term = query.trim()
    if (term.isEmpty()) return this

    val locale = Locale.getDefault()
    val timeZone = TimeZone.getDefault()
    val dateFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd", Locale.ROOT),
        DateFormat.getDateInstance(DateFormat.SHORT, locale),
        DateFormat.getDateInstance(DateFormat.MEDIUM, locale),
        DateFormat.getDateInstance(DateFormat.LONG, locale),
    ).onEach { it.timeZone = timeZone }
    val matchingDates = mutableMapOf<Long, Boolean>()
    return filter { item ->
        item.displayName.contains(term, ignoreCase = true) ||
            item.albumName.contains(term, ignoreCase = true) ||
            item.dateMillis.takeIf { it > 0L }?.let { millis ->
                val localDay = Math.floorDiv(millis + timeZone.getOffset(millis), TimeUnit.DAYS.toMillis(1))
                matchingDates.getOrPut(localDay) {
                    val date = Date(millis)
                    dateFormats.any { it.format(date).contains(term, ignoreCase = true) }
                }
            } == true
    }
}
