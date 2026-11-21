package rs.rajofearth.galery.ui

import rs.rajofearth.galery.data.MediaItem

data class MosaicTile(val item: MediaItem, val clampedAspect: Float)

object MosaicLayout {
    const val TARGET_ROW_ASPECT = 2.2f
    const val MIN_ASPECT = 0.5f
    const val MAX_ASPECT = 2f
    const val MAX_PER_ROW = 4

    fun rows(items: List<MediaItem>): List<List<MosaicTile>> {
        val rows = mutableListOf<List<MosaicTile>>()
        var current = mutableListOf<MosaicTile>()
        var usedAspect = 0f

        for (item in items) {
            val weight = item.aspectRatio.coerceIn(MIN_ASPECT, MAX_ASPECT)
            if (current.isNotEmpty() && (current.size >= MAX_PER_ROW || usedAspect + weight > TARGET_ROW_ASPECT)) {
                rows += current
                current = mutableListOf()
                usedAspect = 0f
            }
            current += MosaicTile(item, weight)
            usedAspect += weight
        }

        if (current.isNotEmpty()) rows += current
        return rows
    }
}
