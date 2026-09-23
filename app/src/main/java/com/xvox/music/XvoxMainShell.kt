package com.xvox.music

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.miniplayer.XvoxMiniPlayer
import com.xvox.music.core.ui.miniplayer.XvoxMiniPlayerPlacement
import com.xvox.music.core.ui.navigation.LocalXvoxBottomInset
import com.xvox.music.core.ui.navigation.LocalXvoxTopInset
import com.xvox.music.core.ui.navigation.XvoxBottomBar
import com.xvox.music.core.ui.navigation.XvoxDestination
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.features.home.HomeScreen
import com.xvox.music.features.home.HomeViewModel
import com.xvox.music.features.home.ProfileEditorBox
import com.xvox.music.features.home.SongInfoBox
import com.xvox.music.features.home.XvoxSongActions
import com.xvox.music.features.home.showCreatePlaylistOverlay
import com.xvox.music.features.home.showLibraryRefresh
import com.xvox.music.features.home.showSongOptionsOverlay
import com.xvox.music.features.search.SearchScreen
import com.xvox.music.features.settings.SettingsScreen
import com.xvox.music.player.nowplaying.XvoxNowPlaying
import com.xvox.music.player.playback.MainPlayerViewModel
import com.xvox.music.shell.ExitMusicBox
import com.xvox.music.shell.XvoxPlaylistPickerBoxContent
import com.xvox.music.shell.XvoxQueueBoxContent
import com.xvox.music.shell.XvoxShellMiniPlayerHost
import com.xvox.music.shell.XvoxShellTopHeader
import com.xvox.music.shell.XvoxTimerBoxContent
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
fun XvoxMainShell(
    homeViewModel: HomeViewModel = viewModel(),
    playerViewModel: MainPlayerViewModel = viewModel(),
    settingsViewModel: com.xvox.music.features.settings.SettingsViewModel = viewModel(),
    backgroundBrightness: Float = 0.8f
) {
    val colors = XvoxTheme.colors
    val homeState by homeViewModel.state.collectAsState()
    val player by playerViewModel.state.collectAsState()
    val homePreferences = remember { com.xvox.music.data.preferences.UserPreferencesRepository(homeViewModel.getApplication<android.app.Application>()) }
    val homeConfig by homePreferences.homePresentation.collectAsState(initial = com.xvox.music.features.home.HomePresentation())
    val backgroundImage by homePreferences.themeBackgroundImage.collectAsState(initial = "")
    val overlays = LocalXvoxOverlayController.current
    val context = LocalContext.current
    val isLandscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(overlays) {
        var popup: Long? = null
        com.xvox.music.player.playback.XvoxBlendMonitor.state.map { it.enabled && it.active }.distinctUntilChanged().collect { active ->
            if (active) popup = overlays.showPersistentP("Crossfading")
            else { popup?.let(overlays::dismissP); popup = null }
        }
    }
    LaunchedEffect(homeState.songs, homeState.loading) {
        if (!homeState.loading) playerViewModel.setQueue(homeState.songs)
    }

    var destination by rememberSaveable { mutableStateOf(XvoxDestination.HOME) }
    var nowPlayingDisplayMode by rememberSaveable { mutableIntStateOf(0) }
    var homeResetKey by rememberSaveable { mutableLongStateOf(0L) }
    var tabEpoch by rememberSaveable { mutableLongStateOf(0L) }
    var hoistedSelectedPlaylistId by rememberSaveable { mutableStateOf<String?>(null) }
    var headerVisible by rememberSaveable { mutableStateOf(true) }
    var headerOffsetPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val headerMaxScrollPx = with(density) { 140.dp.toPx() }

    LaunchedEffect(destination) {
        headerOffsetPx = 0f
    }
    var pendingDeleteSong by remember { mutableStateOf<Song?>(null) }
    val miniDeleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && pendingDeleteSong != null) {
            playerViewModel.removeFromQueue(pendingDeleteSong!!.id)
            homeViewModel.refresh()
            overlays.showP("Song deleted from device")
        }
        pendingDeleteSong = null
    }

    BackHandler(enabled = destination != XvoxDestination.HOME) {
        destination = XvoxDestination.HOME
    }

    fun showProfileEditor() {
        overlays.showBox("Profile") {
            ProfileEditorBox(
                profile = homeState.profile,
                onCancel = overlays::hideBox,
                onSave = { name, pfp, pfpType ->
                    homeViewModel.saveProfile(name, pfp, pfpType) {
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

    fun showMiniPlayerSettings() {
        overlays.showBox("Mini player style") {
            com.xvox.music.features.settings.components.MiniPlayerSettingsBoxContent(
                viewModel = settingsViewModel
            )
        }
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
                onRemoveFromPlaylist = { playlistId ->
                    homeViewModel.removeFromPlaylist(playlistId, song) { updated ->
                        if (updated != null) overlays.showP("Removed from ${updated.name}")
                    }
                },
                onCancel = overlays::hideBox
            )
        }
    }

    fun showQueueBox() {
        val initialActiveId = playerViewModel.state.value.activeQueueId
        var viewingQueueId by mutableStateOf(initialActiveId)

        overlays.showBox(
            title = playerViewModel.state.value.activeQueueName,
            headerTitleContent = {
                val liveState by playerViewModel.state.collectAsState()
                val currentViewingQueue = liveState.savedQueues.firstOrNull { it.id == viewingQueueId }
                val viewingName = when {
                    viewingQueueId == liveState.activeQueueId -> liveState.activeQueueName
                    currentViewingQueue != null -> currentViewingQueue.name
                    else -> "Queue 1"
                }

                com.xvox.music.shell.QueueHeaderDropdown(
                    activeQueueName = viewingName,
                    savedQueues = liveState.savedQueues,
                    currentQueueSize = if (viewingQueueId == liveState.activeQueueId) liveState.queue.size else (currentViewingQueue?.songs?.size ?: 0),
                    onSwitchQueue = { queueId ->
                        viewingQueueId = queueId
                    }
                )
            }
        ) {
            val liveState by playerViewModel.state.collectAsState()
            val isViewingActiveQueue = viewingQueueId == liveState.activeQueueId
            val viewingSaved = liveState.savedQueues.firstOrNull { it.id == viewingQueueId }
            val currentList = if (isViewingActiveQueue) liveState.queue else (viewingSaved?.songs ?: emptyList())
            val viewingName = when {
                isViewingActiveQueue -> liveState.activeQueueName
                viewingSaved != null -> viewingSaved.name
                else -> "Queue 1"
            }

            XvoxQueueBoxContent(
                queue = currentList,
                currentSongId = liveState.currentSongId,
                isPlaying = liveState.isPlaying,
                savedQueues = liveState.savedQueues,
                activeQueueName = viewingName,
                isPlaybackActiveInThisQueue = isViewingActiveQueue,
                onSwitchQueue = { queueId ->
                    viewingQueueId = queueId
                },
                onPlayIndex = { index ->
                    if (!isViewingActiveQueue) {
                        playerViewModel.switchToQueue(viewingQueueId)
                    }
                    playerViewModel.playQueueIndex(index, keepPlayingState = false)
                    overlays.hideBox()
                },
                onMoveItem = { from, to ->
                    if (isViewingActiveQueue) {
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
                    if (isViewingActiveQueue) {
                        playerViewModel.setQueueOrder(reordered)
                    } else if (viewingSaved != null) {
                        playerViewModel.updateSavedQueue(viewingQueueId, reordered)
                    }
                },
                onRemoveIndex = { index ->
                    if (isViewingActiveQueue) {
                        playerViewModel.removeFromQueueAt(index)
                    } else if (viewingSaved != null) {
                        val mutable = viewingSaved.songs.toMutableList()
                        if (index in mutable.indices) {
                            mutable.removeAt(index)
                            playerViewModel.updateSavedQueue(viewingQueueId, mutable)
                        }
                    }
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

    val currentSong = remember(player.queue, player.currentSongId, homeState.songs) {
        player.queue.firstOrNull { it.id == player.currentSongId }
            ?: homeState.songs.firstOrNull { it.id == player.currentSongId }
    }

    val isInPlaylist = remember(homeState.playlists, player.currentSongId) {
        val currentId = player.currentSongId ?: return@remember false
        homeState.playlists.any { playlist ->
            playlist.songIds.contains(currentId)
        }
    }

    fun showMiniPlayerSongOptions(song: Song) {
        showSongOptionsOverlay(
            overlays = overlays,
            context = context,
            song = song,
            isLiked = song.id in homeState.likedSongIds,
            viewModel = homeViewModel,
            playerViewModel = playerViewModel,
            playlists = homeState.playlists,
            songs = homeState.songs,
            deleteLauncher = miniDeleteLauncher,
            onPendingDelete = { pendingDeleteSong = it },
            onSectionSettings = ::showMiniPlayerSettings
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        val density = LocalDensity.current
        val topInset = with(density) { WindowInsets.statusBars.getTop(this).toDp() } + 60.dp
        val bottomInset = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() } +
            if (isLandscape) 84.dp
            else if (player.miniPlayerVisible && destination != XvoxDestination.SETTINGS) 180.dp
            else 104.dp

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
                    TabSurface(backgroundImage = backgroundImage, backgroundBrightness = backgroundBrightness) {
                        when (targetDestination) {
                            XvoxDestination.HOME -> {
                                HomeScreen(
                                    currentSongId = player.currentSongId,
                                    isPlaying = player.isPlaying,
                                    homeResetKey = homeResetKey,
                                    scrollResetKey = tabEpoch,
                                    selectedPlaylistId = hoistedSelectedPlaylistId,
                                    onSelectedPlaylistIdChange = { hoistedSelectedPlaylistId = it },
                                    onQueueReady = playerViewModel::setQueue,
                                    onPlay = playerViewModel::play,
                                    playerViewModel = playerViewModel,
                                    onScrollProgress = { index, offset ->
                                        headerOffsetPx = if (index == 0) (-offset.toFloat()).coerceIn(-headerMaxScrollPx, 0f) else -headerMaxScrollPx
                                    }
                                )
                            }
                            XvoxDestination.SEARCH -> {
                                SearchScreen(
                                    homeViewModel = homeViewModel,
                                    playerViewModel = playerViewModel,
                                    topResetKey = tabEpoch,
                                    onPlaylistSelected = { playlistId ->
                                        hoistedSelectedPlaylistId = playlistId
                                        destination = XvoxDestination.HOME
                                    },
                                    onScrollProgress = { index, offset ->
                                        headerOffsetPx = if (index == 0) (-offset.toFloat()).coerceIn(-headerMaxScrollPx, 0f) else -headerMaxScrollPx
                                    }
                                )
                            }
                            XvoxDestination.SETTINGS -> {
                                SettingsScreen(
                                    homeViewModel = homeViewModel,
                                    settingsViewModel = settingsViewModel,
                                    topResetKey = tabEpoch
                                )
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = destination != XvoxDestination.SETTINGS && (headerVisible || destination == XvoxDestination.SEARCH),
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(260, easing = FastOutSlowInEasing)
            ) + fadeIn(tween(200)),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(220, easing = FastOutSlowInEasing)
            ) + fadeOut(tween(160)),
            modifier = Modifier.graphicsLayer { translationY = headerOffsetPx }
        ) {
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
                },
                onArtistClick = {
                    hoistedSelectedPlaylistId = null
                    homeViewModel.toggleArtistMode()
                }
            )
        }

        val currentSongId = player.currentSongId
        val miniVisibleBase = player.miniPlayerVisible && !player.nowPlayingVisible && currentSongId != null && player.queue.isNotEmpty()
        val miniVisible = if (isLandscape) (player.miniPlayerVisible && currentSongId != null && player.queue.isNotEmpty()) else (miniVisibleBase && destination != XvoxDestination.SETTINGS)

        if (isLandscape) {
            // Landscape Mode: Miniplayer and Navbar in one single balanced bottom row
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (miniVisible && currentSongId != null) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        XvoxMiniPlayer(
                            queue = player.queue,
                            currentSongId = currentSongId,
                            currentIndex = player.currentIndex,
                            isPlaying = player.isPlaying,
                            position = player.position,
                            duration = player.duration,
                            riseKey = player.miniPlayerRiseKey,
                            togglePlay = { playerViewModel.togglePlay() },
                            playQueueIndex = { playerViewModel.playQueueIndex(it) },
                            stopAndDismiss = { playerViewModel.stopPlayback() },
                            openPlayer = { playerViewModel.openNowPlaying() },
                            isLiked = currentSong?.id in homeState.likedSongIds,
                            onLike = {
                                currentSong?.let { song ->
                                    val wasLiked = song.id in homeState.likedSongIds
                                    homeViewModel.toggleLiked(song)
                                    overlays.showP(if (wasLiked) "Removed from liked" else "Added to liked")
                                }
                            },
                            onSongOptions = { currentSong?.let(::showMiniPlayerSongOptions) },
                            modifier = Modifier.fillMaxWidth().height(60.dp)
                        )
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }

                Box(
                    modifier = Modifier.height(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    XvoxBottomBar(
                        selected = destination,
                        onSelected = { next ->
                            if (next == XvoxDestination.HOME) {
                                hoistedSelectedPlaylistId = null
                                homeResetKey = System.currentTimeMillis()
                                homeViewModel.setLibraryMode(com.xvox.music.features.playlist.XvoxHomeLibraryMode.ALL_SONGS)
                            }
                            if (next != destination) {
                                tabEpoch++
                            }
                            destination = next
                        }
                    )
                }
            }
        } else {
            // Portrait Mode: Stacked layout
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
                isLiked = currentSong?.id in homeState.likedSongIds,
                onLike = {
                    currentSong?.let { song ->
                        val wasLiked = song.id in homeState.likedSongIds
                        homeViewModel.toggleLiked(song)
                        overlays.showP(if (wasLiked) "Removed from liked" else "Added to liked")
                    }
                },
                onSongOptions = { currentSong?.let(::showMiniPlayerSongOptions) }
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
                        if (next == XvoxDestination.HOME) {
                            hoistedSelectedPlaylistId = null
                            homeResetKey = System.currentTimeMillis()
                            homeViewModel.setLibraryMode(com.xvox.music.features.playlist.XvoxHomeLibraryMode.ALL_SONGS)
                        }
                        if (next != destination) {
                            tabEpoch++
                        }
                        destination = next
                    }
                )
            }
        }

        AnimatedVisibility(
            visible = player.nowPlayingVisible && currentSong != null,
            enter = androidx.compose.animation.EnterTransition.None,
            exit = androidx.compose.animation.ExitTransition.None,
            modifier = Modifier.fillMaxSize()
        ) {
            val playingSong = currentSong ?: return@AnimatedVisibility

            com.xvox.music.core.design.theme.ProvideXvoxNowPlayingChrome {
                XvoxNowPlaying(
                    song = playingSong,
                    queue = player.queue,
                    currentIndex = player.currentIndex,
                    isPlaying = player.isPlaying,
                    position = player.position,
                    duration = player.duration,
                    onClose = { playerViewModel.closeNowPlaying() },
                    displayMode = nowPlayingDisplayMode,
                    onDisplayModeChange = { nowPlayingDisplayMode = it },
                    onTogglePlay = { playerViewModel.togglePlay() },
                    // Now Playing commits the cover selected on button release.
                    onPlayQueueIndex = { playerViewModel.playQueueIndex(it, keepPlayingState = false) },
                    onSeek = { playerViewModel.seekTo(it) },
                    isLiked = playingSong.id in homeState.likedSongIds,
                    onToggleLiked = {
                        val wasLiked = playingSong.id in homeState.likedSongIds
                        homeViewModel.toggleLiked(playingSong)
                        overlays.showP(if (wasLiked) "Removed from liked" else "Added to liked")
                    },
                    onTimer = ::showTimerBox,
                    onQueue = ::showQueueBox,
                    onInfo = {
                        homeViewModel.loadInfo(playingSong) { info ->
                            overlays.showBox("Song info") {
                                SongInfoBox(info = info)
                            }
                        }
                    },
                    onShare = {
                        XvoxSongActions.share(context, playingSong)
                    },
                    onMore = {
                        showAddCurrentSongToPlaylist(playingSong)
                    },
                    onStarPlaylist = {
                        showAddCurrentSongToPlaylist(playingSong)
                    },
                    isShuffleEnabled = player.isShuffleEnabled,
                    repeatMode = player.repeatMode,
                    onToggleShuffle = { playerViewModel.toggleShuffle() },
                    onToggleRepeat = { playerViewModel.toggleRepeat() },
                    playerStyle = player.playerStyle,
                    sleepTimerProgress = player.sleepTimerProgress,
                    playingSource = player.playingSource,
                    isInPlaylist = isInPlaylist,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }
}

@Composable
private fun TabSurface(
    backgroundImage: String,
    backgroundBrightness: Float,
    content: @Composable () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        if (backgroundImage.isNotBlank()) {
            AsyncImage(
                model = Uri.parse(backgroundImage),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = (1f - backgroundBrightness).coerceIn(0f, 0.95f)))
            )
        }
        content()
    }
}
