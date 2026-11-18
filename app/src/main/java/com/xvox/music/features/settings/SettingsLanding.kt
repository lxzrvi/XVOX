// NAYI FILE BANAO (poori file) - koi extra kaam nahi. Path: app/src/main/java/com/xvox/music/features/settings/SettingsLanding.kt
package com.xvox.music.features.settings

import com.xvox.music.core.design.kit.XvoxTextInput
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
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
import com.xvox.music.core.ui.icons.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.LocalXvoxDesign
import com.xvox.music.core.design.theme.XvoxDesignLanguage
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.design.theme.shapeFor
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

class SettingsSearchEntry(val iconRes: Int, val title: String, val subtitle: String?, val keywords: String, val onClick: () -> Unit)

/** "Search settings" pill under the page title, BitChord style. */
@Composable
fun SettingsSearchBar(query: String, onQueryChange: (String) -> Unit) {
    val colors = XvoxTheme.colors
    val design = LocalXvoxDesign.current
    val shape = design.shapeFor(design.searchRadius)
    val fill = if (design.language == XvoxDesignLanguage.MATERIAL) colors.cardElevated else colors.card
    Row(
        Modifier.fillMaxWidth().padding(horizontal = design.groupInset, vertical = 6.dp).height(design.searchHeight)
            .clip(shape).background(fill).then(if (design.glass) Modifier.xvoxGlassRim(shape) else Modifier).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(R.drawable.ic_xvox_search), null, tint = colors.mutedText, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) Text("Search settings", color = colors.mutedText, fontSize = 15.sp)
            XvoxTextInput(
                value = query, onValueChange = onQueryChange, singleLine = true,
                textStyle = TextStyle(color = colors.primaryText, fontSize = 15.sp),
                cursorBrush = SolidColor(colors.primaryText),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (query.isNotEmpty()) {
            Box(
                Modifier.size(28.dp).clip(CircleShape)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onQueryChange("") },
                contentAlignment = Alignment.Center
            ) { Icon(painterResource(R.drawable.ic_xvox_close), "Clear", tint = colors.mutedText, modifier = Modifier.size(16.dp)) }
        }
    }
}

@Composable
fun SettingsSearchResults(query: String, entries: List<SettingsSearchEntry>) {
    val q = query.trim().lowercase()
    val hits = entries.filter { (it.title + " " + (it.subtitle ?: "") + " " + it.keywords).lowercase().contains(q) }
    if (hits.isEmpty()) {
        Text(
            "No settings match \"$query\"", color = XvoxTheme.colors.mutedText, fontSize = 14.sp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = GroupInset + 6.dp, vertical = 24.dp)
        )
        return
    }
    SettingsLandingGroup(header = "Results") {
        hits.forEachIndexed { i, e ->
            SettingsLandingRow(e.iconRes, e.title, e.subtitle, e.onClick)
            if (i != hits.lastIndex) SettingsLandingDivider()
        }
    }
}

private val GroupInset = 14.dp
private val RowInset = 16.dp

@Composable
fun SettingsLandingGroup(header: String?, content: @Composable () -> Unit) {
    val colors = XvoxTheme.colors
    val design = LocalXvoxDesign.current
    val shape = design.shapeFor(design.groupRadius)
    val material = design.language == XvoxDesignLanguage.MATERIAL
    Column(Modifier.fillMaxWidth()) {
        if (header != null) {
            Text(
                if (design.headerUppercase) header.uppercase() else header,
                color = if (material) colors.primaryAccent else colors.mutedText,
                fontSize = design.headerSize,
                fontWeight = if (material) FontWeight.Medium else FontWeight.SemiBold,
                letterSpacing = if (design.headerUppercase) .6.sp else .1.sp,
                modifier = Modifier.padding(start = design.groupInset + 6.dp, top = 18.dp, bottom = 8.dp)
            )
        } else Spacer(Modifier.height(18.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = design.groupInset)
                .clip(shape)
                .background(if (material) colors.cardElevated else colors.card)
                .then(if (design.glass) Modifier.xvoxGlassRim(shape) else Modifier)
        ) { content() }
    }
}

@Composable
fun SettingsLandingDivider() {
    val design = LocalXvoxDesign.current
    if (!design.rowDividers) return
    val start = if (design.glyphTile) 16.dp + 30.dp + 14.dp else 16.dp + 24.dp + 14.dp
    Box(
        Modifier.fillMaxWidth().padding(start = start).height(.5.dp)
            .background(XvoxTheme.colors.cardBorder.copy(alpha = .55f))
    )
}

@Composable
fun SettingsLandingRow(iconRes: Int, title: String, subtitle: String? = null, onClick: () -> Unit) {
    val colors = XvoxTheme.colors
    val design = LocalXvoxDesign.current
    val material = design.language == XvoxDesignLanguage.MATERIAL
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = design.rowMinHeight)
            .padding(horizontal = RowInset, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (design.glyphTile) {
            // iOS: accent squircle with a white glyph. Material: tonal circle with an accent glyph.
            val tileSize = if (material) 40.dp else 30.dp
            Box(
                Modifier.size(tileSize).clip(if (material) CircleShape else design.shapeFor(design.glyphTileRadius))
                    .background(if (material) colors.primaryAccent.copy(alpha = .16f) else colors.primaryAccent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(iconRes), null,
                    tint = if (material) colors.primaryAccent else Color.White,
                    modifier = Modifier.size(if (material) 22.dp else 18.dp)
                )
            }
            Spacer(Modifier.width(if (material) 16.dp else 14.dp))
        } else {
            Icon(painterResource(iconRes), null, tint = colors.primaryText, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = colors.primaryText, fontSize = if (material) 16.sp else 15.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, color = colors.mutedText, fontSize = 12.sp, lineHeight = 15.sp, maxLines = 2)
        }
        if (!material) {
            Icon(painterResource(R.drawable.ic_xvox_caret_right), null, tint = colors.mutedText, modifier = Modifier.size(16.dp))
        }
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
            title, color = colors.primaryText, fontSize = LocalXvoxDesign.current.titleSize,
            lineHeight = LocalXvoxDesign.current.titleSize * 1.15f, fontWeight = LocalXvoxDesign.current.titleWeight,
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
