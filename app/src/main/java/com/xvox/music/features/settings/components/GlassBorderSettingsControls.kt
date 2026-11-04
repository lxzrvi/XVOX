package com.xvox.music.features.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.ui.chrome.XvoxChromeStyle
import kotlin.math.roundToInt

/**
 * App-wide glass-edge controls.  They intentionally live in general Appearance rather than the
 * Mini Player editor because one persisted choice governs every blurred surface.
 */
@Composable
fun GlassBorderSettingsControls(
    chrome: XvoxChromeStyle,
    onChromeChange: (XvoxChromeStyle) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SettingsToggle(
            title = "Show glass borders",
            subtitle = "Off removes the border from every blurred surface",
            checked = chrome.glassBorderEnabled,
            onChange = { enabled -> onChromeChange(chrome.copy(glassBorderEnabled = enabled)) }
        )

        if (chrome.glassBorderEnabled) {
            val width = chrome.glassBorderWidth.coerceIn(0f, 4f)
            AppearanceGlassSlider(
                label = "Border width · ${"%.1f".format(width)} dp"
            ) {
                XvoxSlider(
                    value = width,
                    onValueChange = { value ->
                        onChromeChange(chrome.copy(glassBorderWidth = value.coerceIn(0f, 4f)))
                    },
                    valueRange = 0f..4f,
                    defaultValue = .8f,
                    valueLabel = { value -> "%.1f dp".format(value) }
                )
            }

            val transparency = 1f - chrome.glassBorderAlpha.coerceIn(0f, 1f)
            AppearanceGlassSlider(
                label = "Border transparency · ${(transparency * 100).roundToInt()}%"
            ) {
                XvoxSlider(
                    value = transparency,
                    onValueChange = { value ->
                        onChromeChange(chrome.copy(glassBorderAlpha = 1f - value.coerceIn(0f, 1f)))
                    },
                    valueRange = 0f..1f,
                    defaultValue = .38f,
                    valueLabel = { value -> "${(value.coerceIn(0f, 1f) * 100).roundToInt()}%" }
                )
            }

            val softness = chrome.glassBorderBlur.coerceIn(0f, 12f)
            AppearanceGlassSlider(
                label = "Border blur (soft edge) · ${"%.1f".format(softness)} dp"
            ) {
                XvoxSlider(
                    value = softness,
                    onValueChange = { value ->
                        onChromeChange(chrome.copy(glassBorderBlur = value.coerceIn(0f, 12f)))
                    },
                    valueRange = 0f..12f,
                    defaultValue = 0f,
                    valueLabel = { value -> "%.1f dp".format(value) }
                )
            }

            SettingsChoiceRow(
                options = listOf("theme" to "Theme colour", "custom" to "Custom colour"),
                selected = if (chrome.glassBorderColor.isBlank()) "theme" else "custom",
                onSelect = { choice ->
                    onChromeChange(chrome.copy(glassBorderColor = if (choice == "theme") "" else "#FFFFFF"))
                }
            )
            if (chrome.glassBorderColor.isNotBlank()) {
                ColorPickerRow(
                    label = "Border colour",
                    hex = chrome.glassBorderColor,
                    onColorChange = { hex -> onChromeChange(chrome.copy(glassBorderColor = hex)) },
                    subtitle = "Applied to every blurred surface",
                    showPreview = true,
                    initiallyExpanded = false
                )
            }
        }
    }
}

/** A label plus rail stays visually grouped without reintroducing a Mini Player-only setting box. */
@Composable
private fun AppearanceGlassSlider(
    label: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        androidx.compose.material3.Text(
            text = label,
            color = com.xvox.music.core.design.theme.XvoxTheme.colors.secondaryText,
            fontSize = 11.sp
        )
        content()
    }
}
