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
        modifier = modifier
            .size(size)
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

/**
 * Apple-Music-style player: a full-bleed cover that dissolves into the blurred, cover-coloured
 * background, big bold title, thin progress, large transport, volume and a bottom icon row.
 * Lyrics open as a separate bold, blur-focus page with a small cover/title header.
 */
@Composable
fun XvoxNowPlayingIosLayout(
    song: Song,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    isLiked: Boolean,
    isShuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    playingSource: String,
    lyrics: XvoxLyrics?,
    lyricsLoading: Boolean,
    lyricsOpen: Boolean,
    accent: Color,
    actions: XvoxAltNpActions,
    artwork: @Composable (Modifier, Dp) -> Unit,
    modifier: Modifier = Modifier
) {
    val white = Color.White
    BoxWithConstraints(modifier.fillMaxSize()) {
        val coverHeight = maxHeight * .47f
        // Darken a touch so white text stays readable over light covers.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        .45f to Color.Black.copy(alpha = .18f),
                        1f to Color.Black.copy(alpha = .46f)
                    )
                )
        )
        Crossfade(targetState = lyricsOpen, animationSpec = tween(420), label = "iosPages") { showLyrics ->
            if (showLyrics) {
                Box(Modifier.fillMaxSize()) {
                    AltLyrics(
                        lyrics = lyrics,
                        loading = lyricsLoading,
                        position = position,
                        onSeek = actions.onSeek,
                        ios = true,
                        followSynced = true,
                        activeColor = white,
                        idleColor = white,
                        contentPadding = PaddingValues(start = 26.dp, end = 26.dp, top = 150.dp, bottom = 210.dp),
                        modifier = Modifier.fillMaxSize()
                    )
                    // Header: small cover + title/artist + share.
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(start = 22.dp, end = 22.dp, top = 26.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = actions.onCloseLyrics
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        XvoxSongArtwork(
                            artwork = song.artworkUri,
                            requestSize = 160,
                            modifier = Modifier.size(58.dp).clip(RoundedCornerShape(10.dp))
                        )
                        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                            Text(song.title, color = white, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.artist, color = white.copy(alpha = .62f), fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        AltIconButton(
                            R.drawable.ic_xvox_share, white, 44.dp,
                            background = white.copy(alpha = .2f),
                            onClick = actions.onShare
                        )
                    }
                    // Slim transport at the bottom so playback stays reachable on the lyrics page.
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
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AltIconButton(R.drawable.ic_xvox_skip_previous, white, 56.dp, onClick = actions.onPrevious)
                            AltIconButton(
                                if (isPlaying) R.drawable.ic_xvox_pause else R.drawable.ic_xvox_play,
                                white, 64.dp, iconSize = 38.dp, onClick = actions.onTogglePlay
                            )
                            AltIconButton(R.drawable.ic_xvox_skip_next, white, 56.dp, onClick = actions.onNext)
                        }
                    }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    // Cover bleeds under the status bar and melts into the background below.
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(coverHeight)
                            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                            .drawWithContent {
                                drawContent()
                                drawRect(
                                    Brush.verticalGradient(0f to Color.Black, .62f to Color.Black, 1f to Color.Transparent),
                                    blendMode = BlendMode.DstIn
                                )
                            }
                    ) {
                        artwork(Modifier.fillMaxSize(), 0.dp)
                    }
                    Column(
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .weight(1f)
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 26.dp)
                    ) {
                        Spacer(Modifier.weight(.9f))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(song.title, color = white, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.artist, color = white.copy(alpha = .6f), fontSize = 21.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            AltIconButton(
                                if (isLiked) R.drawable.ic_xvox_heart else R.drawable.ic_xvox_heart_outline,
                                if (isLiked) accent else white, 40.dp,
                                background = white.copy(alpha = .2f),
                                onClick = actions.onToggleLiked
                            )
                            Spacer(Modifier.width(10.dp))
                            AltIconButton(
                                R.drawable.ic_xvox_more, white, 40.dp,
                                background = white.copy(alpha = .2f),
                                onClick = actions.onMore
                            )
                        }
                        Spacer(Modifier.weight(.8f))
                        // Current lyric line, tap to open the lyrics page.
                        val current = lyrics?.lines?.let { ls ->
                            if (lyrics.synchronized) ls.getOrNull(ls.indexOfLast { (it.timeMs ?: Long.MAX_VALUE) <= position })?.text
                            else ls.firstOrNull { it.text.isNotBlank() }?.text
                        }.orEmpty()
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = actions.onOpenLyrics
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (current.isNotBlank()) current else "Lyrics",
                                color = white.copy(alpha = .78f),
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
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
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AltIconButton(R.drawable.ic_xvox_skip_previous, white, 78.dp, iconSize = 46.dp, onClick = actions.onPrevious)
                            AltIconButton(
                                if (isPlaying) R.drawable.ic_xvox_pause else R.drawable.ic_xvox_play,
                                white, 88.dp, iconSize = 56.dp, onClick = actions.onTogglePlay
                            )
                            AltIconButton(R.drawable.ic_xvox_skip_next, white, 78.dp, iconSize = 46.dp, onClick = actions.onNext)
                        }
                        Spacer(Modifier.weight(.8f))
                        val (volume, setVolume) = rememberVolumeState()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(R.drawable.ic_xvox_volume_low), null, tint = white.copy(alpha = .7f), modifier = Modifier.size(20.dp))
                            AltSlider(
                                fraction = volume,
                                onCommit = { setVolume(it) },
                                onChange = { setVolume(it) },
                                trackColor = white.copy(alpha = .26f), fillColor = white.copy(alpha = .92f),
                                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                            )
                            Icon(painterResource(R.drawable.ic_xvox_volume_high), null, tint = white.copy(alpha = .7f), modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.weight(.5f))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AltIconButton(R.drawable.ic_xvox_lyrics, white.copy(alpha = .85f), 48.dp, onClick = actions.onOpenLyrics)
                            AltIconButton(
                                R.drawable.ic_xvox_shuffle,
                                if (isShuffleEnabled) accent else white.copy(alpha = .85f), 48.dp,
                                onLongClick = actions.onShuffleLong, onClick = actions.onShuffle
                            )
                            AltIconButton(R.drawable.ic_xvox_bluetooth, white.copy(alpha = .85f), 48.dp, onClick = actions.onOutput)
                            AltIconButton(
                                if (repeatMode == RepeatMode.ONE) R.drawable.ic_xvox_repeat_one else R.drawable.ic_xvox_repeat,
                                if (repeatMode != RepeatMode.OFF) accent else white.copy(alpha = .85f), 48.dp,
                                onLongClick = actions.onRepeatLong, onClick = actions.onRepeat
                            )
                            AltIconButton(R.drawable.ic_xvox_queue, white.copy(alpha = .85f), 48.dp, onClick = actions.onQueue)
                        }
                        Text(
                            "Playing from $playingSource",
                            color = white.copy(alpha = .5f),
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 2.dp, bottom = 6.dp)
                        )
                    }
                }
            }
        }
        // Grab handle + source, floating over the cover like Apple Music.
        if (!lyricsOpen) {
            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 6.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = actions.onClose
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(Modifier.width(40.dp).height(5.dp).clip(CircleShape).background(white.copy(alpha = .8f)))
                Text(
                    "Playing from $playingSource",
                    color = white,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        } else {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 6.dp)
                    .width(40.dp).height(5.dp).clip(CircleShape).background(white.copy(alpha = .8f))
            )
        }
    }
}

