package com.xvox.music.features.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxFontOption
import com.xvox.music.core.design.theme.XvoxFontOptions
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.overlay.xvoxBoxScroll

/** A compact, offline picker. Every choice previews itself in its real bundled OFL typeface. */
@Composable
fun XvoxFontPickerContent(
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
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
            text = "Choose a font",
            color = XvoxTheme.colors.secondaryText,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
        XvoxFontOptions.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { option ->
                    XvoxFontChoiceButton(
                        option = option,
                        selected = option.key == selectedKey,
                        onClick = { onSelect(option.key) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) Box(Modifier.weight(1f))
            }
        }
        Text(
            text = "All listed families are bundled offline under SIL Open Font License 1.1.",
            color = XvoxTheme.colors.mutedText,
            fontSize = 10.sp,
            lineHeight = 13.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

/** Small Appearance-card trigger that previews the currently active typeface before opening it. */
@Composable
fun XvoxFontPickerButton(
    selectedKey: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val option = XvoxFontOptions.firstOrNull { it.key == selectedKey } ?: XvoxFontOptions.first()
    XvoxFontChoiceButton(option = option, selected = false, onClick = onClick, modifier = modifier, openLabel = true)
}

@Composable
private fun XvoxFontChoiceButton(
    option: XvoxFontOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    openLabel: Boolean = false
) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .heightIn(min = if (openLabel) 48.dp else 70.dp)
            .clip(shape)
            .background(if (selected) colors.primaryAccent else colors.cardElevated)
            .xvoxPressScale(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = if (openLabel) 8.dp else 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (openLabel) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = option.label,
                        color = colors.primaryText,
                        fontFamily = option.fontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Tap to change",
                        color = colors.secondaryText,
                        fontSize = 10.sp
                    )
                }
                Text("›", color = colors.primaryAccent, fontSize = 22.sp)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = option.label,
                    color = if (selected) colors.background else colors.primaryText,
                    fontFamily = option.fontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Aa The quick brown fox",
                    color = if (selected) colors.background.copy(alpha = .78f) else colors.secondaryText,
                    fontFamily = option.fontFamily,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
