package rs.rajofearth.galery.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope

data class MediaTransitionScopes(
    val sharedTransition: SharedTransitionScope,
    val visibility: AnimatedVisibilityScope,
)
