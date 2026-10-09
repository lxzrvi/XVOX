package com.xvox.music.player.nowplaying.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.xvox.music.core.design.theme.XvoxLogoFont
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.chrome.XvoxChromeStyle
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.features.settings.components.XvoxContinuousSlider
import kotlin.math.roundToInt

/**
 * Full-screen, safe dummy clone for the bottom of Now Playing. It uses the same persisted offsets
 * as the real player but never starts playback actions while editing. Every visible text/bar/button
 * group can be long-pressed and dragged, so the canvas remains usable even when groups overlap.
 */
@Composable
fun NowPlayingLayoutCustomizer(
    chrome: XvoxChromeStyle,
    onChromeChange: (XvoxChromeStyle) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    var draft by remember { mutableStateOf(chrome) }
    val latestDraft by rememberUpdatedState(draft)

    fun update(next: XvoxChromeStyle, persist: Boolean = false) {
        draft = next
        if (persist) onChromeChange(next)
    }

    fun persist() = onChromeChange(latestDraft)

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(80f)
            .background(colors.background)
            .semantics { contentDescription = "Customize Now Playing layout" }
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 18.dp, end = 12.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Customize", color = colors.primaryText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Long-press any control, bar, or text and drag it anywhere in the bottom box.",
                        color = colors.secondaryText,
                        fontSize = 10.sp,
                        lineHeight = 13.sp
                    )
                }
                Text(
                    "Reset",
                    color = colors.primaryAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(10.dp)
                        .xvoxPressScale {
                            val reset = latestDraft.resetNowPlayingLayout()
                            update(reset, persist = true)
                        }
                )
                Text(
                    "Done",
                    color = colors.primaryAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(10.dp)
                        .xvoxPressScale {
                            persist()
                            onClose()
                        }
                )
            }

            // Dummy cover retains the spatial feel of the real full player while only the bottom
            // canvas is editable. No user song artwork is decoded here, so opening Customize is
            // deterministic and inexpensive.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(colors.cardElevated.copy(alpha = .72f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "XVOX",
                    color = colors.primaryAccent.copy(alpha = .84f),
                    fontFamily = XvoxLogoFont,
                    fontSize = 36.sp,
                    letterSpacing = 5.sp
                )
            }

            XvoxBottomLayoutCanvas(
                chrome = draft,
                onDraftChange = { next -> update(next) },
                onDragFinished = ::persist,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(354.dp)
            )

            NowPlayingTransparencyEditor(
                chrome = draft,
                onChange = { update(it, persist = true) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun XvoxBottomLayoutCanvas(
    chrome: XvoxChromeStyle,
    onDraftChange: (XvoxChromeStyle) -> Unit,
    onDragFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    val panelShape = RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp)

    Box(
        modifier = modifier
            .clip(panelShape)
            .background(colors.cardElevated.copy(alpha = .88f * chrome.nowPlayingBottomBoxAlpha.coerceIn(0f, 1f)))
            .padding(horizontal = 14.dp, vertical = 16.dp)
    ) {
        XvoxLayoutDraggableToken(
            label = "Song title · Artist",
            x = chrome.nowPlayingMetadataOffsetX,
            y = chrome.nowPlayingMetadataOffsetY,
            onChange = { x, y -> onDraftChange(chrome.copy(nowPlayingMetadataOffsetX = x, nowPlayingMetadataOffsetY = y)) },
            onDragFinished = onDragFinished,
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Column {
                Text("Song title", color = colors.primaryText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("Artist", color = colors.secondaryText, fontSize = 11.sp)
            }
        }

        XvoxLayoutDraggableToken(
            label = "Progress bar",
            x = chrome.nowPlayingProgressOffsetX,
            y = chrome.nowPlayingProgressOffsetY,
            onChange = { x, y -> onDraftChange(chrome.copy(nowPlayingProgressOffsetX = x, nowPlayingProgressOffsetY = y)) },
            onDragFinished = onDragFinished,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 72.dp)
        ) {
            Column(Modifier.width(235.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(colors.secondaryText.copy(alpha = .28f))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(.48f)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(colors.primaryAccent)
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("1:24", color = colors.secondaryText, fontSize = 9.sp)
                    Text("3:48", color = colors.secondaryText, fontSize = 9.sp)
                }
            }
        }

        XvoxLayoutDraggableToken(
            label = "Timer · Queue · Info",
            x = chrome.nowPlayingUtilityOffsetX,
            y = chrome.nowPlayingUtilityOffsetY,
            onChange = { x, y -> onDraftChange(chrome.copy(nowPlayingUtilityOffsetX = x, nowPlayingUtilityOffsetY = y)) },
            onDragFinished = onDragFinished,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 90.dp)
                .graphicsLayer(alpha = chrome.nowPlayingControlsAlpha.coerceIn(0f, 1f))
        ) {
            XvoxControlPill("◷   ≡   i")
        }

        XvoxLayoutDraggableToken(
            label = "Action pill",
            x = chrome.nowPlayingActionsOffsetX,
            y = chrome.nowPlayingActionsOffsetY,
            onChange = { x, y -> onDraftChange(chrome.copy(nowPlayingActionsOffsetX = x, nowPlayingActionsOffsetY = y)) },
            onDragFinished = onDragFinished,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 90.dp)
                .graphicsLayer(alpha = chrome.nowPlayingControlsAlpha.coerceIn(0f, 1f))
        ) {
            XvoxControlPill("★   ♡")
        }

        XvoxLayoutDraggableToken(
            label = "Shuffle · Repeat",
            x = chrome.nowPlayingShuffleRepeatOffsetX,
            y = chrome.nowPlayingShuffleRepeatOffsetY,
            onChange = { x, y -> onDraftChange(chrome.copy(nowPlayingShuffleRepeatOffsetX = x, nowPlayingShuffleRepeatOffsetY = y)) },
            onDragFinished = onDragFinished,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 26.dp)
                .graphicsLayer(alpha = chrome.nowPlayingControlsAlpha.coerceIn(0f, 1f))
        ) {
            Text("⇄     ↻", color = colors.primaryText, fontSize = 18.sp)
        }

        XvoxLayoutDraggableToken(
            label = "Previous",
            x = chrome.nowPlayingPreviousOffsetX,
            y = chrome.nowPlayingPreviousOffsetY,
            onChange = { x, y -> onDraftChange(chrome.copy(nowPlayingPreviousOffsetX = x, nowPlayingPreviousOffsetY = y)) },
            onDragFinished = onDragFinished,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(end = 90.dp, bottom = 18.dp)
                .graphicsLayer(alpha = chrome.nowPlayingControlsAlpha.coerceIn(0f, 1f))
        ) {
            Text("◀◀", color = colors.primaryText, fontSize = 19.sp)
        }

        XvoxLayoutDraggableToken(
            label = "Play",
            x = chrome.nowPlayingPlayOffsetX,
            y = chrome.nowPlayingPlayOffsetY,
            onChange = { x, y -> onDraftChange(chrome.copy(nowPlayingPlayOffsetX = x, nowPlayingPlayOffsetY = y)) },
            onDragFinished = onDragFinished,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .graphicsLayer(alpha = chrome.nowPlayingPlayAlpha.coerceIn(0f, 1f))
        ) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(colors.primaryAccent),
                contentAlignment = Alignment.Center
            ) {
                Text("▶", color = colors.background, fontSize = 19.sp)
            }
        }

        XvoxLayoutDraggableToken(
            label = "Next",
            x = chrome.nowPlayingNextOffsetX,
            y = chrome.nowPlayingNextOffsetY,
            onChange = { x, y -> onDraftChange(chrome.copy(nowPlayingNextOffsetX = x, nowPlayingNextOffsetY = y)) },
            onDragFinished = onDragFinished,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 90.dp, bottom = 18.dp)
                .graphicsLayer(alpha = chrome.nowPlayingControlsAlpha.coerceIn(0f, 1f))
        ) {
            Text("▶▶", color = colors.primaryText, fontSize = 19.sp)
        }

        XvoxLayoutDraggableToken(
            label = "XVOX label",
            x = chrome.nowPlayingBrandOffsetX,
            y = chrome.nowPlayingBrandOffsetY,
            onChange = { x, y -> onDraftChange(chrome.copy(nowPlayingBrandOffsetX = x, nowPlayingBrandOffsetY = y)) },
            onDragFinished = onDragFinished,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 2.dp)
        ) {
            Text("XVOX", color = colors.secondaryText, fontFamily = XvoxLogoFont, fontSize = 10.sp, letterSpacing = 2.sp)
        }
    }
}

