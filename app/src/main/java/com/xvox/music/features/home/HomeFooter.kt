package com.xvox.music.features.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxLogoFont
import com.xvox.music.core.design.theme.XvoxTheme

@Composable
fun HomeFooter(
    modifier: Modifier = Modifier
) {
    val colors =
        XvoxTheme.colors

    Column(
        modifier = modifier,
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {
        Spacer(
            modifier =
                Modifier.weight(0.43f)
        )

        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(
                    Color.Black
                ),
            contentAlignment =
                Alignment.Center
        ) {
            Image(
                painter =
                    painterResource(
                        R.drawable.xvox
                    ),
                contentDescription = null,
                modifier =
                    Modifier.size(50.dp)
            )
        }

        Spacer(
            modifier =
                Modifier.height(5.dp)
        )

        Text(
            text = "XVOX",
            color =
                colors.secondaryText,
            fontFamily =
                XvoxLogoFont,
            fontSize = 13.sp,
            lineHeight = 15.sp
        )

        Text(
            text =
                "lxzrvi 2026",
            color =
                colors.mutedText,
            fontSize = 8.sp,
            lineHeight = 9.sp
        )

        Spacer(
            modifier =
                Modifier.weight(0.57f)
        )
    }
}
