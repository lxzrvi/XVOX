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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme

@Composable
fun XvoxArtworkLyrics(
    state: XvoxLyricsUiState,
    position: Long,
    onAttach: (Uri) -> Unit,
    onClose: () -> Unit,
    onFullscreen: () -> Unit,
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
        modifier = modifier
            .fillMaxSize()
            .background(
                colors.background.copy(
                    alpha = 0.88f
                )
            )
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(9.dp)
                .height(42.dp)
                .background(
                    colors.card.copy(
                        alpha = 0.34f
                    ),
                    RoundedCornerShape(22.dp)
                )
                .padding(horizontal = 3.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            LyricsAction(
                R.drawable.ic_xvox_fullscreen,
                onFullscreen
            )

            LyricsAction(
                R.drawable.ic_xvox_close,
                onClose
            )
        }

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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = 50.dp,
                            bottom = 18.dp
                        )
                )
            }

            else -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
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
                        }
                        .padding(20.dp),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                colors.card.copy(
                                    alpha = 0.38f
                                ),
                                RoundedCornerShape(23.dp)
                            ),
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
                                Modifier.size(22.dp)
                        )
                    }

                    Text(
                        text = "No lyrics",
                        color = colors.primaryText,
                        fontSize = 13.sp,
                        modifier =
                            Modifier.padding(
                                top = 10.dp
                            )
                    )

                    Text(
                        text = "Add LRC or text",
                        color = colors.secondaryText,
                        fontSize = 10.sp,
                        modifier =
                            Modifier.padding(
                                top = 3.dp
                            )
                    )
                }
            }
        }
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
            painter = painterResource(resource),
            contentDescription = null,
            tint = colors.primaryText,
            modifier = Modifier.size(18.dp)
        )
    }
}
