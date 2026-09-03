package com.xvox.music.player.nowplaying.lyrics

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.features.home.SongArtwork
import com.xvox.music.player.nowplaying.XvoxNowPlayingBackdrop
import com.xvox.music.player.nowplaying.XvoxNowPlayingProgress

@Composable
fun XvoxFullscreenLyrics(
    song: Song,
    state: XvoxLyricsUiState,
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    backgroundColor: Color,
    onAttach: (Uri) -> Unit,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors

    val launcher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            uri?.let(onAttach)
        }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        XvoxNowPlayingBackdrop(
            dominant = backgroundColor,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.statusBars
                )
                .padding(
                    start = 14.dp,
                    top = 10.dp,
                    end = 14.dp,
                    bottom = 12.dp
                )
        ) {
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                SongArtwork(
                    artwork = song.artworkUri,
                    requestSize = 128,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(
                            RoundedCornerShape(
                                10.dp
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(
                            start = 10.dp,
                            end = 8.dp
                        )
                ) {
                    Text(
                        text = song.title,
                        color = colors.primaryText,
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        fontWeight =
                            FontWeight.Bold,
                        maxLines = 1,
                        overflow =
                            TextOverflow.Ellipsis
                    )

                    Text(
                        text = song.artist,
                        color =
                            colors.secondaryText,
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        maxLines = 1,
                        overflow =
                            TextOverflow.Ellipsis
                    )
                }

                LyricsTransport(
                    isPlaying = isPlaying,
                    onPrevious = onPrevious,
                    onTogglePlay =
                        onTogglePlay,
                    onNext = onNext
                )

                LyricsIconButton(
                    resource =
                        R.drawable.ic_xvox_close,
                    onClick = onClose
                )
            }

            XvoxNowPlayingProgress(
                position = position,
                duration = duration,
                onSeek = onSeek,
                modifier =
                    Modifier.padding(
                        top = 9.dp
                    )
            )

            when {
                state.loading -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Text(
                            text =
                                "Loading lyrics…",
                            color =
                                colors.secondaryText,
                            fontSize = 13.sp
                        )
                    }
                }

                state.lyrics != null -> {
                    XvoxSyncedLyrics(
                        lyrics = state.lyrics,
                        position = position,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }

                else -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .background(
                                        colors.card.copy(
                                            alpha = 0.30f
                                        ),
                                        RoundedCornerShape(
                                            25.dp
                                        )
                                    )
                                    .clickable(
                                        interactionSource =
                                            remember {
                                                MutableInteractionSource()
                                            },
                                        indication = null
                                    ) {
                                        launcher.launch(
                                            arrayOf("*/*")
                                        )
                                    },
                                contentAlignment =
                                    Alignment.Center
                            ) {
                                Icon(
                                    painter =
                                        painterResource(
                                            R.drawable
                                                .ic_xvox_lyrics_add
                                        ),
                                    contentDescription =
                                        "Add lyrics",
                                    tint =
                                        colors.primaryText,
                                    modifier =
                                        Modifier.size(
                                            22.dp
                                        )
                                )
                            }

                            Spacer(
                                Modifier.size(10.dp)
                            )

                            Text(
                                text = "No lyrics",
                                color =
                                    colors.primaryText,
                                fontSize = 15.sp,
                                fontWeight =
                                    FontWeight.Bold
                            )

                            Text(
                                text =
                                    "Add LRC or text",
                                color =
                                    colors.secondaryText,
                                fontSize = 11.sp,
                                modifier =
                                    Modifier.padding(
                                        top = 4.dp
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricsTransport(
    isPlaying: Boolean,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit
) {
    val colors = XvoxTheme.colors

    Row(
        modifier = Modifier
            .clip(
                RoundedCornerShape(24.dp)
            )
            .background(
                colors.card.copy(
                    alpha = 0.38f
                )
            )
            .padding(horizontal = 3.dp),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        LyricsIconButton(
            R.drawable.ic_xvox_skip_previous,
            onPrevious
        )

        LyricsIconButton(
            if (isPlaying) {
                R.drawable.ic_xvox_pause
            } else {
                R.drawable.ic_xvox_play
            },
            onTogglePlay
        )

        LyricsIconButton(
            R.drawable.ic_xvox_skip_next,
            onNext
        )
    }
}

@Composable
private fun LyricsIconButton(
    resource: Int,
    onClick: () -> Unit
) {
    val colors = XvoxTheme.colors

    Box(
        modifier = Modifier
            .size(36.dp)
            .clickable(
                interactionSource =
                    remember {
                        MutableInteractionSource()
                    },
                indication = null,
                onClick = onClick
            ),
        contentAlignment =
            Alignment.Center
    ) {
        Icon(
            painter =
                painterResource(resource),
            contentDescription = null,
            tint = colors.primaryText,
            modifier = Modifier.size(18.dp)
        )
    }
}
