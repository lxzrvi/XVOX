package com.xvox.music.features.settings.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.chrome.parseHexColor
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.components.ColorPickerRow
import com.xvox.music.features.settings.components.SettingsChoiceRow
import com.xvox.music.features.settings.components.SettingsToggle
import com.xvox.music.features.settings.components.XvoxThinLineSlider
import kotlin.math.roundToInt

/**
 * Appearance — minimal. No Background section, no Cards section.
 *
 * One "Chrome" slider drives the header, the mini player and the nav bar together; the nav pill
 * keeps its own colour, transparency and icon colour. The little preview above shows every
 * change live, so nothing here needs a paragraph of explanation.
 */
@Composable
fun AppearanceSettingsSection(
    state: SettingsState,
    viewModel: SettingsViewModel
) {
    val colors = XvoxTheme.colors
    val chrome = state.chromeStyle

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {

        // The preview lives in the Settings top pane only; this section is controls alone.

        // Option boxes: every popup opened over the app (queue, playlist picker, song options).
        GroupTitle("Option boxes")
        AlphaRow("Fill", chrome.optionBoxBgAlpha) { a ->
            viewModel.setChromeStyle { it.copy(optionBoxBgAlpha = a) }
        }
        ColorPickerRow(
            label = "Border",
            hex = chrome.optionBoxBorder,
            onColorChange = { hex -> viewModel.setChromeStyle { it.copy(optionBoxBorder = hex) } },
            subtitle = if (chrome.optionBoxBorder.isBlank()) "Default: theme border" else "Box outline",
            alpha = chrome.optionBoxBorderAlpha.coerceIn(0f, 1f),
            onAlphaChange = { a -> viewModel.setChromeStyle { it.copy(optionBoxBorderAlpha = a) } }
        )

        GroupTitle("Background")
        SettingsChoiceRow(
            listOf("Default" to "Default", "Dim" to "Dim", "Dark" to "Dark"),
            state.backgroundName
        ) { viewModel.setBackgroundName(it) }
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

        GroupTitle("Theme")
        SettingsChoiceRow(
            listOf("System" to "System", "Light" to "Light", "Dark" to "Dark", "AMOLED" to "AMOLED"),
            state.theme
        ) { viewModel.setTheme(it) }

        GroupTitle("Accent")
        SettingsChoiceRow(
            listOf("Red" to "Red", "Blue" to "Blue", "White" to "White"),
            if (state.accentColor.startsWith("#")) "custom" else state.accentColor
        ) { key -> if (key != "custom") viewModel.setAccentColor(key) }
        ColorPickerRow(
            label = "Custom accent",
            hex = if (state.accentColor.startsWith("#")) state.accentColor else "",
            onColorChange = { hex -> viewModel.setAccentColor(hex) },
            subtitle = if (state.accentColor.startsWith("#")) "Applied everywhere" else "Pick any colour"
        )

        GroupTitle("Header")
        // The header photo sits behind the header strip; the transparency below is the header's
        // own, kept separate from the mini player and the nav bar.
        HeaderPhotoRow(state, viewModel)
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

        ColorPickerRow(
            label = "Border",
            hex = chrome.headerBorder,
            onColorChange = { hex -> viewModel.setChromeStyle { it.copy(headerBorder = hex) } },
            subtitle = if (chrome.headerBorder.isBlank()) "Default: theme border" else "Header hairline",
            alpha = chrome.headerBorderAlpha.coerceIn(0f, 1f),
            onAlphaChange = { a -> viewModel.setChromeStyle { it.copy(headerBorderAlpha = a) } }
        )

        GroupTitle("Mini player")
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

        ColorPickerRow(
            label = "Border",
            hex = chrome.miniBorder,
            onColorChange = { hex -> viewModel.setChromeStyle { it.copy(miniBorder = hex) } },
            subtitle = if (chrome.miniBorder.isBlank()) "Default: theme border" else "Mini player outline",
            alpha = chrome.miniBorderAlpha.coerceIn(0f, 1f),
            onAlphaChange = { a -> viewModel.setChromeStyle { it.copy(miniBorderAlpha = a) } }
        )

        GroupTitle("Nav bar")
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

        ColorPickerRow(
            label = "Border",
            hex = chrome.navBorder,
            onColorChange = { hex -> viewModel.setChromeStyle { it.copy(navBorder = hex) } },
            subtitle = if (chrome.navBorder.isBlank()) "Default: theme border" else "Bar and pill outline",
            alpha = chrome.navBorderAlpha.coerceIn(0f, 1f),
            onAlphaChange = { a -> viewModel.setChromeStyle { it.copy(navBorderAlpha = a) } }
        )

        GroupTitle("Nav pill")
        // The fill's transparency lives inside its own picker — a single "Transparency" slider
        // under the colour wheel, so the fill and its opacity are tuned in one place.
        ColorPickerRow(
            label = "Pill colour",
            hex = chrome.pillColor,
            onColorChange = { hex -> viewModel.setChromeStyle { it.copy(pillColor = hex) } },
            subtitle = if (chrome.pillColor.isBlank()) "Default: soft grey" else "Custom pill fill",
            alpha = chrome.pillAlpha.coerceIn(0f, 1f),
            onAlphaChange = { a -> viewModel.setChromeStyle { it.copy(pillAlpha = a.coerceIn(0f, 1f)) } }
        )
        ColorPickerRow(
            label = "Icon colour",
            hex = chrome.pillIconColor,
            onColorChange = { hex -> viewModel.setChromeStyle { it.copy(pillIconColor = hex) } },
            subtitle = if (chrome.pillIconColor.isBlank()) "Default: your accent" else "Icon on the pill"
        )

        GroupTitle("Cards")
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

        ColorPickerRow(
            label = "Border",
            hex = chrome.cardBorder,
            onColorChange = { hex -> viewModel.setChromeStyle { it.copy(cardBorder = hex) } },
            subtitle = if (chrome.cardBorder.isBlank()) "Default: theme border" else "Card outline",
            alpha = chrome.cardBorderAlpha.coerceIn(0f, 1f),
            onAlphaChange = { a -> viewModel.setChromeStyle { it.copy(cardBorderAlpha = a) } }
        )

        // Last control in Appearance: the Settings top preview pane can be turned off entirely.
        SettingsToggle(
            title = "Hide preview",
            subtitle = "Hide the live preview at the top of Settings",
            checked = state.previewHidden,
            onChange = viewModel::setPreviewHidden
        )

        GroupTitle("Text size")
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
}

/** A compact live snapshot of the header, mini player and floating nav bar using current chrome. */

@Composable
private fun GroupTitle(title: String) {
    Text(
        text = title.uppercase(),
        color = com.xvox.music.core.design.theme.XvoxTheme.colors.mutedText,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(top = 6.dp)
    )
}

/**
 * The Home header can carry the user's own photo. It is stored as a URI and drawn behind the
 * header's own transparency scrim, so the transparency slider decides how much of it shows.
 */
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
            // An opaque header would hide the photo completely, so picking a photo reveals it.
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

/** A labelled transparency slider row, used by the per-surface chrome controls. */
@Composable
private fun AlphaRow(label: String, value: Float, onChange: (Float) -> Unit) {
    val colors = XvoxTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = colors.primaryAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(118.dp))
        XvoxThinLineSlider(
            value = value.coerceIn(0f, 1f),
            onValueChange = { onChange(it.coerceIn(0f, 1f)) },
            valueRange = 0f..1f,
            defaultValue = 1f,
            modifier = Modifier.weight(1f)
        )
        Text("${(value.coerceIn(0f, 1f) * 100).roundToInt()}%",
            color = colors.primaryText, fontSize = 11.sp, modifier = Modifier.width(40.dp))
    }
}
