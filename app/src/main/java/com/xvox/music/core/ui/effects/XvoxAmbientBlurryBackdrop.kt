package com.xvox.music.core.ui.effects

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song

@Composable
fun XvoxAmbientBlurryBackdrop(
    song: Song?,
    modifier: Modifier = Modifier
) {
    val liveBlur = LocalLiveBlurEnabled.current
    val colors = XvoxTheme.colors

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        if (liveBlur && song?.artworkUri != null) {
            AnimatedContent(
                targetState = song.artworkUri,
                transitionSpec = {
                    fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(400))
                },
                modifier = Modifier.fillMaxSize(),
                label = "ambient_artwork_fade"
            ) { uri ->
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    Modifier.blur(64.dp)
                                } else {
                                    Modifier.blur(25.dp)
                                }
                            )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.52f),
                                Color.Black.copy(alpha = 0.76f),
                                colors.background.copy(alpha = 0.94f)
                            )
                        )
                    )
            )
        }
    }
}
