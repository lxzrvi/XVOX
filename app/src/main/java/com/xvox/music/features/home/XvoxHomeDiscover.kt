// NAYI FILE BANAO (poori file) - koi extra kaam nahi. Path: app/src/main/java/com/xvox/music/features/home/XvoxHomeDiscover.kt
package com.xvox.music.features.home

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
import androidx.compose.ui.res.painterResource
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
import com.xvox.music.core.ui.effects.xvoxSongPress
import kotlin.random.Random

/** One seed per process: Quick picks reshuffle every time the app is opened, never on recomposition. */
private val SessionSeed: Long = System.nanoTime()

private const val QuickPickCount = 24
private const val QuickPickRows = 4

/**
 * XVOX mark + Cinzel wordmark, parked on the right of the header just before the floating profile
 * avatar. It lives in the scrolling header lane, so it leaves with the page.
 */
@Composable
fun XvoxHomeLogo(modifier: Modifier = Modifier) {
    val colors = XvoxTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = 6.dp, end = 58.dp)
            .height(54.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_launcher_foreground_vector),
                contentDescription = null,
                tint = colors.primaryAccent,
                modifier = Modifier.size(46.dp)
            )
            Text(
                "XVOX",
                color = colors.primaryText,
                fontFamily = XvoxLogoFont,
                fontWeight = FontWeight.Normal,
                fontSize = 22.sp,
                letterSpacing = 2.sp,
                maxLines = 1,
                modifier = Modifier.padding(start = 0.dp)
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

private data class XvoxMood(val title: String, val subtitle: String, val from: Color, val to: Color, val songs: List<Song>)

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

private fun buildMoods(all: List<Song>): List<XvoxMood> {
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
                    MoodCard(mood) {
                        val shuffled = mood.songs.shuffled()
                        onPlay(shuffled.first(), shuffled, mood.title)
                    }
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
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).xvoxGlassRim(RoundedCornerShape(8.dp))
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
    }
}

@Composable
private fun MoodCard(mood: XvoxMood, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .width(168.dp)
            .height(92.dp)
            .xvoxSongPress(onClick = onClick, pressedScale = .96f)
            .clip(shape)
            .background(Brush.linearGradient(listOf(mood.from, mood.to)))
            .xvoxGlassRim(shape)
            .padding(14.dp)
    ) {
        Column(Modifier.align(Alignment.BottomStart)) {
            Text(mood.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${mood.songs.size} songs · ${mood.subtitle}", color = Color.White.copy(alpha = .82f), fontSize = 10.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
