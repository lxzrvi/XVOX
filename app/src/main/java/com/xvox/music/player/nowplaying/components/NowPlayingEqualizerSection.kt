package com.xvox.music.player.nowplaying.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.audio.AudioEffectsManager
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.features.settings.components.XvoxThinLineSlider
import kotlin.math.roundToInt

@Composable
fun NowPlayingEqualizerSection(
    eqEnabled: Boolean,
    eqPreset: String,
    eqBands: List<Int>,
    onToggleEq: (Boolean) -> Unit,
    onSelectPreset: (String) -> Unit,
    onBandsChange: (List<Int>) -> Unit
) {
    val colors = XvoxTheme.colors

    Text(
        text = "Equalizer & DSP",
        color = colors.primaryText,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold
    )

    Spacer(Modifier.height(6.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Master Equalizer", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text("Preset: $eqPreset", color = colors.secondaryText, fontSize = 11.sp)
        }

        Switch(
            checked = eqEnabled,
            onCheckedChange = onToggleEq,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.background,
                checkedTrackColor = colors.primaryAccent
            )
        )
    }

    if (eqEnabled) {
        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val presets = listOf("Flat", "Bass Boost", "Treble", "Rock", "Pop", "Jazz", "Electronic", "Vocal", "Custom")
            presets.forEach { preset ->
                val selected = eqPreset == preset
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) colors.primaryAccent else colors.cardElevated)
                        .clickable {
                            onSelectPreset(preset)
                            if (preset != "Custom" && AudioEffectsManager.PRESETS.containsKey(preset)) {
                                onBandsChange(AudioEffectsManager.PRESETS[preset] ?: listOf(0, 0, 0, 0, 0))
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = preset,
                        color = if (selected) colors.background else colors.primaryText,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        val labels = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")
        labels.forEachIndexed { index, label ->
            val value = eqBands.getOrElse(index) { 0 }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    color = colors.secondaryText,
                    fontSize = 11.sp,
                    modifier = Modifier.width(55.dp)
                )

                XvoxThinLineSlider(
                    value = value.toFloat(),
                    onValueChange = { newValue ->
                        val updated = eqBands.toMutableList()
                        while (updated.size <= index) updated.add(0)
                        updated[index] = newValue.roundToInt()
                        onBandsChange(updated)
                    },
                    valueRange = -15f..15f,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "${if (value > 0) "+" else ""}$value dB",
                    color = colors.primaryText,
                    fontSize = 10.sp,
                    modifier = Modifier.width(44.dp)
                )
            }
        }
    }
}
