package com.xvox.music.features.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song

@OptIn(
    ExperimentalFoundationApi::class
)
@Composable
fun AllSongMosaicCard(
    song: Song,
    widthUnits: Float,
    heightUnits: Float,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors

    val interaction =
        remember {
            MutableInteractionSource()
        }

    val pressed by
        interaction.collectIsPressedAsState()

    val scale by
        animateFloatAsState(
            targetValue =
                if (pressed) {
                    0.965f
                } else {
                    1f
                },
            animationSpec =
                spring(
                    dampingRatio = 0.84f,
                    stiffness = 1400f
                ),
            label = "mosaicPress"
        )

    val shape =
        RoundedCornerShape(11.dp)

    val requestSize =
        when {
            widthUnits >= 4f ||
                heightUnits >= 2f ->
                512

            widthUnits >= 2f ->
                320

            else ->
                GridArtworkSize
        }

    Column(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(colors.card)
            .border(
                0.7.dp,
                colors.cardBorder,
                shape
            )
            .combinedClickable(
                interactionSource =
                    interaction,
                indication = null,
                onClick = onClick,
                onLongClick =
                    onLongClick
            )
            .padding(5.dp)
    ) {
        SongArtwork(
            artwork = song.artworkUri,
            requestSize = requestSize,
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .clip(
                    RoundedCornerShape(
                        7.dp
                    )
                )
        )

        Text(
            text = song.title,
            color = colors.primaryText,
            fontSize =
                if (widthUnits >= 2f) {
                    12.sp
                } else {
                    10.sp
                },
            lineHeight =
                if (widthUnits >= 2f) {
                    14.sp
                } else {
                    11.sp
                },
            fontWeight =
                FontWeight.SemiBold,
            maxLines = 1,
            overflow =
                TextOverflow.Ellipsis,
            modifier =
                Modifier.padding(
                    top = 5.dp
                )
        )

        Text(
            text = song.artist,
            color = colors.secondaryText,
            fontSize =
                if (widthUnits >= 2f) {
                    9.sp
                } else {
                    8.sp
                },
            maxLines = 1,
            overflow =
                TextOverflow.Ellipsis
        )
    }
}
