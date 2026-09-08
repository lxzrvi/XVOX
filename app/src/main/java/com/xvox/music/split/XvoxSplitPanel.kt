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
        SettingsPreviewFrame("XvoxSplit · beat left, vocal right") {
            Text("${split.ready}/${split.total} ready · ${split.busy} working", color = colors.primaryAccent, fontWeight = FontWeight.Bold)
            Text(
                when {
                    split.autoPaused -> "Paused after rapid skips"
                    split.active -> "On for ready tracks"
                    split.running -> "Preparing"
                    else -> split.stage
                },
                color = colors.primaryText, fontSize = 12.sp
            )
            if (!split.modelReady) LinearProgressIndicator(progress = { split.modelProgress }, modifier = Modifier.fillMaxWidth())
        }
    }, controls = {
        // Step 1 — the model. Kept separate from the queue so a blocked background service can
        // never be the reason the download does not happen.
        if (!split.modelReady) {
            Text("Separation model · 28.3 MB", color = colors.primaryText, fontWeight = FontWeight.SemiBold)
            SettingsToggle("Allow mobile data", null, mobile) { mobile = it }
            Button(onClick = { XvoxSplitRepository.downloadModel(context, mobile) }, modifier = Modifier.fillMaxWidth()) {
                Text(if (split.modelProgress > 0f) "Downloading ${(split.modelProgress * 100).toInt()}%" else "Download model")
            }
        }

        Text("Uses noticeable CPU, battery and storage.", color = colors.secondaryText, fontSize = 12.sp)
        SettingsToggle("I understand", null, consent) { consent = it }

        Button(enabled = consent && split.initialized && !split.running && player.queue.isNotEmpty(), modifier = Modifier.fillMaxWidth(), onClick = {
            XvoxSplitRepository.start(context, player.queue, player.currentSongId, mobile)
        }) { Text("Prepare queue") }

        if (split.running || split.requested) OutlinedButton(onClick = { XvoxSplitRepository.stop() }, modifier = Modifier.fillMaxWidth()) { Text("Stop") }

        SettingsToggle("Progress pill", null, settings.splitShowPill, settingsViewModel::setSplitShowPill)
        SettingsToggle("Hide collection", null, settings.splitHideCollection, settingsViewModel::setSplitHideCollection)

        Text("Tracks", color = colors.primaryText, fontWeight = FontWeight.SemiBold)
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
        Text("Model: UVR MDX-Net 9482 · on-device, originals untouched.", color = colors.mutedText, fontSize = 10.sp)
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
