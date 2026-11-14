// EXACT CODE REPLACE KARO (poori file) - koi extra kaam nahi. Path: app/src/main/java/com/xvox/music/player/nowplaying/XvoxNowPlayingAltLayouts.kt
package com.xvox.music.player.nowplaying

import android.content.Context
import android.media.AudioManager
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.features.home.XvoxSongArtwork
import com.xvox.music.player.nowplaying.lyrics.XvoxLyrics
import com.xvox.music.player.playback.RepeatMode
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.min

/** Callbacks shared by the iOS and Material You layouts (one place, so both stay in sync). */
class XvoxAltNpActions(
    val onClose: () -> Unit,
    val onMore: () -> Unit,
    val onShare: () -> Unit,
    val onToggleLiked: () -> Unit,
    val onShuffle: () -> Unit,
    val onRepeat: () -> Unit,
    val onShuffleLong: () -> Unit,
    val onRepeatLong: () -> Unit,
    val onQueue: () -> Unit,
    val onOutput: () -> Unit,
    val onPrevious: () -> Unit,
    val onNext: () -> Unit,
    val onTogglePlay: () -> Unit,
    val onSeek: (Long) -> Unit,
    val onOpenLyrics: () -> Unit,
    val onCloseLyrics: () -> Unit,
    val onLyricsSettings: () -> Unit
)

private fun fmtTime(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

// ───────────────────────────── shared building blocks ─────────────────────────────

/** Icon button with a spring press-scale; optional long press (shuffle / repeat sheets). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AltIconButton(
    res: Int,
    tint: Color,
    size: Dp,
    modifier: Modifier = Modifier,
    iconSize: Dp = size * .52f,
    background: Color = Color.Transparent,
    shape: androidx.compose.ui.graphics.Shape = CircleShape,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) .86f else 1f,
        spring(dampingRatio = .55f, stiffness = Spring.StiffnessMedium),
        label = "altBtn"
    )
    Box(
        modifier = Modifier
            .size(size)
            .then(modifier)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(background)
            .combinedClickable(
                interactionSource = source,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(painterResource(res), contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/**
 * Horizontal slider/seek bar. The bar thickens while touched, follows the finger 1:1 and commits
 * on release. [thumbRadius] > 0 draws a round thumb (Material look).
 */
@Composable
private fun AltSlider(
    fraction: Float,
    onCommit: (Float) -> Unit,
    trackColor: Color,
    fillColor: Color,
    modifier: Modifier = Modifier,
    thin: Dp = 5.dp,
    thick: Dp = 10.dp,
    thumbRadius: Dp = 0.dp,
    onChange: (Float) -> Unit = {}
) {
    var drag by remember { mutableStateOf<Float?>(null) }
    val shown = (drag ?: fraction).coerceIn(0f, 1f)
    val height by animateDpAsState(
        if (drag != null) thick else thin,
        spring(dampingRatio = .7f, stiffness = Spring.StiffnessMediumLow),
        label = "altSliderH"
    )
    val commit by rememberUpdatedState(onCommit)
    val change by rememberUpdatedState(onChange)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val w = size.width.toFloat().coerceAtLeast(1f)
                    var f = (down.position.x / w).coerceIn(0f, 1f)
                    drag = f
                    change(f)
                    down.consume()
                    while (true) {
                        val ev = awaitPointerEvent()
                        val c = ev.changes.firstOrNull { it.id == down.id } ?: break
                        if (!c.pressed) break
                        f = (c.position.x / w).coerceIn(0f, 1f)
                        drag = f
                        change(f)
                        c.consume()
                    }
                    commit(f)
                    drag = null
                }
            }
            .drawBehind {
                val h = height.toPx()
                val top = (size.height - h) / 2f
                val r = CornerRadius(h / 2f, h / 2f)
                drawRoundRect(trackColor, Offset(0f, top), Size(size.width, h), r)
                val fw = (size.width * shown).coerceAtLeast(0f)
                if (fw > 0f) drawRoundRect(fillColor, Offset(0f, top), Size(fw, h), r)
                if (thumbRadius > 0.dp) {
                    val tr = thumbRadius.toPx() * (if (drag != null) 1.2f else 1f)
                    drawCircle(fillColor, tr, Offset(fw.coerceIn(tr, size.width - tr), size.height / 2f))
                }
            }
    )
}

