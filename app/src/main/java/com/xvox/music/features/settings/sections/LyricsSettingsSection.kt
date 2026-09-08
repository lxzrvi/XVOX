package com.xvox.music.features.settings.sections

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.components.*
import com.xvox.music.player.nowplaying.lyrics.LyricPresentationLine
import com.xvox.music.player.nowplaying.lyrics.lyricsEdgeFade
import kotlin.math.roundToInt

@Composable
fun LyricsSettingsSection(state: SettingsState, viewModel: SettingsViewModel) {
    val colors = XvoxTheme.colors
    val settings = state.lyrics
    val loop = rememberInfiniteTransition(label = "lyricsLayoutDemo")
    val time by loop.animateFloat(0f, 12000f, infiniteRepeatable(tween(12000, easing = LinearEasing)), label = "demoTime")
    val active = (((time - settings.offsetMs + 9000) % 9000) / 3000).toInt().coerceIn(0, 2)
    PinnedSettingsEditor(preview = {
        SettingsPreviewFrame("Lyrics · layout sample") {
            val demo = remember { com.xvox.music.player.nowplaying.lyrics.XvoxLyrics(
                (0..4).map { com.xvox.music.player.nowplaying.lyrics.XvoxLyricLine(it * 2400L, "Line ${'A' + it}") },
                true, com.xvox.music.player.nowplaying.lyrics.XvoxLyricsSource.USER_TEXT) }
            Box(Modifier.fillMaxWidth().height(205.dp).clip(RoundedCornerShape(14.dp)).background(colors.background)) {
                com.xvox.music.player.nowplaying.lyrics.XvoxSyncedLyrics(demo, time.toLong(), {},
                    settingsOverride = settings, preview = true, textColor = colors.primaryText)
            }
            Text("Five placeholder rows demonstrate the real fade zones and centre motion.", color = colors.secondaryText, fontSize = 10.sp)
        }
    }, controls = {
        Text("Timing: ${if (settings.offsetMs == 0) "Original" else if (settings.offsetMs > 0) "Delay +${settings.offsetMs} ms" else "Advance ${-settings.offsetMs} ms"}", color = colors.primaryText, fontSize = 12.sp)
        XvoxThinLineSlider(settings.offsetMs.toFloat(), { v -> viewModel.updateLyrics { it.copy(offsetMs = (v / 50).roundToInt() * 50) } }, -5000f..5000f, defaultValue = 0f)
        Text("Positive values display lyrics later. Negative values show them earlier. Applies to all songs.", color = colors.secondaryText, fontSize = 10.sp)
        Text("Current line: ${settings.currentSize} sp", color = colors.primaryText, fontSize = 12.sp)
        XvoxThinLineSlider(settings.currentSize.toFloat(), { v -> viewModel.updateLyrics { it.copy(currentSize = v.roundToInt()) } }, 16f..42f)
        Text("Other lines: ${settings.otherSize} sp", color = colors.primaryText, fontSize = 12.sp)
        XvoxThinLineSlider(settings.otherSize.toFloat(), { v -> viewModel.updateLyrics { it.copy(otherSize = v.roundToInt()) } }, 10f..30f)
        Text("Fade from top: ${(settings.fadeTop * 100).roundToInt()}%", color = colors.primaryText, fontSize = 12.sp)
        XvoxThinLineSlider(settings.fadeTop, { v -> viewModel.updateLyrics { it.copy(fadeTop = (v * 100).roundToInt() / 100f) } }, 0f..0.45f)
        Text("Fade from bottom: ${(settings.fadeBottom * 100).roundToInt()}%", color = colors.primaryText, fontSize = 12.sp)
        XvoxThinLineSlider(settings.fadeBottom, { v -> viewModel.updateLyrics { it.copy(fadeBottom = (v * 100).roundToInt() / 100f) } }, 0f..0.45f)
        SettingsChoiceRow(listOf("fade" to "Soft fade", "slide" to "Slide", "focus" to "Focus zoom", "glide" to "Glide", "spring" to "Spring"), settings.animation) { value ->
            viewModel.updateLyrics { it.copy(animation = value) }
        }
        SettingsChoiceRow(listOf("reset" to "Reset lyrics settings", "timing" to "Reset timing"), "") {
            key -> viewModel.updateLyrics { if (key == "timing") it.copy(offsetMs = 0) else com.xvox.music.data.preferences.LyricsSettings() }
        }
    })
}
