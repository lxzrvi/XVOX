package com.xvox.music.features.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalFoundationApi::class
)
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

    val interaction =
        remember {
            MutableInteractionSource()
        }

    val pressed by
        interaction
            .collectIsPressedAsState()

    val scope =
        rememberCoroutineScope()

    var pulse by remember {
        mutableStateOf(false)
    }

    val scale by
        animateFloatAsState(
            targetValue =
                if (
                    pressed ||
                    pulse
                ) {
                    0.965f
                } else {
                    1f
                },
            animationSpec =
                spring(
                    dampingRatio = 0.78f,
                    stiffness = 800f
                ),
            label =
                "songPress"
        )

    val shape =
        RoundedCornerShape(
            11.dp
        )

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(
                colors.card,
                shape
            )
            .border(
                width = 0.7.dp,
                color =
                    colors.cardBorder,
                shape = shape
            )
            .combinedClickable(
                interactionSource =
                    interaction,
                indication = null,
                onClick = {
                    scope.launch {
                        pulse = true
                        delay(70)
                        pulse = false
                    }

                    onClick()
                },
                onLongClick = {
                    pulse = true

                    scope.launch {
                        delay(90)
                        pulse = false
                    }
                }
            )
            .padding(5.dp)
    ) {
        SongArtwork(
            artwork =
                song.artworkUri,
            requestSize =
                GridArtworkSize,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(
                    colors.cardElevated,
                    RoundedCornerShape(
                        7.dp
                    )
                )
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement =
                Arrangement.Center
        ) {
            Text(
                text =
                    song.title,
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
                text =
                    song.artist,
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
