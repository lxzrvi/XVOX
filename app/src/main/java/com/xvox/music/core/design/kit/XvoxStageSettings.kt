package com.xvox.music.core.design.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.features.setup.PfpIcon
import com.xvox.music.features.setup.PfpType
import io.github.neoruaa.glasmio.core.LocalGlassColors
import io.github.neoruaa.glasmio.glass.GlassEmphasis
import io.github.neoruaa.glasmio.glass.render.backdrops.LayerBackdrop
import io.github.neoruaa.glasmio.preference.GlasmioPreferenceGroupTitle
import io.github.neoruaa.glasmio.preference.GlasmioSwitchPreference
import io.github.neoruaa.glasmio.scaffold.GlasmioPinnedListPage
import io.github.neoruaa.glasmio.ui.GlasmioBottomSheet
import io.github.neoruaa.glasmio.ui.GlasmioButton
import io.github.neoruaa.glasmio.ui.GlasmioGroupedList
import io.github.neoruaa.glasmio.ui.GlasmioListRow
import io.github.neoruaa.glasmio.ui.GlasmioSegmentedControl
import io.github.neoruaa.glasmio.ui.GlasmioTextField
import kotlinx.coroutines.launch

/**
 * Settings, built from Glasmio's own page and list vocabulary: a collapsing large title, inset
 * grouped tables, native switches and segmented controls, driven by the same DataStore that the
 * rest of XVOX already reads.
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
    val hapticsEnabled by prefs.hapticFeedbackEnabled.collectAsState(initial = true)

    GlasmioPinnedListPage(
        title = "Settings",
        onNavigateBack = onBack,
        bottomPadding = 48.dp,
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
                    title = "Engine",
                    detail = "Glasmio + M3 Expressive",
                    showTopSeparator = false
                )
            }
        }
    }
}

/**
 * Edit Profile, presented as a Glasmio sheet: name field plus the built-in avatar set, saved into
 * the same profile record the Home header uses.
 */
@Composable
fun XvoxStageEditProfileSheet(
    prefs: UserPreferencesRepository,
    backdrop: LayerBackdrop,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val colors = XvoxTheme.colors
    val profile by prefs.preferences.collectAsState(initial = null)
    var name by remember { mutableStateOf(profile?.username.orEmpty()) }
    var pfp by remember { mutableStateOf(profile?.selectedPfp ?: PfpType.DEFAULT.name) }

    GlasmioBottomSheet(
        onDismissRequest = onDismiss,
        skipPartiallyExpanded = false,
        backdrop = backdrop,
        title = "Edit Profile"
    ) {
        GlasmioTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = "Your name",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(18.dp))
        Text(
            text = "Avatar",
            color = LocalGlassColors.current.secondaryContent,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PfpType.entries.filter { it != PfpType.CUSTOM }.forEach { type ->
                val selected = pfp == type.name
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) colors.primaryAccent.copy(alpha = .22f)
                            else colors.cardElevated.copy(alpha = .55f)
                        )
                        .clickable { pfp = type.name },
                    contentAlignment = Alignment.Center
                ) {
                    PfpIcon(
                        type = type,
                        color = if (selected) colors.primaryAccent else colors.mutedText,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        GlasmioButton(
            onClick = {
                scope.launch {
                    prefs.saveProfile(name, pfp, null)
                    onDismiss()
                }
            },
            backdrop = backdrop,
            emphasis = GlassEmphasis.Prominent,
            enabled = name.isNotBlank(),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text("Save")
        }
    }
}
