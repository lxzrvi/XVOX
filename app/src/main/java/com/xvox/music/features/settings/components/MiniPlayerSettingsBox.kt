package com.xvox.music.features.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.overlay.xvoxBoxScroll
import com.xvox.music.features.settings.SettingsViewModel
import kotlin.math.roundToInt

@Composable
fun MiniPlayerSettingsBoxContent(
    viewModel: SettingsViewModel
) {
    val colors = XvoxTheme.colors
    val state by viewModel.state.collectAsState()
    val chrome = state.chromeStyle

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .xvoxBoxScroll(scrollState)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Mini Player section
        Text("Mini Player", color = colors.primaryAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Cover Style", color = colors.secondaryText, fontSize = 11.sp)
            SettingsChoiceRow(
                options = listOf("standard" to "Standard Box", "full" to "Full Cover Artwork"),
                selected = chrome.miniCoverStyle,
                onSelect = { s -> viewModel.setChromeStyle { it.copy(miniCoverStyle = s) } }
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Mini Player radius", color = colors.secondaryText, fontSize = 11.sp)
                Text("${chrome.miniCornerRadius.coerceIn(6f, 32f).roundToInt()} dp", color = colors.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            XvoxContinuousSlider(
                value = chrome.miniCornerRadius.coerceIn(6f, 32f),
                onValueChange = { radius -> viewModel.setChromeStyle { it.copy(miniCornerRadius = radius.coerceIn(6f, 32f)) } },
                valueRange = 6f..32f,
                defaultValue = 15f,
                contentDescription = "Mini Player radius"
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Mini Player Transparency", color = colors.secondaryText, fontSize = 11.sp)
                Text("${((1f - chrome.miniBgAlpha.coerceIn(0f, 1f)) * 100).toInt()}%", color = colors.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            XvoxSlider(
                value = chrome.miniBgAlpha.coerceIn(0f, 1f),
                onValueChange = { a -> viewModel.setChromeStyle { it.copy(miniBgAlpha = a) } },
                valueRange = 0f..1f,
                defaultValue = 0.94f,
                modifier = Modifier.fillMaxWidth(),
                valueLabel = { a -> "${((1f - a.coerceIn(0f, 1f)) * 100).toInt()}%" }
            )
        }

        Spacer(Modifier.height(4.dp))

        // Navigation Bar section
        Text("Navigation Bar", color = colors.primaryAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Navigation bar height", color = colors.secondaryText, fontSize = 11.sp)
                Text("${chrome.navigationBarHeight.coerceIn(52f, 88f).roundToInt()} dp", color = colors.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            XvoxContinuousSlider(
                value = chrome.navigationBarHeight.coerceIn(52f, 88f),
                onValueChange = { height -> viewModel.setChromeStyle { it.copy(navigationBarHeight = height.coerceIn(52f, 88f)) } },
                valueRange = 52f..88f,
                defaultValue = 64f,
                contentDescription = "Navigation bar height"
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Navigation Bar Transparency", color = colors.secondaryText, fontSize = 11.sp)
                Text("${((1f - chrome.navBgAlpha.coerceIn(0f, 1f)) * 100).toInt()}%", color = colors.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            XvoxSlider(
                value = chrome.navBgAlpha.coerceIn(0f, 1f),
                onValueChange = { a -> viewModel.setChromeStyle { it.copy(navBgAlpha = a) } },
                valueRange = 0f..1f,
                defaultValue = 0.94f,
                modifier = Modifier.fillMaxWidth(),
                valueLabel = { a -> "${((1f - a.coerceIn(0f, 1f)) * 100).toInt()}%" }
            )
        }
    }
}
