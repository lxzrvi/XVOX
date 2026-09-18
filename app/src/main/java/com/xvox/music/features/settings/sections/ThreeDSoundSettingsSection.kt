package com.xvox.music.features.settings.sections

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.components.SettingsAccordionItem
import com.xvox.music.features.settings.components.SettingsControlsEditor
import com.xvox.music.features.settings.components.SettingsToggle
import com.xvox.music.features.settings.components.XvoxThinLineSlider
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun ThreeDSoundSettingsSection(state: SettingsState, viewModel: SettingsViewModel) {
    val colors = XvoxTheme.colors
    var expandedGroup by remember { mutableStateOf<String?>(null) }

    fun toggle(group: String) {
        expandedGroup = if (expandedGroup == group) null else group
    }

    SettingsControlsEditor(controls = {
        ThreeDSoundPreview(
            widthFraction = state.surroundWidth,
            depthFraction = state.surroundDepth,
            orbitSpeedSec = state.surroundPanSpeed,
            hrtfFraction = state.hrtf,
            balance = state.balance,
            enabled = state.stereoWidening
        )

        Spacer(Modifier.height(8.dp))

        SettingsToggle(
            title = "3D sound",
            subtitle = "Widen, move and place the sound around you",
            checked = state.stereoWidening,
            onChange = viewModel::setStereoWidening
        )

        if (state.stereoWidening) {
            SettingsAccordionItem(
                title = "Spatial Stage",
                expanded = expandedGroup == "Spatial",
                onToggle = { toggle("Spatial") }
            ) {
                EqL("Width · ${(state.surroundWidth * 100).roundToInt()}%")
                XvoxThinLineSlider(state.surroundWidth, viewModel::setSurroundWidth, .05f..1f, defaultValue = .78f)
                Spacer(Modifier.height(8.dp))
                EqL("Depth · ${(state.surroundDepth * 100).roundToInt()}%")
                XvoxThinLineSlider(state.surroundDepth, viewModel::setSurroundDepth, 0f..1f, defaultValue = .65f)
            }

            SettingsAccordionItem(
                title = "Motion & Orbit",
                expanded = expandedGroup == "Motion",
                onToggle = { toggle("Motion") }
            ) {
                val orbitOptions = listOf(0 to "Off", 2 to "2s", 4 to "4s", 6 to "6s", 8 to "8s", 12 to "12s", 15 to "15s")
                EqL("Orbit speed · ${if (state.surroundPanSpeed <= 0) "Off" else "${state.surroundPanSpeed}s per sweep"}")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    orbitOptions.forEach { (sec, label) ->
                        val isSelected = if (sec == 0) state.surroundPanSpeed <= 0 else state.surroundPanSpeed == sec
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) colors.primaryAccent else colors.cardElevated)
                                .then(
                                    if (!isSelected) Modifier.border(0.8.dp, colors.cardBorder, RoundedCornerShape(8.dp))
                                    else Modifier
                                )
                                .xvoxPressScale {
                                    viewModel.setSurroundPanSpeed(sec)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) colors.background else colors.primaryText,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            SettingsAccordionItem(
                title = "Acoustics & Balance",
                expanded = expandedGroup == "Acoustics",
                onToggle = { toggle("Acoustics") }
            ) {
                EqL("HRTF / Spatial binaural · ${(state.hrtf * 100).roundToInt()}%")
                XvoxThinLineSlider(state.hrtf, viewModel::setHrtf, 0f..1f, defaultValue = .6f)
                Spacer(Modifier.height(8.dp))
                val balText = if (state.balance < -0.05f) "Left ${(abs(state.balance) * 100).roundToInt()}%"
                else if (state.balance > 0.05f) "Right ${(state.balance * 100).roundToInt()}%"
                else "Center"
                EqL("Stereo Balance · $balText")
                XvoxThinLineSlider(state.balance, viewModel::setBalance, -1f..1f, defaultValue = 0f)
            }
        } else {
            SettingsAccordionItem(
                title = "Acoustics & Balance",
                expanded = expandedGroup == "Acoustics",
                onToggle = { toggle("Acoustics") }
            ) {
                val balText = if (state.balance < -0.05f) "Left ${(abs(state.balance) * 100).roundToInt()}%"
                else if (state.balance > 0.05f) "Right ${(state.balance * 100).roundToInt()}%"
                else "Center"
                EqL("Stereo Balance · $balText")
                XvoxThinLineSlider(state.balance, viewModel::setBalance, -1f..1f, defaultValue = 0f)
            }
        }
    })
}

