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

    // Clean, direct song menu options
    val options = buildList {
        add(SongOption("Play next", R.drawable.ic_xvox_play, onPlayNext))
        add(SongOption("Add to queue", R.drawable.ic_xvox_queue, onAddQueue))
        if (onSelect != null) {
            add(SongOption("Select", R.drawable.ic_xvox_check, onSelect))
        }
        add(
            SongOption(
                if (liked) "Remove from Liked" else "Like song",
                if (liked) R.drawable.ic_xvox_heart else R.drawable.ic_xvox_heart_outline,
                onLiked
            )
        )
        add(SongOption("Add / Remove from Playlist", R.drawable.ic_xvox_playlist, onPlaylist))
        add(SongOption("Info", R.drawable.ic_xvox_info, onInfo))
        add(SongOption("Set ringtone", R.drawable.ic_xvox_music_note, onRingtone))
        add(SongOption("Share", R.drawable.ic_xvox_share, onShare))
        if (onRemoveRecent != null) {
            add(SongOption("Remove from Recently Played", R.drawable.ic_xvox_close, onRemoveRecent))
        }
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
                    .clip(RoundedCornerShape(12.dp))
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = song.title,
                    color = colors.primaryText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = song.artist,
                    color = colors.secondaryText,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .wrapContentHeight()
        ) {
            items(options, key = { it.title }) { option ->
                val isDelete = option.icon == R.drawable.ic_xvox_delete
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = option.action
                        )
                        .padding(horizontal = 4.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(option.icon),
                        contentDescription = option.title,
                        tint = if (isDelete) androidx.compose.ui.graphics.Color(0xFFFF5252) else colors.primaryText,
                        modifier = Modifier.size(19.dp)
                    )
                    Text(
                        text = option.title,
                        color = if (isDelete) androidx.compose.ui.graphics.Color(0xFFFF5252) else colors.primaryText,
                        fontSize = 14.sp,
                        fontWeight = if (isDelete) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.padding(start = 14.dp)
                    )
                }
            }
        }
    }
}
