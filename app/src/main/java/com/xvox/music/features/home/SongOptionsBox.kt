package com.xvox.music.features.home

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.wrapContentHeight
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
fun SongOptionsBox(
    song: Song,
    liked: Boolean,
    playlistName: String? = null,
    /** Every playlist that already holds this song, so the menu can offer "Remove from …". */
    membership: List<Pair<String, () -> Unit>> = emptyList(),
    onAddToEach: (() -> Unit)? = null,
    onPlayNext: () -> Unit,
    onAddQueue: () -> Unit,
    onPlaylist: () -> Unit,
    onRemovePlaylist: (() -> Unit)? = null,
    onRemoveRecent: (() -> Unit)? = null,
    onLiked: () -> Unit,
    onDelete: () -> Unit,
    onInfo: () -> Unit,
    onRingtone: () -> Unit,
    onShare: () -> Unit,
    onSelect: (() -> Unit)? = null,
    sectionSettingsLabel: String? = null,
    onSectionSettings: (() -> Unit)? = null
) {
    val colors = XvoxTheme.colors

    // All available song menu options
    val options = buildList {
        add(SongOption("Play next", R.drawable.ic_xvox_play, onPlayNext))
        add(SongOption("Add to queue", R.drawable.ic_xvox_queue, onAddQueue))
        add(
            SongOption(
                if (liked) "Remove from Liked" else "Like song",
                if (liked) R.drawable.ic_xvox_heart else R.drawable.ic_xvox_heart_outline,
                onLiked
            )
        )

        if (playlistName != null && onRemovePlaylist != null) {
            add(SongOption("Remove from $playlistName", R.drawable.ic_xvox_delete, onRemovePlaylist))
        } else if (membership.isNotEmpty()) {
            membership.forEach { (name, remove) ->
                add(SongOption("Remove from $name", R.drawable.ic_xvox_delete, remove))
            }
            onAddToEach?.let { add(SongOption("Add to playlist", R.drawable.ic_xvox_playlist, it)) }
        } else {
            add(SongOption("Add to playlist", R.drawable.ic_xvox_playlist, onPlaylist))
        }

        if (onRemoveRecent != null) {
            add(SongOption("Remove from Recently Played", R.drawable.ic_xvox_close, onRemoveRecent))
        }

        if (onSelect != null) {
            add(SongOption("Select", R.drawable.ic_xvox_check, onSelect))
        }

        add(SongOption("Info", R.drawable.ic_xvox_info, onInfo))

        if (sectionSettingsLabel != null && onSectionSettings != null) {
            add(SongOption(sectionSettingsLabel, R.drawable.ic_xvox_settings, onSectionSettings))
        }

        add(SongOption("Set ringtone", R.drawable.ic_xvox_music_note, onRingtone))
        add(SongOption("Share", R.drawable.ic_xvox_share, onShare))
        add(SongOption("Delete from library", R.drawable.ic_xvox_delete, onDelete))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            XvoxSongArtwork(
                artwork = song.artworkUri,
                requestSize = 128,
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(10.dp))
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp)
            ) {
                Text(
                    text = song.title,
                    color = colors.primaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = song.artist,
                    color = colors.secondaryText,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .wrapContentHeight()
        ) {
            items(options) { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = option.action
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(38.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(option.icon),
                            contentDescription = null,
                            tint = colors.primaryText,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    Text(
                        text = option.title,
                        color = colors.primaryText,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
