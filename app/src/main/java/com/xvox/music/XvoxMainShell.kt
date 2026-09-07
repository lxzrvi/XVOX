package com.xvox.music

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.activity.compose.BackHandler
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.xvox.music.core.ui.navigation.LocalXvoxTopInset
import com.xvox.music.core.ui.navigation.LocalXvoxBottomInset
import com.xvox.music.core.ui.miniplayer.XvoxMiniPlayerPlacement
import com.xvox.music.shell.ExitMusicBox
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
import com.xvox.music.core.ui.miniplayer.XvoxPlayerTransitionMotion
import com.xvox.music.core.ui.navigation.XvoxBottomBar
import com.xvox.music.core.ui.navigation.XvoxDestination
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.features.home.HomeScreen
import com.xvox.music.features.home.HomeViewModel
import com.xvox.music.features.home.ProfileEditorBox
import com.xvox.music.features.home.SongInfoBox
import com.xvox.music.features.home.showCreatePlaylistOverlay
import com.xvox.music.features.home.showLibraryRefresh
import com.xvox.music.features.search.SearchScreen
import com.xvox.music.features.settings.SettingsScreen
import com.xvox.music.player.nowplaying.XvoxNowPlaying
import com.xvox.music.player.playback.MainPlayerViewModel
import com.xvox.music.shell.XvoxTimerBoxContent
import com.xvox.music.shell.XvoxPlaylistPickerBoxContent
import com.xvox.music.shell.XvoxQueueBoxContent
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
    val homePreferences = remember { com.xvox.music.data.preferences.UserPreferencesRepository(homeViewModel.getApplication<android.app.Application>()) }
    val mergedHome by homePreferences.homeMerge.collectAsState(initial = false)
    val overlays = LocalXvoxOverlayController.current
    val context = LocalContext.current
    LaunchedEffect(homeState.songs, homeState.loading) {
        if (!homeState.loading) playerViewModel.setQueue(homeState.songs)
    }

    var destination by remember { mutableStateOf(XvoxDestination.HOME) }
    var homeResetKey by remember { mutableLongStateOf(0L) }
    var hoistedSelectedPlaylistId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(mergedHome) {
        hoistedSelectedPlaylistId = null
        homeViewModel.setLibraryMode(com.xvox.music.features.playlist.XvoxHomeLibraryMode.ALL_SONGS)
    }

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

    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(destination) {
        focusManager.clearFocus()
        keyboard?.hide()
    }
    // More-specific Home / now-playing handlers are composed after this root handler.
    BackHandler(enabled = !player.nowPlayingVisible) {
        if (destination != XvoxDestination.HOME) {
            destination = XvoxDestination.HOME
        } else {
            overlays.showBox("Leaving so soon?") {
                ExitMusicBox(
                    onNo = overlays::hideBox,
                    onYes = {
                        playerViewModel.stopPlayback()
                        overlays.hideBox()
                        (context as? android.app.Activity)?.finishAffinity()
                    }
                )
            }
        }
    }

    fun showProfileEditor() {
        overlays.showBox("Edit profile") {
            ProfileEditorBox(
                profile = homeState.profile,
                onCancel = overlays::hideBox,
                onSave = { name, pfp, uri ->
                    homeViewModel.saveProfile(name, pfp, uri) {
                        overlays.hideBox()
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
        overlays.showBox("Add to playlist") {
            XvoxPlaylistPickerBoxContent(
                song = song,
                playlists = homeState.playlists,
                onAddToPlaylist = { playlistId ->
                    homeViewModel.addToPlaylist(playlistId, song) { updated ->
                        if (updated != null) {
                            overlays.hideBox()
                            overlays.showP("Added to ${updated.name}")
                        }
                    }
                },
                onCreatePlaylist = {
                    overlays.hideBox()
                    showCreatePlaylistOverlay(overlays, homeViewModel, homeState.songs, song)
                },
                onCancel = overlays::hideBox
            )
        }
    }

    fun showQueueBox() {
        overlays.showBox("Playing queue") {
            val livePlayer by playerViewModel.state.collectAsState()
            XvoxQueueBoxContent(
                queue = livePlayer.queue,
                currentSongId = livePlayer.currentSongId,
                onPlayIndex = { index ->
                    playerViewModel.playQueueIndex(index, keepPlayingState = false)
                    overlays.hideBox()
                },
                onMoveItem = { from, to ->
                    playerViewModel.moveQueueItem(from, to)
                }
            )
        }
    }

    fun showTimerBox() {
        overlays.showBox("Sleep timer") {
            XvoxTimerBoxContent(
                currentMinutes = player.sleepTimerMinutes,
                onSetMinutes = { minutes ->
                    playerViewModel.setSleepTimer(minutes)
                    overlays.hideBox()
                    overlays.showP("Timer set $minutes min")
                },
                onCustom = { minutes, seconds, pause, closeApp ->
                    playerViewModel.setCustomSleepTimer(minutes, seconds, pause, closeApp)
                    overlays.hideBox()
                    val total = minutes * 60 + seconds
                    if (total > 0) {
                        overlays.showP("Custom timer ${minutes}m ${seconds}s")
                    }
                },
                onCancel = {
                    playerViewModel.cancelSleepTimer()
                    overlays.hideBox()
                    overlays.showP("Timer off")
                }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        val density = LocalDensity.current
        val topInset = with(density) { WindowInsets.statusBars.getTop(this).toDp() } + 60.dp
        val bottomInset = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() } +
            if (player.miniPlayerVisible && destination != XvoxDestination.SETTINGS) 180.dp else 104.dp
        val tabState = rememberSaveableStateHolder()
        CompositionLocalProvider(LocalXvoxTopInset provides topInset, LocalXvoxBottomInset provides bottomInset) {
            AnimatedContent(
                targetState = destination,
                transitionSpec = {
                    val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                    ((slideInHorizontally(tween(300)) { it * direction } + fadeIn(tween(160)))
                        togetherWith (slideOutHorizontally(tween(300)) { -it * direction } + fadeOut(tween(160))))
                        .using(null)
                },
                label = "directionalTabs",
                modifier = Modifier.fillMaxSize()
            ) { targetDestination ->
                tabState.SaveableStateProvider(targetDestination.name) {
                    // Opaque isolated page surfaces prevent outgoing Settings becoming Home's backdrop.
                    Box(Modifier.fillMaxSize().background(colors.background)) {
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
        }
            XvoxShellTopHeader(
                profile = homeState.profile,
                destination = destination,
                libraryMode = homeState.libraryMode,
                mergedHome = mergedHome,
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
                .padding(bottom = XvoxMiniPlayerPlacement.navigationHostBottom)
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
                onTimer = ::showTimerBox,
                onQueue = ::showQueueBox,
                onStarPlaylist = { showAddCurrentSongToPlaylist(playingSong) },
                onInfo = {
                    homeViewModel.loadInfo(playingSong) { info ->
                        overlays.showBox("Song info") { SongInfoBox(info = info) }
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
