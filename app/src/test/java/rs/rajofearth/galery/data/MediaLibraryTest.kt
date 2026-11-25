package rs.rajofearth.galery.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MediaLibraryTest {
    @Test
    fun `media with the same type and ID on different volumes have different keys`() {
        val onCard = cameraPhoto.copy(volumeName = "0123-4567")
        assertEquals("content://media/0123-4567/images/media/1", onCard.key)
        assertNotEquals(cameraPhoto.key, onCard.key)
    }

    @Test
    fun `albums distinguish full folder paths and storage volumes`() {
        val sameFolder = cameraPhoto.copy(id = 2, relativePath = "DCIM/Camera")
        val sameName = cameraPhoto.copy(id = 3, relativePath = "Pictures/Camera/")
        val onCard = cameraPhoto.copy(volumeName = "0123-4567")
        val albums = listOf(cameraPhoto, sameFolder, sameName, onCard).toAlbums()
        assertEquals(listOf("Camera", "Camera", "Camera"), albums.map { it.name })
        assertEquals(listOf(listOf(cameraPhoto, sameFolder), listOf(sameName), listOf(onCard)), albums.map { it.items })
    }

    @Test
    fun `default sort uses date taken or date added in consistent units`() {
        val lastTaken = cameraPhoto.copy(id = 2, dateTaken = 3_000L, dateAdded = 1L)
        val missingDateTaken = cameraPhoto.copy(id = 3, dateAdded = 2L)
        val firstTaken = cameraPhoto.copy(dateTaken = 1_000L, dateAdded = 4L)
        val items = listOf(firstTaken, lastTaken, missingDateTaken)
        assertEquals(listOf(lastTaken, missingDateTaken, firstTaken), items.sortedBy(MediaSort()))
    }

    @Test
    fun `sort uses filename or size and supports both directions`() {
        val smaller = cameraPhoto.copy(id = 2, displayName = "z.jpeg", sizeBytes = 5)
        val larger = cameraPhoto.copy(displayName = "A.jpeg", sizeBytes = 10)
        val items = listOf(smaller, larger)
        assertEquals(listOf(larger, smaller), items.sortedBy(MediaSort(MediaSortField.Name, descending = false)))
        assertEquals(listOf(smaller, larger), items.sortedBy(MediaSort(MediaSortField.Name)))
        assertEquals(listOf(smaller, larger), items.sortedBy(MediaSort(MediaSortField.Size, descending = false)))
        assertEquals(listOf(larger, smaller), items.sortedBy(MediaSort(MediaSortField.Size)))
    }

    @Test
    fun `search matches filenames album names and dates only`() {
        val photo = cameraPhoto.copy(dateTaken = 1_700_000_000_000L, mimeType = "image/jpeg")
        val withDateAddedOnly = photo.copy(id = 2, dateTaken = 0L, dateAdded = 1_700_000_000L)
        val items = listOf(photo, withDateAddedOnly)
        assertEquals(items, items.filteredBy(" IMg "))
        assertEquals(items, items.filteredBy("camera"))
        assertEquals(items, items.filteredBy("2023-11"))
        assertEquals(emptyList<MediaItem>(), items.filteredBy("jpeg"))
        assertEquals(emptyList<MediaItem>(), items.filteredBy("DCIM"))
    }

    private val cameraPhoto = MediaItem(
        id = 1L,
        displayName = "IMG.jpg",
        isVideo = false,
        dateAdded = 0L,
        relativePath = "DCIM/Camera/",
        volumeName = "external_primary",
    )
}
