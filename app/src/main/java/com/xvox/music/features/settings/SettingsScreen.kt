package com.xvox.music.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.navigation.LocalXvoxBottomInset
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.features.home.HomeViewModel
import com.xvox.music.features.settings.components.*
import com.xvox.music.features.settings.sections.*

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    topResetKey: Long = 0L,
    homeViewModel: HomeViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val state by settingsViewModel.state.collectAsState()
    val colors = XvoxTheme.colors
    val overlays = LocalXvoxOverlayController.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val bottomInset = LocalXvoxBottomInset.current
    val bottomPadding = bottomInset + if (isLandscape) 16.dp else 40.dp

    val scrollState = rememberLazyStaggeredGridState()

    LaunchedEffect(topResetKey) {
        runCatching { scrollState.scrollToItem(0) }
    }

    fun openCustomColorPicker() {
        overlays.showBox("Custom Accent Color") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ColorPickerRow(
                    label = "Pick Accent Color",
                    hex = if (state.accentColor.startsWith("#")) state.accentColor else "#FFFFFF",
                    onColorChange = { hex -> settingsViewModel.setAccentColor(hex) },
                    subtitle = "Applies across all buttons and highlights"
                )
            }
        }
    }

    var showingWidgetStudio by rememberSaveable { mutableStateOf(false) }
    if (showingWidgetStudio) {
        WidgetStudioScreen(
            state = state,
            viewModel = settingsViewModel,
            onBack = { showingWidgetStudio = false }
        )
        return
    }

    // A staggered grid keeps every settings card at its natural height. Unlike paired Rows with
    // fillMaxHeight, a short card never inherits an empty slab from the taller card beside it.
    val density = LocalDensity.current
    val statusTop = with(density) { WindowInsets.statusBars.getTop(this).toDp() }
    val columns = if (isLandscape) 2 else 1

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(columns),
        state = scrollState,
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
        contentPadding = PaddingValues(
            start = if (isLandscape) 16.dp else 10.dp,
            top = statusTop + 4.dp,
            end = if (isLandscape) 16.dp else 10.dp,
            bottom = bottomPadding
        ),
        verticalItemSpacing = if (isLandscape) 10.dp else 12.dp,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(
            key = "settings_title",
            span = StaggeredGridItemSpan.FullLine
        ) {
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
            AppearanceSectionCard(state, settingsViewModel, onOpenColorWheel = ::openCustomColorPicker)
        }
        item(key = "section_widget") {
            WidgetSectionCard(onOpenStudio = { showingWidgetStudio = true })
        }
        item(key = "section_support_dev") { SupportDeveloperCard() }
        item(key = "section_backup") { BackupSectionCard(homeViewModel) }
        item(key = "section_system") { SystemSectionCard(state, settingsViewModel) }
        item(key = "section_about") { AboutSectionCard() }
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
            val themeOptions = listOf(
                "Dark" to "Dark",
                "Light" to "Light",
                "AMOLED" to "AMOLED",
                "System" to "System"
            )
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
private fun WidgetSectionCard(
    onOpenStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    SettingsCardFrame(title = "Widgets", modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.card)
                .border(0.8.dp, colors.cardBorder.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
                .xvoxPressScale(onClick = onOpenStudio)
                .padding(horizontal = 14.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Widget Studio", color = colors.primaryText, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Edit the widget already placed on your home screen",
                    color = colors.secondaryText,
                    fontSize = 11.sp
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_xvox_caret_right),
                contentDescription = "Open Widget Studio",
                tint = colors.secondaryText,
                modifier = Modifier.size(17.dp)
            )
        }
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
