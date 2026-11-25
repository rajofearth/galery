package rs.rajofearth.galery.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

val GaleryShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
    largeIncreased = RoundedCornerShape(20.dp),
    extraLargeIncreased = RoundedCornerShape(32.dp),
    extraExtraLarge = RoundedCornerShape(48.dp),
)

private val GalleryColors = darkColorScheme(
    primary = Color(0xFFB7DAFF),
    onPrimary = Color(0xFF102436),
    primaryContainer = Color(0xFF253544),
    onPrimaryContainer = Color(0xFFE0EFFF),
    secondaryContainer = Color(0xFF292C30),
    onSecondaryContainer = Color(0xFFF6F7F8),
    background = Color(0xFF000000),
    surface = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF131517),
    surfaceContainer = Color(0xFF1C1C1E),
    surfaceContainerHigh = Color(0xFF222528),
    surfaceContainerHighest = Color(0xFF2A2D30),
    onBackground = Color(0xFFF6F7F8),
    onSurface = Color(0xFFF6F7F8),
    onSurfaceVariant = Color(0xFFA9AFB5),
    outline = Color(0xFF747A80),
    outlineVariant = Color(0xFF34383C),
)

private val GalleryTypography = Typography().let { default ->
    default.copy(headlineMedium = default.headlineMedium.copy(fontWeight = FontWeight.Bold))
}

@Composable
fun GaleryTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GalleryColors,
        shapes = GaleryShapes,
        typography = GalleryTypography,
    ) {
        CompositionLocalProvider(LocalContentColor provides GalleryColors.onSurface, content = content)
    }
}
