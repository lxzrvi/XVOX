package com.xvox.music.features.settings.sections

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.chrome.LocalXvoxChromeStyle
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.features.home.HomeSections
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.components.SettingsAccordionItem
import com.xvox.music.features.settings.components.SettingsChoiceRow
import com.xvox.music.features.settings.components.SettingsControlsEditor
import com.xvox.music.features.settings.components.SettingsToggle
import com.xvox.music.features.settings.components.XvoxContinuousSlider
import com.xvox.music.features.settings.components.XvoxSlider
import kotlin.math.roundToInt

@Composable
fun HomeSettingsSection(
    state: SettingsState,
    viewModel: SettingsViewModel
) {
    val colors = XvoxTheme.colors
    val chrome = LocalXvoxChromeStyle.current
    val haptics = LocalXvoxHaptics.current
    var expandedGroup by remember { mutableStateOf<String?>(null) }

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
                listOf("uniform" to "Default", "mosaic1" to "Mosaic"),
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
            XvoxSlider(
                value = currentHeight.toFloat(),
                onValueChange = { viewModel.setPlaylistLongHeight(it.roundToInt()) },
                valueRange = 100f..260f,
                defaultValue = 178f,
                valueLabel = { height -> "${height.roundToInt()}dp" }
            )
        }

        SettingsAccordionItem(
            title = "Mini Player & Nav Bar",
            expanded = expandedGroup == "Mini Player & Nav Bar",
            onToggle = { toggle("Mini Player & Nav Bar") }
        ) {
            Label("Cover style")
            SettingsChoiceRow(
                listOf("default" to "Default", "full" to "Full cover"),
                chrome.miniCoverStyle,
                onSelect = { style ->
                    viewModel.setChromeStyle { it.copy(miniCoverStyle = style) }
                }
            )

            Spacer(Modifier.height(10.dp))

            val miniRadius = chrome.miniCornerRadius.coerceIn(6f, 32f)
            Label("Mini player corner radius · ${miniRadius.roundToInt()} dp")
            XvoxContinuousSlider(
                value = miniRadius,
                onValueChange = { radius ->
                    viewModel.setChromeStyle { it.copy(miniCornerRadius = radius.coerceIn(6f, 32f)) }
                },
                valueRange = 6f..32f,
                defaultValue = 15f,
                contentDescription = "Mini player corner radius"
            )

            Spacer(Modifier.height(10.dp))

            val navHeight = chrome.navigationBarHeight.coerceIn(52f, 88f)
            Label("Navigation bar height · ${navHeight.roundToInt()} dp")
            XvoxContinuousSlider(
                value = navHeight,
                onValueChange = { height ->
                    viewModel.setChromeStyle { it.copy(navigationBarHeight = height.coerceIn(52f, 88f)) }
                },
                valueRange = 52f..88f,
                defaultValue = 62f,
                contentDescription = "Navigation bar height"
            )

            Spacer(Modifier.height(10.dp))

            val miniTransparency = 1f - chrome.miniBgAlpha.coerceIn(0f, 1f)
            Label("Mini player transparency · ${(miniTransparency * 100).roundToInt()}%")
            XvoxSlider(
                value = miniTransparency,
                onValueChange = { transparency ->
                    viewModel.setChromeStyle { it.copy(miniBgAlpha = 1f - transparency.coerceIn(0f, 1f)) }
                },
                valueRange = 0f..1f,
                defaultValue = .06f,
                valueLabel = { transparency -> "${(transparency.coerceIn(0f, 1f) * 100).roundToInt()}%" }
            )

            Spacer(Modifier.height(10.dp))

            val navTransparency = 1f - chrome.navBgAlpha.coerceIn(0f, 1f)
            Label("Navigation bar transparency · ${(navTransparency * 100).roundToInt()}%")
            XvoxSlider(
                value = navTransparency,
                onValueChange = { transparency ->
                    viewModel.setChromeStyle { it.copy(navBgAlpha = 1f - transparency.coerceIn(0f, 1f)) }
                },
                valueRange = 0f..1f,
                defaultValue = .06f,
                valueLabel = { transparency -> "${(transparency.coerceIn(0f, 1f) * 100).roundToInt()}%" }
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
                    // Recent history moved to the shell header, so it is not an editable Home feed section.
                    val editableSections = state.homeSectionOrder.filterNot { it == HomeSections.RECENT }
                    editableSections.forEachIndexed { index, section ->
                        val sourceIndex = state.homeSectionOrder.indexOf(section)
                        val visible = if (section == HomeSections.ALL) true
                        else section in state.homeMergedSections && section !in state.homeHiddenSections
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
                                    viewModel.setHomeSectionMerged(section, it)
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
                                            val previous = editableSections[index - 1]
                                            viewModel.moveHomeSection(sourceIndex, state.homeSectionOrder.indexOf(previous))
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    SixDotsIcon(tint = if (index > 0) colors.primaryAccent else colors.mutedText.copy(alpha = 0.35f))
                                }
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .xvoxPressScale(enabled = index < editableSections.lastIndex) {
                                            haptics.tap()
                                            val next = editableSections[index + 1]
                                            viewModel.moveHomeSection(sourceIndex, state.homeSectionOrder.indexOf(next))
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    SixDotsIcon(tint = if (index < editableSections.lastIndex) colors.primaryAccent else colors.mutedText.copy(alpha = 0.35f))
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
