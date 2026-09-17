package com.xvox.music.features.artist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.core.ui.overlay.XvoxBox
import com.xvox.music.features.home.XvoxSongArtwork

@Composable
fun ArtistInfoDialog(
    artist: XvoxArtist,
    hideText: Boolean = false,
    onHideTextChange: (Boolean) -> Unit = {},
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onSelectArtist: (() -> Unit)? = null,
    onHideArtist: () -> Unit
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    XvoxBox(
        title = artist.name,
        onDismiss = onDismiss,
        headerLeadingContent = {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colors.cardElevated),
                contentAlignment = Alignment.Center
            ) {
                if (artist.customImageUri != null) {
                    AsyncImage(
                        model = artist.customImageUri,
                        contentDescription = artist.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(36.dp)
                    )
                } else if (artist.coverSong != null) {
                    XvoxSongArtwork(
                        artwork = artist.coverSong.artworkUri,
                        requestSize = 100,
                        modifier = Modifier.size(36.dp)
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_xvox_artist),
                        contentDescription = null,
                        tint = colors.primaryAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    ) {
        if (showDeleteConfirm) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Hide Artist?",
                    color = Color(0xFFFF5252),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Hide \"${artist.name}\" and all their ${artist.songs.size} songs from the artists library? (You can restore hidden artists from Settings)",
                    color = colors.secondaryText,
                    fontSize = 12.5.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.cardElevated)
                            .xvoxPressScale {
                                haptics.tap()
                                showDeleteConfirm = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Cancel", color = colors.secondaryText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFFF5252))
                            .xvoxPressScale {
                                haptics.success()
                                showDeleteConfirm = false
                                onHideArtist()
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Hide Artist", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${artist.songs.size} songs by ${artist.name}",
                    color = colors.secondaryText,
                    fontSize = 11.5.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )

                // 1. Play Next Action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.card)
                        .xvoxPressScale {
                            haptics.tap()
                            onPlayNext()
                            onDismiss()
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_xvox_play),
                        contentDescription = null,
                        tint = colors.primaryAccent,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Play next",
                            color = colors.primaryText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Play all ${artist.songs.size} songs next in queue",
                            color = colors.secondaryText,
                            fontSize = 11.sp
                        )
                    }
                }

                // 2. Add to Queue Action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.card)
                        .xvoxPressScale {
                            haptics.tap()
                            onDismiss()
                            onAddToQueue()
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_xvox_queue),
                        contentDescription = null,
                        tint = colors.primaryAccent,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Add to queue",
                            color = colors.primaryText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Append all ${artist.songs.size} songs to active or saved queue",
                            color = colors.secondaryText,
                            fontSize = 11.sp
                        )
                    }
                }

                // 3. Select Songs for Batch Actions
                if (onSelectArtist != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.card)
                            .xvoxPressScale {
                                haptics.tap()
                                onDismiss()
                                onSelectArtist()
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_xvox_check),
                            contentDescription = null,
                            tint = colors.primaryAccent,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Select songs",
                                color = colors.primaryText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Select all ${artist.songs.size} songs for batch actions",
                                color = colors.secondaryText,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // 4. Artist Layout Settings Card (Only Hide artist names toggle)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.card)
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hide artist names", color = colors.primaryText, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                        Text("Show only circular artwork in grid", color = colors.secondaryText, fontSize = 11.sp)
                    }
                    Switch(
                        checked = hideText,
                        onCheckedChange = onHideTextChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.background,
                            checkedTrackColor = colors.primaryAccent,
                            uncheckedThumbColor = colors.secondaryText,
                            uncheckedTrackColor = colors.cardElevated
                        )
                    )
                }

                // 5. Hide / Delete Artist Action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.card)
                        .xvoxPressScale {
                            haptics.tap()
                            showDeleteConfirm = true
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_xvox_delete),
                        contentDescription = null,
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Hide artist",
                        color = Color(0xFFFF5252),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
