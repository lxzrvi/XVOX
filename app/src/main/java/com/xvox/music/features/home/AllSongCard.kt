package com.xvox.music.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    val colors =
        XvoxTheme.colors

    val shape =
        RoundedCornerShape(15.dp)

    Column(
        modifier = modifier
            .clip(shape)
            .background(
                colors.card
            )
            .border(
                width = 1.dp,
                color =
                    colors.cardBorder,
                shape = shape
            )
            .xvoxPressScale(
                pressedScale = 0.955f,
                onClick = onClick
            )
            .padding(6.dp)
    ) {
        SongArtwork(
            artwork =
                song.artworkUri,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(
                    RoundedCornerShape(
                        11.dp
                    )
                )
        )

        Text(
            text =
                song.title,
            color =
                colors.primaryText,
            fontSize = 12.sp,
            fontWeight =
                FontWeight.Medium,
            maxLines = 1,
            overflow =
                TextOverflow.Ellipsis,
            modifier =
                Modifier.padding(
                    top = 6.dp
                )
        )

        Text(
            text =
                song.artist,
            color =
                colors.secondaryText,
            fontSize = 10.sp,
            maxLines = 1,
            overflow =
                TextOverflow.Ellipsis
        )
    }
}
