package com.xvox.music.features.settings.sections

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.chrome.XvoxChromeStyle
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.components.ColorPickerRow
import com.xvox.music.features.settings.components.SettingsChoiceRow
import com.xvox.music.features.settings.components.XvoxThinLineSlider
import kotlin.math.roundToInt

/**
 * Appearance — minimal settings UI.
 *
 * Background offers exactly two choices (Default or your Image), and every tint/colour/edge in
 * the app is tuned from its own card: Cards, Option boxes and Home chrome (header, mini player,
 * navbar and its pill). Every colour row opens the same free colour wheel + hex + intensity.
 */
@Composable
fun AppearanceSettingsSection(
    state: SettingsState,
    viewModel: SettingsViewModel
) {
    val colors = XvoxTheme.colors
    val chrome = state.chromeStyle
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.setBackgroundImage(uri.toString())
    }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {

        // ---------------------------------------------------------------- Theme + accent.
        GroupTitle("Theme")
        SettingsChoiceRow(
            listOf("System" to "System", "Light" to "Light", "Dark" to "Dark", "AMOLED" to "AMOLED"),
            state.theme
        ) { viewModel.setTheme(it) }

        GroupTitle("Accent")
        SettingsChoiceRow(
            listOf("Red" to "Red", "Blue" to "Blue", "White" to "White"),
            if (state.accentColor.startsWith("#")) "custom" else state.accentColor
        ) { key -> if (key == "custom") return@SettingsChoiceRow else viewModel.setAccentColor(key) }
        ColorPickerRow(
            label = "Custom accent colour",
            hex = if (state.accentColor.startsWith("#")) state.accentColor else "",
            onColorChange = { hex -> viewModel.setAccentColor(hex) },
            subtitle = if (state.accentColor.startsWith("#")) "Applied everywhere" else "Pick any colour to override"
        )

        // ---------------------------------------------------------------- Background.
        GroupTitle("Background")
        val imageUri = state.backgroundImageUri
        SettingsChoiceRow(
            listOf("Default" to "Default", "Image" to "Image"),
            if (imageUri == null) "Default" else "Image"
        ) { key ->
            when (key) {
                "Default" -> {
                    viewModel.setBackgroundImage(null)
                    viewModel.setBackgroundName("Default")
                }
                "Image" -> if (imageUri == null) imagePicker.launch("image/*")
            }
        }
        if (imageUri != null) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.cardElevated)
                ) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Background image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(120.dp)
                    )
                    // Remove (X).
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(30.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(colors.card.copy(alpha = 0.85f))
                            .xvoxPressScale { viewModel.setBackgroundImage(null) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_xvox_close),
                            contentDescription = "Remove background image",
                            tint = colors.primaryText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.card.copy(alpha = 0.9f))
                            .xvoxPressScale { imagePicker.launch("image/*") }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("Choose image", color = colors.primaryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Brightness", color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.width(86.dp))
                    XvoxThinLineSlider(
                        value = state.backgroundBrightness,
                        onValueChange = viewModel::setBackgroundBrightness,
                        valueRange = 0.2f..1f,
                        defaultValue = 0.8f,
                        modifier = Modifier.weight(1f)
                    )
                    Text("${(state.backgroundBrightness * 100).roundToInt()}%", color = colors.primaryText, fontSize = 12.sp, modifier = Modifier.width(42.dp))
                }
            }
        }

        // ---------------------------------------------------------------- Cards.
        GroupTitle("Cards")
        AlphaRow("Transparency", state.cardTransparency / 0.6f, { viewModel.setCardTransparency(it * 0.6f) }, defaultValue = 0f, labelLeft = "Solid", labelRight = "Glass")
        ColorPickerRow(
            label = "Border colour",
            hex = chrome.cardBorder,
            onColorChange = { hex -> viewModel.setChromeStyle { it.copy(cardBorder = hex) } }
        )
        AlphaRow("Border", chrome.cardBorderAlpha, { a -> viewModel.setChromeStyle { it.copy(cardBorderAlpha = a) } }, defaultValue = 1f)

        // ---------------------------------------------------------------- Option boxes.
        GroupTitle("Option boxes")
        AlphaRow("Fill", chrome.optionBoxBgAlpha, { a -> viewModel.setChromeStyle { it.copy(optionBoxBgAlpha = a) } }, defaultValue = 1f, labelRight = "Solid")
        ColorPickerRow(
            label = "Border colour",
            hex = chrome.optionBoxBorder,
            onColorChange = { hex -> viewModel.setChromeStyle { it.copy(optionBoxBorder = hex) } }
        )
        AlphaRow("Border", chrome.optionBoxBorderAlpha, { a -> viewModel.setChromeStyle { it.copy(optionBoxBorderAlpha = a) } }, defaultValue = 1f)

        // ---------------------------------------------------------------- Home chrome.
        GroupTitle("Header")
        ChromeStrip(chrome, viewModel,
            bgAlpha = chrome.headerBgAlpha,
            onBgAlpha = { a -> viewModel.setChromeStyle { it.copy(headerBgAlpha = a) } },
            border = chrome.headerBorder,
            onBorder = { hex -> viewModel.setChromeStyle { it.copy(headerBorder = hex) } },
            borderAlpha = chrome.headerBorderAlpha,
            onBorderAlpha = { a -> viewModel.setChromeStyle { it.copy(headerBorderAlpha = a) } }
        )

        GroupTitle("Mini player")
        ChromeStrip(chrome, viewModel,
            bgAlpha = chrome.miniBgAlpha,
            onBgAlpha = { a -> viewModel.setChromeStyle { it.copy(miniBgAlpha = a) } },
            border = chrome.miniBorder,
            onBorder = { hex -> viewModel.setChromeStyle { it.copy(miniBorder = hex) } },
            borderAlpha = chrome.miniBorderAlpha,
            onBorderAlpha = { a -> viewModel.setChromeStyle { it.copy(miniBorderAlpha = a) } }
        )

        GroupTitle("Navigation bar")
        ChromeStrip(chrome, viewModel,
            bgAlpha = chrome.navBgAlpha,
            onBgAlpha = { a -> viewModel.setChromeStyle { it.copy(navBgAlpha = a) } },
            border = chrome.navBorder,
            onBorder = { hex -> viewModel.setChromeStyle { it.copy(navBorder = hex) } },
            borderAlpha = chrome.navBorderAlpha,
            onBorderAlpha = { a -> viewModel.setChromeStyle { it.copy(navBorderAlpha = a) } }
        )
        ColorPickerRow(
            label = "Pill colour",
            hex = chrome.pillColor,
            onColorChange = { hex -> viewModel.setChromeStyle { it.copy(pillColor = hex) } }
        )
        AlphaRow("Pill", chrome.pillAlpha, { a -> viewModel.setChromeStyle { it.copy(pillAlpha = a) } }, defaultValue = 1f)

        GroupTitle("Text size")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("A", color = colors.mutedText, fontSize = 13.sp, modifier = Modifier.width(30.dp))
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

