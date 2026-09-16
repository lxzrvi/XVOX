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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.chrome.parseHexColor
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.widget.XvoxWidgetHelper

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
            "Notify", "Notifications" -> NotifyPreview(state)
            else -> AppearancePreview(state)
        }
    }
}

/** Accurate miniature phone screen: Header, Main part, Mini player, Nav bar. */
@Composable
fun AppearancePreview(state: SettingsState) {
    val colors = XvoxTheme.colors
    val chrome = state.chromeStyle
    val accent = parseHexColor(state.accentColor) ?: colors.primaryAccent
    val headerEdge = parseHexColor(chrome.headerBorder) ?: colors.cardBorder
    val miniEdge = parseHexColor(chrome.miniBorder) ?: colors.cardBorder
    val navEdge = parseHexColor(chrome.navBorder) ?: colors.cardBorder
    val cardEdge = parseHexColor(chrome.cardBorder) ?: colors.cardBorder
    val photo = state.headerImageUri

    Column(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.background.copy(alpha = state.backgroundBrightness.coerceIn(0.3f, 1f)))
            .padding(6.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Header (Top)
        Box(
            Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surface.copy(alpha = chrome.headerBgAlpha.coerceIn(0f, 1f)))
                .border(0.7.dp, headerEdge.copy(alpha = chrome.headerBorderAlpha.coerceIn(0f, 1f)), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(if (photo != null) accent else colors.cardElevated)
                )
                Column(Modifier.weight(1f)) {
                    Box(Modifier.width(36.dp).height(5.dp).clip(RoundedCornerShape(2.dp)).background(accent))
                    Spacer(Modifier.height(3.dp))
                    Box(Modifier.width(52.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(colors.secondaryText.copy(alpha = 0.6f)))
                }
                Box(
                    Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(colors.cardElevated)
                )
            }
        }

        // 2. Main Content (Middle)
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.card.copy(alpha = (1f - state.cardTransparency).coerceIn(0.2f, 1f)))
                        .border(0.6.dp, cardEdge.copy(alpha = chrome.cardBorderAlpha.coerceIn(0f, 1f)), RoundedCornerShape(6.dp))
                        .padding(4.dp)
                ) {
                    Box(Modifier.size(14.dp).clip(RoundedCornerShape(3.dp)).background(accent.copy(alpha = 0.4f)))
                }
                Box(
                    Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.card.copy(alpha = (1f - state.cardTransparency).coerceIn(0.2f, 1f)))
                        .border(0.6.dp, cardEdge.copy(alpha = chrome.cardBorderAlpha.coerceIn(0f, 1f)), RoundedCornerShape(6.dp))
                        .padding(4.dp)
                ) {
                    Box(Modifier.size(14.dp).clip(RoundedCornerShape(3.dp)).background(accent.copy(alpha = 0.4f)))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.card.copy(alpha = (1f - state.cardTransparency).coerceIn(0.2f, 1f)))
                        .border(0.6.dp, cardEdge.copy(alpha = chrome.cardBorderAlpha.coerceIn(0f, 1f)), RoundedCornerShape(6.dp))
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.card.copy(alpha = (1f - state.cardTransparency).coerceIn(0.2f, 1f)))
                        .border(0.6.dp, cardEdge.copy(alpha = chrome.cardBorderAlpha.coerceIn(0f, 1f)), RoundedCornerShape(6.dp))
                )
            }
        }

        // 3. Miniplayer (Floating)
        Box(
            Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.cardElevated.copy(alpha = chrome.miniBgAlpha.coerceIn(0f, 1f)))
                .border(0.7.dp, miniEdge.copy(alpha = chrome.miniBorderAlpha.coerceIn(0f, 1f)), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)).background(accent.copy(alpha = 0.6f)))
                    Box(Modifier.width(48.dp).height(5.dp).clip(RoundedCornerShape(2.dp)).background(colors.primaryText))
                }
                Box(Modifier.size(16.dp).clip(CircleShape).background(accent))
            }
        }

        Spacer(Modifier.height(3.dp))

        // 4. Navbar (Bottom bar)
        Box(
            Modifier
                .fillMaxWidth()
                .height(30.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.cardElevated.copy(alpha = chrome.navBgAlpha.coerceIn(0f, 1f)))
                .border(0.7.dp, navEdge.copy(alpha = chrome.navBorderAlpha.coerceIn(0f, 1f)), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(accent))
                Box(Modifier.size(12.dp).clip(CircleShape).background(colors.mutedText))
                Box(Modifier.size(12.dp).clip(CircleShape).background(colors.mutedText))
            }
        }
    }
}

