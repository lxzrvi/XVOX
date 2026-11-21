package com.xvox.music.core.design.kit

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.core.design.theme.ProvideXvoxNowPlayingChrome
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.effects.xvoxSongPress
import com.xvox.music.features.home.HomeViewModel
import com.xvox.music.features.home.XvoxSongActions
import com.xvox.music.features.home.XvoxSongArtwork
import com.xvox.music.player.nowplaying.XvoxNowPlaying
import com.xvox.music.player.playback.MainPlayerViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * The full Now Playing screen, opened by tapping the mini player. It is the same screen the app
 * has always had (its iOS layout follows the "UI style" setting), hosted here because the stage
 * replaced the shell that used to host it.
 */
@Composable
fun StageNowPlayingHost(
    playerVm: MainPlayerViewModel,
    homeVm: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val prepared by remember(playerVm) {
        playerVm.state.map { it.nowPlayingPrepared }.distinctUntilChanged()
    }.collectAsState(initial = false)
    if (!prepared) return
    StageNowPlayingContent(playerVm, homeVm, modifier)
}

@Composable
private fun StageNowPlayingContent(
    playerVm: MainPlayerViewModel,
    homeVm: HomeViewModel,
    modifier: Modifier
) {
    val context = LocalContext.current
    val player by playerVm.state.collectAsStateWithLifecycle()
    val home by homeVm.state.collectAsStateWithLifecycle()
    val song = player.queue.firstOrNull { it.id == player.currentSongId }
        ?: home.songs.firstOrNull { it.id == player.currentSongId }
        ?: return
    BackHandler(enabled = player.nowPlayingVisible) { playerVm.closeNowPlaying() }
    AnimatedVisibility(
        visible = player.nowPlayingPrepared,
        enter = EnterTransition.None,
        exit = ExitTransition.None,
        modifier = modifier.fillMaxSize()
    ) {
        ProvideXvoxNowPlayingChrome {
            XvoxNowPlaying(
                song = song,
                queue = player.queue,
                queueOccurrenceIds = player.queueOccurrenceIds,
                currentIndex = player.currentIndex,
                isPlaying = player.isPlaying,
                position = player.position,
                duration = player.duration,
                entranceEnabled = player.nowPlayingVisible,
                onClose = { playerVm.closeNowPlaying() },
                onDismissStart = { playerVm.beginNowPlayingDismissal() },
                onTogglePlay = { playerVm.togglePlay() },
                onPlayQueueIndex = { playerVm.playQueueIndex(it, keepPlayingState = true) },
                onSeek = { playerVm.seekTo(it) },
                isLiked = song.id in home.likedSongIds,
                onToggleLiked = { homeVm.toggleLiked(song) },
                onShare = { XvoxSongActions.share(context, song) },
                isShuffleEnabled = player.isShuffleEnabled,
                repeatMode = player.repeatMode,
                onToggleShuffle = { playerVm.toggleShuffle() },
                onToggleRepeat = { playerVm.toggleRepeat() },
                shuffleMethod = player.shuffleMethod,
                repeatTimesLeft = player.repeatTimesLeft,
                playerStyle = player.playerStyle,
                playingSource = player.playingSource
            )
        }
    }
}

/**
 * Search as a page of its own: it covers Home / Library while the search field is open and lists
 * every song whose title, artist or file name matches.
 */
@Composable
fun StageSearchPage(
    query: String,
    songs: List<Song>,
    currentSongId: Long?,
    topPadding: Dp,
    bottomPadding: Dp,
    onPlay: (Song, List<Song>) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    val needle = query.trim()
    val results = remember(needle, songs) {
        if (needle.isEmpty()) emptyList()
        else songs.filter {
            it.title.contains(needle, true) || it.artist.contains(needle, true) ||
                it.displayName.contains(needle, true) || it.folderName.contains(needle, true)
        }
    }
    Box(modifier.fillMaxSize().background(colors.background).pointerInput(Unit) { }) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding)
        ) {
            item(key = "search_head") {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text(
                        if (needle.isEmpty()) "Search" else "Songs",
                        color = colors.primaryText, fontSize = 26.sp, fontWeight = FontWeight.Bold
                    )
                    Text(
                        when {
                            needle.isEmpty() -> "Find a song by its name, artist or file."
                            results.isEmpty() -> "Nothing matches “$needle”."
                            else -> "${results.size} match${if (results.size == 1) "" else "es"}"
                        },
                        color = colors.secondaryText, fontSize = 13.sp
                    )
                }
            }
            items(results, key = { "search_${it.id}" }) { song ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp)
                        .xvoxSongPress(onClick = { onPlay(song, results) }, pressedScale = .97f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    XvoxSongArtwork(
                        artwork = song.artworkUri,
                        requestSize = 128,
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp))
                    )
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(
                            song.title,
                            color = if (song.id == currentSongId) colors.primaryAccent else colors.primaryText,
                            fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            song.artist.ifBlank { "Unknown artist" },
                            color = colors.secondaryText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
