package com.xvox.music.features.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxFontOption
import com.xvox.music.core.design.theme.XvoxFontOptions
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.overlay.xvoxBoxScroll

/**
 * A Settings-native offline font picker. System remains first and uses FontFamily.Default; each
 * bundled option previews the actual persistent family rather than a lookalike sample typeface.
 */
@Composable
fun XvoxFontPickerContent(
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 520.dp)
            .verticalScroll(scrollState)
            .xvoxBoxScroll(scrollState)
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Choose the typeface used throughout XVOX.",
            color = colors.secondaryText,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 4.dp, bottom = 2.dp)
        )
        XvoxFontOptions.forEach { option ->
            XvoxFontSettingsRow(
                option = option,
                selected = option.key == selectedKey,
                onClick = { onSelect(option.key) }
            )
        }
        Text(
            text = "System follows your Android device font. Other families are bundled offline under SIL Open Font License 1.1.",
            color = colors.mutedText,
            fontSize = 10.sp,
            lineHeight = 13.sp,
            modifier = Modifier.padding(horizontal = 4.dp, top = 3.dp)
        )
    }
}

/** Appearance-card trigger in the same full-width option-row language as Settings. */
@Composable
fun XvoxFontPickerButton(
    selectedKey: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val option = XvoxFontOptions.firstOrNull { it.key == selectedKey }
        ?: XvoxFontOptions.first { it.key == "inter" }
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(colors.cardElevated)
            .border(.8.dp, colors.cardBorder, shape)
            .xvoxPressScale(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = option.label,
                color = colors.primaryText,
                fontFamily = option.fontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (option.key == "system") "Android default typeface" else "Aa The quick brown fox",
                color = colors.secondaryText,
                fontFamily = option.fontFamily,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text("Change", color = colors.primaryAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun XvoxFontSettingsRow(
    option: XvoxFontOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) colors.accentSoft else colors.cardElevated)
            .border(
                width = if (selected) 1.dp else .8.dp,
                color = if (selected) colors.primaryAccent.copy(alpha = .72f) else colors.cardBorder,
                shape = shape
            )
            .xvoxPressScale(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = option.label,
                color = colors.primaryText,
                fontFamily = option.fontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (option.key == "system") "Android system font" else "Aa The quick brown fox jumps",
                color = colors.secondaryText,
                fontFamily = option.fontFamily,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(colors.primaryAccent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_xvox_check),
                    contentDescription = "Selected",
                    tint = colors.background,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}
