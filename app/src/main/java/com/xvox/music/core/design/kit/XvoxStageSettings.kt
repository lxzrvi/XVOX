package com.xvox.music.core.design.kit

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxRed
import com.xvox.music.core.ui.chrome.XvoxBackgroundMode
import com.xvox.music.core.ui.chrome.XvoxChromeStyle
import com.xvox.music.core.ui.chrome.backgroundMode
import com.xvox.music.data.preferences.UserPreferencesRepository
import io.github.neoruaa.glasmio.core.LocalGlassColors
import io.github.neoruaa.glasmio.glass.render.backdrops.LayerBackdrop
import io.github.neoruaa.glasmio.preference.GlasmioPreferenceGroupTitle
import io.github.neoruaa.glasmio.preference.GlasmioSwitchPreference
import io.github.neoruaa.glasmio.scaffold.GlasmioPinnedListPage
import io.github.neoruaa.glasmio.ui.GlasmioColorPicker
import io.github.neoruaa.glasmio.ui.GlasmioGroupedList
import io.github.neoruaa.glasmio.ui.GlasmioListRow
import io.github.neoruaa.glasmio.ui.GlasmioSegmentedControl
import io.github.neoruaa.glasmio.ui.GlasmioSlider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Settings page of the rebuild stage.
 *
 * Every day-to-day option is a live control on this one page - segmented controls, swatches and
 * sliders sit inside their rows - so nothing needs a tap-through. Changes persist immediately and
 * the app-wide backdrop reacts to them through the preferences flows read by the app root.
 */
