// EXACT CODE REPLACE KARO (poori file) - koi extra kaam nahi. Path: app/src/main/java/com/xvox/music/features/home/SongOptionsBox.kt
package com.xvox.music.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import com.xvox.music.core.design.kit.icons.XvoxSfIcons
import com.xvox.music.core.ui.overlay.LocalXvoxHeldLifted
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import com.xvox.music.core.ui.blur.xvoxGlassRim
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
import com.xvox.music.core.ui.icons.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxRed
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.overlay.xvoxBoxScroll

private data class SongOptionAction(
    val label: String,
    val icon: Int,
    val onClick: () -> Unit,
    val destructive: Boolean = false,
    /** Rows of one group sit together; a hairline separates groups, as in BitChord's menu. */
    val group: Int = 0
)

/**
 * Held-item long-press menu (BitChord / Apple Music style): the song lifts off the page as a large cover
 * card and its actions hang beneath it in one frosted plate. Shown through [com.xvox.music.core.ui.overlay.XvoxBoxPresentation.HELD].
 * Callers still supply only the actions that make sense for their entry point.
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
    sectionSettingsLabel: String? = null,
    onEdit: (() -> Unit)? = null,
    onSectionSettings: (() -> Unit)? = null
) {
    val colors = XvoxTheme.colors
    val dark = !colors.isLight
    val plate = (if (dark) Color.Black else Color.White).copy(alpha = if (dark) .5f else .5f)
    val hairline = if (dark) Color.White.copy(alpha = .10f) else Color.Black.copy(alpha = .08f)
    val coverShape = RoundedCornerShape(22.dp)
    val panelShape = RoundedCornerShape(22.dp)
    val contextLabel = song.source.ifBlank { song.folderName.ifBlank { "XVOX library" } }

    val actions = mutableListOf(
        SongOptionAction("Play next", R.drawable.ic_xvox_play, onPlayNext, group = 1),
        SongOptionAction("Add to queue", R.drawable.ic_xvox_queue, onAddQueue, group = 1)
    )
    onSelect?.let { actions += SongOptionAction("Select", R.drawable.ic_xvox_check, it, group = 1) }
    actions += SongOptionAction("Add to playlist", R.drawable.ic_xvox_playlist, onPlaylist, group = 2)
    onAddToEach?.let { actions += SongOptionAction("Add to each", R.drawable.ic_xvox_add, it, group = 2) }
    onRemovePlaylist?.let { remove ->
        actions += SongOptionAction("Remove from ${playlistName ?: "playlist"}", R.drawable.ic_xvox_close, remove, group = 2)
    }
    onRemoveRecent?.let { actions += SongOptionAction("Remove from recent", R.drawable.ic_xvox_close, it, group = 2) }
    onEdit?.let { actions += SongOptionAction("Edit details", R.drawable.ic_xvox_edit, it, group = 3) }
    actions += SongOptionAction("Info", R.drawable.ic_xvox_info, onInfo, group = 3)
    actions += SongOptionAction("Set as ringtone", R.drawable.ic_xvox_music_note, onRingtone, group = 3)

    val lifted = LocalXvoxHeldLifted.current
    // BitChord's iOS menu colours: labels are the page's ink, pressed rows get a soft fill.
    val label = if (dark) Color.White.copy(alpha = .92f) else Color(0xFF1A1A1A)
    val separator = if (dark) Color.White.copy(alpha = .10f) else Color.Black.copy(alpha = .09f)
    val pressedFill = if (dark) Color.White.copy(alpha = .10f) else Color.Black.copy(alpha = .07f)
    val menuShape = RoundedCornerShape(30.dp)

    Column(
        modifier = if (lifted) Modifier.fillMaxWidth() else Modifier.width(300.dp).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!lifted) {
            // Opened from the three-dot button: there is no held card to lift, so show a cover card.
            Column(
                modifier = Modifier
                    .clip(coverShape)
                    .background(plate)
                    .xvoxGlassRim(coverShape, hairlineColor = hairline)
                    .xvoxPressScale(onClick = onPlay)
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                XvoxSongArtwork(
                    artwork = song.artworkUri,
                    fallbackSeed = song.id.hashCode(),
                    requestSize = 384,
                    modifier = Modifier.size(196.dp).clip(RoundedCornerShape(15.dp))
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    song.title, color = colors.primaryText, fontSize = 15.sp, lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center, modifier = Modifier.width(196.dp)
                )
                Text(
                    song.artist.ifBlank { "Unknown artist" }, color = colors.secondaryText, fontSize = 12.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.width(196.dp)
                )
                Text(
                    contextLabel, color = colors.mutedText, fontSize = 10.5.sp, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.width(196.dp)
                )
                Spacer(Modifier.height(2.dp))
            }
            Spacer(Modifier.height(12.dp))
        }

        val scroll = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(menuShape)
                .background(plate)
                .xvoxGlassRim(menuShape, hairlineColor = hairline)
                .heightIn(max = if (lifted) 520.dp else 330.dp)
                .verticalScroll(scroll)
                .xvoxBoxScroll(scroll)
                .padding(vertical = 8.dp)
        ) {
            // Like, Share and Delete as icon tiles along the top of the panel.
            Row(
                modifier = Modifier.fillMaxWidth().height(66.dp).padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MenuTile(
                    icon = painterResource(if (liked) R.drawable.ic_xvox_heart else R.drawable.ic_xvox_heart_outline),
                    label = if (liked) "Unlike" else "Like",
                    ink = if (liked) colors.primaryAccent else label,
                    pressedFill = pressedFill,
                    onClick = onLiked,
                    modifier = Modifier.weight(1f)
                )
                MenuTile(
                    icon = androidx.compose.ui.graphics.vector.rememberVectorPainter(XvoxSfIcons.Share),
                    label = "Share",
                    ink = label,
                    pressedFill = pressedFill,
                    onClick = onShare,
                    modifier = Modifier.weight(1f)
                )
                MenuTile(
                    icon = painterResource(R.drawable.ic_xvox_delete),
                    label = "Delete",
                    ink = XvoxRed,
                    pressedFill = pressedFill,
                    onClick = onDelete,
                    modifier = Modifier.weight(1f)
                )
            }
            Box(Modifier.fillMaxWidth().height(17.dp).padding(horizontal = 18.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(separator))
            }
            var previousGroup = -1
            actions.forEach { action ->
                if (action.group != previousGroup && previousGroup != -1) {
                    // Hairline inset from the panel's edges, with air either side (17 dp tall).
                    Box(Modifier.fillMaxWidth().height(17.dp).padding(horizontal = 18.dp), contentAlignment = Alignment.Center) {
                        Box(Modifier.fillMaxWidth().height(1.dp).background(separator))
                    }
                }
                previousGroup = action.group
                val ink = if (action.destructive) XvoxRed else label
                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (pressed) pressedFill else Color.Transparent)
                        .clickable(interactionSource = interaction, indication = null, onClick = action.onClick)
                        .heightIn(min = 44.dp)
                        .padding(start = 12.dp, end = 14.dp, top = 11.dp, bottom = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.width(28.dp), contentAlignment = Alignment.Center) {
                        Icon(painterResource(action.icon), null, tint = ink, modifier = Modifier.size(21.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        action.label, color = ink, fontSize = 17.sp, lineHeight = 21.sp, letterSpacing = (-0.3).sp,
                        maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuTile(
    icon: androidx.compose.ui.graphics.painter.Painter,
    label: String,
    ink: Color,
    pressedFill: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(if (pressed) pressedFill else Color.Transparent)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = ink, modifier = Modifier.size(23.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, color = ink, fontSize = 12.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
