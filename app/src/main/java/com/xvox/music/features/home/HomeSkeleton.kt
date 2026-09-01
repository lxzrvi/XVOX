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
            Arrangement.spacedBy(20.dp)
    ) {
        Row(
            horizontalArrangement =
                Arrangement.spacedBy(11.dp)
        ) {
            Skeleton(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                color
            )

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(5.dp)
            ) {
                Skeleton(
                    Modifier
                        .width(125.dp)
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
                        .width(180.dp)
                        .height(11.dp)
                        .clip(
                            RoundedCornerShape(
                                5.dp
                            )
                        ),
                    color
                )
            }
        }

        Column(
            verticalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            Skeleton(
                Modifier
                    .width(150.dp)
                    .height(20.dp)
                    .clip(
                        RoundedCornerShape(
                            6.dp
                        )
                    ),
                color
            )

            Skeleton(
                Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(
                        RoundedCornerShape(
                            16.dp
                        )
                    ),
                color
            )
        }

        Column(
            verticalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            Skeleton(
                Modifier
                    .width(100.dp)
                    .height(20.dp)
                    .clip(
                        RoundedCornerShape(
                            6.dp
                        )
                    ),
                color
            )

            repeat(3) {
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
                                .height(125.dp)
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