/** System media volume as 0..1, kept in sync with hardware keys. */
@Composable
private fun rememberVolumeState(): Pair<Float, (Float) -> Unit> {
    val context = LocalContext.current
    val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val max = remember { audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var level by remember { mutableFloatStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC) / max.toFloat()) }
    LaunchedEffect(Unit) {
        while (true) {
            level = audio.getStreamVolume(AudioManager.STREAM_MUSIC) / max.toFloat()
            delay(700)
        }
    }
    return level to { f: Float ->
        level = f
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, (f * max).toInt().coerceIn(0, max), 0)
    }
}

/**
 * Lyrics list shared by both layouts. iOS: huge bold left-aligned lines, the active line sharp and
 * white, the rest dimmed and progressively blurred. Material: calm medium-weight text with the
 * active line tinted. Auto-follows the song until the user scrolls; tap a line to seek.
 */
@Composable
private fun AltLyrics(
    lyrics: XvoxLyrics?,
    loading: Boolean,
    position: Long,
    onSeek: (Long) -> Unit,
    ios: Boolean,
    followSynced: Boolean,
    activeColor: Color,
    idleColor: Color,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val lines = lyrics?.lines.orEmpty()
    if (lines.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                if (loading) "Loading lyrics…" else "No lyrics for this song",
                color = idleColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
        return
    }
    val synced = lyrics!!.synchronized && followSynced
    val active = if (lyrics.synchronized) {
        lines.indexOfLast { (it.timeMs ?: Long.MAX_VALUE) <= position }
    } else -1
    val list = rememberLazyListState()
    val dragged by list.interactionSource.collectIsDraggedAsState()
    var browsing by remember { mutableStateOf(false) }
    LaunchedEffect(dragged) {
        if (dragged) browsing = true
        else if (browsing) {
            delay(2800)
            browsing = false
        }
    }
    LaunchedEffect(active, browsing, synced) {
        if (synced && !browsing && active >= 0) {
            val offset = -(list.layoutInfo.viewportSize.height * if (ios) .2f else .28f).toInt()
            list.animateScrollToItem(active, offset)
        }
    }
    Box(
        modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    Brush.verticalGradient(
                        0f to Color.Transparent, .07f to Color.Black, .86f to Color.Black, 1f to Color.Transparent
                    ),
                    blendMode = BlendMode.DstIn
                )
            }
    ) {
        LazyColumn(
            state = list,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(if (ios) 26.dp else 20.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(lines) { i, line ->
                val dist = if (synced && active >= 0) abs(i - active) else 0
                val isActive = synced && i == active
                val targetAlpha = when {
                    !synced -> if (ios) .86f else .9f
                    isActive -> 1f
                    else -> (if (ios) .5f else .6f) - .07f * min(dist, 4)
                }
                val a by animateFloatAsState(targetAlpha.coerceAtLeast(.2f), tween(380), label = "lyA")
                val blurTarget = if (ios && synced && !browsing && !isActive) min(dist, 4) * 1.5f else 0f
                val blurDp by animateDpAsState(blurTarget.dp, tween(380), label = "lyB")
                val s by animateFloatAsState(if (ios && synced && !isActive) .93f else 1f, tween(380), label = "lyS")
                val text = line.text.ifBlank { "♪" }
                Text(
                    text = text,
                    color = if (isActive && !ios) activeColor else if (isActive) activeColor else idleColor,
                    fontSize = if (ios) 32.sp else 23.sp,
                    lineHeight = if (ios) 39.sp else 31.sp,
                    fontWeight = if (ios) FontWeight.ExtraBold else if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = s
                            scaleY = s
                            transformOrigin = TransformOrigin(0f, .5f)
                        }
                        .alpha(a)
                        .then(if (blurDp > 0.dp) Modifier.blur(blurDp) else Modifier)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { line.timeMs?.let(onSeek) }
                )
            }
        }
    }
}

// ───────────────────────────────── iOS layout ─────────────────────────────────

private val IosEase = androidx.compose.animation.core.CubicBezierEasing(.2f, 0f, 0f, 1f)

