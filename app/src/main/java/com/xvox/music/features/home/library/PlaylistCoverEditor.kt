package com.xvox.music.features.home.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.data.preferences.XvoxPlaylist
import com.xvox.music.features.home.SongArtwork

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

    val selected =
        remember(
            playlist.id
        ) {
            mutableStateListOf<Long>()
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
                selected.clear()
                customUri = uri
            }
        }

    val canApply =
        selected.isNotEmpty() ||
            customUri != null

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
            fontWeight =
                FontWeight.Bold
        )

        LazyVerticalGrid(
            columns =
                GridCells.Fixed(4),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 14.dp
                    )
                    .heightIn(
                        max = 280.dp
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

                val index =
                    selected.indexOf(
                        song.id
                    )

                val active =
                    index >= 0

                val shape =
                    RoundedCornerShape(
                        10.dp
                    )

                Box(
                    modifier =
                        Modifier
                            .aspectRatio(
                                1f
                            )
                            .clip(shape)
                            .background(
                                colors.card
                            )
                            .border(
                                width =
                                    if (active) {
                                        2.dp
                                    } else {
                                        0.6.dp
                                    },
                                color =
                                    if (active) {
                                        colors.primaryAccent
                                    } else {
                                        colors.cardBorder
                                    },
                                shape = shape
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
                                    selected.remove(
                                        song.id
                                    )
                                } else {
                                    if (
                                        selected.size >=
                                        4
                                    ) {
                                        selected.clear()
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
                        requestSize = 112,
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
                                        Alignment.TopStart
                                    )
                                    .padding(
                                        5.dp
                                    )
                                    .size(
                                        19.dp
                                    )
                                    .background(
                                        colors.primaryAccent,
                                        RoundedCornerShape(
                                            6.dp
                                        )
                                    ),
                            contentAlignment =
                                Alignment.Center
                        ) {
                            Text(
                                text =
                                    (index + 1)
                                        .toString(),
                                color =
                                    colors.background,
                                fontSize = 9.sp,
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item(
                key = "custom_cover"
            ) {
                val shape =
                    RoundedCornerShape(
                        10.dp
                    )

                Box(
                    modifier =
                        Modifier
                            .aspectRatio(
                                1f
                            )
                            .clip(shape)
                            .background(
                                colors.card
                            )
                            .border(
                                width =
                                    if (
                                        customUri !=
                                        null
                                    ) {
                                        2.dp
                                    } else {
                                        0.6.dp
                                    },
                                color =
                                    if (
                                        customUri !=
                                        null
                                    ) {
                                        colors.primaryAccent
                                    } else {
                                        colors.cardBorder
                                    },
                                shape =
                                    shape
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
                                "Custom playlist cover",
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
                                colors.primaryText,
                            modifier =
                                Modifier.size(
                                    21.dp
                                )
                        )
                    }
                }
            }
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 14.dp
                    ),
            horizontalArrangement =
                Arrangement.spacedBy(
                    10.dp
                )
        ) {
            CoverButton(
                title = "Cancel",
                enabled = true,
                onClick =
                    onCancel,
                modifier =
                    Modifier.weight(
                        1f
                    )
            )

            CoverButton(
                title = "Okay",
                enabled =
                    canApply,
                onClick = {
                    if (canApply) {
                        onApply(
                            selected.toList(),
                            customUri
                        )
                    }
                },
                modifier =
                    Modifier.weight(
                        1f
                    )
            )
        }
    }
}

@Composable
private fun CoverButton(
    title: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors =
        XvoxTheme.colors

    Box(
        modifier =
            modifier
                .heightIn(
                    min = 44.dp
                )
                .background(
                    colors.card,
                    RoundedCornerShape(
                        14.dp
                    )
                )
                .clickable(
                    enabled = enabled,
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
                if (enabled) {
                    colors.primaryText
                } else {
                    colors.mutedText
                },
            fontSize = 13.sp,
            fontWeight =
                FontWeight.SemiBold
        )
    }
}
