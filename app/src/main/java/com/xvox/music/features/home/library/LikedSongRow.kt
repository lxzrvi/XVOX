package com.xvox.music.features.home.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.features.home.SongArtwork

@OptIn(
    ExperimentalFoundationApi::class
)
@Composable
fun LibrarySongRow(
    song: Song,
    current: Boolean,
    playing: Boolean,
    onClick: () -> Unit,
    onOptions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(62.dp)
            .combinedClickable(
                interactionSource =
                    remember {
                        MutableInteractionSource()
                    },
                indication = null,
                onClick = onClick,
                onLongClick = onOptions
            )
            .padding(
                horizontal = 12.dp,
                vertical = 6.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        SongArtwork(
            artwork = song.artworkUri,
            requestSize = 128,
            modifier = Modifier
                .size(50.dp)
                .clip(
                    RoundedCornerShape(
                        9.dp
                    )
                )
        )

        Spacer(
            Modifier.size(11.dp)
        )

        Column(
            modifier =
                Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                color =
                    colors.primaryText,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                fontWeight =
                    if (current) {
                        FontWeight.Bold
                    } else {
                        FontWeight.SemiBold
                    },
                maxLines = 1,
                overflow =
                    TextOverflow.Ellipsis
            )

            Text(
                text =
                    if (
                        current &&
                        playing
                    ) {
                        "${song.artist} • Playing"
                    } else {
                        song.artist
                    },
                color =
                    colors.secondaryText,
                fontSize = 10.sp,
                lineHeight = 13.sp,
                maxLines = 1,
                overflow =
                    TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .size(38.dp)
                .background(
                    colors.card.copy(
                        alpha = 0.45f
                    ),
                    CircleShape
                )
                .combinedClickable(
                    interactionSource =
                        remember {
                            MutableInteractionSource()
                        },
                    indication = null,
                    onClick = onOptions,
                    onLongClick = onOptions
                ),
            contentAlignment =
                Alignment.Center
        ) {
            Icon(
                painter =
                    painterResource(
                        R.drawable.ic_xvox_more
                    ),
                contentDescription =
                    "Song options",
                tint =
                    colors.primaryText,
                modifier =
                    Modifier.size(18.dp)
            )
        }
    }
}
