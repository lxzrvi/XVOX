package com.xvox.music.player.nowplaying

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme

@Composable
fun XvoxNowPlayingLyricsPreview(
    onLyricsSelected: (Uri) -> Unit,
    onFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors =
        XvoxTheme.colors

    val launcher =
        rememberLauncherForActivityResult(
            ActivityResultContracts
                .OpenDocument()
        ) {
            uri ->
            uri?.let(
                onLyricsSelected
            )
        }

    val shape =
        RoundedCornerShape(
            18.dp
        )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(132.dp)
            .background(
                colors.card.copy(
                    alpha = 0.24f
                ),
                shape
            )
            .border(
                0.65.dp,
                Color.White.copy(
                    alpha = 0.13f
                ),
                shape
            )
    ) {
        Row(
            modifier = Modifier
                .align(
                    Alignment.TopEnd
                )
                .padding(8.dp)
        ) {
            LyricsButton(
                resource =
                    R.drawable
                        .ic_xvox_fullscreen,
                onClick =
                    onFullscreen
            )
        }

        Column(
            modifier =
                Modifier.align(
                    Alignment.Center
                ),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            Text(
                text = "No lyrics",
                color =
                    colors.secondaryText,
                fontSize = 12.sp
            )

            Box(
                modifier = Modifier
                    .padding(
                        top = 8.dp
                    )
                    .size(38.dp)
                    .clickable(
                        interactionSource =
                            remember {
                                MutableInteractionSource()
                            },
                        indication = null
                    ) {
                        launcher.launch(
                            arrayOf(
                                "text/*",
                                "application/octet-stream"
                            )
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
                        "Add lyrics from files",
                    tint =
                        colors.primaryText,
                    modifier =
                        Modifier.size(
                            20.dp
                        )
                )
            }

            Text(
                text =
                    "Add from files",
                color =
                    colors.mutedText,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
private fun LyricsButton(
    resource: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
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
                    resource
                ),
            contentDescription = null,
            tint = Color.White,
            modifier =
                Modifier.size(
                    17.dp
                )
        )
    }
}
