package com.xvox.music.player.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.effects.xvoxLiveBackdropBlur
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics

@Composable
fun XvoxNowPlayingHeader(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onShare: (() -> Unit)? = null,
    onMore: (() -> Unit)? = null,
    playingSource: String = "All Songs",
    useSystemInsets: Boolean = true,
    /** Optional side for the compact share/options group; collapse remains at its stable edge. */
    optionsGroupSide: String = "right",
    /** Independent Customize multiplier for header/right-pill controls. */
    controlsAlpha: Float = 1f,
    /** Shared lyrics-box material alpha used by all painted header surfaces. */
    surfaceAlpha: Float = .27f,
    /** True in-place Now Playing editor chrome; no secondary customization sheet is shown. */
    customizeMode: Boolean = false,
    onCustomizeCancel: (() -> Unit)? = null,
    onCustomizeReset: (() -> Unit)? = null,
    onCustomizeOkay: (() -> Unit)? = null
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (useSystemInsets) Modifier.windowInsetsPadding(WindowInsets.statusBars) else Modifier)
            .padding(horizontal = if (useSystemInsets) 14.dp else 4.dp, vertical = if (useSystemInsets) 4.dp else 2.dp)
            .height(46.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(40.dp)
                .graphicsLayer(alpha = controlsAlpha.coerceIn(0f, 1f))
                .clip(CircleShape)
                .xvoxLiveBackdropBlur(CircleShape, radius = 16, applyInDefault = true)
                .background(colors.card.copy(alpha = surfaceAlpha.coerceIn(0f, 1f)))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        haptics.tap()
                        if (customizeMode) onCustomizeCancel?.invoke() else onClose()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(if (customizeMode) R.drawable.ic_xvox_close else R.drawable.ic_xvox_collapse),
                contentDescription = if (customizeMode) "Cancel customize" else "Collapse Player",
                tint = colors.primaryText,
                modifier = Modifier.size(20.dp)
            )
        }

        val isArtistSource = playingSource.startsWith("Artist · ") ||
                playingSource.startsWith("Playing by ") ||
                playingSource.startsWith("Artist: ")
        val sourceTitle = if (isArtistSource) "PLAYING BY" else "PLAYING FROM"
        val displaySource = when {
            playingSource.startsWith("Artist · ") -> playingSource.removePrefix("Artist · ")
            playingSource.startsWith("Playing by ") -> playingSource.removePrefix("Playing by ")
            playingSource.startsWith("Artist: ") -> playingSource.removePrefix("Artist: ")
            playingSource.startsWith("Playing from ") -> playingSource.removePrefix("Playing from ")
            else -> playingSource
        }

        // This is an actual screen-centred column, not the leftover width between edge buttons.
        // Symmetric padding protects long text from either side while its visual centre remains
        // at the physical display centre in normal and Customize modes.
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 94.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (customizeMode) {
                Text(
                    text = "Customize",
                    color = colors.primaryText,
                    fontSize = 17.5.sp,
                    lineHeight = 19.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = sourceTitle,
                    color = colors.primaryText.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    letterSpacing = 1.3.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = displaySource,
                    color = colors.primaryText,
                    fontSize = 17.5.sp,
                    lineHeight = 19.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Share and overflow live in one compact pill so the header retains a single clear
        // right-side target while still exposing the complete Now Playing control editor.
        if (customizeMode) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .xvoxLiveBackdropBlur(RoundedCornerShape(20.dp), radius = 16, applyInDefault = true)
                    .background(colors.card.copy(alpha = surfaceAlpha.coerceIn(0f, 1f)))
                    .padding(horizontal = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reset",
                    color = colors.primaryText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(horizontal = 7.dp, vertical = 10.dp)
                        .xvoxPressScale { onCustomizeReset?.invoke() }
                )
                Text(
                    text = "Okay",
                    color = colors.primaryAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(horizontal = 7.dp, vertical = 10.dp)
                        .xvoxPressScale { onCustomizeOkay?.invoke() }
                )
            }
        } else if (onShare != null || onMore != null) {
            Row(
                modifier = Modifier
                    .align(if (optionsGroupSide == "left") Alignment.CenterStart else Alignment.CenterEnd)
                    // Left-side placement starts after the always-present collapse button.
                    .then(if (optionsGroupSide == "left") Modifier.padding(start = 46.dp) else Modifier)
                    .height(40.dp)
                    .graphicsLayer(alpha = controlsAlpha.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(20.dp))
                    .xvoxLiveBackdropBlur(RoundedCornerShape(20.dp), radius = 16, applyInDefault = true)
                    .background(colors.card.copy(alpha = surfaceAlpha.coerceIn(0f, 1f)))
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onShare != null) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .xvoxPressScale(pressedScale = 0.90f) {
                                haptics.tap()
                                onShare()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_xvox_share),
                            contentDescription = "Share",
                            tint = colors.primaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                if (onMore != null) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .xvoxPressScale(pressedScale = 0.90f) {
                                haptics.tap()
                                onMore()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_xvox_more),
                            contentDescription = "Now Playing options",
                            tint = colors.primaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