/** Soft blurred copy of the cover, masked to fade out downward: the Apple "strip" under the status bar. */
@Composable
private fun IosBlurStrip(song: Song, height: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    Brush.verticalGradient(0f to Color.Black, .55f to Color.Black.copy(alpha = .85f), 1f to Color.Transparent),
                    blendMode = BlendMode.DstIn
                )
            }
    ) {
        // Tiny decode (like BitChord's backdrop): the blur hides the low resolution, and the work is cheap.
        XvoxSongArtwork(
            artwork = song.artworkUri,
            requestSize = 96,
            modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = 1.25f; scaleY = 1.25f }.blur(26.dp)
        )
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .12f)))
    }
}

@Composable
private fun IosLyricsPage(
    song: Song, isPlaying: Boolean, position: Long, duration: Long,
    lyrics: XvoxLyrics?, lyricsLoading: Boolean,
    actions: XvoxAltNpActions, showThumb: Boolean, modifier: Modifier = Modifier
) {
    val white = Color.White
    Box(modifier.fillMaxSize()) {
        AltLyrics(
            lyrics = lyrics, loading = lyricsLoading, position = position, onSeek = actions.onSeek,
            ios = true, followSynced = true, activeColor = white, idleColor = white,
            contentPadding = PaddingValues(start = 26.dp, end = 26.dp, top = 170.dp, bottom = 210.dp),
            modifier = Modifier.fillMaxSize()
        )
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 22.dp, end = 22.dp, top = 26.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = actions.onCloseLyrics),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showThumb) {
                XvoxSongArtwork(artwork = song.artworkUri, requestSize = 160, modifier = Modifier.size(58.dp).clip(RoundedCornerShape(10.dp)))
            } else Spacer(Modifier.size(58.dp))
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(song.title, color = white, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist, color = white.copy(alpha = .62f), fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            AltIconButton(R.drawable.ic_xvox_ios_ellipsis, white, 44.dp, background = white.copy(alpha = .2f), onClick = actions.onLyricsSettings)
        }
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .55f))))
                .navigationBarsPadding()
                .padding(horizontal = 26.dp, vertical = 12.dp)
        ) {
            AltSlider(
                fraction = if (duration > 0) position / duration.toFloat() else 0f,
                onCommit = { f -> actions.onSeek((f * duration).toLong()) },
                trackColor = white.copy(alpha = .26f), fillColor = white
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                AltIconButton(R.drawable.ic_xvox_ios_rewind, white, 56.dp, iconSize = 30.dp, onClick = actions.onPrevious)
                AltIconButton(if (isPlaying) R.drawable.ic_xvox_ios_pause else R.drawable.ic_xvox_ios_play, white, 64.dp, iconSize = 38.dp, onClick = actions.onTogglePlay)
                AltIconButton(R.drawable.ic_xvox_ios_forward, white, 56.dp, iconSize = 30.dp, onClick = actions.onNext)
            }
        }
    }
}

