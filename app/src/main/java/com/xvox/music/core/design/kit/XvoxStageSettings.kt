package com.xvox.music.core.design.kit

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.ui.chrome.XvoxChromeStyle
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
import kotlinx.coroutines.launch

private const val RouteTheme = "theme"
private const val RouteAccent = "accent"
private const val RouteBackground = "background"
private const val RouteText = "text"
private const val RouteTransparency = "transparency"
private const val RouteStyle = "style"

/**
 * Settings tree. The root page carries labelled rows; every label opens its own Glasmio page with
 * a back chevron, so each option keeps the native large-title + blur header behaviour.
 */
@Composable
fun XvoxStageSettingsPage(
    prefs: UserPreferencesRepository,
    backdrop: LayerBackdrop,
    onBack: () -> Unit
) {
    var route by remember { mutableStateOf<String?>(null) }

    Crossfade(targetState = route, label = "settingsRoute") { current ->
        when (current) {
            null -> SettingsRoot(
                prefs = prefs,
                backdrop = backdrop,
                onBack = onBack,
                onRoute = { route = it }
            )
            RouteTheme -> SettingsThemePage(prefs, backdrop) { route = null }
            RouteAccent -> SettingsAccentPage(prefs, backdrop) { route = null }
            RouteBackground -> SettingsBackgroundPage(prefs, backdrop) { route = null }
            RouteText -> SettingsTextSizePage(prefs, backdrop) { route = null }
            RouteTransparency -> SettingsTransparencyPage(prefs, backdrop) { route = null }
            RouteStyle -> SettingsStylePage(prefs, backdrop) { route = null }
        }
    }
}

