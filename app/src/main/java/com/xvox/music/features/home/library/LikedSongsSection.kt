package com.xvox.music.features.home.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song

@Composable
fun LikedSongsSection(
    songs: List<Song>,
    currentSongId: Long?,
    isPlaying: Boolean,
    onPlay: (Song) -> Unit,
    onOptions: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors =
        XvoxTheme.colors

    Column(
        modifier =
            modifier.fillMaxWidth()
    ) {
        Text(
            text = "Liked Songs",
            color =
                colors.primaryText,
            fontSize = 16.sp,
            lineHeight = 19.sp,
            fontWeight =
                FontWeight.SemiBold,
            modifier =
                Modifier.padding(
                    start = 12.dp,
                    end = 12.dp,
                    bottom = 10.dp
                )
        )

        if (songs.isEmpty()) {
            Text(
                text =
                    "No liked songs",
                color =
                    colors.mutedText,
                fontSize = 12.sp,
                modifier =
                    Modifier.padding(
                        horizontal =
                            12.dp,
                        vertical =
                            24.dp
                    )
            )
        } else {
            songs.forEach {
                song ->

                LibrarySongRow(
                    song = song,
                    current =
                        currentSongId ==
                            song.id,
                    playing =
                        currentSongId ==
                            song.id &&
                            isPlaying,
                    onClick = {
                        onPlay(song)
                    },
                    onOptions = {
                        onOptions(song)
                    },
                    modifier =
                        Modifier
                            .padding(
                                horizontal =
                                    12.dp,
                                vertical =
                                    3.dp
                            )
                            .clip(
                                RoundedCornerShape(
                                    14.dp
                                )
                            )
                            .background(
                                colors.card
                            )
                )
            }
        }

        Spacer(
            Modifier.height(
                8.dp
            )
        )
    }
}
