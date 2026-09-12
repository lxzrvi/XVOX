package com.xvox.music.features.home.allsongs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.features.home.HomePresentation
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.roundToInt

@Composable
fun AllSongsHeader(total: Int, selectedCount: Int = 0) {
    Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(if (selectedCount > 0) "$selectedCount selected" else "All Songs",
            color = XvoxTheme.colors.primaryAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("$total songs", color = XvoxTheme.colors.mutedText, fontSize = 10.sp)
    }
}

/** Emit separate lazy pages instead of composing the entire library inside one LazyColumn item. */
fun LazyListScope.allSongsItems(
    songs: List<Song>, plans: List<XvoxMosaicPagePlan>, config: HomePresentation,
    currentSongId: Long?, isPlaying: Boolean, selectedSongIds: Set<Long>,
    onSongClick: (Song) -> Unit, onSongLongClick: (Song) -> Unit, onPrefetch: (Int) -> Unit
) {
    item(key = "all_header", contentType = "all_header") { AllSongsHeader(songs.size, selectedSongIds.size) }
    if (songs.isEmpty()) item(key = "all_empty") {
        Text("No songs match your library filters", color = XvoxTheme.colors.secondaryText,
            fontSize = 13.sp, modifier = Modifier.padding(16.dp))
    } else if (config.direction == "horizontal") item(key = "all_horizontal", contentType = "mosaic_pager") {
        HorizontalSongPages(songs, plans, config, currentSongId, isPlaying, selectedSongIds,
            onSongClick, onSongLongClick, onPrefetch)
    } else items(plans, key = { "all_page_${it.startIndex}" }, contentType = { "mosaic_page" }) { plan ->
        XvoxSongGridPage(songs, plan, config, currentSongId, isPlaying, selectedSongIds,
            onSongClick, onSongLongClick, compact = true,
            modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, bottom = 6.dp))
    }
}

@Composable
fun HorizontalSongPages(
    songs: List<Song>, plans: List<XvoxMosaicPagePlan>, config: HomePresentation,
    currentSongId: Long?, isPlaying: Boolean, selectedSongIds: Set<Long>,
    onSongClick: (Song) -> Unit, onSongLongClick: (Song) -> Unit,
    onPrefetch: (Int) -> Unit = {}, modifier: Modifier = Modifier
) {
    val state = rememberLazyListState()
    val prefetch by rememberUpdatedState(onPrefetch)
    LaunchedEffect(plans) {
        snapshotFlow { state.firstVisibleItemIndex }.distinctUntilChanged().collect { index ->
            plans.getOrNull(index + 1)?.let { prefetch(it.startIndex) }
        }
    }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        // Row count is fixed for the pager, so every page — including the last — fills the frame.
        val rows = remember(songs.size, config.rows, config.style) {
            if (config.style == "mosaic2") mosaicRows(songs.size, config.rows) else config.rows.coerceIn(3, 8)
        }
        val renderConfig = remember(config, rows) { config.copy(rows = rows) }
        val pageWidth = maxWidth - 12.dp
        val unitHeight = (pageWidth - 18.dp) / 4 + 38.dp
        val pageHeight = unitHeight * rows + 6.dp * (rows - 1)
        // Original native horizontal scrolling. No diagonal/free-pan gesture interceptor.
        LazyRow(state = state, modifier = Modifier.fillMaxWidth().height(pageHeight),
            contentPadding = PaddingValues(horizontal = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(plans, key = { it.startIndex }, contentType = { "mosaic_page" }) { plan ->
                XvoxSongGridPage(songs, plan, renderConfig, currentSongId, isPlaying, selectedSongIds,
                    onSongClick, onSongLongClick, compact = false, modifier = Modifier.width(pageWidth))
            }
        }
    }
}

/** Shared by the real Home and the scale-correct settings preview. */
@Composable
fun XvoxSongGridPage(
    songs: List<Song>, plan: XvoxMosaicPagePlan, config: HomePresentation,
    currentSongId: Long?, isPlaying: Boolean, selectedSongIds: Set<Long>,
    onSongClick: (Song) -> Unit, onSongLongClick: (Song) -> Unit,
    compact: Boolean, modifier: Modifier = Modifier
) {
    val uniform = config.style == "uniform"
    val classic = config.style == "mosaic1"
    // Paged mode fills the whole grid; flowing mode lets a short page claim fewer rows.
    val page = remember(songs, plan, config.rows, uniform, classic, compact) {
        buildMosaicPage(songs, plan, config.rows, uniform, classic, fillRows = !compact)
    }
    val usedRows = remember(page, compact, config.rows) {
        if (compact) page.tiles.maxOfOrNull { it.y + it.height } ?: 0f else config.rows.toFloat()
    }
    // Click handlers are hoisted once per page so a scroll never rebuilds every card's lambda.
    val click by rememberUpdatedState(onSongClick)
    val longClick by rememberUpdatedState(onSongLongClick)

    BoxWithConstraints(modifier) {
        val gap = 6.dp
        val unitWidth = (maxWidth - gap * 3) / 4
        val unitHeight = unitWidth + 38.dp
        val height = unitHeight * usedRows + gap * (usedRows - 1).coerceAtLeast(0f)
        val density = androidx.compose.ui.platform.LocalDensity.current
        val stepX = with(density) { (unitWidth + gap).toPx() }
        val stepY = with(density) { (unitHeight + gap).toPx() }

        Box(Modifier.fillMaxWidth().height(height)) {
            page.tiles.forEach { tile ->
                key(tile.song.id) {
                    // Offsets are applied in the layout phase, so scrolling never triggers a
                    // recomposition of the card itself — this is what removes the grid jitter.
                    val tileModifier = Modifier
                        .offset { IntOffset((stepX * tile.x).roundToInt(), (stepY * tile.y).roundToInt()) }
                        .size(
                            unitWidth * tile.width + gap * (tile.width - 1),
                            unitHeight * tile.height + gap * (tile.height - 1)
                        )
                    val song = tile.song
                    val isCurrent = currentSongId == song.id
                    if (uniform || (classic && tile.width == 1f && tile.height == 1f)) {
                        XvoxAllSongCard(song, isCurrent, isCurrent && isPlaying,
                            onClick = { click(song) }, onLongClick = { longClick(song) },
                            modifier = tileModifier, selected = song.id in selectedSongIds)
                    } else {
                        XvoxAllSongMosaicCard(song, tile.width, tile.height,
                            onClick = { click(song) }, onLongClick = { longClick(song) },
                            modifier = tileModifier, current = isCurrent,
                            playing = isCurrent && isPlaying,
                            selected = song.id in selectedSongIds, styleIndex = tile.style, classic = classic)
                    }
                }
            }
        }
    }
}
