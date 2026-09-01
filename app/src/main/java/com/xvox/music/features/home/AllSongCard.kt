package com.xvox.music.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.effects.xvoxPressScale

@Composable
fun AllSongCard(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    val shape =
        RoundedCornerShape(14.dp)

    Column(
        modifier = modifier
            .clip(shape)
            .background(colors.card)
            .border(
                width = 1.dp,
                color = colors.cardBorder,
                shape = shape
            )
            .xvoxPressScale(
                pressedScale = 0.95f,
                onClick = onClick
            )
            .padding(5.dp)
    ) {
        SongArtwork(
            artwork = song.artworkUri,
            requestSize = 220,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(
                    RoundedCornerShape(
                        10.dp
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = 5.dp,
                    bottom = 3.dp
                ),
            horizontalAlignment =
                Alignment.Start,
            verticalArrangement =
                Arrangement.Center
        ) {
            Text(
                text = song.title,
                color = colors.primaryText,
                fontSize = 11.sp,
                lineHeight = 12.sp,
                fontWeight =
                    FontWeight.SemiBold,
                maxLines = 1,
                overflow =
                    TextOverflow.Ellipsis
            )

            Text(
                text = song.artist,
                color =
                    colors.secondaryText,
                fontSize = 8.sp,
                lineHeight = 10.sp,
                maxLines = 1,
                overflow =
                    TextOverflow.Ellipsis
            )
        }
    }
}
