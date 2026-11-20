package com.xvox.music.features.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.blur.xvoxGlassRim
import com.xvox.music.core.ui.effects.xvoxSongPress
import com.xvox.music.core.ui.icons.painterResource

/**
 * A mood's own page. The mood colour fills the top of the screen and melts into the page
 * background by roughly the middle, so the cover colour blends downward instead of ending in an edge.
 */
@Composable
fun XvoxMoodPage(
    mood: XvoxMood,
    currentSongId: Long?,
    bottomInset: androidx.compose.ui.unit.Dp,
    onBack: () -> Unit,
    onPlay: (Song, List<Song>) -> Unit,
    onMore: (Song) -> Unit
) {
    val colors = XvoxTheme.colors
    BackHandler(onBack = onBack)
    Box(Modifier.fillMaxSize().background(colors.background)) {
        // Blend: strong at the very top, fully transparent at about 55% of the screen.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to mood.from.copy(alpha = .95f),
                    .22f to mood.to.copy(alpha = .70f),
                    .55f to Color.Transparent
                )
            )
        )
        LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = bottomInset)) {
            item(key = "mood_head") {
                Column(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(start = 16.dp, end = 16.dp, top = 76.dp, bottom = 18.dp)) {
                    MoodMotion(Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(24.dp)))
                    Text(mood.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 32.sp, modifier = Modifier.padding(top = 14.dp))
                    Text("${mood.songs.size} songs · ${mood.subtitle}", color = Color.White.copy(alpha = .82f), fontSize = 13.sp)
                    Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MoodButton("Play", R.drawable.ic_xvox_play) { mood.songs.firstOrNull()?.let { onPlay(it, mood.songs) } }
                        MoodButton("Shuffle", R.drawable.ic_xvox_skip_next) {
                            val l = mood.songs.shuffled(); l.firstOrNull()?.let { onPlay(it, l) }
                        }
                    }
                }
            }
            items(mood.songs, key = { "mood_${it.id}" }) { song ->
                Row(
                    Modifier.fillMaxWidth().xvoxSongPress(onClick = { onPlay(song, mood.songs) }, onLongClick = { onMore(song) }, pressedScale = .97f)
                        .padding(horizontal = 16.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    XvoxSongArtwork(
                        artwork = song.artworkUri,
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).xvoxGlassRim(RoundedCornerShape(8.dp)),
                        requestSize = 128
                    )
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(song.title, color = if (song.id == currentSongId) colors.primaryAccent else colors.primaryText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(song.artist.ifBlank { "Unknown artist" }, color = colors.secondaryText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Box(Modifier.size(36.dp).xvoxSongPress(onClick = { onMore(song) }, pressedScale = .8f), contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.ic_xvox_more), "More", tint = colors.secondaryText, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
        Box(
            Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(start = 16.dp, top = 11.dp)
                .size(44.dp).clip(CircleShape).background(Color.Black.copy(alpha = .28f))
                .xvoxSongPress(onClick = onBack, pressedScale = .9f),
            contentAlignment = Alignment.Center
        ) {
            Icon(painterResource(R.drawable.ic_xvox_chevron_down), "Back", tint = Color.White, modifier = Modifier.size(22.dp).androidx_rotate(90f))
        }
    }
}

private fun Modifier.androidx_rotate(deg: Float) = this.then(Modifier.rotate(deg))

@Composable
private fun MoodButton(label: String, icon: Int, onClick: () -> Unit) {
    Row(
        Modifier.clip(CircleShape).background(Color.White.copy(alpha = .22f))
            .xvoxSongPress(onClick = onClick, pressedScale = .94f)
            .padding(horizontal = 20.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(painterResource(icon), null, tint = Color.White, modifier = Modifier.size(18.dp))
        Text(label, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}
