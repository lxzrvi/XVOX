package com.xvox.music.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.data.preferences.XvoxPlaylist

@Composable
fun XvoxPlaylistPickerBoxContent(
    song: Song,
    playlists: List<XvoxPlaylist>,
    onAddToPlaylist: (String) -> Unit,
    onCreatePlaylist: () -> Unit,
    onCancel: () -> Unit
) {
    val colors = XvoxTheme.colors
    var selectedPlaylistId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Add to Playlist",
                    color = colors.primaryText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = song.title,
                    color = colors.secondaryText,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(colors.card)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onCreatePlaylist
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_xvox_plus),
                    contentDescription = "Create playlist",
                    tint = colors.primaryAccent,
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        if (playlists.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No playlists created yet", color = colors.mutedText, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp, max = 340.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(items = playlists, key = { it.id }) { playlist ->
                    val alreadyAdded = song.id in playlist.songIds
                    val selected = selectedPlaylistId == playlist.id

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) colors.card.copy(alpha = 0.95f) else Color.Transparent)
                            .clickable(
                                enabled = !alreadyAdded,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                selectedPlaylistId = if (selected) null else playlist.id
                            }
                            .padding(horizontal = 8.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playlist.name,
                                color = when {
                                    alreadyAdded -> colors.mutedText
                                    selected -> colors.primaryAccent
                                    else -> colors.primaryText
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = if (alreadyAdded) "Already added" else "${playlist.songIds.size} songs",
                                color = colors.secondaryText,
                                fontSize = 11.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        alreadyAdded -> colors.cardElevated
                                        selected -> colors.primaryAccent
                                        else -> colors.card
                                    }
                                )
                                .border(
                                    width = 1.dp,
                                    color = when {
                                        selected -> colors.primaryAccent
                                        alreadyAdded -> colors.mutedText
                                        else -> colors.cardBorder
                                    },
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_xvox_check),
                                    contentDescription = "Selected",
                                    tint = colors.background,
                                    modifier = Modifier.size(14.dp)
                                )
                            } else if (!alreadyAdded) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_xvox_plus),
                                    contentDescription = "Add",
                                    tint = colors.primaryText,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.card)
                    .clickable { onCancel() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Cancel",
                    color = colors.primaryText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            val canAdd = selectedPlaylistId != null
            Box(
                modifier = Modifier
                    .weight(1.5f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (canAdd) colors.primaryAccent else colors.cardElevated)
                    .clickable(
                        enabled = canAdd,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        selectedPlaylistId?.let { onAddToPlaylist(it) }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (canAdd) "Add to Playlist" else "Select Playlist",
                    color = if (canAdd) colors.background else colors.mutedText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
