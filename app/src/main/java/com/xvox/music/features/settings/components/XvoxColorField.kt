package com.xvox.music.features.settings.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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
    val sat = s.coerceIn(0f, 1f); val value = v.coerceIn(0f, 1f)
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
    return Color((r + m), (g + m), (b + m))
}

private fun hexToHsv(hex: String): FloatArray? = parseHexColor(hex)?.let { colorToHsv(it) }

private fun hsvToHex(h: Float, s: Float, v: Float): String =
    "#%06X".format(hsvColor(h, s, v).toArgb() and 0xFFFFFF)

/** A real HSV wheel: hue around the circle, saturation by radius, brightness via the slider. */
private fun buildWheelBitmap(size: Int, value: Float): ImageBitmap {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val center = size / 2f; val radius = center
    val pixel = IntArray(size * size)
    for (y in 0 until size) {
        for (x in 0 until size) {
            val dx = x - center; val dy = y - center
            val dist = sqrt(dx * dx + dy * dy)
            if (dist > radius) {
                pixel[y * size + x] = 0
            } else {
                var angle = atan2(dy, dx) * 180f / PI.toFloat() + 90f
                if (angle < 0f) angle += 360f
                pixel[y * size + x] = hsvColor(angle % 360f, (dist / radius).coerceIn(0f, 1f), value).toArgb()
            }
        }
    }
    bitmap.setPixels(pixel, 0, size, 0, 0, size, size)
    return bitmap.asImageBitmap()
}

private val WheelSwatches = listOf(
    "#FF3B30", "#FF9500", "#FFCC00", "#34C759", "#00C7BE", "#007AFF", "#AF52DE", "#FF2D92"
)

