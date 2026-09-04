package com.xvox.music.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.data.preferences.XvoxPlaylist

@Composable
fun PlaylistPickerBox(
    song: Song,
    playlists: List<XvoxPlaylist>,
    onCreate: () -> Unit,
    onAdd: (XvoxPlaylist) -> Unit,
    onRemove: (XvoxPlaylist) -> Unit
) {
    val colors =
        XvoxTheme.colors

    Column(
        modifier =
            Modifier.fillMaxWidth()
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Text(
                text =
                    "Playlists",
                color =
                    colors.primaryText,
                fontSize = 18.sp,
                fontWeight =
                    FontWeight.Bold,
                modifier =
                    Modifier.weight(
                        1f
                    )
            )

            Box(
                modifier =
                    Modifier
                        .size(
                            36.dp
                        )
                        .background(
                            colors.card,
                            CircleShape
                        )
                        .clickable(
                            interactionSource =
                                remember {
                                    MutableInteractionSource()
                                },
                            indication = null,
                            onClick =
                                onCreate
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
                        "Create playlist",
                    tint =
                        colors.primaryText,
                    modifier =
                        Modifier.size(
                            18.dp
                        )
                )
            }
        }

        Spacer(
            Modifier.height(
                14.dp
            )
        )

        if (
            playlists.isEmpty()
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource =
                                remember {
                                    MutableInteractionSource()
                                },
                            indication = null,
                            onClick =
                                onCreate
                        )
                        .padding(
                            vertical =
                                10.dp
                        ),
                horizontalAlignment =
                    Alignment
                        .CenterHorizontally
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(
                                46.dp
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
                            null,
                        tint =
                            colors.primaryText,
                        modifier =
                            Modifier.size(
                                21.dp
                            )
                    )
                }

                Spacer(
                    Modifier.height(
                        8.dp
                    )
                )

                Text(
                    text =
                        "Create a new playlist",
                    color =
                        colors.secondaryText,
                    fontSize = 12.sp
                )
            }
        } else {
            LazyColumn(
                modifier =
                    Modifier.heightIn(
                        max = 300.dp
                    )
            ) {
                items(
                    items =
                        playlists,
                    key = {
                        it.id
                    }
                ) {
                    playlist ->

                    val contains =
                        song.id in
                            playlist.songIds

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
                                    if (
                                        contains
                                    ) {
                                        onRemove(
                                            playlist
                                        )
                                    } else {
                                        onAdd(
                                            playlist
                                        )
                                    }
                                },
                        verticalAlignment =
                            Alignment
                                .CenterVertically
                    ) {
                        Column(
                            modifier =
                                Modifier
                                    .weight(
                                        1f
                                    )
                                    .padding(
                                        end =
                                            8.dp
                                    )
                        ) {
                            Text(
                                text =
                                    playlist.name,
                                color =
                                    colors.primaryText,
                                fontSize = 14.sp,
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
                                    if (
                                        contains
                                    ) {
                                        "Remove from this playlist"
                                    } else {
                                        "${playlist.songIds.size} songs"
                                    },
                                color =
                                    if (
                                        contains
                                    ) {
                                        colors
                                            .primaryAccent
                                    } else {
                                        colors
                                            .secondaryText
                                    },
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }

                        if (contains) {
                            Icon(
                                painter =
                                    painterResource(
                                        R.drawable
                                            .ic_xvox_check
                                    ),
                                contentDescription =
                                    null,
                                tint =
                                    colors
                                        .primaryAccent,
                                modifier =
                                    Modifier.size(
                                        16.dp
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreatePlaylistBox(
    songs: List<Song>,
    initialSong: Song?,
    onCreate: (
        String,
        Set<Long>
    ) -> Unit
) {
    val colors =
        XvoxTheme.colors

    var name by
        remember {
            mutableStateOf("")
        }

    val selected =
        remember(
            initialSong?.id
        ) {
            mutableStateListOf<Long>()
                .apply {
                    initialSong
                        ?.let {
                            add(it.id)
                        }
                }
        }

    Column(
        modifier =
            Modifier.fillMaxWidth()
    ) {
        Text(
            text =
                "Create playlist",
            color =
                colors.primaryText,
            fontSize = 18.sp,
            fontWeight =
                FontWeight.Bold
        )

        Spacer(
            Modifier.height(
                14.dp
            )
        )

        BasicTextField(
            value = name,
            onValueChange = {
                name = it
            },
            singleLine = true,
            textStyle =
                TextStyle(
                    color =
                        colors.primaryText,
                    fontSize = 14.sp
                ),
            cursorBrush =
                SolidColor(
                    colors.primaryText
                ),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(
                        48.dp
                    )
                    .background(
                        colors.card,
                        RoundedCornerShape(
                            14.dp
                        )
                    ),
            decorationBox = {
                field ->

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(
                                48.dp
                            )
                            .padding(
                                horizontal =
                                    14.dp
                            ),
                    contentAlignment =
                        Alignment.CenterStart
                ) {
                    if (
                        name.isEmpty()
                    ) {
                        Text(
                            text =
                                "Enter name",
                            color =
                                colors.secondaryText,
                            fontSize = 13.sp
                        )
                    }

                    field()
                }
            }
        )

        Spacer(
            Modifier.height(
                12.dp
            )
        )

        LazyColumn(
            modifier =
                Modifier.heightIn(
                    max = 250.dp
                )
        ) {
            items(
                items = songs,
                key = {
                    it.id
                }
            ) {
                song ->

                val checked =
                    song.id in
                        selected

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(
                                44.dp
                            )
                            .clickable(
                                interactionSource =
                                    remember {
                                        MutableInteractionSource()
                                    },
                                indication =
                                    null
                            ) {
                                if (checked) {
                                    selected.remove(
                                        song.id
                                    )
                                } else {
                                    selected.add(
                                        song.id
                                    )
                                }
                            },
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Text(
                        text =
                            song.title,
                        color =
                            colors.primaryText,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow =
                            TextOverflow
                                .Ellipsis,
                        modifier =
                            Modifier
                                .weight(
                                    1f
                                )
                                .padding(
                                    end =
                                        10.dp
                                )
                    )

                    Box(
                        modifier =
                            Modifier
                                .size(
                                    15.dp
                                )
                                .background(
                                    if (
                                        checked
                                    ) {
                                        colors
                                            .primaryAccent
                                    } else {
                                        colors.card
                                    },
                                    CircleShape
                                )
                    )
                }
            }
        }

        Spacer(
            Modifier.height(
                12.dp
            )
        )

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(
                        46.dp
                    )
                    .background(
                        colors.card,
                        RoundedCornerShape(
                            14.dp
                        )
                    )
                    .clickable(
                        enabled =
                            name.isNotBlank(),
                        interactionSource =
                            remember {
                                MutableInteractionSource()
                            },
                        indication = null
                    ) {
                        onCreate(
                            name.trim(),
                            selected.toSet()
                        )
                    },
            contentAlignment =
                Alignment.Center
        ) {
            Text(
                text = "Create",
                color =
                    if (
                        name.isNotBlank()
                    ) {
                        colors.primaryText
                    } else {
                        colors.mutedText
                    },
                fontSize = 13.sp,
                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}
