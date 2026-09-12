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
import com.xvox.music.features.settings.SettingsViewModel

@Composable
fun MiniPlayerSettingsBoxContent(
    viewModel: SettingsViewModel
) {
    val colors = XvoxTheme.colors
    val state by viewModel.state.collectAsState()
    val chrome = state.chromeStyle

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
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
                Text("Mini Player Transparency", color = colors.secondaryText, fontSize = 11.sp)
                Text("${((1f - chrome.miniBgAlpha.coerceIn(0f, 1f)) * 100).toInt()}%", color = colors.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            XvoxThinLineSlider(
                value = chrome.miniBgAlpha.coerceIn(0f, 1f),
                onValueChange = { a -> viewModel.setChromeStyle { it.copy(miniBgAlpha = a) } },
                valueRange = 0f..1f,
                defaultValue = 0.94f,
                modifier = Modifier.fillMaxWidth()
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
                Text("Navigation Bar Transparency", color = colors.secondaryText, fontSize = 11.sp)
                Text("${((1f - chrome.navBgAlpha.coerceIn(0f, 1f)) * 100).toInt()}%", color = colors.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            XvoxThinLineSlider(
                value = chrome.navBgAlpha.coerceIn(0f, 1f),
                onValueChange = { a -> viewModel.setChromeStyle { it.copy(navBgAlpha = a) } },
                valueRange = 0f..1f,
                defaultValue = 0.94f,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
