package com.xvox.music.features.settings.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import kotlin.math.roundToInt
import com.xvox.music.features.home.HomeSections
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.components.HomeSettingsPreview
import com.xvox.music.features.settings.components.SettingsChoiceRow
import com.xvox.music.features.settings.components.SettingsToggle

/**
 * Home settings — including Playlists, which belong here because they are a Home section.
 * Labels only: the preview above shows what each choice does.
 */
@Composable
fun HomeSettingsSection(
    state: SettingsState, viewModel: SettingsViewModel
) {
    val colors = XvoxTheme.colors
    com.xvox.music.features.settings.components.PinnedSettingsEditor(preview = { HomeSettingsPreview(state) }, controls = {
        Label("Card style")
        SettingsChoiceRow(listOf("mosaic1" to "Mosaic 1", "mosaic2" to "Mosaic 2", "uniform" to "One size"),
            state.homeLayoutStyle, viewModel::setHomeLayoutStyle)

        Label("Scroll")
        SettingsChoiceRow(listOf("horizontal" to "Horizontal", "vertical" to "Vertical"),
            state.homeScrollDirection, viewModel::setHomeScrollDirection)

        if (state.homeScrollDirection == "horizontal") {
            Label("Grid")
            SettingsChoiceRow((3..8).map { it.toString() to "4 × $it" }, state.homeHorizontalRows.toString()) {
                viewModel.setHomeHorizontalRows(it.toInt())
            }
        }

        Label("Sort")
        SettingsChoiceRow(listOf("A-Z", "Z-A", "Random").map { it to it }, state.sortOrder, viewModel::setSortOrder)

        Label("Playlist cards")
        SettingsChoiceRow(listOf("cards" to "Grid", "long" to "Long"), state.playlistStyle, viewModel::setPlaylistStyle)

        if (state.playlistStyle == "long") {
            Label(if (state.playlistLongHeight <= 0) "Long card height · Auto"
                else "Long card height · ${state.playlistLongHeight} dp")
            com.xvox.music.features.settings.components.XvoxThinLineSlider(
                value = state.playlistLongHeight.coerceAtLeast(60).toFloat(),
                onValueChange = { v -> viewModel.setPlaylistLongHeight(v.roundToInt()) },
                valueRange = 60f..240f,
                defaultValue = 0f,
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text("Auto (original size)",
                    color = if (state.playlistLongHeight <= 0) colors.background else colors.primaryAccent,
                    fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                        .background(if (state.playlistLongHeight <= 0) colors.primaryAccent else colors.card)
                        .xvoxPressScale { viewModel.setPlaylistLongHeight(0) }
                        .padding(horizontal = 10.dp, vertical = 6.dp))
            }
        }

        SettingsToggle("Hide recents", null, state.hideRecentlyPlayed, viewModel::setHideRecentlyPlayed)

        Label("Recents position")
        SettingsChoiceRow(listOf("top" to "Top", "bottom" to "Bottom"), state.recentsPlacement, viewModel::setRecentsPlacement)

        SettingsToggle("Merge sections", null, state.homeMerge, viewModel::setHomeMerge)

        if (state.homeMerge) {
            Label("Order")
            state.homeSectionOrder.forEachIndexed { index, section ->
                val visible = section !in state.homeHiddenSections &&
                    (section != HomeSections.RECENT || !state.hideRecentlyPlayed)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(visible, onCheckedChange = {
                        viewModel.setHomeSectionVisible(section, it)
                    })
                    Text(HomeSections.label(section), color = colors.primaryText, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Icon(painterResource(R.drawable.ic_xvox_arrow_left), "Move ${HomeSections.label(section)} up",
                        tint = if (index > 0) colors.primaryAccent else colors.mutedText,
                        modifier = Modifier.size(40.dp).xvoxPressScale(enabled = index > 0) { viewModel.moveHomeSection(index, index - 1) }
                            .padding(12.dp).rotate(90f))
                    Icon(painterResource(R.drawable.ic_xvox_arrow_left), "Move ${HomeSections.label(section)} down",
                        tint = if (index < state.homeSectionOrder.lastIndex) colors.primaryAccent else colors.mutedText,
                        modifier = Modifier.size(40.dp).xvoxPressScale(enabled = index < state.homeSectionOrder.lastIndex) {
                            viewModel.moveHomeSection(index, index + 1)
                        }.padding(12.dp).rotate(-90f))
                }
            }
        }

    })
}

@Composable
private fun Label(text: String) {
    Text(text, color = XvoxTheme.colors.primaryAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
}
