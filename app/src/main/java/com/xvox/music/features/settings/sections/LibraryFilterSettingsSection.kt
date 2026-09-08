package com.xvox.music.features.settings.sections

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.features.home.FolderInfo
import com.xvox.music.features.home.FolderPaths
import com.xvox.music.features.home.HomeViewModel
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.components.SettingsChoiceRow
import com.xvox.music.features.settings.components.SettingsPreviewFrame
import kotlin.math.roundToInt

@Composable
fun LibraryFilterSettingsSection(state: SettingsState, viewModel: SettingsViewModel, homeViewModel: HomeViewModel) {
    val colors = XvoxTheme.colors
    val overlays = LocalXvoxOverlayController.current
    val folders by homeViewModel.folders.collectAsState()
    val library by homeViewModel.state.collectAsState()
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsPreviewFrame("Library preview") {
            Text("${library.songs.size} visible songs", color = colors.primaryAccent, fontWeight = FontWeight.Bold)
            Text("${folders.sumOf { it.songCount } - library.songs.size} filtered out",
                color = colors.secondaryText, fontSize = 12.sp)
        }
        Text("Minimum length", color = colors.primaryText, fontWeight = FontWeight.SemiBold)
        SettingsChoiceRow(listOf("0" to "Off", "15" to "15s", "30" to "30s", "60" to "1m", "custom" to "Custom"),
            if (state.ignoreBelowSec in listOf(0, 15, 30, 60)) state.ignoreBelowSec.toString() else "custom") { value ->
            if (value == "custom") overlays.showBox("Custom minimum duration") {
                CustomThresholdBox(state.ignoreBelowSec, false, onCancel = overlays::hideBox) { seconds ->
                    viewModel.setIgnoreBelowSec(seconds); overlays.hideBox()
                }
            } else viewModel.setIgnoreBelowSec(value.toInt())
        }
        Text("Minimum size", color = colors.primaryText, fontWeight = FontWeight.SemiBold)
        SettingsChoiceRow(listOf("0" to "Off", "100" to "100 KB", "500" to "500 KB", "1024" to "1 MB", "custom" to "Custom"),
            if (state.ignoreBelowKb in listOf(0, 100, 500, 1024)) state.ignoreBelowKb.toString() else "custom") { value ->
            if (value == "custom") overlays.showBox("Custom minimum file size") {
                CustomThresholdBox(state.ignoreBelowKb, true, onCancel = overlays::hideBox) { kb ->
                    viewModel.setIgnoreBelowKb(kb); overlays.hideBox()
                }
            } else viewModel.setIgnoreBelowKb(value.toInt())
        }
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.card)
            .xvoxPressScale {
                overlays.showBox("Exclude folders") {
                    val liveFolders by homeViewModel.folders.collectAsState()
                    FolderBrowserBox(liveFolders, state.ignoredFolders, onApply = { excluded ->
                        viewModel.setIgnoredFolders(excluded)
                        overlays.hideBox()
                        overlays.showP("Folder exclusions updated")
                    })
                }
            }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_xvox_folder), null, tint = colors.primaryAccent, modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text("Folders", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("${state.ignoredFolders.size} excluded", color = colors.secondaryText, fontSize = 11.sp)
            }
            Icon(painterResource(R.drawable.ic_xvox_caret_right), "Open folder browser", tint = colors.secondaryText, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun CustomThresholdBox(initial: Int, size: Boolean, onCancel: () -> Unit, onApply: (Int) -> Unit) {
    val colors = XvoxTheme.colors
    var input by remember { mutableStateOf(initial.toString()) }
    var unit by remember { mutableStateOf(if (size) "KB" else "Seconds") }
    val factor = if (unit == "MB") 1024 else if (unit == "Minutes") 60 else 1
    val max = if (size) 10_485_760 else 86_400
    val number = input.toDoubleOrNull()?.times(factor)
    val valid = number != null && number.isFinite() && number in 0.0..max.toDouble()
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (size) "Files below this size are ignored." else "Audio below this duration is ignored.", color = colors.secondaryText, fontSize = 13.sp)
        OutlinedTextField(value = input, onValueChange = { if (it.length <= 12) input = it },
            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            label = { Text("Minimum $unit") }, isError = !valid,
            supportingText = { Text(if (valid) "0 disables this filter" else if (size) "Enter 0–10 GB" else "Enter 0–24 hours") },
            modifier = Modifier.fillMaxWidth())
        val units = if (size) listOf("KB", "MB") else listOf("Seconds", "Minutes")
        SettingsChoiceRow(units.map { it to it }, unit) { next ->
            if (valid) input = (number / (if (next == "MB") 1024 else if (next == "Minutes") 60 else 1)).toString()
            unit = next
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel) { Text("Cancel") }
            Button(enabled = valid, onClick = { number?.let { onApply(it.roundToInt()) } }) { Text("Apply") }
        }
    }
}

