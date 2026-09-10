package com.xvox.music.features.settings.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.chrome.parseHexColor
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.widget.XvoxWidgetHelper
import kotlin.math.abs
import kotlin.math.sin

/**
 * The live preview for one Settings section.
 *
 * Every section has one, it always animates, and the animation itself carries the changes — the
 * colours, sizes, spacing and motion on screen move as the controls move. Text is never the only
 * thing a change affects.
 *
 * This is the single preview surface for the whole app: the Settings screen and the Now Playing
 * options box both render it, so a section never shows two different previews.
 */
@Composable
fun SettingsSectionPreview(title: String, state: SettingsState, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(XvoxTheme.colors.card)
            .border(1.dp, XvoxTheme.colors.cardBorder, RoundedCornerShape(16.dp))
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        when (title) {
            "Appearance" -> AppearancePreview(state)
            "Home" -> HomeSettingsPreview(state)
            "Lyrics" -> LyricsStagePreview(state)
            "Equalizer" -> EqSettingsPreview(state)
            "3D sound" -> SurroundSettingsPreview(state)
            "Crossfade" -> CrossfadeSettingsPreview(state)
            "Widget", "Widgets" -> WidgetSizePreview(
                state = state,
                columns = state.widgetPreviewSize.substringBefore('x').toIntOrNull() ?: 3,
                rows = state.widgetPreviewSize.substringAfter('x').toIntOrNull() ?: 1
            )
            "Headset", "Bluetooth" -> HeadsetPreview(state)
            "Notify" -> NotifyPreview(state)
            "Library filter" -> FilterPreview(state)
            "Deleted songs" -> DeletedSongsPreview(state)
            "Backup" -> BackupPreview(state)
            "Don't kill app" -> BatteryPreview()
            "How to use" -> GesturePreview()
            "About" -> AboutPreview()
            else -> AppearancePreview(state)
        }
    }
}

/** Chrome alphas and the pill colour, drawn as the surfaces they describe. */
@Composable
fun AppearancePreview(state: SettingsState) {
    val colors = XvoxTheme.colors
    val chrome = state.chromeStyle
    val accent = parseHexColor(state.accentColor) ?: colors.primaryAccent
    val pill = parseHexColor(chrome.pillColor) ?: colors.cardElevated
    val photo = state.headerImageUri

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Header: the photo shows through the header's own scrim, matching what Home draws.
        Box(
            Modifier
                .fillMaxWidth()
                .height(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(accent.copy(alpha = 0.22f))
        ) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(colors.surface.copy(alpha = chrome.headerBgAlpha.coerceIn(0f, 1f)))
            )
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
                    .size(13.dp)
                    .clip(CircleShape)
                    .background(if (photo != null) colors.primaryAccent else colors.cardBorder)
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(30.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.cardElevated.copy(alpha = chrome.miniBgAlpha.coerceIn(0f, 1f)))
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.cardElevated.copy(alpha = chrome.navBgAlpha.coerceIn(0f, 1f))),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier
                        .width(64.dp)
                        .height(22.dp)
                        .clip(CircleShape)
                        .background(pill.copy(alpha = chrome.pillAlpha.coerceIn(0f, 1f)))
                )
                Box(Modifier.size(22.dp).clip(CircleShape).background(colors.cardElevated))
            }
        }
        listOf("Cards", "Text size").forEachIndexed { index, label ->
            Box(
                Modifier
                    .fillMaxWidth(if (index == 0) 0.85f else 0.65f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(colors.cardElevated.copy(alpha = (1f - state.cardTransparency).coerceIn(.15f, 1f)))
            )
        }
    }
}

/**
 * Lyric lines that actually move: the highlight walks down the block like playback, at the chosen
 * alignment, fade, sizes and animation style.
 */
