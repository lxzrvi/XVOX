package com.xvox.music.features.home.allsongs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.features.home.HomePresentation
import com.xvox.music.features.home.XvoxGridArtworkSize
import com.xvox.music.features.home.XvoxHomeSectionHeading
import com.xvox.music.features.home.XvoxRecentArtworkSize
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.roundToInt

@Composable
fun AllSongsHeader(total: Int, selectedCount: Int = 0) {
    XvoxHomeSectionHeading(
        title = if (selectedCount > 0) "$selectedCount selected" else "All Songs",
        subtitle = "$total songs"
    )
}

/** Match the exact card request used by a page; prefetching never substitutes a smaller bitmap. */
private fun allSongsPageArtworkRequestSize(
    songs: List<Song>,
    plan: XvoxMosaicPagePlan,
    config: HomePresentation,
    compact: Boolean,
    columns: Int
): Int {
    if (config.style == "uniform") return XvoxGridArtworkSize
    val page = buildMosaicPage(
        songs = songs,
        plan = plan,
        rows = config.rows,
        isUniform = false,
        mosaicOne = config.style == "mosaic1",
        fillRows = !compact,
        cols = columns
    )
    return if (page.tiles.any { it.width != 1f || it.height != 1f }) {
        XvoxRecentArtworkSize
    } else {
        XvoxGridArtworkSize
    }
}

/** Emit separate lazy pages instead of composing the entire library inside one LazyColumn item. */
fun LazyListScope.allSongsItems(
    songs: List<Song>,
    plans: List<XvoxMosaicPagePlan>,
    config: HomePresentation,
    currentSongId: Long?,
    isPlaying: Boolean,
    selectedSongIds: Set<Long>,
    onSongClick: (Song) -> Unit,
    onSongLongClick: (Song) -> Unit,
    onPrefetch: (startIndex: Int, count: Int, requestSize: Int) -> Unit
) {
    item(key = "all_header", contentType = "all_header") {
        val firstPlan = plans.firstOrNull()
        val firstLandscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val firstColumns = config.columnsFor(firstLandscape)
        val initialRequestSize = remember(firstPlan, songs, config, firstColumns) {
            firstPlan?.let { plan ->
                allSongsPageArtworkRequestSize(
                    songs = songs,
                    plan = plan,
                    config = config,
                    compact = config.direction != "horizontal",
                    columns = firstColumns
                )
            } ?: XvoxGridArtworkSize
        }
        LaunchedEffect(firstPlan?.startIndex, firstPlan?.songCount, initialRequestSize) {
            firstPlan?.let { plan ->
                onPrefetch(plan.startIndex, plan.songCount, initialRequestSize)
            }
        }
        AllSongsHeader(songs.size, selectedSongIds.size)
    }

    if (songs.isEmpty()) {
        item(key = "all_empty") {
            Text(
                "No songs match your library filters",
                color = XvoxTheme.colors.secondaryText,
                fontSize = 13.sp,
                modifier = Modifier.padding(16.dp)
            )
        }
    } else if (config.direction == "horizontal") {
        item(key = "all_horizontal", contentType = "mosaic_pager") {
            HorizontalSongPages(
                songs, plans, config, currentSongId, isPlaying, selectedSongIds,
                onSongClick, onSongLongClick, onPrefetch
            )
        }
    } else {
        // Vertical pages are composed just ahead of the viewport. Use that visibility signal to
        // warm only the following page at the exact 256px grid quality, avoiding decode stalls
        // without lowering artwork resolution or preloading an unbounded library.
        itemsIndexed(
            plans,
            key = { _, plan -> "all_page_${plan.startIndex}" },
            contentType = { _, _ -> "mosaic_page" }
        ) { pageIndex, plan ->
            val nextPlan = plans.getOrNull(pageIndex + 1)
            val nextLandscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val nextColumns = config.columnsFor(nextLandscape)
            val nextRequestSize = remember(nextPlan, songs, config, nextColumns) {
                nextPlan?.let { candidate ->
                    allSongsPageArtworkRequestSize(
                        songs = songs,
                        plan = candidate,
                        config = config,
                        compact = true,
                        columns = nextColumns
                    )
                } ?: XvoxGridArtworkSize
            }
            // Uniform cards all request the same 256px source, so warm up to three upcoming
            // shallow pages in one bounded job. Mosaic keeps its exact per-page class to avoid
            // trading a larger visible cover for a lower decode just to prefetch farther ahead.
            val nextWindowCount = remember(pageIndex, plans, config.style, nextPlan) {
                if (config.style == "uniform") {
                    plans.drop(pageIndex + 1).take(3).sumOf { it.songCount }
                } else {
                    nextPlan?.songCount ?: 0
                }
            }
            LaunchedEffect(nextPlan?.startIndex, nextWindowCount, nextRequestSize) {
                nextPlan?.let { candidate ->
                    onPrefetch(candidate.startIndex, nextWindowCount.coerceAtLeast(candidate.songCount), nextRequestSize)
                }
            }
            XvoxSongGridPage(
                songs, plan, config, currentSongId, isPlaying, selectedSongIds,
                onSongClick, onSongLongClick, compact = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, end = 6.dp, bottom = 6.dp)
            )
        }
    }
}

