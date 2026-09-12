package com.xvox.music.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.features.settings.components.SettingsChoiceRow
import com.xvox.music.features.settings.components.XvoxThinLineSlider

@Composable
fun RecentLayoutBoxContent(
    config: HomePresentation,
    viewModel: HomeViewModel
) {
    val colors = XvoxTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Recently Played Settings", color = colors.primaryAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)

        // Hide Recents Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text("Show Recently Played", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("Display recent songs strip on home", color = colors.secondaryText, fontSize = 11.sp)
            }
            Switch(
                checked = !config.hideRecents,
                onCheckedChange = { visible ->
                    viewModel.setHomeSectionVisible(HomeSections.RECENT, visible)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.background,
                    checkedTrackColor = colors.primaryAccent,
                    uncheckedThumbColor = colors.secondaryText,
                    uncheckedTrackColor = colors.cardElevated
                )
            )
        }

        // Placement Position
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Feed Position", color = colors.secondaryText, fontSize = 11.sp)
            SettingsChoiceRow(
                options = listOf("top" to "Top of Home", "bottom" to "Below All Songs"),
                selected = config.recentsPlacement,
                onSelect = { viewModel.setRecentsPlacement(it) }
            )
        }

        // Merge to Home Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text("Merge Section to Home", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("Keep pinned in unified Home feed", color = colors.secondaryText, fontSize = 11.sp)
            }
            Switch(
                checked = config.merge,
                onCheckedChange = { mergeOn ->
                    viewModel.setHomeMerge(mergeOn)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.background,
                    checkedTrackColor = colors.primaryAccent,
                    uncheckedThumbColor = colors.secondaryText,
                    uncheckedTrackColor = colors.cardElevated
                )
            )
        }
    }
}

@Composable
fun AllSongsLayoutBoxContent(
    config: HomePresentation,
    viewModel: HomeViewModel
) {
    val colors = XvoxTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("All Songs Layout & Grid", color = colors.primaryAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)

        // Card Style
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Layout Style", color = colors.secondaryText, fontSize = 11.sp)
            SettingsChoiceRow(
                options = listOf("uniform" to "Standard List", "mosaic1" to "Mosaic 1", "mosaic2" to "Mosaic 2"),
                selected = config.style,
                onSelect = { viewModel.setHomeLayoutStyle(it) }
            )
        }

        // Scroll Direction
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Scroll Direction", color = colors.secondaryText, fontSize = 11.sp)
            SettingsChoiceRow(
                options = listOf("horizontal" to "Horizontal Pages", "vertical" to "Vertical Scroll"),
                selected = config.direction,
                onSelect = { viewModel.setHomeScrollDirection(it) }
            )
        }

        // Grid Rows
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Grid Rows per Page", color = colors.secondaryText, fontSize = 11.sp)
                Text("${config.rows} rows", color = colors.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            XvoxThinLineSlider(
                value = config.rows.toFloat(),
                onValueChange = { viewModel.setHomeHorizontalRows(it.toInt()) },
                valueRange = 3f..8f,
                defaultValue = 4f,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun LikedSongsLayoutBoxContent(
    config: HomePresentation,
    viewModel: HomeViewModel
) {
    val colors = XvoxTheme.colors
    val isMerged = config.merge && HomeSections.LIKED in HomeSections.visible(config)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Liked Songs Settings", color = colors.primaryAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)

        // Merge to Home Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text("Merge to Home Feed", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("Show Liked Songs directly on Home and hide from top pill", color = colors.secondaryText, fontSize = 11.sp)
            }
            Switch(
                checked = isMerged,
                onCheckedChange = { mergeOn ->
                    if (mergeOn) {
                        viewModel.setHomeMerge(true)
                        viewModel.setHomeSectionVisible(HomeSections.LIKED, true)
                    } else {
                        viewModel.setHomeSectionVisible(HomeSections.LIKED, false)
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.background,
                    checkedTrackColor = colors.primaryAccent,
                    uncheckedThumbColor = colors.secondaryText,
                    uncheckedTrackColor = colors.cardElevated
                )
            )
        }
    }
}
