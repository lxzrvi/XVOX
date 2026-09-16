package com.xvox.music.shell

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.data.preferences.XvoxPlaylist

/**
 * Add to playlist (also used from Now Playing).
 *
 * A playlist the song is already in shows a check, not a plus, and tapping that check takes the
 * song back out — you never have to leave the box to undo an add. "Select playlist" keeps a light
 * surface and a border in its disabled state, so it reads as a control instead of a dead area.
 */
@Composable
fun XvoxPlaylistPickerBoxContent(
    song: Song,
    playlists: List<XvoxPlaylist>,
    onAddToPlaylist: (String) -> Unit,
    onCreatePlaylist: () -> Unit,
    onCancel: () -> Unit,
    onRemoveFromPlaylist: (String) -> Unit = {}
) {
    val colors = XvoxTheme.colors
    var selectedPlaylistId by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Add to playlist", color = colors.primaryText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(song.title, color = colors.secondaryText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(
                modifier = Modifier.size(34.dp).clip(CircleShape).background(colors.card)
                    .border(0.9.dp, colors.cardBorder, CircleShape)
                    .xvoxPressScale(onClick = onCreatePlaylist),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(R.drawable.ic_xvox_plus), "Create playlist", tint = colors.primaryAccent, modifier = Modifier.size(17.dp))
            }
        }

        Spacer(Modifier.height(10.dp))

        if (playlists.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                Text("No playlists yet", color = colors.mutedText, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp, max = 340.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(items = playlists, key = { it.id }) { playlist ->
                    val alreadyAdded = song.id in playlist.songIds
                    val selected = selectedPlaylistId == playlist.id

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected || alreadyAdded) colors.card else colors.card.copy(alpha = .45f))
                            .border(
                                if (selected) 1.4.dp else 0.8.dp,
                                if (selected) colors.primaryAccent else colors.cardBorder,
                                RoundedCornerShape(12.dp)
                            )
                            // A check means "already in": tapping it removes the song again.
                            .xvoxPressScale {
                                if (alreadyAdded) onRemoveFromPlaylist(playlist.id)
                                else selectedPlaylistId = if (selected) null else playlist.id
                            }
                            .padding(horizontal = 8.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playlist.name,
                                color = if (selected) colors.primaryAccent else colors.primaryText,
                                fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (alreadyAdded) "Added · tap to remove" else "${playlist.songIds.size} songs",
                                color = if (alreadyAdded) colors.primaryAccent else colors.secondaryText,
                                fontSize = 11.sp
                            )
                        }

                        val marker by animateColorAsState(
                            when {
                                alreadyAdded -> colors.primaryAccent
                                selected -> colors.primaryAccent
                                else -> colors.card
                            }, tween(180), label = "picker_marker"
                        )
                        Box(
                            modifier = Modifier.size(28.dp).clip(CircleShape).background(marker)
                                .border(1.dp, if (alreadyAdded || selected) colors.primaryAccent else colors.cardBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (alreadyAdded || selected) R.drawable.ic_xvox_check else R.drawable.ic_xvox_plus
                                ),
                                contentDescription = if (alreadyAdded) "Remove from ${playlist.name}" else "Add to ${playlist.name}",
                                tint = if (alreadyAdded || selected) colors.background else colors.primaryText,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(12.dp))
                    .background(colors.card).border(0.9.dp, colors.cardBorder, RoundedCornerShape(12.dp))
                    .xvoxPressScale(onClick = onCancel),
                contentAlignment = Alignment.Center
            ) {
                Text("Cancel", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            val canAdd = selectedPlaylistId != null
            // Even when nothing is picked yet the button keeps a light surface and a border.
            val fill by animateColorAsState(
                if (canAdd) colors.primaryAccent else colors.cardElevated, tween(180), label = "add_fill"
            )
            val borderColor by animateColorAsState(
                if (canAdd) colors.primaryAccent else colors.cardBorder, tween(180), label = "add_border"
            )
            val borderWidth by animateDpAsState(if (canAdd) 1.4.dp else 1.dp, tween(180), label = "add_border_w")
            Box(
                modifier = Modifier.weight(1.5f).height(44.dp).clip(RoundedCornerShape(12.dp))
                    .background(fill).border(borderWidth, borderColor, RoundedCornerShape(12.dp))
                    .xvoxPressScale(enabled = canAdd) { selectedPlaylistId?.let { onAddToPlaylist(it) } },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (canAdd) "Add to playlist" else "Select playlist",
                    color = if (canAdd) colors.background else colors.secondaryText,
                    fontSize = 13.sp, fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