@Composable
private fun IosControls(
    song: Song, isPlaying: Boolean, position: Long, duration: Long, isLiked: Boolean,
    isShuffleEnabled: Boolean, repeatMode: RepeatMode, playingSource: String,
    lyrics: XvoxLyrics?, accent: Color, actions: XvoxAltNpActions, modifier: Modifier = Modifier
) {
    val white = Color.White
    var menu by remember { mutableStateOf(false) }
    Column(modifier.padding(horizontal = 26.dp)) {
        Spacer(Modifier.weight(.9f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(song.title, color = white, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist, color = white.copy(alpha = .6f), fontSize = 21.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            AltIconButton(
                if (isLiked) R.drawable.ic_xvox_heart else R.drawable.ic_xvox_heart_outline,
                if (isLiked) accent else white, 40.dp, background = white.copy(alpha = .2f), onClick = actions.onToggleLiked
            )
            Spacer(Modifier.width(10.dp))
            Box {
                AltIconButton(R.drawable.ic_xvox_ios_ellipsis, white, 40.dp, background = white.copy(alpha = .2f), onClick = { menu = true })
                androidx.compose.material3.DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(if (isShuffleEnabled) "Shuffle: on" else "Shuffle: off") },
                        onClick = { menu = false; actions.onShuffle() }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("Repeat: " + when (repeatMode) { RepeatMode.OFF -> "off"; RepeatMode.ONE -> "one"; RepeatMode.ALL -> "all" }) },
                        onClick = { menu = false; actions.onRepeat() }
                    )
                    androidx.compose.material3.DropdownMenuItem(text = { Text("Shuffle method…") }, onClick = { menu = false; actions.onShuffleLong() })
                    androidx.compose.material3.DropdownMenuItem(text = { Text("Repeat options…") }, onClick = { menu = false; actions.onRepeatLong() })
                    androidx.compose.material3.DropdownMenuItem(text = { Text("Share") }, onClick = { menu = false; actions.onShare() })
                    androidx.compose.material3.DropdownMenuItem(text = { Text("Layout & background") }, onClick = { menu = false; actions.onMore() })
                }
            }
        }
        Spacer(Modifier.weight(.7f))
        val current = lyrics?.lines?.let { ls ->
            if (lyrics.synchronized) ls.getOrNull(ls.indexOfLast { (it.timeMs ?: Long.MAX_VALUE) <= position })?.text
            else ls.firstOrNull { it.text.isNotBlank() }?.text
        }.orEmpty()
        Row(
            Modifier.fillMaxWidth().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = actions.onOpenLyrics),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (current.isNotBlank()) current else "Lyrics", color = white.copy(alpha = .78f), fontSize = 19.sp,
                fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(Modifier.width(10.dp))
            Icon(painterResource(R.drawable.ic_xvox_caret_right), null, tint = white.copy(alpha = .6f), modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.weight(.5f))
        AltSlider(
            fraction = if (duration > 0) position / duration.toFloat() else 0f,
            onCommit = { f -> actions.onSeek((f * duration).toLong()) },
            trackColor = white.copy(alpha = .26f), fillColor = white.copy(alpha = .92f)
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(fmtTime(position), color = white.copy(alpha = .6f), fontSize = 14.sp)
            Text("-" + fmtTime((duration - position).coerceAtLeast(0)), color = white.copy(alpha = .6f), fontSize = 14.sp)
        }
        Spacer(Modifier.weight(.8f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            AltIconButton(R.drawable.ic_xvox_ios_rewind, white, 78.dp, iconSize = 40.dp, onClick = actions.onPrevious)
            AltIconButton(if (isPlaying) R.drawable.ic_xvox_ios_pause else R.drawable.ic_xvox_ios_play, white, 88.dp, iconSize = 54.dp, onClick = actions.onTogglePlay)
            AltIconButton(R.drawable.ic_xvox_ios_forward, white, 78.dp, iconSize = 40.dp, onClick = actions.onNext)
        }
        Spacer(Modifier.weight(.8f))
        val (volume, setVolume) = rememberVolumeState()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_xvox_volume_low), null, tint = white.copy(alpha = .7f), modifier = Modifier.size(20.dp))
            AltSlider(
                fraction = volume, onCommit = { setVolume(it) }, onChange = { setVolume(it) },
                trackColor = white.copy(alpha = .26f), fillColor = white.copy(alpha = .92f),
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
            )
            Icon(painterResource(R.drawable.ic_xvox_volume_high), null, tint = white.copy(alpha = .7f), modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.weight(.5f))
        // Exactly the three iOS bottom actions: lyrics, output, queue.
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            AltIconButton(R.drawable.ic_xvox_ios_lyrics, white.copy(alpha = .85f), 52.dp, iconSize = 26.dp, onClick = actions.onOpenLyrics)
            AltIconButton(R.drawable.ic_xvox_ios_airplay, white.copy(alpha = .85f), 52.dp, iconSize = 26.dp, onClick = actions.onOutput)
            AltIconButton(R.drawable.ic_xvox_ios_list, white.copy(alpha = .85f), 52.dp, iconSize = 26.dp, onClick = actions.onQueue)
        }
        Spacer(Modifier.height(4.dp))
    }
}

/**
 * Apple-Music-style player. Portrait: the cover is ONE shared element — full-bleed with a blurred
 * status-bar strip and a bottom melt on the player, shrinking into the small header cover on the
 * lyrics page while every other control drifts out/in. Landscape: cover left, controls right.
 */
