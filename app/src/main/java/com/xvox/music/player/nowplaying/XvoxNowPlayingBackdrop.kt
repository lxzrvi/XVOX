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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.xvox.music.core.design.theme.XvoxTheme

/**
 * Now Playing's adaptive canvas plus its optional local Cover background. The two artwork inputs
 * are supplied by the pager, so foreground cover, blurred background, palette, and lyric canvas
 * all travel on one live timeline. This is intentionally local to Now Playing—not a global blur.
 */
@Composable
fun XvoxNowPlayingBackdrop(
    dominant: Color,
    artwork: Uri?,
    incomingArtwork: Uri? = null,
    artworkTransitionFraction: Float = 0f,
    style: String = XvoxNowPlayingBackgroundStyles.DEFAULT,
    coverBlur: Float = 24f,
    isCoverTransitionInProgress: Boolean = false,
    modifier: Modifier = Modifier
) {
    val target = xvoxNowPlayingBackgroundColor(style, dominant)
    val displayed = remember { Animatable(target, Color.VectorConverter(target.colorSpace)) }

    LaunchedEffect(target, isCoverTransitionInProgress) {
        if (isCoverTransitionInProgress) {
            // Pager frames already provide a precise blend. Do not make the background trail the
            // actual artwork while someone swipes.
            displayed.snapTo(target)
        } else {
            displayed.animateTo(
                targetValue = target,
                animationSpec = tween<Color>(320, easing = CubicBezierEasing(.2f, 0f, 0f, 1f))
            )
        }
    }

    val showCover = style == XvoxNowPlayingBackgroundStyles.COVER && artwork != null
    val fraction = artworkTransitionFraction.coerceIn(0f, 1f)
    val pagerArtworkTransition = showCover && incomingArtwork != null &&
        incomingArtwork != artwork && fraction > .0001f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(displayed.value)
    ) {
        if (showCover) {
            if (pagerArtworkTransition) {
                // During drag/tap/natural completion, blend the same two decoded-art identities
                // the pager is showing. The outgoing layer stays visible while the next one is
                // resolving, preventing a flat or abrupt Cover backdrop.
                PremiumBlurredCover(artwork = artwork, blur = coverBlur, alpha = 1f - fraction)
                PremiumBlurredCover(incomingArtwork, blur = coverBlur, alpha = fraction)
            } else {
                // Lyrics/direct navigation has no visible pager. Preserve a tasteful cover-to-
                // cover fade for that path while normal pager movement uses the live layers above.
                Crossfade(
                    targetState = artwork,
                    animationSpec = tween(300, easing = CubicBezierEasing(.22f, 0f, 0f, 1f)),
                    label = "nowPlayingCoverBackdropCrossfade"
                ) { cover ->
                    PremiumBlurredCover(cover, blur = coverBlur, alpha = 1f)
                }
            }
            // A gentle tonal veil removes noisy blur artifacts and keeps controls readable while
            // retaining actual cover color—not an opaque unrelated replacement surface.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                XvoxTheme.colors.background.copy(alpha = .22f),
                                dominant.copy(alpha = .12f),
                                XvoxTheme.colors.background.copy(alpha = .42f)
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun PremiumBlurredCover(
    artwork: Uri?,
    blur: Float,
    alpha: Float
) {
    artwork ?: return
    AsyncImage(
        model = artwork,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
            // Overscan eliminates exposed hard edges as the local blur samples its boundaries.
            .graphicsLayer {
                this.alpha = alpha.coerceIn(0f, 1f)
                scaleX = 1.15f
                scaleY = 1.15f
            }
            .blur(blur.coerceIn(12f, 50f).dp)
    )
}