@Composable
private fun SettingsRoot(
    prefs: UserPreferencesRepository,
    backdrop: LayerBackdrop,
    onBack: () -> Unit,
    onRoute: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val theme by prefs.theme.collectAsState(initial = "System")
    val accent by prefs.accentColor.collectAsState(initial = "Red")
    val background by prefs.themeBackground.collectAsState(initial = "Default")
    val fontScale by prefs.fontSizeScale.collectAsState(initial = .90f)
    val transparency by prefs.cardTransparency.collectAsState(initial = 0f)
    val design by prefs.designLanguage.collectAsState(initial = "xvox")
    val hapticsEnabled by prefs.hapticFeedbackEnabled.collectAsState(initial = true)

    GlasmioPinnedListPage(
        title = "Settings",
        onNavigateBack = onBack,
        bottomPadding = 132.dp,
        backgroundColor = Color.Transparent,
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            GlasmioPreferenceGroupTitle("Appearance")
            GlasmioGroupedList {
                GlasmioListRow(
                    title = "Theme",
                    detail = theme,
                    showTopSeparator = false,
                    onClick = { onRoute(RouteTheme) }
                )
                GlasmioListRow(
                    title = "Accent colour",
                    detail = accentLabel(accent),
                    onClick = { onRoute(RouteAccent) }
                )
                GlasmioListRow(
                    title = "Background",
                    detail = backgroundLabel(background),
                    onClick = { onRoute(RouteBackground) }
                )
                GlasmioListRow(
                    title = "Text size",
                    detail = scaleKey(fontScale),
                    onClick = { onRoute(RouteText) }
                )
                GlasmioListRow(
                    title = "Card transparency",
                    detail = "${(transparency * 100).toInt()}%",
                    onClick = { onRoute(RouteTransparency) }
                )
                GlasmioListRow(
                    title = "UI style",
                    detail = design,
                    onClick = { onRoute(RouteStyle) }
                )
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
}

@Composable
private fun SettingsSubPage(
    title: String,
    onBack: () -> Unit,
    content: LazyListScope.() -> Unit
) {
    GlasmioPinnedListPage(
        title = title,
        onNavigateBack = onBack,
        bottomPadding = 132.dp,
        backgroundColor = Color.Transparent,
        modifier = Modifier.fillMaxSize(),
        content = content
    )
}

@Composable
private fun SettingsThemePage(
    prefs: UserPreferencesRepository,
    backdrop: LayerBackdrop,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val theme by prefs.theme.collectAsState(initial = "System")
    val glass = LocalGlassColors.current
    SettingsSubPage("Theme", onBack) {
        item {
            GlasmioGroupedList {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    GlasmioSegmentedControl(
                        items = listOf("System" to "Auto", "Light" to "Light", "Dark" to "Dark"),
                        selected = theme.takeIf { it in setOf("System", "Light", "Dark") } ?: "System",
                        onSelected = { scope.launch { prefs.setTheme(it) } },
                        backdrop = backdrop,
                        height = 34.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Auto follows the system appearance.",
                        color = glass.secondaryContent,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsTextSizePage(
    prefs: UserPreferencesRepository,
    backdrop: LayerBackdrop,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val fontScale by prefs.fontSizeScale.collectAsState(initial = .90f)
    val glass = LocalGlassColors.current
    val values = remember { mapOf("XS" to .70f, "S" to .80f, "M" to .90f, "L" to 1.00f) }
    SettingsSubPage("Text size", onBack) {
        item {
            GlasmioGroupedList {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    GlasmioSegmentedControl(
                        items = listOf("XS", "S", "M", "L").map { it to it },
                        selected = scaleKey(fontScale),
                        onSelected = { key ->
                            values[key]?.let { v -> scope.launch { prefs.setFontSizeScale(v) } }
                        },
                        backdrop = backdrop,
                        height = 34.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Scales every text style in the interface.",
                        color = glass.secondaryContent,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsStylePage(
    prefs: UserPreferencesRepository,
    backdrop: LayerBackdrop,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val design by prefs.designLanguage.collectAsState(initial = "xvox")
    val glass = LocalGlassColors.current
    SettingsSubPage("UI style", onBack) {
        item {
            GlasmioGroupedList {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    GlasmioSegmentedControl(
                        items = listOf("xvox" to "XVOX", "material" to "M3", "ios" to "iOS"),
                        selected = design,
                        onSelected = { scope.launch { prefs.setDesignLanguage(it) } },
                        backdrop = backdrop,
                        height = 34.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Chooses the component language for rebuilt surfaces.",
                        color = glass.secondaryContent,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsTransparencyPage(
    prefs: UserPreferencesRepository,
    backdrop: LayerBackdrop,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val transparency by prefs.cardTransparency.collectAsState(initial = 0f)
    val glass = LocalGlassColors.current
    var live by remember(transparency) { mutableFloatStateOf(transparency) }
    SettingsSubPage("Card transparency", onBack) {
        item {
            GlasmioGroupedList {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text(
                        text = "${(live * 100).toInt()}%",
                        color = glass.content,
                        fontSize = 17.sp
                    )
                    GlasmioSlider(
                        value = live,
                        onValueChange = { live = it },
                        onValueChangeFinished = { scope.launch { prefs.setCardTransparency(live) } },
                        valueRange = 0f..0.6f,
                        backdrop = backdrop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    )
                    Text(
                        text = "Cards blend into the background; the glass bar follows the same alpha.",
                        color = glass.secondaryContent,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsAccentPage(
    prefs: UserPreferencesRepository,
    backdrop: LayerBackdrop,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val accent by prefs.accentColor.collectAsState(initial = "Red")
    val colors = com.xvox.music.core.design.theme.XvoxTheme.colors
    var pickerOpen by remember { mutableStateOf(false) }

    SettingsSubPage("Accent colour", onBack) {
        item {
            GlasmioGroupedList {
                listOf("Red" to "Red", "Blue" to "Blue", "White" to "White").forEachIndexed { index, preset ->
                    GlasmioListRow(
                        title = preset.second,
                        showTopSeparator = index != 0,
                        onClick = { scope.launch { prefs.setAccentColor(preset.first) } },
                        trailing = {
                            if (accent.equals(preset.first, true)) {
                                Text("✓", color = colors.primaryAccent, fontSize = 16.sp)
                            }
                        }
                    )
                }
                GlasmioListRow(
                    title = "Custom colour",
                    detail = if (accent.startsWith("#")) accent.uppercase() else "Pick a colour",
                    onClick = { pickerOpen = true }
                )
            }
        }
    }

    GlasmioColorPicker(
        visible = pickerOpen,
        selectedColor = colors.primaryAccent,
        onColorSelected = { color ->
            val hex = "#%06X".format(0xFFFFFF and color.toArgb())
            scope.launch { prefs.setAccentColor(hex) }
            pickerOpen = false
        },
        onDismiss = { pickerOpen = false },
        title = "Accent colour"
    )
}

@Composable
private fun SettingsBackgroundPage(
    prefs: UserPreferencesRepository,
    backdrop: LayerBackdrop,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val glass = LocalGlassColors.current
    val image by prefs.themeBackgroundImage.collectAsState(initial = "")
    val chrome by prefs.chromeStyle.collectAsState(initial = XvoxChromeStyle())
    val brightness by prefs.backgroundBrightness.collectAsState(initial = .8f)
    var liveBlur by remember(chrome.bgImageBlur) { mutableFloatStateOf(chrome.bgImageBlur) }
    var liveBrightness by remember(brightness) { mutableFloatStateOf(brightness) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            scope.launch { prefs.setThemeBackgroundImage(uri.toString()) }
        }
    }

    SettingsSubPage("Background", onBack) {
        item {
            GlasmioPreferenceGroupTitle("Photo")
            GlasmioGroupedList {
                GlasmioListRow(
                    title = if (image.isBlank()) "Choose photo" else "Change photo",
                    subtitle = if (image.isBlank()) "Uses any image from your library" else null,
                    showTopSeparator = false,
                    onClick = {
                        picker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
                GlasmioListRow(
                    title = "Remove photo",
                    enabled = image.isNotBlank(),
                    onClick = {
                        if (image.isNotBlank()) scope.launch { prefs.setThemeBackgroundImage(null) }
                    }
                )
            }
        }
        item {
            GlasmioPreferenceGroupTitle("Photo treatment")
            GlasmioGroupedList {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text("Blur", color = glass.content, fontSize = 17.sp)
                    Text(
                        text = "${(liveBlur * 100).toInt()}%",
                        color = glass.secondaryContent,
                        fontSize = 12.sp
                    )
                    GlasmioSlider(
                        value = liveBlur,
                        onValueChange = { liveBlur = it },
                        onValueChangeFinished = {
                            scope.launch {
                                prefs.setChromeStyle(
                                    chrome.copy(bgImageBlur = liveBlur, bgBlur = liveBlur)
                                )
                            }
                        },
                        backdrop = backdrop,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                    )
                    Text(
                        text = "Brightness",
                        color = glass.content,
                        fontSize = 17.sp,
                        modifier = Modifier.padding(top = 14.dp)
                    )
                    Text(
                        text = "${(liveBrightness * 100).toInt()}%",
                        color = glass.secondaryContent,
                        fontSize = 12.sp
                    )
                    GlasmioSlider(
                        value = liveBrightness,
                        onValueChange = { liveBrightness = it },
                        onValueChangeFinished = {
                            scope.launch { prefs.setBackgroundBrightness(liveBrightness) }
                        },
                        backdrop = backdrop,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                    )
                }
            }
        }
    }
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
