package com.xvox.music.features.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xvox.music.core.design.theme.XvoxTheme

@Composable
fun RecentPositionRail(
    songCount: Int,
    listState: LazyListState,
    itemWidth: Dp,
    itemGap: Dp,
    railWidth: Dp,
    modifier: Modifier = Modifier
) {
    val colors =
        XvoxTheme.colors

    val density =
        LocalDensity.current

    val safeCount =
        songCount.coerceAtLeast(1)

    val pillWidth =
        railWidth /
            safeCount

    val stridePx =
        with(density) {
            (
                itemWidth +
                    itemGap
                ).toPx()
        }

    val progress by remember(
        listState,
        songCount,
        stridePx
    ) {
        derivedStateOf {
            if (
                songCount <= 1 ||
                stridePx <= 0f
            ) {
                0f
            } else {
                val fractionalIndex =
                    listState
                        .firstVisibleItemIndex +
                        (
                            listState
                                .firstVisibleItemScrollOffset /
                                stridePx
                            )

                (
                    fractionalIndex /
                        (songCount - 1)
                            .toFloat()
                    )
                    .coerceIn(
                        0f,
                        1f
                    )
            }
        }
    }

    val targetX =
        (
            railWidth -
                pillWidth
            ) * progress

    val x by
        animateDpAsState(
            targetValue =
                targetX,
            animationSpec =
                spring(
                    dampingRatio = 0.92f,
                    stiffness = 900f
                ),
            label =
                "recentRail"
        )

    Box(
        modifier = modifier
            .width(railWidth)
            .height(3.dp)
            .background(
                colors.progressTrack,
                CircleShape
            )
    ) {
        Box(
            modifier = Modifier
                .offset(x = x)
                .width(pillWidth)
                .height(3.dp)
                .background(
                    colors.progressActive,
                    CircleShape
                )
        )
    }
}
