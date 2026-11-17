package com.xvox.music.core.design.kit

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ermohdamaan.justforpixel.justforpixelexpressivelab.core.components.cards.ExpressiveCard
import com.ermohdamaan.justforpixel.justforpixelexpressivelab.core.components.icons.PlayingEqIcon
import com.ermohdamaan.justforpixel.justforpixelexpressivelab.core.components.progress.ExpressiveShapeMorphLoadingIndicator
import com.ermohdamaan.justforpixel.justforpixelexpressivelab.core.components.segmented.ToggleSegmentRow
import com.ermohdamaan.justforpixel.justforpixelexpressivelab.core.components.sliders.ExpressiveWavySlider
import com.ermohdamaan.justforpixel.justforpixelexpressivelab.core.theme.TactileMotionTokens
import com.xvox.music.core.design.theme.XvoxContinuousShape
import com.xvox.music.core.design.theme.XvoxTheme
import io.github.neoruaa.glasmio.glass.GlassEmphasis
import io.github.neoruaa.glasmio.glass.GlassSurface
import io.github.neoruaa.glasmio.glass.render.backdrops.rememberLayerBackdrop
import io.github.neoruaa.glasmio.ui.GlasmioButton
import io.github.neoruaa.glasmio.ui.GlasmioButtonSize
import io.github.neoruaa.glasmio.ui.GlasmioGroupedList
import io.github.neoruaa.glasmio.ui.GlasmioListRow
import io.github.neoruaa.glasmio.ui.GlasmioSectionTitle
import io.github.neoruaa.glasmio.ui.GlasmioSegmentedControl
import io.github.neoruaa.glasmio.ui.GlasmioSlider
import io.github.neoruaa.glasmio.ui.GlasmioSwitch

/**
 * Shared kit components. Each one is a single call site that resolves to either the iOS liquid
 * glass implementation (Glasmio) or the Material 3 Expressive implementation (ExpressiveLab),
 * so a screen never needs to know which engine is active.
 */

/** Continuous-corner card. iOS renders real liquid glass, Material renders an Expressive card. */
@Composable
fun XvoxKitCard(
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = XvoxTheme.colors
    val cardShape = shape ?: XvoxContinuousShape(24.dp)
    if (xvoxUsesGlass()) {
        val backdrop = LocalXvoxGlassBackdrop.current ?: rememberLayerBackdrop()
        GlassSurface(
            modifier = modifier,
            backdrop = backdrop,
            shape = cardShape,
            emphasis = GlassEmphasis.Regular,
            onClick = onClick,
            content = content
        )
    } else {
        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (pressed) TactileMotionTokens.PRESS_SCALE_CARD else 1f,
            animationSpec = TactileMotionTokens.bouncySpring(),
            label = "kitCardPress"
        )
        ExpressiveCard(
            onClick = { onClick?.invoke() },
            modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
            containerColor = colors.card,
            cornerRadius = 24.dp,
            elevation = 0.dp
        ) {
            content()
        }
    }
}

/** Grouped container: an iOS inset-grouped table, or an Expressive tonal block. */
@Composable
fun XvoxKitGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    if (xvoxUsesGlass()) {
        GlasmioGroupedList(modifier = modifier, content = content)
    } else {
        val colors = XvoxTheme.colors
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(XvoxContinuousShape(20.dp))
                .background(colors.card)
                .border(.6.dp, colors.cardBorder, XvoxContinuousShape(20.dp))
                .padding(vertical = 6.dp),
            content = content
        )
    }
}

@Composable
fun XvoxKitSectionTitle(title: String, modifier: Modifier = Modifier) {
    if (xvoxUsesGlass()) {
        GlasmioSectionTitle(title = title, modifier = modifier)
    } else {
        val colors = XvoxTheme.colors
        Text(
            text = title.uppercase(),
            color = colors.mutedText,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.4.sp,
            modifier = modifier.padding(start = 20.dp, top = 14.dp, bottom = 6.dp)
        )
    }
}

/** One row. iOS keeps hairlines and disclosure chevrons, Material keeps tonal bouncy rows. */
@Composable
fun XvoxKitRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    detail: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    showTopSeparator: Boolean = true,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    if (xvoxUsesGlass()) {
        GlasmioListRow(
            title = title,
            modifier = modifier,
            subtitle = subtitle,
            detail = detail,
            leading = leading,
            onClick = onClick,
            trailing = trailing,
            showTopSeparator = showTopSeparator,
            enabled = enabled
        )
    } else {
        val colors = XvoxTheme.colors
        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (pressed) .985f else 1f,
            animationSpec = TactileMotionTokens.bouncySpring(),
            label = "kitRowPress"
        )
        Row(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .then(
                    if (onClick != null) {
                        Modifier.selectable(
                            selected = false,
                            interactionSource = interaction,
                            indication = androidx.compose.material3.ripple(),
                            enabled = enabled,
                            onClick = onClick
                        )
                    } else Modifier
                )
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leading != null) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(XvoxContinuousShape(12.dp))
                        .background(colors.accentSoft),
                    contentAlignment = Alignment.Center
                ) { leading() }
                Spacer(Modifier.width(14.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    color = colors.primaryText,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        color = colors.mutedText,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (detail != null) {
                Text(text = detail, color = colors.mutedText, style = MaterialTheme.typography.bodyMedium)
            }
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                trailing()
            }
        }
    }
}

@Composable
fun XvoxKitSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    if (xvoxUsesGlass()) {
        val backdrop = LocalXvoxGlassBackdrop.current ?: rememberLayerBackdrop()
        GlasmioSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled,
            backdrop = backdrop
        )
    } else {
        val colors = XvoxTheme.colors
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = colors.background,
                checkedTrackColor = colors.primaryAccent,
                uncheckedThumbColor = colors.mutedText,
                uncheckedTrackColor = colors.cardElevated
            )
        )
    }
}

