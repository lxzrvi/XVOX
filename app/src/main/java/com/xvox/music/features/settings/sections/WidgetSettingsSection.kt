package com.xvox.music.features.settings.sections

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxLogoFont
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.components.SettingsSectionCard
import com.xvox.music.features.settings.components.SettingsToggle
import com.xvox.music.features.settings.components.XvoxThinLineSlider
import com.xvox.music.widget.XvoxAppWidgetProvider
import kotlin.math.roundToInt

@Composable
fun WidgetSettingsSection(
    state: SettingsState,
    viewModel: SettingsViewModel
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current
    val context = LocalContext.current

    SettingsSectionCard(title = "Home Screen Widget Customizer", iconRes = R.drawable.ic_xvox_music_note) {
        WidgetLivePreviewCard(
            transparency = state.widgetTransparency,
            theme = state.widgetTheme,
            customColor = state.widgetCustomColor,
            showLogo = state.widgetShowLogo,
            cornerRadiusDp = state.widgetCornerRadius
        )

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = {
                haptics.success()
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val myProvider = ComponentName(context, XvoxAppWidgetProvider::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appWidgetManager.isRequestPinAppWidgetSupported) {
                    val callbackIntent = Intent(context, XvoxAppWidgetProvider::class.java)
                    val callbackPendingIntent = PendingIntent.getBroadcast(
                        context,
                        0,
                        callbackIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    appWidgetManager.requestPinAppWidget(myProvider, null, callbackPendingIntent)
                    Toast.makeText(context, "Adding XVOX widget to home screen...", Toast.LENGTH_SHORT).show()
                } else {
                    XvoxAppWidgetProvider.notifyWidgetUpdate(context)
                    Toast.makeText(context, "Add XVOX widget from launcher widget picker", Toast.LENGTH_LONG).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primaryAccent,
                contentColor = colors.background
            )
        ) {
            Icon(painter = painterResource(R.drawable.ic_xvox_plus), contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(text = "Add Widget to Home Screen", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Widget Transparency: ${(state.widgetTransparency * 100).roundToInt()}%",
            color = colors.primaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(4.dp))
        XvoxThinLineSlider(
            value = state.widgetTransparency,
            onValueChange = {
                haptics.sliderTick()
                viewModel.setWidgetTransparency(it)
            },
            valueRange = 0.0f..1.0f,
            defaultValue = 0.25f,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Widget Theme Style",
            color = colors.primaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Dynamic", "AMOLED", "Dark", "Light", "Glass").forEach { t ->
                val isSelected = state.widgetTheme == t
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) colors.primaryAccent else colors.cardElevated)
                        .border(1.dp, if (isSelected) colors.primaryAccent else colors.cardBorder, RoundedCornerShape(10.dp))
                        .clickable {
                            haptics.tap()
                            viewModel.setWidgetTheme(t)
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = t,
                        color = if (isSelected) colors.background else colors.primaryText,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Corner Radius: ${state.widgetCornerRadius}dp",
            color = colors.primaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(4.dp))
        XvoxThinLineSlider(
            value = state.widgetCornerRadius.toFloat(),
            onValueChange = {
                haptics.sliderTick()
                viewModel.setWidgetCornerRadius(it.toInt())
            },
            valueRange = 12f..36f,
            defaultValue = 24f,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        SettingsToggle(
            title = "Show X Logo (Cinzel Font)",
            subtitle = "Display brand watermark on widget",
            checked = state.widgetShowLogo
        ) {
            haptics.toggle()
            viewModel.setWidgetShowLogo(it)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                haptics.success()
                XvoxAppWidgetProvider.notifyWidgetUpdate(context)
                Toast.makeText(context, "Home Widgets Updated!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth().height(40.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.cardElevated,
                contentColor = colors.primaryText
            )
        ) {
            Text(text = "Refresh Active Widgets", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun WidgetLivePreviewCard(
    transparency: Float,
    theme: String,
    customColor: String,
    showLogo: Boolean,
    cornerRadiusDp: Int
) {
    val colors = XvoxTheme.colors

    val previewBgColor = when (theme) {
        "AMOLED" -> Color(0xFF000000)
        "Dark" -> Color(0xFF141414)
        "Light" -> Color(0xFFFAFAFA)
        "Glass" -> Color(0xFF1C1C22)
        "Custom" -> runCatching { Color(android.graphics.Color.parseColor(customColor)) }.getOrDefault(Color(0xFF171717))
        else -> colors.cardElevated
    }

    val alphaVal = (1.0f - transparency).coerceIn(0f, 1f)
    val effectiveBg = previewBgColor.copy(alpha = alphaVal)
    val isLight = theme == "Light" && transparency < 0.6f
    val textColor = if (isLight) Color(0xFF111111) else Color(0xFFFFFFFF)
    val subTextColor = if (isLight) Color(0xFF555555) else Color(0xFFA0A0A0)

    val artGradient = Brush.linearGradient(
        listOf(
            colors.primaryAccent.copy(alpha = 0.85f),
            colors.primaryAccent.copy(alpha = 0.45f)
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadiusDp.dp))
            .background(effectiveBg)
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(cornerRadiusDp.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(artGradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_xvox_music_note),
                    contentDescription = null,
                    tint = colors.background,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (showLogo) {
                    Text(
                        text = "X",
                        fontFamily = XvoxLogoFont,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
                Text(
                    text = "Live Track Preview",
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = "XVOX Sound Engine",
                    color = subTextColor,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_xvox_heart),
                    contentDescription = null,
                    tint = Color(0xFFFF453A),
                    modifier = Modifier.size(18.dp)
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_xvox_skip_previous),
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(textColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_xvox_pause),
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Icon(
                    painter = painterResource(id = R.drawable.ic_xvox_skip_next),
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
