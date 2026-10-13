package com.xvox.music.features.settings.sections

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel

@Composable
fun NotifySettingsSection(state: SettingsState, viewModel: SettingsViewModel) {
    val haptics = LocalXvoxHaptics.current
    val context = LocalContext.current
    val overlays = LocalXvoxOverlayController.current
    var permissionRequestInFlight by remember { mutableStateOf(false) }
    // Keep an immediate UI guard as well as the durable preference: a second tap in the small
    // interval before DataStore emits must not launch Android's prompt twice.
    var permissionPromptHandled by remember { mutableStateOf(state.backgroundPlaybackPermissionRequested) }
    androidx.compose.runtime.LaunchedEffect(state.backgroundPlaybackPermissionRequested) {
        if (state.backgroundPlaybackPermissionRequested) permissionPromptHandled = true
    }
    fun notificationPermissionGranted(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionRequestInFlight = false
        if (granted) {
            viewModel.setPersistentBackgroundPlayback(true)
        } else {
            // Do not request again from this setting. Android's notification permission is what
            // makes a foreground media service visibly usable after the app task is removed.
            overlays.showP("Allow notifications in Android settings to enable background playback")
        }
    }

    fun setBackgroundPlayback(enabled: Boolean) {
        haptics.tap()
        if (!enabled) {
            viewModel.setPersistentBackgroundPlayback(false)
        } else if (notificationPermissionGranted()) {
            // Setup or a prior Android Settings grant also counts as the one permission decision.
            if (!permissionPromptHandled) {
                permissionPromptHandled = true
                viewModel.markBackgroundPlaybackPermissionRequested()
            }
            viewModel.setPersistentBackgroundPlayback(true)
        } else if (!permissionPromptHandled && !permissionRequestInFlight) {
            permissionPromptHandled = true
            permissionRequestInFlight = true
            viewModel.markBackgroundPlaybackPermissionRequested()
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            overlays.showP("Allow notifications in Android settings to enable background playback")
        }
    }

    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SettingsToggleRow(
            title = "Background playback",
            subtitle = if (state.persistentBackgroundPlayback) {
                "Keep playing when app is cleared from recents"
            } else {
                "Stop playing when app is closed / cleared"
            },
            checked = state.persistentBackgroundPlayback,
            onCheckedChange = ::setBackgroundPlayback
        )

        SettingsToggleRow(
            title = "Daily music reminders",
            subtitle = "Gentle notification to resume your favourite tracks",
            checked = state.remindersEnabled,
            onCheckedChange = {
                haptics.tap()
                viewModel.setRemindersEnabled(it)
            }
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = XvoxTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = colors.primaryText, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = colors.mutedText, fontSize = 11.sp, lineHeight = 14.sp)
        }
        CompactSettingsSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun CompactSettingsSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .size(width = 42.dp, height = 24.dp)
            .clip(shape)
            .background(if (checked) colors.primaryAccent else colors.cardElevated)
            .border(1.dp, if (checked) colors.primaryAccent else colors.cardBorder.copy(alpha = 0.75f), shape)
            .clickable { onCheckedChange(!checked) }
            .padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}
