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
    /** Song identity remains non-null even when [artwork] is null, preserving stable handoff keys. */
    artworkKey: String,
    artworkSeed: Int,
    incomingArtwork: Uri? = null,
    incomingArtworkKey: String? = null,
    incomingArtworkSeed: Int = 11,
    artworkTransitionFraction: Float = 0f,
    /** A pager endpoint that was already painted at full opacity before the persistent slot updates. */
    alreadyPresentedArtworkKey: String? = null,
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
    val fraction = artworkTransitionFraction.coerceIn(0f, 1f)
    // Incoming identity—not URI—defines a transition. This preserves a stable fallback handoff
    // between coverless songs and avoids treating a shared URI as a new visual identity.
    val pagerArtworkTransition = showCoverTarget && incomingArtworkKey != null &&
        incomingArtworkKey != artworkKey && fraction > .0001f

    // An incoming layer is never allowed to take over before it is drawable. A decoded bitmap,
    // error fallback, or no-cover default art reports readiness; the outgoing cover stays visible until
    // then, eliminating blank/glitch frames during swipes and lyrics navigation.
    var readyArtworkKeys by remember { mutableStateOf(emptySet<String>()) }
    fun markArtworkReady(key: String?) {
        key ?: return
        if (key !in readyArtworkKeys) readyArtworkKeys = readyArtworkKeys + key
    }
    // Null artwork also waits for its bundled default-cover bitmap. That lets a real cover ↔
    // no-cover handoff blend rather than revealing the local surface for a frame.
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
    // The pager state holder records a completed endpoint synchronously between its moving and
    // settled emissions. This avoids the former post-composition SideEffect race where a Next or
    // Previous settle could briefly replay the opposite background before the primary slot caught up.
    val primaryAlreadyShownByPager = !pagerArtworkTransition &&
        artworkKey == alreadyPresentedArtworkKey

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(displayed.value)
    ) {
        if (keepCoverLayer) {
            // Keep one persistent primary slot in composition. XvoxSongArtwork retains/morphs a
            // decoded outgoing bitmap (or bundled fallback) while the settled/lyrics song changes.
            PremiumBlurredCover(
                artwork = artwork,
                fallbackSeed = artworkSeed,
                skipDecodedMorph = primaryAlreadyShownByPager,
                // Once an endpoint was already painted by the pager, a new source must use its
                // supplied fallback/original rather than retaining the previous song beneath it.
                retainPreviousOnLoading = !primaryAlreadyShownByPager,
                blur = coverBlur,
                alpha = coverLayerAlpha * (1f - displayedIncomingFraction),
                onReady = { markArtworkReady(artworkKey) }
            )
            if (pagerArtworkTransition) {
                // Give every incoming occurrence its own composition identity and never retain a
                // former incoming bitmap. A rapid Next ↔ Previous reversal can therefore show only
                // this target (or its supplied fallback), never the opposite song's blurred cover.
                key(incomingArtworkKey) {
                    PremiumBlurredCover(
                        artwork = incomingArtwork,
                        fallbackSeed = incomingArtworkSeed,
                        retainPreviousOnLoading = false,
                        blur = coverBlur,
                        alpha = coverLayerAlpha * displayedIncomingFraction,
                        onReady = { markArtworkReady(incomingArtworkKey) }
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
    skipDecodedMorph: Boolean = false,
    /** Incoming pager layers must never reuse a previous target's bitmap. */
    retainPreviousOnLoading: Boolean = true,
    blur: Float,
    alpha: Float,
    onReady: (() -> Unit)? = null
) {
    XvoxSongArtwork(
        artwork = artwork,
        requestSize = XvoxNowPlayingArtworkSize,
        contentScale = ContentScale.Crop,
        // The persistent primary may bridge a normal controlled change. Incoming pager slots and
        // already-presented endpoints deliberately opt out so an old target cannot flicker back.
        keepPreviousOnLoading = retainPreviousOnLoading,
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
