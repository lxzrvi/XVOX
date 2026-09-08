package com.xvox.music.features.settings.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme

/** One short line per gesture. The old page-long explanations are gone. */
@Composable
fun HowToUseSettingsSection() {
    val colors = XvoxTheme.colors
    val tips = listOf(
        "Play" to "Tap a card.",
        "Options" to "Hold a card.",
        "Multi-select" to "Options › Select, then tap more.",
        "Queue" to "Hold the six dots to reorder.",
        "Lyrics" to "Tap the artwork in Now Playing.",
        "Source" to "Tap the badge on a recent card.",
        "Refresh" to "Header pill, after adding files.",
        "Deleted songs" to "Settings › Deleted songs to restore.",
        "Widget" to "Settings › Widget › Add widget.",
        "XvoxSplit" to "Settings › XvoxMix › 3D sound."
    )
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        tips.forEach { (title, text) ->
            Row(Modifier.fillMaxWidth()) {
                Text(title, color = colors.primaryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(112.dp))
                Text(text, color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.weight(1f))
            }
        }
    }
}
