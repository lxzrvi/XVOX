package com.xvox.music.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import com.xvox.music.core.design.theme.XvoxLogoFont
import com.xvox.music.core.design.theme.XvoxTheme

const val XvoxGridArtworkSize =
    160

const val XvoxRecentArtworkSize =
    512

@Composable
fun XvoxSongArtwork(
    artwork: Any?,
    modifier: Modifier = Modifier,
    requestSize: Int =
        XvoxGridArtworkSize
) {
    val colors =
        XvoxTheme.colors

    val context =
        LocalContext.current

    if (
        artwork == null
    ) {
        Box(
            modifier =
                modifier.background(
                    colors.cardElevated
                ),
            contentAlignment =
                Alignment.Center
        ) {
            Text(
                text = "X",
                color =
                    colors.mutedText,
                fontFamily =
                    XvoxLogoFont,
                fontSize = 20.sp
            )
        }

        return
    }

    val request =
        remember(
            artwork,
            requestSize
        ) {
            ImageRequest
                .Builder(context)
                .data(artwork)
                .size(
                    requestSize,
                    requestSize
                )
                .precision(
                    Precision.INEXACT
                )
                .crossfade(false)
                .memoryCachePolicy(
                    CachePolicy.ENABLED
                )
                .diskCachePolicy(
                    CachePolicy.ENABLED
                )
                .networkCachePolicy(
                    CachePolicy.DISABLED
                )
                .build()
        }

    AsyncImage(
        model =
            request,
        contentDescription =
            null,
        contentScale =
            ContentScale.Crop,
        modifier =
            modifier.background(
                colors.cardElevated
            )
    )
}
