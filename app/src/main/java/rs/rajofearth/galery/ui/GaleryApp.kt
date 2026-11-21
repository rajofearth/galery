package rs.rajofearth.galery.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import rs.rajofearth.galery.R
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
    GaleryTheme {
        var selectedTab by remember { mutableIntStateOf(0) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    actions = {
                        IconButton(onClick = {}) {
                            Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search))
                        }
                    },
                )
            },
            bottomBar = {
                NavigationBar {
                    tabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.labelRes)) },
                        )
                    }
                }
            },
        ) { innerPadding ->
            EmptyView(
                tab = tabs[selectedTab],
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp),
            )
        }
    }
}

@Composable
private fun EmptyView(tab: GaleryTab, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
        )
        Text(text = stringResource(tab.emptyTextRes))
    }
}
