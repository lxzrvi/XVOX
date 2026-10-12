package com.xvox.music.features.settings.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.chrome.XvoxChromeStyle
import com.xvox.music.core.ui.miniplayer.XvoxPlayerTransitionMotion
import com.xvox.music.core.ui.overlay.xvoxBoxScroll
import kotlin.math.roundToInt

/**
 * Live Mini Player/Navbar editor. The Experimental group intentionally exposes inspectable motion
 * values so they can be tuned in the product before any later decision to make a preset permanent.
 */
@Composable
fun MiniPlayerSettingsBoxContent(
    chrome: XvoxChromeStyle,
    onChromeChange: (XvoxChromeStyle) -> Unit
) {
    val colors = XvoxTheme.colors
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .xvoxBoxScroll(scrollState)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Mini Player", color = colors.primaryAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Cover style", color = colors.secondaryText, fontSize = 11.sp)
            SettingsChoiceRow(
                options = listOf("default" to "Standard Box", "full" to "Full Cover Artwork"),
                selected = if (chrome.miniCoverStyle == "full") "full" else "default",
                onSelect = { value -> onChromeChange(chrome.copy(miniCoverStyle = value)) }
            )
        }

        ChromeTransparencySlider(
            label = "Mini Player transparency",
            value = 1f - chrome.miniBgAlpha.coerceIn(0f, 1f),
            default = .06f,
            contentDescription = "Mini Player transparency",
            onChange = { transparency ->
                onChromeChange(chrome.copy(miniBgAlpha = (1f - transparency).coerceIn(0f, 1f)))
            }
        )

        Text("Navigation Bar", color = colors.primaryAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        ChromeTransparencySlider(
            label = "Navbar transparency",
            value = 1f - chrome.navBgAlpha.coerceIn(0f, 1f),
            default = .06f,
            contentDescription = "Navbar transparency",
            onChange = { transparency ->
                onChromeChange(chrome.copy(navBgAlpha = (1f - transparency).coerceIn(0f, 1f)))
            }
        )

        ExperimentalMiniPlayerControls(chrome = chrome, onChromeChange = onChromeChange)
    }
}

@Composable
private fun ExperimentalMiniPlayerControls(
    chrome: XvoxChromeStyle,
    onChromeChange: (XvoxChromeStyle) -> Unit
) {
    val colors = XvoxTheme.colors
    val selectedStyle = XvoxPlayerTransitionMotion.normalizedStyle(chrome.miniPlayerTransitionStyle)
    val shape = RoundedCornerShape(14.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.cardElevated.copy(alpha = .56f))
            .border(.8.dp, colors.cardBorder.copy(alpha = .72f), shape)
            .padding(11.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Experimental", color = colors.primaryAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(
                "Live motion and chrome values — tune freely before choosing a final preset.",
                color = colors.secondaryText,
                fontSize = 10.5.sp,
                lineHeight = 14.sp
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Mini Player animation", color = colors.secondaryText, fontSize = 11.sp)
            SettingsChoiceRow(
                options = listOf(
                    XvoxPlayerTransitionMotion.DefaultStyle to "Default",
                    XvoxPlayerTransitionMotion.ScaleStyle to "Scale"
                ),
                selected = selectedStyle,
                onSelect = { style ->
                    onChromeChange(
                        chrome.copy(
                            miniPlayerTransitionStyle = XvoxPlayerTransitionMotion.normalizedStyle(style)
                        )
                    )
                }
            )
        }

        ExperimentalValueSlider(
            label = "Mini Player below edge",
            value = chrome.miniExitOvershootDp,
            range = 0f..180f,
            default = 12f,
            valueText = { "${it.roundToInt()} dp" },
            contentDescription = "Mini Player below edge",
            onChange = { onChromeChange(chrome.copy(miniExitOvershootDp = it)) }
        )
        ExperimentalValueSlider(
            label = "Now Playing below edge",
            value = chrome.nowPlayingExitOvershootDp,
            range = 0f..220f,
            default = 0f,
            valueText = { "${it.roundToInt()} dp" },
            contentDescription = "Now Playing below edge",
            onChange = { onChromeChange(chrome.copy(nowPlayingExitOvershootDp = it)) }
        )
        ExperimentalValueSlider(
            label = "Handoff interval",
            value = chrome.miniToNowPlayingDelayMillis.toFloat(),
            range = 0f..1200f,
            default = 0f,
            valueText = { "${it.roundToInt()} ms" },
            contentDescription = "Mini Player to Now Playing interval",
            onChange = { onChromeChange(chrome.copy(miniToNowPlayingDelayMillis = it.roundToInt())) }
        )
        ExperimentalValueSlider(
            label = "Mini Player speed",
            value = chrome.miniMotionDuration.toFloat(),
            range = 120f..900f,
            default = XvoxPlayerTransitionMotion.HandoffDuration.toFloat(),
            valueText = { "${it.roundToInt()} ms" },
            contentDescription = "Mini Player animation speed",
            onChange = { onChromeChange(chrome.copy(miniMotionDuration = it.roundToInt())) }
        )
        ExperimentalValueSlider(
            label = "Now Playing speed",
            value = chrome.nowPlayingMotionDuration.toFloat(),
            range = 120f..900f,
            default = XvoxPlayerTransitionMotion.NowPlayingDuration.toFloat(),
            valueText = { "${it.roundToInt()} ms" },
            contentDescription = "Now Playing animation speed",
            onChange = { onChromeChange(chrome.copy(nowPlayingMotionDuration = it.roundToInt())) }
        )

        MotionCurveGraph(
            miniEase = chrome.miniMotionEase,
            nowPlayingEase = chrome.nowPlayingMotionEase
        )
        ExperimentalValueSlider(
            label = "Mini Player ease",
            value = chrome.miniMotionEase,
            range = 0f..1f,
            default = .55f,
            valueText = { "${(it * 100f).roundToInt()}%" },
            contentDescription = "Mini Player animation ease",
            onChange = { onChromeChange(chrome.copy(miniMotionEase = it)) }
        )
        ExperimentalValueSlider(
            label = "Now Playing ease",
            value = chrome.nowPlayingMotionEase,
            range = 0f..1f,
            default = .55f,
            valueText = { "${(it * 100f).roundToInt()}%" },
            contentDescription = "Now Playing animation ease",
            onChange = { onChromeChange(chrome.copy(nowPlayingMotionEase = it)) }
        )

        Text("Borders", color = colors.primaryAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        ExperimentalValueSlider(
            label = "Mini Player border thickness",
            value = chrome.miniBorderWidth,
            range = 0f..4f,
            default = .8f,
            valueText = { "${"%.1f".format(it)} dp" },
            contentDescription = "Mini Player border thickness",
            onChange = { onChromeChange(chrome.copy(miniBorderWidth = it)) }
        )
        ColorPickerRow(
            label = "Mini Player border color",
            hex = chrome.miniBorder,
            subtitle = "Auto uses the active theme border",
            onColorChange = { onChromeChange(chrome.copy(miniBorder = it)) }
        )
        ExperimentalValueSlider(
            label = "Navbar border thickness",
            value = chrome.navBorderWidth,
            range = 0f..4f,
            default = .8f,
            valueText = { "${"%.1f".format(it)} dp" },
            contentDescription = "Navbar border thickness",
            onChange = { onChromeChange(chrome.copy(navBorderWidth = it)) }
        )
        ColorPickerRow(
            label = "Navbar border color",
            hex = chrome.navBorder,
            subtitle = "Auto uses the active theme border",
            onColorChange = { onChromeChange(chrome.copy(navBorder = it)) }
        )
        ExperimentalValueSlider(
            label = "Pill border thickness",
            value = chrome.pillBorderWidth,
            range = 0f..4f,
            default = .8f,
            valueText = { "${"%.1f".format(it)} dp" },
            contentDescription = "Navbar pill border thickness",
            onChange = { onChromeChange(chrome.copy(pillBorderWidth = it)) }
        )
        ColorPickerRow(
            label = "Pill border color",
            hex = chrome.pillBorder,
            subtitle = "Auto follows the navbar border",
            onColorChange = { onChromeChange(chrome.copy(pillBorder = it)) }
        )
    }
}