@Composable
fun XvoxKitSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null
) {
    if (xvoxUsesGlass()) {
        val backdrop = LocalXvoxGlassBackdrop.current ?: rememberLayerBackdrop()
        GlasmioSlider(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            valueRange = valueRange,
            enabled = enabled,
            backdrop = backdrop,
            onValueChangeFinished = onValueChangeFinished
        )
    } else {
        val colors = XvoxTheme.colors
        ExpressiveWavySlider(
            value = { value },
            onValueChange = onValueChange,
            modifier = modifier,
            enabled = enabled,
            valueRange = valueRange,
            onValueChangeFinished = onValueChangeFinished,
            activeTrackColor = colors.primaryAccent,
            inactiveTrackColor = colors.cardElevated,
            thumbColor = colors.primaryAccent
        )
    }
}

/** iOS capsule segmented control / Material Expressive toggle segments. */
@Composable
fun XvoxKitSegmented(
    options: List<Pair<String, String>>,
    selectedKey: String?,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit
) {
    if (xvoxUsesGlass()) {
        val backdrop = LocalXvoxGlassBackdrop.current ?: rememberLayerBackdrop()
        GlasmioSegmentedControl(
            items = options.map { it.first to it.second },
            selected = selectedKey,
            onSelected = onSelect,
            modifier = modifier,
            backdrop = backdrop,
            height = 34.dp
        )
    } else {
        val colors = XvoxTheme.colors
        val index = options.indexOfFirst { it.first == selectedKey }.coerceAtLeast(0)
        ToggleSegmentRow(
            options = options.map { it.second },
            selectedIndex = index,
            onOptionSelected = { i -> options.getOrNull(i)?.let { onSelect(it.first) } },
            modifier = modifier,
            containerColor = colors.cardElevated
        )
    }
}

@Composable
fun XvoxKitButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    prominent: Boolean = false,
    enabled: Boolean = true
) {
    if (xvoxUsesGlass()) {
        val backdrop = LocalXvoxGlassBackdrop.current ?: rememberLayerBackdrop()
        GlasmioButton(
            onClick = onClick,
            modifier = modifier,
            backdrop = backdrop,
            enabled = enabled,
            emphasis = if (prominent) GlassEmphasis.Prominent else GlassEmphasis.Regular,
            size = GlasmioButtonSize.Medium
        ) {
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    } else {
        val colors = XvoxTheme.colors
        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (pressed) TactileMotionTokens.PRESS_SCALE_STANDARD else 1f,
            animationSpec = TactileMotionTokens.bouncySpring(),
            label = "kitButtonPress"
        )
        Box(
            modifier = modifier
                .height(42.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(CircleShape)
                .background(if (prominent) colors.primaryAccent else colors.cardElevated)
                .selectable(
                    selected = false,
                    interactionSource = interaction,
                    indication = androidx.compose.material3.ripple(),
                    enabled = enabled,
                    onClick = onClick
                )
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (prominent) colors.background else colors.primaryText,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
fun XvoxKitIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    if (xvoxUsesGlass()) {
        val backdrop = LocalXvoxGlassBackdrop.current ?: rememberLayerBackdrop()
        io.github.neoruaa.glasmio.ui.GlasmioIconButton(
            onClick = onClick,
            modifier = modifier,
            backdrop = backdrop,
            enabled = enabled,
            size = 38.dp,
            content = content
        )
    } else {
        val colors = XvoxTheme.colors
        Box(
            modifier = modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(colors.cardElevated)
                .selectable(
                    selected = false,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = androidx.compose.material3.ripple(),
                    enabled = enabled,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

/** Loading spinner: Expressive shape-morph in Material, wavy iOS circle in glass mode. */
@Composable
fun XvoxKitLoading(modifier: Modifier = Modifier, size: Dp = 34.dp) {
    val colors = XvoxTheme.colors
    if (xvoxUsesGlass()) {
        androidx.compose.material3.CircularProgressIndicator(
            modifier = Modifier.size(size),
            color = colors.primaryAccent,
            strokeWidth = 2.5.dp
        )
    } else {
        ExpressiveShapeMorphLoadingIndicator(
            modifier = modifier,
            size = size,
            color = colors.primaryAccent,
            innerColor = colors.background
        )
    }
}

/** The playing-equaliser mark shared by rows and the player. */
@Composable
fun XvoxKitEq(modifier: Modifier = Modifier, isPlaying: Boolean = true, color: Color? = null) {
    PlayingEqIcon(
        modifier = modifier,
        color = color ?: XvoxTheme.colors.primaryAccent,
        isPlaying = isPlaying,
        bars = 4
    )
}

/** Thin determinate progress used by kit surfaces (wavy in Material, glass track in iOS). */
@Composable
fun XvoxKitProgressLine(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    val clamped = progress.coerceIn(0f, 1f)
    if (xvoxUsesGlass()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(colors.progressTrack)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clamped)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(colors.progressActive)
            )
        }
    } else {
        // Expressive progress: a squared continuous-corner track with a tonal indicator.
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(XvoxContinuousShape(3.dp))
                .background(colors.cardElevated)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clamped)
                    .height(6.dp)
                    .clip(XvoxContinuousShape(3.dp))
                    .background(colors.primaryAccent)
            )
        }
    }
}

/** Row-level trailing label coloured by the active palette. */
@Composable
fun RowScope.XvoxKitTrailingLabel(text: String) {
    Text(
        text = text,
        color = XvoxTheme.colors.mutedText,
        style = MaterialTheme.typography.bodyMedium
    )
}
