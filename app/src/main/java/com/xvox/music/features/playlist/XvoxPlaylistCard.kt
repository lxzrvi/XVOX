package com.xvox.music.features.playlist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.data.preferences.XvoxPlaylist

import com.xvox.music.core.ui.effects.xvoxSongPress

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun XvoxPlaylistCard(
    playlist: XvoxPlaylist,
    songs: List<Song>,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    longCard: Boolean = false
) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = modifier
            .xvoxSongPress(onClick = onClick, onLongClick = onLongClick, pressedScale = 0.95f)
            .clip(shape)
            .background(colors.card)
            .border(
                width = 0.7.dp,
                color = colors.cardBorder,
                shape = shape
            )
            .padding(6.dp)
    ) {
        XvoxPlaylistCover(
            songs = songs,
            coverSongIds = playlist.coverSongIds,
            customCoverUri = playlist.customCoverUri,
            requestSize = 192,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (longCard) Modifier.weight(1f) else Modifier.aspectRatio(1f))
                .clip(RoundedCornerShape(11.dp))
        )

        Text(
            text = playlist.name,
            color = colors.primaryText,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 7.dp)
        )

        Text(
            text = "${songs.size} songs",
            color = colors.secondaryText,
            fontSize = 10.sp,
            lineHeight = 12.sp
        )
    }
}
