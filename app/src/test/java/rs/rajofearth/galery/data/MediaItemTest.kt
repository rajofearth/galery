package rs.rajofearth.galery.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
}

private fun photo(width: Int, height: Int) = MediaItem(
    id = 1L,
    displayName = "IMG_0001.jpg",
    isVideo = false,
    dateAdded = 1_700_000_000L,
    width = width,
    height = height,
)

private fun video(width: Int, height: Int) = photo(width, height).copy(
    displayName = "VID_0001.mp4",
    isVideo = true,
)
