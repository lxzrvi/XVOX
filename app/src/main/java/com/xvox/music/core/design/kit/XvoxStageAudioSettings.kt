package com.xvox.music.core.design.kit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.data.preferences.UserPreferencesRepository
import io.github.neoruaa.glasmio.core.LocalGlassColors
import io.github.neoruaa.glasmio.glass.render.Backdrop
import io.github.neoruaa.glasmio.preference.GlasmioPreferenceGroupTitle
import io.github.neoruaa.glasmio.preference.GlasmioSwitchPreference
import io.github.neoruaa.glasmio.scaffold.GlasmioPinnedListPage
import io.github.neoruaa.glasmio.ui.GlasmioSlider
import kotlinx.coroutines.launch

/**
 * Audio & playback, in Glasmio's own vocabulary: switches for on/off options, a slider only where
 * the value is continuous, and the same inset card groups as the rest of Settings. Every control
 * writes the DataStore the engine already reads, so a change is live.
 */
@Composable
fun XvoxStageAudioPage(
    prefs: UserPreferencesRepository,
    backdrop: Backdrop,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val glass = LocalGlassColors.current

    val crossfade by prefs.crossfade.collectAsState(initial = false)
    val crossfadeDuration by prefs.crossfadeDuration.collectAsState(initial = 6)
    val crossfadeSmart by prefs.crossfadeSmart.collectAsState(initial = false)
    val crossfadeBeat by prefs.crossfadeBeatSync.collectAsState(initial = false)
    val gapless by prefs.gapless.collectAsState(initial = true)
    val eqEnabled by prefs.equalizerEnabled.collectAsState(initial = false)
    val surroundDepth by prefs.surroundDepth.collectAsState(initial = .5f)
    val headroom by prefs.eqHeadroomDb.collectAsState(initial = 0f)

    var liveDuration by remember(crossfadeDuration) { mutableFloatStateOf(crossfadeDuration.toFloat()) }
    var liveSurround by remember(surroundDepth) { mutableFloatStateOf(surroundDepth) }
    var liveHeadroom by remember(headroom) { mutableFloatStateOf(headroom) }

    GlasmioPinnedListPage(
        title = "Audio & playback",
        onNavigateBack = null,
        bottomPadding = 132.dp,
        backgroundColor = Color.Transparent,
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            GlasmioPreferenceGroupTitle("Transitions")
            CardGroup(backdrop) { cardBackdrop ->
                GlasmioSwitchPreference(
                    title = { Text("Crossfade") },
                    description = "Blend the end of one song into the next",
                    checked = crossfade,
                    onCheckedChange = { scope.launch { prefs.setCrossfade(it) } },
                    showTopSeparator = false
                )
                Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text("Crossfade length", color = glass.content, fontSize = 15.sp)
                    Text(
                        text = "${liveDuration.toInt()} s",
                        color = glass.secondaryContent,
                        fontSize = 12.sp
                    )
                    GlasmioSlider(
                        value = liveDuration,
                        onValueChange = { liveDuration = it },
                        onValueChangeFinished = {
                            scope.launch { prefs.setCrossfadeDuration(liveDuration.toInt()) }
                        },
                        valueRange = 1f..20f,
                        backdrop = cardBackdrop,
                        enabled = crossfade,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                GlasmioSwitchPreference(
                    title = { Text("Smart crossfade") },
                    description = "Skips the fade when the two keys clash",
                    checked = crossfadeSmart,
                    onCheckedChange = { scope.launch { prefs.setCrossfadeSmart(it) } },
                    isEnabled = crossfade
                )
                GlasmioSwitchPreference(
                    title = { Text("Beat-synced crossfade") },
                    description = "Starts the blend on a beat",
                    checked = crossfadeBeat,
                    onCheckedChange = { scope.launch { prefs.setCrossfadeBeatSync(it) } },
                    isEnabled = crossfade
                )
                GlasmioSwitchPreference(
                    title = { Text("Gapless playback") },
                    description = "No silence between album tracks",
                    checked = gapless,
                    onCheckedChange = { scope.launch { prefs.setGapless(it) } }
                )
            }
        }
        item {
            GlasmioPreferenceGroupTitle("Equalizer")
            CardGroup(backdrop) { cardBackdrop ->
                GlasmioSwitchPreference(
                    title = { Text("Equalizer") },
                    description = "Per-band tone shaping",
                    checked = eqEnabled,
                    onCheckedChange = { scope.launch { prefs.setEqualizerEnabled(it) } },
                    showTopSeparator = false
                )
                Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text("Headroom", color = glass.content, fontSize = 15.sp)
                    Text(
                        text = "-${"%.1f".format(liveHeadroom)} dB",
                        color = glass.secondaryContent,
                        fontSize = 12.sp
                    )
                    GlasmioSlider(
                        value = liveHeadroom,
                        onValueChange = { liveHeadroom = it },
                        onValueChangeFinished = {
                            scope.launch { prefs.setEqHeadroomDb(liveHeadroom) }
                        },
                        valueRange = 0f..18f,
                        backdrop = cardBackdrop,
                        enabled = eqEnabled,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        item {
            GlasmioPreferenceGroupTitle("Effects")
            CardGroup(backdrop) { cardBackdrop ->
                Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text("Surround depth", color = glass.content, fontSize = 15.sp)
                    Text(
                        text = "${(liveSurround * 100).toInt()}%",
                        color = glass.secondaryContent,
                        fontSize = 12.sp
                    )
                    GlasmioSlider(
                        value = liveSurround,
                        onValueChange = { liveSurround = it },
                        onValueChangeFinished = {
                            scope.launch { prefs.setSurroundDepth(liveSurround) }
                        },
                        valueRange = 0f..1f,
                        backdrop = cardBackdrop,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
