package com.xvox.music.features.home

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

data class SongOption(
    val title: String,
    @DrawableRes
    val icon: Int,
    val action: () -> Unit
)

@Composable
fun SongOptionsBox(
    song: Song,
    liked: Boolean,
    playlistName: String? = null,
    /** Every playlist that already holds this song, so the menu can offer "Remove from …". */
    membership: List<Pair<String, () -> Unit>> = emptyList(),
    onAddToEach: (() -> Unit)? = null,
    onPlayNext: () -> Unit,
    onAddQueue: () -> Unit,
    onPlaylist: () -> Unit,
    onRemovePlaylist: (() -> Unit)? = null,
    onRemoveRecent: (() -> Unit)? = null,
    onLiked: () -> Unit,
    onDelete: () -> Unit,
    onInfo: () -> Unit,
    onRingtone: () -> Unit,
    onShare: () -> Unit,
    onSelect: (() -> Unit)? = null
) {
    val colors =
        XvoxTheme.colors

    // Membership is checked per playlist, so the menu always matches reality: a playlist that
    // holds the song offers "Remove from this playlist", the others offer "Add to this playlist".
    val playlistOptions: List<SongOption> = buildList {
        if (playlistName != null && onRemovePlaylist != null) {
            add(SongOption("Remove from $playlistName", R.drawable.ic_xvox_delete, onRemovePlaylist))
        } else if (membership.isNotEmpty()) {
            membership.forEach { (name, remove) ->
                add(SongOption("Remove from this playlist · $name", R.drawable.ic_xvox_delete, remove))
            }
            onAddToEach?.let { add(SongOption("Add to this playlist", R.drawable.ic_xvox_playlist, it)) }
        } else {
            add(SongOption("Add to this playlist", R.drawable.ic_xvox_playlist, onPlaylist))
        }
    }

    val options =
        buildList {
            onSelect?.let { add(SongOption("Select", R.drawable.ic_xvox_check, it)) }
            add(
                SongOption(
                    "Play next",
                    R.drawable.ic_xvox_skip_next,
                    onPlayNext
                )
            )

            add(
                SongOption(
                    "Add to queue",
                    R.drawable.ic_xvox_queue,
                    onAddQueue
                )
            )

            addAll(playlistOptions)

            add(
                SongOption(
                    if (liked) {
                        "Remove from liked"
                    } else {
                        "Add to liked"
                    },
                    R.drawable.ic_xvox_heart,
                    onLiked
                )
            )

            if (onRemoveRecent != null) {
                add(
                    SongOption(
                        "Remove from recent",
                        R.drawable.ic_xvox_delete,
                        onRemoveRecent
                    )
                )
            }

            add(
                SongOption(
                    "Delete",
                    R.drawable.ic_xvox_delete,
                    onDelete
                )
            )

            add(
                SongOption(
                    "Info",
                    R.drawable.ic_xvox_info,
                    onInfo
                )
            )

            add(
                SongOption(
                    "Set ringtone",
                    R.drawable.ic_xvox_music_note,
                    onRingtone
                )
            )

            add(
                SongOption(
                    "Share",
                    R.drawable.ic_xvox_share,
                    onShare
                )
            )
        }

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
            XvoxSongArtwork(
                artwork =
                    song.artworkUri,
                requestSize = 128,
                modifier = Modifier
                    .size(54.dp)
                    .clip(
                        RoundedCornerShape(
                            10.dp
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start = 14.dp
                    )
            ) {
                Text(
                    text = song.title,
                    color =
                        colors.primaryText,
                    fontSize = 14.sp,
                    fontWeight =
                        FontWeight.Bold,
                    maxLines = 1,
                    overflow =
                        TextOverflow.Ellipsis
                )

                Text(
                    text = song.artist,
                    color =
                        colors.secondaryText,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow =
                        TextOverflow.Ellipsis
                )
            }
        }

        Spacer(
            Modifier.height(10.dp)
        )

        LazyColumn(
            modifier =
                Modifier.fillMaxWidth()
        ) {
            items(options) {
                option ->

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable(
                            interactionSource =
                                remember {
                                    MutableInteractionSource()
                                },
                            indication = null,
                            onClick =
                                option.action
                        ),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Box(
                        modifier =
                            Modifier.size(
                                42.dp
                            ),
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Icon(
                            painter =
                                painterResource(
                                    option.icon
                                ),
                            contentDescription =
                                null,
                            tint =
                                colors.primaryText,
                            modifier =
                                Modifier.size(
                                    19.dp
                                )
                        )
                    }

                    Text(
                        text =
                            option.title,
                        color =
                            colors.primaryText,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