@Composable
private fun MotionCurveGraph(miniEase: Float, nowPlayingEase: Float) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(10.dp)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Motion curve", color = colors.secondaryText, fontSize = 11.sp, modifier = Modifier.weight(1f))
            Box(Modifier.size(7.dp).clip(RoundedCornerShape(4.dp)).background(colors.primaryAccent))
            Spacer(Modifier.width(4.dp))
            Text("Mini", color = colors.mutedText, fontSize = 9.sp)
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(7.dp).clip(RoundedCornerShape(4.dp)).background(colors.secondaryText))
            Spacer(Modifier.width(4.dp))
            Text("Now Playing", color = colors.mutedText, fontSize = 9.sp)
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(74.dp)
                .clip(shape)
                .background(colors.background.copy(alpha = .38f))
        ) {
            val inset = 9.dp.toPx()
            val width = (size.width - inset * 2f).coerceAtLeast(1f)
            val height = (size.height - inset * 2f).coerceAtLeast(1f)
            drawLine(colors.cardBorder.copy(alpha = .7f), Offset(inset, inset + height), Offset(inset + width, inset + height), 1.dp.toPx())
            drawLine(colors.cardBorder.copy(alpha = .7f), Offset(inset, inset), Offset(inset, inset + height), 1.dp.toPx())
            fun curve(ease: Float, color: androidx.compose.ui.graphics.Color) {
                val path = Path()
                repeat(41) { index ->
                    val fraction = index / 40f
                    val x = inset + width * fraction
                    val y = inset + height * (1f - XvoxPlayerTransitionMotion.easingFor(ease).transform(fraction))
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, color = color, style = Stroke(width = 2.dp.toPx()))
            }
            curve(miniEase, colors.primaryAccent)
            curve(nowPlayingEase, colors.secondaryText)
        }
    }
}

@Composable
private fun ExperimentalValueSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    default: Float,
    valueText: (Float) -> String,
    contentDescription: String,
    onChange: (Float) -> Unit
) {
    val colors = XvoxTheme.colors
    val current = value.coerceIn(range)
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = colors.secondaryText, fontSize = 11.sp)
            Text(valueText(current), color = colors.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        XvoxContinuousSlider(
            value = current,
            onValueChange = { onChange(it.coerceIn(range)) },
            valueRange = range,
            defaultValue = default,
            contentDescription = contentDescription
        )
    }
}

@Composable
private fun ChromeTransparencySlider(
    label: String,
    value: Float,
    default: Float,
    contentDescription: String,
    onChange: (Float) -> Unit
) {
    val colors = XvoxTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = colors.secondaryText, fontSize = 11.sp)
            Text(
                "${(value.coerceIn(0f, 1f) * 100).roundToInt()}%",
                color = colors.primaryAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        XvoxContinuousSlider(
            value = value.coerceIn(0f, 1f),
            onValueChange = { onChange(it.coerceIn(0f, 1f)) },
            valueRange = 0f..1f,
            defaultValue = default,
            contentDescription = contentDescription
        )
    }
}