@Composable
fun ThreeDSoundPreview(
    widthFraction: Float,
    depthFraction: Float,
    orbitSpeedSec: Int,
    hrtfFraction: Float = 0.6f,
    balance: Float = 0f,
    enabled: Boolean
) {
    val colors = XvoxTheme.colors
    val isOrbiting = enabled && orbitSpeedSec > 0

    var currentAngleRad by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }

    LaunchedEffect(isOrbiting, orbitSpeedSec) {
        if (!isOrbiting || orbitSpeedSec <= 0) return@LaunchedEffect
        var lastTime = 0L
        while (kotlinx.coroutines.isActive) {
            androidx.compose.runtime.withFrameNanos { timeNanos ->
                if (lastTime != 0L) {
                    val dtSec = (timeNanos - lastTime) / 1_000_000_000f
                    val speedRadPerSec = (2f * PI.toFloat()) / orbitSpeedSec.toFloat().coerceAtLeast(1f)
                    currentAngleRad = (currentAngleRad + dtSec * speedRadPerSec) % (2f * PI.toFloat())
                }
                lastTime = timeNanos
            }
        }
    }

    val transition = rememberInfiniteTransition(label = "waveAnim")

    val wavePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    val pulse by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(145.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.card)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize().padding(14.dp)) {
            val cx = size.width / 2f + (balance * size.width * 0.18f)
            val cy = size.height / 2f

            // Width scales the circular ring radius
            val maxR = minOf(size.width, size.height) * 0.44f
            val minR = minOf(size.width, size.height) * 0.22f
            val baseRadius = minR + (maxR - minR) * widthFraction.coerceIn(0.1f, 1f)

            // Crosshair guidelines
            drawLine(
                color = colors.cardBorder.copy(alpha = 0.35f),
                start = Offset(cx - baseRadius - 10.dp.toPx(), cy),
                end = Offset(cx + baseRadius + 10.dp.toPx(), cy),
                strokeWidth = 1f
            )
            drawLine(
                color = colors.cardBorder.copy(alpha = 0.35f),
                start = Offset(cx, cy - baseRadius - 10.dp.toPx()),
                end = Offset(cx, cy + baseRadius + 10.dp.toPx()),
                strokeWidth = 1f
            )

            // Draw circular ring (HRTF makes it wavy with sinusoidal rippling)
            val ringPath = androidx.compose.ui.graphics.Path()
            val segments = 80
            val waveAmp = if (enabled && hrtfFraction > 0.05f) (hrtfFraction * 5.5.dp.toPx()) else 0f

            for (i in 0..segments) {
                val theta = (i.toFloat() / segments.toFloat()) * (2f * PI.toFloat())
                val r = baseRadius + waveAmp * sin(6f * theta + wavePhase)
                val x = cx + r * cos(theta)
                val y = cy + r * sin(theta)
                if (i == 0) ringPath.moveTo(x, y) else ringPath.lineTo(x, y)
            }
            ringPath.close()

            drawPath(
                path = ringPath,
                color = colors.primaryAccent.copy(alpha = if (enabled) 0.38f else 0.12f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = if (enabled && hrtfFraction > 0.05f) null
                    else androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
                )
            )

            // Center listener clean circular dot
            drawCircle(
                color = if (enabled) colors.primaryAccent.copy(alpha = 0.20f * pulse) else Color.Transparent,
                radius = 12.dp.toPx() * pulse,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = if (enabled) colors.primaryAccent else colors.mutedText,
                radius = 6.dp.toPx(),
                center = Offset(cx, cy)
            )

            if (enabled) {
                // Depth controls how close the spinning audio circle is to the center
                val dotOrbitRadius = baseRadius * (0.35f + 0.65f * depthFraction.coerceIn(0.1f, 1f))
                val effectivePhase = if (isOrbiting) currentAngleRad else (-PI / 2).toFloat()

                val dotX = cx + dotOrbitRadius * cos(effectivePhase)
                val dotY = cy + dotOrbitRadius * sin(effectivePhase)

                // Clean moving circular dot with glow aura
                drawCircle(
                    color = colors.primaryAccent.copy(alpha = 0.25f * pulse),
                    radius = 14.dp.toPx() * pulse,
                    center = Offset(dotX, dotY)
                )
                drawCircle(
                    color = colors.primaryAccent,
                    radius = 6.dp.toPx(),
                    center = Offset(dotX, dotY)
                )
            }
        }
    }
}

@Composable
private fun EqL(text: String) {
    Text(
        text,
        color = XvoxTheme.colors.primaryAccent,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
    )
}
