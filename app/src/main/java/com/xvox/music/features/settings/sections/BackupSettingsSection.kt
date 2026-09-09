package com.xvox.music.features.settings.sections

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
import kotlinx.coroutines.flow.first
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Xvox Backup: everything the app remembers (settings, accent, colours, queue-time preferences,
 * profile, custom covers…) is zipped into one `.xvox` file by the user. Restore reads that file,
 * puts every value back and restarts the app so the new settings are picked up everywhere.
 */
@Composable
fun BackupSettingsSection(state: SettingsState, viewModel: SettingsViewModel) {
    val colors = XvoxTheme.colors
    val context = LocalContext.current

    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val ok = exportXvox(context, uri)
        Toast.makeText(context, if (ok) "Backup saved (.xvox)" else "Backup failed", Toast.LENGTH_SHORT).show()
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val ok = restoreXvox(context, uri)
        Toast.makeText(context, if (ok) "Settings restored — restarting XVOX" else "Restore failed", Toast.LENGTH_SHORT).show()
        if (ok) {
            // Restart the process so the in-memory DataStore cache reloads the restored file.
            val activity = context as? android.app.Activity
            activity?.let { act ->
                val pending = android.app.PendingIntent.getActivity(
                    act, 0, android.content.Intent(act, act.javaClass),
                    android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
                )
                val alarm = act.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                alarm.set(android.app.AlarmManager.RTC, System.currentTimeMillis() + 1200, pending)
                act.finishAffinity()
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "One file holds every XVOX setting. Keep it somewhere safe, then restore it on a new phone or after a fresh install.",
            color = colors.secondaryText,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            modifier = Modifier.fillMaxWidth()
        )

        BigAction("Export settings (.xvox)", colors) { create.launch("XVOX-backup-${System.currentTimeMillis()}.xvox") }
        BigAction("Restore from .xvox", colors) { open.launch(arrayOf("application/zip", "application/octet-stream")) }

        Text(
            text = "Restore replaces your current settings with the backup. Music files and playlists you created on your device stay untouched.",
            color = colors.mutedText,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun BigAction(label: String, colors: com.xvox.music.core.design.theme.XvoxPalette, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
            .background(colors.cardElevated)
            .xvoxPressScale(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(label, color = colors.primaryAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("Tap to choose where to save / which file to open", color = colors.secondaryText, fontSize = 11.sp)
    }
}

@Suppress("DEPRECATION")
private fun preferencesFile(context: Context): File? {
    val dir = File(context.filesDir, "datastore")
    return dir.listFiles()?.firstOrNull { it.name.endsWith(".preferences_pb") }
}

private fun exportXvox(context: Context, uri: android.net.Uri): Boolean {
    return runCatching {
        var source = preferencesFile(context)
        // A fresh install may not have a DataStore file yet; reading it once creates it.
        if (source == null) {
            kotlinx.coroutines.runBlocking {
                UserPreferencesRepository(context.applicationContext).preferences.first()
            }
            source = preferencesFile(context)
        }
        val sourceFile = source ?: return false
        context.contentResolver.openOutputStream(uri)?.use { out ->
            ZipOutputStream(out).use { zip ->
                zip.putNextEntry(ZipEntry("xvox_preferences.preferences_pb"))
                sourceFile.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("info.txt"))
                zip.write("XVOX backup\nCreated: ${System.currentTimeMillis()}\n".toByteArray())
                zip.closeEntry()
            }
        }
        true
    }.getOrDefault(false)
}

private fun restoreXvox(context: Context, uri: android.net.Uri): Boolean {
    return runCatching {
        val target = preferencesFile(context) ?: return@runCatching false
        var found: ByteArray? = null
        context.contentResolver.openInputStream(uri)?.use { input ->
            val zip = ZipInputStream(input)
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "xvox_preferences.preferences_pb") {
                    val out = java.io.ByteArrayOutputStream()
                    val chunk = ByteArray(8192)
                    var n = zip.read(chunk)
                    while (n >= 0) {
                        if (n > 0) out.write(chunk, 0, n)
                        n = zip.read(chunk)
                    }
                    found = out.toByteArray()
                    break
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        val bytes = found ?: return@runCatching false
        // Replace the DataStore file while no edit is running; the process restarts right after.
        synchronized(context) {
            target.outputStream().use { out -> out.write(bytes) }
            target.setLastModified(System.currentTimeMillis())
        }
        true
    }.getOrDefault(false)
}
