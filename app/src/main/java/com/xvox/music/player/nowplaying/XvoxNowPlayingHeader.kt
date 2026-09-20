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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics

@Composable
fun XvoxNowPlayingHeader(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onShare: (() -> Unit)? = null,
    onMore: (() -> Unit)? = null,
    playingSource: String = "All Songs",
    useSystemInsets: Boolean = true
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
                .clip(CircleShape)
                .background(colors.card.copy(alpha = 0.35f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        haptics.tap()
                        onClose()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_xvox_collapse),
                contentDescription = "Collapse Player",
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

        Column(
            modifier = Modifier
                .padding(horizontal = 52.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = sourceTitle,
                color = colors.primaryText.copy(alpha = 0.85f),
                fontSize = 10.5.sp,
                letterSpacing = 1.3.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = displaySource,
                color = colors.primaryText,
                fontSize = 16.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .height(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(colors.card.copy(alpha = 0.35f))
                .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onShare != null) {
                Icon(
                    painter = painterResource(R.drawable.ic_xvox_share),
                    contentDescription = "Share",
                    tint = colors.primaryText,
                    modifier = Modifier
                        .size(34.dp)
                        .xvoxPressScale(pressedScale = 0.90f) {
                            haptics.tap()
                            onShare()
                        }
                        .padding(7.dp)
                )
            }

            if (onMore != null) {
                Icon(
                    painter = painterResource(R.drawable.ic_xvox_more),
                    contentDescription = "More",
                    tint = colors.primaryText,
                    modifier = Modifier
                        .size(34.dp)
                        .xvoxPressScale(pressedScale = 0.90f) {
                            haptics.tap()
                            onMore()
                        }
                        .padding(7.dp)
                )
            }
        }
    }
}
