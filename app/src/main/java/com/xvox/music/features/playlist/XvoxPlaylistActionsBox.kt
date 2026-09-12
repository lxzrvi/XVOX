package com.xvox.music.features.playlist

import android.net.Uri
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.data.preferences.XvoxPlaylist
import kotlin.math.roundToInt

@Composable
fun XvoxPlaylistActionsBox(
    playlist: XvoxPlaylist,
    songs: List<Song>,
    onRename: (String) -> Unit,
    onSaveCover: (List<Long>, Uri?, () -> Unit) -> Unit,
    onDelete: () -> Unit,
    onInfo: () -> Unit
) {
    var editing by remember(playlist.id) { mutableStateOf(false) }
    var coverEditor by remember(playlist.id) { mutableStateOf(false) }
    var layoutEditor by remember(playlist.id) { mutableStateOf(false) }

    if (coverEditor) {
        XvoxPlaylistCoverEditor(
            playlist = playlist,
            songs = songs,
            onCancel = { coverEditor = false },
            onApply = { ids, uri ->
                onSaveCover(ids, uri) {
                    coverEditor = false
                }
            }
        )
        return
    }

    if (layoutEditor) {
        PlaylistLayoutEditor(onDone = { layoutEditor = false })
        return
    }

    PlaylistActionsMain(
        playlist = playlist,
        songs = songs,
        editing = editing,
        onEditingChange = { editing = it },
        onRename = onRename,
        onEditCover = { coverEditor = true },
        onDelete = onDelete,
        onInfo = onInfo,
        onEditLayout = { layoutEditor = true }
    )
}

@Composable
private fun PlaylistActionsMain(
    playlist: XvoxPlaylist,
    songs: List<Song>,
    editing: Boolean,
    onEditingChange: (Boolean) -> Unit,
    onRename: (String) -> Unit,
    onEditCover: () -> Unit,
    onDelete: () -> Unit,
    onInfo: () -> Unit,
    onEditLayout: () -> Unit
) {
    val colors = XvoxTheme.colors

    var name by remember(playlist.id, playlist.name) {
        mutableStateOf(
            TextFieldValue(
                text = playlist.name,
                selection = TextRange(playlist.name.length)
            )
        )
    }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(editing) {
        if (editing) {
            name = name.copy(selection = TextRange(name.text.length))
            focusRequester.requestFocus()
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier.size(62.dp),
                contentAlignment = Alignment.Center
            ) {
                XvoxPlaylistCover(
                    songs = songs,
                    coverSongIds = playlist.coverSongIds,
                    customCoverUri = playlist.customCoverUri,
                    requestSize = 128,
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(12.dp))
                )

                if (editing) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color.Black.copy(alpha = 0.64f), CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onEditCover
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_xvox_edit),
                            contentDescription = "Edit cover",
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.size(10.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (editing) {
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = colors.primaryText,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        cursorBrush = SolidColor(colors.primaryText),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                } else {
                    Text(
                        text = name.text,
                        color = colors.primaryText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.size(8.dp))

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(colors.card, CircleShape)
                    .clickable(
                        enabled = !editing || name.text.isNotBlank(),
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (editing) {
                            val clean = name.text.trim()
                            if (clean.isNotEmpty()) {
                                onRename(clean)
                                onEditingChange(false)
                            }
                        } else {
                            onEditingChange(true)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        if (editing) R.drawable.ic_xvox_check else R.drawable.ic_xvox_edit
                    ),
                    contentDescription = if (editing) "Save name" else "Rename",
                    tint = if (!editing || name.text.isNotBlank()) colors.primaryText else colors.mutedText,
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Card layout lives on the card: how the playlist reads on Home, and how tall it gets.
        PlaylistAction(
            title = "Card layout",
            onClick = onEditLayout
        )

        PlaylistAction(
            title = "Delete playlist",
            onClick = onDelete
        )

        PlaylistAction(
            title = "Playlist info",
            onClick = onInfo
        )
    }
}

@Composable
private fun PlaylistAction(
    title: String,
    onClick: () -> Unit
) {
    val colors = XvoxTheme.colors

    Text(
        text = title,
        color = colors.primaryText,
        fontSize = 14.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 13.dp)
    )
}

/**
 * Layout of this playlist's card on Home: swiping cards (horizontal) versus a long card that
 * merges into the vertical flow, plus a height for the long form.
 */
@Composable
private fun PlaylistLayoutEditor(
    onDone: () -> Unit,
    settingsViewModel: com.xvox.music.features.settings.SettingsViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val colors = XvoxTheme.colors
    val state by settingsViewModel.state.collectAsState()
    val auto = state.playlistLongHeight <= 0

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Playlist Layout & Settings",
            color = colors.primaryAccent,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "How playlists appear on your Home screen",
            color = colors.secondaryText,
            fontSize = 11.sp
        )

        Spacer(Modifier.height(12.dp))

        // Merge to Home Toggle
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text("Merge to Home", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("Show playlists in main home feed & remove from top pill", color = colors.secondaryText, fontSize = 10.sp)
            }
            androidx.compose.material3.Switch(
                checked = state.homeMerge,
                onCheckedChange = { settingsViewModel.setHomeMerge(it) },
                colors = androidx.compose.material3.SwitchDefaults.colors(
                    checkedThumbColor = colors.background,
                    checkedTrackColor = colors.primaryAccent,
                    uncheckedThumbColor = colors.secondaryText,
                    uncheckedTrackColor = colors.cardElevated
                )
            )
        }

        Spacer(Modifier.height(8.dp))

        Text("Card Style", color = colors.primaryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            LayoutChoice("Swipe cards", state.playlistStyle == "cards") {
                settingsViewModel.setPlaylistStyle("cards")
            }
            LayoutChoice("Long merge", state.playlistStyle == "long") {
                settingsViewModel.setPlaylistStyle("long")
            }
        }

        if (state.playlistStyle == "long") {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LayoutChoice("Horizontal", state.playlistCardOrientation == "horizontal") {
                    settingsViewModel.setPlaylistCardOrientation("horizontal")
                }
                LayoutChoice("Vertical", state.playlistCardOrientation == "vertical") {
                    settingsViewModel.setPlaylistCardOrientation("vertical")
                }
            }
        }

        if (state.playlistStyle == "long") {
            Spacer(Modifier.height(14.dp))
            Text(
                text = if (auto) "Height · Auto" else "Height · ${state.playlistLongHeight} dp",
                color = colors.secondaryText,
                fontSize = 11.sp
            )
            com.xvox.music.features.settings.components.XvoxThinLineSlider(
                value = state.playlistLongHeight.coerceAtLeast(60).toFloat(),
                onValueChange = { settingsViewModel.setPlaylistLongHeight(it.roundToInt()) },
                valueRange = 60f..220f,
                defaultValue = 120f
            )
            Spacer(Modifier.height(8.dp))
            LayoutChoice("Auto", auto) { settingsViewModel.setPlaylistLongHeight(0) }
        }

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(colors.primaryAccent)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDone
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("Done", color = colors.background, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LayoutChoice(label: String, active: Boolean, onClick: () -> Unit) {
    val colors = XvoxTheme.colors
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (active) colors.primaryAccent else colors.card)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (active) colors.background else colors.primaryText,
            fontSize = 12.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
        )
    }
}
