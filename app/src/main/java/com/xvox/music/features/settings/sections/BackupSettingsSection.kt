package com.xvox.music.features.settings.sections

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.core.ui.overlay.XvoxBox
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Xvox Backup & Restore:
 * Exports all settings, playlists, custom covers, images, and history into a .xvox zip archive.
 * Restores smoothly in-place with an applying loading dialog, without killing the app.
 */
@Composable
fun BackupSettingsSection(state: SettingsState, viewModel: SettingsViewModel) {
    val colors = XvoxTheme.colors
    val context = LocalContext.current
    val haptics = LocalXvoxHaptics.current
    val overlays = LocalXvoxOverlayController.current
    val scope = rememberCoroutineScope()

    var restoring by remember { mutableStateOf(false) }
    var restoreComplete by remember { mutableStateOf(false) }
    var restoreSuccess by remember { mutableStateOf(true) }

    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) { exportFullXvox(context, uri) }
            if (ok) {
                haptics.tap()
                overlays.showP("Full backup saved (.xvox)")
            } else {
                overlays.showP("Backup export failed")
            }
        }
    }

    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        restoring = true
        restoreComplete = false
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                delay(800) // smooth UX feel
                restoreFullXvox(context, uri)
            }
            restoreSuccess = ok
            restoring = false
            restoreComplete = true
        }
    }

    if (restoring) {
        XvoxBox(onDismiss = {}) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = colors.primaryAccent, strokeWidth = 3.dp)
                Spacer(Modifier.height(16.dp))
                Text("Restoring backup…", color = colors.primaryText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Applying settings and custom data", color = colors.secondaryText, fontSize = 12.sp)
            }
        }
    }

    if (restoreComplete) {
        XvoxBox(onDismiss = { restoreComplete = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (restoreSuccess) "Backup Restored Successfully" else "Restore Failed",
                    color = if (restoreSuccess) colors.primaryAccent else colors.primaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (restoreSuccess) "All your settings, playlists, covers and data have been restored."
                    else "The selected file could not be parsed as a valid XVOX backup.",
                    color = colors.secondaryText,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Spacer(Modifier.height(18.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.primaryAccent)
                        .xvoxPressScale {
                            haptics.tap()
                            restoreComplete = false
                        }
                        .padding(horizontal = 28.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("OK", color = colors.background, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Full backup includes settings, playlists, custom covers, avatars, and history.",
            color = colors.primaryAccent,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth()
        )

        BigAction("Export complete backup (.xvox)", "Save all preferences, playlists & images", colors) {
            haptics.tap()
            create.launch("XVOX-backup-${System.currentTimeMillis()}.xvox")
        }
        BigAction("Restore from .xvox", "Apply backup directly without leaving the app", colors) {
            haptics.tap()
            open.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
        }
    }
}

@Composable
private fun BigAction(label: String, subtitle: String, colors: com.xvox.music.core.design.theme.XvoxPalette, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.cardElevated)
            .xvoxPressScale(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(label, color = colors.primaryAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = colors.secondaryText, fontSize = 11.sp)
    }
}

private fun exportFullXvox(context: Context, uri: Uri): Boolean {
    return runCatching {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            ZipOutputStream(out).use { zip ->
                // 1. DataStore preferences
                val datastoreDir = File(context.filesDir, "datastore")
                if (datastoreDir.exists()) {
                    datastoreDir.listFiles()?.forEach { file ->
                        zip.putNextEntry(ZipEntry("datastore/${file.name}"))
                        file.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }

                // 2. Custom images / covers
                val imagesDir = File(context.filesDir, "custom_images")
                if (imagesDir.exists()) {
                    imagesDir.listFiles()?.forEach { file ->
                        zip.putNextEntry(ZipEntry("custom_images/${file.name}"))
                        file.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }

                // 3. Info metadata
                zip.putNextEntry(ZipEntry("info.txt"))
                zip.write("XVOX complete backup v2\nCreated: ${System.currentTimeMillis()}\n".toByteArray())
                zip.closeEntry()
            }
        }
        true
    }.getOrDefault(false)
}

private fun restoreFullXvox(context: Context, uri: Uri): Boolean {
    return runCatching {
        var restoredAny = false
        context.contentResolver.openInputStream(uri)?.use { input ->
            val zip = ZipInputStream(input)
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                if (name.startsWith("datastore/")) {
                    val target = File(context.filesDir, name)
                    target.parentFile?.mkdirs()
                    target.outputStream().use { out -> zip.copyTo(out) }
                    target.setLastModified(System.currentTimeMillis())
                    restoredAny = true
                } else if (name.startsWith("custom_images/")) {
                    val target = File(context.filesDir, name)
                    target.parentFile?.mkdirs()
                    target.outputStream().use { out -> zip.copyTo(out) }
                    restoredAny = true
                } else if (name == "xvox_preferences.preferences_pb") {
                    // Legacy single-file backup support
                    val target = File(context.filesDir, "datastore/xvox_preferences.preferences_pb")
                    target.parentFile?.mkdirs()
                    target.outputStream().use { out -> zip.copyTo(out) }
                    target.setLastModified(System.currentTimeMillis())
                    restoredAny = true
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        restoredAny
    }.getOrDefault(false)
}
