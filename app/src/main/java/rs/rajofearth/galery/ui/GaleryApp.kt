package rs.rajofearth.galery.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import rs.rajofearth.galery.R
import rs.rajofearth.galery.data.MediaItem
import rs.rajofearth.galery.data.MediaStoreMediaRepository
import rs.rajofearth.galery.ui.theme.GaleryTheme

private data class GaleryTab(
    val labelRes: Int,
    val icon: ImageVector,
    val emptyTextRes: Int,
)

private val tabs = listOf(
    GaleryTab(R.string.all_media, Icons.Filled.PhotoLibrary, R.string.all_media_empty),
    GaleryTab(R.string.albums, Icons.Filled.PhotoAlbum, R.string.albums_empty),
    GaleryTab(R.string.favorites, Icons.Filled.FavoriteBorder, R.string.favorites_empty),
)

@Composable
fun GaleryApp() {
    val context = LocalContext.current
    val repository = remember { MediaStoreMediaRepository(context.applicationContext) }

    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    var askedBefore by rememberSaveable { mutableStateOf(false) }
    var accessGranted by rememberSaveable { mutableStateOf(mediaAccessGranted(context)) }

    val requestAccess = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        askedBefore = true
        accessGranted = results.isNotEmpty() && results.values.all { it }
    }
    val openSettings = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        accessGranted = mediaAccessGranted(context)
    }

    val accessState = MediaAccessDecision.forResult(accessGranted, askedBefore)

    LaunchedEffect(accessState) {
        if (accessState == MediaAccessState.RequestFirstTime) {
            requestAccess.launch(mediaPermissions())
        }
    }

    var mediaItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    LaunchedEffect(accessState, repository) {
        if (accessState == MediaAccessState.Granted) {
            mediaItems = repository.loadMedia()
        }
    }

    GaleryTheme {
        MediaAccessGate(
            state = accessState,
            onRequestAccess = { requestAccess.launch(mediaPermissions()) },
            onOpenSettings = { openSettings.launch(settingsIntent(context)) },
        ) {
            GaleryScreen(
                selectedTab = tabs[selectedIndex],
                onTabSelected = { tab -> selectedIndex = tabs.indexOf(tab) },
                mediaItems = mediaItems,
            )
        }
    }
}

@Composable
private fun GaleryScreen(
    selectedTab: GaleryTab,
    onTabSelected: (GaleryTab) -> Unit,
    mediaItems: List<MediaItem>,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLargeEmphasized,
                    )
                },
                actions = {
                    IconButton(onClick = {}, enabled = false) {
                        Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search))
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        when (selectedTab) {
            tabs[0] -> AllMediaContent(
                items = mediaItems,
                modifier = contentModifier.padding(horizontal = 16.dp),
            )

            else -> EmptyView(
                icon = selectedTab.icon,
                text = stringResource(selectedTab.emptyTextRes),
                modifier = contentModifier.padding(horizontal = 24.dp),
            )
        }
    }
}

@Composable
private fun AllMediaContent(items: List<MediaItem>, modifier: Modifier = Modifier) {
    if (items.isEmpty()) {
        EmptyView(
            icon = tabs[0].icon,
            text = stringResource(R.string.all_media_no_items),
            modifier = modifier,
        )
        return
    }

    LazyColumn(modifier = modifier) {
        items(items, key = { it.id }) { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (item.isVideo) Icons.Filled.Movie else Icons.Filled.Image,
                    contentDescription = null,
                )
                Spacer(Modifier.width(16.dp))
                Text(text = item.displayName, style = MaterialTheme.typography.bodyLarge)
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun EmptyView(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(text = text)
    }
}

private fun mediaPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

private fun mediaAccessGranted(context: Context): Boolean =
    mediaPermissions().all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

private fun settingsIntent(context: Context): Intent =
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.parse("package:${context.packageName}"),
    )

@Preview
@Composable
private fun AllMediaPreview() {
    GaleryTheme {
        GaleryScreen(selectedTab = tabs[0], onTabSelected = {}, mediaItems = emptyList())
    }
}

@Preview
@Composable
private fun AlbumsPreview() {
    GaleryTheme {
        GaleryScreen(selectedTab = tabs[1], onTabSelected = {}, mediaItems = emptyList())
    }
}

@Preview
@Composable
private fun FavoritesDarkPreview() {
    GaleryTheme(darkTheme = true, dynamicColor = false) {
        GaleryScreen(selectedTab = tabs[2], onTabSelected = {}, mediaItems = emptyList())
    }
}
