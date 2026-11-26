package rs.rajofearth.galery.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseVersionTest {
    @Test
    fun comparesNumericVersionComponents() {
        assertTrue(isNewerRelease("0.10.0", "0.9.9"))
        assertTrue(isNewerRelease("2.0.0", "1.99.99"))
        assertFalse(isNewerRelease("0.9.9", "0.10.0"))
        assertFalse(isNewerRelease("0.1.0", "0.1.0"))
    }

    @Test
    fun rejectsPrereleasesAndMalformedVersions() {
        assertFalse(isNewerRelease("1.0.0-beta", "0.1.0"))
        assertFalse(isNewerRelease("1.0", "0.1.0"))
        assertFalse(isNewerRelease("999999999999.0.0", "0.1.0"))
        assertFalse(isNewerRelease("1.0.0", "custom"))
    }
}
