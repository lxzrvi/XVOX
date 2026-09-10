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
import com.xvox.music.features.home.HomeSections
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.components.SettingsAccordionItem
import com.xvox.music.features.settings.components.SettingsChoiceRow
import com.xvox.music.features.settings.components.SettingsControlsEditor
import com.xvox.music.features.settings.components.SettingsToggle

@Composable
fun HomeSettingsSection(
    state: SettingsState,
    viewModel: SettingsViewModel
) {
    val colors = XvoxTheme.colors
    var expandedGroup by remember { mutableStateOf<String?>("Layout") }

    fun toggle(group: String) {
        expandedGroup = if (expandedGroup == group) null else group
    }

    SettingsControlsEditor(controls = {
        SettingsAccordionItem(
            title = "Card style",
            expanded = expandedGroup == "Card style",
            onToggle = { toggle("Card style") }
        ) {
            SettingsChoiceRow(
                listOf("mosaic1" to "Mosaic 1", "mosaic2" to "Mosaic 2", "uniform" to "One size"),
                state.homeLayoutStyle,
                viewModel::setHomeLayoutStyle
            )
        }

        SettingsAccordionItem(
            title = "Scroll & Grid",
            expanded = expandedGroup == "Scroll & Grid",
            onToggle = { toggle("Scroll & Grid") }
        ) {
            Label("Direction")
            SettingsChoiceRow(
                listOf("horizontal" to "Horizontal", "vertical" to "Vertical"),
                state.homeScrollDirection,
                viewModel::setHomeScrollDirection
            )

            if (state.homeScrollDirection == "horizontal") {
                Spacer(Modifier.height(8.dp))
                Label("Rows")
                SettingsChoiceRow(
                    (3..8).map { it.toString() to "4 × $it" },
                    state.homeHorizontalRows.toString()
                ) {
                    viewModel.setHomeHorizontalRows(it.toInt())
                }
            }
        }

        SettingsAccordionItem(
            title = "Sort",
            expanded = expandedGroup == "Sort",
            onToggle = { toggle("Sort") }
        ) {
            SettingsChoiceRow(
                listOf("A-Z", "Z-A", "Random").map { it to it },
                state.sortOrder,
                viewModel::setSortOrder
            )
        }

        SettingsAccordionItem(
            title = "Playlist cover",
            expanded = expandedGroup == "Playlist cover",
            onToggle = { toggle("Playlist cover") }
        ) {
            Label("Orientation")
            SettingsChoiceRow(
                listOf("vertical" to "Vertical", "horizontal" to "Horizontal"),
                state.playlistCardOrientation,
                viewModel::setPlaylistCardOrientation
            )
        }

        SettingsAccordionItem(
            title = "Recently played",
            expanded = expandedGroup == "Recently played",
            onToggle = { toggle("Recently played") }
        ) {
            SettingsToggle(
                "Hide recents",
                null,
                state.hideRecentlyPlayed,
                viewModel::setHideRecentlyPlayed
            )
            Spacer(Modifier.height(8.dp))
            Label("Position")
            SettingsChoiceRow(
                listOf("top" to "Top", "bottom" to "Bottom"),
                state.recentsPlacement,
                viewModel::setRecentsPlacement
            )
        }

        SettingsAccordionItem(
            title = "Merge sections",
            expanded = expandedGroup == "Merge sections",
            onToggle = { toggle("Merge sections") }
        ) {
            SettingsToggle("Merge sections", null, state.homeMerge, viewModel::setHomeMerge)

            if (state.homeMerge) {
                Spacer(Modifier.height(8.dp))
                Label("Order")
                state.homeSectionOrder.forEachIndexed { index, section ->
                    val visible = section !in state.homeHiddenSections &&
                        (section != HomeSections.RECENT || !state.hideRecentlyPlayed)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(visible, onCheckedChange = {
                            viewModel.setHomeSectionVisible(section, it)
                        })
                        Text(HomeSections.label(section), color = colors.primaryText, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        Icon(
                            painterResource(R.drawable.ic_xvox_arrow_left),
                            "Move ${HomeSections.label(section)} up",
                            tint = if (index > 0) colors.primaryAccent else colors.mutedText,
                            modifier = Modifier
                                .size(40.dp)
                                .xvoxPressScale(enabled = index > 0) { viewModel.moveHomeSection(index, index - 1) }
                                .padding(12.dp)
                                .rotate(90f)
                        )
                        Icon(
                            painterResource(R.drawable.ic_xvox_arrow_left),
                            "Move ${HomeSections.label(section)} down",
                            tint = if (index < state.homeSectionOrder.lastIndex) colors.primaryAccent else colors.mutedText,
                            modifier = Modifier
                                .size(40.dp)
                                .xvoxPressScale(enabled = index < state.homeSectionOrder.lastIndex) {
                                    viewModel.moveHomeSection(index, index + 1)
                                }
                                .padding(12.dp)
                                .rotate(-90f)
                        )
                    }
                }
            }
        }
    })
}

@Composable
private fun Label(text: String) {
    Text(
        text,
        color = XvoxTheme.colors.primaryAccent,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}
