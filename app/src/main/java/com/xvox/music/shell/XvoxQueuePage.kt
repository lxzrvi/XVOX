// NAYI FILE BANAO (poori file) - koi extra kaam nahi. Path: app/src/main/java/com/xvox/music/shell/XvoxQueuePage.kt
package com.xvox.music.shell

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.xvox.music.player.playback.RepeatMode
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.xvox.music.core.ui.blur.xvoxGlassRim
import com.xvox.music.features.home.XvoxSongArtwork
import com.xvox.music.player.playback.MainPlayerViewModel

/**
 * Queue as a page of Now Playing: opened by scrolling the player up (everything above slides off the
 * screen). Top: "Queue" label + queue options (switch queue). Under it: the playing song's card.
 * Then the songs - hold a row to reorder, swipe it left to remove.
 */
@Composable
fun XvoxQueuePage(
    playerViewModel: MainPlayerViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    val liveState by playerViewModel.state.collectAsState()
    var viewingQueueId by remember { mutableStateOf(liveState.activeQueueId) }
    val isActive = viewingQueueId == liveState.activeQueueId
    val viewingSaved = liveState.savedQueues.firstOrNull { it.id == viewingQueueId }
    val list = if (isActive) liveState.queue else (viewingSaved?.songs ?: emptyList())
    val name = when {
        isActive -> liveState.activeQueueName
        viewingSaved != null -> viewingSaved.name
        else -> "Queue 1"
    }
    BackHandler(onBack = onClose)
    val playingForBackdrop = liveState.queue.getOrNull(liveState.currentIndex)
    Box(modifier.fillMaxSize().background(colors.background)) {
    // Same blurred-cover world as Now Playing, so the sheet reads as its continuation to the bottom edge.
    XvoxSongArtwork(
        artwork = playingForBackdrop?.artworkUri,
        requestSize = 96,
        modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = 1.4f; scaleY = 1.4f }.blur(48.dp)
    )
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (colors.isLight) .18f else .55f)))
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp)
    ) {
        // Label + queue options.
        Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(R.drawable.ic_xvox_chevron_down), "Close queue", tint = colors.primaryText, modifier = Modifier.size(24.dp))
            }
            Text(
                "Queue", color = colors.primaryText, fontWeight = FontWeight.Bold, fontSize = 22.sp,
                modifier = Modifier.padding(start = 8.dp).weight(1f)
            )
            QueueHeaderDropdown(
                activeQueueName = name,
                savedQueues = liveState.savedQueues,
                currentQueueSize = list.size,
                onSwitchQueue = { viewingQueueId = it }
            )
        }
        // The song that is playing now.
        val playing = list.getOrNull(if (isActive) liveState.currentIndex else -1)
        if (playing != null) {
            val shape = RoundedCornerShape(18.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 10.dp)
                    .clip(shape)
                    .background(colors.primaryAccent.copy(alpha = .16f))
                    .xvoxGlassRim(shape)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                XvoxSongArtwork(
                    artwork = playing.artworkUri,
                    requestSize = 192,
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))
                )
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("Now playing", color = colors.primaryAccent, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                    Text(playing.title, color = colors.primaryText, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(playing.artist, color = colors.secondaryText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(
                    painterResource(if (liveState.isPlaying) R.drawable.ic_xvox_waveform else R.drawable.ic_xvox_play),
                    null, tint = colors.primaryAccent, modifier = Modifier.padding(end = 8.dp).size(22.dp)
                )
            }
        }
        Text(
            "Up next · ${list.size} songs", color = colors.mutedText, fontSize = 11.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        Box(Modifier.weight(1f).fillMaxWidth()) {
            XvoxQueueBoxContent(
                queue = list,
                currentSongId = liveState.currentSongId,
                currentIndex = if (isActive) liveState.currentIndex else -1,
                isPlaying = liveState.isPlaying,
                queueOccurrenceIds = if (isActive) liveState.queueOccurrenceIds else emptyList(),
                savedQueues = liveState.savedQueues,
                activeQueueName = name,
                isPlaybackActiveInThisQueue = isActive,
                onSwitchQueue = { viewingQueueId = it },
                onPlayIndex = { index ->
                    if (!isActive) playerViewModel.switchToQueue(viewingQueueId)
                    playerViewModel.playQueueIndex(index, keepPlayingState = true)
                },
                onMoveItem = { from, to ->
                    if (isActive) {
                        playerViewModel.moveQueueItem(from, to)
                    } else if (viewingSaved != null) {
                        val mutable = viewingSaved.songs.toMutableList()
                        if (from in mutable.indices && to in mutable.indices) {
                            val item = mutable.removeAt(from)
                            mutable.add(to, item)
                            playerViewModel.updateSavedQueue(viewingQueueId, mutable)
                        }
                    }
                },
                onReorderQueue = { reordered ->
                    if (isActive) playerViewModel.setQueueOrder(reordered)
                    else if (viewingSaved != null) playerViewModel.updateSavedQueue(viewingQueueId, reordered)
                },
                onReorderOccurrences = if (isActive) {
                    { ordered: List<String> -> playerViewModel.setQueueOrderByOccurrenceIds(ordered) }
                } else null,
                onRemoveIndex = { index ->
                    if (isActive) {
                        playerViewModel.removeFromQueueAt(index)
                    } else if (viewingSaved != null) {
                        val mutable = viewingSaved.songs.toMutableList()
                        if (index in mutable.indices) {
                            mutable.removeAt(index)
                            playerViewModel.updateSavedQueue(viewingQueueId, mutable)
                        }
                    }
                },
                fullHeight = true
            )
        }
        // iOS-style bottom pill: shuffle / repeat live right under the list.
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            val pillShape = RoundedCornerShape(26.dp)
            Row(
                Modifier.clip(pillShape).background(Color.White.copy(alpha = .14f)).xvoxGlassRim(pillShape).padding(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val repeatOn = liveState.repeatMode != RepeatMode.OFF
                QueuePillButton(R.drawable.ic_xvox_shuffle, liveState.isShuffleEnabled) { playerViewModel.toggleShuffle() }
                Spacer(Modifier.width(4.dp))
                QueuePillButton(
                    if (liveState.repeatMode == RepeatMode.ONE) R.drawable.ic_xvox_repeat_one else R.drawable.ic_xvox_repeat,
                    repeatOn
                ) { playerViewModel.toggleRepeat() }
            }
        }
    }
    }
}

@Composable
private fun QueuePillButton(icon: Int, active: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        Modifier.size(width = 72.dp, height = 44.dp).clip(shape)
            .background(if (active) Color.White.copy(alpha = .9f) else Color.Transparent)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(painterResource(icon), null, tint = if (active) Color.Black.copy(alpha = .8f) else Color.White.copy(alpha = .9f), modifier = Modifier.size(22.dp))
    }
}