@Composable
private fun XvoxControlPill(text: String) {
    val colors = XvoxTheme.colors
    Text(
        text = text,
        color = colors.primaryText,
        fontSize = 16.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(21.dp))
            .background(colors.card.copy(alpha = .58f))
            .padding(horizontal = 11.dp, vertical = 8.dp)
    )
}

@Composable
private fun XvoxLayoutDraggableToken(
    label: String,
    x: Float,
    y: Float,
    onChange: (Float, Float) -> Unit,
    onDragFinished: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val latestX by rememberUpdatedState(x)
    val latestY by rememberUpdatedState(y)
    val latestChange by rememberUpdatedState(onChange)
    val latestFinish by rememberUpdatedState(onDragFinished)

    Box(
        modifier = modifier
            .offset { IntOffset(with(density) { latestX.dp.toPx() }.roundToInt(), with(density) { latestY.dp.toPx() }.roundToInt()) }
            .pointerInput(label) {
                detectDragGesturesAfterLongPress(
                    onDrag = { change, amount ->
                        change.consume()
                        val nextX = (latestX + with(density) { amount.x.toDp().value }).coerceIn(-220f, 220f)
                        val nextY = (latestY + with(density) { amount.y.toDp().value }).coerceIn(-260f, 260f)
                        latestChange(nextX, nextY)
                    },
                    onDragEnd = { latestFinish() },
                    onDragCancel = { latestFinish() }
                )
            }
            .semantics { contentDescription = "$label. Long press and drag to reposition." }
    ) {
        content()
    }
}

