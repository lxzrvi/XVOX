package com.xvox.music.features.playlist

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.data.preferences.XvoxPlaylist
import com.xvox.music.features.home.XvoxHomeSectionHeadingText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun XvoxPlaylistDetail(
    playlist: XvoxPlaylist,
    songs: List<Song>,
    currentSongId: Long?,
    isPlaying: Boolean,
    selectedSongIds: Set<Long> = emptySet(),
    onPlay: (Song) -> Unit,
    onOptions: (Song) -> Unit,
    onLongClick: ((Song) -> Unit)? = null,
    onAddSongs: () -> Unit,
    onClosed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = XvoxTheme.colors
    val isSelectionMode = selectedSongIds.isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxWidth()
,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 6.dp, end = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                XvoxHomeSectionHeadingText(
                    title = if (isSelectionMode) "${selectedSongIds.size} Selected" else playlist.name,
                    subtitle = "${songs.size} songs"
                )
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onAddSongs,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_xvox_plus),
                    contentDescription = "Add songs",
                    tint = colors.primaryAccent,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        if (songs.isEmpty()) {
            Text(
                text = "No songs in this playlist",
                color = colors.mutedText,
                fontSize = 12.sp,
                modifier = Modifier.padding(12.dp),
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
    }
}
