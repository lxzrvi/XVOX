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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.data.preferences.XvoxPlaylist

@Composable
fun PlaylistPickerBox(
    playlists: List<XvoxPlaylist>,
    onCreate: () -> Unit,
    onChoose: (XvoxPlaylist) -> Unit
) {
    val colors = XvoxTheme.colors

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
                text = "Playlists",
                color = colors.primaryText,
                fontSize = 18.sp,
                fontWeight =
                    FontWeight.Bold,
                modifier =
                    Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .size(38.dp)
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
                        onClick = onCreate
                    ),
                contentAlignment =
                    Alignment.Center
            ) {
                Text(
                    text = "+",
                    color = colors.primaryText,
                    fontSize = 22.sp
                )
            }
        }

        Spacer(
            Modifier.height(14.dp)
        )

        if (playlists.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource =
                            remember {
                                MutableInteractionSource()
                            },
                        indication = null,
                        onClick = onCreate
                    ),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            colors.card,
                            CircleShape
                        ),
                    contentAlignment =
                        Alignment.Center
                ) {
                    Text(
                        text = "+",
                        color =
                            colors.primaryText,
                        fontSize = 25.sp
                    )
                }

                Spacer(
                    Modifier.height(8.dp)
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
                    playlists,
                    key = {
                        it.id
                    }
                ) {
                    playlist ->

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clickable(
                                interactionSource =
                                    remember {
                                        MutableInteractionSource()
                                    },
                                indication = null
                            ) {
                                onChoose(
                                    playlist
                                )
                            },
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Column(
                            modifier =
                                Modifier.weight(1f)
                        ) {
                            Text(
                                text =
                                    playlist.name,
                                color =
                                    colors.primaryText,
                                fontSize = 14.sp,
                                fontWeight =
                                    FontWeight.SemiBold
                            )

                            Text(
                                text =
                                    "${playlist.songIds.size} songs",
                                color =
                                    colors.secondaryText,
                                fontSize = 10.sp
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
    val colors = XvoxTheme.colors

    var name by remember {
        mutableStateOf("")
    }

    val selected =
        remember(initialSong?.id) {
            mutableStateListOf<Long>()
                .also {
                    initialSong?.let {
                        song ->
                        it.add(song.id)
                    }
                }
        }

    Column(
        modifier =
            Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Create playlist",
            color = colors.primaryText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            Modifier.height(12.dp)
        )

        BasicTextField(
            value = name,
            onValueChange = {
                name = it
            },
            singleLine = true,
            textStyle =
                androidx.compose.ui.text
                    .TextStyle(
                        color =
                            colors.primaryText,
                        fontSize = 14.sp
                    ),
            cursorBrush =
                SolidColor(
                    colors.primaryText
                ),
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    colors.card,
                    RoundedCornerShape(
                        14.dp
                    )
                )
                .height(48.dp),
            decorationBox = {
                field ->

                Box(
                    modifier =
                        Modifier.fillMaxWidth(),
                    contentAlignment =
                        Alignment.CenterStart
                ) {
                    if (name.isBlank()) {
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
            Modifier.height(12.dp)
        )

        LazyColumn(
            modifier =
                Modifier.heightIn(
                    max = 250.dp
                )
        ) {
            items(
                songs,
                key = {
                    it.id
                }
            ) {
                song ->

                val checked =
                    song.id in selected

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clickable(
                            interactionSource =
                                remember {
                                    MutableInteractionSource()
                                },
                            indication = null
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
                        text = song.title,
                        color =
                            colors.primaryText,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow =
                            TextOverflow.Ellipsis,
                        modifier =
                            Modifier.weight(1f)
                    )

                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(
                                if (checked) {
                                    colors.primaryText
                                } else {
                                    colors.card
                                }
                            )
                    )
                }
            }
        }

        Spacer(
            Modifier.height(12.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
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
                        name,
                        selected.toSet()
                    )
                },
            contentAlignment =
                Alignment.Center
        ) {
            Text(
                text = "Create",
                color =
                    if (name.isNotBlank()) {
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
