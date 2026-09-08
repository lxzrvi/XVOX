package com.xvox.music.split

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.core.ui.overlay.XvoxOverlayController
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.components.PinnedSettingsEditor
import com.xvox.music.features.settings.components.SettingsPreviewFrame
import com.xvox.music.features.settings.components.SettingsToggle
import com.xvox.music.player.playback.MainPlayerViewModel

@Composable
fun XvoxSplitPanel(playerViewModel: MainPlayerViewModel = viewModel(), settingsViewModel: SettingsViewModel = viewModel()) {
    val context = LocalContext.current
    val colors = XvoxTheme.colors
    val split by XvoxSplitRepository.state.collectAsState()
    val player by playerViewModel.state.collectAsState()
    val settings by settingsViewModel.state.collectAsState()
    val overlays = LocalXvoxOverlayController.current
    var consent by remember { mutableStateOf(false) }
    var mobile by remember { mutableStateOf(false) }
    PinnedSettingsEditor(preview = {
        SettingsPreviewFrame("XvoxSplit · real vocal / instrumental stems") {
            Text("${split.ready}/${split.total} ready · ${split.busy} processing", color = colors.primaryAccent, fontWeight = FontWeight.Bold)
            Text(when { split.autoPaused -> "Paused after rapid skips"; split.active && split.currentId != null && (split.currentId !in split.readyTracks || split.currentId in split.normalIds) -> "This track is playing normally; prepared tracks remain available"
                split.active -> "Ready tracks: instrumental ↔ vocals"; split.running -> "Normal playback until the first ${split.gateIds.size} tracks are ready"; else -> split.stage },
                color = colors.primaryText, fontSize = 12.sp)
            if (split.running && !split.modelReady) LinearProgressIndicator(progress = { split.modelProgress }, modifier = Modifier.fillMaxWidth())
            Text("True AI separation can contain leakage/artifacts. ‘Beat’ means accompaniment, not a drum-only stem.", color = colors.secondaryText, fontSize = 10.sp)
        }
    }, controls = {
        Text("Battery & storage warning", color = colors.primaryText, fontWeight = FontWeight.Bold)
        Text("Initial setup downloads a 28.3 MB model. Processing can take minutes and uses substantial CPU, memory and battery. Prepared audio uses about 10 MB per minute plus temporary decoding space. Prefer Wi-Fi and charging; normal playback remains available.", color = colors.secondaryText, fontSize = 12.sp)
        Text("First up to two queued tracks are prepared, then the queue continues in the background. A slower phone may run out of ready tracks; that song stays normal until ready. Three rapid manual track changes within four seconds switch XvoxSplit off automatically.", color = colors.secondaryText, fontSize = 11.sp)
        SettingsToggle("I understand the processing cost", null, consent) { consent = it }
        if (!split.modelReady) SettingsToggle("Allow mobile data for model download", "No music is uploaded", mobile) { mobile = it }
        Button(enabled = consent && split.initialized && !split.running && player.queue.isNotEmpty(), modifier = Modifier.fillMaxWidth(), onClick = {
            XvoxSplitRepository.start(context, player.queue, player.currentSongId, mobile)
        }) { Text(if (split.modelReady) "Prepare queue & enable XvoxSplit" else "Download model & prepare first tracks") }
        if (split.running || split.requested) OutlinedButton(onClick = { XvoxSplitRepository.stop() }, modifier = Modifier.fillMaxWidth()) { Text("Stop preparation · back to normal") }
        SettingsToggle("Show progress pill", "Before the star in Now Playing", settings.splitShowPill, settingsViewModel::setSplitShowPill)
        SettingsToggle("Hide XvoxSplit collection", "Hide it from Home and the Liked/Split cycle", settings.splitHideCollection, settingsViewModel::setSplitHideCollection)
        Text("Queue / prepared tracks", color = colors.primaryText, fontWeight = FontWeight.SemiBold)
        Text("Tap a pending/working row to cancel it. Hold a ready row for Normal, XvoxSplit, Save/Add or removal options.", color = colors.secondaryText, fontSize = 10.sp)
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 460.dp)) {
            items(split.tracks, key = { it.id }) { track ->
                val working = track.status in setOf(SplitStatus.QUEUED, SplitStatus.DECODING, SplitStatus.SEPARATING)
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(12.dp)).background(colors.card)
                    .combinedClickable(hapticFeedbackEnabled = false,
                        onClick = { if (working) XvoxSplitRepository.cancel(track.id) else showSplitTrackOptions(overlays, track.id) },
                        onLongClick = { showSplitTrackOptions(overlays, track.id) }).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_xvox_split), null, tint = if (track.status == SplitStatus.READY) colors.primaryAccent else colors.mutedText, modifier = Modifier.size(22.dp))
                    Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                        Text(track.title, color = colors.primaryText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(track.error ?: "${track.status.name.lowercase()}${if (working) " · ${((if (split.processingId == track.id) split.processingProgress else track.progress) * 100).toInt()}%" else ""}${if (track.saved) " · saved" else ""}", color = colors.secondaryText, fontSize = 10.sp)
                    }
                    if (working) Text("Stop", color = colors.primaryAccent, fontSize = 11.sp)
                }
            }
        }
        Text("Model: UVR MDX-Net 9482, credited to UVR/Anjok07 and contributors, Kuielab and the sherpa-onnx distribution. Model downloaded on demand and checksum verified. Separation runs on-device; originals are not modified.", color = colors.mutedText, fontSize = 10.sp)
    })
}