@Composable
fun XvoxStageSettingsPage(
    prefs: UserPreferencesRepository,
    backdrop: LayerBackdrop,
    onBack: () -> Unit,
    /** Owned by the stage so the navigation bar can follow this page's scroll. */
    listState: LazyListState = rememberLazyListState()
) {
    val scope = rememberCoroutineScope()
    val theme by prefs.theme.collectAsState(initial = "System")
    val accent by prefs.accentColor.collectAsState(initial = "Red")
    val fontScale by prefs.fontSizeScale.collectAsState(initial = .90f)
    val transparency by prefs.cardTransparency.collectAsState(initial = 0f)
    val design by prefs.designLanguage.collectAsState(initial = "xvox")
    val hapticsEnabled by prefs.hapticFeedbackEnabled.collectAsState(initial = true)
    var accentPickerOpen by remember { mutableStateOf(false) }
    val colors = com.xvox.music.core.design.theme.XvoxTheme.colors
    val textSizes = remember { mapOf("XS" to .70f, "S" to .80f, "M" to .90f, "L" to 1.00f) }

    GlasmioPinnedListPage(
        title = "Settings",
        onNavigateBack = onBack,
        listState = listState,
        bottomPadding = 132.dp,
        backgroundColor = Color.Transparent,
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            GlasmioPreferenceGroupTitle("Appearance")
            GlasmioGroupedList {
                InlineSetting(title = "Theme", showTopSeparator = false) {
                    GlasmioSegmentedControl(
                        items = listOf("System" to "Auto", "Light" to "Light", "Dark" to "Dark"),
                        selected = theme.takeIf { it in setOf("System", "Light", "Dark") } ?: "System",
                        onSelected = { scope.launch { prefs.setTheme(it) } },
                        backdrop = backdrop,
                        height = 34.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                InlineSetting(title = "Accent colour", detail = accentLabel(accent)) {
                    AccentSwatches(
                        accent = accent,
                        onPreset = { scope.launch { prefs.setAccentColor(it) } },
                        onCustom = { accentPickerOpen = true }
                    )
                }
                InlineSetting(title = "Text size", detail = scaleKey(fontScale)) {
                    GlasmioSegmentedControl(
                        items = listOf("XS", "S", "M", "L").map { it to it },
                        selected = scaleKey(fontScale),
                        onSelected = { key ->
                            textSizes[key]?.let { v -> scope.launch { prefs.setFontSizeScale(v) } }
                        },
                        backdrop = backdrop,
                        height = 34.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                var liveTransparency by remember(transparency) { mutableFloatStateOf(transparency) }
                InlineSetting(
                    title = "Card transparency",
                    detail = "${(liveTransparency / .6f * 100).toInt()}%"
                ) {
                    GlasmioSlider(
                        value = liveTransparency,
                        onValueChange = { liveTransparency = it },
                        onValueChangeFinished = { scope.launch { prefs.setCardTransparency(liveTransparency) } },
                        valueRange = 0f..0.6f,
                        backdrop = backdrop,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                InlineSetting(title = "UI style") {
                    GlasmioSegmentedControl(
                        items = listOf("xvox" to "XVOX", "material" to "M3", "ios" to "iOS"),
                        selected = design,
                        onSelected = { scope.launch { prefs.setDesignLanguage(it) } },
                        backdrop = backdrop,
                        height = 34.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        item {
            GlasmioPreferenceGroupTitle("Background")
            GlasmioGroupedList {
                BackgroundSettings(prefs = prefs, backdrop = backdrop)
            }
        }
        item {
            GlasmioPreferenceGroupTitle("Feedback")
            GlasmioGroupedList {
                GlasmioSwitchPreference(
                    title = { Text("Haptic feedback") },
                    description = "Tap and slider haptics across the app",
                    checked = hapticsEnabled,
                    onCheckedChange = { scope.launch { prefs.setHapticFeedbackEnabled(it) } },
                    showTopSeparator = false
                )
            }
        }
        item {
            GlasmioPreferenceGroupTitle("About")
            GlasmioGroupedList {
                GlasmioListRow(
                    title = "Version",
                    detail = "1.02.11",
                    showTopSeparator = false
                )
            }
        }
    }

    GlasmioColorPicker(
        visible = accentPickerOpen,
        selectedColor = colors.primaryAccent,
        onColorSelected = { color ->
            val hex = "#%06X".format(0xFFFFFF and color.toArgb())
            scope.launch { prefs.setAccentColor(hex) }
            accentPickerOpen = false
        },
        onDismiss = { accentPickerOpen = false },
        title = "Accent colour"
    )
}

/**
 * App background controls. The chosen mode is written to [XvoxChromeStyle.bgStyle] together with
 * the photo, because the app root only paints a photo while the mode is Custom: choosing a photo
 * without switching the mode (or removing one without leaving Custom) used to change nothing.
 */
@Composable
private fun BackgroundSettings(
    prefs: UserPreferencesRepository,
    backdrop: LayerBackdrop
) {
    val scope = rememberCoroutineScope()
    val glass = LocalGlassColors.current
    val image by prefs.themeBackgroundImage.collectAsState(initial = "")
    val chrome by prefs.chromeStyle.collectAsState(initial = XvoxChromeStyle())
    val brightness by prefs.backgroundBrightness.collectAsState(initial = .8f)
    val base by prefs.themeBackground.collectAsState(initial = "Default")
    val mode = chrome.backgroundMode(image.isNotBlank())

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                prefs.setThemeBackgroundImage(uri.toString())
                prefs.updateChrome { it.copy(bgStyle = BgStyleCustom) }
            }
        }
    }
    fun pickPhoto() = picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    InlineSetting(title = "Style", showTopSeparator = false) {
        GlasmioSegmentedControl(
            items = listOf("gradient" to "Gradient", "photo" to "Photo", "plain" to "Plain"),
            selected = when (mode) {
                XvoxBackgroundMode.Gradient -> "gradient"
                XvoxBackgroundMode.Custom -> "photo"
                XvoxBackgroundMode.Off -> "plain"
            },
            onSelected = { choice ->
                when (choice) {
                    "gradient" -> scope.launch { prefs.updateChrome { it.copy(bgStyle = BgStyleGradient) } }
                    "plain" -> scope.launch { prefs.updateChrome { it.copy(bgStyle = BgStyleOff) } }
                    // No photo yet: ask for one; the picker result switches the mode itself.
                    else -> if (image.isBlank()) pickPhoto()
                    else scope.launch { prefs.updateChrome { it.copy(bgStyle = BgStyleCustom) } }
                }
            },
            backdrop = backdrop,
            height = 34.dp,
            modifier = Modifier.fillMaxWidth()
        )
    }
    InlineSetting(title = "Base colour", detail = backgroundLabel(base)) {
        GlasmioSegmentedControl(
            items = listOf("Default" to "Plain", "Midnight" to "Midnight", "Warm" to "Warm"),
            selected = if (base == "Midnight" || base == "Warm") base else "Default",
            onSelected = { scope.launch { prefs.setThemeBackground(it) } },
            backdrop = backdrop,
            height = 34.dp,
            modifier = Modifier.fillMaxWidth()
        )
    }
    if (mode == XvoxBackgroundMode.Custom) {
        GlasmioListRow(
            title = "Change photo",
            onClick = { pickPhoto() }
        )
        GlasmioListRow(
            title = "Remove photo",
            onClick = {
                scope.launch {
                    prefs.setThemeBackgroundImage(null)
                    prefs.updateChrome { it.copy(bgStyle = BgStyleGradient) }
                }
            }
        )
        var liveBlur by remember(chrome.bgImageBlur) { mutableFloatStateOf(chrome.bgImageBlur) }
        InlineSetting(title = "Blur", detail = "${(liveBlur * 100).toInt()}%") {
            GlasmioSlider(
                value = liveBlur,
                onValueChange = { liveBlur = it },
                onValueChangeFinished = {
                    scope.launch { prefs.updateChrome { it.copy(bgImageBlur = liveBlur, bgBlur = liveBlur) } }
                },
                backdrop = backdrop,
                modifier = Modifier.fillMaxWidth()
            )
        }
        var liveBrightness by remember(brightness) { mutableFloatStateOf(brightness) }
        InlineSetting(title = "Brightness", detail = "${(liveBrightness * 100).toInt()}%") {
            GlasmioSlider(
                value = liveBrightness,
                onValueChange = { liveBrightness = it },
                onValueChangeFinished = { scope.launch { prefs.setBackgroundBrightness(liveBrightness) } },
                backdrop = backdrop,
                modifier = Modifier.fillMaxWidth()
            )
        }
    } else {
        Text(
            text = if (mode == XvoxBackgroundMode.Gradient) {
                "Gradient follows your accent colour."
            } else {
                "A plain fill in the base colour."
            },
            color = glass.secondaryContent,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
        )
    }
}

/** Stored values of [XvoxChromeStyle.bgStyle] (see [XvoxBackgroundMode]). */
private const val BgStyleOff = 1f
private const val BgStyleCustom = 2f
private const val BgStyleGradient = 3f

/** Reads the newest stored style first, so two quick edits never overwrite each other. */
private suspend fun UserPreferencesRepository.updateChrome(block: (XvoxChromeStyle) -> XvoxChromeStyle) {
    setChromeStyle(block(chromeStyle.first()))
}

/**
 * One settings row whose control lives inside it: title (and optional value) on the first line,
 * the control underneath. Rows inside a grouped list draw their own hairline separator.
 */
@Composable
private fun InlineSetting(
    title: String,
    detail: String? = null,
    showTopSeparator: Boolean = true,
    content: @Composable () -> Unit
) {
    val glass = LocalGlassColors.current
    Column(Modifier.fillMaxWidth()) {
        if (showTopSeparator) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp)
                    .height(.5.dp)
                    .background(glass.separator)
            )
        }
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    color = glass.content,
                    fontSize = 17.sp,
                    modifier = Modifier.weight(1f)
                )
                if (detail != null) {
                    Text(text = detail, color = glass.secondaryContent, fontSize = 15.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

private val AccentPresets = listOf(
    "Red" to XvoxRed,
    "Blue" to Color(0xFF0A84FF),
    "White" to Color.White
)

/** Preset accents plus a rainbow swatch that opens the colour picker. */
@Composable
private fun AccentSwatches(
    accent: String,
    onPreset: (String) -> Unit,
    onCustom: () -> Unit
) {
    val glass = LocalGlassColors.current
    Row(
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AccentPresets.forEach { (key, color) ->
            Swatch(
                selected = accent.equals(key, ignoreCase = true),
                ring = glass.content,
                onClick = { onPreset(key) }
            ) {
                Box(Modifier.fillMaxSize().background(color))
            }
        }
        Swatch(selected = accent.startsWith("#"), ring = glass.content, onClick = onCustom) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.sweepGradient(
                        listOf(
                            Color(0xFFFF3B30), Color(0xFFFFCC00), Color(0xFF34C759),
                            Color(0xFF0A84FF), Color(0xFFAF52DE), Color(0xFFFF3B30)
                        )
                    )
                )
            )
        }
    }
}

@Composable
private fun Swatch(
    selected: Boolean,
    ring: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .then(if (selected) Modifier.border(2.dp, ring, CircleShape) else Modifier)
            .padding(4.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) { content() }
}

private fun accentLabel(value: String): String = when {
    value.startsWith("#") -> "Custom"
    else -> value
}

private fun backgroundLabel(value: String): String = when {
    value.startsWith("#") -> "Custom"
    value == "Default" -> "Plain"
    else -> value
}

private fun scaleKey(scale: Float): String = when {
    scale <= .74f -> "XS"
    scale <= .84f -> "S"
    scale <= .94f -> "M"
    else -> "L"
}
