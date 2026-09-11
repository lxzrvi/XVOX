package com.xvox.music.features.playlist

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.platform.LocalContext
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.data.preferences.XvoxPlaylist
import com.xvox.music.features.home.XvoxSongArtwork

@Composable
fun XvoxPlaylistCoverEditor(
    playlist: XvoxPlaylist,
    songs: List<Song>,
    onCancel: () -> Unit,
    onApply: (List<Long>, Uri?) -> Unit
) {
    val colors = XvoxTheme.colors
    val selected = remember(playlist.id) {
        mutableStateListOf<Long>().apply {
            val available = songs.mapTo(HashSet()) { it.id }
            addAll(playlist.coverSongIds.filter { it in available }.ifEmpty {
                songs.take(if (songs.size >= 4) 4 else 1).map { it.id }
            })
        }
    }
    var customUri by remember(playlist.id) { mutableStateOf(playlist.customCoverUri?.let(Uri::parse)) }

    // Custom covers stack and persist, exactly like custom profile pictures.
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember(context) { UserPreferencesRepository(context.applicationContext) }
    val savedCovers by prefs.customCoverUris.collectAsState(initial = emptyList())
    var croppingUri by remember { mutableStateOf<Uri?>(null) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            croppingUri = uri
        }
    }

    if (croppingUri != null) {
        com.xvox.music.core.ui.components.XvoxImageCropDialog(
            sourceUri = croppingUri!!,
            isCircle = false,
            onCropped = { croppedUri ->
                croppingUri = null
                scope.launch {
                    val stored = prefs.addCustomCover(croppedUri.toString())
                    selected.clear()
                    customUri = Uri.parse(stored ?: croppedUri.toString())
                }
            },
            onDismiss = { croppingUri = null }
        )
    }

    // Any pick between 1 and 4 covers is valid; the mosaic renders whichever count is kept.
    val canApply = (selected.isNotEmpty() && selected.size <= 4) || customUri != null

    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 660.dp)) {
        Text(
            text = "Playlist cover",
            color = colors.primaryText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = when {
                customUri != null -> "Custom image"
                selected.size == 1 -> "Single cover"
                selected.isEmpty() -> "Pick 1–4 covers"
                else -> "${selected.size} covers • Mosaic"
            },
            color = colors.secondaryText,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .padding(top = 14.dp)
                .heightIn(max = 540.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            items(items = songs, key = { it.id }) { song ->
                val index = selected.indexOf(song.id)
                val active = index >= 0
                val shape = RoundedCornerShape(10.dp)

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(shape)
                        .background(colors.card)
                        .border(
                            width = if (active) 2.dp else 0.6.dp,
                            color = if (active) colors.primaryAccent else colors.cardBorder,
                            shape = shape
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            customUri = null
                            if (active) {
                                selected.remove(song.id)
                            } else {
                                if (selected.size >= 4) {
                                    selected.clear()
                                }
                                selected.add(song.id)
                            }
                        }
                ) {
                    XvoxSongArtwork(
                        artwork = song.artworkUri,
                        requestSize = 112,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                    )

                    if (active) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(28.dp)
                                .background(colors.primaryAccent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (index + 1).toString(),
                                color = colors.background,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Every kept cover stays available, each with its own delete badge.
            items(items = savedCovers, key = { "saved_$it" }) { stored ->
                val shape = RoundedCornerShape(10.dp)
                val active = customUri?.toString() == stored
                Box(Modifier.aspectRatio(1f), contentAlignment = Alignment.TopEnd) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(shape)
                            .background(colors.card)
                            .border(
                                width = if (active) 2.dp else 0.6.dp,
                                color = if (active) colors.primaryAccent else colors.cardBorder,
                                shape = shape
                            )
                            .xvoxPressScale { selected.clear(); customUri = Uri.parse(stored) }
                    ) {
                        AsyncImage(
                            model = Uri.parse(stored),
                            contentDescription = "Saved cover",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .offset(x = 4.dp, y = (-4).dp)
                            .size(20.dp)
                            .background(colors.background, CircleShape)
                            .border(0.8.dp, colors.cardBorder, CircleShape)
                            .xvoxPressScale(pressedScale = 0.85f) {
                                if (customUri?.toString() == stored) customUri = null
                                scope.launch { prefs.removeCustomCover(stored) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_xvox_close),
                            contentDescription = "Remove this cover",
                            tint = colors.primaryText,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }

            // The add button always stays at the end of the stack.
            item(key = "custom_cover") {
                val shape = RoundedCornerShape(10.dp)
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(shape)
                        .background(colors.card)
                        .border(0.6.dp, colors.cardBorder, shape)
                        .xvoxPressScale {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_xvox_plus),
                        contentDescription = "Add a cover",
                        tint = colors.primaryText,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CoverButton(
                title = "Cancel",
                enabled = true,
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            )

            CoverButton(
                title = "Okay",
                enabled = canApply,
                onClick = {
                    if (canApply) {
                        onApply(selected.toList(), customUri)
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CoverButton(
    title: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors

    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .background(colors.card, RoundedCornerShape(14.dp))
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (enabled) colors.primaryText else colors.mutedText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
