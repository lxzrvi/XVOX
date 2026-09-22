package com.xvox.music.features.settings

import android.app.Activity
import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.chrome.parseHexColor
import com.xvox.music.core.ui.components.XvoxCustomColorDialog
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.insets.LocalXvoxBottomInset
import com.xvox.music.core.ui.insets.LocalXvoxTopInset
import com.xvox.music.core.ui.miniplayer.XvoxMiniPlayerPlacement
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.core.ui.overlay.XvoxOverlayController
import com.xvox.music.features.home.HomeViewModel
import com.xvox.music.features.settings.components.*
import com.xvox.music.features.settings.sections.*

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    settingsViewModel: SettingsViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel(),
    initialCategory: String? = null
) {
    val state by settingsViewModel.state.collectAsState()
    val homeState by homeViewModel.state.collectAsState()
    val colors = XvoxTheme.colors
    val context = LocalContext.current
    val overlays = LocalXvoxOverlayController.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val topInset = LocalXvoxTopInset.current
    val bottomInset = LocalXvoxBottomInset.current
    val bottomPadding = bottomInset + XvoxMiniPlayerPlacement.settingsBottomPadding

    val scrollState = rememberLazyListState()
    var showCustomColorDialog by remember { mutableStateOf(false) }

    if (showCustomColorDialog) {
        val initialCustomColor = parseHexColor(state.accentColor) ?: colors.primaryAccent
        XvoxCustomColorDialog(
            initialColor = initialCustomColor,
            onColorSelected = { selected ->
                showCustomColorDialog = false
                val hex = String.format("#%06X", (0xFFFFFF and selected.hashCode()))
                settingsViewModel.setAccentColor(hex)
            },
            onDismiss = { showCustomColorDialog = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(top = topInset + 4.dp)
    ) {
        if (isLandscape) {
            // Landscape Mode: Balanced 2-Column Grid
            LazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = bottomPadding),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "settings_title_land") {
                    Text(
                        text = "Settings",
                        color = colors.primaryAccent,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // Row 1: Appearance & Library
                item(key = "row_1") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            AppearanceSectionCard(state, settingsViewModel, onOpenColorWheel = { showCustomColorDialog = true }, modifier = Modifier.fillMaxHeight())
                        }
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            LibrarySectionCard(state, settingsViewModel, homeViewModel, overlays, modifier = Modifier.fillMaxHeight())
                        }
                    }
                }

                // Row 2: Widget Customization & Support Developer (Side by Side)
                item(key = "row_2") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            WidgetSectionCard(state, settingsViewModel, modifier = Modifier.fillMaxHeight())
                        }
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            SupportDeveloperCard(modifier = Modifier.fillMaxHeight())
                        }
                    }
                }

                // Row 3: System & Backup
                item(key = "row_3") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            SystemSectionCard(state, settingsViewModel, modifier = Modifier.fillMaxHeight())
                        }
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            BackupSectionCard(homeViewModel, modifier = Modifier.fillMaxHeight())
                        }
                    }
                }

                // Full Width About Box
                item(key = "row_about") {
                    AboutSectionCard()
                }
            }
        } else {
            // Portrait Mode Single Column
            LazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 10.dp, end = 10.dp, bottom = bottomPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "settings_title_portrait") {
                    Text(
                        text = "Settings",
                        color = colors.primaryAccent,
                        fontSize = 22.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 2.dp, bottom = 4.dp)
                    )
                }

                item(key = "section_appearance") {
                    AppearanceSectionCard(state, settingsViewModel, onOpenColorWheel = { showCustomColorDialog = true })
                }

                item(key = "section_library") {
                    LibrarySectionCard(state, settingsViewModel, homeViewModel, overlays)
                }

                item(key = "section_widget") {
                    WidgetSectionCard(state, settingsViewModel)
                }

                item(key = "section_support_dev") {
                    SupportDeveloperCard()
                }

                item(key = "section_backup") {
                    BackupSectionCard(homeViewModel)
                }

                item(key = "section_system") {
                    SystemSectionCard(state, settingsViewModel)
                }

                item(key = "section_about") {
                    AboutSectionCard()
                }
            }
        }
    }
}

