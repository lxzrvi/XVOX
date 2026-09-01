package com.xvox.music.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
            Arrangement.spacedBy(12.dp)
    ) {
        Row(
            horizontalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {
            SkeletonBlock(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape),
                color
            )

            SkeletonBlock(
                Modifier
                    .width(130.dp)
                    .height(30.dp)
                    .clip(
                        RoundedCornerShape(9.dp)
                    ),
                color
            )
        }

        SkeletonBlock(
            Modifier
                .width(150.dp)
                .height(22.dp)
                .clip(RoundedCornerShape(7.dp)),
            color
        )

        SkeletonBlock(
            Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(14.dp)),
            color
        )

        SkeletonBlock(
            Modifier
                .width(110.dp)
                .height(22.dp)
                .clip(RoundedCornerShape(7.dp)),
            color
        )

        repeat(5) {
            SkeletonBlock(
                Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .clip(
                        RoundedCornerShape(14.dp)
                    ),
                color
            )
        }
    }
}

@Composable
private fun SkeletonBlock(
    modifier: Modifier,
    color: androidx.compose.ui.graphics.Color
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier.background(color)
    )
}
