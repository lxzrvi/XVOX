package com.xvox.music.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.effects.xvoxPressScale

private data class SongOptionGridAction(
    val label: String,
    val icon: Int,
    val onClick: () -> Unit
)

/**
 * Compact song action panel shared by song cards and search results. The callbacks remain
 * deliberately contextual: callers only supply the actions that make sense for their entry point.
 */
@Composable
fun SongOptionsBox(
    song: Song,
    liked: Boolean,
    onAddQueue: () -> Unit,
    onPlaylist: () -> Unit,
    onLiked: () -> Unit,
    onDelete: () -> Unit,
    onInfo: () -> Unit,
    onRingtone: () -> Unit,
    onShare: () -> Unit,
    onPlayNext: () -> Unit,
    onSelect: (() -> Unit)? = null,
    onAddToEach: (() -> Unit)? = null,
    membership: List<String> = emptyList(),
    playlistName: String? = null,
    onRemovePlaylist: (() -> Unit)? = null,
    onRemoveRecent: (() -> Unit)? = null,
    sectionSettingsLabel: String? = null
) {
    val colors = XvoxTheme.colors
    val identityShape = RoundedCornerShape(18.dp)
    val tileFill = colors.primaryText.copy(alpha = if (colors.isLight) 0.045f else 0.055f)
    val destructiveColor = Color(0xFFFF5252)
    val tileBorder = colors.cardBorder.copy(alpha = 0.78f)
    val contextLabel = song.source.ifBlank {
        song.folderName.ifBlank { "XVOX library" }
    }

    // Keep menu order stable so the compact two-column layout stays easy to scan.
    val actions = mutableListOf(
        SongOptionGridAction("Play next", R.drawable.ic_xvox_play, onPlayNext),
        SongOptionGridAction("Add to queue", R.drawable.ic_xvox_queue, onAddQueue)
    )
    onSelect?.let { actions += SongOptionGridAction("Select", R.drawable.ic_xvox_check, it) }
    actions += SongOptionGridAction(
        if (liked) "Unlike" else "Like",
        if (liked) R.drawable.ic_xvox_heart else R.drawable.ic_xvox_heart_outline,
        onLiked
    )
    actions += SongOptionGridAction("Add to playlist", R.drawable.ic_xvox_playlist, onPlaylist)
    actions += SongOptionGridAction("Info", R.drawable.ic_xvox_info, onInfo)
    actions += SongOptionGridAction("Set as ringtone", R.drawable.ic_xvox_music_note, onRingtone)
    actions += SongOptionGridAction("Share", R.drawable.ic_xvox_share, onShare)
    onAddToEach?.let { actions += SongOptionGridAction("Add to each", R.drawable.ic_xvox_add, it) }
    onRemovePlaylist?.let { remove ->
        actions += SongOptionGridAction(
            "Remove from ${playlistName ?: "playlist"}",
            R.drawable.ic_xvox_close,
            remove
        )
    }
    onRemoveRecent?.let { actions += SongOptionGridAction("Remove from recent", R.drawable.ic_xvox_close, it) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 430.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(identityShape)
                .background(tileFill)
                .border(1.dp, tileBorder, identityShape)
                .padding(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(82.dp)
                    .clip(RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                XvoxSongArtwork(
                    artwork = song.artworkUri,
                    requestSize = 240,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = song.title,
                    color = colors.primaryText,
                    fontSize = 16.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist.ifBlank { "Unknown artist" },
                    color = colors.secondaryText,
                    fontSize = 12.5.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp)
                )
                Text(
                    text = contextLabel,
                    color = colors.mutedText,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        actions.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pair.forEach { action ->
                    SongOptionGridTile(
                        action = action,
                        background = tileFill,
                        border = tileBorder,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (pair.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }

        SongOptionDeleteTile(
            color = destructiveColor,
            background = destructiveColor.copy(alpha = if (colors.isLight) 0.10f else 0.16f),
            border = destructiveColor.copy(alpha = 0.42f),
            onClick = onDelete
        )
    }
}

@Composable
private fun SongOptionGridTile(
    action: SongOptionGridAction,
    background: Color,
    border: Color,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(12.dp)

    Row(
        modifier = modifier
            .heightIn(min = 50.dp)
            .clip(shape)
            .background(background)
            .border(1.dp, border, shape)
            .xvoxPressScale(onClick = action.onClick)
            .padding(horizontal = 11.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(action.icon),
            contentDescription = null,
            tint = colors.primaryText.copy(alpha = 0.86f),
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = action.label,
            color = colors.primaryText,
            fontSize = 12.5.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SongOptionDeleteTile(
    color: Color,
    background: Color,
    border: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .clip(shape)
            .background(background)
            .border(1.dp, border, shape)
            .xvoxPressScale(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_xvox_delete),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(17.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Delete from library",
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
