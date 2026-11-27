package rs.rajofearth.galery.ui

import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import rs.rajofearth.galery.R
import rs.rajofearth.galery.data.GaleryRelease
import rs.rajofearth.galery.data.UpdateDownloadProgress
import rs.rajofearth.galery.ui.theme.GalleryPalette
import rs.rajofearth.galery.ui.theme.GalleryText
import rs.rajofearth.galery.ui.theme.GalleryTypography

@Composable
internal fun UpdateSheet(
    installedVersion: String,
    release: GaleryRelease,
    phase: UpdatePhase,
    onDismiss: () -> Unit,
    onInstall: () -> Unit,
    onCancel: () -> Unit,
    frameAtDraw: (() -> GlassFrame?)? = null,
) {
    val context = LocalContext.current
    val size = remember(release.size) { Formatter.formatShortFileSize(context, release.size.toLong()) }
    val notes = remember(release.notes) { displayReleaseNotes(release.notes) }
    val progress = (phase as? UpdatePhase.Downloading)?.progress
    val status = when (phase) {
        UpdatePhase.Offer -> null
        is UpdatePhase.Downloading -> if (phase.progress.bytes == 0) R.string.update_connecting else R.string.update_downloading
        UpdatePhase.Stopping -> R.string.update_stopping
        UpdatePhase.Verifying -> R.string.update_verifying
        UpdatePhase.Installing -> R.string.update_installing
        UpdatePhase.Failed -> R.string.update_install_failed
        UpdatePhase.PermissionRequired -> R.string.update_install_permission
        UpdatePhase.InstallCanceled -> R.string.update_install_canceled
    }
    val transferring = phase is UpdatePhase.Downloading || phase == UpdatePhase.Verifying
    val canInstall = !transferring && phase != UpdatePhase.Installing && phase != UpdatePhase.Stopping
    GallerySheet(
        onDismissRequest = onDismiss,
        frameAtDraw = frameAtDraw,
        dismissEnabled = phase != UpdatePhase.Installing,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = GalleryDesign.inset),
            verticalArrangement = Arrangement.spacedBy(GalleryDesign.separation),
        ) {
            GalleryText(
                stringResource(R.string.update_sheet_title),
                Modifier.semantics { heading() },
                style = GalleryTypography.titleLarge,
            )
            AppIdentityLabel(stringResource(R.string.update_version_transition, installedVersion, release.version))
            GalleryText(
                stringResource(R.string.update_download_size, size),
                color = GalleryPalette.onSurfaceVariant,
                style = GalleryTypography.bodyMedium,
            )
            Column(
                Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(GalleryDesign.gap),
            ) {
                GalleryText(stringResource(R.string.update_release_notes), Modifier.semantics { heading() }, style = GalleryTypography.titleMedium)
                GalleryText(
                    notes.ifBlank { stringResource(R.string.update_no_notes) },
                    color = GalleryPalette.onSurfaceVariant,
                    style = GalleryTypography.bodyMedium,
                )
            }
            if (status != null) {
                GalleryText(
                    stringResource(status),
                    Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    color = if (phase == UpdatePhase.Failed) GalleryPalette.error else GalleryPalette.onSurface,
                    style = GalleryTypography.titleSmall,
                )
            }
            if (progress?.bytes == 0 || phase == UpdatePhase.Stopping) {
                LibraryLoadingIndicator()
            } else if (progress != null || phase == UpdatePhase.Verifying) {
                UpdateProgress(progress ?: UpdateDownloadProgress(release.size), release.size, size)
            }
            Row(
                Modifier.fillMaxWidth().padding(bottom = GalleryDesign.inset),
                horizontalArrangement = Arrangement.spacedBy(GalleryDesign.gap),
            ) {
                if (transferring) {
                    GalleryPillAction(stringResource(R.string.update_cancel), onClick = onCancel)
                } else if (canInstall) {
                    GalleryPillAction(stringResource(R.string.update_remind), modifier = Modifier.weight(1f), onClick = onDismiss)
                    val action = when (phase) {
                        UpdatePhase.PermissionRequired -> R.string.update_allow_installs
                        UpdatePhase.Failed, UpdatePhase.InstallCanceled -> R.string.update_try_again
                        else -> R.string.update_install_now
                    }
                    GalleryPillAction(stringResource(action), modifier = Modifier.weight(1f), primary = true, onClick = onInstall)
                }
            }
        }
    }
}

@Composable
private fun UpdateProgress(progress: UpdateDownloadProgress, total: Int, formattedTotal: String) {
    val context = LocalContext.current
    val fraction = (progress.bytes.toFloat() / total).coerceIn(0f, 1f)
    val fill = animateFloatAsState(fraction, GalleryDesign.effects(), label = "Update progress")
    Box(
        Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(GalleryPalette.surfaceContainerHighest)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f) },
    ) {
        Box(Modifier.fillMaxWidth(fill.value.coerceIn(0f, 1f)).height(6.dp).background(GalleryPalette.primary, CircleShape))
    }
    if (progress.bytes > 0) {
        val transferred = Formatter.formatShortFileSize(context, progress.bytes.toLong())
        GalleryText(
            stringResource(R.string.update_transfer, transferred, formattedTotal),
            color = GalleryPalette.onSurfaceVariant,
            style = GalleryTypography.bodySmall,
        )
    }
    if (progress.bytesPerSecond > 0 && progress.bytes in 1 until total) {
        val speed = Formatter.formatShortFileSize(context, progress.bytesPerSecond)
        val left = (total - progress.bytes + progress.bytesPerSecond - 1) / progress.bytesPerSecond
        GalleryText(
            stringResource(R.string.update_speed_and_eta, speed, DateUtils.formatElapsedTime(left)),
            color = GalleryPalette.onSurfaceVariant,
            style = GalleryTypography.bodySmall,
        )
    }
}

internal fun displayReleaseNotes(notes: String): String = notes.lineSequence()
    .map { it.trimEnd() }
    .filterNot { it.trimStart().startsWith("<!--") }
    .map { line ->
        line.replace(Regex("^#{1,6} +"), "")
            .replace(Regex("^[-*] +"), "• ")
            .replace(Regex("\\[([^]]+)]\\([^)]*\\)"), "$1")
            .replace("**", "")
            .replace("`", "")
    }
    .dropWhile { it.isBlank() || it.lowercase().replace('’', '\'') == "what's new" }
    .joinToString("\n").trim()
