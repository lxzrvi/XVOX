package com.xvox.music.player.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
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
    playingSource: String = "All Songs"
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 14.dp, vertical = 2.dp)
            .height(52.dp),
        contentAlignment = Alignment.Center
    ) {
        // Down / Collapse Button on the left
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(42.dp)
                .clip(CircleShape)
                .background(colors.card.copy(alpha = 0.50f))
                .border(0.65.dp, colors.cardBorder, CircleShape)
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

        // Centered Header Source Info
        Column(
            modifier = Modifier
                .padding(horizontal = 56.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "PLAYING FROM",
                color = colors.secondaryText,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = playingSource,
                color = colors.primaryText,
                fontSize = 15.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }

        // Top right pill with Share & More
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .height(42.dp)
                .clip(RoundedCornerShape(21.dp))
                .background(colors.card.copy(alpha = 0.50f))
                .border(0.65.dp, colors.cardBorder, RoundedCornerShape(21.dp))
                .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onShare != null) {
                Icon(
                    painter = painterResource(R.drawable.ic_xvox_share),
                    contentDescription = "Share",
                    tint = colors.primaryText,
                    modifier = Modifier
                        .size(36.dp)
                        .xvoxPressScale(pressedScale = 0.90f) {
                            haptics.tap()
                            onShare()
                        }
                        .padding(8.dp)
                )
            }

            if (onMore != null) {
                Icon(
                    painter = painterResource(R.drawable.ic_xvox_more),
                    contentDescription = "More",
                    tint = colors.primaryText,
                    modifier = Modifier
                        .size(36.dp)
                        .xvoxPressScale(pressedScale = 0.90f) {
                            haptics.tap()
                            onMore()
                        }
                        .padding(8.dp)
                )
            }
        }
    }
}
