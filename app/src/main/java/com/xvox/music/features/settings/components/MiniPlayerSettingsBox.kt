package com.xvox.music.features.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.chrome.XvoxChromeStyle
import com.xvox.music.core.ui.miniplayer.XvoxPlayerTransitionMotion
import com.xvox.music.core.ui.overlay.xvoxBoxScroll
import kotlin.math.roundToInt

/** Mini Player-only visual controls plus the accepted Default/Scale handoff motion. */
@Composable
fun MiniPlayerSettingsBoxContent(
    chrome: XvoxChromeStyle,
    onChromeChange: (XvoxChromeStyle) -> Unit
) {
    val colors = XvoxTheme.colors
    val scrollState = rememberScrollState()
    val selectedStyle = XvoxPlayerTransitionMotion.normalizedStyle(chrome.miniPlayerTransitionStyle)
    val coverMode = when (chrome.miniCoverStyle) {
        "full", "blur" -> chrome.miniCoverStyle
        else -> "default"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .xvoxBoxScroll(scrollState)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("Mini Player cover", color = colors.secondaryText, fontSize = 11.sp)
        SettingsChoiceRow(
            options = listOf(
                "default" to "Default",
                "full" to "Full cover",
                "blur" to "Blur background"
            ),
            selected = coverMode,
            onSelect = { style -> onChromeChange(chrome.copy(miniCoverStyle = style)) }
        )

        if (coverMode == "blur") {
            Spacer(Modifier.height(4.dp))
            val blur = chrome.miniCoverBlur.coerceIn(12f, 50f)
            Text("Blur background · ${blur.roundToInt()} dp", color = colors.secondaryText, fontSize = 11.sp)
            XvoxContinuousSlider(
                value = blur,
                onValueChange = { value -> onChromeChange(chrome.copy(miniCoverBlur = value.coerceIn(12f, 50f))) },
                valueRange = 12f..50f,
                defaultValue = 26.25f,
                contentDescription = "Mini Player background blur"
            )
        }

        Spacer(Modifier.height(4.dp))
        val miniTransparency = 1f - chrome.miniBgAlpha.coerceIn(0f, 1f)
        Text("Mini Player transparency · ${(miniTransparency * 100).roundToInt()}%", color = colors.secondaryText, fontSize = 11.sp)
        XvoxSlider(
            value = miniTransparency,
            onValueChange = { transparency ->
                onChromeChange(chrome.copy(miniBgAlpha = 1f - transparency.coerceIn(0f, 1f)))
            },
            valueRange = 0f..1f,
            defaultValue = .06f,
            valueLabel = { transparency -> "${(transparency.coerceIn(0f, 1f) * 100).roundToInt()}%" }
        )

        Spacer(Modifier.height(4.dp))
        val navTransparency = 1f - chrome.navBgAlpha.coerceIn(0f, 1f)
        Text("Navigation bar transparency · ${(navTransparency * 100).roundToInt()}%", color = colors.secondaryText, fontSize = 11.sp)
        XvoxSlider(
            value = navTransparency,
            onValueChange = { transparency ->
                onChromeChange(chrome.copy(navBgAlpha = 1f - transparency.coerceIn(0f, 1f)))
            },
            valueRange = 0f..1f,
            defaultValue = .06f,
            valueLabel = { transparency -> "${(transparency.coerceIn(0f, 1f) * 100).roundToInt()}%" }
        )

        Spacer(Modifier.height(4.dp))
        Text("Mini Player animation", color = colors.secondaryText, fontSize = 11.sp)
        SettingsChoiceRow(
            options = listOf(
                XvoxPlayerTransitionMotion.DefaultStyle to "Default",
                XvoxPlayerTransitionMotion.ScaleStyle to "Scale"
            ),
            selected = selectedStyle,
            onSelect = { style ->
                onChromeChange(
                    chrome.copy(
                        miniPlayerTransitionStyle = XvoxPlayerTransitionMotion.normalizedStyle(style)
                    )
                )
            }
        )
    }
}
