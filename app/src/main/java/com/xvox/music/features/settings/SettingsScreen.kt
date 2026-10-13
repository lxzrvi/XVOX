package com.xvox.music.features.settings

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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
import com.xvox.music.core.ui.overlay.xvoxBoxScroll
import com.xvox.music.features.home.HomeViewModel
import com.xvox.music.features.home.XvoxSongActions
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
    val context = LocalContext.current
    var pendingDeviceDeletion by remember { mutableStateOf<com.xvox.music.core.model.Song?>(null) }
    val deviceDeleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val song = pendingDeviceDeletion
        pendingDeviceDeletion = null
        if (song != null && result.resultCode == Activity.RESULT_OK) {
            // Android's scoped-storage confirmation succeeded; now discard only the soft-delete
            // metadata and rescan, never restore an unavailable media entry into the library.
            homeViewModel.discardHiddenSong(song.id)
            overlays.showP("Deleted from device")
        } else if (song != null) {
            overlays.showP("Device deletion cancelled")
        }
    }

    fun permanentlyDeleteFromDevice(song: com.xvox.music.core.model.Song) {
        val pendingIntent = XvoxSongActions.deletePendingIntent(context, song)
        if (pendingIntent != null) {
            pendingDeviceDeletion = song
            deviceDeleteLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
        } else if (XvoxSongActions.deleteLegacy(context, song)) {
            homeViewModel.discardHiddenSong(song.id)
            overlays.showP("Deleted from device")
        } else {
            overlays.showP("Could not delete this file")
        }
    }

    fun confirmPermanentDeviceDelete(song: com.xvox.music.core.model.Song) {
        overlays.showBox("Delete from device") {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Permanently delete \"${song.title}\" from this device? This cannot be undone.",
                    color = colors.primaryText,
                    fontSize = 14.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SettingsDialogAction("Cancel", Modifier.weight(1f)) { overlays.hideBox() }
                    SettingsDialogAction("Delete", Modifier.weight(1f), destructive = true) {
                        overlays.hideBox()
                        permanentlyDeleteFromDevice(song)
                    }
                }
            }
        }
    }

    fun openDeletedItems() {
        overlays.showBox("Recycle Bin") {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Songs deleted from XVOX stay here until you restore them or permanently remove the file from this device.",
                    color = colors.secondaryText,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                HiddenSongsSettingsSection(
                    viewModel = homeViewModel,
                    onDeleteFromDevice = ::confirmPermanentDeviceDelete,
                    onDiscardMissing = homeViewModel::discardHiddenSong
                )
            }
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val bottomInset = LocalXvoxBottomInset.current
    // The shell Header slides away before Settings settles. Settings therefore owns only a
    // status-bar-safe starting line and never reserves/recreates a duplicate Header.
    val bottomPadding = bottomInset + if (isLandscape) 16.dp else 30.dp
    val scrollState = rememberLazyGridState()

    LaunchedEffect(topResetKey) {
        runCatching { scrollState.scrollToItem(0) }
    }

    fun openCustomColorPicker() {
        overlays.showBox("Custom Accent Color") {
            val accentScroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(accentScroll)
                    .xvoxBoxScroll(accentScroll)
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

    fun openFontPicker() {
        overlays.showBox("Fonts") {
            XvoxFontPickerContent(
                selectedKey = state.fontFamily,
                onSelect = { key ->
                    settingsViewModel.setFontFamily(key)
                    overlays.hideBox()
                }
            )
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
        LazyVerticalGrid(
            // Landscape returns to two equal columns. A regular grid (rather than a staggered
            // masonry layout) keeps each row aligned, with equal outer and inter-section gaps.
            columns = GridCells.Fixed(if (isLandscape) 2 else 1),
            state = scrollState,
            modifier = Modifier
                .then(if (isLandscape) Modifier else Modifier.widthIn(max = 440.dp))
                .fillMaxWidth()
                .fillMaxHeight(),
            contentPadding = PaddingValues(
                start = if (isLandscape) 10.dp else 6.dp,
                top = statusTop + 6.dp,
                end = if (isLandscape) 10.dp else 6.dp,
                bottom = bottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(if (isLandscape) 10.dp else 12.dp),
            horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 10.dp else 12.dp)
        ) {
            item(
                key = "settings_title",
                span = { GridItemSpan(maxLineSpan) }
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

            if (isLandscape) {
                // Preserve the portrait sequence left-to-right, then top-to-bottom: Appearance →
                // Recycle Bin → Widgets → Backup & Restore → Support → About. Equal-height pairs
                // retain the landscape grid's balanced physical rows.
                item(key = "settings_pair_appearance_recycle", span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AppearanceSectionCard(
                            state, settingsViewModel, ::openCustomColorPicker, ::openFontPicker,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        DeletedItemsSectionCard(
                            onOpen = ::openDeletedItems,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
                item(key = "settings_pair_widgets_backup", span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        WidgetSectionCard(
                            onOpenStudio = { showingWidgetStudio = true },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        BackupSectionCard(homeViewModel, Modifier.weight(1f).fillMaxHeight())
                    }
                }
                // Support's tier buttons and the comprehensive About guide both need their
                // natural width/height. Keeping each full-width avoids stretching Support into
                // About's longer guide lane while retaining the same reading order.
                item(key = "settings_support", span = { GridItemSpan(maxLineSpan) }) {
                    SupportDeveloperCard(Modifier.fillMaxWidth())
                }
                item(key = "settings_about", span = { GridItemSpan(maxLineSpan) }) {
                    AboutSectionCard(Modifier.fillMaxWidth())
                }
                // Source-mode experiments intentionally stay out of the everyday Settings page.
            } else {
                item(key = "section_appearance") {
                    AppearanceSectionCard(state, settingsViewModel, ::openCustomColorPicker, ::openFontPicker)
                }
                // The requested everyday portrait order starts immediately after Appearance.
                item(key = "section_deleted_items") { DeletedItemsSectionCard(onOpen = ::openDeletedItems) }
                item(key = "section_widget") {
                    WidgetSectionCard(onOpenStudio = { showingWidgetStudio = true })
                }
                item(key = "section_backup") { BackupSectionCard(homeViewModel) }
                item(key = "section_support_dev") { SupportDeveloperCard() }
                item(key = "section_about") { AboutSectionCard() }
            }
        }
    }
}

@Composable
private fun AppearanceSectionCard(
    state: SettingsState,
    viewModel: SettingsViewModel,
    onOpenColorWheel: () -> Unit,
    onOpenFontPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
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
    // M is the fresh-install default; each other label maps to its explicit persisted scale.
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

            SettingsField("Fonts") {
                XvoxFontPickerButton(
                    selectedKey = state.fontFamily,
                    onClick = onOpenFontPicker,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            SettingsField("Text size") {
                XvoxSegmentedPill(
                    options = scaleOptions.map { it.first to it.second },
                    selectedKey = selectedScale,
                    onSelect = { key ->
                        scaleOptions.firstOrNull { it.first == key }?.let { viewModel.setFontSizeScale(it.third) }
                    },
                    compact = true
                )
            }

            SettingsField("Orientation") {
                XvoxSegmentedPill(
                    options = listOf(
                        "auto" to "Auto",
                        "portrait" to "Portrait",
                        "landscape" to "Landscape"
                    ),
                    selectedKey = state.appOrientation.lowercase(),
                    onSelect = viewModel::setAppOrientation,
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
    val configuration = LocalConfiguration.current
    val responsiveCompact = compact || configuration.screenHeightDp <= 500
    val pillHeight = when {
        configuration.screenHeightDp <= 430 -> 34.dp
        responsiveCompact -> 37.dp
        else -> 42.dp
    }
    val shape = RoundedCornerShape(pillHeight / 2f)
    val selectedTextColor = xvoxOnAccent(colors.primaryAccent)
    val selectedIndex = options.indexOfFirst { (key, _) -> key == selectedKey }
    val hasSelection = selectedIndex >= 0

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(pillHeight)
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
                        fontSize = if (configuration.screenHeightDp <= 430) 10.5.sp else if (responsiveCompact) 11.5.sp else 12.sp,
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
    val isLandscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    SettingsCardFrame(title = "Widgets", modifier = modifier) {
        // Landscape cards share a bottom with Appearance. Fill that shared lane deliberately
        // rather than leaving a single centred row floating in an oversized empty card.
        Column(
            modifier = if (isLandscape) Modifier.fillMaxHeight() else Modifier.fillMaxWidth(),
            verticalArrangement = if (isLandscape) Arrangement.SpaceBetween else Arrangement.Top
        ) {
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
            if (isLandscape) {
                Text(
                    text = "Open Studio to place and style your live widget.",
                    color = colors.mutedText,
                    fontSize = 10.5.sp,
                    lineHeight = 14.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
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
                tiers.forEachIndexed { treatment, (amount, price, label) ->
                    SupportTierButton(
                        amount = amount,
                        price = price,
                        label = label,
                        treatment = treatment,
                        modifier = Modifier.weight(1f),
                        onClick = { initiatePayment(amount) }
                    )
                }
            }
        }
    }
}

/**
 * Accent-derived support tier with restrained, tier-specific motion. These are intentionally not
 * fixed-red donation buttons: a custom accent and either theme keep their own visual language.
 */
@Composable
private fun SupportTierButton(
    amount: Int,
    price: String,
    label: String,
    treatment: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(14.dp)
    val transition = rememberInfiniteTransition(label = "supportTierMotion$amount")
    val duration = when (treatment) {
        0 -> 2800 // Coffee: slow diagonal drift.
        1 -> 3600 // Burger: wide horizontal drift.
        2 -> 3100 // Full Pack: vertical float.
        else -> 4400 // Treat: long diagonal orbit.
    }
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = duration, easing = LinearEasing),
            // Reverse loops have no hard jump at the end of a pass; every tier continuously
            // drifts as a soft patch rather than restarting a visible stripe.
            repeatMode = RepeatMode.Reverse
        ),
        label = "supportTierPhase$amount"
    )

    val accent = colors.primaryAccent
    val patchAlpha = if (colors.isLight) .28f else .42f
    val mistAlpha = if (colors.isLight) .13f else .20f
    val primaryCenter = when (treatment) {
        0 -> Offset(-40f + phase * 150f, 5f + phase * 58f)
        1 -> Offset(168f - phase * 190f, 48f)
        2 -> Offset(66f, -48f + phase * 154f)
        else -> Offset(156f - phase * 196f, -42f + phase * 136f)
    }
    val secondaryCenter = when (treatment) {
        0 -> Offset(138f - phase * 104f, 94f - phase * 42f)
        1 -> Offset(-24f + phase * 154f, 92f)
        2 -> Offset(144f, 124f - phase * 136f)
        else -> Offset(-28f + phase * 172f, 112f - phase * 94f)
    }
    val primaryRadius = when (treatment) {
        0 -> 142f
        1 -> 178f
        2 -> 154f
        else -> 196f
    }
    // Two low-alpha radial fields over the Settings surface create diffuse moving colour patches
    // instead of hard linear bands. Both colours derive from the active accent and current theme.
    val primaryPatch = Brush.radialGradient(
        colors = listOf(
            accent.copy(alpha = patchAlpha),
            lerp(accent, colors.cardElevated, .56f).copy(alpha = mistAlpha),
            Color.Transparent
        ),
        center = primaryCenter,
        radius = primaryRadius
    )
    val secondaryPatch = Brush.radialGradient(
        colors = listOf(
            accent.copy(alpha = patchAlpha * .48f),
            Color.Transparent
        ),
        center = secondaryCenter,
        radius = primaryRadius * 1.12f
    )

    Column(
        modifier = modifier
            .clip(shape)
            .background(colors.cardElevated, shape)
            .background(secondaryPatch, shape)
            .background(primaryPatch, shape)
            // Donation tiers intentionally have no outline; their soft moving accent fields are
            // the visual separator instead of a hard Settings-card border.
            .xvoxPressScale(onClick = onClick)
            .padding(vertical = 9.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(price, color = colors.primaryText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(label, color = colors.secondaryText.copy(alpha = .88f), fontSize = 9.5.sp, maxLines = 1)
    }
}

/** About-adjacent entry point for songs soft-deleted from the visible XVOX library. */
@Composable
private fun DeletedItemsSectionCard(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(14.dp)
    SettingsCardFrame(title = "Recycle Bin", modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Restore songs removed from XVOX, or permanently delete their files from this device.",
                color = colors.secondaryText,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(colors.cardElevated)
                    .border(.8.dp, colors.cardBorder.copy(alpha = .72f), shape)
                    .xvoxPressScale(onClick = onOpen)
                    .padding(horizontal = 13.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Open recycle bin", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("Open", color = colors.primaryAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SettingsDialogAction(
    label: String,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(shape)
            .background(if (destructive) Color(0xFFE85B5B) else colors.cardElevated)
            .border(.8.dp, if (destructive) Color.Transparent else colors.cardBorder, shape)
            .xvoxPressScale(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (destructive) Color.White else colors.primaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AboutSectionCard(modifier: Modifier = Modifier) {
    val colors = XvoxTheme.colors
    val context = LocalContext.current
    val haptics = LocalXvoxHaptics.current
    val isLandscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val shareShape = CircleShape
    SettingsCardFrame(
        title = "About XVOX",
        modifier = modifier,
        titleAction = {
            // Share belongs beside the actual About title, keeping the card's primary action
            // visible without adding another wide control above its feature guide.
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(shareShape)
                    .background(colors.cardElevated)
                    .border(.8.dp, colors.cardBorder.copy(alpha = .78f), shareShape)
                    .xvoxPressScale {
                        haptics.tap()
                        shareXvoxApp(context)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_xvox_share),
                    contentDescription = "Share XVOX",
                    tint = colors.primaryAccent,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    ) {
        AboutSettingsSection(expanded = isLandscape)
    }
}

@Composable
private fun SettingsCardFrame(
    title: String,
    modifier: Modifier = Modifier,
    titleAction: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(22.dp)
    val isLandscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.card)
            .border(1.dp, colors.cardBorder.copy(alpha = if (colors.isLight) 0.8f else 0.72f), shape)
            .padding(horizontal = 14.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = colors.primaryAccent,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            titleAction?.invoke()
        }
        if (isLandscape) {
            // Paired cards intentionally share a physical bottom. Center each card's own content
            // in the remaining lane instead of leaving an arbitrary blank tail below short cards.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true),
                contentAlignment = Alignment.Center
            ) {
                content()
            }
        } else {
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

private fun xvoxOnAccent(accent: Color): Color {
    val luminance = 0.2126f * accent.red + 0.7152f * accent.green + 0.0722f * accent.blue
    return if (luminance > 0.62f) Color(0xFF111111) else Color.White
}
