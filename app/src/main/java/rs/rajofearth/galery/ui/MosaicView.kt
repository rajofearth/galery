package rs.rajofearth.galery.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import rs.rajofearth.galery.data.MediaItem

private val tileGap = 4.dp

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MosaicView(
    items: List<MediaItem>,
    onItemClick: (Int) -> Unit,
    openedKey: String?,
    scopes: MediaTransitionScopes,
    modifier: Modifier = Modifier,
) {
    val rows = remember(items) { MosaicLayout.rows(items) }
    val indexById = remember(items) {
        items.mapIndexed { index, item -> item.key to index }.toMap()
    }

    LazyColumn(modifier = modifier) {
        items(rows, key = { row -> row.first().item.key }) { row ->
            scopes.sharedTransition.MosaicRow(
                row = row,
                openedKey = openedKey,
                indexById = indexById,
                scopes = scopes,
                onItemClick = onItemClick,
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.MosaicRow(
    row: List<MosaicTile>,
    openedKey: String?,
    indexById: Map<String, Int>,
    scopes: MediaTransitionScopes,
    onItemClick: (Int) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val gapPx = with(density) { tileGap.toPx() } * (row.size - 1)
        val rowWidthPx = with(density) { maxWidth.toPx() } - gapPx
        val rowHeight = with(density) { (rowWidthPx / row.sumOf { it.clampedAspect.toDouble() }.toFloat()).toDp() }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight)
                .padding(bottom = tileGap),
            horizontalArrangement = Arrangement.spacedBy(tileGap),
        ) {
            row.forEach { tile ->
                val item = tile.item
                MosaicTileImage(
                    item = item,
                    weight = tile.clampedAspect,
                    sharedElementKey = if (item.key == openedKey) "media-${item.key}" else null,
                    transition = scopes.sharedTransition,
                    visibilityScope = scopes.visibility,
                    onClick = indexById[item.key]?.let { index -> { onItemClick(index) } },
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun RowScope.MosaicTileImage(
    item: MediaItem,
    weight: Float,
    sharedElementKey: String?,
    transition: SharedTransitionScope,
    visibilityScope: AnimatedVisibilityScope,
    onClick: (() -> Unit)?,
) {
    var imageModifier = Modifier
        .fillMaxSize()
        .clip(MaterialTheme.shapes.extraSmall)

    if (sharedElementKey != null) {
        imageModifier = with(transition) {
            imageModifier.sharedElement(
                rememberSharedContentState(key = sharedElementKey),
                animatedVisibilityScope = visibilityScope,
            )
        }
    }

    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .weight(weight)
            .fillMaxSize()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.TopEnd,
    ) {
        AsyncImage(
            model = rememberMediaImageModel(item),
            contentDescription = item.displayName,
            contentScale = ContentScale.Crop,
            modifier = imageModifier,
        )
        if (item.isVideo) {
            Surface(
                modifier = Modifier.padding(4.dp),
                shape = MaterialTheme.shapes.extraSmall,
                color = Color.Black.copy(alpha = 0.6f),
            ) {
                Text(
                    text = item.durationLabel,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }
        }
    }
}
