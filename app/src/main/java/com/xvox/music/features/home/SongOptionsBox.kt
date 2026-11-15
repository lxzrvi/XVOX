// EXACT CODE REPLACE KARO (poori file) - koi extra kaam nahi. Path: app/src/main/java/com/xvox/music/features/home/SongOptionsBox.kt
package com.xvox.music.features.home

import androidx.compose.foundation.background
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
import com.xvox.music.core.ui.overlay.xvoxBoxScroll

private data class SongOptionAction(
    val label: String,
    val icon: Int,
    val onClick: () -> Unit,
    val destructive: Boolean = false
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
        SongOptionAction("Play next", R.drawable.ic_xvox_play, onPlayNext),
        SongOptionAction("Add to queue", R.drawable.ic_xvox_queue, onAddQueue)
    )
    onSelect?.let { actions += SongOptionAction("Select", R.drawable.ic_xvox_check, it) }
    actions += SongOptionAction(
        if (liked) "Unlike" else "Like",
        if (liked) R.drawable.ic_xvox_heart else R.drawable.ic_xvox_heart_outline,
        onLiked
    )
    actions += SongOptionAction("Add to playlist", R.drawable.ic_xvox_playlist, onPlaylist)
    onEdit?.let { actions += SongOptionAction("Edit details", R.drawable.ic_xvox_edit, it) }
    actions += SongOptionAction("Info", R.drawable.ic_xvox_info, onInfo)
    actions += SongOptionAction("Set as ringtone", R.drawable.ic_xvox_music_note, onRingtone)
    actions += SongOptionAction("Share", R.drawable.ic_xvox_share, onShare)
    onAddToEach?.let { actions += SongOptionAction("Add to each", R.drawable.ic_xvox_add, it) }
    onRemovePlaylist?.let { remove ->
        actions += SongOptionAction("Remove from ${playlistName ?: "playlist"}", R.drawable.ic_xvox_close, remove)
    }
    onRemoveRecent?.let { actions += SongOptionAction("Remove from recent", R.drawable.ic_xvox_close, it) }
    onSectionSettings?.let { actions += SongOptionAction(sectionSettingsLabel ?: "Section settings", R.drawable.ic_xvox_settings, it) }
    actions += SongOptionAction("Delete", R.drawable.ic_xvox_delete, onDelete, destructive = true)

    Column(
        modifier = Modifier.width(300.dp).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // The lifted item: tap plays it.
        Column(
            modifier = Modifier
                .shadow(22.dp, coverShape, clip = false)
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

        // Actions hang off it in one frosted plate, rows like an iOS menu: label left, icon right.
        val scroll = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(18.dp, panelShape, clip = false)
                .clip(panelShape)
                .background(plate)
                .xvoxGlassRim(panelShape, hairlineColor = hairline)
                .heightIn(max = 330.dp)
                .verticalScroll(scroll)
                .xvoxBoxScroll(scroll)
        ) {
            actions.forEachIndexed { index, action ->
                val tint = if (action.destructive) XvoxRed else colors.primaryText
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 46.dp)
                        .xvoxPressScale(pressedScale = .985f, onClick = action.onClick)
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        action.label, color = tint, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                    )
                    Icon(painterResource(action.icon), null, tint = tint.copy(alpha = .9f), modifier = Modifier.size(19.dp))
                }
                if (index != actions.lastIndex) {
                    Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp).height(.5.dp).background(hairline))
                }
            }
        }
    }
}