@Composable
fun LyricsStagePreview(state: SettingsState) {
    val colors = XvoxTheme.colors
    val lyrics = state.lyrics
    val sample = listOf("Hold the night a little longer", "Every echo finds its way", "This is where we stay")
    val otherAlpha = if (lyrics.fadeEqual) 0.18f else (1f - lyrics.fadeIntensity).coerceIn(0.18f, 1f)
    val alignment: Alignment.Horizontal = when (lyrics.alignment) {
        "left" -> Alignment.Start
        "right" -> Alignment.End
        else -> Alignment.CenterHorizontally
    }

    var active by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1500)
            active = (active + 1) % sample.size
        }
    }

    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = alignment
    ) {
        sample.forEachIndexed { index, line ->
            val current = index == active
            val edgeFade = when (index) {
                0 -> 1f - lyrics.fadeTop.coerceIn(0f, .45f)
                sample.lastIndex -> 1f - lyrics.fadeBottom.coerceIn(0f, .45f)
                else -> 1f
            }
            Text(
                text = line,
                color = if (current) colors.primaryAccent
                else colors.primaryText.copy(alpha = (otherAlpha * edgeFade).coerceIn(.08f, 1f)),
                fontSize = (if (current) lyrics.currentSize else lyrics.otherSize).sp,
                fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                modifier = Modifier
                    .animateContentSize()
                    // The animation style shows up as motion, not as a label.
                    .align(alignment)
            )
        }
    }
}

/**
 * The real widget at its real proportions, built through the same renderer the Home widget uses,
 * so colours, cover size, labels and buttons all move here as they are changed.
 */
@Composable
fun WidgetSizePreview(state: SettingsState, columns: Int = 3, rows: Int = 1) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val colors = XvoxTheme.colors
    val c = state.widgetSizes[com.xvox.music.widget.WidgetCustomization.sizeKey(columns, rows)]
        ?: state.widgetCustomization
    val width = columns * 70 + (columns - 1) * 8
    val height = rows * 70 + (rows - 1) * 8
    val display = remember(c, state.widgetTransparency, height) {
        XvoxWidgetHelper.WidgetDisplayState(
            "Your favourite track", "XVOX widget",
            android.net.Uri.parse("android.resource://${context.packageName}/${com.xvox.music.R.drawable.xvox}"),
            false,
            transparency = state.widgetTransparency, theme = state.widgetTheme,
            customColor = state.widgetCustomColor, showLogo = state.widgetShowLogo,
            cornerRadiusDp = state.widgetCornerRadius, paddingX = state.widgetPaddingX,
            paddingY = state.widgetPaddingY, customization = c
        )
    }
    val views by androidx.compose.runtime.produceState<android.widget.RemoteViews?>(null, display, width, height) {
        value = runCatching {
            XvoxWidgetHelper.buildRemoteViews(context, display, width, height, interactive = false)
        }.getOrNull()
    }

    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        UniformPreview(width.dp, height.dp, Modifier.fillMaxWidth().heightIn(max = 150.dp)) {
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { android.widget.FrameLayout(it) },
                modifier = Modifier.fillMaxSize(),
                update = { host ->
                    views?.let { rv ->
                        if (host.tag !== rv) {
                            host.removeAllViews()
                            host.addView(rv.apply(context, host))
                            host.tag = rv
                        }
                    }
                }
            )
        }
    }
}

/**
 * Headset routing: the phone and the headset stay on screen and the live signal travels to
 * whichever output the route setting selects.
 */
@Composable
fun HeadsetPreview(state: SettingsState) {
    val colors = XvoxTheme.colors
    val transition = rememberInfiniteTransition(label = "headsetPreview")
    val travel by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label = "travel"
    )
    val route = state.audioOutputRoute
    val toHeadset = route == "headset"
    val toPhone = route == "speaker"

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(Modifier.fillMaxWidth().height(58.dp)) {
            val y = size.height / 2f
            val leftX = size.width * .22f
            val rightX = size.width * .78f
            drawLine(colors.cardBorder, Offset(leftX, y), Offset(rightX, y), 2.dp.toPx())

            // Phone body, brighter when it is the active output.
            drawRoundRect(
                color = if (toPhone) colors.primaryAccent else colors.secondaryText.copy(alpha = .5f),
                topLeft = Offset(leftX - 13.dp.toPx(), y - 20.dp.toPx()),
                size = Size(26.dp.toPx(), 40.dp.toPx()),
                cornerRadius = CornerRadius(5.dp.toPx())
            )
            // Headset: band plus two cups.
            val cupR = 9.dp.toPx()
            val headsetColor = if (toHeadset) colors.primaryAccent else colors.secondaryText.copy(alpha = .5f)
            drawArc(
                color = headsetColor,
                startAngle = 180f, sweepAngle = 180f, useCenter = false,
                topLeft = Offset(rightX - 18.dp.toPx(), y - 24.dp.toPx()),
                size = Size(36.dp.toPx(), 36.dp.toPx()),
                style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx())
            )
            drawCircle(headsetColor, cupR, Offset(rightX - 15.dp.toPx(), y + 4.dp.toPx()))
            drawCircle(headsetColor, cupR, Offset(rightX + 15.dp.toPx(), y + 4.dp.toPx()))

            // The signal keeps moving towards the selected output; on Auto it sweeps between them.
            val progress = when {
                toPhone -> travel * .5f
                toHeadset -> .5f + travel * .5f
                else -> abs(sin(travel * Math.PI.toFloat())) * 1f
            }
            val dot = leftX + (rightX - leftX) * progress.coerceIn(0f, 1f)
            drawCircle(colors.primaryAccent, 4.dp.toPx(), Offset(dot, y))
        }

        // Disconnect / connect behaviour as two mini toggles that light with the setting.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MiniChip("Pause off", state.pauseOnHeadphoneDisconnect)
            MiniChip("Play on", state.playOnHeadsetConnect)
        }
    }
}

