package rs.rajofearth.galery.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import rs.rajofearth.galery.ui.theme.GalleryPalette
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.findViewTreeLifecycleOwner
import java.io.IOException
import rs.rajofearth.galery.data.GaleryPreferences
import rs.rajofearth.galery.data.MediaItem
import rs.rajofearth.galery.data.MediaLayout
import rs.rajofearth.galery.data.MediaSort
import rs.rajofearth.galery.data.MediaSortField
import rs.rajofearth.galery.data.MediaStoreMediaRepository
import rs.rajofearth.galery.data.filteredBy
import rs.rajofearth.galery.data.sortedBy
import rs.rajofearth.galery.data.toAlbums

@Composable
fun GaleryApp() {
    val context = LocalContext.current
    val lifecycle = LocalView.current.findViewTreeLifecycleOwner()?.lifecycle
    var foreground by remember(lifecycle) {
        mutableStateOf(lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) == true)
    }
    val updates = rememberGalleryUpdates(foreground)
    val permissionHistory = remember(context) {
        context.getSharedPreferences("media-access", Context.MODE_PRIVATE)
    }
    var accessGranted by remember(context) { mutableStateOf(mediaAccessGranted(context)) }
    var askedBefore by remember(permissionHistory) {
        mutableStateOf(permissionHistory.getBoolean("asked", false))
    }
    var refresh by remember { mutableIntStateOf(0) }
    val requestAccess = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        accessGranted = mediaAccessGranted(context)
        refresh++
    }
    fun askForAccess() {
        if (!askedBefore) {
            permissionHistory.edit { putBoolean("asked", true) }
            askedBefore = true
        }
        requestAccess.launch(mediaPermissions())
    }

    DisposableEffect(lifecycle, context) {
        var hasResumed = false
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                foreground = true
                val currentAccess = mediaAccessGranted(context)
                if (hasResumed || currentAccess != accessGranted) refresh++
                accessGranted = currentAccess
                hasResumed = true
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                foreground = false
            }
        }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer) }
    }

    val accessState = MediaAccessDecision.forResult(accessGranted, askedBefore)
    LaunchedEffect(accessState) {
        if (accessState == MediaAccessState.RequestFirstTime) askForAccess()
    }

    MediaAccessGate(
        state = accessState,
        onRequestAccess = ::askForAccess,
        onOpenSettings = {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null),
                ),
            )
        },
    ) {
        GaleryLibrary(refresh, updates, onRefresh = { refresh++ })
    }
}

private const val TRASH_ROUTE = "<trash>"

private data class ViewerSession(val items: List<MediaItem>, val initialIndex: Int) {
    val initialItem: MediaItem get() = items[initialIndex]
}

