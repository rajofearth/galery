package rs.rajofearth.galery.ui

import rs.rajofearth.galery.ui.theme.GalleryPalette
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import rs.rajofearth.galery.R
import rs.rajofearth.galery.data.MediaItem

@Composable
internal fun TimelineTransition(open: Boolean, content: @Composable (Boolean) -> Unit) {
    val offset = with(LocalDensity.current) { GalleryDesign.gap.roundToPx() }
    AnimatedContent(
        targetState = open,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            val direction = if (targetState) 1 else -1
            (fadeIn(GalleryDesign.effects()) + slideInVertically(GalleryDesign.spatial()) { direction * offset })
                .togetherWith(fadeOut(GalleryDesign.effects()) + slideOutVertically(GalleryDesign.spatial()) { -direction * offset })
        },
        label = "Timeline navigation",
    ) { content(it) }
}

@Composable
internal fun TimelineBrowser(
    items: List<MediaItem>,
    contentPadding: PaddingValues,
    onJumpToDay: (MediaItem) -> Unit,
    onClose: () -> Unit,
) {
    val backdrop = rememberGlassBackdrop()
    val locale = LocalLocale.current.platformLocale
    val index by produceState(initialValue = TimelineIndex(), items) {
        value = withContext(Dispatchers.Default) { buildTimelineIndex(items) { ensureActive() } }
    }
    var viewingYears by rememberSaveable { mutableStateOf(true) }
    var selectedYear by rememberSaveable { mutableStateOf<Int?>(null) }
    val yearsState = rememberLazyListState()
    val monthsState = rememberLazyListState()
    val months = selectedYear?.let { index.monthsByYear[it] } ?: index.months
    val visiblePeriods = if (viewingYears) index.years else months
    val scrollState = if (viewingYears) yearsState else monthsState
    val layoutDirection = LocalLayoutDirection.current
    val navPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val undated = stringResource(R.string.timeline_undated)
    val monthFormatter = remember(locale) {
        SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "yMMMM"), locale)
    }
    val yearFormatter = remember(locale) {
        SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "y"), locale)
    }

    fun showMonths(year: Int) {
        selectedYear = year
        monthsState.requestScrollToItem(0)
        viewingYears = false
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = viewingYears,
            modifier = Modifier.align(Alignment.TopCenter).widthIn(max = 560.dp)
                .fillMaxSize().glassBackdropSource(backdrop),
            transitionSpec = { fadeIn(GalleryDesign.effects()) togetherWith fadeOut(GalleryDesign.effects()) },
            label = "Timeline periods",
        ) { years ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = if (years) yearsState else monthsState,
                verticalArrangement = Arrangement.spacedBy(GalleryDesign.separation),
                contentPadding = PaddingValues(
                    start = contentPadding.calculateStartPadding(layoutDirection) + GalleryDesign.inset,
                    top = contentPadding.calculateTopPadding() + 4.dp,
                    end = contentPadding.calculateEndPadding(layoutDirection) + GalleryDesign.inset,
                    bottom = maxOf(navPadding + GalleryDesign.dock + GalleryDesign.gap * 2, contentPadding.calculateBottomPadding()),
                ),
            ) {
                items(if (years) index.years else months, key = { it.key }) { period ->
                    val label = remember(period.key, years, locale, undated) {
                        when {
                            period.year == UnknownYear -> undated
                            years -> yearFormatter.format(period.anchor.dateMillis)
                            else -> monthFormatter.format(period.anchor.dateMillis)
                        }
                    }
                    PeriodCover(period, label, years) {
                        if (years && period.year != UnknownYear) showMonths(period.year)
                        else onJumpToDay(period.anchor)
                    }
                }
            }
        }
        CompositionLocalProvider(LocalGalleryGlass provides backdrop) {
            Row(
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                    .padding(horizontal = GalleryDesign.inset, vertical = GalleryDesign.gap),
                horizontalArrangement = Arrangement.spacedBy(GalleryDesign.separation),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GalleryIconButton(
                    Icons.Filled.Close,
                    stringResource(R.string.timeline_close),
                    onClose,
                    Modifier.galleryPill(),
                    size = GalleryDesign.dock,
                )
                GalleryTabs(
                    listOf(stringResource(R.string.timeline_years), stringResource(R.string.timeline_months), stringResource(R.string.timeline_days)),
                    if (viewingYears) 0 else 1,
                    horizontalPadding = GalleryDesign.separation,
                    onSelect = { destination ->
                        when (destination) {
                            0 -> viewingYears = true
                            1 -> if (viewingYears) {
                                val year = visiblePeriods.getOrNull(scrollState.firstVisibleItemIndex)?.year
                                if (year == null || year == UnknownYear) {
                                    selectedYear = null
                                    monthsState.requestScrollToItem(0)
                                    viewingYears = false
                                } else showMonths(year)
                            }
                            2 -> {
                                val anchor = visiblePeriods.getOrNull(scrollState.firstVisibleItemIndex)?.anchor ?: items.firstOrNull()
                                if (anchor != null) onJumpToDay(anchor) else onClose()
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun PeriodCover(period: TimelinePeriod, label: String, yearCover: Boolean, onClick: () -> Unit) {
    val shape = GalleryDesign.cover
    Box(
        Modifier.fillMaxWidth().aspectRatio(if (yearCover) 1.10f else 1.55f)
            .clip(shape).background(GalleryPalette.surfaceContainer)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        AsyncImage(
            model = rememberMediaImageModel(period.cover, thumbnail = true),
            onSuccess = galleryImageReady(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier.fillMaxWidth().align(Alignment.BottomStart).background(
                Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))),
            ).padding(start = 16.dp, end = 16.dp, top = 36.dp, bottom = 16.dp),
        ) {
            BasicText(
                text = label,
                style = TextStyle(
                    color = Color.White,
                    fontSize = if (yearCover) 30.sp else 22.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }
    }
}

private const val UnknownYear = -1

private data class TimelinePeriod(
    val key: Int,
    val year: Int,
    val anchor: MediaItem,
    val cover: MediaItem,
)

private data class TimelineIndex(
    val years: List<TimelinePeriod> = emptyList(),
    val months: List<TimelinePeriod> = emptyList(),
    val monthsByYear: Map<Int, List<TimelinePeriod>> = emptyMap(),
)

private class PeriodItems(initial: MediaItem) {
    var newest: MediaItem = initial
        private set
    private var newestPhoto: MediaItem? = initial.takeUnless { it.isVideo }

    fun add(item: MediaItem) {
        if (item.dateMillis > newest.dateMillis) newest = item
        if (!item.isVideo && item.dateMillis > (newestPhoto?.dateMillis ?: Long.MIN_VALUE)) newestPhoto = item
    }

    fun period(key: Int, year: Int) = TimelinePeriod(key, year, newest, newestPhoto ?: newest)
}

private fun buildTimelineIndex(items: List<MediaItem>, checkActive: () -> Unit): TimelineIndex {
    val calendar = Calendar.getInstance()
    val years = mutableMapOf<Int, PeriodItems>()
    val months = mutableMapOf<Int, PeriodItems>()
    for (item in items) {
        checkActive()
        val timestamp = item.dateMillis
        val year: Int
        val month: Int
        if (timestamp > 0L) {
            calendar.timeInMillis = timestamp
            year = calendar.get(Calendar.YEAR)
            month = year * 12 + calendar.get(Calendar.MONTH)
        } else {
            year = UnknownYear
            month = UnknownYear
        }
        years.getOrPut(year) { PeriodItems(item) }.add(item)
        months.getOrPut(month) { PeriodItems(item) }.add(item)
    }
    val monthPeriods = months.toSortedMap(reverseOrder()).map { (key, period) ->
        period.period(key, if (key == UnknownYear) UnknownYear else key / 12)
    }
    return TimelineIndex(
        years = years.toSortedMap(reverseOrder()).map { (key, period) -> period.period(key, key) },
        months = monthPeriods,
        monthsByYear = monthPeriods.groupBy { it.year },
    )
}
