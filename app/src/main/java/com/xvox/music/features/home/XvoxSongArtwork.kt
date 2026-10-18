package com.xvox.music.features.home

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.BitmapImage
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.Precision
import com.xvox.music.artwork.XvoxArtworkCache
import com.xvox.music.artwork.XvoxImageWork
import com.xvox.music.core.design.theme.XvoxTheme

const val XvoxGridArtworkSize = 256
const val XvoxRecentArtworkSize = 512
const val XvoxNowPlayingArtworkSize = 0

/**
 * Shared artwork renderer.
 *
 * Normal library cards still use the requested resolution unchanged. Full-player callers can ask
 * to retain their last decoded bitmap while Coil resolves the next cover, avoiding a card-colour
 * flash when playback advances or an adjacent queue row changes.
 */
@Composable
fun XvoxSongArtwork(
    artwork: Any?,
    modifier: Modifier = Modifier,
    requestSize: Int = XvoxGridArtworkSize,
    contentScale: ContentScale = ContentScale.Crop,
    keepPreviousOnLoading: Boolean = false,
    /** Active All Songs scrolling defers uncached decode; cached/full-size art remains drawable. */
    loadArtwork: Boolean = true,
    /** The surrounding transition already rendered this decoded incoming cover at full opacity. */
    skipDecodedMorph: Boolean = false,
    /** Use the bundled default cover as a bitmap so a local blur can morph it with real covers. */
    renderFallbackAsBitmap: Boolean = false,
    /** Fallback mark behavior follows the surface; visible artwork always centers the XVOX mark. */
    fallbackBrand: XvoxFallbackBrand = XvoxFallbackBrand.STANDARD,
    /** Retained only as a stable caller identity; every fallback now shares the supplied cover. */
    fallbackSeed: Int? = null,
    /** Reports after a current-key decoded bridge/original or the bundled fallback is ready. */
    onArtworkReady: (() -> Unit)? = null
) {
    val colors = XvoxTheme.colors
    val context = LocalContext.current
    val latestArtworkReady by rememberUpdatedState(onArtworkReady)

    if (artwork == null && !renderFallbackAsBitmap) {
        // Regular card art is immediately drawable.  The local blurred Now Playing path opts
        // into a bitmap below so it can blend the supplied cover with decoded old/new artwork.
        LaunchedEffect(fallbackSeed) { latestArtworkReady?.invoke() }
        WavePatternSquare(
            modifier = modifier,
            seed = fallbackSeed ?: 11,
            bitmapSize = requestSize,
            brand = fallbackBrand
        )
        return
    }

    val baseKey = remember(artwork) {
        artwork?.let(XvoxArtworkCache::keyFor) ?: "xvox_default_cover"
    }
    val defaultCoverKey = remember(colors.isLight) {
        "xvox_default_cover_v2:${if (colors.isLight) "light" else "dark"}"
    }
    val cacheKey = remember(baseKey, requestSize, artwork, renderFallbackAsBitmap, defaultCoverKey) {
        if (artwork == null && renderFallbackAsBitmap) defaultCoverKey else "${baseKey}_$requestSize"
    }

    fun cachedBitmap(): Bitmap? {
        if (artwork == null && renderFallbackAsBitmap) return XvoxArtworkCache.get(defaultCoverKey)
        return when {
            requestSize == 0 -> XvoxArtworkCache.get("${baseKey}_0")
            requestSize >= 1024 -> {
                XvoxArtworkCache.get(cacheKey)
                    ?: XvoxArtworkCache.get("${baseKey}_1024")
                    ?: XvoxArtworkCache.get("${baseKey}_0")
            }
            requestSize >= 512 -> {
                XvoxArtworkCache.get(cacheKey)
                    ?: XvoxArtworkCache.get("${baseKey}_512")
                    ?: XvoxArtworkCache.get("${baseKey}_1024")
                    ?: XvoxArtworkCache.get("${baseKey}_0")
            }
            else -> {
                XvoxArtworkCache.get(cacheKey)
                    ?: XvoxArtworkCache.get("${baseKey}_256")
                    ?: XvoxArtworkCache.get(baseKey)
                    ?: XvoxArtworkCache.get("${baseKey}_512")
                    ?: XvoxArtworkCache.get("${baseKey}_0")
            }
        }
    }

    // Original-quality full-player art must still request/decode its source even when a grid or
    // mini-player bitmap is already cached. That smaller bitmap is a visual bridge only; treating
    // it as `decodedBitmap` would accidentally suppress the original request forever.
    fun cachedOriginalBitmap(): Bitmap? = when {
        artwork != null && requestSize == 0 -> XvoxArtworkCache.get("${baseKey}_0")
        else -> cachedBitmap()
    }

    fun cachedVisualBridge(): Bitmap? = when {
        artwork != null && requestSize == 0 -> cachedOriginalBitmap()
            ?: XvoxArtworkCache.get("${baseKey}_1024")
            ?: XvoxArtworkCache.get("${baseKey}_512")
            ?: XvoxArtworkCache.get("${baseKey}_256")
            ?: XvoxArtworkCache.get(baseKey)
        else -> cachedBitmap()
    }

    // Keep a Compose-observable result in addition to the LRU cache. LruCache writes themselves
    // do not invalidate composition, so this state lets a freshly decoded cover replace its
    // placeholder immediately without reducing its requested quality.
    var decodedBitmap by remember(cacheKey, baseKey, requestSize) { mutableStateOf(cachedOriginalBitmap()) }
    // A failed MediaStore endpoint is stable for this process. Start straight on the bundled
    // fallback on later composition instead of re-opening that URI and leaving a blank interval.
    var artworkFailed by remember(cacheKey, baseKey, requestSize) {
        mutableStateOf(artwork != null && XvoxArtworkCache.isKnownFailed(artwork))
    }
    // A URI already known to be unreadable is equivalent to an absent cover for the local
    // bitmap path. Request the supplied default immediately rather than leaving its first
    // fallback frame as only a transient draw while the backdrop waits for readiness.
    var fallbackRequested by remember(cacheKey) {
        mutableStateOf(
            renderFallbackAsBitmap &&
                (artwork == null || (artwork != null && XvoxArtworkCache.isKnownFailed(artwork)))
        )
    }
    // Original-quality adjacent pages can be composed a frame before the preloader completes.
    // Listen only for that narrow full-player case so the cache write becomes a visible bridge on
    // the first swipe rather than leaving a one-frame fallback flicker.
    if (requestSize == 0 && artwork != null && decodedBitmap == null) {
        val artworkCacheRevision by XvoxArtworkCache.revision.collectAsState()
        LaunchedEffect(cacheKey, artworkCacheRevision) {
            // A same-cover 256/512 bridge is already a real drawable for the local Now Playing
            // backdrop. Report that current-key bridge immediately, rather than keeping the old
            // song's blur in place until a second original-size decode completes on a first swipe.
            // This never reports the retained previous bitmap: the lookup is keyed to this source.
            if (cachedVisualBridge() != null) latestArtworkReady?.invoke()
            if (XvoxArtworkCache.isKnownFailed(artwork)) {
                artworkFailed = true
            } else {
                cachedOriginalBitmap()?.let { cached -> decodedBitmap = cached }
            }
        }
    }
    LaunchedEffect(defaultCoverKey, renderFallbackAsBitmap, artwork, fallbackRequested) {
        if (renderFallbackAsBitmap && (artwork == null || fallbackRequested) && decodedBitmap == null) {
            // The default cover is decoded once on the bounded artwork worker.  Unlike the old
            // procedural fallback, a dense coverless All Songs page creates no per-card pixels.
            decodedBitmap = kotlinx.coroutines.withContext(XvoxImageWork.decoderContext) {
                xvoxDefaultCoverBitmap(context, colors.isLight)
            }
        }
    }
    LaunchedEffect(cacheKey, decodedBitmap, artworkFailed, renderFallbackAsBitmap) {
        // A permanent decoder failure still has the bundled fallback, so callers waiting to
        // blend an incoming cover must be released instead of holding the previous cover forever.
        // The supplied fallback is already fully drawable synchronously via WavePatternSquare;
        // do not make a Cover-background pager wait for a second decode merely because the source
        // URI was known bad. This closes the last coverless first-swipe readiness gap.
        if (decodedBitmap != null || artworkFailed) latestArtworkReady?.invoke()
    }
    // A retained decoded bitmap has two jobs for full-player callers: it keeps a real old cover
    // visible while the next original decodes, then becomes the source of an internal image
    // morph. This avoids a transient card-colour frame between cover identities.
    var retainedBitmap by remember { mutableStateOf<Bitmap?>(cachedVisualBridge()) }
    var morphFromBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var morphKey by remember { mutableStateOf<String?>(null) }
    val morphProgress = remember { Animatable(1f) }

    LaunchedEffect(cacheKey, decodedBitmap, keepPreviousOnLoading, skipDecodedMorph) {
        val next = decodedBitmap ?: return@LaunchedEffect
        val previous = retainedBitmap
        if (
            keepPreviousOnLoading &&
            !skipDecodedMorph &&
            previous != null &&
            previous != next &&
            morphKey != cacheKey
        ) {
            morphKey = cacheKey
            morphFromBitmap = previous
            // Make the new bitmap the retained fallback immediately. A rapid next change can
            // therefore blend from the most recently decoded real artwork, never a placeholder.
            retainedBitmap = next
            morphProgress.snapTo(0f)
            morphProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(300, easing = CubicBezierEasing(.22f, 0f, 0f, 1f))
            )
            if (morphKey == cacheKey) morphFromBitmap = null
        } else {
            retainedBitmap = next
            // If the background pager already showed this exact incoming bitmap at full opacity,
            // cancel any stale old-cover morph instead of replaying it during the handoff frame.
            if (skipDecodedMorph || morphKey != cacheKey) {
                morphFromBitmap = null
                if (skipDecodedMorph) morphKey = cacheKey
            }
        }
    }

    val visibleBitmap = decodedBitmap ?: retainedBitmap.takeIf {
        keepPreviousOnLoading && (!artworkFailed || renderFallbackAsBitmap && fallbackRequested)
    }
    // asImageBitmap() allocations on every parent playback-state recomposition were enough to
    // make a dense library fling feel behind the finger. Keep the exact decoded bitmap/quality;
    // only its lightweight Compose wrapper is memoized.
    val visibleImageBitmap = remember(visibleBitmap) { visibleBitmap?.asImageBitmap() }
    val retainedImageBitmap = remember(retainedBitmap) { retainedBitmap?.asImageBitmap() }
    val decodedImageBitmap = remember(decodedBitmap) { decodedBitmap?.asImageBitmap() }
    val morphFromImageBitmap = remember(morphFromBitmap) { morphFromBitmap?.asImageBitmap() }
    // On a cached/direct track change, composition can see the new bitmap one frame before the
    // morph coroutine installs its old source. Keep painting the retained cover for that frame so
    // lyrics Next/Previous never flashes new → old → new.
    val awaitingMorphStart = keepPreviousOnLoading &&
        !skipDecodedMorph &&
        decodedImageBitmap != null &&
        retainedImageBitmap != null &&
        retainedBitmap != decodedBitmap &&
        morphKey != cacheKey
    val request = remember(artwork, requestSize) {
        val builder = ImageRequest.Builder(context)
            .data(artwork)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.DISABLED)

        if (requestSize > 0) {
            builder.size(requestSize, requestSize).precision(Precision.EXACT)
        } else {
            builder.size(coil3.size.Size.ORIGINAL).precision(Precision.INEXACT)
        }
        builder.build()
    }

    // A cold adjacent Now Playing page stays transparent while its real cover is resolving. The
    // pager/backdrop remains visible beneath it, which is materially smoother than flashing the
    // generic fallback between two real covers on a user's first swipe.
    val keepColdPlayerFrameTransparent = artwork != null && keepPreviousOnLoading &&
        !renderFallbackAsBitmap && visibleImageBitmap == null && !artworkFailed
    Box(
        modifier = if (keepColdPlayerFrameTransparent) modifier else modifier.background(colors.cardElevated),
        contentAlignment = Alignment.Center
    ) {
        val activeMorph = keepPreviousOnLoading &&
            decodedImageBitmap != null &&
            morphFromImageBitmap != null &&
            morphProgress.value < .999f
        if (renderFallbackAsBitmap && visibleImageBitmap == null) {
            // The bitmap decode happens off the main thread.  Paint the same supplied cover for
            // the first frame—even while a real URI is resolving—so a failed cover cannot reveal
            // a generic card colour beneath the local Now Playing blur.  Loading artwork omits
            // the mark because this temporary bitmap is purely a backdrop source.
            WavePatternSquare(
                modifier = Modifier.fillMaxSize(),
                seed = fallbackSeed ?: 11,
                bitmapSize = requestSize,
                brand = if (artwork == null || artworkFailed) fallbackBrand else XvoxFallbackBrand.NONE
            )
        } else if (!loadArtwork && visibleImageBitmap == null && !artworkFailed) {
            // During an active All Songs drag do not launch a fresh full-size decode on the cards
            // crossing the viewport. Draw the exact supplied fallback briefly, then request the
            // unchanged 256px/512px artwork once the finger has settled.
            WavePatternSquare(
                modifier = Modifier.fillMaxSize(),
                seed = fallbackSeed ?: 11,
                bitmapSize = requestSize,
                brand = fallbackBrand
            )
        } else if (visibleImageBitmap == null && keepPreviousOnLoading && !artworkFailed) {
            // Keep only a *cold real-artwork* frame transparent. A known dead endpoint must fall
            // through to the supplied default below; otherwise Now Playing can expose its card
            // surface indefinitely after a coverless first swipe.
        } else if (artworkFailed && visibleImageBitmap == null) {
            WavePatternSquare(
                modifier = Modifier.fillMaxSize(),
                seed = fallbackSeed ?: 11,
                bitmapSize = requestSize,
                brand = fallbackBrand
            )
        } else if (awaitingMorphStart) {
            // The launched morph begins immediately after this composition. Holding the old frame
            // for one beat avoids exposing a decoded target before its matching blend exists.
            Image(
                bitmap = retainedImageBitmap!!,
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        } else if (activeMorph) {
            // One stable image slot owns this premium cover-to-cover morph. The old bitmap remains
            // genuinely decoded until the new bitmap is visible, so blurred Now Playing backdrops
            // cannot flash their card surface while songs change.
            Image(
                bitmap = morphFromImageBitmap!!,
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    alpha = (1f - morphProgress.value).coerceIn(0f, 1f)
                }
            )
            Image(
                bitmap = decodedImageBitmap!!,
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    alpha = morphProgress.value.coerceIn(0f, 1f)
                }
            )
        } else {
            visibleImageBitmap?.let { imageBitmap ->
                Image(
                    bitmap = imageBitmap,
                    contentDescription = null,
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Do not draw a solid loading layer above a retained full-player cover. Coil only decodes
        // the original here; controlled Compose rendering owns the visible bridge-to-original handoff.
        if (loadArtwork && artwork != null && decodedBitmap == null && !artworkFailed) {
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = contentScale,
                onSuccess = { success ->
                    (success.result.image as? BitmapImage)?.bitmap?.let { bitmap ->
                        XvoxArtworkCache.markSuccessful(artwork)
                        XvoxArtworkCache.put(cacheKey, bitmap)
                        if (requestSize == 0) XvoxArtworkCache.put("${baseKey}_0", bitmap)
                        decodedBitmap = bitmap
                    } ?: run {
                        // A non-bitmap result cannot participate in the retained-bitmap morph.
                        // The Now Playing bitmap path turns this into the bundled cover; regular
                        // cards still render their branded Compose fallback below.
                        artworkFailed = true
                        XvoxArtworkCache.markFailed(artwork)
                        if (renderFallbackAsBitmap) fallbackRequested = true
                    }
                },
                onError = {
                    artworkFailed = true
                    XvoxArtworkCache.markFailed(artwork)
                    if (renderFallbackAsBitmap) fallbackRequested = true
                },
                // The image request is deliberately invisible while a retained bridge is on
                // screen. Its callback installs the original bitmap into the controlled morph
                // above, preventing Coil from painting one unpaired direct frame over the bridge.
                modifier = if (keepPreviousOnLoading) {
                    Modifier.fillMaxSize().graphicsLayer { alpha = 0f }
                } else {
                    Modifier.fillMaxSize()
                }
            )
        }
    }
}