fun showSplitTrackOptions(overlays: XvoxOverlayController, id: Long) {
    overlays.showBox("XvoxSplit track") {
        val state by XvoxSplitRepository.state.collectAsState()
        val track = state.tracks.firstOrNull { it.id == id }
        if (track == null) Text("This prepared version is no longer available", color = XvoxTheme.colors.secondaryText)
        else Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(track.title, color = XvoxTheme.colors.primaryText, fontWeight = FontWeight.Bold)
            if (track.status == SplitStatus.READY) {
                Button(onClick = { XvoxSplitRepository.useSplit(id, resetRapidGuard = true); overlays.hideBox() }, modifier = Modifier.fillMaxWidth()) { Text("Use XvoxSplit") }
                OutlinedButton(onClick = { XvoxSplitRepository.useNormal(id); overlays.hideBox() }, modifier = Modifier.fillMaxWidth()) { Text("Back to normal") }
                OutlinedButton(onClick = { XvoxSplitRepository.save(id, !track.saved); overlays.hideBox() }, modifier = Modifier.fillMaxWidth()) { Text(if (track.saved) "Remove from saved XvoxSplit" else "Add / save to XvoxSplit") }
                TextButton(onClick = { XvoxSplitRepository.removePrepared(id); overlays.hideBox() }) { Text("Delete prepared copy (keep original)") }
            } else if (track.status in setOf(SplitStatus.QUEUED, SplitStatus.DECODING, SplitStatus.SEPARATING)) {
                Button(onClick = { XvoxSplitRepository.cancel(id); overlays.hideBox() }) { Text("Stop this track") }
            } else Text(track.error ?: "Start queue preparation again to retry this track", color = XvoxTheme.colors.secondaryText)
        }
    }
}

@Composable
fun XvoxSplitPill() {
    val state by remember { XvoxSplitRepository.state.map { it.copy(processingProgress = 0f, modelProgress = 0f) }.distinctUntilChanged() }.collectAsState(initial = SplitState())
    val overlays = LocalXvoxOverlayController.current
    if (!state.showPill || (!state.running && !state.requested && state.tracks.none { it.status == SplitStatus.READY })) return
    val ready = if (state.total > 0) state.ready else state.tracks.count { it.status == SplitStatus.READY }
    Row(Modifier.padding(end = 5.dp).clip(RoundedCornerShape(20.dp)).background(XvoxTheme.colors.card)
        .xvoxPressScale { overlays.showBox("XvoxSplit") { XvoxSplitPanel() } }
        .padding(horizontal = 8.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(R.drawable.ic_xvox_split), "XvoxSplit: $ready ready, ${state.busy} processing", tint = XvoxTheme.colors.primaryAccent, modifier = Modifier.size(14.dp))
        Text(if (state.total > 0) " $ready/${state.total} · ${state.busy}" else " $ready ready", color = XvoxTheme.colors.primaryText, fontSize = 9.sp)
    }
}
