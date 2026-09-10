package com.xvox.music.features.settings.sections

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.components.*
import kotlin.math.roundToInt

/**
 * Lyrics settings with a full-screen preview: the whole lyrics view at the phone's real
 * proportions, scaled to fit, so fade zones, sizes and the chosen animation are seen in context
 * rather than through a small window.
 */
@Composable
fun LyricsSettingsSection(state: SettingsState, viewModel: SettingsViewModel) {
    val colors = XvoxTheme.colors
    val settings = state.lyrics
    val configuration = LocalConfiguration.current
    val loop = rememberInfiniteTransition(label = "lyricsLayoutDemo")
    val time by loop.animateFloat(0f, 12000f, infiniteRepeatable(tween(12000, easing = LinearEasing)), label = "demoTime")

    com.xvox.music.features.settings.components.SettingsControlsEditor(controls = {

        // Timing: −1000..+1000 ms at 1 ms resolution. Small offsets (say +37 ms) sit exactly where
        // you leave them — the value is never snapped back to the centre, not even within ±100 ms
        // of zero. Fine nudges sit right under the bar for precise sync.
        Label(if (settings.offsetMs == 0) "Timing" else if (settings.offsetMs > 0) "Timing +${settings.offsetMs} ms" else "Timing ${settings.offsetMs} ms")
        XvoxThinLineSlider(settings.offsetMs.toFloat(), { v -> viewModel.updateLyrics { it.copy(offsetMs = v.roundToInt()) } }, -1000f..1000f, defaultValue = 0f, snapRadius = 0f)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(-100, -10, -1, 1, 10, 100).forEach { step ->
                val label = if (step < 0) "${step} ms" else "+$step ms"
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(9.dp))
                        .background(colors.cardElevated)
                        .xvoxPressScale {
                            viewModel.updateLyrics { it.copy(offsetMs = (it.offsetMs + step).coerceIn(-1000, 1000)) }
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = colors.secondaryText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        Label("Current line ${settings.currentSize}")
        XvoxThinLineSlider(settings.currentSize.toFloat(), { v -> viewModel.updateLyrics { it.copy(currentSize = v.roundToInt()) } }, 16f..42f)

        Label("Other lines ${settings.otherSize}")
        XvoxThinLineSlider(settings.otherSize.toFloat(), { v -> viewModel.updateLyrics { it.copy(otherSize = v.roundToInt()) } }, 10f..30f)

        Label("Top fade ${(settings.fadeTop * 100).roundToInt()}%")
        XvoxThinLineSlider(settings.fadeTop, { v -> viewModel.updateLyrics { it.copy(fadeTop = (v * 100).roundToInt() / 100f) } }, 0f..0.45f, defaultValue = .22f)

        Label("Bottom fade ${(settings.fadeBottom * 100).roundToInt()}%")
        XvoxThinLineSlider(settings.fadeBottom, { v -> viewModel.updateLyrics { it.copy(fadeBottom = (v * 100).roundToInt() / 100f) } }, 0f..0.45f, defaultValue = .22f)

        SettingsToggle(
            title = "Equal fade",
            subtitle = "Fade every line above and below — only the current line stays clear",
            checked = settings.fadeEqual,
            onChange = { on -> viewModel.updateLyrics { it.copy(fadeEqual = on) } }
        )

        if (settings.fadeEqual) {
            Label("Fade strength ${(settings.fadeIntensity * 100).roundToInt()}%")
            XvoxThinLineSlider(settings.fadeIntensity, { v -> viewModel.updateLyrics { it.copy(fadeIntensity = (v * 100).roundToInt() / 100f) } }, 0f..1f, defaultValue = 1f)
        }

        Label("Lines")
        SettingsChoiceRow(
            listOf("left" to "Left", "center" to "Centre", "right" to "Right"),
            settings.alignment
        ) { chosen -> viewModel.updateLyrics { it.copy(alignment = chosen) } }

        Label("Animation")
        SettingsChoiceRow(
            listOf(
                "fade" to "Fade", "slide" to "Slide", "focus" to "Zoom", "glide" to "Glide",
                "spring" to "Spring", "rise" to "Rise", "pulse" to "Pulse", "wave" to "Wave"
            ),
            settings.animation
        ) { value -> viewModel.updateLyrics { it.copy(animation = value) } }

        SettingsChoiceRow(listOf("reset" to "Reset all", "timing" to "Reset timing"), "") { key ->
            viewModel.updateLyrics { if (key == "timing") it.copy(offsetMs = 0) else com.xvox.music.data.preferences.LyricsSettings() }
        }
    })
}

@Composable
private fun Label(text: String) {
    Text(text, color = XvoxTheme.colors.primaryAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
}
