package com.xvox.music.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.xvox.music.core.design.theme.XvoxTheme

@Composable
fun HomeSkeleton() {
    val color =
        XvoxTheme.colors.cardElevated

    Column(
        verticalArrangement =
            Arrangement.spacedBy(22.dp)
    ) {
        Row(
            horizontalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {
            Skeleton(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape),
                color = color
            )

            Skeleton(
                modifier = Modifier
                    .width(125.dp)
                    .height(30.dp)
                    .clip(
                        RoundedCornerShape(
                            9.dp
                        )
                    ),
                color = color
            )
        }

        Column(
            verticalArrangement =
                Arrangement.spacedBy(7.dp)
        ) {
            Skeleton(
                Modifier
                    .width(155.dp)
                    .height(21.dp)
                    .clip(
                        RoundedCornerShape(
                            6.dp
                        )
                    ),
                color
            )

            Skeleton(
                Modifier
                    .width(55.dp)
                    .height(11.dp)
                    .clip(
                        RoundedCornerShape(
                            5.dp
                        )
                    ),
                color
            )

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    )
            ) {
                repeat(2) {
                    Skeleton(
                        Modifier
                            .width(205.dp)
                            .height(118.dp)
                            .clip(
                                RoundedCornerShape(
                                    15.dp
                                )
                            ),
                        color
                    )
                }
            }
        }

        Column(
            verticalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            Skeleton(
                Modifier
                    .width(100.dp)
                    .height(21.dp)
                    .clip(
                        RoundedCornerShape(
                            6.dp
                        )
                    ),
                color
            )

            repeat(5) {
                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            7.dp
                        )
                ) {
                    repeat(4) {
                        Skeleton(
                            Modifier
                                .weight(1f)
                                .height(104.dp)
                                .clip(
                                    RoundedCornerShape(
                                        14.dp
                                    )
                                ),
                            color
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Skeleton(
    modifier: Modifier,
    color: androidx.compose.ui.graphics.Color
) {
    Box(
        modifier =
            modifier.background(
                color
            )
    )
}