@Composable
private fun NowPlayingTransparencyEditor(
    chrome: XvoxChromeStyle,
    onChange: (XvoxChromeStyle) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text("Transparency", color = colors.primaryAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        XvoxLayoutTransparencySlider(
            "Bottom box",
            1f - chrome.nowPlayingBottomBoxAlpha,
            { onChange(chrome.copy(nowPlayingBottomBoxAlpha = 1f - it)) }
        )
        XvoxLayoutTransparencySlider(
            "Controls and utility pills",
            1f - chrome.nowPlayingControlsAlpha,
            { onChange(chrome.copy(nowPlayingControlsAlpha = 1f - it)) }
        )
        XvoxLayoutTransparencySlider(
            "Play button",
            1f - chrome.nowPlayingPlayAlpha,
            { onChange(chrome.copy(nowPlayingPlayAlpha = 1f - it)) }
        )
    }
}

@Composable
private fun XvoxLayoutTransparencySlider(label: String, value: Float, onChange: (Float) -> Unit) {
    val colors = XvoxTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = colors.secondaryText, fontSize = 10.sp)
            Text("${(value.coerceIn(0f, 1f) * 100f).roundToInt()}%", color = colors.primaryAccent, fontSize = 10.sp)
        }
        XvoxContinuousSlider(
            value = value.coerceIn(0f, 1f),
            onValueChange = { onChange(it.coerceIn(0f, 1f)) },
            valueRange = 0f..1f,
            defaultValue = 0f,
            contentDescription = "$label transparency"
        )
    }
}
