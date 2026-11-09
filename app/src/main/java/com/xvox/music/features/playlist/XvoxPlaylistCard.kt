// EXACT CODE REPLACE KARO (poori file) - koi extra kaam nahi. Path: app/src/main/java/com/xvox/music/features/playlist/XvoxPlaylistCard.kt
package com.xvox.music.features.playlist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.blur.LocalXvoxSurfaceSky
import com.xvox.music.core.ui.blur.XvoxBlurLevel
import com.xvox.music.core.ui.blur.xvoxLiveBlur
import com.xvox.music.core.ui.components.XvoxLikedHeartBadge
import com.xvox.music.core.ui.components.XvoxSelectionTick
import com.xvox.music.core.ui.effects.xvoxSongPress
import com.xvox.music.data.preferences.XvoxPlaylist

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun XvoxPlaylistCard(
    playlist: XvoxPlaylist,
    songs: List<Song>,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    longCard: Boolean = false,
    /** Outer playlist selection is intentionally separate from song selection inside a playlist. */
    selected: Boolean = false,
    liked: Boolean = false
) {
    val colors = XvoxTheme.colors
    val pageSky = LocalXvoxSurfaceSky.current
    val shape = RoundedCornerShape(16.dp)
    val coverShape = RoundedCornerShape(13.dp)

    Box(
        modifier = modifier
            .xvoxSongPress(onClick = onClick, onLongClick = onLongClick, pressedScale = .95f)
            .xvoxLiveBlur(
                shape = shape,
                level = if (selected) XvoxBlurLevel.Selection else XvoxBlurLevel.Card,
                baseColor = colors.background,
                sky = pageSky,
                borderColor = if (selected) colors.primaryAccent else colors.cardBorder,
                borderAlpha = 1f,
                borderWidth = if (selected) 2.dp else .7.dp
            )
            .padding(3.dp)
    ) {
        Column(if (longCard) Modifier.fillMaxSize() else Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (longCard) Modifier.weight(1f) else Modifier.aspectRatio(1f))
            ) {
                XvoxPlaylistCover(
                    songs = songs,
                    coverSongIds = playlist.coverSongIds,
                    customCoverUri = playlist.customCoverUri,
                    requestSize = 192,
                    coverShape = coverShape,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(coverShape)
                )
                XvoxSelectionTick(selected, coverShape)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(35.dp)
                    .padding(start = 5.dp, end = 4.dp, top = 7.dp)
            ) {
                Text(
                    text = playlist.name,
                    color = colors.primaryText,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "${songs.size} songs",
                    color = colors.secondaryText,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (liked) {
            XvoxLikedHeartBadge(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(7.dp)
            )
        }
    }
}
