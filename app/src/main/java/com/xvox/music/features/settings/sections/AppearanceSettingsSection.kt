package com.xvox.music.features.settings.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.components.ColorPickerRow
import com.xvox.music.features.settings.components.SettingsAccordionItem
import com.xvox.music.features.settings.components.SettingsChoiceRow
import com.xvox.music.features.settings.components.SettingsControlsEditor
import com.xvox.music.features.settings.components.SettingsToggle
import com.xvox.music.features.settings.components.XvoxThinLineSlider
import kotlin.math.roundToInt

/**
 * Appearance settings — organized into accordion label cards.
 * Tapping a card expands it and collapses the previous one.
 */
@Composable
fun AppearanceSettingsSection(
    state: SettingsState,
    viewModel: SettingsViewModel
) {
    val colors = XvoxTheme.colors
    val chrome = state.chromeStyle
    var expandedGroup by remember { mutableStateOf<String?>("Theme") }

    fun toggle(group: String) {
        expandedGroup = if (expandedGroup == group) null else group
    }

    SettingsControlsEditor(controls = {
        SettingsAccordionItem(
            title = "Theme",
            expanded = expandedGroup == "Theme",
            onToggle = { toggle("Theme") }
        ) {
            SettingsChoiceRow(
                listOf("System" to "System", "Light" to "Light", "Dark" to "Dark", "AMOLED" to "AMOLED"),
                state.theme
            ) { viewModel.setTheme(it) }
        }

        SettingsAccordionItem(
            title = "Accent",
            expanded = expandedGroup == "Accent",
            onToggle = { toggle("Accent") }
        ) {
            SettingsChoiceRow(
                listOf("Red" to "Red", "Blue" to "Blue", "White" to "White"),
                if (state.accentColor.startsWith("#")) "custom" else state.accentColor
            ) { key -> if (key != "custom") viewModel.setAccentColor(key) }
            Spacer(Modifier.height(8.dp))
            ColorPickerRow(
                label = "Custom accent",
                hex = if (state.accentColor.startsWith("#")) state.accentColor else "",
                onColorChange = { hex -> viewModel.setAccentColor(hex) },
                subtitle = if (state.accentColor.startsWith("#")) "Applied everywhere" else "Pick any colour"
            )
        }

        SettingsAccordionItem(
            title = "Background",
            expanded = expandedGroup == "Background",
            onToggle = { toggle("Background") }
        ) {
            SettingsChoiceRow(
                listOf("Default" to "Default", "Dim" to "Dim", "Dark" to "Dark"),
                state.backgroundName
            ) { viewModel.setBackgroundName(it) }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Brightness", color = colors.primaryAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(118.dp))
                XvoxThinLineSlider(
                    value = state.backgroundBrightness,
                    onValueChange = viewModel::setBackgroundBrightness,
                    valueRange = 0.3f..1f,
                    defaultValue = 0.8f,
                    modifier = Modifier.weight(1f)
                )
                Text("${(state.backgroundBrightness.coerceIn(0f, 1f) * 100).roundToInt()}%",
                    color = colors.primaryText, fontSize = 11.sp, modifier = Modifier.width(40.dp))
            }
        }

        SettingsAccordionItem(
            title = "Header",
            expanded = expandedGroup == "Header",
            onToggle = { toggle("Header") }
        ) {
            HeaderPhotoRow(state, viewModel)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Transparency", color = colors.primaryAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(118.dp))
                XvoxThinLineSlider(
                    value = chrome.headerBgAlpha,
                    onValueChange = { a -> viewModel.setChromeStyle { it.copy(headerBgAlpha = a) } },
                    valueRange = 0f..1f,
                    defaultValue = 1f,
                    modifier = Modifier.weight(1f)
                )
                Text("${(chrome.headerBgAlpha.coerceIn(0f, 1f) * 100).roundToInt()}%",
                    color = colors.primaryText, fontSize = 11.sp, modifier = Modifier.width(40.dp))
            }
            Spacer(Modifier.height(8.dp))
            ColorPickerRow(
                label = "Border",
                hex = chrome.headerBorder,
                onColorChange = { hex -> viewModel.setChromeStyle { it.copy(headerBorder = hex) } },
                subtitle = if (chrome.headerBorder.isBlank()) "Default: theme border" else "Header hairline",
                alpha = chrome.headerBorderAlpha.coerceIn(0f, 1f),
                onAlphaChange = { a -> viewModel.setChromeStyle { it.copy(headerBorderAlpha = a) } }
            )
        }

        SettingsAccordionItem(
            title = "Mini player",
            expanded = expandedGroup == "Mini player",
            onToggle = { toggle("Mini player") }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Transparency", color = colors.primaryAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(118.dp))
                XvoxThinLineSlider(
                    value = chrome.miniBgAlpha,
                    onValueChange = { a -> viewModel.setChromeStyle { it.copy(miniBgAlpha = a) } },
                    valueRange = 0f..1f,
                    defaultValue = 1f,
                    modifier = Modifier.weight(1f)
                )
                Text("${(chrome.miniBgAlpha.coerceIn(0f, 1f) * 100).roundToInt()}%",
                    color = colors.primaryText, fontSize = 11.sp, modifier = Modifier.width(40.dp))
            }
            Spacer(Modifier.height(8.dp))
            ColorPickerRow(
                label = "Border",
                hex = chrome.miniBorder,
                onColorChange = { hex -> viewModel.setChromeStyle { it.copy(miniBorder = hex) } },
                subtitle = if (chrome.miniBorder.isBlank()) "Default: theme border" else "Mini player outline",
                alpha = chrome.miniBorderAlpha.coerceIn(0f, 1f),
                onAlphaChange = { a -> viewModel.setChromeStyle { it.copy(miniBorderAlpha = a) } }
            )
        }

        SettingsAccordionItem(
            title = "Nav bar",
            expanded = expandedGroup == "Nav bar",
            onToggle = { toggle("Nav bar") }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Transparency", color = colors.primaryAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(118.dp))
                XvoxThinLineSlider(
                    value = chrome.navBgAlpha,
                    onValueChange = { a -> viewModel.setChromeStyle { it.copy(navBgAlpha = a) } },
                    valueRange = 0f..1f,
                    defaultValue = 0.88f,
                    modifier = Modifier.weight(1f)
                )
                Text("${(chrome.navBgAlpha.coerceIn(0f, 1f) * 100).roundToInt()}%",
                    color = colors.primaryText, fontSize = 11.sp, modifier = Modifier.width(40.dp))
            }
            Spacer(Modifier.height(8.dp))
            ColorPickerRow(
                label = "Border",
                hex = chrome.navBorder,
                onColorChange = { hex -> viewModel.setChromeStyle { it.copy(navBorder = hex) } },
                subtitle = if (chrome.navBorder.isBlank()) "Default: theme border" else "Bar and pill outline",
                alpha = chrome.navBorderAlpha.coerceIn(0f, 1f),
                onAlphaChange = { a -> viewModel.setChromeStyle { it.copy(navBorderAlpha = a) } }
            )
        }

        SettingsAccordionItem(
            title = "Cards",
            expanded = expandedGroup == "Cards",
            onToggle = { toggle("Cards") }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Transparency", color = colors.primaryAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(118.dp))
                XvoxThinLineSlider(
                    value = state.cardTransparency,
                    onValueChange = viewModel::setCardTransparency,
                    valueRange = 0f..0.6f,
                    defaultValue = 0f,
                    modifier = Modifier.weight(1f)
                )
                Text("${(state.cardTransparency.coerceIn(0f, 1f) * 100).roundToInt()}%",
                    color = colors.primaryText, fontSize = 11.sp, modifier = Modifier.width(40.dp))
            }
            Spacer(Modifier.height(8.dp))
            ColorPickerRow(
                label = "Border",
                hex = chrome.cardBorder,
                onColorChange = { hex -> viewModel.setChromeStyle { it.copy(cardBorder = hex) } },
                subtitle = if (chrome.cardBorder.isBlank()) "Default: theme border" else "Card outline",
                alpha = chrome.cardBorderAlpha.coerceIn(0f, 1f),
                onAlphaChange = { a -> viewModel.setChromeStyle { it.copy(cardBorderAlpha = a) } }
            )
        }

        SettingsAccordionItem(
            title = "Option boxes",
            expanded = expandedGroup == "Option boxes",
            onToggle = { toggle("Option boxes") }
        ) {
            ColorPickerRow(
                label = "Border",
                hex = chrome.optionBoxBorder,
                onColorChange = { hex -> viewModel.setChromeStyle { it.copy(optionBoxBorder = hex) } },
                subtitle = if (chrome.optionBoxBorder.isBlank()) "Default: theme border" else "Box outline",
                alpha = chrome.optionBoxBorderAlpha.coerceIn(0f, 1f),
                onAlphaChange = { a -> viewModel.setChromeStyle { it.copy(optionBoxBorderAlpha = a) } }
            )
        }

        SettingsAccordionItem(
            title = "Text size",
            expanded = expandedGroup == "Text size",
            onToggle = { toggle("Text size") }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("A", color = colors.mutedText, fontSize = 12.sp, modifier = Modifier.width(30.dp))
                XvoxThinLineSlider(
                    value = state.fontSizeScale,
                    onValueChange = viewModel::setFontSizeScale,
                    valueRange = 0.8f..1.4f,
                    defaultValue = 1f,
                    modifier = Modifier.weight(1f)
                )
                Text("A", color = colors.primaryText, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp))
            }
        }

        SettingsToggle(
            title = "Hide preview",
            subtitle = "Hide the live preview at the top of Settings",
            checked = state.previewHidden,
            onChange = viewModel::setPreviewHidden
        )
    })
}

@Composable
private fun HeaderPhotoRow(state: SettingsState, viewModel: SettingsViewModel) {
    val colors = XvoxTheme.colors
    val context = androidx.compose.ui.platform.LocalContext.current
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.setHeaderImageUri(uri.toString())
            if (viewModel.state.value.chromeStyle.headerBgAlpha >= 0.9f) {
                viewModel.setChromeStyle { it.copy(headerBgAlpha = 0.28f) }
            }
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Photo", color = colors.primaryAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(110.dp))
        Box(
            modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(colors.card)
                .xvoxPressScale {
                    picker.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                }
                .padding(horizontal = 14.dp, vertical = 9.dp)
        ) { Text(if (state.headerImageUri == null) "Choose photo" else "Change photo",
            color = colors.primaryText, fontSize = 12.sp) }
        if (state.headerImageUri != null) {
            Box(
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(colors.cardElevated)
                    .xvoxPressScale { viewModel.setHeaderImageUri(null) }
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) { Text("Remove", color = colors.secondaryText, fontSize = 12.sp) }
        }
    }
}
