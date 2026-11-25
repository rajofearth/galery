package rs.rajofearth.galery.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class MediaVisibilityTest {
    @get:Rule
    val storage = TemporaryFolder()

    @Test
    fun `keeps an ordinary photo in a visible folder`() {
        assertTrue(
            MediaVisibility.isVisible(
                displayName = "IMG_20260101_120000.jpg",
                relativePath = "DCIM/Camera/",
                nomediaFolders = emptySet(),
            ),
        )
    }

    @Test
    fun `excludes a file whose name starts with a dot`() {
        assertFalse(
            MediaVisibility.isVisible(
                displayName = ".thumb.jpg",
                relativePath = "DCIM/Camera/",
                nomediaFolders = emptySet(),
            ),
        )
    }

    @Test
    fun `excludes media in a folder containing a nomedia file`() {
        assertFalse(
            MediaVisibility.isVisible(
                displayName = "IMG_0002.jpg",
                relativePath = "DCIM/Private/",
                nomediaFolders = setOf("DCIM/Private/"),
            ),
        )
    }

    @Test
    fun `excludes media nested under a hidden folder`() {
        assertFalse(MediaVisibility.isVisible("photo.jpg", "DCIM/.private/subfolder/", emptySet()))
    }

    @Test
    fun `a nomedia file in the storage root hides descendants`() {
        assertFalse(MediaVisibility.isVisible("photo.jpg", "DCIM/Camera/", setOf("/")))
    }

    @Test
    fun `a nomedia marker hides subfolders but leaves siblings visible`() {
        val nested = storage.newFolder("Private", "Nested")
        val visible = storage.newFolder("Public")
        File(nested.parentFile, ".nomedia").writeText("")
        val cache = mutableMapOf<File, Boolean>()

        assertTrue(MediaVisibility.hasNomediaAncestor(nested, cache))
        assertFalse(MediaVisibility.hasNomediaAncestor(visible, cache))
    }

    @Test
    fun `excludes media in a subfolder of a folder containing a nomedia file`() {
        assertFalse(
            MediaVisibility.isVisible(
                displayName = "IMG_0003.jpg",
                relativePath = "DCIM/Private/Nested/",
                nomediaFolders = setOf("DCIM/Private/"),
            ),
        )
    }

    @Test
    fun `keeps media in a sibling folder when another folder contains a nomedia file`() {
        assertTrue(
            MediaVisibility.isVisible(
                displayName = "IMG_0004.jpg",
                relativePath = "DCIM/Camera/",
                nomediaFolders = setOf("DCIM/Private/"),
            ),
        )
    }
}
