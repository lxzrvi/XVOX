package com.xvox.music.features.home.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.data.preferences.XvoxPlaylist
import com.xvox.music.features.home.SongArtwork

@OptIn(
    ExperimentalFoundationApi::class
)
@Composable
fun PlaylistCoverEditor(
    playlist: XvoxPlaylist,
    songs: List<Song>,
    onCancel: () -> Unit,
    onApply: (
        List<Long>,
        Uri?
    ) -> Unit
) {
    val colors =
        XvoxTheme.colors

    val initial =
        remember(
            playlist.id
        ) {
            val stored =
                playlist
                    .coverSongIds
                    .filter {
                        id ->

                        songs.any {
                            it.id == id
                        }
                    }

            (
                stored +
                    songs
                        .map {
                            it.id
                        }
                        .filterNot {
                            it in stored
                        }
                )
                .distinct()
                .take(4)
        }

    val selected =
        remember(
            playlist.id
        ) {
            mutableStateListOf<Long>()
                .apply {
                    addAll(initial)
                }
        }

    var customUri by
        remember(
            playlist.id
        ) {
            mutableStateOf<Uri?>(
                null
            )
        }

    val picker =
        rememberLauncherForActivityResult(
            ActivityResultContracts
                .PickVisualMedia()
        ) {
            uri ->

            if (uri != null) {
                customUri = uri
            }
        }

    Column(
        modifier =
            Modifier.fillMaxWidth()
    ) {
        Text(
            text =
                "Playlist cover",
            color =
                colors.primaryText,
            fontSize = 18.sp,
            modifier =
                Modifier.padding(
                    end = 44.dp
                )
        )

        LazyVerticalGrid(
            columns =
                GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .padding(
                    top = 14.dp
                ),
            horizontalArrangement =
                Arrangement.spacedBy(
                    7.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(
                    7.dp
                )
        ) {
            items(
                items = songs,
                key = {
                    it.id
                }
            ) {
                song ->

                val selectedIndex =
                    selected.indexOf(
                        song.id
                    )

                val active =
                    selectedIndex >= 0

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(
                            RoundedCornerShape(
                                10.dp
                            )
                        )
                        .background(
                            colors.card
                        )
                        .then(
                            if (active) {
                                Modifier.border(
                                    width =
                                        2.dp,
                                    color =
                                        colors
                                            .primaryAccent,
                                    shape =
                                        RoundedCornerShape(
                                            10.dp
                                        )
                                )
                            } else {
                                Modifier
                            }
                        )
                        .clickable(
                            interactionSource =
                                remember {
                                    MutableInteractionSource()
                                },
                            indication = null
                        ) {
                            customUri =
                                null

                            if (active) {
                                selected.removeAt(
                                    selectedIndex
                                )

                                selected.add(
                                    song.id
                                )
                            } else {
                                if (
                                    selected.size >=
                                    4
                                ) {
                                    selected.removeAt(
                                        0
                                    )
                                }

                                selected.add(
                                    song.id
                                )
                            }
                        }
                ) {
                    SongArtwork(
                        artwork =
                            song.artworkUri,
                        requestSize = 128,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(
                                    1f
                                )
                    )

                    if (active) {
                        Box(
                            modifier =
                                Modifier
                                    .align(
                                        Alignment
                                            .TopStart
                                    )
                                    .padding(
                                        5.dp
                                    )
                                    .size(
                                        19.dp
                                    )
                                    .background(
                                        colors
                                            .primaryAccent,
                                        RoundedCornerShape(
                                            6.dp
                                        )
                                    ),
                            contentAlignment =
                                Alignment.Center
                        ) {
                            Text(
                                text =
                                    (
                                        selectedIndex +
                                            1
                                        )
                                        .toString(),
                                color =
                                    colors
                                        .background,
                                fontSize =
                                    9.sp
                            )
                        }
                    }
                }
            }

            item(
                key =
                    "custom_cover"
            ) {
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(
                            RoundedCornerShape(
                                10.dp
                            )
                        )
                        .background(
                            colors.card
                        )
                        .then(
                            if (
                                customUri !=
                                null
                            ) {
                                Modifier.border(
                                    width =
                                        2.dp,
                                    color =
                                        colors
                                            .primaryAccent,
                                    shape =
                                        RoundedCornerShape(
                                            10.dp
                                        )
                                )
                            } else {
                                Modifier
                            }
                        )
                        .clickable(
                            interactionSource =
                                remember {
                                    MutableInteractionSource()
                                },
                            indication = null
                        ) {
                            picker.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts
                                        .PickVisualMedia
                                        .ImageOnly
                                )
                            )
                        },
                    contentAlignment =
                        Alignment.Center
                ) {
                    if (
                        customUri !=
                        null
                    ) {
                        AsyncImage(
                            model =
                                customUri,
                            contentDescription =
                                null,
                            contentScale =
                                ContentScale.Crop,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(
                                        1f
                                    )
                        )
                    } else {
                        Icon(
                            painter =
                                painterResource(
                                    R.drawable
                                        .ic_xvox_plus
                                ),
                            contentDescription =
                                "Custom cover",
                            tint =
                                colors
                                    .primaryText,
                            modifier =
                                Modifier.size(
                                    22.dp
                                )
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = 14.dp
                ),
            horizontalArrangement =
                Arrangement.spacedBy(
                    10.dp
                )
        ) {
            CoverAction(
                title = "Cancel",
                onClick =
                    onCancel,
                modifier =
                    Modifier.weight(1f)
            )

            CoverAction(
                title = "Okay",
                onClick = {
                    onApply(
                        selected.toList(),
                        customUri
                    )
                },
                modifier =
                    Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CoverAction(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier =
        Modifier
) {
    val colors =
        XvoxTheme.colors

    Box(
        modifier =
            modifier
                .height(44.dp)
                .background(
                    colors.card,
                    RoundedCornerShape(
                        14.dp
                    )
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
        Text(
            text = title,
            color =
                colors.primaryText,
            fontSize = 13.sp
        )
    }
}
