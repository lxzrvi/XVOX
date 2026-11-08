// EXACT CODE REPLACE KARO (poori file) - koi extra kaam nahi. Path: app/src/main/java/com/xvox/music/features/settings/SettingsScreen.kt
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import com.xvox.music.core.ui.chrome.backgroundMode
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
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
import coil3.compose.AsyncImage
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.blur.XvoxBlurLevel
import com.xvox.music.core.ui.blur.xvoxLiveBlur
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
import kotlin.math.roundToInt

/** Shared compact height for every full-width choice surface in Appearance. */
private val XvoxAppearancePillHeight = 42.dp

/**
 * The compact, single-column Settings landing page. It intentionally keeps just the everyday
 * controls here; detailed playback, lyrics, audio, and layout controls remain in their dedicated
 * pages so this screen stays as focused as the reference design.
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    topResetKey: Long = 0L,
    /** Explicit re-selection of Settings returns any local subpage/sheet to this landing page. */
    baseResetKey: Long = 0L,
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
    val bottomPadding = bottomInset + if (isLandscape) 10.dp else 30.dp
    val scrollState = rememberLazyGridState()
    var showingWidgetStudio by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(topResetKey) {
        runCatching { scrollState.scrollToItem(0) }
    }
    LaunchedEffect(baseResetKey) {
        if (baseResetKey > 0L) {
            // Widget Studio is a Settings-owned full subpage. Any ordinary Settings sheet is also
            // local to this destination, so the active navbar tap closes it with the same return.
            showingWidgetStudio = false
            overlays.hideBox()
            runCatching { scrollState.scrollToItem(0) }
        }
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
            .fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyVerticalGrid(
            // Landscape uses two compact, independently-sized card lanes that remain entirely
            // scrollable; portrait keeps the familiar readable single column.
            columns = GridCells.Fixed(if (isLandscape) 2 else 1),
            state = scrollState,
            modifier = Modifier
                .then(if (isLandscape) Modifier else Modifier.widthIn(max = 440.dp))
                .fillMaxWidth()
                .fillMaxHeight(),
            contentPadding = PaddingValues(
                start = if (isLandscape) 8.dp else 6.dp,
                top = statusTop + if (isLandscape) 4.dp else 6.dp,
                end = if (isLandscape) 8.dp else 6.dp,
                bottom = bottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(if (isLandscape) 8.dp else 12.dp),
            horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 8.dp else 12.dp)
        ) {
            item(
                key = "settings_title",
                span = { GridItemSpan(maxLineSpan) }
            ) {
                Text(
                    text = "Settings",
                    color = colors.primaryAccent,
                    fontSize = if (isLandscape) 20.sp else 24.sp,
                    lineHeight = if (isLandscape) 24.sp else 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = if (isLandscape) 2.dp else 4.dp)
                )
            }

            if (isLandscape) {
                // The grid remains the scroll host, while this one full-width item balances the
                // cards inside two equal lanes and lets the shorter lane finish flush.
                item(
                    key = "settings_balanced_two_columns",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    BalancedTwoColumn(modifier = Modifier.fillMaxWidth(), gap = 10.dp) {
                        AppearanceSectionCard(
                            state = state,
                            viewModel = settingsViewModel,
                            onOpenColorWheel = ::openCustomColorPicker,
                            onOpenFontPicker = ::openFontPicker,
                            modifier = Modifier.fillMaxWidth()
                        )
                        BackgroundPlaybackSectionCard(
                            state = state,
                            viewModel = settingsViewModel,
                            modifier = Modifier.fillMaxWidth()
                        )
                        LibrarySectionCard(
                            state = state,
                            settingsViewModel = settingsViewModel,
                            homeViewModel = homeViewModel,
                            modifier = Modifier.fillMaxWidth()
                        )
                        DeletedItemsSectionCard(
                            onOpen = ::openDeletedItems,
                            modifier = Modifier.fillMaxWidth()
                        )
                        WidgetSectionCard(
                            onOpenStudio = { showingWidgetStudio = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                        BackupSectionCard(homeViewModel, Modifier.fillMaxWidth())
                        SupportDeveloperCard(Modifier.fillMaxWidth())
                        AboutSectionCard(Modifier.fillMaxWidth())
                    }
                }
            } else {
                item(key = "section_appearance") {
                    AppearanceSectionCard(state, settingsViewModel, ::openCustomColorPicker, ::openFontPicker)
                }
                item(key = "section_background_playback") {
                    BackgroundPlaybackSectionCard(state, settingsViewModel)
                }
                item(key = "section_library") {
                    LibrarySectionCard(
                        state = state,
                        settingsViewModel = settingsViewModel,
                        homeViewModel = homeViewModel
                    )
                }
                item(key = "section_deleted_items") { DeletedItemsSectionCard(onOpen = ::openDeletedItems) }
                item(key = "section_widget") {
                    WidgetSectionCard(onOpenStudio = { showingWidgetStudio = true })
                }
                item(key = "section_backup") { BackupSectionCard(homeViewModel) }
                item(key = "section_support_dev") { SupportDeveloperCard() }
                item(key = "section_about") { AboutSectionCard() }
            }
            // Source-mode experiments intentionally stay out of the everyday Settings page.
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
    // The repository copies this into app-private storage, so the selected app background remains
    // available after the document provider's transient permission disappears.
    val overlays = LocalXvoxOverlayController.current
    val latestState by rememberUpdatedState(state)
    // Draft values for the Custom background box. Nothing is applied until Okay.
    var draftUri by remember { mutableStateOf<String?>(null) }
    var draftBlur by remember { mutableStateOf(0f) }
    var draftDim by remember { mutableStateOf(.2f) }
    val backgroundPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { draftUri = it.toString() }
    }
    var gStyle by remember { mutableStateOf(0) }
    var gAngle by remember { mutableStateOf(35f) }
    var gC1 by remember { mutableStateOf("") }
    var gC2 by remember { mutableStateOf("") }
    fun hexOf(c: Color) = String.format("#%06X", c.toArgb() and 0xFFFFFF)
    fun openGradientBox() {
        val cs = latestState.chromeStyle
        val cols = cs.bgColors.split(",").map { it.trim() }.filter { it.isNotBlank() }
        gStyle = cs.bgGradStyle.toInt().coerceIn(0, 5)
        gAngle = cs.bgGradAngle
        gC1 = cols.getOrNull(0) ?: hexOf(colors.primaryAccent)
        gC2 = cols.getOrNull(1) ?: hexOf(Color(android.graphics.Color.HSVToColor(FloatArray(3).also { android.graphics.Color.colorToHSV(colors.primaryAccent.toArgb(), it); it[0] = (it[0] + 38f) % 360f })))
        overlays.showBox(
            title = "Gradient",
            bottomAction = {
                XvoxTransactionalFooterActions(
                    onCancel = { overlays.hideBox() },
                    onOkay = {
                        viewModel.setChromeStyle {
                            it.copy(bgStyle = 3f, bgGradStyle = gStyle.toFloat(), bgGradAngle = gAngle, bgColors = "$gC1,$gC2")
                        }
                        overlays.hideBox()
                    }
                )
            }
        ) {
            GradientBoxContent(
                style = gStyle, angle = gAngle, c1 = gC1, c2 = gC2,
                onStyle = { gStyle = it }, onAngle = { gAngle = it },
                onC1 = { gC1 = it }, onC2 = { gC2 = it }
            )
        }
    }
    fun openCustomBackground() {
        draftUri = latestState.backgroundImageUri
        draftBlur = latestState.chromeStyle.bgImageBlur.coerceIn(0f, 1f)
        draftDim = (1f - latestState.backgroundBrightness).coerceIn(0f, .80f)
        overlays.showBox(
            title = "Custom background",
            bottomAction = {
                XvoxTransactionalFooterActions(
                    onCancel = { overlays.hideBox() },
                    onOkay = {
                        val chosen = draftUri
                        if (chosen != latestState.backgroundImageUri) viewModel.setBackgroundImage(chosen)
                        viewModel.setBackgroundBrightness(1f - draftDim)
                        viewModel.setChromeStyle {
                            it.copy(bgImageBlur = draftBlur, bgStyle = if (chosen == null) 3f else 2f)
                        }
                        overlays.hideBox()
                    }
                )
            }
        ) {
            CustomBackgroundBoxContent(
                uri = draftUri,
                blur = draftBlur,
                dim = draftDim,
                onChoose = { backgroundPicker.launch("image/*") },
                onRemove = { draftUri = null },
                onBlur = { draftBlur = it },
                onDim = { draftDim = it }
            )
        }
    }
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

    val compact = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    SettingsCardFrame(title = "Appearance", modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(if (compact) 9.dp else 14.dp)) {
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

            SettingsField("Liquid glass") {
                val blur = state.chromeStyle.bgBlur.coerceIn(0f, 1f)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Off = default look (flat theme + default borders). On = live blur on elements.
                    XvoxSegmentedPill(
                        options = listOf("off" to "Off", "on" to "On"),
                        selectedKey = if (blur <= .001f) "off" else "on",
                        onSelect = { key -> viewModel.setChromeStyle { it.copy(bgBlur = if (key == "on") 1f else 0f) } }
                    )
                    if (com.xvox.music.core.ui.blur.XvoxGpuGuard.safeMode) {
                        val ctx = androidx.compose.ui.platform.LocalContext.current
                        Text(
                            text = "Safe mode: the last run closed unexpectedly, so blur effects are paused. Tap to try blur again.",
                            color = colors.primaryAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .xvoxPressScale { com.xvox.music.core.ui.blur.XvoxGpuGuard.clearSafeMode(ctx) }
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            SettingsField("App background") {
                val hasImage = state.backgroundImageUri != null
                val bgMode = state.chromeStyle.backgroundMode(hasImage)
                XvoxSegmentedPill(
                    options = listOf("off" to "Off", "gradient" to "Gradient", "custom" to "Custom"),
                    selectedKey = when (bgMode) {
                        com.xvox.music.core.ui.chrome.XvoxBackgroundMode.Off -> "off"
                        com.xvox.music.core.ui.chrome.XvoxBackgroundMode.Gradient -> "gradient"
                        com.xvox.music.core.ui.chrome.XvoxBackgroundMode.Custom -> "custom"
                    },
                    imageKey = "custom",
                    imageUri = if (bgMode == com.xvox.music.core.ui.chrome.XvoxBackgroundMode.Custom) state.backgroundImageUri else null,
                    onSelect = { key ->
                        when (key) {
                            "off" -> viewModel.setChromeStyle { it.copy(bgStyle = 1f) }
                            "gradient" -> {
                                viewModel.setChromeStyle { it.copy(bgStyle = 3f) }
                                openGradientBox()
                            }
                            else -> openCustomBackground()
                        }
                    }
                )
            }

            SettingsField("Fonts") {
                XvoxFontPickerButton(
                    selectedKey = state.fontFamily,
                    onClick = onOpenFontPicker,
                    modifier = Modifier.fillMaxWidth(),
                    pillHeight = XvoxAppearancePillHeight
                )
            }

            SettingsField("Text size") {
                XvoxSegmentedPill(
                    options = scaleOptions.map { it.first to it.second },
                    selectedKey = selectedScale,
                    onSelect = { key ->
                        scaleOptions.firstOrNull { it.first == key }?.let { viewModel.setFontSizeScale(it.third) }
                    }
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
                    onSelect = viewModel::setAppOrientation
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
    val compact = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    Column(verticalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 8.dp)) {
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
    /** Optional: this option shows a photo thumbnail as its face (App background > Custom). */
    imageKey: String? = null,
    imageUri: String? = null
) {
    val colors = XvoxTheme.colors
    val compact = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    // Landscape keeps the same pill language but trims its physical lane so two columns retain
    // comfortable scroll density rather than requiring oversized equal-height cards.
    val pillHeight = if (compact) 36.dp else XvoxAppearancePillHeight
    val shape = RoundedCornerShape(pillHeight / 2f)
    val selectedTextColor = xvoxOnAccent(colors.primaryAccent)
    val selectedIndex = options.indexOfFirst { (key, _) -> key == selectedKey }
    val hasSelection = selectedIndex >= 0

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(pillHeight)
            // Active selections stay solid below; only the surrounding option lane uses the
            // lighter glass level so the selected pill remains immediately recognizable.
            .xvoxLiveBlur(
                shape = shape,
                level = XvoxBlurLevel.Selection,
                baseColor = colors.cardElevated,
                borderAlpha = .70f
            )
    ) {
        val horizontalInset = if (compact) 2.dp else 3.dp
        val verticalInset = if (compact) 2.dp else 3.dp
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
                    val showsImage = key == imageKey && imageUri != null
                    if (showsImage) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                        Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = .38f)))
                        if (isSelected) {
                            Box(Modifier.matchParentSize().border(2.dp, colors.primaryAccent, CircleShape))
                        }
                    }
                    Text(
                        text = label,
                        color = if (showsImage) Color.White else if (isSelected) selectedTextColor else colors.mutedText,
                        fontSize = 12.sp,
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
        Column(modifier = Modifier.fillMaxWidth()) {
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
            SupportDeveloperHero()

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
 * The support card's hero stays within the active palette: a pair of slow accent blooms gives it
 * a living thank-you feel without introducing a separate red/orange donation brand.
 */
@Composable
private fun SupportDeveloperHero() {
    val colors = XvoxTheme.colors
    val transition = rememberInfiniteTransition(label = "supportDeveloperHero")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "supportDeveloperHeroPhase"
    )
    val shape = RoundedCornerShape(17.dp)
    val accent = colors.primaryAccent
    val bloomA = Brush.radialGradient(
        colors = listOf(accent.copy(alpha = if (colors.isLight) .30f else .44f), Color.Transparent),
        center = Offset(-8f + phase * 190f, 8f + phase * 52f),
        radius = 155f
    )
    val bloomB = Brush.radialGradient(
        colors = listOf(lerp(accent, Color.White, .30f).copy(alpha = if (colors.isLight) .15f else .24f), Color.Transparent),
        center = Offset(218f - phase * 170f, 112f - phase * 72f),
        radius = 135f
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.cardElevated, shape)
            .background(bloomB, shape)
            .background(bloomA, shape)
            .border(1.dp, accent.copy(alpha = if (colors.isLight) .22f else .30f), shape)
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                text = "XVOX, made with care",
                color = colors.primaryText,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Developer Ixzrvi crafted XVOX using Kotlin and Jetpack Compose. If the experience brings you joy, a small optional treat helps support future improvements and new apps.",
                color = colors.primaryText.copy(alpha = .90f),
                fontSize = 11.5.sp,
                lineHeight = 16.sp
            )
            Text(
                text = "Completely optional · deeply appreciated",
                color = colors.secondaryText,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
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

/** Everyday library controls, deliberately placed immediately below Background Playback. */
@Composable
private fun LibrarySectionCard(
    state: SettingsState,
    settingsViewModel: SettingsViewModel,
    homeViewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val overlays = LocalXvoxOverlayController.current

    fun openLibraryFilters() {
        overlays.showBox("Library filters") {
            LibraryFilterSettingsSection(
                state = state,
                viewModel = settingsViewModel,
                homeViewModel = homeViewModel
            )
        }
    }

    SettingsCardFrame(title = "Library", modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsLandingActionRow(
                title = "Hide Folder",
                subtitle = if (state.ignoredFolders.isEmpty()) {
                    "Choose folders to keep out of your library"
                } else {
                    "${state.ignoredFolders.size} ${if (state.ignoredFolders.size == 1) "folder" else "folders"} hidden"
                },
                onClick = ::openLibraryFilters
            )

            SettingsField("Minimum duration") {
                SettingsFlexChoiceRow(
                    options = listOf("0" to "Off", "15" to "15s", "30" to "30s", "60" to "1m", "custom" to "Custom"),
                    selected = state.ignoreBelowSec.takeIf { it in setOf(0, 15, 30, 60) }?.toString() ?: "custom",
                    onSelect = { value ->
                        if (value == "custom") openLibraryFilters()
                        else settingsViewModel.setIgnoreBelowSec(value.toInt())
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsLandingActionRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.cardElevated)
            .border(.8.dp, colors.cardBorder.copy(alpha = .72f), shape)
            .xvoxPressScale(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = colors.mutedText, fontSize = 11.sp, lineHeight = 14.sp)
        }
        Icon(
            painter = painterResource(R.drawable.ic_xvox_caret_right),
            contentDescription = "Open $title",
            tint = colors.mutedText,
            modifier = Modifier.size(16.dp)
        )
    }
}

/** Keeps the accepted background-playback permission behaviour on the main Settings landing page. */
@Composable
private fun BackgroundPlaybackSectionCard(
    state: SettingsState,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    SettingsCardFrame(title = "Background Playback", modifier = modifier) {
        NotifySettingsSection(state, viewModel)
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
    val isLandscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    SettingsCardFrame(title = "About XVOX", modifier = modifier) {
        // Sharing and feedback are expandable in the About content, keeping one clear action
        // cluster rather than a detached title icon and an unrelated footer.
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
            .xvoxLiveBlur(
                shape = shape,
                level = XvoxBlurLevel.Card,
                baseColor = colors.card,
                borderAlpha = if (colors.isLight) .80f else .72f,
                borderWidth = 1.dp
            )
            .padding(
                horizontal = if (isLandscape) 10.dp else 14.dp,
                vertical = if (isLandscape) 11.dp else 16.dp
            )
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
        Spacer(Modifier.height(if (isLandscape) 8.dp else 14.dp))
        // Cards now keep their natural compact height in the two-column landscape grid; this
        // avoids the forced blank lane created by the previous paired-card implementation.
        content()
    }
}

private fun xvoxOnAccent(accent: Color): Color {
    val luminance = 0.2126f * accent.red + 0.7152f * accent.green + 0.0722f * accent.blue
    return if (luminance > 0.62f) Color(0xFF111111) else Color.White
}


/** Body of the Custom background box: live preview, Choose/Remove, then Blur and Dimness. */
@Composable
private fun CustomBackgroundBoxContent(
    uri: String?,
    blur: Float,
    dim: Float,
    onChoose: () -> Unit,
    onRemove: () -> Unit,
    onBlur: (Float) -> Unit,
    onDim: (Float) -> Unit
) {
    val colors = XvoxTheme.colors
    val scroll = rememberScrollState()
    val shape = RoundedCornerShape(18.dp)
    val actionShape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scroll)
            .xvoxBoxScroll(scroll)
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(shape)
                .background(colors.background),
            contentAlignment = Alignment.Center
        ) {
            if (uri != null) {
                AsyncImage(
                    model = uri,
                    contentDescription = "Background preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer { scaleX = 1.1f; scaleY = 1.1f }
                        .blur((blur * 27f).dp, androidx.compose.ui.draw.BlurredEdgeTreatment.Unbounded)
                )
                Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = dim.coerceIn(0f, .95f))))
            } else {
                Text("No photo chosen", color = colors.secondaryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(actionShape)
                    .background(colors.primaryAccent)
                    .xvoxPressScale(onClick = onChoose),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (uri == null) "Choose" else "Replace",
                    color = xvoxOnAccent(colors.primaryAccent),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (uri != null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(actionShape)
                        .background(colors.cardElevated)
                        .xvoxPressScale(onClick = onRemove),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Remove", color = colors.primaryText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (uri != null) {
            Text(
                "Blur · ${(blur * 100f).roundToInt()}%",
                color = colors.secondaryText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp)
            )
            XvoxSlider(
                value = blur,
                onValueChange = onBlur,
                valueRange = 0f..1f,
                defaultValue = 0f,
                snapRadius = .02f,
                valueLabel = { "${(it * 100f).roundToInt()} percent background blur" },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Dimness · ${(dim * 100f).roundToInt()}%",
                color = colors.secondaryText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp)
            )
            XvoxSlider(
                value = dim,
                onValueChange = onDim,
                valueRange = 0f..0.80f,
                defaultValue = .20f,
                snapRadius = .015f,
                valueLabel = { "${(it * 100f).roundToInt()} percent background dimness" },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}


/** Body of the Gradient box: live preview, style chips, angle, two colours. */
@Composable
private fun GradientBoxContent(
    style: Int,
    angle: Float,
    c1: String,
    c2: String,
    onStyle: (Int) -> Unit,
    onAngle: (Float) -> Unit,
    onC1: (String) -> Unit,
    onC2: (String) -> Unit
) {
    val colors = XvoxTheme.colors
    val scroll = rememberScrollState()
    val names = listOf("Mesh", "Rays", "Patches", "Aurora", "Linear", "Spotlight")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scroll)
            .xvoxBoxScroll(scroll)
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(18.dp))) {
            com.xvox.music.core.ui.chrome.XvoxGeneratedBackground(
                accent = colors.primaryAccent,
                base = colors.background,
                isLight = colors.isLight,
                style = 0f,
                blurDp = 0.dp,
                modifier = Modifier.fillMaxSize(),
                custom = listOfNotNull(
                    com.xvox.music.core.ui.chrome.parseHexColor(c1),
                    com.xvox.music.core.ui.chrome.parseHexColor(c2)
                ),
                gradStyle = style,
                angle = angle
            )
        }
        names.chunked(3).forEachIndexed { row, group ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                group.forEachIndexed { i, name ->
                    val index = row * 3 + i
                    val active = index == style
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (active) colors.primaryAccent else colors.cardElevated)
                            .xvoxPressScale { onStyle(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            name,
                            color = if (active) xvoxOnAccent(colors.primaryAccent) else colors.primaryText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        Text(
            "Angle · ${angle.roundToInt()}°",
            color = colors.secondaryText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp)
        )
        XvoxSlider(
            value = angle,
            onValueChange = onAngle,
            valueRange = 0f..360f,
            defaultValue = 35f,
            snapRadius = 4f,
            valueLabel = { "${it.roundToInt()} degrees gradient angle" },
            modifier = Modifier.fillMaxWidth()
        )
        ColorPickerRow(label = "Colour 1", hex = c1, onColorChange = onC1)
        ColorPickerRow(label = "Colour 2", hex = c2, onColorChange = onC2)
    }
}
