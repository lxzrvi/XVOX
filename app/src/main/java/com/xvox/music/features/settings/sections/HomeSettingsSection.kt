package com.xvox.music.features.settings.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.features.home.HomeSections
import com.xvox.music.features.home.HomeViewModel
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.components.HomeSettingsPreview
import com.xvox.music.features.settings.components.SettingsChoiceRow
import com.xvox.music.features.settings.components.SettingsToggle
import com.xvox.music.player.playback.MainPlayerViewModel

@Composable
fun HomeSettingsSection(
    state: SettingsState, viewModel: SettingsViewModel,
    homeViewModel: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    playerViewModel: MainPlayerViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val colors = XvoxTheme.colors
    val library by homeViewModel.state.collectAsState()
    val player by playerViewModel.state.collectAsState()
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HomeSettingsPreview(state, library, player)
        Text("Card style", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        SettingsChoiceRow(listOf("mosaic1" to "Mosaic 1", "mosaic2" to "Mosaic 2", "uniform" to "One Size"),
            state.homeLayoutStyle, viewModel::setHomeLayoutStyle)
        Text(when (state.homeLayoutStyle) {
            "mosaic1" -> "The original XVOX mosaic: familiar squares, portraits and wide cards."
            "mosaic2" -> "The new varied mosaic. Covers follow their cards—no discs, holes or overflow buttons."
            else -> "Equal-size artwork cards with consistent spacing."
        }, color = colors.secondaryText, fontSize = 11.sp)
        Text("Scroll direction", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        SettingsChoiceRow(listOf("horizontal" to "Horizontal", "vertical" to "Vertical"),
            state.homeScrollDirection, viewModel::setHomeScrollDirection)
        if (state.homeScrollDirection == "horizontal") {
            Text("Grid rows", color = colors.secondaryText, fontSize = 12.sp)
            SettingsChoiceRow((3..8).map { it.toString() to "4 × $it" }, state.homeHorizontalRows.toString()) {
                viewModel.setHomeHorizontalRows(it.toInt())
            }
        }
        SettingsChoiceRow(listOf("A-Z", "Z-A", "Random").map { it to it }, state.sortOrder, viewModel::setSortOrder)
        SettingsToggle("Hide Recents", "Remove Recently Played from Home", state.hideRecentlyPlayed, viewModel::setHideRecentlyPlayed)
        Text("Recents placement", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        SettingsChoiceRow(listOf("top" to "Top", "bottom" to "Bottom"), state.recentsPlacement, viewModel::setRecentsPlacement)
        Text("Top = before All Songs. Bottom = after All Songs.", color = colors.secondaryText, fontSize = 11.sp)
        SettingsToggle("Merge", "Bring Liked Songs and Playlists onto Home. Only Refresh remains in the top-right pill.",
            state.homeMerge, viewModel::setHomeMerge)
        if (state.homeMerge) {
            Text("Home order & visibility", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text("Checked sections are shown. Use the arrows to reorder them; the preview uses this exact order.",
                color = colors.secondaryText, fontSize = 11.sp)
            state.homeSectionOrder.forEachIndexed { index, section ->
                val visible = section !in state.homeHiddenSections && (section != HomeSections.RECENT || !state.hideRecentlyPlayed)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(visible, onCheckedChange = { viewModel.setHomeSectionVisible(section, it) })
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
    }
}
