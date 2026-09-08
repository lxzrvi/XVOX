package com.xvox.music.features.settings.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
 * The whole Home screen, at the real phone's proportions, shrunk to fit.
 *
 * It is not a scroll window onto a partial layout: the entire screen — header, every enabled
 * section in its configured order, the mosaic at the chosen grid, playlists at their chosen card
 * style and height, and the bottom bar — is laid out at full device size and then scaled down by
 * [UniformPreview]. What is on screen is exactly what Home will look like.
 */
@Composable
fun HomeSettingsPreview(state: SettingsState) {
    val colors = XvoxTheme.colors
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    val config = HomePresentation(
        state.homeLayoutStyle, state.homeScrollDirection, state.homeHorizontalRows,
        state.hideRecentlyPlayed, state.recentsPlacement, state.homeMerge,
        state.homeSectionOrder, state.homeHiddenSections, state.playlistStyle,
        state.splitHideCollection, state.playlistLongHeight
    )
    val sections = HomeSections.visible(config)

    // Real generators, real row count: the tile shapes here are the ones Home will produce.
    val tiles = remember(state.homeLayoutStyle, state.homeHorizontalRows) {
        val rows = state.homeHorizontalRows.coerceIn(3, 8)
        val seeded = Random(4801)
        when (state.homeLayoutStyle) {
            "uniform" -> regularSpecs(4, rows * 4)
            "mosaic2" -> generateMosaicSpecs(4, rows, (rows * 2.4f).toInt().coerceIn(rows, rows * 4 - 1), seeded)
            else -> generateClassicMosaicSpecs(4, rows, (rows * 3.1f).toInt().coerceIn(rows, rows * 4 - 1), seeded)
        }
    }

    SettingsPreviewFrame("Home · full screen") {
        UniformPreview(screenWidth, screenHeight, Modifier.fillMaxWidth().heightIn(max = 260.dp)) {
            Column(Modifier.fillMaxSize().background(colors.background)) {
                // Status strip + profile header, exactly where the shell puts them.
                Row(
                    Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 30.dp, bottom = 10.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Box(Modifier.size(34.dp).clip(RoundedCornerShape(17.dp)).background(colors.card))
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.size(width = 96.dp, height = 32.dp).clip(RoundedCornerShape(16.dp)).background(colors.card))
                }

                Column(Modifier.weight(1f).fillMaxWidth()) {
                    sections.forEach { section ->
                        PreviewSectionTitle(HomeSections.label(section))
                        when (section) {
                            HomeSections.ALL -> PreviewMosaic(tiles, state.homeHorizontalRows, state.homeScrollDirection == "horizontal")
                            HomeSections.RECENT -> PreviewRecentRow()
                            HomeSections.PLAYLISTS -> PreviewPlaylists(state)
                            else -> PreviewSongRows(3)
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }

                // Bottom navigation bar.
                Box(
                    Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 12.dp)
                        .height(52.dp).clip(RoundedCornerShape(26.dp)).background(colors.cardElevated)
                )
            }
        }
    }
}

@Composable
private fun PreviewSectionTitle(label: String) {
    Text(
        label, color = XvoxTheme.colors.primaryAccent, fontSize = 16.sp,
        fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
    )
}

@Composable
private fun PreviewMosaic(tiles: List<Spec>, rows: Int, horizontal: Boolean) {
    val colors = XvoxTheme.colors
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 6.dp)) {
        val gap = 6.dp
        // Identical geometry to XvoxSongGridPage, so proportions match Home exactly.
        val unitWidth = (maxWidth - gap * 3) / 4
        val unitHeight = unitWidth + 38.dp
        val used = if (horizontal) rows.toFloat() else (tiles.maxOfOrNull { it.y + it.height } ?: 0f)
        Canvas(Modifier.fillMaxWidth().height(unitHeight * used + gap * (used - 1).coerceAtLeast(0f))) {
            val gx = gap.toPx(); val w = unitWidth.toPx(); val h = unitHeight.toPx()
            tiles.forEach { tile ->
                val at = Offset(tile.x * (w + gx), tile.y * (h + gx))
                val area = Size(tile.width * w + (tile.width - 1) * gx, tile.height * h + (tile.height - 1) * gx)
                drawRoundRect(colors.card, at, area, CornerRadius(12.dp.toPx()))
                drawRoundRect(
                    colors.primaryAccent.copy(alpha = .30f), at + Offset(6.dp.toPx(), 6.dp.toPx()),
                    Size((area.width - 12.dp.toPx()).coerceAtLeast(1f), (area.height - 44.dp.toPx()).coerceAtLeast(1f)),
                    CornerRadius(9.dp.toPx())
                )
            }
        }
    }
}

@Composable
private fun PreviewRecentRow() {
    val colors = XvoxTheme.colors
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) {
            Box(Modifier.weight(1f).height(120.dp).clip(RoundedCornerShape(14.dp)).background(colors.card))
        }
    }
}

@Composable
private fun PreviewSongRows(count: Int) {
    val colors = XvoxTheme.colors
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) {
            Box(Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(14.dp)).background(colors.card))
        }
    }
}

@Composable
private fun PreviewPlaylists(state: SettingsState) {
    val colors = XvoxTheme.colors
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 6.dp)) {
        if (state.playlistStyle == "long") {
            val height = if (state.playlistLongHeight > 0) state.playlistLongHeight.dp else (maxWidth - 6.dp) / 2 + 35.dp
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(2) {
                    Box(Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(16.dp)).background(colors.card))
                }
            }
        } else {
            val size = (maxWidth - 6.dp) / 2
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(2) {
                    Box(Modifier.width(size).height(size + 35.dp).clip(RoundedCornerShape(16.dp)).background(colors.card))
                }
            }
        }
    }
}
