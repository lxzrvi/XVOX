package com.xvox.music.player.nowplaying

import android.net.Uri
import androidx.compose.animation.VectorConverter
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    // Never let an undecoded incoming layer take over the blurred canvas. The outgoing cover
    // remains fully painted until the next cover has a real bitmap, then the supplied pager
    // fraction is released through a short availability ramp instead of a blank-frame flash.
    var readyArtworkKeys by remember { mutableStateOf(emptySet<String>()) }
    fun markArtworkReady(uri: Uri?) {
        val key = uri?.toString() ?: return
        if (key !in readyArtworkKeys) readyArtworkKeys = readyArtworkKeys + key
    }
    val incomingKey = incomingArtwork?.toString()
    val incomingIsDecoded = incomingKey != null && incomingKey in readyArtworkKeys
    val incomingAvailability by animateFloatAsState(
        targetValue = if (pagerArtworkTransition && incomingIsDecoded) 1f else 0f,
        animationSpec = tween(150, easing = CubicBezierEasing(.22f, 0f, 0f, 1f)),
        label = "nowPlayingBackdropIncomingAvailability"
    )
    val displayedIncomingFraction = if (pagerArtworkTransition) {
        fraction * incomingAvailability
    } else {
        0f
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(displayed.value)
    ) {
        if (showCover) {
            // Keep one persistent primary artwork slot in composition at all times. It retains
            // the last decoded bitmap and morphs internally when its URI changes, which avoids
            // Crossfade creating a fresh blank image slot at a song boundary.
            PremiumBlurredCover(
                artwork = artwork,
                blur = coverBlur,
                alpha = 1f - displayedIncomingFraction,
                onReady = { markArtworkReady(artwork) }
            )
            if (pagerArtworkTransition) {
                // During drag/tap/natural completion, this is the pager's actual incoming cover.
                // It receives opacity only after its decoded bitmap is ready, so the persistent
                // primary never drops away to a card-colour/blank frame under a fast swipe.
                PremiumBlurredCover(
                    artwork = incomingArtwork,
                    blur = coverBlur,
                    alpha = displayedIncomingFraction,
                    onReady = { markArtworkReady(incomingArtwork) }
                )
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
    alpha: Float,
    onReady: (() -> Unit)? = null
) {
    artwork ?: return
    XvoxSongArtwork(
        artwork = artwork,
        requestSize = XvoxNowPlayingArtworkSize,
        contentScale = ContentScale.Crop,
        // The pager has already requested this exact original-quality key. Retaining the last
        // bitmap while an adjacent one resolves makes the two blurred layers crossfade instead of
        // sticking on an empty/placeholder frame mid-swipe.
        keepPreviousOnLoading = true,
        onArtworkReady = onReady,
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
