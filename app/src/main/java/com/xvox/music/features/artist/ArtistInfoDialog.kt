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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.features.home.XvoxSongArtwork
import com.xvox.music.features.settings.components.SettingsChoiceRow

@Composable
fun ArtistInfoDialog(
    artist: XvoxArtist,
    columns: Int = 5,
    gap: Int = 8,
    onColumnsChange: (Int) -> Unit = {},
    onGapChange: (Int) -> Unit = {},
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onEditPhoto: () -> Unit,
    onHideArtist: () -> Unit
) {
    val colors = XvoxTheme.colors

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.card,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Circle Photo with quick tap to edit
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(colors.surface)
                        .xvoxPressScale { onEditPhoto() },
                    contentAlignment = Alignment.Center
                ) {
                    if (artist.customImageUri != null) {
                        AsyncImage(
                            model = artist.customImageUri,
                            contentDescription = artist.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(88.dp)
                        )
                    } else if (artist.coverSong != null) {
                        XvoxSongArtwork(
                            artwork = artist.coverSong.artworkUri,
                            requestSize = 180,
                            modifier = Modifier.size(88.dp)
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_xvox_microphone),
                            contentDescription = null,
                            tint = colors.primaryAccent,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = artist.name,
                    color = colors.primaryText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${artist.songs.size} ${if (artist.songs.size == 1) "Song" else "Songs"}",
                    color = colors.secondaryText,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                )

                // Layout Controls
                Text(
                    text = "Section Layout",
                    color = colors.primaryAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                )

                SettingsChoiceRow(
                    options = listOf("3" to "3 Cols", "4" to "4 Cols", "5" to "5 Cols", "6" to "6 Cols"),
                    selected = columns.toString(),
                    onSelect = { onColumnsChange(it.toIntOrNull() ?: 5) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                SettingsChoiceRow(
                    options = listOf("4" to "Compact", "8" to "Normal", "12" to "Spacious"),
                    selected = gap.toString(),
                    onSelect = { onGapChange(it.toIntOrNull() ?: 8) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
                )

                // Actions
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.surface.copy(alpha = 0.6f))
                            .xvoxPressScale {
                                onPlayNext()
                                onDismiss()
                            }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_xvox_playlist),
                            contentDescription = null,
                            tint = colors.primaryAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Play this artist next",
                            color = colors.primaryText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.surface.copy(alpha = 0.6f))
                            .xvoxPressScale {
                                onEditPhoto()
                            }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_xvox_edit),
                            contentDescription = null,
                            tint = colors.primaryAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Crop & edit custom artist photo",
                            color = colors.primaryText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.surface.copy(alpha = 0.6f))
                            .xvoxPressScale {
                                onHideArtist()
                                onDismiss()
                            }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_xvox_delete),
                            contentDescription = null,
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Delete / Hide artist",
                            color = Color(0xFFFF5252),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.secondaryText),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done")
                }
            }
        }
    }
}
