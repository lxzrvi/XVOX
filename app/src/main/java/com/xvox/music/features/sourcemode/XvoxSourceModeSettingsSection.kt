package com.xvox.music.features.sourcemode

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme

/**
 * Experimental source-mode card kept in its own feature package. The selected mode is live and
 * persistent; Online deliberately presents capability/readiness rather than pretending that an
 * unapproved catalogue or stream source exists.
 */
@Composable
fun XvoxSourceModeSettingsSection(
    mode: XvoxSourceMode,
    onModeSelected: (XvoxSourceMode) -> Unit,
    providerGateway: XvoxOfficialProviderGateway = XvoxUnconfiguredOfficialProviderGateway,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.card)
            .border(1.dp, colors.cardBorder.copy(alpha = if (colors.isLight) .80f else .72f), shape)
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Experimental",
            color = colors.primaryAccent,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Music source",
            color = colors.mutedText,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp)
        )
        XvoxSourceModePill(mode = mode, onModeSelected = onModeSelected)
        Text(
            text = mode.summary,
            color = colors.secondaryText,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        if (mode == XvoxSourceMode.ONLINE) {
            val readiness = providerGateway.readiness
            Text(
                text = readiness.message,
                color = colors.mutedText,
                fontSize = 10.5.sp,
                lineHeight = 14.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Text(
                text = "Only official/provider-supported discovery and playback can be added here. " +
                    "No stream extraction, ad bypassing, or entitlement circumvention is used.",
                color = colors.mutedText.copy(alpha = .90f),
                fontSize = 10.sp,
                lineHeight = 13.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun XvoxSourceModePill(
    mode: XvoxSourceMode,
    onModeSelected: (XvoxSourceMode) -> Unit
) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(percent = 50)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(shape)
            .background(colors.cardElevated)
            .border(.8.dp, colors.cardBorder.copy(alpha = .72f), shape)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        XvoxSourceMode.entries.forEach { option ->
            val selected = option == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(CircleShapeCompat)
                    .background(if (selected) colors.primaryAccent else Color.Transparent)
                    .clickable(
                        interactionSource = remember(option) { MutableInteractionSource() },
                        indication = null
                    ) { onModeSelected(option) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option.title,
                    color = if (selected) xvoxSourceOnAccent(colors.primaryAccent) else colors.primaryText,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// A fully rounded rectangle retains the same compact visual language as other Settings pills.
private val CircleShapeCompat = RoundedCornerShape(percent = 50)

private fun xvoxSourceOnAccent(accent: Color): Color {
    val luminance = .2126f * accent.red + .7152f * accent.green + .0722f * accent.blue
    return if (luminance > .62f) Color(0xFF111111) else Color.White
}
