package com.xvox.music.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import coil3.compose.SubcomposeAsyncImage
import com.xvox.music.core.design.theme.XvoxTheme

@Composable
fun SongArtwork(
    artwork: Any?,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors

    SubcomposeAsyncImage(
        model = artwork,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier,
        loading = {
            ArtworkFallback()
        },
        error = {
            ArtworkFallback()
        }
    )
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
            fontWeight = FontWeight.Bold
        )
    }
}
