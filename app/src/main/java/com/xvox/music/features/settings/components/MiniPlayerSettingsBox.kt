package com.xvox.music.features.settings.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.chrome.XvoxChromeStyle
import com.xvox.music.core.ui.miniplayer.XvoxPlayerTransitionMotion
import com.xvox.music.core.ui.overlay.xvoxBoxScroll
import kotlin.math.roundToInt

/**
 * Live body for the Mini Player / Navbar editor. Placement, radius, width, height, and image
 * choices retain their existing saved values but are not edited in this compact sheet.
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
        verticalArrangement = Arrangement.spacedBy(15.dp)
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

        // 0% is solid and 100% is clear. The continuous slider includes its visible default mark.
        ChromeTransparencySlider(
            label = "Mini Player transparency",
            value = 1f - chrome.miniBgAlpha.coerceIn(0f, 1f),
            default = .06f,
            contentDescription = "Mini Player transparency",
            onChange = { transparency ->
                onChromeChange(chrome.copy(miniBgAlpha = (1f - transparency).coerceIn(0f, 1f)))
            }
        )

        MiniPlayerTransitionExperimentEditor(
            chrome = chrome,
            onChromeChange = onChromeChange
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
    }
}

@Composable
private fun MiniPlayerTransitionExperimentEditor(
    chrome: XvoxChromeStyle,
    onChromeChange: (XvoxChromeStyle) -> Unit
) {
    val colors = XvoxTheme.colors
    val selectedStyle = XvoxPlayerTransitionMotion.normalizedStyle(chrome.miniPlayerTransitionStyle)

    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text("Animation", color = colors.secondaryText, fontSize = 11.sp)
        SettingsChoiceRow(
            options = listOf(
                XvoxPlayerTransitionMotion.DefaultStyle to "Default",
                XvoxPlayerTransitionMotion.ScaleStyle to "Scale"
            ),
            selected = selectedStyle,
            onSelect = { style ->
                // Scale remains deliberately bounded: it can settle into place but never grows
                // beyond its final card bounds.
                onChromeChange(
                    chrome.copy(
                        miniPlayerTransitionDuration = XvoxPlayerTransitionMotion.Duration,
                        miniPlayerTransitionStyle = XvoxPlayerTransitionMotion.normalizedStyle(style)
                    )
                )
            }
        )
        MiniPlayerTransitionPreview(selectedStyle)
    }
}

/** A wordless live loop makes the chosen handoff legible without publishing a timing value. */
@Composable
private fun MiniPlayerTransitionPreview(style: String) {
    val colors = XvoxTheme.colors
    val density = LocalDensity.current
    val cycle = rememberInfiniteTransition(label = "miniPlayerTransitionPreview")
    val phase by cycle.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "miniPlayerTransitionPreviewPhase"
    )
    // The preview deliberately holds the incoming card below the preview's lower edge until the
    // Mini Player has completed its departure, matching the actual motion contract.
    val miniExit = (phase / .42f).coerceIn(0f, 1f)
    val nowEnter = ((phase - .42f) / .46f).coerceIn(0f, 1f)
    val resetFade = if (phase > .92f) ((1f - phase) / .08f).coerceIn(0f, 1f) else 1f
    val nowScale = if (style == XvoxPlayerTransitionMotion.ScaleStyle) {
        .94f + (.06f * nowEnter)
    } else {
        1f
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.cardElevated.copy(alpha = .72f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Tiny exiting Mini Player. It leaves downwards first, with no simultaneous entry card.
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(.72f)
                .height(27.dp)
                .graphicsLayer {
                    translationY = with(density) { (miniExit * 44f).dp.toPx() }
                    alpha = (1f - miniExit * .18f) * resetFade
                }
                .clip(RoundedCornerShape(10.dp))
                .background(colors.card.copy(alpha = .92f))
                .padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                Modifier
                    .height(15.dp)
                    .fillMaxWidth(.13f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.primaryAccent.copy(alpha = .72f))
            )
            Box(
                Modifier
                    .height(5.dp)
                    .weight(1f)
                    .clip(RoundedCornerShape(3.dp))
                    .background(colors.secondaryText.copy(alpha = .42f))
            )
        }

        // Incoming Now Playing card starts fully below the frame, then rises only after exit.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(.88f)
                .height(56.dp)
                .graphicsLayer {
                    translationY = with(density) { ((1f - nowEnter) * 64f).dp.toPx() }
                    scaleX = nowScale
                    scaleY = nowScale
                    alpha = nowEnter * resetFade
                }
                .clip(RoundedCornerShape(13.dp))
                .background(colors.card.copy(alpha = .78f))
                .padding(9.dp)
        ) {
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth(.72f)
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.primaryText.copy(alpha = .72f))
            )
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 14.dp)
                    .fillMaxWidth(.48f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(colors.secondaryText.copy(alpha = .48f))
            )
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.primaryAccent.copy(alpha = .8f))
            )
        }
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