@Composable
fun XvoxNowPlayingIosLayout(
    song: Song, isPlaying: Boolean, position: Long, duration: Long, isLiked: Boolean,
    isShuffleEnabled: Boolean, repeatMode: RepeatMode, playingSource: String,
    lyrics: XvoxLyrics?, lyricsLoading: Boolean, lyricsOpen: Boolean, accent: Color,
    landscape: Boolean, actions: XvoxAltNpActions,
    artwork: @Composable (Modifier, Dp) -> Unit, modifier: Modifier = Modifier
) {
    val white = Color.White
    val p by animateFloatAsState(if (lyricsOpen) 1f else 0f, tween(520, easing = IosEase), label = "iosP")
    BoxWithConstraints(modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0f to Color.Transparent, .45f to Color.Black.copy(alpha = .18f), 1f to Color.Black.copy(alpha = .46f))
            )
        )
        if (landscape) {
            Crossfade(targetState = lyricsOpen, animationSpec = tween(420), label = "iosLand") { showLyrics ->
                if (showLyrics) {
                    IosLyricsPage(song, isPlaying, position, duration, lyrics, lyricsLoading, actions, showThumb = true)
                } else {
                    Row(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(12.dp)) {
                        Box(Modifier.fillMaxHeight().aspectRatio(1f).clip(RoundedCornerShape(18.dp))) { artwork(Modifier.fillMaxSize(), 0.dp) }
                        IosControls(song, isPlaying, position, duration, isLiked, isShuffleEnabled, repeatMode, playingSource, lyrics, accent, actions, Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
            Box(Modifier.align(Alignment.TopStart).statusBarsPadding().padding(start = 18.dp, top = 6.dp).size(34.dp).clickable(onClick = actions.onClose)) {
                Icon(painterResource(R.drawable.ic_xvox_chevron_down), null, tint = white, modifier = Modifier.size(26.dp))
            }
            return@BoxWithConstraints
        }
        val density = androidx.compose.ui.platform.LocalDensity.current
        val statusTop = with(density) { androidx.compose.foundation.layout.WindowInsets.statusBars.getTop(density).toDp() }
        val coverH0 = maxHeight * .47f
        val stripH = statusTop + 120.dp
        // Lyrics page (drifts up into place).
        if (p > .01f) {
            IosLyricsPage(
                song, isPlaying, position, duration, lyrics, lyricsLoading, actions, showThumb = false,
                modifier = Modifier.graphicsLayer { alpha = ((p - .25f) / .75f).coerceIn(0f, 1f); translationY = (1f - p) * 70.dp.toPx() }
            )
        }
        // Player controls (drift down + fade while lyrics open).
        if (p < .99f) {
            IosControls(
                song, isPlaying, position, duration, isLiked, isShuffleEnabled, repeatMode, playingSource, lyrics, accent, actions,
                Modifier.fillMaxSize().navigationBarsPadding().padding(top = coverH0)
                    .graphicsLayer { alpha = (1f - p * 1.8f).coerceIn(0f, 1f); translationY = p * 60.dp.toPx() }
            )
        }
        // Blurred cover strip under the shared cover (visible on the lyrics page).
        Box(Modifier.graphicsLayer { alpha = p }) { IosBlurStrip(song, stripH) }
        // Shared cover.
        val w = androidx.compose.ui.unit.lerp(maxWidth, 58.dp, p)
        val h = androidx.compose.ui.unit.lerp(coverH0, 58.dp, p)
        val x = androidx.compose.ui.unit.lerp(0.dp, 22.dp, p)
        val y = androidx.compose.ui.unit.lerp(0.dp, statusTop + 26.dp, p)
        val corner = androidx.compose.ui.unit.lerp(0.dp, 10.dp, p)
        Box(
            Modifier
                .absoluteOffset(x, y)
                .size(w, h)
                .clip(RoundedCornerShape(corner))
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        Brush.verticalGradient(0f to Color.Black, .62f to Color.Black, 1f to Color.Black.copy(alpha = p)),
                        blendMode = BlendMode.DstIn
                    )
                }
        ) { artwork(Modifier.fillMaxSize(), 0.dp) }
        // Blurred strip from the status bar down, over the cover (player page).
        Box(Modifier.graphicsLayer { alpha = 1f - p }) { IosBlurStrip(song, stripH) }
        // Header tap target to leave lyrics, over the small cover.
        if (lyricsOpen) {
            Box(
                Modifier.absoluteOffset(0.dp, 0.dp).fillMaxWidth().height(statusTop + 100.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = actions.onCloseLyrics)
            )
        }
        // Grab handle + source.
        Column(
            Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 6.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = actions.onClose),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.width(40.dp).height(5.dp).clip(CircleShape).background(white.copy(alpha = .8f)))
            Text(
                "Playing from $playingSource", color = white, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 6.dp).graphicsLayer { alpha = 1f - p }
            )
        }
    }
}

// ─────────────────────────────── Material You layout ───────────────────────────────

