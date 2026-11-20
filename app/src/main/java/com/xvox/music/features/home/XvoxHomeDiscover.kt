// NAYI FILE BANAO (poori file) - koi extra kaam nahi. Path: app/src/main/java/com/xvox/music/features/home/XvoxHomeDiscover.kt
package com.xvox.music.features.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import com.xvox.music.core.ui.icons.painterResource
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxLogoFont
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.blur.xvoxGlassRim
import com.xvox.music.core.ui.effects.xvoxThumbBorder
import com.xvox.music.core.ui.effects.xvoxSongPress
import com.xvox.music.core.ui.effects.hiddenWhileHeld
import kotlin.random.Random

/** One seed per process: Quick picks reshuffle every time the app is opened, never on recomposition. */
private val SessionSeed: Long = System.nanoTime()

private const val QuickPickCount = 24
private const val QuickPickRows = 4

/**
 * XVOX app icon + Cinzel wordmark, parked on the left of the header opposite the floating profile
 * avatar. It lives in the scrolling header lane, so it leaves with the page.
 */
@Composable
fun XvoxHomeLogo(modifier: Modifier = Modifier) {
    val tile = RoundedCornerShape(12.dp)
    // 44 dp like the profile button, centred on the same line: status bar + 11 dp + 22 dp.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = 11.dp, start = 16.dp)
            .height(44.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // The real app icon: launcher background tile + white glyph.
        Box(
            Modifier.size(44.dp).clip(tile).background(androidx.compose.ui.res.colorResource(R.color.xvox_launcher_background)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_launcher_foreground_vector),
                contentDescription = "XVOX",
                tint = Color.White,
                modifier = Modifier.size(60.dp)
            )
        }
    }
}

/**
 * Content that scrolls up behind the status bar fades out instead of being cut hard, like the
 * BitChord lists. Only the strip above the status-bar line is touched; everything below is opaque.
 */
@Composable
fun Modifier.xvoxStatusBarFade(enabled: Boolean = true): Modifier {
    if (!enabled) return this
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    return this
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val h = statusBar.toPx() + 6.dp.toPx()
            if (h > 0f) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(0f to Color.Transparent, .4f to Color.Black.copy(alpha = .25f), 1f to Color.Black),
                        startY = 0f,
                        endY = h
                    ),
                    size = Size(size.width, h),
                    blendMode = BlendMode.DstIn
                )
            }
        }
}

data class XvoxMood(val title: String, val subtitle: String, val from: Color, val to: Color, val songs: List<Song>)

private val ChillWords = listOf("chill", "lofi", "lo-fi", "acoustic", "slow", "sleep", "calm", "relax", "unplugged", "soft", "piano")
private val WorkoutWords = listOf("remix", "bass", "gym", "beat", "trap", "edm", "boosted", "workout", "phonk")
private val PartyWords = listOf("dj", "party", "dance", "club", "nonstop", "mashup", "bhangra", "dhol")
private val RomanceWords = listOf("love", "pyar", "ishq", "dil", "romantic", "heart", "tum", "mohabbat", "saath", "kiss")
private val SadWords = listOf("sad", "dard", "judaai", "alone", "broken", "bewafa", "tanha", "cry", "alvida", "tears")
private val FocusWords = listOf("instrumental", "study", "focus", "ambient", "meditation", "flute")

private fun Song.matches(words: List<String>): Boolean {
    val hay = (title + " " + displayName + " " + folderName).lowercase()
    return words.any { hay.contains(it) }
}

fun buildMoods(all: List<Song>): List<XvoxMood> {
    fun pick(words: List<String>) = all.filter { it.matches(words) }
    val rng = Random(SessionSeed)
    val list = listOf(
        XvoxMood("Chill", "Slow it down", Color(0xFF3A7BD5), Color(0xFF00D2FF), pick(ChillWords)),
        XvoxMood("Workout", "Push harder", Color(0xFFFF512F), Color(0xFFF09819), pick(WorkoutWords)),
        XvoxMood("Party", "Turn it up", Color(0xFFDA22FF), Color(0xFF9733EE), pick(PartyWords)),
        XvoxMood("Romance", "Feel it", Color(0xFFFF416C), Color(0xFFFF4B2B), pick(RomanceWords)),
        XvoxMood("Sad hours", "Let it out", Color(0xFF283E51), Color(0xFF4B79A1), pick(SadWords)),
        XvoxMood("Focus", "Stay in the zone", Color(0xFF11998E), Color(0xFF38EF7D), pick(FocusWords)),
        XvoxMood("Short & sweet", "Under 3 minutes", Color(0xFFF7971E), Color(0xFFFFD200), all.filter { it.duration in 1L..180_000L }),
        XvoxMood("Long listens", "Over 5 minutes", Color(0xFF614385), Color(0xFF516395), all.filter { it.duration > 300_000L })
    ).filter { it.songs.size >= 3 }
    val surprise = XvoxMood("Surprise me", "Random mix", Color(0xFF232526), Color(0xFF414345), all.shuffled(rng).take(60))
    return (list + surprise).filter { it.songs.isNotEmpty() }
}

