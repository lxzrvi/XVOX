package com.xvox.music.features.settings.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.chrome.parseHexColor
import com.xvox.music.core.ui.effects.xvoxPressScale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

fun colorToHsv(color: Color): FloatArray {
    val r = color.red; val g = color.green; val b = color.blue
    val max = maxOf(r, g, b); val min = minOf(r, g, b); val delta = max - min
    var h = 0f
    if (delta > 0f) {
        h = when (max) {
            r -> (((g - b) / delta) % 6f)
            g -> ((b - r) / delta + 2f)
            else -> ((r - g) / delta + 4f)
        } * 60f
        if (h < 0f) h += 360f
    }
    val s = if (max == 0f) 0f else delta / max
    return floatArrayOf(h, s, max)
}

fun hsvColor(h: Float, s: Float, v: Float): Color {
    val hue = ((h % 360f) + 360f) % 360f
    val sat = s.coerceIn(0f, 1f)
    val value = v.coerceIn(0f, 1f)
    val c = value * sat
    val x = c * (1f - abs((hue / 60f) % 2f - 1f))
    val m = value - c
    val (r, g, b) = when (hue.toInt()) {
        in 0 until 60 -> Triple(c, x, 0f)
        in 60 until 120 -> Triple(x, c, 0f)
        in 120 until 180 -> Triple(0f, c, x)
        in 180 until 240 -> Triple(0f, x, c)
        in 240 until 300 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(r + m, g + m, b + m)
}

private fun hexToHsv(hex: String): FloatArray? = parseHexColor(hex)?.let(::colorToHsv)

private fun hsvToHex(h: Float, s: Float, v: Float): String =
    "#%06X".format(hsvColor(h, s, v).toArgb() and 0xFFFFFF)

/** A real HSV wheel: hue around the circle, saturation by radius, brightness by the separate rail. */
private fun buildWheelBitmap(size: Int, value: Float): ImageBitmap {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val center = size / 2f
    val radius = center
    val pixels = IntArray(size * size)
    for (y in 0 until size) {
        for (x in 0 until size) {
            val dx = x - center
            val dy = y - center
            val distance = sqrt(dx * dx + dy * dy)
            pixels[y * size + x] = if (distance > radius) {
                0
            } else {
                val edgeAlpha = if (distance > radius - 1.2f) {
                    ((radius - distance) / 1.2f).coerceIn(0f, 1f)
                } else {
                    1f
                }
                var angle = atan2(dy, dx) * 180f / PI.toFloat() + 90f
                if (angle < 0f) angle += 360f
                hsvColor(angle, (distance / radius).coerceIn(0f, 1f), value)
                    .copy(alpha = edgeAlpha)
                    .toArgb()
            }
        }
    }
    bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
    return bitmap.asImageBitmap()
}

/** Ten fixed starting swatches; the eleventh slot below is always the live custom colour. */
private val BaseAccentSwatches = listOf(
    "#F01E2C", // XVOX Red
    "#FF9500",
    "#FFCC00",
    "#34C759",
    "#00C7BE",
    "#007AFF",
    "#5856D6",
    "#AF52DE",
    "#FF2D92",
    "#8E8E93"
)

/**
 * A direct HSV colour editor. [showPreview] retains the old reusable settings-row affordance for
 * non-accent uses. Accent Custom passes false, so the wheel is present immediately—there is no
 * collapsed preview box between the user and the picker.
 */
@Composable
fun ColorPickerRow(
    label: String,
    hex: String,
    onColorChange: (String) -> Unit,
    subtitle: String? = null,
    alpha: Float? = null,
    onAlphaChange: ((Float) -> Unit)? = null,
    showPreview: Boolean = true,
    initiallyExpanded: Boolean = false
) {
    val colors = XvoxTheme.colors
    var expanded by remember(showPreview, initiallyExpanded) {
        mutableStateOf(!showPreview || initiallyExpanded)
    }
    val seed = hexToHsv(hex) ?: hexToHsv("#F01E2C")!!
    var wheelH by remember { mutableFloatStateOf(seed[0]) }
    var wheelS by remember { mutableFloatStateOf(seed[1]) }
    var wheelV by remember { mutableFloatStateOf(seed[2]) }
    var hexInput by remember { mutableStateOf(if (hex.isBlank()) "" else hex.uppercase()) }

    LaunchedEffect(hex) {
        hexToHsv(hex)?.let { hsv ->
            wheelH = hsv[0]
            wheelS = hsv[1]
            wheelV = hsv[2]
            hexInput = hex.uppercase()
        }
    }

    fun publish(h: Float, s: Float, v: Float) {
        wheelH = h.coerceIn(0f, 360f)
        wheelS = s.coerceIn(0f, 1f)
        wheelV = v.coerceIn(0f, 1f)
        val generated = hsvToHex(wheelH, wheelS, wheelV)
        hexInput = generated
        // No debounce: both wheel drag and brightness movement update accent live.
        onColorChange(generated)
    }

    val normalizedInput = hexInput.trim().let { raw -> if (raw.startsWith("#")) raw else "#$raw" }
    val hexIsValid = hexInput.isBlank() ||
        (normalizedInput.matches(Regex("#[0-9A-Fa-f]{6}")) && parseHexColor(normalizedInput) != null)
    val current = parseHexColor(hex)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (showPreview) 4.dp else 0.dp)
    ) {
        if (showPreview) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.cardElevated)
                    .xvoxPressScale { expanded = !expanded }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (hex.isBlank()) colors.cardBorder else current ?: colors.cardBorder)
                        .drawBehind {
                            if (hex.isBlank()) {
                                drawLine(
                                    colors.primaryText.copy(alpha = .45f),
                                    Offset.Zero,
                                    Offset(size.width, size.height),
                                    2.dp.toPx()
                                )
                            }
                        }
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(label, color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    subtitle?.let { Text(it, color = colors.secondaryText, fontSize = 11.sp) }
                }
                Text(
                    text = hex.takeIf { it.isNotBlank() }?.uppercase() ?: "Auto",
                    color = if (hex.isBlank()) colors.mutedText else colors.primaryAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (expanded) {
            ColorPickerEditor(
                label = label,
                subtitle = subtitle,
                wheelH = wheelH,
                wheelS = wheelS,
                wheelV = wheelV,
                hexInput = hexInput,
                hexIsValid = hexIsValid,
                alpha = alpha,
                onAlphaChange = onAlphaChange,
                onWheelChange = ::publish,
                onHexInputChange = { raw ->
                    val compact = raw.replace(" ", "").uppercase().take(7)
                    hexInput = compact
                    val normalized = compact.let { if (it.startsWith("#")) it else "#$it" }
                    if (normalized.matches(Regex("#[0-9A-F]{6}"))) {
                        hexToHsv(normalized)?.let { hsv -> publish(hsv[0], hsv[1], hsv[2]) }
                    }
                },
                onResetAuto = { hexInput = ""; onColorChange("") },
                onSwatch = { swatch ->
                    hexToHsv(swatch)?.let { hsv -> publish(hsv[0], hsv[1], hsv[2]) }
                },
                showHeading = !showPreview
            )
        }
    }
}

