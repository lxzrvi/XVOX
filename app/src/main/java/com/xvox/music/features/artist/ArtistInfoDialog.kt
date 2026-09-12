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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.overlay.XvoxBox
import com.xvox.music.features.home.XvoxSongArtwork
import com.xvox.music.features.settings.components.SettingsChoiceRow
import com.xvox.music.features.settings.components.XvoxThinLineSlider

@Composable
fun ArtistInfoDialog(
    artist: XvoxArtist,
    columns: Int = 5,
    gap: Int = 8,
    hideText: Boolean = false,
    mergedToHome: Boolean = false,
    onColumnsChange: (Int) -> Unit = {},
    onGapChange: (Int) -> Unit = {},
    onHideTextChange: (Boolean) -> Unit = {},
    onMergeToHomeChange: (Boolean) -> Unit = {},
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onEditPhoto: () -> Unit,
    onHideArtist: () -> Unit
) {
    val colors = XvoxTheme.colors
    var showDeleteConfirm by remember { mutableStateOf(false) }

    XvoxBox(
        title = "Artist Settings & Actions",
        onDismiss = onDismiss
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
                    text = "Hide ${artist.name}?",
                    color = colors.primaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "This will hide the artist and their songs from the library. You can restore them anytime in Settings › Deleted songs.",
                    color = colors.secondaryText,
                    fontSize = 12.sp,
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
                            .xvoxPressScale { showDeleteConfirm = false },
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
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Circle Photo with quick tap to edit
                Box(
                    modifier = Modifier
                        .size(80.dp)
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
                            modifier = Modifier.size(80.dp)
                        )
                    } else if (artist.coverSong != null) {
                        XvoxSongArtwork(
                            artwork = artist.coverSong.artworkUri,
                            requestSize = 180,
                            modifier = Modifier.size(80.dp)
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_xvox_microphone),
                            contentDescription = null,
                            tint = colors.primaryAccent,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = artist.name,
                    color = colors.primaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${artist.songs.size} ${if (artist.songs.size == 1) "Song" else "Songs"}",
                    color = colors.secondaryText,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 1.dp, bottom = 12.dp)
                )

                // Layout Controls
                Text(
                    text = "Section Layout & Grid",
                    color = colors.primaryAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                )

                SettingsChoiceRow(
                    options = listOf("3" to "3 Cols", "4" to "4 Cols", "5" to "5 Cols", "6" to "6 Cols"),
                    selected = columns.toString(),
                    onSelect = { onColumnsChange(it.toIntOrNull() ?: 5) }
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Gap Slider
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Spacing Gap", color = colors.secondaryText, fontSize = 11.sp)
                    Text("${gap}dp", color = colors.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                XvoxThinLineSlider(
                    value = gap.toFloat(),
                    onValueChange = { onGapChange(it.toInt()) },
                    valueRange = 2f..24f,
                    defaultValue = 8f,
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 10.dp)
                )

                // Hide text toggle
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Hide artist names", color = colors.primaryText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
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

                // Merge to Home toggle
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Merge section to Home", color = colors.primaryText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = mergedToHome,
                        onCheckedChange = onMergeToHomeChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.background,
                            checkedTrackColor = colors.primaryAccent,
                            uncheckedThumbColor = colors.secondaryText,
                            uncheckedTrackColor = colors.cardElevated
                        )
                    )
                }

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
                            .padding(horizontal = 14.dp, vertical = 10.dp),
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
                            .padding(horizontal = 14.dp, vertical = 10.dp),
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
                            text = "Crop & edit artist photo",
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
                                showDeleteConfirm = true
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
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
            }
        }
    }
}
