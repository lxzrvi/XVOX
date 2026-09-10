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
import com.xvox.music.core.ui.chrome.XvoxChromeStyle
import com.xvox.music.core.ui.chrome.parseHexColor
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.components.ColorPickerRow
import com.xvox.music.features.settings.components.SettingsChoiceRow
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

        // Live preview of the current chrome settings (header / mini player / nav pill).
        XvoxChromePreview(chrome)
        Spacer(Modifier.height(2.dp))

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

        GroupTitle("Chrome")
        // One slider controls the whole top + bottom chrome together.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Header / Mini / Bar", color = colors.primaryAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(118.dp))
            XvoxThinLineSlider(
                value = chrome.headerBgAlpha,
                onValueChange = { a ->
                    viewModel.setChromeStyle { it.copy(headerBgAlpha = a, miniBgAlpha = a, navBgAlpha = a) }
                },
                valueRange = 0.25f..1f,
                defaultValue = 0.88f,
                modifier = Modifier.weight(1f)
            )
            Text("${(chrome.headerBgAlpha.coerceIn(0f, 1f) * 100).roundToInt()}%",
                color = colors.primaryText, fontSize = 11.sp, modifier = Modifier.width(40.dp))
        }

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
private fun XvoxChromePreview(chrome: XvoxChromeStyle) {
    val colors = XvoxTheme.colors
    val fillAlpha = chrome.headerBgAlpha.coerceIn(0f, 1f)
    val pillIcon = parseHexColor(chrome.pillIconColor) ?: colors.primaryAccent
    val pillBg = parseHexColor(chrome.pillColor)
        ?: colors.cardElevated.copy(alpha = 0.42f)
    val pillFill = if (chrome.pillColor.isBlank()) pillBg
        else pillBg.copy(alpha = pillBg.alpha * chrome.pillAlpha.coerceIn(0f, 1f))

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.card)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text("PREVIEW", color = colors.secondaryText, fontSize = 9.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold)

        // Header strip.
        Row(
            Modifier.fillMaxWidth().height(26.dp).clip(RoundedCornerShape(8.dp))
                .background(colors.surface.copy(alpha = fillAlpha)).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(14.dp).clip(CircleShape).background(colors.primaryAccent.copy(alpha = 0.8f)))
            Spacer(Modifier.width(8.dp))
            Box(Modifier.width(52.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(colors.primaryText.copy(alpha = .55f)))
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(10.dp).clip(CircleShape).background(colors.cardBorder))
        }

        // Mini player bar.
        Row(
            Modifier.fillMaxWidth().height(34.dp).clip(RoundedCornerShape(9.dp))
                .background(colors.surface.copy(alpha = chrome.miniBgAlpha.coerceIn(0f, 1f))).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(colors.primaryAccent.copy(alpha = 0.35f)))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Box(Modifier.width(70.dp).height(5.dp).clip(RoundedCornerShape(3.dp)).background(colors.primaryText.copy(alpha = .5f)))
                Spacer(Modifier.height(4.dp))
                Box(Modifier.width(42.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(colors.secondaryText.copy(alpha = .5f)))
            }
            Box(Modifier.size(20.dp).clip(CircleShape).background(colors.primaryAccent.copy(alpha = 0.35f)))
        }

        // Floating nav bar with pill.
        Box(Modifier.fillMaxWidth().height(34.dp), contentAlignment = Alignment.Center) {
            Row(
                Modifier.width(150.dp).height(26.dp).clip(RoundedCornerShape(13.dp))
                    .background(colors.surface.copy(alpha = chrome.navBgAlpha.coerceIn(0f, 1f))).padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(true, false, false, false).forEach { active ->
                    Box(
                        Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(10.dp))
                            .background(if (active) pillFill else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier.size(7.dp).clip(CircleShape)
                                .background(if (active) pillIcon else colors.mutedText.copy(alpha = .5f))
                        )
                    }
                }
            }
        }
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
        modifier = Modifier.padding(top = 6.dp)
    )
}
