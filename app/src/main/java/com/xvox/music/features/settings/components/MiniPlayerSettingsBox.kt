package com.xvox.music.features.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.chrome.XvoxChromeStyle
import com.xvox.music.core.ui.overlay.xvoxBoxScroll
import kotlin.math.roundToInt

/**
 * Live body for the Mini Player / Navbar editor. Placement, radius, width, height, and image
 * choices retain their existing saved values but are not edited in this compact sheet.
 */
@Composable
fun MiniPlayerSettingsBoxContent(
    chrome: XvoxChromeStyle,
    onChromeChange: (XvoxChromeStyle) -> Unit
) {
    val colors = XvoxTheme.colors
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .xvoxBoxScroll(scrollState)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Text("Mini Player", color = colors.primaryAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Cover style", color = colors.secondaryText, fontSize = 11.sp)
            SettingsChoiceRow(
                options = listOf("default" to "Standard Box", "full" to "Full Cover Artwork"),
                selected = if (chrome.miniCoverStyle == "full") "full" else "default",
                onSelect = { value -> onChromeChange(chrome.copy(miniCoverStyle = value)) }
            )
        }

        // 0% is solid and 100% is clear. The continuous slider includes its visible default mark.
        ChromeTransparencySlider(
            label = "Mini Player transparency",
            value = 1f - chrome.miniBgAlpha.coerceIn(0f, 1f),
            default = .06f,
            contentDescription = "Mini Player transparency",
            onChange = { transparency ->
                onChromeChange(chrome.copy(miniBgAlpha = (1f - transparency).coerceIn(0f, 1f)))
            }
        )

        Text("Navigation Bar", color = colors.primaryAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)

        ChromeTransparencySlider(
            label = "Navbar transparency",
            value = 1f - chrome.navBgAlpha.coerceIn(0f, 1f),
            default = .06f,
            contentDescription = "Navbar transparency",
            onChange = { transparency ->
                onChromeChange(chrome.copy(navBgAlpha = (1f - transparency).coerceIn(0f, 1f)))
            }
        )
    }
}

@Composable
private fun ChromeTransparencySlider(
    label: String,
    value: Float,
    default: Float,
    contentDescription: String,
    onChange: (Float) -> Unit
) {
    val colors = XvoxTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = colors.secondaryText, fontSize = 11.sp)
            Text(
                "${(value.coerceIn(0f, 1f) * 100).roundToInt()}%",
                color = colors.primaryAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        XvoxContinuousSlider(
            value = value.coerceIn(0f, 1f),
            onValueChange = { onChange(it.coerceIn(0f, 1f)) },
            valueRange = 0f..1f,
            defaultValue = default,
            contentDescription = contentDescription
        )
    }
}
