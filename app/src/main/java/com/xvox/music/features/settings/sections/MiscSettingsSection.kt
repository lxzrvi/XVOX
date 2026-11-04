package com.xvox.music.features.settings.sections

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.AppViewModel
import com.xvox.music.artwork.XvoxArtworkCache
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.core.ui.overlay.XvoxBox
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.components.SettingsControlsEditor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Misc settings:
 * 1. Storage & Cache size display with instant in-app cache clearing.
 * 2. Clear App Data in-app without going to Android system settings.
 * 3. "Back to start" with warning dialog, 5-second progress loading, and full reset back to Setup.
 */
@Composable
fun MiscSettingsSection(
    state: SettingsState,
    viewModel: SettingsViewModel,
    appViewModel: AppViewModel? = null
) {
    val colors = XvoxTheme.colors
    val context = LocalContext.current
    val haptics = LocalXvoxHaptics.current
    val overlays = LocalXvoxOverlayController.current
    val scope = rememberCoroutineScope()

    var cacheSizeBytes by remember { mutableLongStateOf(calculateCacheSize(context)) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showClearDataConfirm by remember { mutableStateOf(false) }
    var resetting by remember { mutableStateOf(false) }

    fun refreshCacheSize() {
        scope.launch {
            cacheSizeBytes = withContext(Dispatchers.IO) { calculateCacheSize(context) }
        }
    }

    if (showResetConfirm) {
        XvoxBox(onDismiss = { showResetConfirm = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Reset to Startup?",
                    color = colors.primaryAccent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "All your data, settings, playlists and history will be erased. This will take you back to the initial setup screen.",
                    color = colors.secondaryText,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(110.dp)
                            .height(38.dp)
                            .clip(RoundedCornerShape(19.dp))
                            .background(colors.cardElevated)
                            .xvoxPressScale {
                                haptics.tap()
                                showResetConfirm = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Cancel", color = colors.secondaryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .width(110.dp)
                            .height(38.dp)
                            .clip(RoundedCornerShape(19.dp))
                            .background(colors.primaryAccent)
                            .xvoxPressScale {
                                haptics.heavy()
                                showResetConfirm = false
                                resetting = true
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        // 5 seconds loading reset as requested
                                        delay(5000)
                                        wipeAllAppData(context)
                                    }
                                    resetting = false
                                    appViewModel?.resetToSetup()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Reset", color = colors.background, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (resetting) {
        XvoxBox(onDismiss = {}) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = colors.primaryAccent, strokeWidth = 3.dp)
                Spacer(Modifier.height(16.dp))
                Text("Resetting XVOX…", color = colors.primaryText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Erasing data and returning to setup", color = colors.secondaryText, fontSize = 12.sp)
            }
        }
    }

    if (showClearDataConfirm) {
        XvoxBox(onDismiss = { showClearDataConfirm = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Clear App Data?", color = colors.primaryAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "All preferences and cached settings will be reset to defaults directly from inside the app.",
                    color = colors.secondaryText,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(110.dp)
                            .height(38.dp)
                            .clip(RoundedCornerShape(19.dp))
                            .background(colors.cardElevated)
                            .xvoxPressScale {
                                haptics.tap()
                                showClearDataConfirm = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Cancel", color = colors.secondaryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .width(110.dp)
                            .height(38.dp)
                            .clip(RoundedCornerShape(19.dp))
                            .background(colors.primaryAccent)
                            .xvoxPressScale {
                                haptics.tap()
                                showClearDataConfirm = false
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        clearAppPreferences(context)
                                    }
                                    refreshCacheSize()
                                    overlays.showP("App data cleared")
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Clear", color = colors.background, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    SettingsControlsEditor(controls = {
        // 1. Cache Storage Manager
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.cardElevated)
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("App cache", color = colors.primaryAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        formatSize(cacheSizeBytes),
                        color = colors.primaryText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.primaryAccent)
                        .xvoxPressScale {
                            haptics.tap()
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    clearAppCache(context)
                                }
                                cacheSizeBytes = 0L
                                overlays.showP("Cache cleared")
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Clear Cache", color = colors.background, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // 2. Clear App Data
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.cardElevated)
                .xvoxPressScale {
                    haptics.tap()
                    showClearDataConfirm = true
                }
                .padding(14.dp)
        ) {
            Text("Clear App Data", color = colors.primaryAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text("Reset all configurations directly from within the app", color = colors.secondaryText, fontSize = 11.sp)
        }

        Spacer(Modifier.height(10.dp))

        // 3. Back to Start
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.cardElevated)
                .xvoxPressScale {
                    haptics.heavy()
                    showResetConfirm = true
                }
                .padding(14.dp)
        ) {
            Text("Back to start", color = colors.primaryAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text("Erase all data and return to the startup setup onboarding", color = colors.secondaryText, fontSize = 11.sp)
        }
    })
}

private fun calculateCacheSize(context: Context): Long {
    var size = 0L
    runCatching {
        context.cacheDir.walkTopDown().filter { it.isFile }.forEach { size += it.length() }
        context.codeCacheDir?.walkTopDown()?.filter { it.isFile }?.forEach { size += it.length() }
    }
    return size
}

private fun clearAppCache(context: Context) {
    runCatching {
        context.cacheDir.deleteRecursively()
        context.cacheDir.mkdirs()
        XvoxArtworkCache.clear()
    }
}

private fun clearAppPreferences(context: Context) {
    runCatching {
        clearAppCache(context)
        val datastoreDir = File(context.filesDir, "datastore")
        datastoreDir.deleteRecursively()
        datastoreDir.mkdirs()
    }
}

private fun wipeAllAppData(context: Context) {
    runCatching {
        clearAppCache(context)
        val datastoreDir = File(context.filesDir, "datastore")
        datastoreDir.deleteRecursively()
        val imagesDir = File(context.filesDir, "custom_images")
        imagesDir.deleteRecursively()
        File(context.filesDir, "playlists").deleteRecursively()
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0.0 KB"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) String.format("%.1f MB", mb)
    else String.format("%.1f KB", kb)
}
