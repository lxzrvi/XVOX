package com.xvox.music.features.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
            val blur = chrome.miniCoverBlur.coerceIn(12f, 50f)
            MiniPlayerSliderSection("Blur background · ${blur.roundToInt()} dp") {
                XvoxContinuousSlider(
                    value = blur,
                    onValueChange = { value -> onChromeChange(chrome.copy(miniCoverBlur = value.coerceIn(12f, 50f))) },
                    valueRange = 12f..50f,
                    defaultValue = 26.25f,
                    contentDescription = "Mini Player background blur"
                )
            }
        }

        val miniTransparency = 1f - chrome.miniBgAlpha.coerceIn(0f, 1f)
        MiniPlayerSliderSection("Mini Player transparency · ${(miniTransparency * 100).roundToInt()}%") {
            XvoxSlider(
                value = miniTransparency,
                onValueChange = { transparency ->
                    onChromeChange(chrome.copy(miniBgAlpha = 1f - transparency.coerceIn(0f, 1f)))
                },
                valueRange = 0f..1f,
                defaultValue = .06f,
                valueLabel = { transparency -> "${(transparency.coerceIn(0f, 1f) * 100).roundToInt()}%" }
            )
        }

        val navTransparency = 1f - chrome.navBgAlpha.coerceIn(0f, 1f)
        MiniPlayerSliderSection("Navigation bar transparency · ${(navTransparency * 100).roundToInt()}%") {
            XvoxSlider(
                value = navTransparency,
                onValueChange = { transparency ->
                    onChromeChange(chrome.copy(navBgAlpha = 1f - transparency.coerceIn(0f, 1f)))
                },
                valueRange = 0f..1f,
                defaultValue = .06f,
                valueLabel = { transparency -> "${(transparency.coerceIn(0f, 1f) * 100).roundToInt()}%" }
            )
        }

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

/** A labelled rail is one bounded option material, not loose text plus an isolated slider. */
@Composable
private fun MiniPlayerSliderSection(
    label: String,
    content: @Composable () -> Unit
) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.cardElevated.copy(alpha = .62f))
            .border(.8.dp, colors.cardBorder.copy(alpha = .82f), shape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(label, color = colors.secondaryText, fontSize = 11.sp)
        content()
    }
}
