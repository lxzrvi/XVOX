package com.xvox.music.features.settings

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.core.ui.navigation.LocalXvoxBottomInset
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.features.home.HomeViewModel
import com.xvox.music.features.settings.components.*
import com.xvox.music.features.settings.sections.*
import kotlin.math.abs

/**
 * The compact, single-column Settings landing page. It intentionally keeps just the everyday
 * controls here; detailed playback, lyrics, audio, and layout controls remain in their dedicated
 * pages so this screen stays as focused as the reference design.
 */
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
    val bottomPadding = bottomInset + if (isLandscape) 16.dp else 30.dp
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
                    hex = if (state.accentColor.startsWith("#")) state.accentColor else "#F01E2C",
                    onColorChange = settingsViewModel::setAccentColor,
                    subtitle = "Applies across all buttons and highlights",
                    showPreview = false,
                    initiallyExpanded = true
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

    val density = LocalDensity.current
    val statusTop = with(density) { WindowInsets.statusBars.getTop(this).toDp() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(1),
            state = scrollState,
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .fillMaxHeight(),
            contentPadding = PaddingValues(
                start = 6.dp,
                top = statusTop + 6.dp,
                end = 6.dp,
                bottom = bottomPadding
            ),
            verticalItemSpacing = 12.dp,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(
                key = "settings_title",
                span = StaggeredGridItemSpan.FullLine
            ) {
                Text(
                    text = "Settings",
                    color = colors.primaryAccent,
                    fontSize = 24.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            item(key = "section_appearance") {
                AppearanceSectionCard(state, settingsViewModel, ::openCustomColorPicker)
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
}

@Composable
private fun AppearanceSectionCard(
    state: SettingsState,
    viewModel: SettingsViewModel,
    onOpenColorWheel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    val chrome = state.chromeStyle
    val themeOptions = listOf("System" to "System", "Light" to "Light", "Dark" to "Dark")
    val selectedTheme = themeOptions.firstOrNull { it.first.equals(state.theme, ignoreCase = true) }?.first
        ?: if (state.theme.equals("AMOLED", ignoreCase = true)) "Dark" else null

    val accentOptions = listOf("Red" to "Red", "Blue" to "Blue", "custom" to "Custom")
    val selectedAccent = when {
        state.accentColor.startsWith("#") -> "custom"
        state.accentColor.equals("Red", ignoreCase = true) -> "Red"
        state.accentColor.equals("Blue", ignoreCase = true) -> "Blue"
        // Legacy White/default values are shown through the Custom slot instead of leaving
        // the segmented control visually unselected.
        else -> "custom"
    }

    // L retains the prior Medium physical scale; each lower label steps down one size.
    val scaleOptions = listOf(
        Triple("xs", "XS", 0.70f),
        Triple("s", "S", 0.80f),
        Triple("m", "M", 0.90f),
        Triple("l", "L", 1.00f)
    )
    val selectedScale = scaleOptions.minByOrNull { abs(state.fontSizeScale - it.third) }?.first

    SettingsCardFrame(title = "Appearance", modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SettingsField("Theme Mode") {
                XvoxSegmentedPill(
                    options = themeOptions,
                    selectedKey = selectedTheme,
                    onSelect = viewModel::setTheme
                )
            }

            SettingsField("Accent Color") {
                XvoxSegmentedPill(
                    options = accentOptions,
                    selectedKey = selectedAccent,
                    onSelect = { key ->
                        if (key == "custom") onOpenColorWheel() else viewModel.setAccentColor(key)
                    }
                )
            }

            SettingsField("Header") {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    SettingsToggle(
                        title = "Header dimness",
                        subtitle = "Dim the selected Header image",
                        checked = chrome.headerDimEnabled,
                        onChange = { enabled -> viewModel.setChromeStyle { it.copy(headerDimEnabled = enabled) } }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Dimness", color = colors.secondaryText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${(chrome.headerDimAmount.coerceIn(0f, 1f) * 100).toInt()}%",
                            color = colors.primaryAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    XvoxContinuousSlider(
                        value = chrome.headerDimAmount,
                        onValueChange = { amount -> viewModel.setChromeStyle { it.copy(headerDimAmount = amount) } },
                        defaultValue = .50f,
                        enabled = chrome.headerDimEnabled,
                        contentDescription = "Header dimness"
                    )
                }
            }

            SettingsField("Text Scale") {
                XvoxSegmentedPill(
                    options = scaleOptions.map { it.first to it.second },
                    selectedKey = selectedScale,
                    onSelect = { key ->
                        scaleOptions.firstOrNull { it.first == key }?.let { viewModel.setFontSizeScale(it.third) }
                    },
                    compact = true
                )
            }
        }
    }
}

@Composable
private fun SettingsField(
    label: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            color = XvoxTheme.colors.mutedText,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp)
        )
        content()
    }
}