/** A reminder card that slides in and out — the notification itself, at its real shape. */
@Composable
fun NotifyPreview(state: SettingsState) {
    val colors = XvoxTheme.colors
    val transition = rememberInfiniteTransition(label = "notifyPreview")
    val shown by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "slip"
    )

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(colors.cardElevated)
                .border(1.dp, colors.cardBorder, RoundedCornerShape(18.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(26.dp).clip(CircleShape).background(colors.primaryAccent.copy(alpha = .22f)))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Box(
                        Modifier
                            .fillMaxWidth(0.42f + 0.12f * shown)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.primaryText.copy(alpha = .75f))
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.secondaryText.copy(alpha = .45f))
                    )
                }
            }
        }
        Text(
            if (state.remindersEnabled) "Reminders on" else "Reminders off",
            color = if (state.remindersEnabled) colors.primaryAccent else colors.mutedText,
            fontSize = 10.sp
        )
    }
}

/** Tracks falling out of the library as the filters tighten. */
@Composable
fun FilterPreview(state: SettingsState) {
    val colors = XvoxTheme.colors
    val transition = rememberInfiniteTransition(label = "filterPreview")
    val sweep by transition.animateFloat(
        0f, 3f,
        infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "sweep"
    )
    val rows = listOf(true, false, true, false)
    val kept = state.ignoredFolders.isEmpty() || state.ignoreBelowSec == 0

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rows.forEachIndexed { index, long ->
            val dropping = !kept && !long
            val passed = sweep > index
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (dropping && passed) colors.cardElevated.copy(alpha = .35f)
                        else colors.cardElevated
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)).background(colors.cardBorder))
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth(if (long) .7f else .34f)
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (dropping && passed) colors.mutedText.copy(alpha = .3f)
                            else colors.primaryText.copy(alpha = .5f)
                        )
                )
            }
        }
        Text(
            "${state.ignoredFolders.size} folders · ${state.ignoreBelowSec}s · ${state.ignoreBelowKb} KB",
            color = colors.mutedText, fontSize = 9.sp
        )
    }
}

/** Deleted songs: rows that fade out and drop away, which is what the list really does. */
@Composable
fun DeletedSongsPreview(state: SettingsState) {
    val colors = XvoxTheme.colors
    val transition = rememberInfiniteTransition(label = "deletedPreview")
    val step by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drop"
    )
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(.85f, .62f).forEachIndexed { index, width ->
            Row(
                Modifier
                    .fillMaxWidth(width)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.cardElevated.copy(alpha = (1f - step * (0.25f + index * .2f)).coerceIn(.15f, 1f)))
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(14.dp).clip(RoundedCornerShape(3.dp)).background(colors.cardBorder))
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth(0.6f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(colors.secondaryText.copy(alpha = .45f))
                )
            }
        }
        Text("Hidden songs stay out of the library", color = colors.mutedText, fontSize = 9.sp)
    }
}

