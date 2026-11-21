package rs.rajofearth.galery.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaAccessDecisionTest {
    @Test
    fun `granted access shows All media`() {
        assertEquals(MediaAccessState.Granted, MediaAccessDecision.forResult(granted = true, askedBefore = false))
    }

    @Test
    fun `access granted after a previous request shows All media`() {
        assertEquals(MediaAccessState.Granted, MediaAccessDecision.forResult(granted = true, askedBefore = true))
    }

    @Test
    fun `first launch without access asks for permission`() {
        assertEquals(
            MediaAccessState.RequestFirstTime,
            MediaAccessDecision.forResult(granted = false, askedBefore = false),
        )
    }

    @Test
    fun `denied access after being asked shows the blocking screen`() {
        assertEquals(MediaAccessState.Denied, MediaAccessDecision.forResult(granted = false, askedBefore = true))
    }
}
