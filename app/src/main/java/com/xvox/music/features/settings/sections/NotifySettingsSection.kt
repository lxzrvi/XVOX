package com.xvox.music.features.settings.sections

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.components.SettingsToggle
import com.xvox.music.notifications.XvoxReminderManager
import androidx.core.content.ContextCompat as Compat

/**
 * Notify: XVOX may gently remind you to play music — at any hour, never more than three times in
 * 24 hours. Asks for notification permission, keeps the app alive in the background, and offers
 * a Try-now button so one nudge lands immediately.
 */
@Composable
fun NotifySettingsSection(state: SettingsState, viewModel: SettingsViewModel) {
    val colors = XvoxTheme.colors
    val context = LocalContext.current
    val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        Compat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            XvoxReminderManager.ensureChannel(context)
            viewModel.setRemindersEnabled(true)
            XvoxReminderManager.scheduleNext(context)
            Toast.makeText(context, "Reminders on — see you in a few hours", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Notifications are blocked for XVOX", Toast.LENGTH_SHORT).show()
        }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Friendly reminders to play music — never more than three in 24 hours.",
            color = colors.primaryAccent,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth()
        )

        SettingsToggle(
            title = "Music reminders",
            subtitle = "Max 3 messages every 24 hours",
            checked = state.remindersEnabled,
            onChange = { on ->
                if (on) {
                    if (needsPermission) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else {
                        XvoxReminderManager.ensureChannel(context)
                        viewModel.setRemindersEnabled(true)
                        XvoxReminderManager.scheduleNext(context)
                    }
                } else {
                    viewModel.setRemindersEnabled(false)
                    XvoxReminderManager.cancel(context)
                }
            }
        )

        Text(
            text = "Keep XVOX open in the background so reminders can arrive:",
            color = colors.secondaryText,
            fontSize = 12.sp
        )
        RowAction("Open battery / background settings", colors) {
            runCatching {
                context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }
        RowAction("Open app info (allow notifications)", colors) {
            runCatching {
                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                } else {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                }
                context.startActivity(intent)
            }
        }

        Spacer(Modifier.height(6.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.cardElevated)
                .xvoxPressScale {
                    if (needsPermission) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else {
                        XvoxReminderManager.ensureChannel(context)
                        XvoxReminderManager.fireNow(context)
                        Toast.makeText(context, "Test notification sent", Toast.LENGTH_SHORT).show()
                    }
                }
                .padding(16.dp)
        ) {
            Text("Try now", color = colors.primaryAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("Send one test notification right away", color = colors.secondaryText, fontSize = 11.sp)
        }
    }
}

@Composable
private fun RowAction(label: String, colors: com.xvox.music.core.design.theme.XvoxPalette, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.cardElevated)
            .xvoxPressScale(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp)
    ) {
        Text(label, color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
