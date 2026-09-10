package com.xvox.music.features.settings.sections

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.features.home.HomeSections
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.components.SettingsAccordionItem
import com.xvox.music.features.settings.components.SettingsChoiceRow
import com.xvox.music.features.settings.components.SettingsControlsEditor
import com.xvox.music.features.settings.components.SettingsToggle
import com.xvox.music.features.settings.components.XvoxThinLineSlider
import kotlin.math.roundToInt

@Composable
fun HomeSettingsSection(
    state: SettingsState,
    viewModel: SettingsViewModel
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current
    var expandedGroup by remember { mutableStateOf<String?>("Card style") }

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

            Spacer(Modifier.height(10.dp))

            val currentHeight = if (state.playlistLongHeight == 0) 178 else state.playlistLongHeight
            Label("Height · $currentHeight dp")
            XvoxThinLineSlider(
                value = currentHeight.toFloat(),
                onValueChange = { viewModel.setPlaylistLongHeight(it.roundToInt()) },
                valueRange = 100f..260f,
                defaultValue = 178f
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
                Spacer(Modifier.height(10.dp))
                Label("Sections order & visibility")
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    state.homeSectionOrder.forEachIndexed { index, section ->
                        val visible = section !in state.homeHiddenSections &&
                            (section != HomeSections.RECENT || !state.hideRecentlyPlayed)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.cardElevated)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = visible,
                                onCheckedChange = {
                                    haptics.tap()
                                    viewModel.setHomeSectionVisible(section, it)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = colors.primaryAccent,
                                    uncheckedColor = colors.mutedText,
                                    checkmarkColor = colors.background
                                )
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = HomeSections.label(section),
                                color = if (visible) colors.primaryText else colors.mutedText,
                                fontSize = 13.sp,
                                fontWeight = if (visible) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            // 6 Dots drag / reorder affordance
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .xvoxPressScale(enabled = index > 0) {
                                            haptics.tap()
                                            viewModel.moveHomeSection(index, index - 1)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    SixDotsIcon(tint = if (index > 0) colors.primaryAccent else colors.mutedText.copy(alpha = 0.35f))
                                }
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .xvoxPressScale(enabled = index < state.homeSectionOrder.lastIndex) {
                                            haptics.tap()
                                            viewModel.moveHomeSection(index, index + 1)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    SixDotsIcon(tint = if (index < state.homeSectionOrder.lastIndex) colors.primaryAccent else colors.mutedText.copy(alpha = 0.35f))
                                }
                            }
                        }
                    }
                }
            }
        }
    })
}

@Composable
private fun SixDotsIcon(tint: androidx.compose.ui.graphics.Color) {
    Canvas(Modifier.size(width = 14.dp, height = 18.dp)) {
        val radius = 1.6.dp.toPx()
        val colGap = 5.dp.toPx()
        val rowGap = 5.dp.toPx()
        val startX = (size.width - colGap) / 2f
        val startY = (size.height - rowGap * 2) / 2f
        for (c in 0..1) for (r in 0..2) {
            drawCircle(tint, radius, Offset(startX + c * colGap, startY + r * rowGap))
        }
    }
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
