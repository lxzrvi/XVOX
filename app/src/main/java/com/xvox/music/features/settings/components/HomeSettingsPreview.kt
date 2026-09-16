package com.xvox.music.features.settings.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.features.home.HomePresentation
import com.xvox.music.features.home.HomeSections
import com.xvox.music.features.home.allsongs.*
import com.xvox.music.features.settings.SettingsState
import kotlin.random.Random

/**
 * Scrollable Home Layout preview: user can scroll through the entire Home screen preview
 * to see every section (All songs, Recently played, Playlists, Liked) as configured.
 */
@Composable
fun HomeSettingsPreview(state: SettingsState) {
    val colors = XvoxTheme.colors
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    val config = HomePresentation(
        style = state.homeLayoutStyle,
        direction = state.homeScrollDirection,
        rows = state.homeHorizontalRows,
        hideRecents = state.hideRecentlyPlayed,
        recentsPlacement = state.recentsPlacement,
        merge = state.homeMerge,
        order = state.homeSectionOrder,
        hidden = state.homeHiddenSections,
        playlistStyle = state.playlistStyle,
        hideSplit = state.splitHideCollection,
        playlistLongHeight = state.playlistLongHeight,
        playlistCardOrientation = state.playlistCardOrientation
    )
    val sections = HomeSections.visible(config)

    val tiles = remember(state.homeLayoutStyle, state.homeHorizontalRows) {
        val rows = state.homeHorizontalRows.coerceIn(3, 8)
        val seeded = Random(4801)
        when (state.homeLayoutStyle) {
            "uniform" -> regularSpecs(4, rows * 4)
            "mosaic2" -> generateMosaicSpecs(4, rows, (rows * 2.4f).toInt().coerceIn(rows, rows * 4 - 1), seeded)
            else -> generateClassicMosaicSpecs(4, rows, (rows * 3.1f).toInt().coerceIn(rows, rows * 4 - 1), seeded)
        }
    }

    SettingsPreviewFrame("Home · scroll to preview") {
        Box(
            Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.background)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                // Header mockup
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Box(Modifier.size(24.dp).clip(RoundedCornerShape(12.dp)).background(colors.primaryAccent.copy(alpha = 0.3f)))
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.width(64.dp).height(8.dp).clip(RoundedCornerShape(4.dp)).background(colors.cardElevated))
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.size(width = 54.dp, height = 20.dp).clip(RoundedCornerShape(10.dp)).background(colors.card))
                }

                sections.forEach { section ->
                    PreviewSectionTitle(HomeSections.label(section))
                    when (section) {
                        HomeSections.ALL -> PreviewMosaic(tiles, state.homeHorizontalRows, state.homeScrollDirection == "horizontal")
                        HomeSections.RECENT -> PreviewRecentRow()
                        HomeSections.PLAYLISTS -> PreviewPlaylists(state)
                        else -> PreviewSongRows(3)
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun PreviewSectionTitle(label: String) {
    Text(
        label, color = XvoxTheme.colors.primaryAccent, fontSize = 11.sp,
        fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun PreviewMosaic(tiles: List<Spec>, rows: Int, horizontal: Boolean) {
    val colors = XvoxTheme.colors
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val gap = 4.dp
        val unitWidth = (maxWidth - gap * 3) / 4
        val unitHeight = unitWidth + 16.dp
        val used = if (horizontal) rows.toFloat().coerceAtMost(4f) else (tiles.maxOfOrNull { it.y + it.height } ?: 0f).coerceAtMost(5f)
        Canvas(Modifier.fillMaxWidth().height(unitHeight * used + gap * (used - 1).coerceAtLeast(0f))) {
            val gx = gap.toPx(); val w = unitWidth.toPx(); val h = unitHeight.toPx()
            tiles.forEach { tile ->
                val at = Offset(tile.x * (w + gx), tile.y * (h + gx))
                val area = Size(tile.width * w + (tile.width - 1) * gx, tile.height * h + (tile.height - 1) * gx)
                drawRoundRect(colors.card, at, area, CornerRadius(8.dp.toPx()))
                drawRoundRect(
                    colors.primaryAccent.copy(alpha = .30f), at + Offset(3.dp.toPx(), 3.dp.toPx()),
                    Size((area.width - 6.dp.toPx()).coerceAtLeast(1f), (area.height - 18.dp.toPx()).coerceAtLeast(1f)),
                    CornerRadius(6.dp.toPx())
                )
            }
        }
    }
}

@Composable
private fun PreviewRecentRow() {
    val colors = XvoxTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(3) {
            Box(Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(8.dp)).background(colors.card))
        }
    }
}

@Composable
private fun PreviewSongRows(count: Int) {
    val colors = XvoxTheme.colors
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(count) {
            Box(Modifier.fillMaxWidth().height(28.dp).clip(RoundedCornerShape(6.dp)).background(colors.card))
        }
    }
}

@Composable
private fun PreviewPlaylists(state: SettingsState) {
    val colors = XvoxTheme.colors
    val isHorizontal = state.playlistCardOrientation == "horizontal"
    if (isHorizontal) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(8.dp)).background(colors.card))
        }
    } else {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(2) {
                Box(Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(8.dp)).background(colors.card))
            }
        }
    }
}
