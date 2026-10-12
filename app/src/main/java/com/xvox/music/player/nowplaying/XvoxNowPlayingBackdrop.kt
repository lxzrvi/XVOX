package com.xvox.music.player.nowplaying

import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.VectorConverter
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.xvox.music.core.design.theme.XvoxTheme

/**
 * Now Playing's ordinary adaptive canvas plus its optional local Cover background. Cover blur is
 * intentionally scoped to this one artwork image; it does not capture or blur the surrounding UI.
 */
@Composable
fun XvoxNowPlayingBackdrop(
    dominant: Color,
    artwork: Uri?,
    style: String = XvoxNowPlayingBackgroundStyles.DEFAULT,
    coverBlur: Float = 24f,
    isCoverTransitionInProgress: Boolean = false,
    modifier: Modifier = Modifier
) {
    val target = xvoxNowPlayingBackgroundColor(style, dominant)
    val displayed = remember { Animatable(target, Color.VectorConverter(target.colorSpace)) }

    LaunchedEffect(target, isCoverTransitionInProgress) {
        if (isCoverTransitionInProgress) {
            displayed.snapTo(target)
        } else {
            displayed.animateTo(
                targetValue = target,
                animationSpec = tween<Color>(320, easing = CubicBezierEasing(.2f, 0f, 0f, 1f))
            )
        }
    }

    val showCover = style == XvoxNowPlayingBackgroundStyles.COVER && artwork != null
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(displayed.value)
    ) {
        if (showCover) {
            // The backdrop has its own artwork layer, so give it the same gentle cover-to-cover
            // crossfade as the foreground artwork instead of letting Coil replace it abruptly.
            // This remains local to Now Playing's Cover mode; it is not an app-wide blur surface.
            Crossfade(
                targetState = artwork,
                animationSpec = tween(300, easing = CubicBezierEasing(.22f, 0f, 0f, 1f)),
                label = "nowPlayingCoverBackdropCrossfade"
            ) { cover ->
                AsyncImage(
                    model = cover,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        // Overscan prevents a blurred image from exposing hard edges at the canvas.
                        .graphicsLayer { scaleX = 1.08f; scaleY = 1.08f }
                        .blur(coverBlur.coerceIn(12f, 50f).dp)
                )
            }
            // Keep text and controls readable over every album art palette without reverting to
            // a global glass material or an opaque, unrelated background surface.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(XvoxTheme.colors.background.copy(alpha = .30f))
            )
        }
    }
}