@Composable
private fun GaleryLibrary(refresh: Int, updates: GalleryUpdates, onRefresh: () -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) { MediaStoreMediaRepository(context.applicationContext) }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val prefs = remember(context) { GaleryPreferences(context.applicationContext) }
    var layout by remember(prefs) { mutableStateOf(prefs.layout) }
    var sort by remember(prefs) { mutableStateOf(prefs.sort) }
    var photoFilter by rememberSaveable { mutableStateOf(PhotoFilter.All) }
    var searchFilter by rememberSaveable { mutableStateOf(PhotoFilter.All) }
    var timelineJump by remember { mutableStateOf<String?>(null) }
    var favoriteKeys by remember(prefs) { mutableStateOf(prefs.favoriteKeys) }
    var media by remember { mutableStateOf<List<MediaItem>?>(null) }
    var trashItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var trashCount by remember { mutableStateOf<Int?>(null) }
    var loadFailed by remember { mutableStateOf(false) }
    var tab by rememberSaveable { mutableStateOf(GaleryTab.AllMedia) }
    var albumKey by rememberSaveable { mutableStateOf<String?>(null) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var selectedKeys by remember { mutableStateOf(emptySet<String>()) }
    var viewer by remember { mutableStateOf<ViewerSession?>(null) }
    var viewerItem by remember { mutableStateOf<MediaItem?>(null) }
    var closeRequested by remember { mutableStateOf(false) }
    val visibility = remember { MutableTransitionState(false) }
    val scrollStates = rememberSaveableStateHolder()
    val albumScroll = rememberLazyGridState()

    LaunchedEffect(repository, refresh) {
        loadFailed = false
        try {
            val items = repository.loadMedia()
            media = items
            favoriteKeys = prefs.pruneFavorites(items)
        } catch (_: IOException) {
            loadFailed = true
        } catch (_: SecurityException) {
            loadFailed = true
        }
    }

    val trashOpen = tab == GaleryTab.Albums && albumKey == TRASH_ROUTE
    val showTrashCount = tab == GaleryTab.Albums && albumKey == null && !searching && !settingsOpen
    LaunchedEffect(repository, trashOpen, showTrashCount, refresh) {
        try {
            if (trashOpen) {
                trashItems = repository.loadMedia(trashed = true)
                trashCount = trashItems.size
            } else if (showTrashCount) {
                trashCount = repository.countTrashedMedia()
            }
        } catch (_: IOException) {
            if (trashOpen) trashItems = emptyList()
            trashCount = null
        } catch (_: SecurityException) {
            if (trashOpen) trashItems = emptyList()
            trashCount = null
        }
    }
    val sortedMedia = remember(media, sort) {
        val items = media.orEmpty()
        if (sort == MediaSort()) items else items.sortedBy(sort)
    }
    val albumIndex = remember(sortedMedia) { lazy { sortedMedia.toAlbums() } }
    val albums = if (tab == GaleryTab.Albums && !trashOpen) {
        albumIndex.value
    } else {
        emptyList()
    }
    val album = remember(albums, albumKey) { albums.firstOrNull { it.key == albumKey } }
    val unfiltered = remember(tab, sortedMedia, album, favoriteKeys, trashOpen, trashItems, sort) {
        when (tab) {
            GaleryTab.AllMedia -> sortedMedia
            GaleryTab.Albums -> if (trashOpen) trashItems.sortedBy(sort) else album?.items ?: sortedMedia
            GaleryTab.Favorites -> sortedMedia.filter { it.key in favoriteKeys }
        }
    }
    val filter = if (searching) searchFilter else if (tab == GaleryTab.AllMedia) photoFilter else PhotoFilter.All
    val photos = remember(sortedMedia, photoFilter) { sortedMedia.withPhotoFilter(photoFilter) }
    val photosRoot = tab == GaleryTab.AllMedia && !searching
    val visibleItems = if (photosRoot) photos else remember(unfiltered, query, filter) {
        unfiltered.withPhotoFilter(filter).filteredBy(query)
    }
    val dated = sort.field == MediaSortField.Date
    val photoContent = remember(photos, layout, dated) { lazy { MediaTileContent(photos, layout, dated) } }
    val favoriteCover = if (tab == GaleryTab.Albums) remember(sortedMedia, favoriteKeys) {
        if (favoriteKeys.isEmpty()) null else sortedMedia.firstOrNull { it.key in favoriteKeys }
    } else null
    val visibleKeys = remember(visibleItems) {
        lazy { visibleItems.mapTo(HashSet(visibleItems.size)) { it.key } }
    }
    val selectedVisibleKeys = remember(visibleKeys, selectedKeys) {
        if (selectedKeys.all { it in visibleKeys.value }) selectedKeys else selectedKeys.intersect(visibleKeys.value)
    }

    fun selectedMedia() = visibleItems.filter { it.key in selectedVisibleKeys }

    fun closeSearch() {
        query = ""
        searching = false
        searchFilter = PhotoFilter.All
    }

    val backdrop = rememberGlassBackdrop(enabled = visibleItems.isNotEmpty() && viewer == null)
    val viewerGlass = remember { mutableStateOf<(() -> GlassFrame?)?>(null) }
    val actions = rememberMediaSelectionActions(
        frameAtDraw = { if (viewer == null) backdrop.frame else viewerGlass.value?.invoke() },
        onMediaChanged = {
            selectedKeys = emptySet()
            if (viewer != null) closeRequested = true
            onRefresh()
        },
    )

    BackHandler(enabled = viewer == null && (selectedVisibleKeys.isNotEmpty() || searching || albumKey != null || tab == GaleryTab.Favorites)) {
        when {
            selectedVisibleKeys.isNotEmpty() -> selectedKeys = emptySet()
            searching -> closeSearch()
            tab == GaleryTab.Favorites -> tab = GaleryTab.Albums
            else -> albumKey = null
        }
    }

    val routeKey = if (tab == GaleryTab.Albums) albumKey ?: "albums" else tab.name
    val sectionKey = if (searching) "$routeKey:search" else routeKey
    var activeTiles by remember { mutableStateOf<MediaTilesState?>(null) }
    LaunchedEffect(closeRequested) {
        if (!closeRequested) return@LaunchedEffect
        val itemKey = viewerItem?.key
        if (itemKey != null && itemKey != viewer?.initialItem?.key && activeTiles?.reveal(itemKey) == true) {
            withFrameNanos {}
        }
        visibility.targetState = false
    }
    LaunchedEffect(visibility.isIdle, visibility.currentState, visibility.targetState) {
        if (closeRequested && visibility.isIdle && !visibility.targetState) {
            viewer = null
            viewerItem = null
            closeRequested = false
        }
    }

    if (settingsOpen) {
        GallerySettings(updates) { settingsOpen = false }
        return
    }

    Box {
        Box(Modifier.fillMaxSize()) {
            GaleryScreen(
                tab = tab,
                onOpenSettings = { settingsOpen = true },
                onTabSelected = {
                    if (it != tab) {
                        tab = it
                        albumKey = null
                        selectedKeys = emptySet()
                        closeSearch()
                    } else if (it == GaleryTab.Albums && albumKey != null) {
                        albumKey = null
                        selectedKeys = emptySet()
                        closeSearch()
                    }
                },
                album = album,
                onAlbumSelected = { albumKey = it.key },
                onExitAlbum = {
                    albumKey = null
                    closeSearch()
                },
                albums = albums,
                albumScroll = albumScroll,
                favoriteCover = favoriteCover,
                favoriteCount = favoriteKeys.size,
                trashOpen = trashOpen,
                trashCount = trashCount,
                onOpenTrash = if (actions.canTrash) ({ albumKey = TRASH_ROUTE; closeSearch() }) else null,
                onRestore = { actions.restore(selectedMedia()) },
                timelineItems = visibleItems,
                photoFilter = photoFilter,
                onPhotoFilter = { photoFilter = it },
                searchFilter = searchFilter,
                onSearchFilter = { searchFilter = it },
                onTimelineJump = {
                    layout = MediaLayout.Grid
                    prefs.layout = layout
                    timelineJump = it.key
                },
                searching = searching,
                onSearch = { searching = true },
                onCloseSearch = ::closeSearch,
                query = query,
                onQueryChange = { query = it },
                layout = layout,
                onToggleLayout = {
                    layout = if (layout == MediaLayout.Mosaic) MediaLayout.Grid else MediaLayout.Mosaic
                    prefs.layout = layout
                },
                sort = sort,
                onSort = {
                    sort = it
                    prefs.sort = it
                },
                selectionCount = selectedVisibleKeys.size,
                selectionBusy = actions.isBusy,
                onClearSelection = { selectedKeys = emptySet() },
                onSelectAll = { selectedKeys = visibleKeys.value },
                onShare = { actions.share(selectedMedia()) },
                removeFavorites = favoriteKeys.containsAll(selectedVisibleKeys),
                onFavoriteSelection = {
                    favoriteKeys = prefs.toggleFavorites(selectedVisibleKeys)
                    selectedKeys = emptySet()
                },
                onTrash = { actions.trash(selectedMedia()) },
                onDetails = { actions.details(selectedMedia(), favoriteKeys) },
                hasMedia = media != null,
                backdrop = backdrop,
                loadFailed = loadFailed,
                onRetry = onRefresh,
                hasItems = visibleItems.isNotEmpty(),
            ) { modifier, contentPadding ->
                scrollStates.SaveableStateProvider(sectionKey) {
                    val content = if (photosRoot) photoContent.value else remember(visibleItems, layout, dated) {
                        MediaTileContent(visibleItems, layout, dated)
                    }
                    val tiles = rememberMediaTilesState(content)
                    DisposableEffect(tiles) {
                        activeTiles = tiles
                        onDispose { if (activeTiles === tiles) activeTiles = null }
                    }
                    LaunchedEffect(timelineJump, layout) {
                        val itemKey = timelineJump ?: return@LaunchedEffect
                        if (layout == MediaLayout.Grid) {
                            tiles.jumpToDate(itemKey)
                            timelineJump = null
                        }
                    }
                    LaunchedEffect(query, searchFilter) {
                        if (searching) tiles.resetScroll()
                    }
                    MediaTiles(
                        state = tiles,
                        selectedKeys = selectedVisibleKeys,
                        mediaKeys = { visibleItems.map { it.key } },
                        onRangeSelectionChange = { if (viewer == null) selectedKeys = it },
                        onOpen = { item ->
                            if (viewer == null) {
                                val index = visibleItems.indexOfFirst { it.key == item.key }
                                if (index >= 0) {
                                    focusManager.clearFocus()
                                    keyboard?.hide()
                                    viewer = ViewerSession(visibleItems, index)
                                    viewerItem = item
                                    visibility.targetState = true
                                }
                            }
                        },
                        onSelect = { item ->
                            if (viewer == null) {
                                selectedKeys = if (item.key in selectedKeys) {
                                    selectedKeys - item.key
                                } else {
                                    selectedKeys + item.key
                                }
                            }
                        },
                        modifier = modifier,
                        contentPadding = contentPadding,
                    )
                }
            }

            val effects = GalleryDesign.effects<Float>()
            AnimatedVisibility(
                visibleState = visibility,
                enter = fadeIn(effects) + scaleIn(GalleryDesign.spatial(), initialScale = 0.96f),
                exit = fadeOut(effects) + scaleOut(GalleryDesign.spatial(), targetScale = 0.98f),
                label = "Viewer",
            ) {
                val session = viewer ?: return@AnimatedVisibility
                MediaViewer(
                    items = session.items,
                    initialIndex = session.initialIndex,
                    onDismiss = { closeRequested = true },
                    visible = visibility.targetState,
                    onPageChanged = { viewerItem = it },
                    onGlassSource = { viewerGlass.value = it },
                    isFavorite = viewerItem?.key in favoriteKeys,
                    onToggleFavorite = if (trashOpen) null else ({
                        viewerItem?.let { favoriteKeys = prefs.toggleFavorite(it.key) }
                    }),
                    onShare = { actions.share(listOf(it)) },
                    onTrash = if (trashOpen || !actions.canTrash) null else ({ actions.trash(listOf(it)) }),
                    onRestore = if (trashOpen) ({ actions.restore(listOf(it)); closeRequested = true }) else null,
                    onDetails = { actions.details(listOf(it), favoriteKeys) },
                )
            }
        }
    }
}

private fun List<MediaItem>.withPhotoFilter(filter: PhotoFilter): List<MediaItem> = when (filter) {
    PhotoFilter.All -> this
    PhotoFilter.Photos -> filterNot { it.isVideo }
    PhotoFilter.Videos -> filter { it.isVideo }
}

private fun fullMediaPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

private fun mediaPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        fullMediaPermissions() + Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
    } else {
        fullMediaPermissions()
    }

private fun mediaAccessGranted(context: Context): Boolean =
    fullMediaPermissions().all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
