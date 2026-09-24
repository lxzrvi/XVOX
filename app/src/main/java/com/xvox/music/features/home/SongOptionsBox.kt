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
import com.xvox.music.core.design.theme.XvoxRed
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.effects.xvoxPressScale

private data class SongOptionGridAction(
    val label: String,
    val icon: Int,
    val onClick: () -> Unit,
    val destructive: Boolean = false
)

/**
 * Compact song action panel shared by song cards and search results. The callbacks remain
 * deliberately contextual: callers only supply the actions that make sense for their entry point.
 */
@Composable
fun SongOptionsBox(
    song: Song,
    liked: Boolean,
    onPlay: () -> Unit,
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
    val identityShape = RoundedCornerShape(16.dp)
    // Action cards deliberately share the song identity card's rounded silhouette.
    val tileShape = identityShape
    // Settings-page card fill/border: this overlay is intentionally the same palette in light,
    // dark, and AMOLED themes rather than a separate translucent dark surface.
    val tileFill = colors.card
    val tileBorder = colors.cardBorder.copy(alpha = if (colors.isLight) .8f else .72f)
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

    val deleteAction = SongOptionGridAction(
        label = "Delete",
        icon = R.drawable.ic_xvox_delete,
        onClick = onDelete,
        destructive = true
    )
    // Context-specific menus sometimes leave one item by itself. Put Delete beside that item
    // instead of adding another row, while retaining the full-width delete row for even grids.
    val deletePairsWithLastAction = actions.size % 2 != 0
    val gridActions = if (deletePairsWithLastAction) actions + deleteAction else actions

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 360.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(identityShape)
                .background(tileFill)
                .border(0.7.dp, tileBorder, identityShape)
                .xvoxPressScale(onClick = onPlay)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                XvoxSongArtwork(
                    artwork = song.artworkUri,
                    requestSize = 192,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = song.title,
                    color = colors.primaryText,
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist.ifBlank { "Unknown artist" },
                    color = colors.secondaryText,
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = contextLabel,
                    color = colors.mutedText,
                    fontSize = 10.5.sp,
                    lineHeight = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
        }

        gridActions.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                pair.forEach { action ->
                    SongOptionGridTile(
                        action = action,
                        tileShape = tileShape,
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

        if (!deletePairsWithLastAction) {
            SongOptionDeleteTile(
                background = tileFill,
                border = XvoxRed.copy(alpha = 0.42f),
                tileShape = tileShape,
                onClick = onDelete
            )
        }
    }
}

@Composable
private fun SongOptionGridTile(
    action: SongOptionGridAction,
    tileShape: RoundedCornerShape,
    background: Color,
    border: Color,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    // Keep destructive emphasis in the edge/text, but retain the exact Settings-card fill.
    val fill = background
    val edge = if (action.destructive) XvoxRed.copy(alpha = 0.42f) else border
    val tint = if (action.destructive) XvoxRed else colors.primaryText.copy(alpha = 0.86f)

    Row(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(tileShape)
            .background(fill)
            .border(0.7.dp, edge, tileShape)
            .xvoxPressScale(onClick = action.onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(action.icon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = action.label,
            color = tint,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SongOptionDeleteTile(
    background: Color,
    border: Color,
    tileShape: RoundedCornerShape,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clip(tileShape)
            .background(background)
            .border(0.7.dp, border, tileShape)
            .xvoxPressScale(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_xvox_delete),
            contentDescription = null,
            tint = XvoxRed,
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = "Delete",
            color = XvoxRed,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