/** Accurate Lyrics Stage Preview: full line alignment & highlight motion. */
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
    val textAlign = when (lyrics.alignment) {
        "left" -> androidx.compose.ui.text.style.TextAlign.Start
        "right" -> androidx.compose.ui.text.style.TextAlign.End
        else -> androidx.compose.ui.text.style.TextAlign.Center
    }

    var active by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1500)
            active = (active + 1) % sample.size
        }
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    UniformPreview(
        configuration.screenWidthDp.dp,
        configuration.screenHeightDp.dp,
        Modifier.fillMaxWidth().heightIn(max = 176.dp)
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = alignment
        ) {
            sample.forEachIndexed { index, line ->
                val current = index == active
                val edgeFade = when (index) {
                    0 -> 1f - lyrics.fadeTop.coerceIn(0f, .45f)
                    sample.lastIndex -> 1f - lyrics.fadeBottom.coerceIn(0f, .45f)
                    else -> 1f
                }
                val lineFontSize = when {
                    current -> lyrics.currentSize
                    index < active -> lyrics.topSize
                    else -> lyrics.bottomSize
                }
                Text(
                    text = line,
                    color = if (current) colors.primaryAccent
                    else colors.primaryText.copy(alpha = (otherAlpha * edgeFade).coerceIn(.08f, 1f)),
                    fontSize = lineFontSize.sp,
                    fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    textAlign = textAlign,
                    modifier = Modifier
                        .animateContentSize()
                        .fillMaxWidth()
                        .align(alignment)
                )
            }
        }
    }
}

/** Widget size live preview. */
@Composable
fun WidgetSizePreview(state: SettingsState, columns: Int = 3, rows: Int = 1) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val colors = XvoxTheme.colors
    val c = state.widgetSizes[com.xvox.music.widget.WidgetCustomization.sizeKey(columns, rows)]
        ?: state.widgetCustomization
    val width = columns * 70 + (columns - 1) * 8
    val height = rows * 70 + (rows - 1) * 8
    val display = remember(c, state.widgetTransparency, state.theme, state.accentColor, state.widgetCornerRadius, state.widgetPaddingX, state.widgetPaddingY) {
        XvoxWidgetHelper.WidgetDisplayState(
            songTitle = "Your favourite track",
            songArtist = "XVOX widget",
            artworkUri = null,
            isPlaying = true,
            isLiked = false,
            currentPosition = 0L,
            duration = 0L,
            transparency = state.widgetTransparency,
            theme = state.theme,
            customColor = state.accentColor,
            showLogo = true,
            cornerRadiusDp = state.widgetCornerRadius,
            paddingX = state.widgetPaddingX,
            paddingY = state.widgetPaddingY,
            customization = c
        )
    }
    val views by produceState<android.widget.RemoteViews?>(null, display, width, height) {
        value = XvoxWidgetHelper.buildRemoteViews(context, display, width, height)
    }

    UniformPreview(width.dp, height.dp, Modifier.fillMaxWidth().heightIn(max = 160.dp)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { ctx -> android.widget.FrameLayout(ctx) },
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
 * Headset preview: Phone and Headset icons.
 * Auto -> both accent colored.
 * Speaker -> Phone accent.
 * Headset -> Headset accent.
 * Connected/disconnected state shown on headset without plain UI text.
 */
@Composable
fun HeadsetPreview(state: SettingsState) {
    val colors = XvoxTheme.colors
    val route = state.audioOutputRoute
    val isAuto = route == "auto" || route.isBlank()
    val toHeadset = route == "headset" || isAuto
    val toPhone = route == "speaker" || isAuto
    val isConnected = state.playOnHeadsetConnect || toHeadset

    Row(
        Modifier
            .fillMaxWidth()
            .height(86.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.cardElevated)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Phone device icon
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (toPhone) colors.primaryAccent.copy(alpha = 0.22f) else colors.card),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_xvox_phone),
                    contentDescription = "Phone Speaker",
                    tint = if (toPhone) colors.primaryAccent else colors.mutedText,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Center visual indicator / link
        Canvas(Modifier.width(60.dp).height(24.dp)) {
            val y = size.height / 2
            drawLine(
                color = if (isAuto) colors.primaryAccent.copy(alpha = 0.7f) else colors.cardBorder,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 2.dp.toPx()
            )
            drawCircle(
                color = if (isAuto) colors.primaryAccent else colors.cardBorder,
                radius = 3.dp.toPx(),
                center = Offset(size.width / 2, y)
            )
        }

        // Headset device icon
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (toHeadset) colors.primaryAccent.copy(alpha = 0.22f) else colors.card),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_xvox_headset),
                    contentDescription = "Headset Output",
                    tint = if (toHeadset) colors.primaryAccent else colors.mutedText,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Notifications preview:
 * Realistic rich music reminder notification with 20-30 words rich description and quick action buttons.
 */
@Composable
fun NotifyPreview(state: SettingsState) {
    val colors = XvoxTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.cardElevated)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(colors.primaryAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_xvox_music_note),
                        contentDescription = null,
                        tint = colors.background,
                        modifier = Modifier.size(10.dp)
                    )
                }
                Text("XVOX · Music Reminder", color = colors.secondaryText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Text("Just now", color = colors.mutedText, fontSize = 10.sp)
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Time to reconnect with your rhythm",
            color = colors.primaryText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(3.dp))

        Text(
            text = "Take a breather and dive into your personal audio universe. Your favorite playlists and immersive 3D beats are ready whenever you want to escape into crystal clear rhythm.",
            color = colors.secondaryText,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.primaryAccent)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Play Music", color = colors.background, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surface)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Open Library", color = colors.primaryText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
