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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.State
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.kit.icons.XvoxSfIcons
import com.xvox.music.core.design.theme.XvoxRed
import com.xvox.music.core.ui.icons.painterResource
import com.xvox.music.core.ui.chrome.XvoxBackgroundMode
import com.xvox.music.core.ui.chrome.XvoxChromeStyle
import com.xvox.music.core.ui.chrome.backgroundMode
import com.xvox.music.data.preferences.UserPreferencesRepository
import io.github.neoruaa.glasmio.core.LocalGlassColors
import io.github.neoruaa.glasmio.glass.render.Backdrop
import io.github.neoruaa.glasmio.glass.render.backdrops.layerBackdrop
import io.github.neoruaa.glasmio.glass.render.backdrops.rememberCombinedBackdrop
import io.github.neoruaa.glasmio.glass.render.backdrops.rememberLayerBackdrop
import androidx.compose.foundation.shape.RoundedCornerShape
import com.xvox.music.core.design.theme.XvoxTheme
import io.github.neoruaa.glasmio.preference.GlasmioPreferenceGroupTitle
import io.github.neoruaa.glasmio.preference.GlasmioSwitchPreference
import io.github.neoruaa.glasmio.scaffold.GlasmioPinnedListPage
import io.github.neoruaa.glasmio.ui.GlasmioColorPicker
import io.github.neoruaa.glasmio.ui.GlasmioGroupedList
import io.github.neoruaa.glasmio.ui.GlasmioListRow
import io.github.neoruaa.glasmio.ui.GlasmioMenu
import io.github.neoruaa.glasmio.ui.GlasmioMenuItem
import io.github.neoruaa.glasmio.ui.GlasmioSegmentedControl
import io.github.neoruaa.glasmio.ui.GlasmioSlider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Everything the Settings page shows, read once by the stage. Settings only ever receives resolved
 * values, so opening the page never starts from a default and animates to the stored value: the
 * sliders and segmented thumbs are already at their final position on the first frame.
 */
class XvoxStageSettingsValues(
    val theme: State<String>,
    val accent: State<String>,
    val fontScale: State<Float>,
    val transparency: State<Float>,
    val design: State<String>,
    val haptics: State<Boolean>,
    val image: State<String>,
    val chrome: State<XvoxChromeStyle>,
    val brightness: State<Float>,
    val base: State<String>
)

@Composable
fun rememberXvoxStageSettings(prefs: UserPreferencesRepository): XvoxStageSettingsValues =
    XvoxStageSettingsValues(
        theme = prefs.theme.collectAsState(initial = "System"),
        accent = prefs.accentColor.collectAsState(initial = "Red"),
        fontScale = prefs.fontSizeScale.collectAsState(initial = .90f),
        transparency = prefs.cardTransparency.collectAsState(initial = 0f),
        design = prefs.designLanguage.collectAsState(initial = "xvox"),
        haptics = prefs.hapticFeedbackEnabled.collectAsState(initial = true),
        image = prefs.themeBackgroundImage.collectAsState(initial = ""),
        chrome = prefs.chromeStyle.collectAsState(initial = XvoxChromeStyle()),
        brightness = prefs.backgroundBrightness.collectAsState(initial = .8f),
        base = prefs.themeBackground.collectAsState(initial = "Default")
    )

/**
 * Settings page of the rebuild stage.
 *
 * Each option uses the Glasmio control that suits it: an icon segmented control for the theme,
 * swatches for colours, pop-up menu rows for pick-one lists, sliders only for real continuous
 * values, and a switch for the on/off option. Changes persist immediately and the app-wide
 * backdrop reacts through the preference flows read by the app root.
 */
