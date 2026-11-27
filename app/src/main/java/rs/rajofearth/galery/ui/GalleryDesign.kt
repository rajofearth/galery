package rs.rajofearth.galery.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import rs.rajofearth.galery.ui.theme.GalleryIcon
import rs.rajofearth.galery.ui.theme.GalleryPalette
import rs.rajofearth.galery.ui.theme.GalleryTypography
import rs.rajofearth.galery.ui.theme.GalleryText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

internal object GalleryDesign {
    val inset = 16.dp
    val gap = 8.dp
    val separation = 12.dp
    val icon = 20.dp
    val button = 40.dp
    val target = 48.dp
    val dock = 56.dp
    val cover = RoundedCornerShape(16.dp)
    val tileCover = RoundedCornerShape(12.dp)
    val panel = RoundedCornerShape(20.dp)
    val sheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    const val glassTint = 0.60f
    const val viewerTintOpacity = 0.68f

    fun <T> spatial(): SpringSpec<T> = spring(dampingRatio = 0.9f, stiffness = 460f)
    fun <T> effects(): SpringSpec<T> = spring(dampingRatio = 1f, stiffness = 620f)
}

internal val LocalGalleryGlass = staticCompositionLocalOf<GlassBackdrop?> { null }

@Composable
internal fun Modifier.galleryPill(
    shape: Shape = CircleShape,
    backdrop: GlassBackdrop? = LocalGalleryGlass.current,
): Modifier {
    val tint = GalleryPalette.surfaceContainer
    return if (backdrop == null) {
        clip(shape).background(tint)
    } else {
        glassBackdrop(backdrop, shape, tint)
    }
}

@Composable
internal fun Modifier.galleryEntrance(destination: Any): Modifier {
    val progress = remember(destination) { Animatable(0f) }
    LaunchedEffect(progress) { progress.animateTo(1f, GalleryDesign.spatial()) }
    return graphicsLayer {
        val amount = progress.value.coerceIn(0f, 1f)
        alpha = 0.82f + amount * 0.18f
        translationY = (1f - amount) * GalleryDesign.gap.toPx()
    }
}

@Composable
private fun Modifier.galleryPressMotion(interaction: MutableInteractionSource): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.90f else 1f,
        GalleryDesign.spatial(),
        label = "Control press",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
internal fun GalleryIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = GalleryDesign.target,
    tint: Color = GalleryPalette.onSurface,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier.size(size).clip(CircleShape).clickable(
            interactionSource = interaction,
            indication = LocalIndication.current,
            role = Role.Button,
            onClick = onClick,
        ),
        contentAlignment = Alignment.Center,
    ) {
        GalleryIcon(icon, contentDescription = label, modifier = Modifier.size(GalleryDesign.icon).galleryPressMotion(interaction), tint = tint)
    }
}

@Composable
internal fun GalleryPillAction(
    label: String,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val colors = GalleryPalette
    val fill = when {
        !enabled -> Modifier.clip(CircleShape).background(colors.surfaceContainerHighest)
        primary -> Modifier.clip(CircleShape).background(colors.primary)
        else -> Modifier.galleryPill()
    }
    Box(
        modifier.then(fill)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .heightIn(min = GalleryDesign.target)
            .padding(horizontal = GalleryDesign.inset, vertical = GalleryDesign.gap),
        contentAlignment = Alignment.Center,
    ) {
        GalleryText(
            label,
            modifier = Modifier.galleryPressMotion(interaction),
            color = when {
                !enabled -> colors.onSurfaceVariant
                primary -> colors.onPrimary
                else -> colors.onSurface
            },
            style = GalleryTypography.labelLarge,
        )
    }
}

@Composable
internal fun GallerySegment(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = GalleryDesign.inset,
    showIndicator: Boolean = true,
) {
    val colors = GalleryPalette
    val interaction = remember { MutableInteractionSource() }
    val fill by animateColorAsState(
        if (selected && showIndicator) colors.surfaceContainerHighest else Color.Transparent,
        GalleryDesign.effects(),
        label = "Segment selection",
    )
    val textColor by animateColorAsState(
        if (selected) colors.onSurface else colors.onSurfaceVariant,
        GalleryDesign.effects(),
        label = "Segment label",
    )
    Box(
        modifier.clip(CircleShape).background(fill)
            .selectable(
                selected,
                interactionSource = interaction,
                indication = LocalIndication.current,
                role = Role.Tab,
                onClick = onClick,
            )
            .heightIn(min = GalleryDesign.target).padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        GalleryText(label, modifier = Modifier.galleryPressMotion(interaction), color = textColor, style = GalleryTypography.labelLarge, maxLines = 1)
    }
}

@Composable
internal fun GalleryTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    horizontalPadding: Dp = GalleryDesign.inset,
) {
    val indicator by animateFloatAsState(
        selectedIndex.toFloat(),
        GalleryDesign.effects(),
        label = "Tab indicator",
    )
    Row(
        Modifier.galleryPill()
            .selectableGroup()
            .width(IntrinsicSize.Max)
            .padding(4.dp)
            .drawBehind {
                val segmentWidth = size.width / labels.size
                val slot = if (layoutDirection == LayoutDirection.Ltr) indicator else labels.lastIndex - indicator
                drawRoundRect(
                    color = GalleryPalette.surfaceContainerHighest,
                    topLeft = Offset(segmentWidth * slot, 0f),
                    size = Size(segmentWidth, size.height),
                    cornerRadius = CornerRadius(size.height / 2),
                )
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        labels.forEachIndexed { index, label ->
            GallerySegment(
                label,
                index == selectedIndex,
                { onSelect(index) },
                Modifier.weight(1f),
                horizontalPadding = horizontalPadding,
                showIndicator = false,
            )
        }
    }
}
