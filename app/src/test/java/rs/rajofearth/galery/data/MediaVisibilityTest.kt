package rs.rajofearth.galery.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaVisibilityTest {
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