@Composable
private fun AppearanceSectionCard(
    state: SettingsState,
    viewModel: SettingsViewModel,
    onOpenColorWheel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors

    SettingsCardFrame(title = "Appearance", modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Theme Mode", color = colors.secondaryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            val themeOptions = listOf("Dark" to "Dark", "Light" to "Light", "System" to "System")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                themeOptions.forEach { (key, label) ->
                    val isSelected = state.theme.equals(key, ignoreCase = true)
                    val bgColor = if (isSelected) colors.primaryAccent else colors.cardElevated
                    val textColor = if (isSelected) colors.background else colors.primaryText

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(19.dp))
                            .background(bgColor)
                            .border(0.8.dp, if (isSelected) Color.Transparent else colors.cardBorder.copy(alpha = 0.55f), RoundedCornerShape(19.dp))
                            .xvoxPressScale { viewModel.setTheme(key) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = textColor,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            Text("Accent Color", color = colors.secondaryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            val accentOptions = listOf(
                "White" to "Default",
                "Red" to "Red",
                "Blue" to "Blue",
                "custom" to "Custom"
            )
            val isCustomActive = state.accentColor.startsWith("#")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                accentOptions.forEach { (key, label) ->
                    val isSelected = if (key == "custom") isCustomActive else (!isCustomActive && state.accentColor.equals(key, ignoreCase = true))
                    val multiGradient = Brush.horizontalGradient(
                        listOf(
                            Color(0xFFFF3B30),
                            Color(0xFFFF9500),
                            Color(0xFF34C759),
                            Color(0xFF007AFF),
                            Color(0xFFAF52DE)
                        )
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(19.dp))
                            .background(if (isSelected && key != "custom") colors.primaryAccent else colors.cardElevated)
                            .border(
                                width = if (isSelected && key == "custom") 2.dp else 0.8.dp,
                                color = if (isSelected && key == "custom") colors.primaryAccent else if (isSelected) Color.Transparent else colors.cardBorder.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(19.dp)
                            )
                            .xvoxPressScale {
                                if (key == "custom") onOpenColorWheel()
                                else viewModel.setAccentColor(key)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        when (key) {
                            "Red" -> Text(
                                text = label,
                                color = if (isSelected) colors.background else Color(0xFFFF453A),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            "Blue" -> Text(
                                text = label,
                                color = if (isSelected) colors.background else Color(0xFF0A84FF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            "custom" -> Text(
                                text = if (isCustomActive) "Active" else label,
                                color = if (isCustomActive) colors.primaryAccent else colors.primaryText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            else -> Text(
                                text = label,
                                color = if (isSelected) colors.background else colors.primaryText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            Text("Text Scale", color = colors.secondaryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            val scales = listOf(0.85f to "Small", 1.0f to "Normal", 1.15f to "Large")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                scales.forEach { (scale, label) ->
                    val isSelected = kotlin.math.abs(state.fontSizeScale - scale) < 0.05f
                    val bgColor = if (isSelected) colors.primaryAccent else colors.cardElevated
                    val textColor = if (isSelected) colors.background else colors.primaryText

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(19.dp))
                            .background(bgColor)
                            .border(0.8.dp, if (isSelected) Color.Transparent else colors.cardBorder.copy(alpha = 0.55f), RoundedCornerShape(19.dp))
                            .xvoxPressScale { viewModel.setFontSizeScale(scale) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = textColor,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibrarySectionCard(
    state: SettingsState,
    settingsViewModel: SettingsViewModel,
    homeViewModel: HomeViewModel,
    overlays: XvoxOverlayController,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors

    SettingsCardFrame(title = "Audio & Engine", modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PlaybackSettingsSection(state, settingsViewModel, showPreview = false)

            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.card)
                    .border(0.8.dp, colors.cardBorder.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                    .clickable {
                        overlays.showBox("Equalizer & Sound") {
                            EqualizerSettingsSection(state, settingsViewModel)
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Equalizer & Reverb", color = colors.primaryText, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                    Text("5-Band EQ, room reverb, noise reduction", color = colors.secondaryText, fontSize = 11.sp)
                }
                Icon(painterResource(R.drawable.ic_xvox_caret_right), null, tint = colors.secondaryText, modifier = Modifier.size(16.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.card)
                    .border(0.8.dp, colors.cardBorder.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                    .clickable {
                        overlays.showBox("3D Spatial Audio") {
                            ThreeDSoundSettingsSection(state, settingsViewModel)
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("3D Spatial Sound", color = colors.primaryText, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                    Text("Spatial stage, orbit motion & HRTF binaural", color = colors.secondaryText, fontSize = 11.sp)
                }
                Icon(painterResource(R.drawable.ic_xvox_caret_right), null, tint = colors.secondaryText, modifier = Modifier.size(16.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.card)
                    .border(0.8.dp, colors.cardBorder.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                    .clickable {
                        overlays.showBox("Lyrics Customization") {
                            LyricsSettingsSection(state, settingsViewModel)
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Lyrics Customization", color = colors.primaryText, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                    Text("Line sizing, font weight, fading & alignment", color = colors.secondaryText, fontSize = 11.sp)
                }
                Icon(painterResource(R.drawable.ic_xvox_caret_right), null, tint = colors.secondaryText, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun WidgetSectionCard(state: SettingsState, viewModel: SettingsViewModel, modifier: Modifier = Modifier) {
    SettingsCardFrame(title = "Widget Customization", modifier = modifier) {
        WidgetSettingsSection(state = state, viewModel = viewModel)
    }
}

@Composable
private fun BackupSectionCard(homeViewModel: HomeViewModel, modifier: Modifier = Modifier) {
    SettingsCardFrame(title = "Backup & Restore", modifier = modifier) {
        BackupSettingsSection(viewModel = homeViewModel)
    }
}

@Composable
private fun SystemSectionCard(state: SettingsState, viewModel: SettingsViewModel, modifier: Modifier = Modifier) {
    SettingsCardFrame(title = "System & Background", modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            BatteryOptimizationSection()
            NotifySettingsSection(state, viewModel)
        }
    }
}

@Composable
private fun SupportDeveloperCard(modifier: Modifier = Modifier) {
    val colors = XvoxTheme.colors
    val context = LocalContext.current
    val haptics = com.xvox.music.core.ui.haptics.LocalXvoxHaptics.current
    val overlays = com.xvox.music.core.ui.overlay.LocalXvoxOverlayController.current

    val upiId = "thaparavi382-1@oksbi"
    val upiName = "lxzrvi"

    fun initiatePayment(amount: Int) {
        haptics.success()
        val uri = android.net.Uri.parse("upi://pay?pa=$upiId&pn=$upiName&am=$amount&cu=INR&tn=Support%20XVOX%20Developer")
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
        runCatching {
            context.startActivity(android.content.Intent.createChooser(intent, "Pay ₹$amount with UPI"))
        }.onFailure {
            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("UPI ID", upiId))
            overlays.showP("UPI ID copied: $upiId")
        }
    }

    SettingsCardFrame(title = "Support the Developer", modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.primaryAccent.copy(alpha = 0.12f))
                    .border(0.8.dp, colors.primaryAccent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = "Developer lxzrvi crafted XVOX using Kotlin & Jetpack Compose. If you love the experience and want to encourage further development and new upcoming apps, you can show your support! It is totally your own choice and deeply appreciated.",
                    color = colors.primaryText,
                    fontSize = 12.sp,
                    lineHeight = 16.5.sp
                )
            }

            Text(
                text = "UPI: $upiId",
                color = colors.primaryAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val tiers = listOf(
                    Triple(50, "₹50", "Coffee ☕"),
                    Triple(100, "₹100", "Burger 🍔"),
                    Triple(200, "₹200", "Full Pack 🍱"),
                    Triple(500, "₹500", "Treat 🚀")
                )

                tiers.forEach { (amt, price, label) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.cardElevated)
                            .border(0.8.dp, colors.cardBorder.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                            .xvoxPressScale { initiatePayment(amt) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(price, color = colors.primaryAccent, fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.height(2.dp))
                            Text(label, color = colors.primaryText, fontSize = 9.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutSectionCard(modifier: Modifier = Modifier) {
    SettingsCardFrame(title = "About XVOX", modifier = modifier) {
        AboutSettingsSection()
    }
}

@Composable
private fun SettingsCardFrame(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = XvoxTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.cardElevated.copy(alpha = 0.82f))
            .padding(14.dp)
    ) {
        Text(
            text = title,
            color = colors.primaryAccent,
            fontSize = 17.5.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        content()
    }
}