/** Virtual file manager backed by MediaStore: no broad storage permission, no file mutations. */
@Composable
private fun FolderBrowserBox(folders: List<FolderInfo>, initial: Set<String>, onApply: (Set<String>) -> Unit) {
    val colors = XvoxTheme.colors
    var path by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(initial) }
    val paths = remember(folders) { folders.map { FolderPaths.normalize(it.path) }.distinct() }
    val roots = remember(paths) {
        paths.map { full ->
            if (FolderPaths.contains("/storage/emulated/0", full)) "/storage/emulated/0"
            else if (full.startsWith("/storage/")) "/storage/" + full.removePrefix("/storage/").substringBefore('/')
            else "/" + full.trimStart('/').substringBefore('/')
        }.distinct().sorted()
    }
    val children = remember(paths, path) {
        if (path.isEmpty()) roots else paths.mapNotNull { full ->
            if (!full.startsWith("$path/")) return@mapNotNull null
            val name = full.removePrefix("$path/").substringBefore('/')
            if (name.isBlank()) null else "$path/$name"
        }.distinct().sorted()
    }
    fun up() { path = if (path in roots) "" else path.substringBeforeLast('/', "").takeIf { it != "/" }.orEmpty() }
    BackHandler(path.isNotEmpty()) { up() }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Checked = excluded, including all subfolders. Nothing is deleted.", color = colors.secondaryText, fontSize = 12.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (path.isNotEmpty()) Icon(painterResource(R.drawable.ic_xvox_arrow_left), "Parent folder", tint = colors.primaryAccent,
                modifier = Modifier.size(44.dp).clickable { up() }.padding(12.dp))
            Text(path.ifEmpty { "Device storage" }, color = colors.primaryText, fontSize = 12.sp, modifier = Modifier.weight(1f))
        }
        LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(max = 360.dp)) {
            items(children, key = { it }) { child ->
                val matching = folders.filter { FolderPaths.contains(child, it.path) }
                val checked = selected.any { FolderPaths.contains(it, child) || it == child.substringAfterLast('/') }
                val parentSelection = selected.firstOrNull { it != child && FolderPaths.contains(it, child) }
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(12.dp)).background(colors.card)
                    .clickable { path = child }.padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = checked, enabled = parentSelection == null, onCheckedChange = { value ->
                        selected = if (value) selected.filterNot { FolderPaths.contains(child, it) }.toSet() + child
                            else selected - child - child.substringAfterLast('/')
                    })
                    Icon(painterResource(R.drawable.ic_xvox_folder), null, tint = colors.primaryAccent, modifier = Modifier.size(20.dp))
                    Column(Modifier.weight(1f).padding(10.dp)) {
                        Text(if (child == "/storage/emulated/0") "Internal storage" else child.substringAfterLast('/'), color = colors.primaryText, fontSize = 13.sp)
                        Text("${matching.sumOf { it.songCount }} songs${if (parentSelection != null) " · Parent excluded" else ""}", color = colors.secondaryText, fontSize = 10.sp)
                    }
                    Icon(painterResource(R.drawable.ic_xvox_caret_right), "Open folder", tint = colors.mutedText, modifier = Modifier.size(16.dp))
                }
            }
            if (children.isEmpty()) item {
                Text("No subfolders with indexed audio here", color = colors.mutedText, fontSize = 12.sp, modifier = Modifier.padding(12.dp))
            }
        }
        if (selected.isNotEmpty()) TextButton(onClick = { selected = emptySet() }) { Text("Clear exclusions (${selected.size})") }
        Button(onClick = { onApply(selected) }, modifier = Modifier.fillMaxWidth()) { Text("Apply folder exclusions") }
    }
}
