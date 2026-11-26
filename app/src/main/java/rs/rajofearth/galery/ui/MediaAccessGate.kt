package rs.rajofearth.galery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoLibrary
import rs.rajofearth.galery.ui.theme.GalleryIcon
import rs.rajofearth.galery.ui.theme.GalleryPalette
import rs.rajofearth.galery.ui.theme.GalleryTypography
import rs.rajofearth.galery.ui.theme.GalleryText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import rs.rajofearth.galery.R

@Composable
fun MediaAccessGate(
    state: MediaAccessState,
    onRequestAccess: () -> Unit,
    onOpenSettings: () -> Unit,
    content: @Composable () -> Unit,
) {
    when (state) {
        MediaAccessState.Granted -> content()

        MediaAccessState.RequestFirstTime,
        MediaAccessState.Denied,
        -> MediaAccessScreen(
            denied = state == MediaAccessState.Denied,
            onRequestAccess = onRequestAccess,
            onOpenSettings = onOpenSettings,
        )
    }
}

@Composable
private fun MediaAccessScreen(
    denied: Boolean,
    onRequestAccess: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().background(GalleryPalette.surface)
            .safeDrawingPadding().padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth().padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(64.dp).background(GalleryPalette.surfaceContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                GalleryIcon(
                    imageVector = Icons.Outlined.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = GalleryPalette.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(16.dp))
            GalleryText(
                text = stringResource(R.string.media_access_title),
                style = GalleryTypography.headlineSmall,
                color = GalleryPalette.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            GalleryText(
                text = stringResource(R.string.media_access_explanation),
                style = GalleryTypography.bodyMedium,
                color = GalleryPalette.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            GalleryPillAction(
                label = stringResource(if (denied) R.string.media_access_retry else R.string.media_access_grant),
                modifier = Modifier.fillMaxWidth(),
                primary = true,
                onClick = onRequestAccess,
            )
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier.clickable(role = Role.Button, onClick = onOpenSettings)
                    .heightIn(min = 48.dp).padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                GalleryText(
                    stringResource(R.string.media_access_open_settings),
                    color = GalleryPalette.onSurfaceVariant,
                    style = GalleryTypography.labelLarge,
                )
            }
        }
    }
}
