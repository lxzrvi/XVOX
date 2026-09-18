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
    val transition = rememberInfiniteTransition(label = "orbitAnim")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (!isOrbiting) 600000 else (orbitSpeedSec.coerceIn(1, 20) * 1000),
                easing = LinearEasing
            ),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "phase"
    )

    val pulse by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(138.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.card)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize().padding(14.dp)) {
            val cx = size.width / 2f + (balance * size.width * 0.20f)
            val cy = size.height / 2f
            val radiusX = (size.width * 0.40f * widthFraction.coerceIn(0.15f, 1f))
            val radiusY = (size.height * 0.38f * depthFraction.coerceIn(0.15f, 1f))

            // Crosshair guidelines (Front / Back / Left / Right)
            drawLine(
                color = colors.cardBorder.copy(alpha = 0.4f),
                start = Offset(cx - radiusX - 8.dp.toPx(), cy),
                end = Offset(cx + radiusX + 8.dp.toPx(), cy),
                strokeWidth = 1f
            )
            drawLine(
                color = colors.cardBorder.copy(alpha = 0.4f),
                start = Offset(cx, cy - radiusY - 8.dp.toPx()),
                end = Offset(cx, cy + radiusY + 8.dp.toPx()),
                strokeWidth = 1f
            )

            // Outer and inner orbit paths
            drawOval(
                color = colors.primaryAccent.copy(alpha = if (enabled) 0.30f else 0.10f),
                topLeft = Offset(cx - radiusX, cy - radiusY),
                size = androidx.compose.ui.geometry.Size(radiusX * 2, radiusY * 2),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                )
            )

            // Subtle inner distance ring
            drawOval(
                color = colors.primaryAccent.copy(alpha = if (enabled) 0.12f else 0.05f),
                topLeft = Offset(cx - radiusX * 0.5f, cy - radiusY * 0.5f),
                size = androidx.compose.ui.geometry.Size(radiusX, radiusY),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
            )

            // Listener Headphone Center (Head + Earcups + Directional Nose)
            val headRadius = 10.dp.toPx()
            drawCircle(
                color = if (enabled) colors.primaryAccent.copy(alpha = 0.25f * pulse) else Color.Transparent,
                radius = headRadius * 1.5f,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = if (enabled) colors.primaryAccent else colors.mutedText,
                radius = headRadius,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = colors.background,
                radius = headRadius * 0.65f,
                center = Offset(cx, cy)
            )

            // Headphone Earcups (Left & Right)
            val earcupW = 4.dp.toPx()
            val earcupH = 9.dp.toPx()
            drawRoundRect(
                color = if (enabled) colors.primaryAccent else colors.mutedText,
                topLeft = Offset(cx - headRadius - earcupW, cy - earcupH / 2f),
                size = androidx.compose.ui.geometry.Size(earcupW, earcupH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
            )
            drawRoundRect(
                color = if (enabled) colors.primaryAccent else colors.mutedText,
                topLeft = Offset(cx + headRadius, cy - earcupH / 2f),
                size = androidx.compose.ui.geometry.Size(earcupW, earcupH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
            )

            // Direction arrow pointing forward (top)
            val arrowY = cy - headRadius - 4.dp.toPx()
            drawCircle(color = colors.primaryAccent, radius = 2.dp.toPx(), center = Offset(cx, arrowY))

            if (enabled) {
                val effectivePhase = if (isOrbiting) phase else 0f

                // Orbiting Left Sound Source (L)
                val lx = cx + radiusX * cos(effectivePhase)
                val ly = cy + radiusY * sin(effectivePhase)

                // Glowing outer aura for Left Node
                drawCircle(
                    color = colors.primaryAccent.copy(alpha = 0.35f * pulse),
                    radius = 12.dp.toPx() * pulse,
                    center = Offset(lx, ly)
                )
                drawCircle(
                    color = colors.primaryAccent,
                    radius = 7.5.dp.toPx(),
                    center = Offset(lx, ly)
                )

                // Orbiting Right Sound Source (R, 180 deg opposite)
                val rx = cx + radiusX * cos(effectivePhase + PI.toFloat())
                val ry = cy + radiusY * sin(effectivePhase + PI.toFloat())

                // Glowing outer aura for Right Node
                drawCircle(
                    color = colors.primaryAccent.copy(alpha = 0.28f * pulse),
                    radius = 11.dp.toPx() * pulse,
                    center = Offset(rx, ry)
                )
                drawCircle(
                    color = colors.primaryAccent.copy(alpha = 0.90f),
                    radius = 6.5.dp.toPx(),
                    center = Offset(rx, ry)
                )

                // HRTF Binaural wave rings pulsing towards listener
                if (hrtfFraction > 0.05f) {
                    val ringAlpha = (hrtfFraction * 0.45f).coerceIn(0.08f, 0.55f)
                    drawCircle(
                        color = colors.primaryAccent.copy(alpha = ringAlpha),
                        radius = (16.dp * hrtfFraction * pulse).toPx(),
                        center = Offset(lx, ly),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                    )
                    drawCircle(
                        color = colors.primaryAccent.copy(alpha = ringAlpha),
                        radius = (16.dp * hrtfFraction * pulse).toPx(),
                        center = Offset(rx, ry),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                    )

                    // Secondary larger wave
                    drawCircle(
                        color = colors.primaryAccent.copy(alpha = ringAlpha * 0.5f),
                        radius = (26.dp * hrtfFraction * pulse).toPx(),
                        center = Offset(lx, ly),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                    )
                    drawCircle(
                        color = colors.primaryAccent.copy(alpha = ringAlpha * 0.5f),
                        radius = (26.dp * hrtfFraction * pulse).toPx(),
                        center = Offset(rx, ry),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                    )
                }
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
