package rs.rajofearth.galery.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import rs.rajofearth.galery.R
import rs.rajofearth.galery.data.MediaItem
import rs.rajofearth.galery.data.MediaLayout
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Calendar
import androidx.compose.foundation.layout.PaddingValues
import java.util.Date
import java.util.TimeZone

private val tileGap = 2.dp

private sealed interface PhotoGridEntry {
    val key: String

    data class Photo(val item: MediaItem) : PhotoGridEntry {
        override val key get() = item.key
    }

    data class Day(val day: Long, val timestamp: Long) : PhotoGridEntry {
        override val key get() = "day-$day"
    }
}

private fun photoGridEntries(items: List<MediaItem>, dated: Boolean): List<PhotoGridEntry> = buildList {
    val zone = TimeZone.getDefault()
    var previousDay: Long? = null
    for (item in items) {
        if (dated) {
            val time = item.dateMillis
            val day = if (time > 0L) Math.floorDiv(time + zone.getOffset(time), 86_400_000L) else Long.MIN_VALUE
            if (day != previousDay) {
                add(PhotoGridEntry.Day(day, time))
                previousDay = day
            }
        }
        add(PhotoGridEntry.Photo(item))
    }
}

internal class MediaTilesState internal constructor(
    val layout: MediaLayout,
    internal val listState: LazyListState,
    internal val gridState: LazyGridState,
    items: List<MediaItem>,
    dated: Boolean,
) {
    private val grid = if (layout == MediaLayout.Grid) photoGridEntries(items, dated) else emptyList()
    internal val rows = if (layout == MediaLayout.Mosaic) MosaicLayout.rows(items) else emptyList()

    fun resetScroll() {
        listState.requestScrollToItem(0)
        gridState.requestScrollToItem(0)
    }

    fun jumpToDate(key: String) {
        val index = grid.indexOfFirst { it.key == key }
        if (index >= 0) gridState.requestScrollToItem((index - 1).coerceAtLeast(0))
    }

    suspend fun reveal(key: String): Boolean = when (layout) {
        MediaLayout.Grid -> {
            val index = grid.indexOfFirst { it.key == key }
            val needsScroll = index >= 0 && gridState.layoutInfo.visibleItemsInfo.none { it.index == index }
            if (needsScroll) gridState.scrollToItem(index)
            needsScroll
        }
        MediaLayout.Mosaic -> {
            val index = rows.indexOfFirst { row -> row.any { it.item.key == key } }
            val needsScroll = index >= 0 && listState.layoutInfo.visibleItemsInfo.none { it.index == index }
            if (needsScroll) listState.scrollToItem(index)
            needsScroll
        }
    }

    internal fun populateGrid(
        scope: androidx.compose.foundation.lazy.grid.LazyGridScope,
        selectedKeys: Set<String>,
        onOpen: (MediaItem) -> Unit,
        onSelect: (MediaItem) -> Unit,
    ) = with(scope) {
        items(
            grid,
            key = { it.key },
            span = { GridItemSpan(if (it is PhotoGridEntry.Day) maxLineSpan else 1) },
            contentType = { if (it is PhotoGridEntry.Day) "date" else "media" },
        ) { entry ->
            when (entry) {
                is PhotoGridEntry.Photo -> MediaTile(entry.item, selectedKeys, onOpen, onSelect, Modifier.aspectRatio(1f))
                is PhotoGridEntry.Day -> {
                    val text = if (entry.timestamp > 0L) {
                        remember(entry.timestamp) { galleryDayLabel(entry.timestamp) }
                    } else stringResource(R.string.photo_date_unknown)
                    Text(
                        text,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun rememberMediaTilesState(items: List<MediaItem>, layout: MediaLayout, dated: Boolean): MediaTilesState {
    val list = rememberLazyListState()
    val grid = rememberLazyGridState()
    return remember(items, layout, dated, list, grid) { MediaTilesState(layout, list, grid, items, dated) }
}

@Composable
internal fun MediaTiles(
    state: MediaTilesState,
    selectedKeys: Set<String>,
    onOpen: (MediaItem) -> Unit,
    onSelect: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val layout = state.layout
    val motion = spring<Float>(dampingRatio = 1f, stiffness = 600f)
    val arrival = remember { Animatable(1f) }
    var previousLayout by remember { mutableStateOf(layout) }
    LaunchedEffect(layout) {
        if (layout != previousLayout) {
            previousLayout = layout
            arrival.snapTo(0f)
            arrival.animateTo(1f, motion)
        }
    }
    val animatedModifier = modifier.graphicsLayer {
        alpha = 0.78f + 0.22f * arrival.value
        scaleX = 0.97f + 0.03f * arrival.value
        scaleY = scaleX
    }
    if (layout == MediaLayout.Grid) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(70.dp),
            contentPadding = contentPadding,
            state = state.gridState,
            modifier = animatedModifier,
            horizontalArrangement = Arrangement.spacedBy(tileGap),
            verticalArrangement = Arrangement.spacedBy(tileGap),
        ) {
            state.populateGrid(this, selectedKeys, onOpen, onSelect)
        }
    } else BoxWithConstraints(animatedModifier) {
        val availableWidth = maxWidth
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = state.listState,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(tileGap),
        ) {
            items(state.rows, key = { it.first().item.key }) { row ->
                val width = availableWidth - tileGap * (row.size - 1)
                val ratio = row.sumOf { it.clampedAspect.toDouble() }.toFloat()
                Row(
                    modifier = Modifier.fillMaxWidth().height(width / ratio),
                    horizontalArrangement = Arrangement.spacedBy(tileGap),
                ) {
                    row.forEach { tile ->
                        key(tile.item.key) {
                            MediaTile(
                                tile.item, selectedKeys, onOpen, onSelect,
                                Modifier.weight(tile.clampedAspect).fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaTile(
    item: MediaItem,
    selectedKeys: Set<String>,
    onOpen: (MediaItem) -> Unit,
    onSelect: (MediaItem) -> Unit,
    modifier: Modifier,
) {
    val isSelected = item.key in selectedKeys
    val shape = androidx.compose.ui.graphics.RectangleShape
    val selectionLabel = stringResource(R.string.select_item)
    Box(
        modifier = modifier
            .clip(shape)
            .then(if (selectedKeys.isEmpty()) Modifier else Modifier.semantics { selected = isSelected })
            .combinedClickable(
                onClick = { if (selectedKeys.isEmpty()) onOpen(item) else onSelect(item) },
                onLongClickLabel = selectionLabel,
                onLongClick = { onSelect(item) },
            ),
    ) {
        AsyncImage(
            model = rememberMediaImageModel(item, thumbnail = true),
            onSuccess = galleryImageReady(),
            contentDescription = item.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh),
        )
        if (item.isVideo) {
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
                shape = shape,
                color = Color.Black.copy(alpha = 0.72f),
                contentColor = Color.White,
            ) {
                Text(
                    item.durationLabel,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        if (isSelected) {
            val colors = MaterialTheme.colorScheme
            Box(
                Modifier.matchParentSize()
                    .background(colors.surface.copy(alpha = 0.20f))
                    .border(2.dp, colors.onSurface.copy(alpha = 0.88f), shape),
            )
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(24.dp).galleryEntrance(item.key),
                color = colors.onSurface,
                contentColor = colors.surfaceContainer,
                shape = CircleShape,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

internal fun galleryDayLabel(timestamp: Long): String {
    val calendar = Calendar.getInstance()
    val currentYear = calendar.get(Calendar.YEAR)
    calendar.timeInMillis = timestamp
    val locale = Locale.getDefault()
    val fields = if (calendar.get(Calendar.YEAR) == currentYear) "MMMd" else "yMMMd"
    return SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, fields), locale).format(Date(timestamp))
}
