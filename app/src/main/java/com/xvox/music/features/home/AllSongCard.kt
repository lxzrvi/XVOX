package com.xvox.music.features.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import kotlinx.coroutines.delay

@Composable
fun AllSongCard(
    song: Song,
    current: Boolean,
    playing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors =
        XvoxTheme.colors

    val cardShape =
        RoundedCornerShape(11.dp)

    val artworkShape =
        RoundedCornerShape(7.dp)

    val interactionSource =
        remember {
            MutableInteractionSource()
        }

    var showPausedPlay by
        remember(song.id) {
            mutableStateOf(false)
        }

    LaunchedEffect(
        current,
        playing
    ) {
        if (
            current &&
            !playing
        ) {
            showPausedPlay = true

            delay(900)

            showPausedPlay = false
        } else {
            showPausedPlay = false
        }
    }

    Column(
        modifier = modifier
            .clip(cardShape)
            .background(
                colors.card
            )
            .border(
                width = 0.7.dp,
                color = colors.cardBorder,
                shape = cardShape
            )
            .padding(5.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(artworkShape)
                .background(
                    colors.cardElevated
                )
                .clickable(
                    interactionSource =
                        interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment =
                Alignment.Center
        ) {
            SongArtwork(
                artwork =
                    song.artworkUri,
                requestSize =
                    GridArtworkSize,
                modifier =
                    Modifier.fillMaxSize()
            )

            androidx.compose.animation.AnimatedVisibility(
                visible =
                    current &&
                        (
                            playing ||
                                showPausedPlay
                            ),
                modifier =
                    Modifier.align(
                        Alignment.Center
                    ),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(
                            Color.Black.copy(
                                alpha = 0.58f
                            )
                        ),
                    contentAlignment =
                        Alignment.Center
                ) {
                    PlaybackIcon(
                        type =
                            if (playing) {
                                PlaybackIconType.PAUSE
                            } else {
                                PlaybackIconType.PLAY
                            },
                        color =
                            Color.White,
                        modifier =
                            Modifier.size(
                                13.dp
                            )
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement =
                Arrangement.Center
        ) {
            Text(
                text = song.title,
                color =
                    colors.primaryText,
                fontSize = 10.sp,
                lineHeight = 11.sp,
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
                lineHeight = 9.sp,
                maxLines = 1,
                overflow =
                    TextOverflow.Ellipsis
            )
        }
    }
}
