package com.xvox.music.features.settings.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.components.SettingsSectionCard
import com.xvox.music.features.settings.components.SettingsToggle
import com.xvox.music.features.settings.components.XvoxThinLineSlider
import kotlin.math.roundToInt

@Composable
fun EqualizerSettingsSection(
    state: SettingsState,
    viewModel: SettingsViewModel
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current

    SettingsSectionCard(title = "Equalizer & DSP Engine", iconRes = R.drawable.ic_xvox_equalizer) {
        SettingsToggle(
            title = "Master Equalizer",
            subtitle = "Hardware real-time sound processing",
            checked = state.equalizerEnabled
        ) {
            haptics.toggle()
            viewModel.setEqualizerEnabled(it)
        }

        AnimatedVisibility(
            visible = state.equalizerEnabled,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        haptics.heavy()
                        viewModel.resetEqualizer()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.cardElevated,
                        contentColor = colors.primaryAccent
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_xvox_refresh),
                        contentDescription = "Reset Equalizer",
                        tint = colors.primaryAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Reset Equalizer to Defaults",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Presets",
                    color = colors.secondaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf("Flat", "Bass Boost", "Treble", "Rock", "Pop", "Jazz", "Electronic", "Vocal", "Custom")
                    presets.forEach { p ->
                        val isSelected = state.eqPreset == p
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) colors.primaryAccent else colors.cardElevated)
                                .clickable {
                                    haptics.tap()
                                    viewModel.setEqPreset(p)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = p,
                                color = if (isSelected) colors.background else colors.primaryText,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val bandLabels = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")
                Text(
                    text = "5-Band Equalizer (-15 dB to +15 dB)",
                    color = colors.primaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                bandLabels.forEachIndexed { index, label ->
                    val currentVal = state.eqBands.getOrElse(index) { 0 }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            color = colors.secondaryText,
                            fontSize = 11.sp,
                            modifier = Modifier.width(55.dp)
                        )
                        XvoxThinLineSlider(
                            value = currentVal.toFloat(),
                            onValueChange = { newVal ->
                                haptics.sliderTick()
                                val updated = state.eqBands.toMutableList()
                                while (updated.size <= index) updated.add(0)
                                updated[index] = newVal.roundToInt()
                                viewModel.setEqBands(updated)
                            },
                            valueRange = -15f..15f,
                            defaultValue = 0f,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${if (currentVal > 0) "+" else ""}$currentVal dB",
                            color = colors.primaryText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.width(44.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                SettingsToggle(
                    title = "Bass Boost",
                    subtitle = "Deep low-end enhancement",
                    checked = state.bassBoost
                ) {
                    haptics.toggle()
                    viewModel.setBassBoost(it)
                }
                if (state.bassBoost) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Strength", color = colors.secondaryText, fontSize = 11.sp, modifier = Modifier.width(55.dp))
                        XvoxThinLineSlider(
                            value = state.bassBoostStrength.toFloat(),
                            onValueChange = {
                                haptics.sliderTick()
                                viewModel.setBassBoostStrength(it.roundToInt())
                            },
                            valueRange = 0f..1000f,
                            defaultValue = 0f,
                            modifier = Modifier.weight(1f)
                        )
                        Text("${(state.bassBoostStrength / 10)}%", color = colors.primaryText, fontSize = 11.sp, modifier = Modifier.width(36.dp))
                    }
                }

                SettingsToggle(
                    title = "Virtualizer / 3D Surround",
                    subtitle = "Stereo sound stage expander",
                    checked = state.virtualizerEnabled
                ) {
                    haptics.toggle()
                    viewModel.setVirtualizer(it)
                }
                if (state.virtualizerEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Strength", color = colors.secondaryText, fontSize = 11.sp, modifier = Modifier.width(55.dp))
                        XvoxThinLineSlider(
                            value = state.virtualizerStrength.toFloat(),
                            onValueChange = {
                                haptics.sliderTick()
                                viewModel.setVirtualizerStrength(it.roundToInt())
                            },
                            valueRange = 0f..1000f,
                            defaultValue = 0f,
                            modifier = Modifier.weight(1f)
                        )
                        Text("${(state.virtualizerStrength / 10)}%", color = colors.primaryText, fontSize = 11.sp, modifier = Modifier.width(36.dp))
                    }
                }

                SettingsToggle(
                    title = "Loudness Enhancer",
                    subtitle = "Volume boost for quiet audio files",
                    checked = state.loudnessEnhancer
                ) {
                    haptics.toggle()
                    viewModel.setLoudnessEnhancer(it)
                }
                if (state.loudnessEnhancer) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Gain", color = colors.secondaryText, fontSize = 11.sp, modifier = Modifier.width(55.dp))
                        XvoxThinLineSlider(
                            value = state.loudnessGainMb.toFloat(),
                            onValueChange = {
                                haptics.sliderTick()
                                viewModel.setLoudnessGainMb(it.roundToInt())
                            },
                            valueRange = 0f..2000f,
                            defaultValue = 0f,
                            modifier = Modifier.weight(1f)
                        )
                        Text("+${(state.loudnessGainMb / 100)} dB", color = colors.primaryText, fontSize = 11.sp, modifier = Modifier.width(44.dp))
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Stereo Balance L / R", color = colors.primaryText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("L", color = colors.secondaryText, fontSize = 11.sp, modifier = Modifier.width(18.dp))
                    XvoxThinLineSlider(
                        value = state.balance,
                        onValueChange = {
                            haptics.sliderTick()
                            viewModel.setBalance(it)
                        },
                        valueRange = -1.0f..1.0f,
                        defaultValue = 0f,
                        modifier = Modifier.weight(1f)
                    )
                    Text("R", color = colors.secondaryText, fontSize = 11.sp, modifier = Modifier.width(18.dp))
                }
            }
        }
    }
}
