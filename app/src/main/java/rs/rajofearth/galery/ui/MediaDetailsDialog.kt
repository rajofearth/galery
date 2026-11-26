package rs.rajofearth.galery.ui

import android.text.format.DateFormat
import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import rs.rajofearth.galery.ui.theme.GalleryPalette
import rs.rajofearth.galery.ui.theme.GalleryTypography
import rs.rajofearth.galery.ui.theme.GalleryText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import rs.rajofearth.galery.R
import rs.rajofearth.galery.data.MediaItem
import java.util.Date

@Composable
internal fun MediaDetailsDialog(
    items: List<MediaItem>,
    favoriteKeys: Set<String>,
    frameAtDraw: () -> GlassFrame?,
    onDismiss: () -> Unit,
) {
    GallerySheet(onDismissRequest = onDismiss, frameAtDraw = frameAtDraw) {
        GalleryText(
            stringResource(R.string.media_details_title),
            modifier = Modifier.fillMaxWidth().padding(horizontal = GalleryDesign.inset),
            style = GalleryTypography.titleLarge,
        )
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
            contentPadding = PaddingValues(horizontal = GalleryDesign.inset, vertical = GalleryDesign.inset),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            items(items, key = { it.key }) { item ->
                MediaDetailsFields(item, item.key in favoriteKeys)
            }
        }
    }
}

@Composable
private fun MediaDetailsFields(item: MediaItem, isFavorite: Boolean) {
    val context = LocalContext.current
    val cardTint = GalleryPalette.surfaceContainerHighest.copy(alpha = 0.48f)
    val unknown = stringResource(R.string.media_details_unknown)
    val folder = item.relativePath.trimEnd('/').ifBlank {
        item.filePath?.substringBeforeLast('/', missingDelimiterValue = "").orEmpty()
    }
    val dateTaken = item.dateTaken.takeIf { it > 0L }?.let { timestamp ->
        val date = Date(timestamp)
        stringResource(
            R.string.media_details_date_time,
            DateFormat.getDateFormat(context).format(date),
            DateFormat.getTimeFormat(context).format(date),
        )
    }

    SelectionContainer {
        Column(verticalArrangement = Arrangement.spacedBy(GalleryDesign.separation)) {
            Row(
                Modifier.fillMaxWidth().clip(GalleryDesign.panel)
                    .background(cardTint).padding(GalleryDesign.separation),
                horizontalArrangement = Arrangement.spacedBy(GalleryDesign.separation),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    rememberMediaImageModel(item, thumbnail = true), contentDescription = null,
                    modifier = Modifier.size(64.dp).clip(GalleryDesign.cover),
                    contentScale = ContentScale.Crop,
                )
                Column(Modifier.weight(1f)) {
                    GalleryText(item.displayName.ifBlank { unknown }, style = GalleryTypography.titleMedium)
                    if (dateTaken != null) {
                        GalleryText(
                            dateTaken,
                            style = GalleryTypography.bodySmall,
                            color = GalleryPalette.onSurfaceVariant,
                        )
                    }
                }
            }
            Column(
                Modifier.fillMaxWidth().clip(GalleryDesign.panel)
                    .background(cardTint)
                    .padding(GalleryDesign.inset),
                verticalArrangement = Arrangement.spacedBy(GalleryDesign.separation),
            ) {
                DetailField(R.string.media_details_folder, folder.ifBlank { unknown })
                DetailField(R.string.media_details_path, item.filePath?.ifBlank { unknown } ?: unknown)
                if (dateTaken == null) DetailField(R.string.media_details_date_taken, unknown)
                DetailField(
                    R.string.media_details_size,
                    if (item.sizeBytes >= 0L) Formatter.formatFileSize(context, item.sizeBytes) else unknown,
                )
                DetailField(
                    R.string.media_details_dimensions,
                    if (item.width > 0 && item.height > 0) {
                        stringResource(R.string.media_details_pixel_dimensions, item.width, item.height)
                    } else {
                        unknown
                    },
                )
                DetailField(R.string.media_details_mime_type, item.mimeType?.ifBlank { unknown } ?: unknown)
                DetailField(
                    R.string.media_details_favorite,
                    stringResource(if (isFavorite) R.string.media_details_yes else R.string.media_details_no),
                )
                if (item.isVideo) {
                    DetailField(
                        R.string.media_details_duration,
                        if (item.durationMs > 0L) item.durationLabel else unknown,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailField(label: Int, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        GalleryText(
            stringResource(label),
            modifier = Modifier.weight(0.36f),
            color = GalleryPalette.onSurfaceVariant,
            style = GalleryTypography.bodyMedium,
        )
        GalleryText(
            value,
            modifier = Modifier.weight(0.64f),
            color = GalleryPalette.onSurface,
            style = GalleryTypography.bodyMedium,
        )
    }
}
