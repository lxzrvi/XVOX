package com.xvox.music

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.effects.XvoxAmbientBlurryBackdrop
import com.xvox.music.core.ui.miniplayer.XvoxPlayerTransitionMotion
import com.xvox.music.core.ui.navigation.XvoxBottomBar
import com.xvox.music.core.ui.navigation.XvoxDestination
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.features.home.HomeScreen
import com.xvox.music.features.home.HomeViewModel
import com.xvox.music.features.home.ProfileEditorBox
import com.xvox.music.features.home.SongInfoBox
import com.xvox.music.features.home.showLibraryRefresh
import com.xvox.music.features.search.SearchScreen
import com.xvox.music.features.settings.SettingsScreen
import com.xvox.music.player.nowplaying.XvoxNowPlaying
import com.xvox.music.player.playback.MainPlayerViewModel
import com.xvox.music.shell.TimerSheetContent
import com.xvox.music.shell.XvoxPlaylistPickerSheetContent
import com.xvox.music.shell.XvoxQueueSheetContent
import com.xvox.music.shell.XvoxShellMiniPlayerHost
import com.xvox.music.shell.XvoxShellTopHeader

@Composable
fun XvoxMainShell(
    homeViewModel: HomeViewModel = viewModel(),
    playerViewModel: MainPlayerViewModel = viewModel()
) {
    val colors = XvoxTheme.colors
    val homeState by homeViewModel.state.collectAsState()
    val player by playerViewModel.state.collectAsState()
    val overlays = LocalXvoxOverlayController.current
    val context = LocalContext.current

    var destination by remember { mutableStateOf(XvoxDestination.HOME) }
    var homeResetKey by remember { mutableLongStateOf(0L) }
    var hoistedSelectedPlaylistId by remember { mutableStateOf<String?>(null) }

    val currentSong = remember(player.queue, player.currentSongId) {
        player.queue.firstOrNull { it.id == player.currentSongId }
    }

    val isInPlaylist = remember(homeState.playlists, player.currentSongId) {
        val id = player.currentSongId
        if (id != null) {
            homeState.playlists.any { id in it.songIds }
        } else false
    }

    LaunchedEffect(player.sleepTimerShouldCloseApp) {
        if (player.sleepTimerShouldCloseApp) {
            playerViewModel.consumeCloseApp()
            (context as? android.app.Activity)?.finishAffinity()
        }
    }

    LaunchedEffect(destination) {
        if (destination == XvoxDestination.HOME) {
            hoistedSelectedPlaylistId = null
        }
    }

    fun showProfileEditor() {
        overlays.showL {
            ProfileEditorBox(
                profile = homeState.profile,
                onCancel = overlays::hideL,
                onSave = { name, pfp, uri ->
                    homeViewModel.saveProfile(name, pfp, uri) {
                        overlays.hideL()
                        overlays.showP("Profile updated")
                    }
                }
            )
        }
    }

    fun showRefreshOverlay() {
        showLibraryRefresh(overlays, homeViewModel)
    }

    fun showAddCurrentSongToPlaylist(song: Song) {
        overlays.showL {
            XvoxPlaylistPickerSheetContent(
                song = song,
                playlists = homeState.playlists,
                onAddToPlaylist = { playlistId ->
                    homeViewModel.addToPlaylist(playlistId, song) { updated ->
                        if (updated != null) {
                            overlays.hideL()
                            overlays.showP("Added to ${updated.name}")
                        }
                    }
                },
                onCancel = overlays::hideL
            )
        }
    }

    fun showQueueSheet() {
        overlays.showL {
            XvoxQueueSheetContent(
                queue = player.queue,
                currentSongId = player.currentSongId,
                onPlayIndex = { index ->
                    playerViewModel.playQueueIndex(index)
                    overlays.hideL()
                },
                onMoveItem = { from, to ->
                    playerViewModel.moveQueueItem(from, to)
                }
            )
        }
    }

    fun showTimerSheet() {
        overlays.showL {
            TimerSheetContent(
                currentMinutes = player.sleepTimerMinutes,
                onSetMinutes = { minutes ->
                    playerViewModel.setSleepTimer(minutes)
                    overlays.hideL()
                    overlays.showP("Timer set $minutes min")
                },
                onCustom = { minutes, seconds, pause, closeApp ->
                    playerViewModel.setCustomSleepTimer(minutes, seconds, pause, closeApp)
                    overlays.hideL()
                    val total = minutes * 60 + seconds
                    if (total > 0) {
                        overlays.showP("Custom timer ${minutes}m ${seconds}s")
                    }
                },
                onCancel = {
                    playerViewModel.cancelSleepTimer()
                    overlays.hideL()
                    overlays.showP("Timer off")
                }
            )
        }
    }

    val currentPlayingSong = remember(player.currentSongId, homeState.songs) {
        homeState.songs.firstOrNull { it.id == player.currentSongId }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        if (destination == XvoxDestination.HOME || destination == XvoxDestination.SEARCH) {
            XvoxAmbientBlurryBackdrop(
                song = currentPlayingSong,
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            XvoxShellTopHeader(
                profile = homeState.profile,
                destination = destination,
                libraryMode = homeState.libraryMode,
                onProfileClick = ::showProfileEditor,
                onRefreshClick = ::showRefreshOverlay,
                onLikedClick = {
                    hoistedSelectedPlaylistId = null
                    homeViewModel.toggleLikedMode()
                },
                onPlaylistClick = {
                    hoistedSelectedPlaylistId = null
                    homeViewModel.togglePlaylistMode()
                }
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = destination,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(220)) +
                            slideInHorizontally(animationSpec = tween(220)) {
                                if (targetState.ordinal > initialState.ordinal) 50 else -50
                            }).togetherWith(fadeOut(animationSpec = tween(160)))
                    },
                    label = "tab_switch_transition",
                    modifier = Modifier.fillMaxSize()
                ) { targetDestination ->
                    when (targetDestination) {
                        XvoxDestination.HOME -> {
                            HomeScreen(
                                currentSongId = player.currentSongId,
                                isPlaying = player.isPlaying,
                                homeResetKey = homeResetKey,
                                selectedPlaylistId = hoistedSelectedPlaylistId,
                                onSelectedPlaylistIdChange = { hoistedSelectedPlaylistId = it },
                                onQueueReady = playerViewModel::setQueue,
                                onPlay = playerViewModel::play,
                                playerViewModel = playerViewModel
                            )
                        }
                        XvoxDestination.SEARCH -> {
                            SearchScreen(
                                homeViewModel = homeViewModel,
                                playerViewModel = playerViewModel,
                                onPlaylistSelected = { playlistId ->
                                    hoistedSelectedPlaylistId = playlistId
                                    destination = XvoxDestination.HOME
                                }
                            )
                        }
                        XvoxDestination.SETTINGS -> {
                            SettingsScreen(homeViewModel = homeViewModel)
                        }
                    }
                }
            }
        }

        val currentSongId = player.currentSongId
        val miniVisibleBase = player.miniPlayerVisible && !player.nowPlayingVisible && currentSongId != null && player.queue.isNotEmpty()
        val miniVisible = miniVisibleBase && destination != XvoxDestination.SETTINGS

        XvoxShellMiniPlayerHost(
            visible = miniVisible,
            currentSongId = if (miniVisibleBase) currentSongId else null,
            queue = player.queue,
            currentIndex = player.currentIndex,
            isPlaying = player.isPlaying,
            position = player.position,
            duration = player.duration,
            riseKey = player.miniPlayerRiseKey,
            onTogglePlay = { playerViewModel.togglePlay() },
            onPlayQueueIndex = { playerViewModel.playQueueIndex(it) },
            onStopAndDismiss = { playerViewModel.stopPlayback() },
            onOpenPlayer = { playerViewModel.openNowPlaying() },
            onLike = {
                currentSong?.let { song ->
                    val wasLiked = song.id in homeState.likedSongIds
                    homeViewModel.toggleLiked(song)
                    overlays.showP(if (wasLiked) "Removed from liked" else "Added to liked")
                }
            },
            onAdd = {
                currentSong?.let(::showAddCurrentSongToPlaylist)
            }
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 10.dp)
        ) {
            XvoxBottomBar(
                selected = destination,
                onSelected = { next ->
                    if (destination == XvoxDestination.HOME && next == XvoxDestination.HOME) {
                        hoistedSelectedPlaylistId = null
                        homeResetKey = System.currentTimeMillis()
                    }
                    destination = next
                }
            )
        }

        AnimatedVisibility(
            visible = player.nowPlayingVisible && currentSong != null,
            enter = fadeIn(
                animationSpec = tween(
                    durationMillis = XvoxPlayerTransitionMotion.Duration,
                    easing = XvoxPlayerTransitionMotion.easing
                )
            ),
            exit = fadeOut(
                animationSpec = tween(
                    durationMillis = XvoxPlayerTransitionMotion.Duration,
                    easing = XvoxPlayerTransitionMotion.easing
                )
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            val playingSong = currentSong ?: return@AnimatedVisibility

            XvoxNowPlaying(
                song = playingSong,
                queue = player.queue,
                currentIndex = player.currentIndex,
                isPlaying = player.isPlaying,
                position = player.position,
                duration = player.duration,
                onClose = { playerViewModel.closeNowPlaying() },
                onTogglePlay = { playerViewModel.togglePlay() },
                onPrevious = { playerViewModel.playPrevious() },
                onNext = { playerViewModel.playNext() },
                onPlayQueueIndex = { playerViewModel.playQueueIndex(it) },
                onSeek = { playerViewModel.seekTo(it) },
                isLiked = playingSong.id in homeState.likedSongIds,
                isInPlaylist = isInPlaylist,
                onToggleLiked = {
                    val wasLiked = playingSong.id in homeState.likedSongIds
                    homeViewModel.toggleLiked(playingSong)
                    overlays.showP(if (wasLiked) "Removed from liked" else "Added to liked")
                },
                onTimer = ::showTimerSheet,
                onQueue = ::showQueueSheet,
                onStarPlaylist = { showAddCurrentSongToPlaylist(playingSong) },
                onInfo = {
                    homeViewModel.loadInfo(playingSong) { info ->
                        overlays.showL { SongInfoBox(info = info) }
                    }
                },
                isShuffleEnabled = player.isShuffleEnabled,
                repeatMode = player.repeatMode,
                onToggleShuffle = {
                    val wasEnabled = player.isShuffleEnabled
                    playerViewModel.toggleShuffle()
                    overlays.showP(if (wasEnabled) "Shuffle off" else "Shuffle on")
                },
                onToggleRepeat = { playerViewModel.toggleRepeat() },
                playerStyle = player.playerStyle,
                sleepTimerProgress = player.sleepTimerProgress,
                playingSource = player.playingSource,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
