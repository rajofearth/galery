package rs.rajofearth.galery.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewQuilt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import rs.rajofearth.galery.ui.theme.GalleryIcon
import rs.rajofearth.galery.ui.theme.GalleryPalette
import rs.rajofearth.galery.ui.theme.GalleryTypography
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawingPadding
import rs.rajofearth.galery.ui.theme.GalleryText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import rs.rajofearth.galery.data.MediaItem
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import rs.rajofearth.galery.R
import rs.rajofearth.galery.data.MediaAlbum
import rs.rajofearth.galery.data.MediaLayout
import rs.rajofearth.galery.data.MediaSort
import rs.rajofearth.galery.data.MediaSortField

internal enum class PhotoFilter(val label: Int) {
    All(R.string.all_media), Photos(R.string.photos_title), Videos(R.string.filter_videos),
}

internal enum class GaleryTab(val title: Int, val icon: ImageVector, val empty: Int) {
    AllMedia(R.string.all_media, Icons.Filled.PhotoLibrary, R.string.all_media_no_items),
    Albums(R.string.albums, Icons.Filled.PhotoAlbum, R.string.albums_empty),
    Favorites(R.string.favorites, Icons.Filled.FavoriteBorder, R.string.favorites_empty),
}

@Composable
internal fun GaleryScreen(
    tab: GaleryTab,
    onTabSelected: (GaleryTab) -> Unit,
    onOpenSettings: () -> Unit,
    album: MediaAlbum?,
    onAlbumSelected: (MediaAlbum) -> Unit,
    onExitAlbum: () -> Unit,
    albums: List<MediaAlbum>,
    albumScroll: LazyGridState,
    favoriteCover: MediaItem?,
    favoriteCount: Int,
    trashOpen: Boolean,
    trashCount: Int?,
    onOpenTrash: (() -> Unit)?,
    onRestore: () -> Unit,
    timelineItems: List<MediaItem>,
    photoFilter: PhotoFilter,
    onPhotoFilter: (PhotoFilter) -> Unit,
    searchFilter: PhotoFilter,
    onSearchFilter: (PhotoFilter) -> Unit,
    onTimelineJump: (MediaItem) -> Unit,
    searching: Boolean,
    onSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    layout: MediaLayout,
    onToggleLayout: () -> Unit,
    sort: MediaSort,
    onSort: (MediaSort) -> Unit,
    selectionCount: Int,
    selectionBusy: Boolean,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onShare: () -> Unit,
    removeFavorites: Boolean,
    onFavoriteSelection: () -> Unit,
    onTrash: () -> Unit,
    onDetails: () -> Unit,
    hasMedia: Boolean,
    backdrop: GlassBackdrop,
    loadFailed: Boolean,
    onRetry: () -> Unit,
    hasItems: Boolean,
    mediaTiles: @Composable (Modifier, PaddingValues) -> Unit,
) {
    var timelineOpen by rememberSaveable(tab) { mutableStateOf(false) }
    BackHandler(enabled = timelineOpen) { timelineOpen = false }
    val showAlbums = tab == GaleryTab.Albums && album == null && !trashOpen && !searching
    val title = if (trashOpen) stringResource(R.string.trash_album) else album?.displayName() ?: stringResource(if (tab == GaleryTab.AllMedia) R.string.photos_title else tab.title)
    CompositionLocalProvider(LocalGalleryGlass provides backdrop) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= 720.dp
            Row(Modifier.fillMaxSize()) {
                if (wide) {
                    GalleryRail(tab, onTabSelected)
                }
                GalleryScaffold(
                    modifier = Modifier.weight(1f),
                    topBar = {
                        when {
                            selectionCount > 0 -> SelectionBar(selectionCount, onClearSelection, onSelectAll)
                            searching -> SearchBar(query, onQueryChange, searchFilter, onSearchFilter, onCloseSearch)
                            else -> Row(
                                Modifier.fillMaxWidth()
                                    .background(Brush.verticalGradient(listOf(Color.Black, Color.Black.copy(alpha = 0.62f), Color.Transparent)))
                                    .statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (album != null || trashOpen || tab == GaleryTab.Favorites) {
                                    RoundTool(Icons.AutoMirrored.Filled.ArrowBack, R.string.go_back) { if (tab == GaleryTab.Favorites) onTabSelected(GaleryTab.Albums) else onExitAlbum() }
                                }
                                GalleryText(
                                    title,
                                    modifier = Modifier.weight(1f),
                                    style = GalleryTypography.headlineMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (tab != GaleryTab.Favorites) {
                                    RoundTool(Icons.Filled.Search, R.string.search, onClick = onSearch)
                                }
                                if (showAlbums) {
                                    RoundTool(Icons.Filled.Settings, R.string.settings, onClick = onOpenSettings)
                                }
                                if (!showAlbums && !timelineOpen) {
                                    LayoutTool(layout, onToggleLayout)
                                    SortMenu(sort, onSort)
                                }
                            }
                        }
                    },
                    bottomBar = {
                        if (selectionCount > 0) {
                            SelectionDock(
                                selectionBusy, onShare, removeFavorites, onFavoriteSelection, onTrash, onDetails,
                                if (trashOpen) onRestore else null,
                            )
                        } else if (!wide && !searching && !timelineOpen) {
                            Row(
                                Modifier.fillMaxWidth()
                                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
                                    .navigationBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (tab == GaleryTab.AllMedia) {
                                    RoundTool(Icons.Filled.DateRange, R.string.timeline_browse, GalleryDesign.dock) { timelineOpen = true }
                                }
                                LibraryDock(tab, onTabSelected)
                                if (tab == GaleryTab.AllMedia) PhotoFilterMenu(photoFilter, onPhotoFilter)
                            }
                        }
                    },
                ) { innerPadding ->
                    val gallery = Modifier.fillMaxSize().glassBackdropSource(backdrop).then(
                        if (album != null || trashOpen || searching || timelineOpen || tab == GaleryTab.Favorites) {
                            Modifier.galleryEntrance(listOf(album?.key, trashOpen, searching, timelineOpen))
                        } else Modifier,
                    )
                    val body = gallery.padding(innerPadding)
                    when {
                        timelineOpen -> TimelineBrowser(timelineItems, gallery, innerPadding,
                            onJumpToDay = { timelineOpen = false; onTimelineJump(it) },
                            onClose = { timelineOpen = false })
                        loadFailed && !hasMedia -> Box(body, contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                GalleryText(stringResource(R.string.media_load_error))
                                GalleryPillAction(stringResource(R.string.media_access_retry), primary = true, onClick = onRetry)
                            }
                        }
                        !hasMedia -> Box(body, contentAlignment = Alignment.Center) { LibraryLoadingIndicator() }
                        showAlbums -> AlbumGrid(
                            albums, favoriteCover, favoriteCount, trashCount, onAlbumSelected, onOpenTrash,
                            gallery, innerPadding, wide, albumScroll, { onTabSelected(GaleryTab.Favorites) },
                        )
                        !showAlbums && hasItems -> mediaTiles(gallery, innerPadding)
                        else -> EmptyLibrary(
                            if (query.isBlank()) tab.icon else Icons.Filled.Search,
                            if (query.isNotBlank()) R.string.no_search_results else if (trashOpen) R.string.trash_empty else tab.empty,
                            body,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumGrid(
    albums: List<MediaAlbum>,
    favoriteCover: MediaItem?,
    favoriteCount: Int,
    trashCount: Int?,
    onOpen: (MediaAlbum) -> Unit,
    onOpenTrash: (() -> Unit)?,
    modifier: Modifier,
    contentPadding: PaddingValues,
    wide: Boolean,
    state: LazyGridState,
    onOpenFavorites: () -> Unit,
) {
    val (shortcuts, folders) = remember(albums) {
        albums.partition { it.name.equals("Camera", true) || it.name.equals("Screenshots", true) }
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(if (wide) 6 else 3),
        state = state,
        contentPadding = contentPadding,
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (shortcuts.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                AlbumSectionTitle(R.string.album_shortcuts)
            }
            items(shortcuts, key = { it.key }) {
                AlbumCover(it, onOpen)
            }
        }
        if (folders.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                AlbumSectionTitle(R.string.album_folders)
            }
            items(folders, key = { it.key }) {
                AlbumCover(it, onOpen)
            }
        }
        item(key = "favorites") {
            AlbumCover(stringResource(R.string.favorites), favoriteCover, favoriteCount, Icons.Filled.FavoriteBorder, onOpenFavorites)
        }
        if (onOpenTrash != null) {
            item(key = "trash") {
                AlbumCover(stringResource(R.string.trash_album), null, trashCount, Icons.Filled.DeleteOutline, onOpenTrash)
            }
        }
    }
}

@Composable
private fun GalleryRail(tab: GaleryTab, onTabSelected: (GaleryTab) -> Unit) {
    Column(
        Modifier.width(80.dp).background(GalleryPalette.surfaceContainer)
            .safeDrawingPadding().selectableGroup().padding(GalleryDesign.gap),
        verticalArrangement = Arrangement.spacedBy(GalleryDesign.gap),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        listOf(GaleryTab.AllMedia, GaleryTab.Albums).forEach { destination ->
            val selected = tab == destination || destination == GaleryTab.Albums && tab == GaleryTab.Favorites
            Column(
                Modifier.fillMaxWidth().clip(GalleryDesign.cover)
                    .background(if (selected) GalleryPalette.surfaceContainerHighest else Color.Transparent)
                    .selectable(selected, role = Role.Tab) { onTabSelected(destination) }
                    .padding(vertical = GalleryDesign.gap),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                GalleryIcon(destination.icon, contentDescription = null)
                GalleryText(
                    stringResource(if (destination == GaleryTab.AllMedia) R.string.photos_title else destination.title),
                    style = GalleryTypography.labelSmall,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun AlbumSectionTitle(label: Int) {
    GalleryText(
        stringResource(label),
        style = GalleryTypography.titleMedium,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun AlbumCover(album: MediaAlbum, onOpen: (MediaAlbum) -> Unit) {
    AlbumCover(album.displayName(), album.items.firstOrNull(), album.items.size) { onOpen(album) }
}

@Composable
private fun AlbumCover(
    name: String,
    cover: MediaItem?,
    count: Int?,
    fallback: ImageVector = Icons.Filled.PhotoAlbum,
    onOpen: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().clip(GalleryDesign.cover).clickable(role = Role.Button, onClick = onOpen),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).clip(GalleryDesign.cover)
                .background(GalleryPalette.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            if (cover != null) {
                AsyncImage(
                    rememberMediaImageModel(cover, thumbnail = true),
                    onSuccess = galleryImageReady(),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                GalleryIcon(fallback, null, Modifier.size(32.dp), tint = GalleryPalette.onSurfaceVariant)
            }
        }
        Column(Modifier.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            GalleryText(
                name,
                style = GalleryTypography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (count != null) {
                GalleryText(
                    pluralStringResource(R.plurals.album_item_count, count, count),
                    style = GalleryTypography.bodySmall,
                    color = GalleryPalette.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MediaAlbum.displayName(): String =
    name.ifBlank { stringResource(R.string.device_storage) }

@Composable
private fun EmptyLibrary(icon: ImageVector, label: Int, modifier: Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        GalleryIcon(icon, contentDescription = null, modifier = Modifier.size(40.dp))
        GalleryText(stringResource(label), style = GalleryTypography.bodyLarge)
    }
}

@Composable
private fun LibraryLoadingIndicator() {
    val colors = GalleryPalette
    val rotation by rememberInfiniteTransition(label = "Library loading").animateFloat(
        initialValue = -90f,
        targetValue = 270f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "Loading arc",
    )
    Canvas(
        Modifier.size(28.dp)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate },
    ) {
        val width = 2.dp.toPx()
        val bounds = Size(size.width - width, size.height - width)
        val start = Offset(width / 2, width / 2)
        val stroke = Stroke(width, cap = StrokeCap.Round)
        drawArc(colors.outlineVariant, 0f, 360f, false, start, bounds, style = stroke)
        drawArc(colors.onSurface, rotation, 88f, false, start, bounds, style = stroke)
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    filter: PhotoFilter,
    onFilter: (PhotoFilter) -> Unit,
    onClose: () -> Unit,
) {
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val colors = GalleryPalette
    Column(
        Modifier.fillMaxWidth().statusBarsPadding()
            .padding(horizontal = GalleryDesign.inset, vertical = GalleryDesign.gap),
        verticalArrangement = Arrangement.spacedBy(GalleryDesign.gap),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.weight(1f).heightIn(min = GalleryDesign.target).galleryPill().padding(start = GalleryDesign.separation, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GalleryIcon(Icons.Filled.Search, null, Modifier.size(20.dp), tint = colors.onSurfaceVariant)
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f).focusRequester(focus),
                    singleLine = true,
                    textStyle = GalleryTypography.bodyLarge.copy(color = colors.onSurface),
                    cursorBrush = SolidColor(colors.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                    decorationBox = { field ->
                        Box {
                            if (query.isEmpty()) {
                                GalleryText(stringResource(R.string.search_photos_hint), color = colors.onSurfaceVariant)
                            }
                            field()
                        }
                    },
                )
                if (query.isNotEmpty()) {
                    GalleryIconButton(
                        Icons.AutoMirrored.Filled.Backspace,
                        stringResource(R.string.clear_search),
                        { onQueryChange("") },
                        size = GalleryDesign.button,
                        tint = colors.onSurfaceVariant,
                    )
                }
            }
            RoundTool(Icons.Filled.Close, android.R.string.cancel, GalleryDesign.target) {
                keyboard?.hide()
                onClose()
            }
        }
        Row(Modifier.galleryPill().selectableGroup().padding(4.dp)) {
            PhotoFilter.entries.forEach { option ->
                GallerySegment(
                    stringResource(option.label), filter == option, { onFilter(option) },
                    Modifier.weight(1f), horizontalPadding = 4.dp,
                )
            }
        }
    }
    LaunchedEffect(focus) { focus.requestFocus() }
}

@Composable
private fun SortMenu(sort: MediaSort, onSort: (MediaSort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        RoundTool(Icons.AutoMirrored.Filled.Sort, R.string.sort_media) { expanded = true }
        GalleryPopup(expanded, onClose = { expanded = false }, preferBelow = true) {
            MediaSortField.entries.forEach { field ->
                val label = when (field) {
                    MediaSortField.Date -> R.string.sort_date
                    MediaSortField.Name -> R.string.sort_name
                    MediaSortField.Size -> R.string.sort_size
                }
                GalleryMenuRow(stringResource(label), selected = sort.field == field) {
                    onSort(sort.copy(field = field))
                    expanded = false
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(GalleryPalette.outlineVariant))
            listOf(true, false).forEach { descending ->
                GalleryMenuRow(
                    stringResource(if (descending) R.string.sort_descending else R.string.sort_ascending),
                    selected = sort.descending == descending,
                ) {
                    onSort(sort.copy(descending = descending))
                    expanded = false
                }
            }
        }
    }
}

@Composable
private fun SelectionBar(count: Int, onClear: () -> Unit, onSelectAll: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding()
            .padding(horizontal = GalleryDesign.inset, vertical = GalleryDesign.gap)
            .galleryPill().padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToolIcon(Icons.Filled.Close, R.string.clear_selection, onClear)
        GalleryText(
            pluralStringResource(R.plurals.selection_count, count, count),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = GalleryTypography.titleLarge,
        )
        ToolIcon(Icons.Filled.SelectAll, R.string.select_all, onSelectAll)
    }
}

@Composable
private fun SelectionDock(
    busy: Boolean,
    onShare: () -> Unit,
    removeFavorites: Boolean,
    onFavorite: () -> Unit,
    onTrash: () -> Unit,
    onDetails: () -> Unit,
    onRestore: (() -> Unit)?,
) {
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(GalleryDesign.gap),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.galleryPill().padding(horizontal = 8.dp)) {
            if (onRestore != null) {
                ToolIcon(Icons.Filled.Restore, R.string.restore_media, onRestore)
            } else {
                ToolIcon(Icons.Filled.Share, R.string.share_selection, onShare)
                ToolIcon(
                    if (removeFavorites) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    if (removeFavorites) R.string.viewer_remove_favorite else R.string.viewer_add_favorite,
                    onFavorite,
                )
            }
            ToolIcon(Icons.Filled.Info, R.string.selection_details, onDetails)
        }
        if (onRestore == null) {
            Box {
                RoundTool(Icons.Filled.MoreVert, R.string.more_actions) { menu = true }
                GalleryPopup(menu, onClose = { menu = false }) {
                    GalleryMenuRow(
                        label = stringResource(R.string.trash_selection),
                        icon = Icons.Filled.DeleteOutline,
                        enabled = !busy,
                        destructive = true,
                    ) {
                        menu = false
                        onTrash()
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolIcon(icon: ImageVector, label: Int, onClick: () -> Unit) {
    GalleryIconButton(icon, stringResource(label), onClick)
}

@Composable
private fun LibraryDock(tab: GaleryTab, onTabSelected: (GaleryTab) -> Unit) {
    val indicator by animateFloatAsState(
        if (tab == GaleryTab.AllMedia) 0f else 1f,
        GalleryDesign.effects(),
        label = "Library tab indicator",
    )
    val fill = GalleryPalette.surfaceContainerHighest
    Row(
        Modifier.galleryPill()
            .selectableGroup()
            .width(IntrinsicSize.Max)
            .padding(4.dp)
            .drawBehind {
                val segmentWidth = size.width / 2
                val progress = if (layoutDirection == LayoutDirection.Ltr) indicator else 1f - indicator
                drawRoundRect(
                    color = fill,
                    topLeft = Offset(segmentWidth * progress, 0f),
                    size = Size(segmentWidth, size.height),
                    cornerRadius = CornerRadius(size.height / 2),
                )
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(GaleryTab.AllMedia, GaleryTab.Albums).forEach { destination ->
            val selected = tab == destination || destination == GaleryTab.Albums && tab == GaleryTab.Favorites
            val label = if (destination == GaleryTab.AllMedia) R.string.photos_title else destination.title
            GallerySegment(
                stringResource(label),
                selected,
                { onTabSelected(destination) },
                Modifier.weight(1f),
                showIndicator = false,
            )
        }
    }
}

@Composable
private fun LayoutTool(layout: MediaLayout, onClick: () -> Unit) {
    if (layout == MediaLayout.Mosaic) {
        RoundTool(Icons.Filled.GridView, R.string.show_grid, onClick = onClick)
    } else {
        RoundTool(Icons.AutoMirrored.Filled.ViewQuilt, R.string.show_mosaic, onClick = onClick)
    }
}

@Composable
private fun RoundTool(icon: ImageVector, label: Int, size: androidx.compose.ui.unit.Dp = GalleryDesign.button, onClick: () -> Unit) {
    GalleryIconButton(icon, stringResource(label), onClick, Modifier.galleryPill(), size)
}

@Composable
private fun PhotoFilterMenu(selected: PhotoFilter, onSelect: (PhotoFilter) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        RoundTool(Icons.Filled.FilterList, R.string.filter_media, GalleryDesign.dock) { open = true }
        GalleryPopup(open, onClose = { open = false }) {
            PhotoFilter.entries.forEach { filter ->
                GalleryMenuRow(
                    label = stringResource(filter.label),
                    selected = selected == filter,
                ) {
                    onSelect(filter)
                    open = false
                }
            }
        }
    }
}

@Composable
private fun GalleryPopup(
    open: Boolean,
    onClose: () -> Unit,
    preferBelow: Boolean = false,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val visibility = remember { MutableTransitionState(false) }
    visibility.targetState = open
    if (visibility.isIdle && !open) return
    val margin = with(androidx.compose.ui.platform.LocalDensity.current) { 12.dp.roundToPx() }
    val position = remember(margin, preferBelow) {
        object : androidx.compose.ui.window.PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: androidx.compose.ui.unit.IntRect,
                windowSize: androidx.compose.ui.unit.IntSize,
                layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                popupContentSize: androidx.compose.ui.unit.IntSize,
            ): androidx.compose.ui.unit.IntOffset {
                val right = anchorBounds.right - popupContentSize.width
                val start = if (layoutDirection == androidx.compose.ui.unit.LayoutDirection.Ltr) right else anchorBounds.left
                val above = anchorBounds.top - popupContentSize.height - margin
                val below = anchorBounds.bottom + margin
                return androidx.compose.ui.unit.IntOffset(
                    start.coerceIn(margin, (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin)),
                    (if (preferBelow && below + popupContentSize.height <= windowSize.height - margin) below else if (above >= margin) above else below)
                        .coerceIn(margin, (windowSize.height - popupContentSize.height - margin).coerceAtLeast(margin)),
                )
            }
        }
    }
    androidx.compose.ui.window.Popup(
        popupPositionProvider = position,
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.PopupProperties(focusable = open),
    ) {
        AnimatedVisibility(
            visibleState = visibility,
            enter = fadeIn(GalleryDesign.effects()) + scaleIn(GalleryDesign.spatial(), initialScale = 0.96f),
            exit = fadeOut(GalleryDesign.effects()) + scaleOut(GalleryDesign.spatial(), targetScale = 0.98f),
        ) {
            Column(
                Modifier.widthIn(min = 164.dp, max = 240.dp)
                    .galleryPill(GalleryDesign.panel)
                    .padding(vertical = 4.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun GalleryMenuRow(
    label: String,
    icon: ImageVector? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = GalleryPalette
    val color = when {
        !enabled -> colors.onSurfaceVariant
        destructive -> colors.error
        selected -> colors.onSurface
        else -> colors.onSurface
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) GalleryIcon(icon, null, Modifier.size(20.dp), tint = color)
        GalleryText(label, Modifier.weight(1f), color = color)
        if (selected) GalleryIcon(Icons.Filled.Check, null, Modifier.size(18.dp), tint = color)
    }
}
