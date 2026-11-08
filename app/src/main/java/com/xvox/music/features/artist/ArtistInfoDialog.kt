// EXACT CODE REPLACE KARO (poori file) - koi extra kaam nahi. Path: app/src/main/java/com/xvox/music/features/artist/ArtistInfoDialog.kt
package com.xvox.music.features.artist

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.components.XvoxLikedHeartBadge
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.core.ui.overlay.xvoxBoxScroll
import com.xvox.music.features.home.XvoxSongArtwork


/** Header artwork used by the application-wide artist options sheet. */
@Composable
fun ArtistInfoHeaderArtwork(artist: XvoxArtist) {
    val colors = XvoxTheme.colors
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(colors.cardElevated),
        contentAlignment = Alignment.Center
    ) {
        XvoxSongArtwork(
            artwork = artist.customImageUri
                ?.takeIf { it.isNotBlank() }
                ?.let(Uri::parse)
                ?: artist.coverSong?.artworkUri,
            fallbackSeed = artist.coverSong?.id?.hashCode() ?: artist.name.hashCode(),
            requestSize = 100,
            modifier = Modifier.size(36.dp)
        )
    }
}

@Composable
fun ArtistInfoBox(
    artist: XvoxArtist,
    hideText: Boolean = false,
    liked: Boolean = false,
    onToggleLiked: () -> Unit = {},
    columns: Int = 4,
    onColumnsChange: (Int) -> Unit = {},
    onHideTextChange: (Boolean) -> Unit = {},
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onSelectArtist: (() -> Unit)? = null,
    onHideArtist: () -> Unit
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current
    val sheetScrollState = rememberScrollState()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(sheetScrollState)
                .xvoxBoxScroll(sheetScrollState)
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
                .verticalScroll(sheetScrollState)
                .xvoxBoxScroll(sheetScrollState)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "${artist.songs.size} songs by ${artist.name}",
                color = colors.secondaryText,
                fontSize = 11.5.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )

            // Group 1: quick actions in a 2 x 2 grid.
            ArtistGroupLabel("Actions")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ArtistActionTile(
                    iconRes = R.drawable.ic_xvox_play,
                    title = "Play next",
                    subtitle = "${artist.songs.size} songs",
                    onClick = { haptics.tap(); onPlayNext(); onDismiss() },
                    modifier = Modifier.weight(1f)
                )
                ArtistActionTile(
                    iconRes = R.drawable.ic_xvox_queue,
                    title = "Add to queue",
                    subtitle = "Append all",
                    onClick = { haptics.tap(); onDismiss(); onAddToQueue() },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                if (onSelectArtist != null) {
                    ArtistActionTile(
                        iconRes = R.drawable.ic_xvox_check,
                        title = "Select",
                        subtitle = "Batch actions",
                        onClick = { haptics.tap(); onDismiss(); onSelectArtist() },
                        modifier = Modifier.weight(1f)
                    )
                }
                ArtistActionTile(
                    iconRes = null,
                    title = if (liked) "Unlike" else "Like artist",
                    subtitle = if (liked) "Pinned on top" else "Pin on top",
                    onClick = { haptics.tap(); onToggleLiked() },
                    modifier = Modifier.weight(1f),
                    leading = { XvoxLikedHeartBadge(badgeSize = 22.dp) }
                )
            }

            // Group 2: grid layout. Column boxes share the full width equally.
            ArtistGroupLabel("Grid layout")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.card)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hide artist names", color = colors.primaryText, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                        Text("Show only circular artwork", color = colors.secondaryText, fontSize = 11.sp)
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
                Text("Columns", color = colors.secondaryText, fontSize = 11.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (2..7).forEach { option ->
                        val selected = columns.coerceIn(2, 7) == option
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) colors.primaryAccent else colors.cardElevated)
                                .xvoxPressScale {
                                    haptics.tap()
                                    onColumnsChange(option)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                option.toString(),
                                color = if (selected) colors.background else colors.primaryText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Group 3: destructive.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
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


@Composable
private fun ArtistGroupLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = XvoxTheme.colors.secondaryText,
        fontSize = 10.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = .8.sp,
        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
    )
}

@Composable
private fun ArtistActionTile(
    iconRes: Int?,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null
) {
    val colors = XvoxTheme.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.card)
            .xvoxPressScale(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (leading != null) {
            Box(Modifier.size(22.dp), contentAlignment = Alignment.CenterStart) { leading() }
        } else if (iconRes != null) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = colors.primaryAccent,
                modifier = Modifier.size(21.dp)
            )
        }
        Text(title, color = colors.primaryText, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(subtitle, color = colors.secondaryText, fontSize = 11.sp, maxLines = 1)
    }
}
