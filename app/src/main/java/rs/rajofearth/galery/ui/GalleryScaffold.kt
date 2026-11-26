package rs.rajofearth.galery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import rs.rajofearth.galery.ui.theme.GalleryPalette

private enum class GallerySlot { Top, Bottom, Media }

@Composable
internal fun GalleryScaffold(
    modifier: Modifier,
    topBar: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val systemPadding = WindowInsets.systemBars.union(WindowInsets.displayCutout).asPaddingValues()
    SubcomposeLayout(modifier.background(GalleryPalette.background)) { constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val top = subcompose(GallerySlot.Top, topBar).map { it.measure(loose) }
        val bottom = subcompose(GallerySlot.Bottom, bottomBar).map { it.measure(loose) }
        val padding = PaddingValues(
            start = systemPadding.calculateStartPadding(layoutDirection),
            top = top.maxOfOrNull { it.height }?.toDp() ?: systemPadding.calculateTopPadding(),
            end = systemPadding.calculateEndPadding(layoutDirection),
            bottom = bottom.maxOfOrNull { it.height }?.toDp() ?: systemPadding.calculateBottomPadding(),
        )
        val media = subcompose(GallerySlot.Media) { content(padding) }.map { it.measure(loose) }
        layout(constraints.maxWidth, constraints.maxHeight) {
            media.forEach { it.place(0, 0) }
            top.forEach { it.place(0, 0) }
            bottom.forEach { it.place(0, constraints.maxHeight - it.height) }
        }
    }
}
