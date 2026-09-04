package com.xvox.music.features.home.library

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.data.preferences.XvoxPlaylist
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PlaylistDetail(
    playlist: XvoxPlaylist,
    songs: List<Song>,
    currentSongId: Long?,
    isPlaying: Boolean,
    onPlay: (Song) -> Unit,
    onOptions: (Song) -> Unit,
    onClosed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    val scope = rememberCoroutineScope()

    var expanded by remember {
        mutableStateOf(false)
    }

    val scale by
        animateFloatAsState(
            targetValue =
                if (expanded) {
                    1f
                } else {
                    0.86f
                },
            animationSpec =
                tween(240),
            label =
                "playlistDetailScale"
        )

    val alpha by
        animateFloatAsState(
            targetValue =
                if (expanded) {
                    1f
                } else {
                    0f
                },
            animationSpec =
                tween(200),
            label =
                "playlistDetailAlpha"
        )

    fun close() {
        scope.launch {
            expanded = false
            delay(220L)
            onClosed()
        }
    }

    LaunchedEffect(
        playlist.id
    ) {
        expanded = true
    }

    BackHandler {
        close()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
    ) {
        Text(
            text = playlist.name,
            color =
                colors.primaryText,
            fontSize = 22.sp,
            fontWeight =
                FontWeight.Bold,
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 12.dp
            )
        )

        Text(
            text =
                "${songs.size} songs",
            color =
                colors.secondaryText,
            fontSize = 11.sp,
            modifier = Modifier.padding(
                horizontal = 12.dp
            )
        )

        Spacer(
            Modifier.height(10.dp)
        )

        if (songs.isEmpty()) {
            Text(
                text =
                    "No songs in this playlist",
                color =
                    colors.mutedText,
                fontSize = 12.sp,
                modifier =
                    Modifier.padding(
                        12.dp
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
                    }
                )
            }
        }
    }
}
