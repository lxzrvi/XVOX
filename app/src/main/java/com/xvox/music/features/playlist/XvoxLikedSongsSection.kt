package com.xvox.music.features.playlist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.features.home.HomeGeometry

@Composable
fun XvoxLikedSongsSection(
    songs: List<Song>,
    currentSongId: Long?,
    isPlaying: Boolean,
    onPlay: (Song) -> Unit,
    onOptions: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors =
        XvoxTheme.colors

    Column(
        modifier =
            modifier.fillMaxWidth(),
    ) {
        Text(
            text =
                "Liked Songs",
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
                    bottom = HomeGeometry.sectionGap,
                ),
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
                        horizontal = 12.dp,
                        vertical = 24.dp,
                    ),
            )
        } else {
            Column(
                modifier =
                    Modifier.padding(
                        horizontal = 6.dp,
                    ),
            ) {
                songs.forEach { song ->

                    XvoxLikedSongRow(
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
                    )

                    Spacer(
                        Modifier.height(
                            6.dp,
                        ),
                    )
                }
            }
        }

        Spacer(
            Modifier.height(
                8.dp,
            ),
        )
    }
}
