package com.xvox.music.player.nowplaying

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R

@Composable
fun XvoxNowPlayingHeader(
    onClose: () -> Unit,
    onShare: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.statusBars
            )
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp
            )
            .height(44.dp),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        val closeInteraction =
            remember {
                MutableInteractionSource()
            }

        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    Color.White.copy(
                        alpha = 0.12f
                    ),
                    CircleShape
                )
                .clickable(
                    interactionSource =
                        closeInteraction,
                    indication = null,
                    onClick = onClose
                ),
            contentAlignment =
                Alignment.Center
        ) {
            Text(
                text = "⌄",
                color = Color.White,
                fontSize = 23.sp,
                lineHeight = 23.sp
            )
        }

        Column(
            modifier =
                Modifier.weight(1f),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            Text(
                text =
                    "PLAYING FROM",
                color =
                    Color.White.copy(
                        alpha = 0.65f
                    ),
                fontSize = 10.sp,
                letterSpacing =
                    1.2.sp,
                fontWeight =
                    FontWeight.SemiBold
            )

            Text(
                text =
                    "All Songs",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight =
                    FontWeight.Bold
            )
        }

        Row(
            modifier = Modifier
                .height(36.dp)
                .background(
                    Color.White.copy(
                        alpha = 0.12f
                    ),
                    RoundedCornerShape(
                        20.dp
                    )
                )
                .padding(
                    horizontal = 4.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            HeaderIcon(
                resource =
                    R.drawable
                        .ic_xvox_share,
                onClick =
                    onShare
            )

            HeaderIcon(
                resource =
                    R.drawable
                        .ic_xvox_more,
                onClick =
                    onMore
            )
        }
    }
}

@Composable
private fun HeaderIcon(
    resource: Int,
    onClick: () -> Unit
) {
    val interaction =
        remember {
            MutableInteractionSource()
        }

    Box(
        modifier = Modifier
            .size(32.dp)
            .clickable(
                interactionSource =
                    interaction,
                indication = null,
                onClick =
                    onClick
            ),
        contentAlignment =
            Alignment.Center
    ) {
        Icon(
            painter =
                painterResource(
                    resource
                ),
            contentDescription =
                null,
            tint = Color.White,
            modifier =
                Modifier.size(
                    16.dp
                )
        )
    }
}
