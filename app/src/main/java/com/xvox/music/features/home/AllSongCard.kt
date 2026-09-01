package com.xvox.music.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val colors =
        XvoxTheme.colors

    Column(
        modifier = modifier
            .clip(
                RoundedCornerShape(
                    14.dp
                )
            )
            .background(
                colors.card
            )
            .xvoxPressScale(
                onClick = onClick
            )
            .padding(5.dp)
    ) {
        SongArtwork(
            artwork =
                song.artworkUri,
            modifier = Modifier
                .fillMaxWidth()
                .height(61.dp)
                .clip(
                    RoundedCornerShape(
                        10.dp
                    )
                )
        )

        Text(
            text =
                song.title,
            color =
                colors.primaryText,
            fontSize = 11.sp,
            maxLines = 1,
            overflow =
                TextOverflow.Ellipsis,
            modifier =
                Modifier.padding(
                    top = 4.dp
                )
        )

        Text(
            text =
                song.artist,
            color =
                colors.secondaryText,
            fontSize = 9.sp,
            maxLines = 1,
            overflow =
                TextOverflow.Ellipsis
        )
    }
}
