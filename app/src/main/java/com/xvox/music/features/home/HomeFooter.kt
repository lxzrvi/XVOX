package com.xvox.music.features.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxLogoFont
import com.xvox.music.core.design.theme.XvoxTheme

@Composable
fun HomeFooter() {
    val colors =
        XvoxTheme.colors

    Column(
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.Center
    ) {
        Image(
            painter =
                painterResource(
                    R.drawable.xvox
                ),
            contentDescription = null,
            colorFilter =
                ColorFilter.tint(
                    colors.mutedText
                ),
            modifier =
                Modifier.size(18.dp)
        )

        Spacer(
            Modifier.height(8.dp)
        )

        Text(
            text = "XVOX",
            color =
                colors.secondaryText,
            fontFamily =
                XvoxLogoFont,
            fontSize = 13.sp
        )

        Text(
            text = "lxzrvi 2026",
            color =
                colors.mutedText,
            fontSize = 8.sp
        )
    }
}
