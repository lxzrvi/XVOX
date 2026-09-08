package com.xvox.music.features.settings.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.features.home.HomePresentation
import com.xvox.music.features.home.HomeSections
import com.xvox.music.features.home.allsongs.*
import com.xvox.music.features.settings.SettingsState
import kotlin.random.Random

/** A compact schematic, not a second live Home screen or a source of playback work. */
@Composable
fun HomeSettingsPreview(state: SettingsState) {
    val colors = XvoxTheme.colors
    val config = HomePresentation(state.homeLayoutStyle, state.homeScrollDirection, state.homeHorizontalRows,
        state.hideRecentlyPlayed, state.recentsPlacement, state.homeMerge, state.homeSectionOrder, state.homeHiddenSections, state.playlistStyle, state.splitHideCollection)
    val tiles = remember(state.homeLayoutStyle, state.homeHorizontalRows) {
        when (state.homeLayoutStyle) {
            "uniform" -> regularSpecs(4, state.homeHorizontalRows * 4)
            "mosaic2" -> generateMosaicSpecs(4, state.homeHorizontalRows, state.homeHorizontalRows * 2 + 1, Random(81))
            else -> generateClassicMosaicSpecs(4, state.homeHorizontalRows, state.homeHorizontalRows * 3, Random(32))
        }
    }
    SettingsPreviewFrame("Home · layout") {
        Text("${if (state.homeScrollDirection == "horizontal") "Horizontal ↔" else "Vertical ↕"} · 4 × ${state.homeHorizontalRows} grid",
            color = colors.secondaryText, fontSize = 11.sp)
        HomeSections.visible(config).forEach { section ->
            Text(HomeSections.label(section), color = colors.primaryText, fontSize = 10.sp)
            if (section == HomeSections.ALL) {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val gap = 4.dp
                    val cell = (maxWidth - gap * 3) / 4
                    val cellH = cell * 1.25f
                    val naturalHeight = cellH * state.homeHorizontalRows + gap * (state.homeHorizontalRows - 1)
                    val scale = minOf(1f, 240.dp / naturalHeight)
                    Canvas(Modifier.fillMaxWidth().height(naturalHeight * scale)) {
                        val gx = gap.toPx() * scale; val w = cell.toPx() * scale; val h = cellH.toPx() * scale
                        val startX = (size.width - (w * 4 + gx * 3)) / 2
                        for (tile in tiles) {
                            val pos = Offset(startX + tile.x * (w + gx), tile.y * (h + gx))
                            val area = Size(tile.width * w + (tile.width - 1) * gx, tile.height * h + (tile.height - 1) * gx)
                            drawRoundRect(colors.primaryAccent.copy(alpha = .17f), pos, area, CornerRadius(7.dp.toPx() * scale))
                            drawRoundRect(colors.primaryAccent.copy(alpha = .28f), pos + Offset(4.dp.toPx() * scale, 4.dp.toPx() * scale),
                                Size((area.width - 8.dp.toPx() * scale).coerceAtLeast(1f), (area.height - 22.dp.toPx() * scale).coerceAtLeast(1f)), CornerRadius(4.dp.toPx() * scale))
                        }
                    }
                }
            } else Canvas(Modifier.fillMaxWidth().height(if (section == HomeSections.PLAYLISTS) 44.dp else 28.dp)) {
                val count = if (section == HomeSections.PLAYLISTS) 2 else 1
                val gap = 6.dp.toPx(); val width = (size.width - gap * (count - 1)) / count
                repeat(count) { i -> drawRoundRect(colors.primaryAccent.copy(alpha = .16f), Offset(i * (width + gap), 0f),
                    Size(width, size.height), CornerRadius(6.dp.toPx())) }
            }
        }
        Text("Layout only; grid rows keep their proportions. No live songs or full-screen preview.", color = colors.mutedText, fontSize = 10.sp)
    }
}