@Composable
fun XvoxSongArtwork(
    song: com.xvox.music.core.model.Song?,
    modifier: Modifier = Modifier,
    requestSize: Int = XvoxGridArtworkSize,
    contentScale: ContentScale = ContentScale.Crop,
    keepPreviousOnLoading: Boolean = false
) {
    XvoxSongArtwork(
        artwork = song?.artworkUri,
        modifier = modifier,
        requestSize = requestSize,
        contentScale = contentScale,
        keepPreviousOnLoading = keepPreviousOnLoading,
        fallbackSeed = song?.id?.hashCode()
    )
}

/**
 * Lean all-songs artwork path. It avoids the full-player retained-image machinery while preserving
 * the exact 256px/512px request. A smaller cached cover may draw as a bridge only while its own
 * requested size resolves; songs with artwork never fall back to the default cover during a scroll.
 */
@Composable
fun XvoxGridArtwork(
    artwork: Uri?,
    fallbackSeed: Int,
    modifier: Modifier = Modifier,
    requestSize: Int = XvoxGridArtworkSize
) {
    val colors = XvoxTheme.colors
    if (artwork == null) {
        WavePatternSquare(
            modifier = modifier,
            seed = fallbackSeed,
            bitmapSize = requestSize,
            brand = XvoxFallbackBrand.STANDARD
        )
        return
    }

    val context = LocalContext.current
    val baseKey = remember(artwork) { XvoxArtworkCache.keyFor(artwork) }
    val cacheKey = remember(baseKey, requestSize) { "${baseKey}_$requestSize" }
    // Preserve image quality: a 512px mosaic tile accepts only a 512px/original cached bitmap as
    // final output. A 256px cache may appear temporarily beneath an invisible exact-size request.
    val cachedExactOrBetter = remember(cacheKey, requestSize) {
        when {
            requestSize >= XvoxRecentArtworkSize -> {
                XvoxArtworkCache.get(cacheKey)
                    ?: XvoxArtworkCache.get("${baseKey}_512")
                    ?: XvoxArtworkCache.get("${baseKey}_1024")
                    ?: XvoxArtworkCache.get("${baseKey}_0")
            }
            else -> {
                XvoxArtworkCache.get(cacheKey)
                    ?: XvoxArtworkCache.get("${baseKey}_256")
                    ?: XvoxArtworkCache.get(baseKey)
                    ?: XvoxArtworkCache.get("${baseKey}_512")
                    ?: XvoxArtworkCache.get("${baseKey}_0")
            }
        }
    }
    val cachedBridge = remember(cacheKey, requestSize, cachedExactOrBetter) {
        cachedExactOrBetter
            ?: if (requestSize >= XvoxRecentArtworkSize) XvoxArtworkCache.get("${baseKey}_256") else null
    }
    var decodedBitmap by remember(cacheKey) { mutableStateOf(cachedExactOrBetter) }
    var failed by remember(cacheKey) { mutableStateOf(XvoxArtworkCache.isKnownFailed(artwork)) }
    val visibleBitmap = decodedBitmap ?: cachedBridge
    val visibleImage = remember(visibleBitmap) { visibleBitmap?.asImageBitmap() }
    val needsExactRequest = decodedBitmap == null && !failed

    Box(modifier = modifier) {
        visibleImage?.let { image ->
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        when {
            needsExactRequest -> {
                val request = remember(cacheKey, requestSize) {
                    ImageRequest.Builder(context)
                        .data(artwork)
                        .size(requestSize, requestSize)
                        .precision(Precision.EXACT)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .networkCachePolicy(CachePolicy.DISABLED)
                        .build()
                }
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    onSuccess = { success ->
                        (success.result.image as? BitmapImage)?.bitmap?.let { bitmap ->
                            XvoxArtworkCache.markSuccessful(artwork)
                            XvoxArtworkCache.put(cacheKey, bitmap)
                            decodedBitmap = bitmap
                        } ?: run {
                            failed = true
                            XvoxArtworkCache.markFailed(artwork)
                        }
                    },
                    onError = {
                        failed = true
                        XvoxArtworkCache.markFailed(artwork)
                    },
                    // A lower-size cached bridge remains visible until the requested exact image
                    // arrives; otherwise Coil draws the real cover itself over the card surface.
                    modifier = if (visibleImage == null) {
                        Modifier.fillMaxSize().background(colors.cardElevated)
                    } else {
                        Modifier.fillMaxSize().graphicsLayer { alpha = 0f }
                    }
                )
            }
            failed && visibleImage == null -> {
                WavePatternSquare(
                    modifier = Modifier.fillMaxSize(),
                    seed = fallbackSeed,
                    bitmapSize = requestSize,
                    brand = XvoxFallbackBrand.STANDARD
                )
            }
        }
    }
}
