package com.xvox.music.player.nowplaying

import android.net.Uri
import androidx.compose.animation.VectorConverter
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
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
    /** Song identity stays non-null even when [artwork] is null, preserving a unique fallback. */
    artworkKey: String,
    artworkSeed: Int,
    incomingArtwork: Uri? = null,
    incomingArtworkKey: String? = null,
    incomingArtworkSeed: Int = 11,
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

    val showCover = style == XvoxNowPlayingBackgroundStyles.COVER
    val fraction = artworkTransitionFraction.coerceIn(0f, 1f)
    // Incoming identity—not URI—defines a transition. This lets two coverless songs blend their
    // distinct wave patterns, and avoids treating a shared URI as a new visual identity.
    val pagerArtworkTransition = showCover && incomingArtworkKey != null &&
        incomingArtworkKey != artworkKey && fraction > .0001f

    // An incoming layer is never allowed to take over before it is drawable. A decoded bitmap,
    // error fallback, or no-cover wave reports readiness; the outgoing cover stays visible until
    // then, eliminating blank/glitch frames during swipes and lyrics navigation.
    var readyArtworkKeys by remember { mutableStateOf(emptySet<String>()) }
    fun markArtworkReady(key: String?) {
        key ?: return
        if (key !in readyArtworkKeys) readyArtworkKeys = readyArtworkKeys + key
    }
    // Null artwork also waits for its generated wave bitmap. That lets a real cover ↔ no-cover
    // handoff blend rather than revealing the local surface for a frame.
    val incomingAvailable = incomingArtworkKey != null && incomingArtworkKey in readyArtworkKeys
    val incomingAvailability = remember(incomingArtworkKey) { Animatable(0f) }
    LaunchedEffect(incomingArtworkKey, pagerArtworkTransition, incomingAvailable) {
        when {
            !pagerArtworkTransition -> incomingAvailability.snapTo(0f)
            !incomingAvailable -> incomingAvailability.snapTo(0f)
            else -> {
                // Once drawable, bind the background directly to the pager fraction. The previous
                // delayed release ramp left a 0.5–0.9 layer behind a button Next/Previous motion,
                // then jumped to the settled primary cover on the final frame.
                incomingAvailability.snapTo(1f)
            }
        }
    }
    val displayedIncomingFraction = if (pagerArtworkTransition) {
        fraction * incomingAvailability.value
    } else {
        0f
    }
    // Once the pager has shown its incoming cover at the endpoint, the persistent primary slot
    // must adopt that decoded bitmap directly. Otherwise its own old→new morph would replay the
    // outgoing cover for one frame after the incoming layer is removed.
    var completedIncomingKey by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(pagerArtworkTransition, incomingArtworkKey) {
        if (pagerArtworkTransition) completedIncomingKey = incomingArtworkKey
    }
    val primaryAlreadyShownByPager = !pagerArtworkTransition && artworkKey == completedIncomingKey

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(displayed.value)
    ) {
        if (showCover) {
            // Keep one persistent primary slot in composition. XvoxSongArtwork retains/morphs a
            // decoded outgoing bitmap (or wave fallback) while the settled/lyrics song changes.
            PremiumBlurredCover(
                artwork = artwork,
                fallbackSeed = artworkSeed,
                skipDecodedMorph = primaryAlreadyShownByPager,
                blur = coverBlur,
                alpha = 1f - displayedIncomingFraction,
                onReady = { markArtworkReady(artworkKey) }
            )
            if (pagerArtworkTransition) {
                // The incoming layer uses the same progress as the foreground cover. Its identity
                // remains stable across Pager.currentPage's midpoint hand-off, so roles cannot
                // swap and flash halfway through a finger drag.
                PremiumBlurredCover(
                    artwork = incomingArtwork,
                    fallbackSeed = incomingArtworkSeed,
                    blur = coverBlur,
                    alpha = displayedIncomingFraction,
                    onReady = { markArtworkReady(incomingArtworkKey) }
                )
            }
            // A gentle tonal veil removes noisy blur artifacts and keeps controls readable while
            // retaining actual cover/wave colour—not an opaque unrelated replacement surface.
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
    fallbackSeed: Int,
    skipDecodedMorph: Boolean = false,
    blur: Float,
    alpha: Float,
    onReady: (() -> Unit)? = null
) {
    XvoxSongArtwork(
        artwork = artwork,
        requestSize = XvoxNowPlayingArtworkSize,
        contentScale = ContentScale.Crop,
        // The pager has already requested this exact original-quality key. Retaining the last
        // bitmap while an adjacent one resolves makes the two blurred layers crossfade instead of
        // sticking on an empty/placeholder frame mid-swipe.
        keepPreviousOnLoading = true,
        skipDecodedMorph = skipDecodedMorph,
        renderFallbackAsBitmap = true,
        fallbackSeed = fallbackSeed,
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
