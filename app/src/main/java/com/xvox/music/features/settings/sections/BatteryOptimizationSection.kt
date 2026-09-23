package com.xvox.music.features.settings.sections

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale

@Composable
fun BatteryOptimizationSection() {
    val colors = XvoxTheme.colors
    val context = androidx.compose.ui.platform.LocalContext.current
    val powerManager = remember {
        context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    }
    var isIgnoring by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
            } else {
                true
            }
        )
    }

    fun openBatterySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }.onFailure {
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    )
                }
            }
            isIgnoring = powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("Background playback", color = colors.primaryText, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = if (isIgnoring) "Unrestricted" else "Restricted",
                color = if (isIgnoring) colors.primaryAccent else colors.mutedText,
                fontSize = 11.sp,
                fontWeight = if (isIgnoring) FontWeight.SemiBold else FontWeight.Normal
            )
        }
        Text(
            text = if (isIgnoring) "Recheck" else "Allow",
            color = if (isIgnoring) colors.mutedText else colors.primaryAccent,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .xvoxPressScale { openBatterySettings() }
                .padding(horizontal = 6.dp, vertical = 6.dp)
        )
    }
}