@Composable
fun HorizontalSongPages(
    songs: List<Song>,
    plans: List<XvoxMosaicPagePlan>,
    config: HomePresentation,
    currentSongId: Long?,
    isPlaying: Boolean,
    selectedSongIds: Set<Long>,
    onSongClick: (Song) -> Unit,
    onSongLongClick: (Song) -> Unit,
    onPrefetch: (startIndex: Int, count: Int, requestSize: Int) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val state = rememberLazyListState()
    val prefetch by rememberUpdatedState(onPrefetch)
    val isLandscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val columns = config.columnsFor(isLandscape)
    // A horizontal page can be taller than the viewport. Use the page's actual decode class so
    // its first visible covers come from cache at full requested resolution.
    val horizontalPrefetchSize = if (config.style == "uniform") XvoxGridArtworkSize else XvoxRecentArtworkSize

    LaunchedEffect(plans, horizontalPrefetchSize) {
        // Warm the page immediately after the trailing visible page, rather than waiting until it
        // becomes the first page. That keeps a fast horizontal fling decode-free at the exact
        // 256px card quality the grid renders.
        snapshotFlow { state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .distinctUntilChanged()
            .collect { lastVisible ->
                plans.getOrNull(lastVisible + 1)?.let { next ->
                    val count = if (config.style == "uniform") {
                        plans.drop(lastVisible + 1).take(3).sumOf { it.songCount }
                    } else {
                        next.songCount
                    }
                    prefetch(next.startIndex, count.coerceAtLeast(next.songCount), horizontalPrefetchSize)
                }
            }
    }

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val rows = remember(songs.size, config.rows, config.style) {
            if (config.style == "mosaic2") mosaicRows(songs.size, config.rows.coerceIn(3, 10)) else config.rows.coerceIn(3, 10)
        }
        val renderConfig = remember(config, rows) { config.copy(rows = rows) }
        val pageWidth = maxWidth - 12.dp
        // Dense five/six-column portrait layouts use a tighter tile gutter so artwork remains
        // visually clean inside its own card rather than leaving oversized seams.
        val denseColumns = !isLandscape && columns >= 5
        val gap = if (denseColumns) 4.dp else 6.dp
        val unitWidth = (pageWidth - gap * (columns - 1)) / columns
        // Keep broad three-column cards compact and reduce the metadata band again at dense
        // five/six-column counts so six columns never turn into unusually tall tiles.
        val unitHeight = if (isLandscape) unitWidth + 30.dp else unitWidth + when {
            columns >= 6 -> 24.dp
            columns >= 5 -> 30.dp
            columns < 4 -> 20.dp
            else -> 38.dp
        }
        val pageHeight = unitHeight * rows + gap * (rows - 1).coerceAtLeast(0)

        LazyRow(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .height(pageHeight),
            contentPadding = PaddingValues(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            items(plans, key = { it.startIndex }, contentType = { "mosaic_page" }) { plan ->
                XvoxSongGridPage(
                    songs, plan, renderConfig, currentSongId, isPlaying, selectedSongIds,
                    onSongClick, onSongLongClick, compact = false,
                    modifier = Modifier.width(pageWidth)
                )
            }
        }
    }
}

/** Shared by the real Home and the scale-correct settings preview. */
@Composable
fun XvoxSongGridPage(
    songs: List<Song>,
    plan: XvoxMosaicPagePlan,
    config: HomePresentation,
    currentSongId: Long?,
    isPlaying: Boolean,
    selectedSongIds: Set<Long>,
    onSongClick: (Song) -> Unit,
    onSongLongClick: (Song) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val isLandscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val columns = config.columnsFor(isLandscape)
    val uniform = config.style == "uniform"
    val classic = config.style == "mosaic1"

    val page = remember(songs, plan, config.rows, uniform, classic, compact, isLandscape, columns) {
        buildMosaicPage(songs, plan, config.rows, uniform, classic, fillRows = !compact, cols = columns)
    }

    val usedRows = remember(page, compact, config.rows) {
        if (compact) page.tiles.maxOfOrNull { it.y + it.height } ?: 0f else config.rows.toFloat()
    }

    val click by rememberUpdatedState(onSongClick)
    val longClick by rememberUpdatedState(onSongLongClick)

    BoxWithConstraints(modifier) {
        val denseColumns = !isLandscape && columns >= 5
        val gap = if (denseColumns) 4.dp else 6.dp
        val unitWidth = (maxWidth - gap * (columns - 1)) / columns
        // Keep broad three-column cards compact and reduce the metadata band again at dense
        // five/six-column counts so six columns never turn into unusually tall tiles.
        val unitHeight = if (isLandscape) unitWidth + 30.dp else unitWidth + when {
            columns >= 6 -> 24.dp
            columns >= 5 -> 30.dp
            columns < 4 -> 20.dp
            else -> 38.dp
        }
        val height = unitHeight * usedRows + gap * (usedRows - 1).coerceAtLeast(0f)
        val density = androidx.compose.ui.platform.LocalDensity.current
        val stepX = with(density) { (unitWidth + gap).toPx() }
        val stepY = with(density) { (unitHeight + gap).toPx() }

        Box(Modifier.fillMaxWidth().height(height)) {
            page.tiles.forEach { tile ->
                key(tile.song.id) {
                    val tileModifier = Modifier
                        .offset { IntOffset((stepX * tile.x).roundToInt(), (stepY * tile.y).roundToInt()) }
                        .size(
                            unitWidth * tile.width + gap * (tile.width - 1),
                            unitHeight * tile.height + gap * (tile.height - 1)
                        )
                    val song = tile.song
                    val isCurrent = currentSongId == song.id
                    if (uniform || (classic && tile.width == 1f && tile.height == 1f)) {
                        XvoxAllSongCard(
                            song = song,
                            current = isCurrent,
                            playing = isCurrent && isPlaying,
                            onClick = { click(song) },
                            onLongClick = { longClick(song) },
                            modifier = tileModifier,
                            selected = song.id in selectedSongIds,
                            dense = denseColumns
                        )
                    } else {
                        XvoxAllSongMosaicCard(
                            song = song,
                            widthUnits = tile.width,
                            heightUnits = tile.height,
                            onClick = { click(song) },
                            onLongClick = { longClick(song) },
                            modifier = tileModifier,
                            current = isCurrent,
                            playing = isCurrent && isPlaying,
                            selected = song.id in selectedSongIds,
                            styleIndex = tile.style,
                            classic = classic,
                            dense = denseColumns
                        )
                    }
                }
            }
        }
    }
}