@Composable
private fun ColorPickerEditor(
    label: String,
    subtitle: String?,
    wheelH: Float,
    wheelS: Float,
    wheelV: Float,
    hexInput: String,
    hexIsValid: Boolean,
    alpha: Float?,
    onAlphaChange: ((Float) -> Unit)?,
    onWheelChange: (Float, Float, Float) -> Unit,
    onHexInputChange: (String) -> Unit,
    onResetAuto: () -> Unit,
    onSwatch: (String) -> Unit,
    showHeading: Boolean
) {
    val colors = XvoxTheme.colors
    val liveCustomSwatch = hsvToHex(wheelH, wheelS, wheelV)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (showHeading) 0.dp else 8.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (showHeading) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, color = colors.primaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                subtitle?.let { Text(it, color = colors.secondaryText, fontSize = 11.sp) }
            }
        }

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            var wheelPx by remember { mutableStateOf(0) }
            Box(
                modifier = Modifier
                    .size(212.dp)
                    .onSizeChanged { wheelPx = it.width }
            ) {
                val bitmap = remember(wheelPx, wheelV) {
                    if (wheelPx == 0) null else buildWheelBitmap(wheelPx, wheelV)
                }
                bitmap?.let {
                    Image(bitmap = it, contentDescription = "Accent colour wheel", modifier = Modifier.fillMaxSize())
                }
                val half = if (wheelPx == 0) 106f else wheelPx / 2f
                val markerRadians = (wheelH - 90f) * PI.toFloat() / 180f
                val markerRadius = half * wheelS.coerceIn(0f, 1f)
                val marker = Offset(
                    half + markerRadius * cos(markerRadians),
                    half + markerRadius * sin(markerRadians)
                )
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(colors.primaryText, radius = 6.dp.toPx(), center = marker)
                    drawCircle(colors.background.copy(alpha = .75f), radius = 2.dp.toPx(), center = marker)
                }
                fun updateFromPosition(position: Offset) {
                    val dx = position.x - half
                    val dy = position.y - half
                    val distance = sqrt(dx * dx + dy * dy)
                    var hue = atan2(dy, dx) * 180f / PI.toFloat() + 90f
                    if (hue < 0f) hue += 360f
                    onWheelChange(hue % 360f, (distance / half).coerceIn(0f, 1f), wheelV)
                }
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .pointerInput(wheelH, wheelS, wheelV) {
                            detectTapGestures { updateFromPosition(it) }
                        }
                        .pointerInput(wheelH, wheelS, wheelV) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                updateFromPosition(change.position)
                            }
                        }
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Brightness", color = colors.mutedText, fontSize = 11.sp)
                Text("${(wheelV * 100).roundToInt()}%", color = colors.primaryText, fontSize = 11.sp)
            }
            XvoxContinuousSlider(
                value = wheelV,
                onValueChange = { value -> onWheelChange(wheelH, wheelS, value) },
                valueRange = 0f..1f,
                defaultValue = 1f,
                contentDescription = "Accent brightness"
            )
        }

        if (alpha != null && onAlphaChange != null) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Transparency", color = colors.mutedText, fontSize = 11.sp)
                    Text("${(alpha.coerceIn(0f, 1f) * 100).roundToInt()}%", color = colors.primaryText, fontSize = 11.sp)
                }
                XvoxContinuousSlider(
                    value = alpha.coerceIn(0f, 1f),
                    onValueChange = { onAlphaChange(it.coerceIn(0f, 1f)) },
                    defaultValue = 1f,
                    contentDescription = "Accent transparency"
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Hex", color = colors.mutedText, fontSize = 11.sp, modifier = Modifier.width(34.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.cardElevated)
                    .border(
                        .8.dp,
                        if (hexIsValid) colors.cardBorder else colors.primaryAccent.copy(alpha = .75f),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                BasicTextField(
                    value = hexInput,
                    onValueChange = onHexInputChange,
                    textStyle = TextStyle(color = colors.primaryText, fontSize = 13.sp),
                    singleLine = true
                )
            }
            Spacer(Modifier.width(7.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.cardElevated)
                    .xvoxPressScale(onClick = onResetAuto)
                    .padding(horizontal = 11.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Auto", color = colors.secondaryText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
        if (!hexIsValid) {
            XvoxPill("Invalid")
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BaseAccentSwatches.forEach { swatch ->
                ColorSwatch(
                    color = parseHexColor(swatch) ?: Color.Transparent,
                    active = hexInput.equals(swatch, ignoreCase = true),
                    onClick = { onSwatch(swatch) }
                )
            }
            // The eleventh swatch is intentionally not sticky: it changes live with wheel,
            // brightness, and valid hex edits, so it always reflects the active custom colour.
            ColorSwatch(
                color = parseHexColor(liveCustomSwatch) ?: Color.Transparent,
                active = true,
                custom = true,
                onClick = { onSwatch(liveCustomSwatch) }
            )
        }
    }
}

@Composable
private fun ColorSwatch(
    color: Color,
    active: Boolean,
    custom: Boolean = false,
    onClick: () -> Unit
) {
    val colors = XvoxTheme.colors
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (active || custom) Modifier.drawBehind {
                    drawCircle(colors.primaryText.copy(alpha = if (active) .95f else .65f), style = Stroke(1.5.dp.toPx()))
                } else Modifier
            )
            .xvoxPressScale(onClick = onClick)
    )
}

/** Small theme-derived status pill used by the live hex editor. */
@Composable
fun XvoxPill(text: String, modifier: Modifier = Modifier) {
    val colors = XvoxTheme.colors
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(colors.cardElevated)
            .border(.7.dp, colors.primaryAccent.copy(alpha = .60f), CircleShape)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = colors.primaryAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
