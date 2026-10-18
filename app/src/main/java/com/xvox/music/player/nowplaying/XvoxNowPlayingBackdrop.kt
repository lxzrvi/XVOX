package com.xvox.music.player.nowplaying

import android.net.Uri
import androidx.compose.animation.VectorConverter
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.features.home.XvoxNowPlayingArtworkSize
import com.xvox.music.features.home.XvoxSongArtwork

/** One stable cover occurrence in the controlled Now Playing backdrop swipe pair. */
data class XvoxBackdropCover(
    val artwork: Uri?,
    val key: String,
    val seed: Int
)

/**
 * Now Playing's adaptive canvas plus its optional local Cover background. During a pager gesture,
 * the backdrop receives the pager's stable outgoing/incoming cover pair—not a transient raw page
 * frame—so reversals can fade only the current adjacent target without replaying a stale blur.
 * This stays local to Now Playing—not a global blur.
 */
@Composable
fun XvoxNowPlayingBackdrop(
    dominant: Color,
    artwork: Uri?,
    /** Song identity remains non-null even when [artwork] is null, preserving stable handoff keys. */
    artworkKey: String,
    artworkSeed: Int,
    style: String = XvoxNowPlayingBackgroundStyles.DEFAULT,
    coverBlur: Float = 24f,
    isCoverTransitionInProgress: Boolean = false,
    adjacentCover: XvoxBackdropCover? = null,
    adjacentFraction: () -> Float = { 0f },
    modifier: Modifier = Modifier
) {
    val target = xvoxNowPlayingBackgroundColor(style, dominant)
    val displayed = remember { Animatable(target, Color.VectorConverter(target.colorSpace)) }

    LaunchedEffect(target, isCoverTransitionInProgress) {
        if (isCoverTransitionInProgress) {
            // The pager supplies a stable physical blend every frame. Snap only to that already
            // blended colour, rather than letting an independent animation trail the finger.
            displayed.snapTo(target)
        } else {
            // Lyrics next/previous has no visible pager, so its cover-matched canvas receives a
            // deliberate smooth morph rather than an abrupt palette jump.
            displayed.animateTo(
                targetValue = target,
                animationSpec = tween<Color>(320, easing = CubicBezierEasing(.2f, 0f, 0f, 1f))
            )
        }
    }

    val showCoverTarget = style == XvoxNowPlayingBackgroundStyles.COVER
    // Style changes are committed only from the overflow sheet's Okay action. Keep the outgoing
    // local cover layer alive while the default canvas arrives (and vice versa), so confirmation
    // never flashes/jumps between the two entirely different background compositions.
    val coverLayerAlpha by animateFloatAsState(
        targetValue = if (showCoverTarget) 1f else 0f,
        animationSpec = tween(320, easing = CubicBezierEasing(.2f, 0f, 0f, 1f)),
        label = "nowPlayingCoverBackgroundAlpha"
    )
    val keepCoverLayer = showCoverTarget || coverLayerAlpha > .001f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(displayed.value)
    ) {
        if (keepCoverLayer) {
            // Keep a stable outgoing base plus at most one current adjacent cover. The pair comes
            // from the pager's stable base, so a reversal replaces/removes the old adjacent key
            // before it can be painted again; each layer owns only its own artwork identity.
            val layers = listOfNotNull(
                XvoxBackdropCover(artwork, artworkKey, artworkSeed),
                adjacentCover?.takeIf { it.key != artworkKey }
            )
            for (layer in layers) {
                key(layer.key) {
                    val isBase = layer.key == artworkKey
                    PremiumBlurredCover(
                        artwork = layer.artwork,
                        fallbackSeed = layer.seed,
                        blur = coverBlur,
                        alpha = {
                            if (isBase) coverLayerAlpha
                            else coverLayerAlpha * adjacentFraction().coerceIn(0f, 1f)
                        }
                    )
                }
            }
            // A gentle tonal veil removes noisy blur artifacts and keeps controls readable while
            // retaining actual cover/wave colour—not an opaque unrelated replacement surface.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                XvoxTheme.colors.background.copy(alpha = .22f * coverLayerAlpha),
                                dominant.copy(alpha = .12f * coverLayerAlpha),
                                XvoxTheme.colors.background.copy(alpha = .42f * coverLayerAlpha)
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
    fallbackSeed: Int,
    blur: Float,
    alpha: () -> Float
) {
    XvoxSongArtwork(
        artwork = artwork,
        requestSize = XvoxNowPlayingArtworkSize,
        contentScale = ContentScale.Crop,
        // Never retain an earlier song in this committed blur slot. The exact supplied fallback
        // is safer and more truthful than a stale next/previous cover while decoding completes.
        keepPreviousOnLoading = false,
        renderFallbackAsBitmap = true,
        fallbackSeed = fallbackSeed,
        modifier = Modifier
            .fillMaxSize()
            // Overscan eliminates exposed hard edges as the local blur samples its boundaries.
            .graphicsLayer {
                this.alpha = alpha().coerceIn(0f, 1f)
                scaleX = 1.15f
                scaleY = 1.15f
            }
            .blur(blur.coerceIn(12f, 50f).dp)
    )
}
