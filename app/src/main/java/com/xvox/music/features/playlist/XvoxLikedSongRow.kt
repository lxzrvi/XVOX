package com.xvox.music.features.playlist

import com.xvox.music.core.ui.effects.xvoxSongPress
import com.xvox.music.features.home.rememberSongCardColor
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.features.home.XvoxSongArtwork

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun XvoxLikedSongRow(
    song: Song,
    current: Boolean,
    playing: Boolean,
    onClick: () -> Unit,
    onOptions: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    val colors = XvoxTheme.colors
    val cardColor = rememberSongCardColor(song, current, selected)
    val shape = RoundedCornerShape(14.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .xvoxSongPress(onClick = onClick, onLongClick = onOptions, pressedScale = 0.95f)
            .clip(shape)
            .background(cardColor)
            .border(width = 0.7.dp, color = colors.cardBorder, shape = shape)
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // The 64dp card has a 3dp frame on every side. Fill its remaining 58dp exactly so
        // Liked, playlist-detail, and artist song rows have the same thin top/bottom inset as
        // their left/right artwork edge.
        XvoxSongArtwork(
            artwork = song.artworkUri,
            requestSize = 128,
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(9.dp))
        )

        Spacer(Modifier.size(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = colors.primaryText,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = if (current && playing) "${song.artist} • Playing" else song.artist,
                color = colors.secondaryText,
                fontSize = 10.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .size(36.dp)
                .combinedClickable(
                    hapticFeedbackEnabled = false,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onOptions,
                    onLongClick = onOptions
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_xvox_more),
                contentDescription = "Song options",
                tint = colors.primaryText,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}