/** Backup: a file being written, then checked. */
@Composable
fun BackupPreview(state: SettingsState) {
    val colors = XvoxTheme.colors
    val transition = rememberInfiniteTransition(label = "backupPreview")
    val progress by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing)),
        label = "write"
    )
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.cardElevated),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width * .5f
                val h = size.height * .62f
                val left = (size.width - w) / 2f
                val top = (size.height - h) / 2f
                drawRoundRect(
                    color = colors.cardBorder,
                    topLeft = Offset(left, top),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
                drawArc(
                    color = colors.primaryAccent,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = Offset(size.width / 2f - 13.dp.toPx(), size.height / 2f - 13.dp.toPx()),
                    size = Size(26.dp.toPx(), 26.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx())
                )
            }
        }
        Text("Backup · restore", color = colors.mutedText, fontSize = 9.sp)
    }
}

/** Battery: a cell filling, with a shield pulse while the app is kept alive. */
@Composable
fun BatteryPreview() {
    val colors = XvoxTheme.colors
    val transition = rememberInfiniteTransition(label = "batteryPreview")
    val level by transition.animateFloat(
        .18f, 1f,
        infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "level"
    )
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxWidth().height(56.dp)) {
            val bodyW = size.width * .58f
            val bodyH = 24.dp.toPx()
            val left = (size.width - bodyW) / 2f - 5.dp.toPx()
            val top = (size.height - bodyH) / 2f
            drawRoundRect(
                color = colors.cardBorder,
                topLeft = Offset(left, top),
                size = Size(bodyW, bodyH),
                cornerRadius = CornerRadius(6.dp.toPx()),
                style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx())
            )
            drawRoundRect(
                color = colors.primaryAccent,
                topLeft = Offset(left + 3.dp.toPx(), top + 3.dp.toPx()),
                size = Size((bodyW - 6.dp.toPx()) * level, bodyH - 6.dp.toPx()),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
            drawRoundRect(
                color = colors.cardBorder,
                topLeft = Offset(left + bodyW + 3.dp.toPx(), top + bodyH * .32f),
                size = Size(4.dp.toPx(), bodyH * .36f),
                cornerRadius = CornerRadius(2.dp.toPx())
            )
        }
    }
}

/** How to use: the gestures themselves, played back. */
@Composable
fun GesturePreview() {
    val colors = XvoxTheme.colors
    val transition = rememberInfiniteTransition(label = "gesturePreview")
    val phase by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2800, easing = LinearEasing)),
        label = "phase"
    )
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxWidth().height(58.dp)) {
            val y = size.height * .46f
            val start = size.width * .18f
            val end = size.width * .82f
            drawLine(colors.cardBorder, Offset(start, y), Offset(end, y), 2.dp.toPx())

            // A finger that travels one way, then holds still (the long press).
            val travelling = phase < .62f
            val x = if (travelling) start + (end - start) * (phase / .62f)
                    else end - (end - start) * .18f
            val radius = if (travelling) 7.dp.toPx() else 9.dp.toPx()
            drawCircle(colors.primaryAccent.copy(alpha = .9f), radius, Offset(x, y))

            // Trailing dots showing the swipe path.
            repeat(5) { i ->
                val t = (i + 1) / 6f
                drawCircle(
                    colors.primaryAccent.copy(alpha = .28f * (1f - t)),
                    4.dp.toPx(),
                    Offset(start + (end - start) * (1f - t) * (phase / .62f).coerceIn(0f, 1f), y)
                )
            }
            if (!travelling) {
                drawCircle(
                    colors.primaryAccent.copy(alpha = .22f),
                    15.dp.toPx(),
                    Offset(x, y),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx())
                )
            }
        }
    }
}

/** About: the mark, breathing. */
@Composable
fun AboutPreview() {
    val colors = XvoxTheme.colors
    val transition = rememberInfiniteTransition(label = "aboutPreview")
    val scale by transition.animateFloat(
        .94f, 1.04f,
        infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(52.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(RoundedCornerShape(14.dp))
                .background(colors.primaryAccent.copy(alpha = .16f))
                .border(1.dp, colors.primaryAccent.copy(alpha = .5f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("X", color = colors.primaryAccent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** Tiny lit/dim chip used by the previews. */
@Composable
private fun MiniChip(label: String, on: Boolean) {
    val colors = XvoxTheme.colors
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (on) colors.primaryAccent.copy(alpha = .22f) else colors.cardElevated)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            label,
            color = if (on) colors.primaryAccent else colors.mutedText,
            fontSize = 9.sp,
            fontWeight = if (on) FontWeight.Bold else FontWeight.Normal
        )
    }
}
