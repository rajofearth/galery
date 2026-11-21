package rs.rajofearth.galery.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import rs.rajofearth.galery.data.MediaItem

class MosaicLayoutTest {
    @Test
    fun `an empty All media view has no rows`() {
        assertTrue(MosaicLayout.rows(emptyList()).isEmpty())
    }

    @Test
    fun `one media item forms one row`() {
        val rows = MosaicLayout.rows(listOf(mediaItem(1, 1.5f)))

        assertEquals(1, rows.size)
        assertEquals(listOf(1L), rows.single().map { it.item.id })
    }

    @Test
    fun `tiles keep media items in sort order`() {
        val rows = MosaicLayout.rows(List(6) { index -> mediaItem(index.toLong() + 1, 1f) })

        assertEquals((1L..6L).toList(), rows.flatten().map { it.item.id })
    }

    @Test
    fun `a tile keeps its item's aspect ratio inside the row`() {
        val row = MosaicLayout.rows(listOf(mediaItem(1, 1.5f))).single()

        assertEquals(1.5f, row.single().clampedAspect, 0.0001f)
    }

    @Test
    fun `extreme aspect ratios are clamped`() {
        val tiles = MosaicLayout.rows(listOf(mediaItem(1, 6f), mediaItem(2, 0.1f))).flatten()

        assertEquals(MosaicLayout.MAX_ASPECT, tiles[0].clampedAspect, 0.0001f)
        assertEquals(MosaicLayout.MIN_ASPECT, tiles[1].clampedAspect, 0.0001f)
    }

    @Test
    fun `a new row starts before the next tile would exceed the row aspect budget`() {
        val rows = MosaicLayout.rows(
            listOf(1.5f, 0.6f, 1.2f, 1f, 0.8f).mapIndexed { index, aspect ->
                mediaItem(index.toLong() + 1, aspect)
            },
        )

        assertEquals(
            listOf(listOf(1L, 2L), listOf(3L, 4L), listOf(5L)),
            rows.map { row -> row.map { it.item.id } },
        )
        assertTrue(rows.all { row -> row.sumOf { it.clampedAspect.toDouble() } <= MosaicLayout.TARGET_ROW_ASPECT })
    }

    @Test
    fun `a row never holds more than the maximum number of tiles`() {
        val rows = MosaicLayout.rows(List(14) { index -> mediaItem(index.toLong() + 1, 0.5f) })

        assertEquals(listOf(4, 4, 4, 2), rows.map { it.size })
        assertTrue(rows.all { it.size <= MosaicLayout.MAX_PER_ROW })
    }
}

private fun mediaItem(id: Long, aspect: Float) = MediaItem(
    id = id,
    displayName = "IMG_$id.jpg",
    isVideo = false,
    dateAdded = id,
    width = (aspect * 100).toInt(),
    height = 100,
)