// ─────────────────────────────── Material You layout ───────────────────────────────

/**
 * Material 3 Expressive-style player: flat tonal surface, big rounded cover, title with a lyrics
 * button, thumb slider, three chunky pill transport buttons (the play pill morphs shape) and a
 * segmented shuffle / repeat / like tray. Lyrics: calm list + round play + progress pill +
 * Synced/Static switch.
 */
@Composable
fun XvoxNowPlayingMaterialLayout(
    song: Song,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    isLiked: Boolean,
    isShuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    playingSource: String,
    lyrics: XvoxLyrics?,
    lyricsLoading: Boolean,
    lyricsOpen: Boolean,
    actions: XvoxAltNpActions,
    artwork: @Composable (Modifier, Dp) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    val surface = colors.background
    val container = colors.cardElevated
    val onSurface = colors.primaryText
    val onMuted = colors.mutedText
    val primary = colors.primaryAccent
    val onPrimary = colors.background
    val tonal = colors.primaryText.copy(alpha = .62f)
    Box(modifier.fillMaxSize().background(surface)) {
        Crossfade(targetState = lyricsOpen, animationSpec = tween(380), label = "matPages") { showLyrics ->
            if (showLyrics) {
                var syncedMode by rememberSaveable { mutableStateOf(true) }
                Box(Modifier.fillMaxSize()) {
                    AltLyrics(
                        lyrics = lyrics,
                        loading = lyricsLoading,
                        position = position,
                        onSeek = actions.onSeek,
                        ios = false,
                        followSynced = syncedMode,
                        activeColor = onSurface,
                        idleColor = onMuted,
                        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 120.dp, bottom = 250.dp),
                        modifier = Modifier.fillMaxSize()
                    )
                    // Header pill.
                    Row(
                        Modifier
                            .statusBarsPadding()
                            .padding(start = 16.dp, top = 14.dp)
                            .clip(RoundedCornerShape(50))
                            .background(container)
                            .padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        XvoxSongArtwork(
                            artwork = song.artworkUri,
                            requestSize = 160,
                            modifier = Modifier.size(54.dp).clip(CircleShape)
                        )
                        Column(Modifier.padding(horizontal = 14.dp).widthIn(max = 180.dp)) {
                            Text(song.title, color = onSurface, fontSize = 17.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.artist, color = onMuted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        AltIconButton(R.drawable.ic_xvox_more, onMuted, 28.dp, onClick = actions.onLyricsSettings)
                    }
                    // Bottom controls over a soft scrim.
                    Column(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Color.Transparent, surface.copy(alpha = .94f), surface)))
                            .navigationBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AltIconButton(
                                if (isPlaying) R.drawable.ic_xvox_pause else R.drawable.ic_xvox_play,
                                onPrimary, 92.dp, iconSize = 40.dp,
                                background = onSurface.copy(alpha = .86f),
                                onClick = actions.onTogglePlay
                            )
                            Spacer(Modifier.width(12.dp))
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(80.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(container)
                                    .padding(horizontal = 18.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AltSlider(
                                    fraction = if (duration > 0) position / duration.toFloat() else 0f,
                                    onCommit = { f -> actions.onSeek((f * duration).toLong()) },
                                    trackColor = onSurface.copy(alpha = .2f), fillColor = onSurface.copy(alpha = .86f),
                                    thin = 6.dp, thick = 12.dp, thumbRadius = 11.dp
                                )
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AltIconButton(
                                R.drawable.ic_xvox_arrow_left, onSurface, 52.dp,
                                background = container, onClick = actions.onCloseLyrics
                            )
                            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SegmentChip("Synced", syncedMode, onSurface, container, Modifier.weight(1f)) { syncedMode = true }
                                SegmentChip("Static", !syncedMode, onSurface, container, Modifier.weight(1f)) { syncedMode = false }
                            }
                            AltIconButton(
                                R.drawable.ic_xvox_more, onSurface, 52.dp,
                                background = container, onClick = actions.onLyricsSettings
                            )
                        }
                    }
                }
            } else {
                Column(
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp)
                ) {
                    // Top bar.
                    Row(
                        Modifier.fillMaxWidth().padding(top = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AltIconButton(
                            R.drawable.ic_xvox_chevron_down, onSurface, 50.dp,
                            background = container, onClick = actions.onClose
                        )
                        Text(
                            "Now Playing",
                            color = onSurface,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f).padding(start = 18.dp)
                        )
                        AltIconButton(
                            R.drawable.ic_xvox_bluetooth, onSurface, 50.dp,
                            shape = RoundedCornerShape(16.dp), background = container, onClick = actions.onOutput
                        )
                        Spacer(Modifier.width(6.dp))
                        AltIconButton(
                            R.drawable.ic_xvox_queue, onSurface, 50.dp,
                            shape = RoundedCornerShape(16.dp), background = container, onClick = actions.onQueue
                        )
                    }
                    Spacer(Modifier.weight(.55f))
                    Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
                        artwork(Modifier.fillMaxSize(), 30.dp)
                    }
                    Spacer(Modifier.weight(.7f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(song.title, color = onSurface, fontSize = 30.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.artist, color = onMuted, fontSize = 19.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        AltIconButton(
                            R.drawable.ic_xvox_lyrics, onSurface, 58.dp,
                            background = container, onClick = actions.onOpenLyrics
                        )
                    }
                    Spacer(Modifier.weight(.5f))
                    AltSlider(
                        fraction = if (duration > 0) position / duration.toFloat() else 0f,
                        onCommit = { f -> actions.onSeek((f * duration).toLong()) },
                        trackColor = onSurface.copy(alpha = .16f), fillColor = onSurface.copy(alpha = .8f),
                        thin = 6.dp, thick = 12.dp, thumbRadius = 11.dp
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(fmtTime(position), color = onMuted, fontSize = 14.sp)
                        Text(
                            playingSource,
                            color = onMuted,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(container)
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                        Text(fmtTime(duration), color = onMuted, fontSize = 14.sp)
                    }
                    Spacer(Modifier.weight(.6f))
                    // Transport: three chunky pills; the play pill morphs rounder while playing.
                    val playRadius by animateDpAsState(
                        if (isPlaying) 30.dp else 48.dp,
                        spring(dampingRatio = .62f, stiffness = Spring.StiffnessMediumLow),
                        label = "playMorph"
                    )
                    Row(
                        Modifier.fillMaxWidth().height(92.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AltIconButton(
                            R.drawable.ic_xvox_skip_previous, onPrimary, 92.dp,
                            modifier = Modifier.weight(1f).fillMaxHeight(), iconSize = 34.dp,
                            shape = RoundedCornerShape(46.dp), background = tonal,
                            onClick = actions.onPrevious
                        )
                        AltIconButton(
                            if (isPlaying) R.drawable.ic_xvox_pause else R.drawable.ic_xvox_play,
                            onPrimary, 92.dp,
                            modifier = Modifier.weight(1f).fillMaxHeight(), iconSize = 40.dp,
                            shape = RoundedCornerShape(playRadius), background = onSurface.copy(alpha = .86f),
                            onClick = actions.onTogglePlay
                        )
                        AltIconButton(
                            R.drawable.ic_xvox_skip_next, onPrimary, 92.dp,
                            modifier = Modifier.weight(1f).fillMaxHeight(), iconSize = 34.dp,
                            shape = RoundedCornerShape(46.dp), background = tonal,
                            onClick = actions.onNext
                        )
                    }
                    Spacer(Modifier.weight(.5f))
                    // Segmented tray: shuffle | repeat | like.
                    Row(
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .fillMaxWidth(.9f)
                            .height(62.dp)
                            .clip(RoundedCornerShape(50))
                            .background(container.copy(alpha = .7f))
                            .padding(5.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        TraySegment(
                            R.drawable.ic_xvox_shuffle, isShuffleEnabled, onSurface, primary,
                            Modifier.weight(1f), actions.onShuffle, actions.onShuffleLong
                        )
                        TraySegment(
                            if (repeatMode == RepeatMode.ONE) R.drawable.ic_xvox_repeat_one else R.drawable.ic_xvox_repeat,
                            repeatMode != RepeatMode.OFF, onSurface, primary,
                            Modifier.weight(1f), actions.onRepeat, actions.onRepeatLong
                        )
                        TraySegment(
                            if (isLiked) R.drawable.ic_xvox_heart else R.drawable.ic_xvox_heart_outline,
                            isLiked, onSurface, primary,
                            Modifier.weight(1f), actions.onToggleLiked, null
                        )
                    }
                    Spacer(Modifier.weight(.25f))
                    // Overflow lives in the title area of the other layouts; keep it reachable here.
                    Text(
                        "Layout & background",
                        color = onMuted,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = actions.onMore)
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SegmentChip(
    label: String,
    selected: Boolean,
    onSurface: Color,
    container: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val bg by androidx.compose.animation.animateColorAsState(
        if (selected) onSurface.copy(alpha = .78f) else container, tween(220), label = "segBg"
    )
    Box(
        modifier
            .height(58.dp)
            .clip(RoundedCornerShape(if (selected) 29.dp else 18.dp))
            .background(bg)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) XvoxTheme.colors.background else onSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TraySegment(
    res: Int,
    selected: Boolean,
    onSurface: Color,
    primary: Color,
    modifier: Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?
) {
    val bg by androidx.compose.animation.animateColorAsState(
        if (selected) primary.copy(alpha = .28f) else Color.Transparent, tween(220), label = "trayBg"
    )
    AltIconButton(
        res, if (selected) primary else onSurface, 52.dp,
        modifier = modifier.fillMaxHeight(), iconSize = 24.dp,
        shape = RoundedCornerShape(24.dp), background = bg,
        onLongClick = onLongClick, onClick = onClick
    )
}
