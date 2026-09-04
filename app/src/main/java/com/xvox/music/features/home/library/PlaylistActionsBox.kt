package com.xvox.music.features.home.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
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
import java.text.DateFormat
import java.util.Date

@Composable
fun PlaylistActionsBox(
    playlist: XvoxPlaylist,
    songs: List<Song>,
    onRename: (String) -> Unit,
    onEditCover: () -> Unit,
    onDelete: () -> Unit,
    onInfo: () -> Unit
) {
    val colors =
        XvoxTheme.colors

    var editing by
        remember(
            playlist.id
        ) {
            mutableStateOf(false)
        }

    var name by
        remember(
            playlist.id,
            playlist.name
        ) {
            mutableStateOf(
                playlist.name
            )
        }

    val focusRequester =
        remember {
            FocusRequester()
        }

    LaunchedEffect(
        editing
    ) {
        if (editing) {
            focusRequester
                .requestFocus()
        }
    }

    Column(
        modifier =
            Modifier.fillMaxWidth()
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        end = 42.dp
                    ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Box(
                modifier =
                    Modifier.size(
                        64.dp
                    ),
                contentAlignment =
                    Alignment.Center
            ) {
                PlaylistCover(
                    songs = songs,
                    coverSongIds =
                        playlist
                            .coverSongIds,
                    customCoverUri =
                        playlist
                            .customCoverUri,
                    requestSize = 128,
                    modifier =
                        Modifier
                            .size(
                                58.dp
                            )
                            .clip(
                                RoundedCornerShape(
                                    12.dp
                                )
                            )
                )

                if (editing) {
                    Box(
                        modifier =
                            Modifier
                                .size(
                                    34.dp
                                )
                                .background(
                                    Color.Black.copy(
                                        alpha =
                                            0.62f
                                    ),
                                    CircleShape
                                )
                                .clickable(
                                    interactionSource =
                                        remember {
                                            MutableInteractionSource()
                                        },
                                    indication =
                                        null,
                                    onClick =
                                        onEditCover
                                ),
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Icon(
                            painter =
                                painterResource(
                                    R.drawable
                                        .ic_xvox_edit
                                ),
                            contentDescription =
                                "Edit cover",
                            tint =
                                Color.White,
                            modifier =
                                Modifier.size(
                                    17.dp
                                )
                        )
                    }
                }
            }

            Spacer(
                Modifier.size(
                    10.dp
                )
            )

            if (editing) {
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
                            fontSize = 17.sp,
                            fontWeight =
                                FontWeight.Bold
                        ),
                    cursorBrush =
                        SolidColor(
                            colors.primaryText
                        ),
                    modifier =
                        Modifier
                            .weight(1f)
                            .focusRequester(
                                focusRequester
                            )
                )
            } else {
                Text(
                    text = name,
                    color =
                        colors.primaryText,
                    fontSize = 17.sp,
                    fontWeight =
                        FontWeight.Bold,
                    maxLines = 1,
                    overflow =
                        TextOverflow.Ellipsis,
                    modifier =
                        Modifier.weight(1f)
                )
            }

            Spacer(
                Modifier.size(
                    8.dp
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
                            indication =
                                null
                        ) {
                            if (editing) {
                                val clean =
                                    name.trim()

                                if (
                                    clean.isNotEmpty()
                                ) {
                                    onRename(
                                        clean
                                    )

                                    editing =
                                        false
                                }
                            } else {
                                editing =
                                    true
                            }
                        },
                contentAlignment =
                    Alignment.Center
            ) {
                Icon(
                    painter =
                        painterResource(
                            if (editing) {
                                R.drawable
                                    .ic_xvox_check
                            } else {
                                R.drawable
                                    .ic_xvox_edit
                            }
                        ),
                    contentDescription =
                        if (editing) {
                            "Save name"
                        } else {
                            "Rename"
                        },
                    tint =
                        if (
                            !editing ||
                            name.isNotBlank()
                        ) {
                            colors.primaryText
                        } else {
                            colors.mutedText
                        },
                    modifier =
                        Modifier.size(
                            17.dp
                        )
                )
            }
        }

        Spacer(
            Modifier.height(
                14.dp
            )
        )

        PlaylistAction(
            title =
                "Delete playlist",
            onClick =
                onDelete
        )

        PlaylistAction(
            title =
                "Playlist info",
            onClick =
                onInfo
        )
    }
}

@Composable
fun PlaylistInfoBox(
    playlist: XvoxPlaylist,
    songCount: Int
) {
    val colors =
        XvoxTheme.colors

    val created =
        if (
            playlist.createdAt >
            0L
        ) {
            DateFormat
                .getDateTimeInstance(
                    DateFormat.MEDIUM,
                    DateFormat.SHORT
                )
                .format(
                    Date(
                        playlist
                            .createdAt
                    )
                )
        } else {
            "Unknown"
        }

    Column(
        modifier =
            Modifier.fillMaxWidth()
    ) {
        Text(
            text =
                "Playlist info",
            color =
                colors.primaryText,
            fontSize = 18.sp,
            fontWeight =
                FontWeight.Bold,
            modifier =
                Modifier.padding(
                    end = 44.dp
                )
        )

        Spacer(
            Modifier.height(
                14.dp
            )
        )

        Text(
            text =
                playlist.name,
            color =
                colors.primaryText,
            fontSize = 14.sp
        )

        Spacer(
            Modifier.height(
                8.dp
            )
        )

        Text(
            text =
                "$songCount songs",
            color =
                colors.secondaryText,
            fontSize = 12.sp
        )

        Spacer(
            Modifier.height(
                6.dp
            )
        )

        Text(
            text =
                "Created $created",
            color =
                colors.secondaryText,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun PlaylistAction(
    title: String,
    onClick: () -> Unit
) {
    val colors =
        XvoxTheme.colors

    Text(
        text = title,
        color =
            colors.primaryText,
        fontSize = 14.sp,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource =
                        remember {
                            MutableInteractionSource()
                        },
                    indication = null,
                    onClick = onClick
                )
                .padding(
                    vertical = 13.dp
                )
    )
}
