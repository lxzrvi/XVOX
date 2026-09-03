package com.xvox.music.player.nowplaying.lyrics

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme

@Composable
fun XvoxArtworkLyrics(
    state: XvoxLyricsUiState,
    position: Long,
    onSeek: (Long) -> Unit,
    onAttach: (Uri) -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
    onFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors

    val custom =
        state.lyrics?.source ==
            XvoxLyricsSource.USER_LRC ||
            state.lyrics?.source ==
            XvoxLyricsSource.USER_TEXT

    val launcher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            uri?.let(onAttach)
        }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                colors.background.copy(
                    alpha = 0.27f
                )
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, _ ->
                        change.consume()
                    }
                )
            }
    ) {
        when {
            state.loading -> {
                Text(
                    text = "Loading lyrics…",
                    color = colors.secondaryText,
                    fontSize = 12.sp,
                    modifier =
                        Modifier.align(
                            Alignment.Center
                        )
                )
            }

            state.lyrics != null -> {
                XvoxSyncedLyrics(
                    lyrics = state.lyrics,
                    position = position,
                    onSeek = onSeek,
                    modifier =
                        Modifier.fillMaxSize()
                )
            }

            else -> {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(54.dp)
                        .background(
                            colors.card.copy(
                                alpha = 0.25f
                            ),
                            CircleShape
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
                        tint = colors.primaryText,
                        modifier =
                            Modifier.size(23.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(9.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            if (custom) {
                LyricsDeleteButton(
                    onClick = onDelete
                )

                Spacer(
                    Modifier.size(7.dp)
                )
            }

            Row(
                modifier = Modifier
                    .background(
                        colors.card.copy(
                            alpha = 0.20f
                        ),
                        RoundedCornerShape(
                            18.dp
                        )
                    )
                    .padding(horizontal = 3.dp),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                LyricsAction(
                    resource =
                        R.drawable
                            .ic_xvox_fullscreen,
                    onClick = onFullscreen
                )

                LyricsAction(
                    resource =
                        R.drawable
                            .ic_xvox_close,
                    onClick = onClose
                )
            }
        }
    }
}

@Composable
private fun LyricsDeleteButton(
    onClick: () -> Unit
) {
    val colors = XvoxTheme.colors

    Box(
        modifier = Modifier
            .size(36.dp)
            .background(
                colors.card.copy(
                    alpha = 0.20f
                ),
                CircleShape
            )
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
                painterResource(
                    R.drawable.ic_xvox_delete
                ),
            contentDescription =
                "Remove custom lyrics",
            tint = colors.primaryText,
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun LyricsAction(
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
