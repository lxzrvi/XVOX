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
import com.xvox.music.artwork.XvoxArtworkCache
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.core.ui.overlay.XvoxBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun AppResetSettingsSection() {
    val colors = XvoxTheme.colors
    val context = LocalContext.current
    val haptics = LocalXvoxHaptics.current
    val overlays = LocalXvoxOverlayController.current
    val scope = rememberCoroutineScope()

    var showResetConfirm by remember { mutableStateOf(false) }
    var resetInProgress by remember { mutableStateOf(false) }
    var resetProgress by remember { mutableFloatStateOf(0f) }

    if (showResetConfirm) {
        XvoxBox(
            title = "Back to start?",
            onDismiss = {
                if (!resetInProgress) showResetConfirm = false
            }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "This will completely erase all your custom settings, playlists, photos, and cached audio data, resetting XVOX to its initial state.",
                    color = colors.secondaryText,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))

                if (resetInProgress) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { resetProgress },
                            color = colors.primaryAccent,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Resetting… ${(resetProgress * 100).toInt()}%",
                            color = colors.primaryAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
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

                        Spacer(Modifier.width(14.dp))

                        Box(
                            modifier = Modifier
                                .width(110.dp)
                                .height(38.dp)
                                .clip(RoundedCornerShape(19.dp))
                                .background(androidx.compose.ui.graphics.Color(0xFFEF4444))
                                .xvoxPressScale {
                                    haptics.heavy()
                                    resetInProgress = true
                                    scope.launch {
                                        for (i in 1..20) {
                                            delay(100)
                                            resetProgress = i / 20f
                                        }
                                        withContext(Dispatchers.IO) {
                                            wipeAllAppData(context)
                                        }
                                        showResetConfirm = false
                                        resetInProgress = false
                                        // Restart process / main activity
                                        val pm = context.packageManager
                                        val intent = pm.getLaunchIntentForPackage(context.packageName)
                                        val mainIntent = android.content.Intent.makeRestartActivityTask(intent?.component)
                                        context.startActivity(mainIntent)
                                        Runtime.getRuntime().exit(0)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Reset", color = androidx.compose.ui.graphics.Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

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
        Text("Erase all data and cleanly return to the startup setup onboarding", color = colors.secondaryText, fontSize = 11.sp)
    }
}

private fun wipeAllAppData(context: Context) {
    runCatching {
        context.cacheDir.deleteRecursively()
        context.cacheDir.mkdirs()
        context.codeCacheDir?.deleteRecursively()
        XvoxArtworkCache.clear()
        val datastoreDir = File(context.filesDir, "datastore")
        datastoreDir.deleteRecursively()
        val imagesDir = File(context.filesDir, "custom_images")
        imagesDir.deleteRecursively()
        File(context.filesDir, "playlists").deleteRecursively()
    }
}
