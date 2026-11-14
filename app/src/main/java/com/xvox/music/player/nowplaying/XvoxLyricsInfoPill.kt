// EXACT CODE REPLACE KARO (poori file) - koi extra kaam nahi. Path: app/src/main/java/com/xvox/music/player/nowplaying/XvoxLyricsInfoPill.kt
package com.xvox.music.player.nowplaying

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.model.Song
import com.xvox.music.features.home.XvoxSongArtwork
import kotlinx.coroutines.delay

/**
 * Pill shown at the top of the xvox lyrics box: cover, title / artist, like and play. It appears when
 * the lyrics open (or the song changes) and fades away after a few seconds, like the other layouts' headers.
 */
@Composable
fun XvoxLyricsInfoPill(
    song: Song,
    isPlaying: Boolean,
    isLiked: Boolean,
    onToggleLiked: () -> Unit,
    onTogglePlay: () -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var shown by remember(song.id) { mutableStateOf(true) }
    LaunchedEffect(song.id) {
        shown = true
        delay(4500)
        shown = false
    }
    val a by animateFloatAsState(if (shown) 1f else 0f, tween(450), label = "lyricsPillA")
    if (a < .01f) return
    val white = Color.White
    Row(
        modifier
            .padding(top = 10.dp, start = 10.dp, end = 10.dp)
            .graphicsLayer { alpha = a; translationY = -(1f - a) * 24.dp.toPx() }
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = .34f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onTap)
            .padding(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        XvoxSongArtwork(artwork = song.artworkUri, requestSize = 120, modifier = Modifier.size(42.dp).clip(CircleShape))
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(song.title, color = white, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artist, color = white.copy(alpha = .68f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            Modifier.size(38.dp).clip(CircleShape)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onToggleLiked),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painterResource(if (isLiked) R.drawable.ic_xvox_heart else R.drawable.ic_xvox_heart_outline), null,
                tint = white, modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(2.dp))
        Box(
            Modifier.size(42.dp).clip(CircleShape).background(white.copy(alpha = .22f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onTogglePlay),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painterResource(if (isPlaying) R.drawable.ic_xvox_pause else R.drawable.ic_xvox_play), null,
                tint = white, modifier = Modifier.size(22.dp)
            )
        }
    }
}