@Composable
fun XvoxStageSettingsPage(
    prefs: UserPreferencesRepository,
    values: XvoxStageSettingsValues,
    backdrop: Backdrop,
    onBack: () -> Unit,
    /** Owned by the stage so the navigation bar can follow this page's scroll. */
    listState: LazyListState = rememberLazyListState()
) {
    val scope = rememberCoroutineScope()
    val theme by values.theme
    val accent by values.accent
    val fontScale by values.fontScale
    val transparency by values.transparency
    val design by values.design
    val hapticsEnabled by values.haptics
    var accentPickerOpen by remember { mutableStateOf(false) }
    val colors = com.xvox.music.core.design.theme.XvoxTheme.colors
    val glass = LocalGlassColors.current
    val textSizes = remember { listOf("XS" to .70f, "S" to .80f, "M" to .90f, "L" to 1.00f) }

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
            CardGroup(backdrop) { backdrop ->
                InlineSetting(title = "Theme", detail = themeLabel(theme), showTopSeparator = false) {
                    GlasmioSegmentedControl(
                        items = listOf("System" to "", "Light" to "", "Dark" to ""),
                        selected = theme.takeIf { it in setOf("System", "Light", "Dark") } ?: "System",
                        onSelected = { scope.launch { prefs.setTheme(it) } },
                        backdrop = backdrop,
                        height = 36.dp,
                        modifier = Modifier.fillMaxWidth(),
                        itemContent = { value, _ -> ThemeGlyph(value, glass.content) }
                    )
                }
                InlineSetting(title = "Accent colour", detail = accentLabel(accent)) {
                    AccentSwatches(
                        accent = accent,
                        onPreset = { scope.launch { prefs.setAccentColor(it) } },
                        onCustom = { accentPickerOpen = true }
                    )
                }
                PickerRow(
                    title = "Text size",
                    options = textSizes.map { it.second to sizeLabel(it.first) },
                    selected = textSizes.first { it.first == scaleKey(fontScale) }.second,
                    onSelected = { v -> scope.launch { prefs.setFontSizeScale(v) } },
                    backdrop = backdrop
                )
                PickerRow(
                    title = "UI style",
                    options = listOf("xvox" to "XVOX", "material" to "Material 3", "ios" to "iOS"),
                    selected = design,
                    onSelected = { scope.launch { prefs.setDesignLanguage(it) } },
                    backdrop = backdrop
                )
            }
        }
        item {
            GlasmioPreferenceGroupTitle("Background")
            CardGroup(backdrop) { backdrop ->
                BackgroundSettings(prefs = prefs, values = values)
            }
        }
        item {
            GlasmioPreferenceGroupTitle("Feedback")
            CardGroup(backdrop) { backdrop ->
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
            CardGroup(backdrop) { backdrop ->
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

/** Sun / moon / half-disc glyph inside the theme segmented control. */
@Composable
private fun BoxScope.ThemeGlyph(value: String, tint: Color) {
    Icon(
        imageVector = when (value) {
            "Light" -> XvoxSfIcons.SunFill
            "Dark" -> XvoxSfIcons.MoonFill
            else -> XvoxSfIcons.CircleHalfFill
        },
        contentDescription = value,
        tint = tint,
        modifier = Modifier.size(20.dp).align(Alignment.Center)
    )
}

/**
 * A pick-one row: the current choice sits on the right and tapping opens Glasmio's liquid menu
 * anchored on the row, so choosing never needs a tap-through to another screen.
 */
@Composable
private fun <T> PickerRow(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit,
    backdrop: Backdrop,
    showTopSeparator: Boolean = true
) {
    var open by remember { mutableStateOf(false) }
    val glass = LocalGlassColors.current
    GlasmioMenu(
        expanded = open,
        onExpandedChange = { open = it },
        itemCount = options.size,
        menuWidth = 220.dp,
        backdrop = backdrop,
        modifier = Modifier.fillMaxWidth(),
        anchor = { toggle ->
            GlasmioListRow(
                title = title,
                detail = options.firstOrNull { it.first == selected }?.second.orEmpty(),
                onClick = toggle,
                showDisclosure = false,
                showTopSeparator = showTopSeparator,
                trailing = {
                    Icon(
                        painter = painterResource(R.drawable.ic_xvox_chevron_down),
                        contentDescription = null,
                        tint = glass.secondaryContent,
                        modifier = Modifier.size(14.dp)
                    )
                }
            )
        }
    ) { menuBackdrop, close ->
        options.forEach { (key, label) ->
            GlasmioMenuItem(
                title = label,
                onClick = {
                    onSelected(key)
                    close()
                },
                backdrop = menuBackdrop,
                trailing = if (key == selected) {
                    {
                        Icon(
                            painter = painterResource(R.drawable.ic_xvox_check),
                            contentDescription = null,
                            tint = glass.content,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else null
            )
        }
    }
}

/**
 * App background: one solid colour, nothing else (no photo, no gradient, no accent tint). The
 * first swatch follows the theme; the others are fixed solids, and the rainbow swatch opens the
 * colour wheel. Text colours adapt to whatever is picked.
 */
@Composable
private fun BackgroundSettings(
    prefs: UserPreferencesRepository,
    values: XvoxStageSettingsValues
) {
    val scope = rememberCoroutineScope()
    val glass = LocalGlassColors.current
    val base by values.base
    var plainPickerOpen by remember { mutableStateOf(false) }

    InlineSetting(title = "Background colour", detail = plainLabel(base), showTopSeparator = false) {
        PlainSwatches(
            current = base,
            onPick = { scope.launch { prefs.setThemeBackground(it) } },
            onCustom = { plainPickerOpen = true }
        )
    }
    Text(
        text = "A single solid colour. Text switches to light or dark ink to stay readable.",
        color = glass.secondaryContent,
        fontSize = 12.sp,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
    )

    GlasmioColorPicker(
        visible = plainPickerOpen,
        selectedColor = parsePlain(base) ?: Color.Black,
        onColorSelected = { color ->
            scope.launch { prefs.setThemeBackground("#%06X".format(0xFFFFFF and color.toArgb())) }
            plainPickerOpen = false
        },
        onDismiss = { plainPickerOpen = false },
        title = "Background colour"
    )
}

/** Wraps one grouped list with a card-colour layer so its controls sample the card they sit on. */
@Composable
private fun CardGroup(backdrop: Backdrop, content: @Composable (Backdrop) -> Unit) {
    val layer = rememberLayerBackdrop()
    val cardBackdrop = rememberCombinedBackdrop(backdrop, layer)
    val card = XvoxTheme.colors.card
    Box {
        // A sibling of the list, never its parent: the controls sample the card colour layer
        // together with the wallpaper, and never the layer they are drawn in.
        Box(
            Modifier
                .matchParentSize()
                .padding(1.dp)
                .clip(RoundedCornerShape(26.dp))
                .layerBackdrop(layer)
                .background(card)
        )
        GlasmioGroupedList { content(cardBackdrop) }
    }
}

private val PlainPresets = listOf(
    "Default" to Color(0xFF8E8E93),
    "#000000" to Color(0xFF000000),
    "#1C1C1E" to Color(0xFF1C1C1E),
    "#0B1220" to Color(0xFF0B1220),
    "#16130E" to Color(0xFF16130E),
    "#FFFFFF" to Color(0xFFFFFFFF),
    "#F6F0E6" to Color(0xFFF6F0E6)
)

private fun parsePlain(value: String): Color? =
    if (value.startsWith("#")) runCatching { Color(android.graphics.Color.parseColor(value)) }.getOrNull() else null

private fun plainLabel(value: String): String = when {
    value.startsWith("#") -> PlainPresets.firstOrNull { it.first.equals(value, true) }?.first ?: "Custom"
    else -> "Theme default"
}

/** Solid colour swatches for Plain mode, plus a rainbow swatch that opens the colour wheel. */
@Composable
private fun PlainSwatches(
    current: String,
    onPick: (String) -> Unit,
    onCustom: () -> Unit
) {
    val glass = LocalGlassColors.current
    val isPreset = PlainPresets.any { it.first.equals(current, true) }
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlainPresets.forEach { (hex, color) ->
            Swatch(
                selected = current.equals(hex, true) || (hex == "Default" && !current.startsWith("#")),
                ring = glass.content,
                onClick = { onPick(hex) },
                size = 34.dp
            ) {
                Box(Modifier.fillMaxSize().background(color).border(.5.dp, glass.separator, CircleShape))
            }
        }
        Swatch(selected = current.startsWith("#") && !isPreset, ring = glass.content, onClick = onCustom, size = 34.dp) {
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
    size: Dp = 40.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
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

private fun themeLabel(value: String): String = if (value == "System") "Auto" else value

private fun sizeLabel(key: String): String = when (key) {
    "XS" -> "Extra small"
    "S" -> "Small"
    "M" -> "Medium"
    else -> "Large"
}

private fun scaleKey(scale: Float): String = when {
    scale <= .74f -> "XS"
    scale <= .84f -> "S"
    scale <= .94f -> "M"
    else -> "L"
}
