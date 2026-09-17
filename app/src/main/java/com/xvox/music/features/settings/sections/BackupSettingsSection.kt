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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.core.ui.overlay.XvoxBox
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Xvox Backup & Restore:
 * Exports all settings, custom images, colors, preferences, playlists, and covers.
 * Formatted as: xvoxbackup(YYYY-MM-DD).xvox
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

    val dateStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
    val defaultBackupFileName = "xvoxbackup($dateStr).xvox"

    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) { exportFullXvox(context, uri) }
            if (ok) {
                haptics.tap()
                overlays.showP("Backup saved: $defaultBackupFileName")
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
                delay(600)
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
                modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = colors.primaryAccent, strokeWidth = 3.dp)
                Spacer(Modifier.height(14.dp))
                Text("Restoring backup…", color = colors.primaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
                    text = if (restoreSuccess) "Backup Restored" else "Restore Failed",
                    color = if (restoreSuccess) colors.primaryAccent else colors.primaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (restoreSuccess) "Your settings, playlists, covers and data have been restored."
                    else "This isn't xvoxbackup file",
                    color = colors.secondaryText,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.primaryAccent)
                        .xvoxPressScale {
                            haptics.tap()
                            restoreComplete = false
                        }
                        .padding(horizontal = 28.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("OK", color = colors.background, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Save or restore your settings, playlists, custom covers, and colors.",
            color = colors.secondaryText,
            fontSize = 11.5.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Export Button
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.cardElevated)
                    .xvoxPressScale {
                        haptics.tap()
                        create.launch(defaultBackupFileName)
                    }
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_xvox_share),
                    contentDescription = null,
                    tint = colors.primaryAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Export Backup", color = colors.primaryText, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            }

            // Import Button
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.cardElevated)
                    .xvoxPressScale {
                        haptics.tap()
                        open.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                    }
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_xvox_refresh),
                    contentDescription = null,
                    tint = colors.primaryAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Import Backup", color = colors.primaryText, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun exportFullXvox(context: Context, uri: Uri): Boolean {
    return runCatching {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            ZipOutputStream(out).use { zip ->
                // Signature entry to identify valid xvox backup
                zip.putNextEntry(ZipEntry("xvox_signature.txt"))
                zip.write("XVOX_BACKUP_V1\n".toByteArray())
                zip.closeEntry()

                // DataStore preferences
                val datastoreDir = File(context.filesDir, "datastore")
                if (datastoreDir.exists()) {
                    datastoreDir.listFiles()?.forEach { file ->
                        zip.putNextEntry(ZipEntry("datastore/${file.name}"))
                        file.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }

                // Custom images / covers
                val imagesDir = File(context.filesDir, "custom_images")
                if (imagesDir.exists()) {
                    imagesDir.listFiles()?.forEach { file ->
                        zip.putNextEntry(ZipEntry("custom_images/${file.name}"))
                        file.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            }
        }
        true
    }.getOrDefault(false)
}

private fun restoreFullXvox(context: Context, uri: Uri): Boolean {
    return runCatching {
        var isValidXvoxBackup = false
        var restoredAny = false

        context.contentResolver.openInputStream(uri)?.use { input ->
            val zip = ZipInputStream(input)
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                if (name == "xvox_signature.txt" || name.startsWith("datastore/") || name == "xvox_preferences.preferences_pb") {
                    isValidXvoxBackup = true
                }

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
        isValidXvoxBackup && restoredAny
    }.getOrDefault(false)
}