@Composable
private fun ChromeStrip(
    chrome: XvoxChromeStyle,
    viewModel: SettingsViewModel,
    bgAlpha: Float,
    onBgAlpha: (Float) -> Unit,
    border: String,
    onBorder: (String) -> Unit,
    borderAlpha: Float,
    onBorderAlpha: (Float) -> Unit
) {
    AlphaRow("Fill", bgAlpha, onBgAlpha, defaultValue = 1f)
    ColorPickerRow(
        label = "Border colour",
        hex = border,
        onColorChange = onBorder
    )
    AlphaRow("Border", borderAlpha, onBorderAlpha, defaultValue = if (borderAlpha == 0f) 0f else 1f)
}

@Composable
private fun AlphaRow(
    title: String,
    value: Float,
    onChange: (Float) -> Unit,
    defaultValue: Float,
    labelLeft: String = "Clear",
    labelRight: String = "Solid"
) {
    val colors = XvoxTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.width(86.dp))
        XvoxThinLineSlider(
            value = value.coerceIn(0f, 1f),
            onValueChange = onChange,
            valueRange = 0f..1f,
            defaultValue = defaultValue,
            modifier = Modifier.weight(1f)
        )
        Text(labelLeft, color = colors.mutedText, fontSize = 10.sp, modifier = Modifier.width(30.dp))
        Text("${(value.coerceIn(0f, 1f) * 100).roundToInt()}%", color = colors.primaryText, fontSize = 11.sp, modifier = Modifier.width(38.dp))
        Spacer(Modifier.width(2.dp))
    }
}

@Composable
private fun GroupTitle(title: String) {
    Text(
        text = title.uppercase(),
        color = com.xvox.music.core.design.theme.XvoxTheme.colors.mutedText,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(top = 4.dp)
    )
}


