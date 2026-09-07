package com.xvox.music.features.settings.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.components.SettingsToggle

@Composable
fun HomeSettingsSection(
    state: SettingsState,
    viewModel: SettingsViewModel
) {
    val colors = XvoxTheme.colors

    Column(modifier = Modifier.fillMaxWidth()) {
        com.xvox.music.features.settings.components.HomeSettingsPreview(state)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Home Style",
            color = colors.secondaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val styles = listOf(
                "uniform" to "One Size",
                "mosaic" to "Mosaic"
            )

            styles.forEach { (key, label) ->
                val isSelected = state.homeLayoutStyle == key
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) colors.primaryAccent else colors.cardElevated)
                        .clickable { viewModel.setHomeLayoutStyle(key) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) colors.background else colors.primaryText,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        if (state.homeLayoutStyle == "mosaic") {
            Spacer(Modifier.height(8.dp))
            Text("24 card treatments + thousands of generated layouts. Fresh on every app restart; stable while you browse.",
                color = colors.secondaryText, fontSize = 11.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "All Songs navigation",
            color = colors.secondaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val directions = listOf(
                "horizontal" to "Free pan ↗",
                "vertical" to "Vertical"
            )

            directions.forEach { (key, label) ->
                val isSelected = state.homeScrollDirection == key
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) colors.primaryAccent else colors.cardElevated)
                        .clickable { viewModel.setHomeScrollDirection(key) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) colors.background else colors.primaryText,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        if (state.homeScrollDirection == "horizontal") {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Canvas rows (drag to explore)",
                color = colors.secondaryText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val rowOptions = listOf(3, 4, 5, 6, 7, 8)
                rowOptions.forEach { r ->
                    val isSelected = state.homeHorizontalRows == r
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) colors.primaryAccent else colors.cardElevated)
                            .clickable {
                                viewModel.setHomeHorizontalRows(r)
                                viewModel.setFourRowsGrid(r == 4)
                            }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "4x$r",
                            color = if (isSelected) colors.background else colors.primaryText,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Song Sort Order",
            color = colors.secondaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val sortOptions = listOf("A-Z", "Z-A", "Random")
            sortOptions.forEach { opt ->
                val isSelected = state.sortOrder == opt
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isSelected) colors.primaryAccent else colors.cardElevated)
                        .clickable { viewModel.setSortOrder(opt) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = opt,
                        color = if (isSelected) colors.background else colors.primaryText,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsToggle(
            title = "Hide Recents",
            subtitle = "Hide Recently Played section and only display All Songs on Home",
            checked = state.hideRecentlyPlayed,
            onChange = viewModel::setHideRecentlyPlayed
        )
        Spacer(Modifier.height(14.dp))
        Text("Recents placement", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text("Top: above All Songs. Bottom: below All Songs.", color = colors.secondaryText, fontSize = 11.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
        com.xvox.music.features.settings.components.SettingsChoiceRow(
            listOf("top" to "Top", "bottom" to "Bottom"), state.recentsPlacement, viewModel::setRecentsPlacement)
    }
}
