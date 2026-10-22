package com.xvox.music.features.settings.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 2.dp)
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
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 3.dp)
        )
    }
}

/**
 * Full-width Appearance control for the selected typeface.
 *
 * A tap still opens the complete accessible picker. Vertical swipes cycle adjacent choices inside
 * this same pill: moving forward sends the current label below the control while the next label
 * descends from above; moving backward reverses that path.
 */
@Composable
fun XvoxFontPickerButton(
    selectedKey: String,
    onClick: () -> Unit,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    pillHeight: Dp = 54.dp
) {
    val option = XvoxFontOptions.firstOrNull { it.key == selectedKey }
        ?: XvoxFontOptions.first { it.key == "inter" }
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(pillHeight / 2f)
    val swipeThresholdPx = with(LocalDensity.current) { 26.dp.toPx() }
    val selectFont by rememberUpdatedState(onSelect)
    var verticalDrag by remember { mutableFloatStateOf(0f) }
    // +1 is next/forward. Its content enters from above while the former label exits below.
    var contentDirection by remember { mutableIntStateOf(1) }

    fun selectAdjacent(direction: Int): Boolean {
        val currentIndex = XvoxFontOptions.indexOfFirst { it.key == option.key }.coerceAtLeast(0)
        val targetIndex = (currentIndex + direction).coerceIn(0, XvoxFontOptions.lastIndex)
        if (targetIndex == currentIndex) return false
        contentDirection = direction
        selectFont(XvoxFontOptions[targetIndex].key)
        return true
    }

    Row(
        modifier = modifier
            .height(pillHeight)
            .clip(shape)
            .background(colors.cardElevated)
            .border(.8.dp, colors.cardBorder, shape)
            .pointerInput(selectedKey, swipeThresholdPx) {
                detectVerticalDragGestures(
                    onDragStart = { verticalDrag = 0f },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        verticalDrag += dragAmount
                    },
                    onDragEnd = {
                        val direction = when {
                            verticalDrag <= -swipeThresholdPx -> 1
                            verticalDrag >= swipeThresholdPx -> -1
                            else -> 0
                        }
                        verticalDrag = 0f
                        if (direction != 0) selectAdjacent(direction)
                    },
                    onDragCancel = { verticalDrag = 0f }
                )
            }
            .semantics {
                stateDescription = "${option.label} selected. Swipe up for the next font or down for the previous font."
                customActions = listOf(
                    CustomAccessibilityAction("Next font") { selectAdjacent(1) },
                    CustomAccessibilityAction("Previous font") { selectAdjacent(-1) }
                )
            }
            .xvoxPressScale(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.CenterStart
        ) {
            AnimatedContent(
                targetState = option.key,
                transitionSpec = {
                    if (contentDirection > 0) {
                        (
                            slideInVertically(
                                initialOffsetY = { -it },
                                animationSpec = tween(250)
                            ) + fadeIn(tween(180))
                        ).togetherWith(
                            slideOutVertically(
                                targetOffsetY = { it },
                                animationSpec = tween(230)
                            ) + fadeOut(tween(150))
                        )
                    } else {
                        (
                            slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = tween(250)
                            ) + fadeIn(tween(180))
                        ).togetherWith(
                            slideOutVertically(
                                targetOffsetY = { -it },
                                animationSpec = tween(230)
                            ) + fadeOut(tween(150))
                        )
                    }
                },
                label = "fontPillVerticalSelection"
            ) { visibleKey ->
                val visibleOption = XvoxFontOptions.firstOrNull { it.key == visibleKey } ?: option
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        text = visibleOption.label,
                        color = colors.primaryText,
                        fontFamily = visibleOption.fontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (visibleOption.key == "system") {
                            "Android default typeface"
                        } else {
                            "Aa The quick brown fox"
                        },
                        color = colors.secondaryText,
                        fontFamily = visibleOption.fontFamily,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
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
