// NAYI FILE BANAO (poori file) - koi extra kaam nahi. Path: app/src/main/java/com/xvox/music/features/settings/SettingsLanding.kt
package com.xvox.music.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.blur.xvoxGlassRim
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.core.ui.overlay.XvoxBoxPresentation
import com.xvox.music.core.ui.overlay.xvoxBoxScroll
import com.xvox.music.features.settings.components.XvoxTransactionalFooterActions
import com.xvox.music.features.settings.sections.EqualizerFooterActions
import com.xvox.music.features.settings.sections.EqualizerSettingsSection
import com.xvox.music.features.settings.sections.HeadsetSettingsDraftSection
import com.xvox.music.features.settings.sections.PlaybackSettingsDraftSection
import com.xvox.music.features.settings.sections.ThreeDSoundDraftSection

// BitChord-style settings: a big page title, inset grouped lists with an uppercase header,
// rows = glyph + title (+ subtitle) + chevron, hairline dividers between rows.

private val GroupInset = 14.dp
private val RowInset = 16.dp

@Composable
fun SettingsLandingGroup(header: String?, content: @Composable () -> Unit) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(18.dp)
    Column(Modifier.fillMaxWidth()) {
        if (header != null) {
            Text(
                header.uppercase(), color = colors.mutedText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                letterSpacing = .6.sp, modifier = Modifier.padding(start = GroupInset + 6.dp, top = 18.dp, bottom = 8.dp)
            )
        } else Spacer(Modifier.height(18.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = GroupInset)
                .clip(shape)
                .background(colors.card)
                .xvoxGlassRim(shape)
        ) { content() }
    }
}

@Composable
fun SettingsLandingDivider() {
    Box(
        Modifier.fillMaxWidth().padding(start = RowInset + 24.dp + 14.dp).height(.5.dp)
            .background(XvoxTheme.colors.cardBorder.copy(alpha = .55f))
    )
}

@Composable
fun SettingsLandingRow(iconRes: Int, title: String, subtitle: String? = null, onClick: () -> Unit) {
    val colors = XvoxTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 54.dp)
            .padding(horizontal = RowInset, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(iconRes), null, tint = colors.primaryText, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = colors.primaryText, fontSize = 15.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, color = colors.mutedText, fontSize = 12.sp, lineHeight = 15.sp, maxLines = 2)
        }
        Icon(painterResource(R.drawable.ic_xvox_caret_right), null, tint = colors.mutedText, modifier = Modifier.size(16.dp))
    }
}

/** Large page title; with [onBack] it becomes a sub-page header with a back arrow. */
@Composable
fun SettingsPageTitle(title: String, onBack: (() -> Unit)? = null) {
    val colors = XvoxTheme.colors
    Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            Box(
                Modifier.size(40.dp).clip(CircleShape)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onBack),
                contentAlignment = Alignment.Center
            ) { Icon(painterResource(R.drawable.ic_xvox_arrow_left), "Back", tint = colors.primaryText, modifier = Modifier.size(22.dp)) }
        }
        Text(
            title, color = colors.primaryText, fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = if (onBack != null) 6.dp else 6.dp)
        )
    }
}

/**
 * Equalizer, 3D sound, crossfade & playback and audio output - moved out of Now Playing into Settings.
 * Each row opens its editor in a sheet; changes apply live.
 */
@Composable
fun SoundSettingsPage(state: SettingsState, viewModel: SettingsViewModel) {
    val overlays = LocalXvoxOverlayController.current
    fun apply3d(d: SettingsState) {
        viewModel.setStereoWidening(d.stereoWidening); viewModel.setSurroundWidth(d.surroundWidth)
        viewModel.setSurroundDepth(d.surroundDepth); viewModel.setSurroundPanSpeed(d.surroundPanSpeed)
        viewModel.setHrtf(d.hrtf); viewModel.setBalance(d.balance)
    }
    fun applyCrossfade(d: SettingsState) {
        viewModel.setCrossfade(d.crossfade); viewModel.setGapless(d.gapless)
        viewModel.setCrossfadeDuration(d.crossfadeDuration); viewModel.setCrossfadeSmart(d.crossfadeSmart)
        viewModel.setCrossfadeClashControl(d.crossfadeClashControl); viewModel.setCrossfadeBeatSync(d.crossfadeBeatSync)
        viewModel.setCrossfadeCurveStyle(d.crossfadeCurveStyle)
    }
    fun applyOutput(d: SettingsState) {
        viewModel.setAudioOutputRoute(d.audioOutputRoute); viewModel.setBtConnectAction(d.btConnectAction)
        viewModel.setPlayOnHeadsetConnect(d.playOnHeadsetConnect); viewModel.setBtDisconnectAction(d.btDisconnectAction)
        viewModel.setPauseOnHeadphoneDisconnect(d.pauseOnHeadphoneDisconnect)
    }
    SettingsLandingGroup(header = "Sound") {
        SettingsLandingRow(R.drawable.ic_xvox_equalizer, "Equalizer", "Bands, presets and bass") {
            overlays.showBox(
                title = "Equalizer",
                presentation = XvoxBoxPresentation.EQUALIZER,
                bottomAction = {
                    EqualizerFooterActions(
                        onCancel = overlays::hideBox,
                        onReset = viewModel::resetEqualizerControls,
                        onDone = overlays::hideBox
                    )
                }
            ) {
                EqualizerSettingsSection(state = state, viewModel = viewModel, onCancel = overlays::hideBox, onDone = overlays::hideBox, showFooter = false)
            }
        }
        SettingsLandingDivider()
        SettingsLandingRow(R.drawable.ic_xvox_waveform, "3D sound", "Stereo widening, surround and HRTF") {
            overlays.showBox("3D Sound") {
                var draft by remember { mutableStateOf(state) }
                val scroll = androidx.compose.foundation.rememberScrollState()
                Column(Modifier.fillMaxWidth().verticalScroll(scroll).xvoxBoxScroll(scroll)) {
                    ThreeDSoundDraftSection(state = draft, onStateChange = { draft = it; apply3d(it) })
                }
            }
        }
        SettingsLandingDivider()
        SettingsLandingRow(R.drawable.ic_xvox_crossfade, "Playback & crossfade", "Crossfade, gapless, beat sync") {
            overlays.showBox("Playback & Crossfade") {
                var draft by remember { mutableStateOf(state) }
                val scroll = androidx.compose.foundation.rememberScrollState()
                Column(Modifier.fillMaxWidth().verticalScroll(scroll).xvoxBoxScroll(scroll)) {
                    PlaybackSettingsDraftSection(state = draft, onStateChange = { draft = it; applyCrossfade(it) })
                }
            }
        }
        SettingsLandingDivider()
        SettingsLandingRow(R.drawable.ic_xvox_bluetooth, "Audio output & Bluetooth", "Output route, connect and disconnect actions") {
            overlays.showBox("Audio Output") {
                var draft by remember { mutableStateOf(state) }
                val scroll = androidx.compose.foundation.rememberScrollState()
                Column(Modifier.fillMaxWidth().verticalScroll(scroll).xvoxBoxScroll(scroll)) {
                    HeadsetSettingsDraftSection(state = draft, onStateChange = { draft = it; applyOutput(it) })
                }
            }
        }
    }
}
