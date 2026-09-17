package com.xvox.music.features.settings.sections

import android.content.Context
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
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
import com.xvox.music.features.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@Composable
fun BackupSettingsSection(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = LocalXvoxHaptics.current

    val currentDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val defaultBackupFileName = "xvoxbackup($currentDateStr).xvox"

    val create = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    exportFullXvox(context, uri)
                }
                if (ok) {
                    Toast.makeText(context, "Backup exported successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val open = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    importFullXvox(context, uri)
                }
                when (result) {
                    ImportResult.SUCCESS -> {
                        Toast.makeText(context, "Backup restored successfully", Toast.LENGTH_SHORT).show()
                        viewModel.refresh()
                    }
                    ImportResult.INVALID_FILE -> {
                        Toast.makeText(context, "This isn't xvoxbackup file", Toast.LENGTH_LONG).show()
                    }
                    ImportResult.ERROR -> {
                        Toast.makeText(context, "Restore failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Export and restore your app settings, preferences, and custom data.",
            color = colors.secondaryText,
            fontSize = 11.5.sp,
            lineHeight = 15.sp,
            modifier = Modifier.padding(bottom = 12.dp)
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

                val dataDir = context.filesDir.parentFile ?: return@use
                val dirsToBackup = listOf("shared_prefs", "databases", "files")

                for (dirName in dirsToBackup) {
                    val dir = File(dataDir, dirName)
                    if (dir.exists() && dir.isDirectory) {
                        dir.walkTopDown().forEach { file ->
                            if (file.isFile) {
                                // Exclude queue-specific files/prefs as requested
                                val relPath = file.relativeTo(dataDir).path
                                if (!relPath.contains("queue", ignoreCase = true)) {
                                    zip.putNextEntry(ZipEntry(relPath))
                                    FileInputStream(file).use { input ->
                                        input.copyTo(zip)
                                    }
                                    zip.closeEntry()
                                }
                            }
                        }
                    }
                }
            }
        }
        true
    }.getOrDefault(false)
}

private enum class ImportResult {
    SUCCESS,
    INVALID_FILE,
    ERROR
}

private fun importFullXvox(context: Context, uri: Uri): ImportResult {
    return runCatching {
        var hasValidSignature = false
        val dataDir = context.filesDir.parentFile ?: return ImportResult.ERROR

        // First pass: verify signature
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "xvox_signature.txt") {
                        val sig = zip.bufferedReader().readLine()
                        if (sig != null && sig.startsWith("XVOX_BACKUP")) {
                            hasValidSignature = true
                            break
                        }
                    }
                    entry = zip.nextEntry
                }
            }
        } ?: return ImportResult.ERROR

        if (!hasValidSignature) {
            return ImportResult.INVALID_FILE
        }

        // Second pass: extract files
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    if (entry.name != "xvox_signature.txt" && !entry.isDirectory) {
                        val targetFile = File(dataDir, entry.name)
                        // Security check: prevent zip slip
                        if (targetFile.canonicalPath.startsWith(dataDir.canonicalPath)) {
                            targetFile.parentFile?.mkdirs()
                            FileOutputStream(targetFile).use { out ->
                                zip.copyTo(out)
                            }
                        }
                    }
                    entry = zip.nextEntry
                }
            }
        } ?: return ImportResult.ERROR

        ImportResult.SUCCESS
    }.getOrElse { ImportResult.ERROR }
}
