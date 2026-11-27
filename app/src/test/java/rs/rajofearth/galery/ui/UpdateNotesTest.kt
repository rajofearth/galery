package rs.rajofearth.galery.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateNotesTest {
    @Test
    fun formatsGitHubNotesWithoutASecondWhatsNewHeading() {
        val notes = """
            ## What's new

            - **Move** items between albums.
            - Tap `Install now` to continue.

            [Changelog](https://example.com/releases)
        """.trimIndent()

        assertEquals(
            "• Move items between albums.\n• Tap Install now to continue.\n\nChangelog",
            displayReleaseNotes(notes),
        )
    }
}
