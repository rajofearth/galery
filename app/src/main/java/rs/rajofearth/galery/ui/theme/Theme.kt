package rs.rajofearth.galery.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal object GalleryPalette {
    val primary = Color(0xFFB7DAFF)
    val onPrimary = Color(0xFF102436)
    val background = Color.Black
    val surface = Color.Black
    val surfaceContainerLow = Color(0xFF131517)
    val surfaceContainer = Color(0xFF1C1C1E)
    val surfaceContainerHigh = Color(0xFF222528)
    val surfaceContainerHighest = Color(0xFF2A2D30)
    val onSurface = Color(0xFFF6F7F8)
    val onSurfaceVariant = Color(0xFFA9AFB5)
    val outlineVariant = Color(0xFF34383C)
    val error = Color(0xFFFFB4AB)
}

internal object GalleryTypography {
    val headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold)
    val headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 32.sp)
    val titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp)
    val titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium)
    val titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
    val bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp)
    val bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp)
    val bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp)
    val labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
    val labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
}

@Composable
internal fun GalleryText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = GalleryPalette.onSurface,
    style: TextStyle = GalleryTypography.bodyLarge,
    textAlign: TextAlign = TextAlign.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.merge(color = color, textAlign = textAlign),
        maxLines = maxLines,
        overflow = overflow,
    )
}

@Composable
internal fun GalleryIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = GalleryPalette.onSurface,
) {
    Image(
        painter = rememberVectorPainter(imageVector),
        contentDescription = contentDescription,
        modifier = modifier.size(24.dp),
        colorFilter = ColorFilter.tint(tint),
    )
}