@Composable
private fun MaterialControls(
    song: Song, isPlaying: Boolean, position: Long, duration: Long, isLiked: Boolean,
    isShuffleEnabled: Boolean, repeatMode: RepeatMode, playingSource: String,
    onSurface: Color, onMuted: Color, container: Color, accent: Color, accentSoft: Color, onAccent: Color,
    actions: XvoxAltNpActions, modifier: Modifier = Modifier
) {
    Column(modifier) {
        Spacer(Modifier.weight(.6f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(song.title, color = onSurface, fontSize = 30.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist, color = onMuted, fontSize = 19.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            AltIconButton(R.drawable.ic_xvox_lyrics, onAccent, 56.dp, background = accent, onClick = actions.onOpenLyrics)
        }
        Spacer(Modifier.weight(.5f))
        AltSlider(
            fraction = if (duration > 0) position / duration.toFloat() else 0f,
            onCommit = { f -> actions.onSeek((f * duration).toLong()) },
            trackColor = accent.copy(alpha = .25f), fillColor = accent, thin = 6.dp, thick = 12.dp, thumbRadius = 11.dp
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(fmtTime(position), color = onMuted, fontSize = 14.sp)
            Text(playingSource, color = onSurface, fontSize = 12.sp, modifier = Modifier.clip(RoundedCornerShape(50)).background(accentSoft).padding(horizontal = 12.dp, vertical = 5.dp))
            Text(fmtTime(duration), color = onMuted, fontSize = 14.sp)
        }
        Spacer(Modifier.weight(.6f))
        val playRadius by animateDpAsState(if (isPlaying) 22.dp else 34.dp, spring(dampingRatio = .62f, stiffness = Spring.StiffnessMediumLow), label = "playMorph")
        Row(Modifier.fillMaxWidth().height(68.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AltIconButton(R.drawable.ic_xvox_skip_previous, onSurface, 68.dp, Modifier.weight(1f).fillMaxHeight(), iconSize = 28.dp, shape = RoundedCornerShape(34.dp), background = accentSoft, onClick = actions.onPrevious)
            AltIconButton(if (isPlaying) R.drawable.ic_xvox_pause else R.drawable.ic_xvox_play, onAccent, 68.dp, Modifier.weight(1.35f).fillMaxHeight(), iconSize = 32.dp, shape = RoundedCornerShape(playRadius), background = accent, onClick = actions.onTogglePlay)
            AltIconButton(R.drawable.ic_xvox_skip_next, onSurface, 68.dp, Modifier.weight(1f).fillMaxHeight(), iconSize = 28.dp, shape = RoundedCornerShape(34.dp), background = accentSoft, onClick = actions.onNext)
        }
        Spacer(Modifier.weight(.5f))
        Row(
            Modifier.align(Alignment.CenterHorizontally).fillMaxWidth(.9f).height(60.dp).clip(RoundedCornerShape(50)).background(container).padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            TraySegment(R.drawable.ic_xvox_shuffle, isShuffleEnabled, onSurface, accent, onAccent, Modifier.weight(1f), actions.onShuffle, actions.onShuffleLong)
            TraySegment(if (repeatMode == RepeatMode.ONE) R.drawable.ic_xvox_repeat_one else R.drawable.ic_xvox_repeat, repeatMode != RepeatMode.OFF, onSurface, accent, onAccent, Modifier.weight(1f), actions.onRepeat, actions.onRepeatLong)
            TraySegment(if (isLiked) R.drawable.ic_xvox_heart else R.drawable.ic_xvox_heart_outline, isLiked, onSurface, accent, onAccent, Modifier.weight(1f), actions.onToggleLiked, null)
        }
        Spacer(Modifier.weight(.2f))
        Text(
            "Layout & background", color = onMuted, fontSize = 12.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally).clip(RoundedCornerShape(50)).clickable(onClick = actions.onMore).padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun MaterialLyricsPage(
    song: Song, isPlaying: Boolean, position: Long, duration: Long,
    lyrics: XvoxLyrics?, lyricsLoading: Boolean,
    surface: Color, container: Color, onSurface: Color, onMuted: Color, accent: Color, accentSoft: Color, onAccent: Color,
    actions: XvoxAltNpActions, showThumb: Boolean, modifier: Modifier = Modifier
) {
    var syncedMode by rememberSaveable { mutableStateOf(true) }
    Box(modifier.fillMaxSize()) {
        AltLyrics(
            lyrics = lyrics, loading = lyricsLoading, position = position, onSeek = actions.onSeek, ios = false,
            followSynced = syncedMode, activeColor = accent, idleColor = onMuted,
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 120.dp, bottom = 250.dp), modifier = Modifier.fillMaxSize()
        )
        Row(
            Modifier.statusBarsPadding().padding(start = 16.dp, top = 14.dp).clip(RoundedCornerShape(50)).background(container)
                .padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showThumb) XvoxSongArtwork(artwork = song.artworkUri, requestSize = 160, modifier = Modifier.size(54.dp).clip(CircleShape))
            else Spacer(Modifier.size(54.dp))
            Column(Modifier.padding(horizontal = 14.dp).widthIn(max = 180.dp)) {
                Text(song.title, color = onSurface, fontSize = 17.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist, color = onMuted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            AltIconButton(R.drawable.ic_xvox_more, onMuted, 28.dp, onClick = actions.onLyricsSettings)
        }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, surface.copy(alpha = .94f), surface)))
                .navigationBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AltIconButton(if (isPlaying) R.drawable.ic_xvox_pause else R.drawable.ic_xvox_play, onAccent, 84.dp, iconSize = 36.dp, background = accent, onClick = actions.onTogglePlay)
                Spacer(Modifier.width(12.dp))
                Box(Modifier.weight(1f).height(76.dp).clip(RoundedCornerShape(50)).background(container).padding(horizontal = 18.dp), contentAlignment = Alignment.Center) {
                    AltSlider(
                        fraction = if (duration > 0) position / duration.toFloat() else 0f,
                        onCommit = { f -> actions.onSeek((f * duration).toLong()) },
                        trackColor = accent.copy(alpha = .25f), fillColor = accent, thin = 6.dp, thick = 12.dp, thumbRadius = 11.dp
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                AltIconButton(R.drawable.ic_xvox_arrow_left, onSurface, 52.dp, background = accentSoft, onClick = actions.onCloseLyrics)
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SegmentChip("Synced", syncedMode, accent, onAccent, onSurface, accentSoft, Modifier.weight(1f)) { syncedMode = true }
                    SegmentChip("Static", !syncedMode, accent, onAccent, onSurface, accentSoft, Modifier.weight(1f)) { syncedMode = false }
                }
                AltIconButton(R.drawable.ic_xvox_more, onSurface, 52.dp, background = accentSoft, onClick = actions.onLyricsSettings)
            }
        }
    }
}

/**
 * Material You player: background takes the cover's tone, accent-coloured controls, a boxy
 * edge-to-edge cover that becomes the round header avatar of the lyrics page (one shared element).
 */
@Composable
fun XvoxNowPlayingMaterialLayout(
    song: Song, isPlaying: Boolean, position: Long, duration: Long, isLiked: Boolean,
    isShuffleEnabled: Boolean, repeatMode: RepeatMode, playingSource: String,
    lyrics: XvoxLyrics?, lyricsLoading: Boolean, lyricsOpen: Boolean, tone: Color,
    landscape: Boolean, actions: XvoxAltNpActions,
    artwork: @Composable (Modifier, Dp) -> Unit, modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    val surface = androidx.compose.ui.graphics.lerp(colors.background, tone, if (colors.isLight) .22f else .34f)
    val container = androidx.compose.ui.graphics.lerp(surface, tone, .22f)
    val onSurface = colors.primaryText
    val onMuted = colors.mutedText
    val accent = colors.primaryAccent
    val onAccent = if (accent.luminance() > .5f) Color.Black else Color.White
    val accentSoft = androidx.compose.ui.graphics.lerp(surface, accent, .3f)
    val p by animateFloatAsState(if (lyricsOpen) 1f else 0f, tween(520, easing = IosEase), label = "matP")
    BoxWithConstraints(modifier.fillMaxSize().background(surface)) {
        if (landscape) {
            Crossfade(targetState = lyricsOpen, animationSpec = tween(380), label = "matLand") { showLyrics ->
                if (showLyrics) {
                    MaterialLyricsPage(song, isPlaying, position, duration, lyrics, lyricsLoading, surface, container, onSurface, onMuted, accent, accentSoft, onAccent, actions, showThumb = true)
                } else {
                    Row(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        Box(Modifier.fillMaxHeight().aspectRatio(1f).clip(RoundedCornerShape(14.dp))) { artwork(Modifier.fillMaxSize(), 0.dp) }
                        Column(Modifier.weight(1f).fillMaxHeight()) {
                            AltIconButton(R.drawable.ic_xvox_chevron_down, onSurface, 44.dp, background = accentSoft, onClick = actions.onClose)
                            MaterialControls(song, isPlaying, position, duration, isLiked, isShuffleEnabled, repeatMode, playingSource, onSurface, onMuted, container, accent, accentSoft, onAccent, actions, Modifier.weight(1f))
                        }
                    }
                }
            }
            return@BoxWithConstraints
        }
        val density = androidx.compose.ui.platform.LocalDensity.current
        val statusTop = with(density) { androidx.compose.foundation.layout.WindowInsets.statusBars.getTop(density).toDp() }
        val size0 = minOf(maxWidth, maxHeight * .46f)
        val x0 = (maxWidth - size0) / 2
        val y0 = statusTop + 72.dp
        // Lyrics page.
        Crossfade(targetState = lyricsOpen, animationSpec = tween(380), label = "matPages") { showLyrics ->
            if (showLyrics) {
                MaterialLyricsPage(song, isPlaying, position, duration, lyrics, lyricsLoading, surface, container, onSurface, onMuted, accent, accentSoft, onAccent, actions, showThumb = false)
            } else {
                Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp)) {
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        AltIconButton(R.drawable.ic_xvox_chevron_down, onSurface, 50.dp, background = accentSoft, onClick = actions.onClose)
                        Text("Now Playing", color = onSurface, fontSize = 21.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f).padding(start = 18.dp))
                        AltIconButton(R.drawable.ic_xvox_bluetooth, onSurface, 50.dp, shape = RoundedCornerShape(16.dp), background = accentSoft, onClick = actions.onOutput)
                        Spacer(Modifier.width(6.dp))
                        AltIconButton(R.drawable.ic_xvox_queue, onSurface, 50.dp, shape = RoundedCornerShape(16.dp), background = accentSoft, onClick = actions.onQueue)
                    }
                    Spacer(Modifier.height(12.dp + size0))
                    MaterialControls(song, isPlaying, position, duration, isLiked, isShuffleEnabled, repeatMode, playingSource, onSurface, onMuted, container, accent, accentSoft, onAccent, actions, Modifier.weight(1f))
                }
            }
        }
        // Shared cover: boxy + edge-to-edge on the player, round avatar in the lyrics header.
        val s = androidx.compose.ui.unit.lerp(size0, 54.dp, p)
        val cx = androidx.compose.ui.unit.lerp(x0, 22.dp, p)
        val cy = androidx.compose.ui.unit.lerp(y0, statusTop + 20.dp, p)
        val corner = androidx.compose.ui.unit.lerp(14.dp, 27.dp, p)
        Box(Modifier.absoluteOffset(cx, cy).size(s).clip(RoundedCornerShape(corner))) { artwork(Modifier.fillMaxSize(), 0.dp) }
        if (lyricsOpen) {
            Box(Modifier.absoluteOffset(16.dp, statusTop + 14.dp).size(66.dp, 66.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = actions.onCloseLyrics))
        }
    }
}

@Composable
private fun SegmentChip(
    label: String, selected: Boolean, accent: Color, onAccent: Color, onSurface: Color, soft: Color,
    modifier: Modifier, onClick: () -> Unit
) {
    val bg by androidx.compose.animation.animateColorAsState(if (selected) accent else soft, tween(220), label = "segBg")
    Box(
        modifier.height(58.dp).clip(RoundedCornerShape(if (selected) 29.dp else 18.dp)).background(bg)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) onAccent else onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun TraySegment(
    res: Int, selected: Boolean, onSurface: Color, accent: Color, onAccent: Color,
    modifier: Modifier, onClick: () -> Unit, onLongClick: (() -> Unit)?
) {
    val bg by androidx.compose.animation.animateColorAsState(if (selected) accent else Color.Transparent, tween(220), label = "trayBg")
    AltIconButton(
        res, if (selected) onAccent else onSurface, 52.dp, modifier = modifier.fillMaxHeight(), iconSize = 24.dp,
        shape = RoundedCornerShape(24.dp), background = bg, onLongClick = onLongClick, onClick = onClick
    )
}
