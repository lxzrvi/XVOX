package com.xvox.music.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.xvox.music.core.design.theme.XvoxLogoFont
import com.xvox.music.core.design.theme.XvoxTheme

@Composable
fun SongArtwork(
    artwork: Any?,
    modifier: Modifier = Modifier,
    requestSize: Int = 360
) {
    val colors = XvoxTheme.colors
    val context = LocalContext.current

    Box(
        modifier = modifier
            .background(colors.cardElevated),
        contentAlignment = Alignment.Center
    ) {
        if (artwork == null) {
            ArtworkFallback()
            return@Box
        }

        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(artwork)
                .size(requestSize)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .crossfade(false)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun ArtworkFallback() {
    val colors = XvoxTheme.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.cardElevated),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "X",
            color = colors.mutedText,
            fontFamily = XvoxLogoFont,
            fontSize = 22.sp
        )
    }
}
