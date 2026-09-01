package com.xvox.music.features.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.xvox.music.core.design.theme.XvoxTheme

@Composable
fun HomeSkeleton(
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors

    val transition =
        rememberInfiniteTransition(
            label = "homeSkeleton"
        )

    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.85f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(850),
                repeatMode =
                    RepeatMode.Reverse
            ),
        label = "homeSkeletonAlpha"
    )

    val block =
        colors.cardElevated.copy(
            alpha = alpha
        )

    Column(
        modifier = modifier
            .padding(top = 18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(
                    horizontal = 18.dp
                ),
            verticalAlignment =
                androidx.compose.ui.Alignment
                    .CenterVertically
        ) {
            SkeletonBlock(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape),
                color = block
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start = 10.dp
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(
                        4.dp
                    )
            ) {
                SkeletonBlock(
                    modifier = Modifier
                        .width(72.dp)
                        .height(17.dp)
                        .clip(
                            RoundedCornerShape(
                                5.dp
                            )
                        ),
                    color = block
                )

                SkeletonBlock(
                    modifier = Modifier
                        .width(145.dp)
                        .height(9.dp)
                        .clip(
                            RoundedCornerShape(
                                5.dp
                            )
                        ),
                    color = block
                )
            }

            SkeletonBlock(
                modifier = Modifier
                    .width(80.dp)
                    .height(42.dp)
                    .clip(
                        RoundedCornerShape(
                            22.dp
                        )
                    ),
                color = block
            )
        }

        Column(
            modifier = Modifier
                .padding(
                    top = 24.dp,
                    start = 12.dp
                )
        ) {
            SkeletonBlock(
                modifier = Modifier
                    .width(105.dp)
                    .height(21.dp)
                    .clip(
                        RoundedCornerShape(
                            5.dp
                        )
                    ),
                color = block
            )

            SkeletonBlock(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .width(75.dp)
                    .height(9.dp)
                    .clip(
                        RoundedCornerShape(
                            4.dp
                        )
                    ),
                color = block
            )
        }

        androidx.compose.foundation.layout.BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        ) {
            val edge = 6.dp
            val gap = 6.dp

            val cardWidth =
                (
                    maxWidth -
                        edge * 2 -
                        gap * 3
                    ) / 4

            val cardHeight =
                cardWidth + 34.dp

            Column(
                modifier = Modifier
                    .padding(
                        horizontal = edge
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(
                        gap
                    )
            ) {
                repeat(3) {
                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(
                                gap
                            )
                    ) {
                        repeat(4) {
                            SkeletonBlock(
                                modifier = Modifier
                                    .width(
                                        cardWidth
                                    )
                                    .height(
                                        cardHeight
                                    )
                                    .clip(
                                        RoundedCornerShape(
                                            11.dp
                                        )
                                    ),
                                color = block
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier.padding(
                start = 12.dp,
                end = 12.dp,
                top = 26.dp
            )
        ) {
            SkeletonBlock(
                modifier = Modifier
                    .width(145.dp)
                    .height(21.dp)
                    .clip(
                        RoundedCornerShape(
                            5.dp
                        )
                    ),
                color = block
            )

            SkeletonBlock(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .width(55.dp)
                    .height(9.dp)
                    .clip(
                        RoundedCornerShape(
                            4.dp
                        )
                    ),
                color = block
            )
        }

        SkeletonBlock(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 6.dp,
                    end = 6.dp,
                    top = 10.dp
                )
                .height(122.dp)
                .clip(
                    RoundedCornerShape(
                        16.dp
                    )
                ),
            color = block
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 9.dp),
            horizontalArrangement =
                Arrangement.Center
        ) {
            SkeletonBlock(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape),
                color = block
            )
        }
    }
}

@Composable
private fun SkeletonBlock(
    modifier: Modifier,
    color: androidx.compose.ui.graphics.Color
) {
    Box(
        modifier =
            modifier.background(color)
    )
}
