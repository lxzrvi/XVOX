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
fun SongOptionsSheet(
    song: Song,
    liked: Boolean,
    onPlayNext: () -> Unit,
    onAddQueue: () -> Unit,
    onPlaylist: () -> Unit,
    onLiked: () -> Unit,
    onDelete: () -> Unit,
    onInfo: () -> Unit,
    onRingtone: () -> Unit,
    onShare: () -> Unit
) {
    val colors = XvoxTheme.colors

    val options =
        listOf(
            SongOption(
                "Play next",
                R.drawable.ic_xvox_skip_next,
                onPlayNext
            ),
            SongOption(
                "Add to queue",
                R.drawable.ic_xvox_queue,
                onAddQueue
            ),
            SongOption(
                "Add to playlist",
                R.drawable.ic_xvox_playlist,
                onPlaylist
            ),
            SongOption(
                if (liked) {
                    "Remove from liked"
                } else {
                    "Add to liked"
                },
                R.drawable.ic_xvox_heart,
                onLiked
            ),
            SongOption(
                "Delete",
                R.drawable.ic_xvox_delete,
                onDelete
            ),
            SongOption(
                "Info",
                R.drawable.ic_xvox_info,
                onInfo
            ),
            SongOption(
                "Set ringtone",
                R.drawable.ic_xvox_music_note,
                onRingtone
            ),
            SongOption(
                "Share",
                R.drawable.ic_xvox_share,
                onShare
            )
        )

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
            SongArtwork(
                artwork = song.artworkUri,
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
                modifier =
                    Modifier.weight(1f)
            ) {
                Text(
                    text = song.title,
                    color = colors.primaryText,
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