@Composable
fun ColorPickerRow(
    label: String,
    hex: String,
    onColorChange: (String) -> Unit,
    subtitle: String? = null,
    alpha: Float? = null,
    onAlphaChange: ((Float) -> Unit)? = null
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    var wheelH by remember { mutableFloatStateOf(hexToHsv(hex)?.get(0) ?: 210f) }
    var wheelS by remember { mutableFloatStateOf(hexToHsv(hex)?.get(1) ?: 0.8f) }
    var wheelV by remember { mutableFloatStateOf(hexToHsv(hex)?.get(2) ?: 0.9f) }

    LaunchedEffect(hex) {
        val hsv = hexToHsv(hex) ?: return@LaunchedEffect
        wheelH = hsv[0]
        wheelS = hsv[1]
        wheelV = hsv[2]
    }

    var pendingPush by remember { mutableStateOf<Job?>(null) }
    fun commit(h: Float, s: Float, v: Float) {
        wheelH = h; wheelS = s; wheelV = v
        pendingPush?.cancel()
        pendingPush = scope.launch {
            kotlinx.coroutines.delay(40)
            onColorChange(hsvToHex(wheelH, wheelS, wheelV))
        }
    }
    fun flushPending() {
        pendingPush?.cancel()
        pendingPush = null
        onColorChange(hsvToHex(wheelH, wheelS, wheelV))
    }

    val current = parseHexColor(hex)

    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            Modifier
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
                    .background(
                        if (hex.isBlank()) colors.cardBorder
                        else current ?: colors.cardBorder
                    )
                    .drawBehind {
                        if (hex.isBlank()) {
                            drawLine(
                                Color(0x99FFFFFF), Offset.Zero,
                                Offset(size.width, size.height), 2.dp.toPx()
                            )
                        }
                    }
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(label, color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                if (subtitle != null) {
                    Text(subtitle, color = colors.secondaryText, fontSize = 11.sp)
                }
            }
            if (hex.isBlank()) {
                Text("Auto", color = colors.mutedText, fontSize = 11.sp)
            } else {
                Text(hex.uppercase(), color = colors.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (expanded) {
            Column(
                Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    var wheelPx by remember { mutableStateOf(0) }
                    Box(
                        Modifier
                            .size(200.dp)
                            .onSizeChanged { wheelPx = it.width }
                    ) {
                        val bitmap = remember(wheelPx, wheelV) {
                            if (wheelPx == 0) null else buildWheelBitmap(wheelPx, wheelV)
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = "Colour wheel",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        val half = if (wheelPx == 0) 100f else wheelPx / 2f
                        val rad = (wheelH - 90f) * PI.toFloat() / 180f
                        val markerR = half * wheelS.coerceIn(0f, 1f)
                        val mx = half + markerR * cos(rad)
                        val my = half + markerR * sin(rad)
                        Canvas(Modifier.fillMaxSize()) {
                            drawCircle(Color.White, radius = 6.dp.toPx(), center = Offset(mx, my))
                            drawCircle(Color.Black.copy(alpha = 0.75f), radius = 2.dp.toPx(), center = Offset(mx, my))
                        }
                        Box(
                            Modifier
                                .matchParentSize()
                                .pointerInput(Unit) {
                                    detectTapGestures { offset ->
                                        val dx = offset.x - half; val dy = offset.y - half
                                        val dist = sqrt(dx * dx + dy * dy)
                                        if (dist <= half) {
                                            var a = atan2(dy, dx) * 180f / PI.toFloat() + 90f
                                            if (a < 0f) a += 360f
                                            commit(a % 360f, (dist / half).coerceIn(0f, 1f), wheelV)
                                            flushPending()
                                        }
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragEnd = { flushPending() },
                                        onDragCancel = { flushPending() }
                                    ) { change, _ ->
                                        change.consume()
                                        val dx = change.position.x - half; val dy = change.position.y - half
                                        val dist = sqrt(dx * dx + dy * dy)
                                        if (dist <= half) {
                                            var a = atan2(dy, dx) * 180f / PI.toFloat() + 90f
                                            if (a < 0f) a += 360f
                                            commit(a % 360f, (dist / half).coerceIn(0f, 1f), wheelV)
                                        }
                                    }
                                }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Dark", color = colors.mutedText, fontSize = 11.sp, modifier = Modifier.width(34.dp))
                    XvoxThinLineSlider(
                        value = wheelV,
                        onValueChange = { v -> commit(wheelH, wheelS, v) },
                        valueRange = 0.15f..1f,
                        defaultValue = 0.9f,
                        modifier = Modifier.weight(1f),
                        onValueChangeFinished = { flushPending() }
                    )
                    Text("Light", color = colors.mutedText, fontSize = 11.sp, modifier = Modifier.width(34.dp))
                }

                if (alpha != null && onAlphaChange != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Transparency", color = colors.mutedText, fontSize = 11.sp, modifier = Modifier.width(70.dp))
                        XvoxThinLineSlider(
                            value = alpha.coerceIn(0f, 1f),
                            onValueChange = { a -> onAlphaChange(a.coerceIn(0f, 1f)) },
                            valueRange = 0f..1f,
                            defaultValue = 1f,
                            modifier = Modifier.weight(1f)
                        )
                        Text("${(alpha.coerceIn(0f, 1f) * 100).roundToInt()}%",
                            color = colors.primaryText, fontSize = 11.sp, modifier = Modifier.width(38.dp))
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Hex", color = colors.mutedText, fontSize = 11.sp, modifier = Modifier.width(34.dp))
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.card)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        BasicTextField(
                            value = hex.ifBlank { "#000000" }.uppercase(),
                            onValueChange = { raw ->
                                val clean = raw.replace("#", "").uppercase().take(6)
                                if (clean.length == 6) {
                                    hexToHsv("#$clean")?.let { commit(it[0], it[1], it[2]) }
                                }
                            },
                            textStyle = TextStyle(color = colors.primaryText, fontSize = 13.sp),
                            singleLine = true
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.cardElevated)
                            .xvoxPressScale { onColorChange("") }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Auto", color = colors.secondaryText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WheelSwatches.forEach { swatch ->
                        val active = hex.equals(swatch, ignoreCase = true)
                        Box(
                            Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(swatch) ?: Color.Transparent)
                                .then(if (active) {
                                    Modifier.drawBehind {
                                        drawCircle(Color.White, style = Stroke(2.dp.toPx()))
                                    }
                                } else Modifier)
                                .xvoxPressScale { onColorChange(swatch) }
                        )
                    }
                }
            }
        }
    }
}
