package com.xvox.music.features.artist

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.components.XvoxImageCropDialog
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.core.ui.overlay.XvoxBox
import com.xvox.music.features.home.HomePresentation
import com.xvox.music.features.home.HomeSectionReorderControls
import com.xvox.music.features.home.HomeViewModel
import com.xvox.music.features.home.XvoxSongArtwork
import com.xvox.music.features.settings.components.SettingsChoiceRow
import com.xvox.music.features.settings.components.XvoxThinLineSlider

@Composable
fun ArtistInfoDialog(
    artist: XvoxArtist,
    allArtists: List<XvoxArtist> = emptyList(),
    config: HomePresentation = HomePresentation(),
    homeViewModel: HomeViewModel? = null,
    columns: Int = 5,
    gap: Int = 8,
    hideText: Boolean = false,
    mergedToHome: Boolean = false,
    onColumnsChange: (Int) -> Unit = {},
    onGapChange: (Int) -> Unit = {},
    onHideTextChange: (Boolean) -> Unit = {},
    onMergeToHomeChange: (Boolean) -> Unit = {},
    onRenameArtist: (oldName: String, newName: String, merge: Boolean) -> Unit = { _, _, _ -> },
    onSaveArtistPhoto: (artistName: String, uri: Uri?) -> Unit = { _, _ -> },
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onEditPhoto: () -> Unit = {},
    onHideArtist: () -> Unit
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current
    var editing by remember(artist.name) { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var duplicateTargetArtist by remember { mutableStateOf<String?>(null) }
    var croppingUri by remember { mutableStateOf<Uri?>(null) }

    var nameField by remember(artist.name) {
        mutableStateOf(
            TextFieldValue(
                text = artist.name,
                selection = TextRange(artist.name.length)
            )
        )
    }

    val focusRequester = remember { FocusRequester() }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            croppingUri = uri
        }
    }

    if (croppingUri != null) {
        XvoxImageCropDialog(
            sourceUri = croppingUri!!,
            isCircle = true,
            onCropped = { croppedUri ->
                croppingUri = null
                onSaveArtistPhoto(artist.name, croppedUri)
            },
            onDismiss = { croppingUri = null }
        )
    }

    LaunchedEffect(editing) {
        if (editing) {
            nameField = nameField.copy(selection = TextRange(nameField.text.length))
            runCatching { focusRequester.requestFocus() }
        }
    }

    fun submitRename(mergeDuplicates: Boolean) {
        val clean = nameField.text.trim()
        if (clean.isNotBlank() && clean != artist.name) {
            onRenameArtist(artist.name, clean, mergeDuplicates)
        }
        editing = false
        duplicateTargetArtist = null
    }

    XvoxBox(
        title = if (editing) "Edit Artist" else artist.name,
        onDismiss = onDismiss
    ) {
        if (duplicateTargetArtist != null) {
            // Mix / Separate prompt dialog
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Artist already exists",
                    color = colors.primaryAccent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "An artist named \"$duplicateTargetArtist\" already exists in your library. Do you want their songs to mix together as one artist?",
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
                            .xvoxPressScale {
                                haptics.tap()
                                submitRename(mergeDuplicates = false)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Separate", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.primaryAccent)
                            .xvoxPressScale {
                                haptics.success()
                                submitRename(mergeDuplicates = true)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Yes, Mix", color = colors.background, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else if (showDeleteConfirm) {
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
                    .padding(vertical = 4.dp)
            ) {
                // Header (like Playlist edit box)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (editing) {
                            BasicTextField(
                                value = nameField,
                                onValueChange = { nameField = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = colors.primaryText,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                cursorBrush = SolidColor(colors.primaryAccent),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                            )
                            Text(
                                text = "Note: This does not change the actual artist name from song metadata. It is only used inside XVOX to easily manage your artists.",
                                color = colors.mutedText,
                                fontSize = 10.sp,
                                lineHeight = 13.sp,
                                modifier = Modifier.padding(top = 4.dp, end = 6.dp)
                            )
                        } else {
                            Text(
                                text = artist.name,
                                color = colors.primaryText,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${artist.songs.size} ${if (artist.songs.size == 1) "Song" else "Songs"}",
                                color = colors.secondaryText,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(10.dp))

                    // Pencil edit / save button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.cardElevated)
                            .xvoxPressScale {
                                haptics.tap()
                                if (editing) {
                                    val clean = nameField.text.trim()
                                    if (clean.isNotEmpty() && clean != artist.name) {
                                        val duplicateExists = allArtists.any { it.name.equals(clean, ignoreCase = true) }
                                        if (duplicateExists) {
                                            duplicateTargetArtist = clean
                                        } else {
                                            submitRename(mergeDuplicates = false)
                                        }
                                    } else {
                                        editing = false
                                    }
                                } else {
                                    editing = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(
                                if (editing) R.drawable.ic_xvox_check else R.drawable.ic_xvox_edit
                            ),
                            contentDescription = if (editing) "Save" else "Edit",
                            tint = colors.primaryAccent,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    // Artist Image with Pencil overlay when editing
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(colors.card)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (artist.customImageUri != null) {
                            AsyncImage(
                                model = artist.customImageUri,
                                contentDescription = artist.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(58.dp)
                            )
                        } else if (artist.coverSong != null) {
                            XvoxSongArtwork(
                                artwork = artist.coverSong.artworkUri,
                                requestSize = 140,
                                modifier = Modifier.size(58.dp)
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.ic_xvox_microphone),
                                contentDescription = null,
                                tint = colors.primaryAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        if (editing) {
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .background(Color.Black.copy(alpha = 0.55f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_xvox_edit),
                                    contentDescription = "Edit photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                if (editing) {
                    Spacer(Modifier.height(10.dp))
                    Text("Available Artist Artwork", color = colors.secondaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))

                    val artworks = remember(artist.songs) {
                        artist.songs.mapNotNull { it.artworkUri }.distinct().take(10)
                    }

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(colors.cardElevated)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {
                                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_xvox_add),
                                    contentDescription = "Pick Custom Photo",
                                    tint = colors.primaryAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        items(artworks) { artUri ->
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {
                                            onSaveArtistPhoto(artist.name, artUri)
                                        }
                                    )
                            ) {
                                XvoxSongArtwork(
                                    artwork = artUri,
                                    requestSize = 100,
                                    modifier = Modifier.size(46.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Artist Layout & Settings Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.card)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Artist Layout & Settings",
                        color = colors.primaryAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    SettingsChoiceRow(
                        options = listOf("3" to "3 Cols", "4" to "4 Cols", "5" to "5 Cols", "6" to "6 Cols"),
                        selected = columns.toString(),
                        onSelect = { onColumnsChange(it.toIntOrNull() ?: 5) }
                    )

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
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Hide text toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Merge to Home", color = colors.primaryText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
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

                    if (mergedToHome && homeViewModel != null) {
                        HomeSectionReorderControls(
                            config = config,
                            viewModel = homeViewModel
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Actions
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.card)
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
                            .background(colors.card)
                            .xvoxPressScale {
                                showDeleteConfirm = true
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
            }
        }
    }
}