/** Rounded, gliding-pill style option control used by the reference Settings layout. */
@Composable
private fun XvoxSegmentedPill(
    options: List<Pair<String, String>>,
    selectedKey: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(50)
    val selectedTextColor = xvoxOnAccent(colors.primaryAccent)
    val selectedIndex = options.indexOfFirst { (key, _) -> key == selectedKey }
    val hasSelection = selectedIndex >= 0

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(if (compact) 40.dp else 42.dp)
            .clip(shape)
            .background(colors.cardElevated)
            .border(0.8.dp, colors.cardBorder.copy(alpha = 0.7f), shape)
    ) {
        val horizontalInset = 3.dp
        val verticalInset = 3.dp
        val usableWidth = (maxWidth - (horizontalInset * 2)).coerceAtLeast(0.dp)
        val segmentWidth = if (options.isEmpty()) 0.dp else usableWidth / options.size.toFloat()
        val targetOffset = horizontalInset + (segmentWidth * selectedIndex.coerceAtLeast(0).toFloat())
        val animatedOffset by animateDpAsState(
            targetValue = targetOffset,
            animationSpec = tween(300, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)),
            label = "settingsSegmentedPillSlide"
        )

        // One persistent selection surface moves between slots; changing an option never redraws
        // the highlight in place, so every Settings pill visibly glides to its next choice.
        if (hasSelection) {
            Box(
                modifier = Modifier
                    .offset(x = animatedOffset, y = verticalInset)
                    .width(segmentWidth)
                    .height((maxHeight - (verticalInset * 2)).coerceAtLeast(0.dp))
                    .clip(CircleShape)
                    .background(colors.primaryAccent)
            )
        }

        Row(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = horizontalInset, vertical = verticalInset),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { (key, label) ->
                val isSelected = key == selectedKey
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .xvoxPressScale { onSelect(key) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) selectedTextColor else colors.mutedText,
                        fontSize = if (compact) 11.5.sp else 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1
                    )
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
                .xvoxPressScale(onClick = onOpenStudio)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Widget Studio", color = colors.primaryText, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Edit the widget already placed on your home screen",
                    color = colors.mutedText,
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_xvox_caret_right),
                contentDescription = "Open Widget Studio",
                tint = colors.mutedText,
                modifier = Modifier.size(16.dp)
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
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            BatteryOptimizationSection()
            NotifySettingsSection(state, viewModel)
        }
    }
}

@Composable
private fun SupportDeveloperCard(modifier: Modifier = Modifier) {
    val colors = XvoxTheme.colors
    val context = LocalContext.current
    val haptics = LocalXvoxHaptics.current
    val overlays = LocalXvoxOverlayController.current
    val upiId = "thaparavi382-1@oksbi"
    val upiName = "lxzrvi"

    fun copyUpiId() {
        haptics.tap()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("UPI ID", upiId))
        overlays.showP("UPI ID copied")
    }

    fun initiatePayment(amount: Int) {
        haptics.success()
        val uri = Uri.parse("upi://pay?pa=$upiId&pn=$upiName&am=$amount&cu=INR&tn=Support%20XVOX%20Developer")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        runCatching {
            context.startActivity(Intent.createChooser(intent, "Pay ₹$amount with UPI"))
        }.onFailure {
            copyUpiId()
        }
    }

    SettingsCardFrame(title = "Support the Developer", modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.primaryAccent.copy(alpha = 0.18f))
                    .border(1.dp, colors.primaryAccent.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "Developer Ixzrvi crafted XVOX using Kotlin & Jetpack Compose. If you love the experience and want to encourage further development and new upcoming apps, you can show your support! It is totally your own choice and deeply appreciated.",
                    color = colors.primaryText,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }

            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colors.cardElevated)
                    .border(0.8.dp, colors.cardBorder.copy(alpha = 0.7f), CircleShape)
                    .xvoxPressScale { copyUpiId() }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_xvox_copy),
                    contentDescription = "Copy UPI ID",
                    tint = colors.primaryAccent,
                    modifier = Modifier.size(14.dp)
                )
                Text("UPI: $upiId", color = colors.primaryAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val tiers = listOf(
                    Triple(50, "₹50", "Coffee ☕"),
                    Triple(100, "₹100", "Burger 🍔"),
                    Triple(200, "₹200", "Full Pack 📦"),
                    Triple(500, "₹500", "Treat 🥢")
                )
                tiers.forEach { (amount, price, label) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(colors.cardElevated)
                            .border(0.8.dp, colors.cardBorder.copy(alpha = 0.7f), CircleShape)
                            .xvoxPressScale { initiatePayment(amount) }
                            .padding(vertical = 8.dp, horizontal = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(price, color = colors.primaryAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(label, color = colors.mutedText, fontSize = 9.5.sp, maxLines = 1)
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
    val shape = RoundedCornerShape(22.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.card)
            .border(1.dp, colors.cardBorder.copy(alpha = if (colors.isLight) 0.8f else 0.72f), shape)
            .padding(horizontal = 14.dp, vertical = 16.dp)
    ) {
        Text(
            text = title,
            color = colors.primaryAccent,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(14.dp))
        content()
    }
}

private fun xvoxOnAccent(accent: Color): Color {
    val luminance = 0.2126f * accent.red + 0.7152f * accent.green + 0.0722f * accent.blue
    return if (luminance > 0.62f) Color(0xFF111111) else Color.White
}
