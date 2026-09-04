package com.xvox.music.features.home.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.features.home.SongArtwork

@Composable
fun AddPlaylistSongsBox(
    songs: List<Song>,
    existingSongIds: Set<Long>,
    onAdd: (Song) -> Unit
) {
    val colors =
        XvoxTheme.colors

    val available =
        remember(
            songs,
            existingSongIds
        ) {
            songs.filterNot {
                it.id in
                    existingSongIds
            }
        }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    end = 42.dp
                )
    ) {
        Text(
            text =
                "Add songs",
            color =
                colors.primaryText,
            fontSize = 18.sp,
            fontWeight =
                FontWeight.Bold
        )

        if (available.isEmpty()) {
            Text(
                text =
                    "All songs are already in this playlist",
                color =
                    colors.secondaryText,
                fontSize = 12.sp,
                modifier =
                    Modifier.padding(
                        top = 18.dp,
                        bottom = 8.dp
                    )
            )

            return
        }

        LazyColumn(
            modifier =
                Modifier
                    .padding(
                        top = 12.dp
                    )
                    .heightIn(
                        max = 340.dp
                    )
        ) {
            items(
                items =
                    available,
                key = {
                    it.id
                }
            ) {
                song ->

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(
                                58.dp
                            )
                            .clickable(
                                interactionSource =
                                    remember {
                                        MutableInteractionSource()
                                    },
                                indication =
                                    null
                            ) {
                                onAdd(
                                    song
                                )
                            },
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    SongArtwork(
                        artwork =
                            song.artworkUri,
                        requestSize = 112,
                        modifier =
                            Modifier
                                .size(
                                    44.dp
                                )
                                .background(
                                    colors.card,
                                    RoundedCornerShape(
                                        9.dp
                                    )
                                )
                    )

                    Column(
                        modifier =
                            Modifier
                                .weight(
                                    1f
                                )
                                .padding(
                                    start =
                                        11.dp
                                )
                    ) {
                        Text(
                            text =
                                song.title,
                            color =
                                colors.primaryText,
                            fontSize = 12.sp,
                            fontWeight =
                                FontWeight
                                    .SemiBold,
                            maxLines = 1,
                            overflow =
                                TextOverflow
                                    .Ellipsis
                        )

                        Text(
                            text =
                                song.artist,
                            color =
                                colors.secondaryText,
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow =
                                TextOverflow
                                    .Ellipsis
                        )
                    }

                    Box(
                        modifier =
                            Modifier
                                .size(
                                    32.dp
                                )
                                .background(
                                    colors.card,
                                    CircleShape
                                ),
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Icon(
                            painter =
                                painterResource(
                                    R.drawable
                                        .ic_xvox_plus
                                ),
                            contentDescription =
                                "Add",
                            tint =
                                colors.primaryText,
                            modifier =
                                Modifier
                                    .size(
                                        16.dp
                                    )
                        )
                    }
                }
            }
        }
    }
}
