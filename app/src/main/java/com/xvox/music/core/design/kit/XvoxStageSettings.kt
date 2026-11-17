package com.xvox.music.core.design.kit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.data.preferences.UserPreferencesRepository
import io.github.neoruaa.glasmio.core.LocalGlassColors
import io.github.neoruaa.glasmio.glass.render.backdrops.LayerBackdrop
import io.github.neoruaa.glasmio.preference.GlasmioPreferenceGroupTitle
import io.github.neoruaa.glasmio.preference.GlasmioSwitchPreference
import io.github.neoruaa.glasmio.scaffold.GlasmioPinnedListPage
import io.github.neoruaa.glasmio.ui.GlasmioGroupedList
import io.github.neoruaa.glasmio.ui.GlasmioListRow
import io.github.neoruaa.glasmio.ui.GlasmioSegmentedControl
import io.github.neoruaa.glasmio.ui.GlasmioSlider
import kotlinx.coroutines.launch

/**
 * Settings built entirely from Glasmio: collapsing large title, inset grouped tables, native
 * segmented controls, switches and a glass slider. Every control writes the DataStore that the
 * rest of XVOX already reads, so changes apply immediately.
 */
@Composable
fun XvoxStageSettingsPage(
    prefs: UserPreferencesRepository,
    backdrop: LayerBackdrop,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val theme by prefs.theme.collectAsState(initial = "System")
    val accent by prefs.accentColor.collectAsState(initial = "Red")
    val design by prefs.designLanguage.collectAsState(initial = "xvox")
    val background by prefs.themeBackground.collectAsState(initial = "Default")
    val fontScale by prefs.fontSizeScale.collectAsState(initial = .90f)
    val transparency by prefs.cardTransparency.collectAsState(initial = 0f)
    val hapticsEnabled by prefs.hapticFeedbackEnabled.collectAsState(initial = true)
    val glass = LocalGlassColors.current

    var liveTransparency by remember(transparency) { mutableFloatStateOf(transparency) }

    val scaleKey = when {
        fontScale <= .74f -> "XS"
        fontScale <= .84f -> "S"
        fontScale <= .94f -> "M"
        else -> "L"
    }
    val scaleValue = mapOf("XS" to .70f, "S" to .80f, "M" to .90f, "L" to 1.00f)

    GlasmioPinnedListPage(
        title = "Settings",
        onNavigateBack = onBack,
        bottomPadding = 56.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            GlasmioPreferenceGroupTitle("Appearance")
            GlasmioGroupedList {
                GlasmioListRow(
                    title = "Theme",
                    showTopSeparator = false,
                    trailing = {
                        GlasmioSegmentedControl(
                            items = listOf("System" to "Auto", "Light" to "Light", "Dark" to "Dark"),
                            selected = theme.takeIf { it in setOf("System", "Light", "Dark") } ?: "System",
                            onSelected = { scope.launch { prefs.setTheme(it) } },
                            backdrop = backdrop,
                            height = 30.dp
                        )
                    }
                )
                GlasmioListRow(
                    title = "Accent",
                    trailing = {
                        GlasmioSegmentedControl(
                            items = listOf("Red" to "Red", "Blue" to "Blue"),
                            selected = if (accent.equals("Blue", true)) "Blue" else "Red",
                            onSelected = { scope.launch { prefs.setAccentColor(it) } },
                            backdrop = backdrop,
                            height = 30.dp
                        )
                    }
                )
                GlasmioListRow(
                    title = "Background",
                    trailing = {
                        GlasmioSegmentedControl(
                            items = listOf("Default" to "Plain", "Midnight" to "Night", "Warm" to "Warm"),
                            selected = background.takeIf { it in setOf("Default", "Midnight", "Warm") } ?: "Default",
                            onSelected = { scope.launch { prefs.setThemeBackground(it) } },
                            backdrop = backdrop,
                            height = 30.dp
                        )
                    }
                )
                GlasmioListRow(
                    title = "Text size",
                    trailing = {
                        GlasmioSegmentedControl(
                            items = listOf("XS", "S", "M", "L").map { it to it },
                            selected = scaleKey,
                            onSelected = { key ->
                                scaleValue[key]?.let { value ->
                                    scope.launch { prefs.setFontSizeScale(value) }
                                }
                            },
                            backdrop = backdrop,
                            height = 30.dp
                        )
                    }
                )
                GlasmioListRow(
                    title = "UI style",
                    trailing = {
                        GlasmioSegmentedControl(
                            items = listOf("xvox" to "XVOX", "material" to "M3", "ios" to "iOS"),
                            selected = design,
                            onSelected = { scope.launch { prefs.setDesignLanguage(it) } },
                            backdrop = backdrop,
                            height = 30.dp
                        )
                    }
                )
            }
        }

        item {
            GlasmioPreferenceGroupTitle("Glass")
            GlasmioGroupedList {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text(
                        text = "Card transparency",
                        color = glass.content,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "${(liveTransparency * 100).toInt()}%",
                        color = glass.secondaryContent,
                        fontSize = 12.sp
                    )
                    GlasmioSlider(
                        value = liveTransparency,
                        onValueChange = { liveTransparency = it },
                        onValueChangeFinished = {
                            scope.launch { prefs.setCardTransparency(liveTransparency) }
                        },
                        backdrop = backdrop
                    )
                }
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
                    title = "Design engine",
                    detail = "Glasmio + M3",
                    showTopSeparator = false
                )
            }
        }
    }
}
