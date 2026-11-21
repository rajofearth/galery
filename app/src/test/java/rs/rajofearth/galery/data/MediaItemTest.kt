package rs.rajofearth.galery.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaItemTest {
    @Test
    fun `aspect ratio comes from catalog width and height`() {
        val item = photo(width = 4000, height = 3000)

        assertEquals(4f / 3f, item.aspectRatio, 0.0001f)
    }

    @Test
    fun `a portrait photo has an aspect ratio below one`() {
        val item = photo(width = 3000, height = 4000)

        assertEquals(0.75f, item.aspectRatio, 0.0001f)
    }

    @Test
    fun `an image and a video with the same media store id have different keys`() {
        assertNotEquals(photo(width = 100, height = 100).key, video(width = 100, height = 100).key)
    }

    @Test
    fun `missing catalog dimensions fall back to a square aspect ratio`() {
        assertEquals(1f, photo(width = 0, height = 0).aspectRatio, 0.0001f)
        assertEquals(1f, photo(width = 0, height = 3000).aspectRatio, 0.0001f)
    }

    @Test
    fun `a video with a known duration shows a duration badge`() {
        val item = video(width = 100, height = 100, durationMs = 90_000L)

        assertTrue(item.hasDurationBadge)
        assertEquals("1:30", item.durationLabel)
    }

    @Test
    fun `a video with an unknown duration still shows a duration badge`() {
        val item = video(width = 100, height = 100)

        assertTrue(item.hasDurationBadge)
        assertEquals("--:--", item.durationLabel)
    }

    @Test
    fun `a photo never shows a duration badge`() {
        val item = photo(width = 100, height = 100).copy(durationMs = 90_000L)

        assertFalse(item.hasDurationBadge)
    }

    @Test
    fun `short videos pad the seconds in the duration badge`() {
        assertEquals("0:05", video(width = 100, height = 100, durationMs = 5_000L).durationLabel)
    }

    @Test
    fun `videos longer than an hour show hours in the duration badge`() {
        assertEquals("1:02:03", video(width = 100, height = 100, durationMs = 3_723_000L).durationLabel)
    }
}

private fun photo(width: Int, height: Int) = MediaItem(
    id = 1L,
    displayName = "IMG_0001.jpg",
    isVideo = false,
    dateAdded = 1_700_000_000L,
    width = width,
    height = height,
)

private fun video(width: Int, height: Int, durationMs: Long = 0L) = photo(width, height).copy(
    displayName = "VID_0001.mp4",
    isVideo = true,
    durationMs = durationMs,
)
