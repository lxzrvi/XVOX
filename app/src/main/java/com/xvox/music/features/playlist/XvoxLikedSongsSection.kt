package com.xvox.music.features.playlist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.features.home.XvoxHomeSectionHeading

@Composable
fun XvoxLikedSongsSection(
    songs: List<Song>,
    currentSongId: Long?,
    isPlaying: Boolean,
    selectedSongIds: Set<Long> = emptySet(),
    onPlay: (Song) -> Unit,
    onOptions: (Song) -> Unit,
    onLongClick: ((Song) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = XvoxTheme.colors
    val isSelectionMode = selectedSongIds.isNotEmpty()

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        XvoxHomeSectionHeading(
            title = if (isSelectionMode) "${selectedSongIds.size} Selected" else "Liked Songs",
            subtitle = "Total ${songs.size} songs"
        )

        if (songs.isEmpty()) {
            Text(
                text = "No liked songs",
                color = colors.mutedText,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 24.dp),
            )
        } else {
            Column(modifier = Modifier.padding(horizontal = 6.dp)) {
                songs.forEach { song ->
                    val isSelected = song.id in selectedSongIds
                    XvoxLikedSongRow(
                        song = song,
                        current = currentSongId == song.id,
                        playing = currentSongId == song.id && isPlaying,
                        selected = isSelected,
                        onClick = { onPlay(song) },
                        onOptions = {
                            if (onLongClick != null && isSelectionMode) {
                                onLongClick(song)
                            } else {
                                onOptions(song)
                            }
                        },
                    )

                    Spacer(Modifier.height(6.dp))
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}
