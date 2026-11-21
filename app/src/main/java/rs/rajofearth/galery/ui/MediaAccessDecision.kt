package rs.rajofearth.galery.ui

enum class MediaAccessState {
    Granted,
    RequestFirstTime,
    Denied,
}

object MediaAccessDecision {
    fun forResult(granted: Boolean, askedBefore: Boolean): MediaAccessState = when {
        granted -> MediaAccessState.Granted
        askedBefore -> MediaAccessState.Denied
        else -> MediaAccessState.RequestFirstTime
    }
}
