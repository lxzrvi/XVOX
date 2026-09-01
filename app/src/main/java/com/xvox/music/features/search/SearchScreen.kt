package com.xvox.music.features.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme

@Composable
fun SearchScreen() {
    val colors =
        XvoxTheme.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                colors.background
            )
            .windowInsetsPadding(
                WindowInsets.statusBars
            ),
        contentAlignment =
            Alignment.Center
    ) {
        Text(
            text = "Search",
            color =
                colors.primaryText,
            fontSize = 20.sp
        )
    }
}
