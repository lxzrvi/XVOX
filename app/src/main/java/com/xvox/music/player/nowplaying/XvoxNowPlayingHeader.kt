package com.xvox.music.player.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme

@Composable
fun XvoxNowPlayingHeader(
    onClose: () -> Unit,
    onShare: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors =
        XvoxTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.statusBars
            )
            .padding(
                start = 14.dp,
                end = 14.dp,
                top = 10.dp
            )
            .height(48.dp)
    ) {
        NowPlayingHeaderButton(
            resource =
                R.drawable.ic_xvox_collapse,
            onClick =
                onClose,
            modifier =
                Modifier
                    .align(
                        Alignment.CenterStart
                    )
                    .size(42.dp)
        )

        androidx.compose.foundation.layout.Column(
            modifier =
                Modifier.align(
                    Alignment.Center
                ),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            Text(
                text =
                    "PLAYING FROM",
                color =
                    Color.White.copy(
                        alpha = 0.68f
                    ),
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                fontWeight =
                    FontWeight.SemiBold
            )

            Text(
                text = "All Songs",
                color = Color.White,
                fontSize = 15.sp,
                lineHeight = 17.sp,
                fontWeight =
                    FontWeight.Bold
            )
        }

        Row(
            modifier = Modifier
                .align(
                    Alignment.CenterEnd
                )
                .height(42.dp)
                .background(
                    colors.card.copy(
                        alpha = 0.48f
                    ),
                    RoundedCornerShape(
                        22.dp
                    )
                )
                .border(
                    0.65.dp,
                    Color.White.copy(
                        alpha = 0.14f
                    ),
                    RoundedCornerShape(
                        22.dp
                    )
                )
                .padding(
                    horizontal = 3.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            HeaderAction(
                R.drawable.ic_xvox_share,
                onShare
            )

            HeaderAction(
                R.drawable.ic_xvox_more,
                onMore
            )
        }
    }
}

@Composable
private fun NowPlayingHeaderButton(
    resource: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                Color.Black.copy(
                    alpha = 0.24f
                ),
                CircleShape
            )
            .border(
                0.65.dp,
                Color.White.copy(
                    alpha = 0.14f
                ),
                CircleShape
            )
            .clickable(
                interactionSource =
                    remember {
                        MutableInteractionSource()
                    },
                indication = null,
                onClick = onClick
            ),
        contentAlignment =
            Alignment.Center
    ) {
        Icon(
            painter =
                painterResource(
                    resource
                ),
            contentDescription = null,
            tint = Color.White,
            modifier =
                Modifier.size(21.dp)
        )
    }
}

@Composable
private fun HeaderAction(
    resource: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clickable(
                interactionSource =
                    remember {
                        MutableInteractionSource()
                    },
                indication = null,
                onClick = onClick
            ),
        contentAlignment =
            Alignment.Center
    ) {
        Icon(
            painter =
                painterResource(
                    resource
                ),
            contentDescription = null,
            tint = Color.White,
            modifier =
                Modifier.size(18.dp)
        )
    }
}
