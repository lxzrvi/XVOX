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
import com.xvox.music.features.home.HomeGeometry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun XvoxPlaylistDetail(
    playlist: XvoxPlaylist,
    songs: List<Song>,
    currentSongId: Long?,
    isPlaying: Boolean,
    onPlay: (Song) -> Unit,
    onOptions: (Song) -> Unit,
    onAddSongs: () -> Unit,
    onClosed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors =
        XvoxTheme.colors

    val scope =
        rememberCoroutineScope()

    var expanded by remember(
        playlist.id,
    ) {
        mutableStateOf(false)
    }

    val scale by
        animateFloatAsState(
            targetValue =
                if (expanded) {
                    1f
                } else {
                    0.94f
                },
            animationSpec =
                tween(220),
            label =
                "playlistDetailScale",
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
                tween(180),
            label =
                "playlistDetailAlpha",
        )

    fun close() {
        scope.launch {
            expanded = false

            delay(210L)

            onClosed()
        }
    }

    LaunchedEffect(
        playlist.id,
    ) {
        expanded = true
    }

    BackHandler {
        close()
    }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                },
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 12.dp,
                        top = 2.dp,
                        end = 12.dp,
                        bottom = HomeGeometry.sectionGap,
                    ),
            verticalAlignment =
                Alignment.CenterVertically,
        ) {
            Column(
                modifier =
                    Modifier.weight(
                        1f,
                    ),
            ) {
                Text(
                    text =
                        playlist.name,
                    color =
                        colors.primaryText,
                    fontSize = 22.sp,
                    fontWeight =
                        FontWeight.Bold,
                    maxLines = 1,
                    overflow =
                        TextOverflow.Ellipsis,
                )

                Text(
                    text =
                        "${songs.size} songs",
                    color =
                        colors.secondaryText,
                    fontSize = 11.sp,
                )
            }

            Box(
                modifier =
                    Modifier
                        .size(
                            38.dp,
                        ).background(
                            colors.card,
                            CircleShape,
                        ).clickable(
                            interactionSource =
                                remember {
                                    MutableInteractionSource()
                                },
                            indication = null,
                            onClick =
                            onAddSongs,
                        ),
                contentAlignment =
                    Alignment.Center,
            ) {
                Icon(
                    painter =
                        painterResource(
                            R.drawable
                                .ic_xvox_plus,
                        ),
                    contentDescription =
                        "Add songs",
                    tint =
                        colors.primaryText,
                    modifier =
                        Modifier.size(
                            18.dp,
                        ),
                )
            }
        }

        if (songs.isEmpty()) {
            Text(
                text =
                    "No songs in this playlist",
                color =
                    colors.mutedText,
                fontSize = 12.sp,
                modifier =
                    Modifier.padding(
                        12.dp,
                    ),
            )
        } else {
            Column(
                modifier =
                    Modifier.padding(
                        horizontal =
                            6.dp,
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
    }
}