/**
 * Home feed above All Songs: Quick picks (BitChord style: four rows per page, swipe across),
 * Find your mood (rectangular cards) and the All Songs heading.
 */
fun LazyListScope.homeDiscoverItems(
    songs: List<Song>,
    currentSongId: Long?,
    isPlaying: Boolean,
    onPlay: (Song, List<Song>, String) -> Unit,
    onLongClick: (Song) -> Unit,
    onMoodOpen: (XvoxMood) -> Unit = {},
    quickPicksVisible: Boolean = true,
    moodsVisible: Boolean = true
) {
    if (songs.isEmpty()) return
    if (quickPicksVisible) item(key = "home_quick_picks", contentType = "quick_picks") {
        val picks = remember(songs.size) { songs.shuffled(Random(SessionSeed)).take(QuickPickCount) }
        Column(Modifier.padding(bottom = 14.dp)) {
            XvoxHomeSectionHeading("Quick picks", "Start here · ${picks.size} songs")
            BoxWithConstraints {
                val columnWidth: Dp = minOf(maxWidth * .86f, 380.dp)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(picks.chunked(QuickPickRows)) { column ->
                        Column(Modifier.width(columnWidth)) {
                            column.forEach { song ->
                                QuickPickRow(
                                    song = song,
                                    current = song.id == currentSongId,
                                    playing = song.id == currentSongId && isPlaying,
                                    onClick = { onPlay(song, picks, "Quick picks") },
                                    onLongClick = { onLongClick(song) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    if (moodsVisible) item(key = "home_moods", contentType = "moods") {
        val moods = remember(songs.size) { buildMoods(songs) }
        if (moods.isNotEmpty()) Column(Modifier.padding(bottom = 14.dp)) {
            XvoxHomeSectionHeading("Find your mood", "Sorted from your library")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(moods, key = { it.title }) { mood ->
                    MoodCard(mood) { onMoodOpen(mood) }
                }
            }
        }
    }
    item(key = "home_all_songs_heading") {
        XvoxHomeSectionHeading("All songs", "${songs.size} songs")
    }
}

@Composable
private fun QuickPickRow(song: Song, current: Boolean, playing: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val colors = XvoxTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .xvoxSongPress(onClick = onClick, onLongClick = onLongClick, pressedScale = .97f)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        XvoxSongArtwork(
            artwork = song.artworkUri,
            requestSize = 128,
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).xvoxThumbBorder(RoundedCornerShape(8.dp))
        )
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                song.title,
                color = if (current) colors.primaryAccent else colors.primaryText,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                song.artist.ifBlank { "Unknown artist" } + if (playing) " · Playing" else "",
                color = colors.secondaryText,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        // The three dots open the same option sheet as a long press.
        Box(
            Modifier.size(36.dp).hiddenWhileHeld().xvoxSongPress(onClick = onLongClick, pressedScale = .8f),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_xvox_more),
                contentDescription = "More",
                tint = colors.secondaryText,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun MoodCard(mood: XvoxMood, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .width(236.dp)
            .height(148.dp)
            .xvoxSongPress(onClick = onClick, pressedScale = .96f)
            .clip(shape)
            .background(Brush.linearGradient(listOf(mood.from, mood.to)))
            .xvoxGlassRim(shape)
    ) {
        MoodMotion(Modifier.matchParentSize())
        Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) {
            Text(mood.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${mood.songs.size} songs · ${mood.subtitle}", color = Color.White.copy(alpha = .84f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Soft translucent discs and long wave lines drifting very slowly, in the style of iOS cards. */
@Composable
fun MoodMotion(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "moodMotion")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(18_000, easing = LinearEasing), RepeatMode.Restart),
        label = "moodPhase"
    )
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val turn = (2 * PI).toFloat() * phase.value
        drawCircle(
            Color.White.copy(alpha = .15f),
            radius = h * .62f,
            center = Offset(w * (.80f + .06f * cos(turn)), h * (.18f + .09f * sin(turn)))
        )
        drawCircle(
            Color.White.copy(alpha = .10f),
            radius = h * .42f,
            center = Offset(w * (.16f + .08f * sin(turn + 1f)), h * (.92f + .06f * cos(turn + 1f)))
        )
        val stroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
        for (k in 0..2) {
            val path = Path()
            val baseY = h * (.30f + .17f * k)
            val steps = 28
            for (i in 0..steps) {
                val x = w * i / steps
                val y = baseY + h * .06f * sin(i * .42f + turn + k * 1.7f)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, Color.White.copy(alpha = .16f - .03f * k), style = stroke)
        }
    }
}
