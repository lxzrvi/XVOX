package com.xvox.music

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.net.Uri
import coil3.compose.AsyncImage
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
    playerViewModel: MainPlayerViewModel = viewModel(),
    backgroundBrightness: Float = 0.8f
) {
    val colors = XvoxTheme.colors
    val homeState by homeViewModel.state.collectAsState()
    val player by playerViewModel.state.collectAsState()
    val homePreferences = remember { com.xvox.music.data.preferences.UserPreferencesRepository(homeViewModel.getApplication<android.app.Application>()) }
    val mergedHome by homePreferences.homeMerge.collectAsState(initial = false)
    val hiddenHomeSections by homePreferences.homeHiddenSections.collectAsState(initial = emptySet())
    val backgroundImage by homePreferences.themeBackgroundImage.collectAsState(initial = "")
    val overlays = LocalXvoxOverlayController.current
    val context = LocalContext.current
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

    var destination by remember { mutableStateOf(XvoxDestination.HOME) }
    var homeResetKey by remember { mutableLongStateOf(0L) }
    // Bumped on every tab switch so each freshly opened tab lands at the top of its content.
    var tabEpoch by remember { mutableLongStateOf(0L) }
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

    fun showMiniPlayerSettings() {
        overlays.showBox("Mini Player & Nav Bar") {
            com.xvox.music.features.settings.components.MiniPlayerSettingsBoxContent(settingsViewModel)
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
        // A full picker box rather than the small popup: choosing a playlist (or creating one) is
        // an editor action and should read as one, with the song list still visible underneath.
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
                                playerViewModel = playerViewModel
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
                                }
                            )
                        }
                        XvoxDestination.SETTINGS -> {
                            SettingsScreen(homeViewModel = homeViewModel, topResetKey = tabEpoch)
                        }
                    }
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = destination != XvoxDestination.SETTINGS,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(360, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))
            ) + fadeIn(tween(260)),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(320, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))
            ) + fadeOut(tween(220))
        ) {
            XvoxShellTopHeader(
                profile = homeState.profile,
                destination = destination,
                libraryMode = homeState.libraryMode,
                mergedHome = mergedHome,
                likedSectionHidden = com.xvox.music.features.home.HomeSections.LIKED in hiddenHomeSections,
                playlistsSectionHidden = com.xvox.music.features.home.HomeSections.PLAYLISTS in hiddenHomeSections,
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
            },
            onDelete = {
                currentSong?.let { song ->
                    homeViewModel.hideSong(song)
                    overlays.showP("Song deleted")
                }
            },
            onSettings = ::showMiniPlayerSettings
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
                    if (next != destination) tabEpoch++
                    destination = next
                }
            )
        }

        AnimatedVisibility(
            visible = player.nowPlayingVisible && currentSong != null,
            enter = androidx.compose.animation.EnterTransition.None,
            exit = androidx.compose.animation.ExitTransition.None,
            modifier = Modifier.fillMaxSize()
        ) {
            val playingSong = currentSong ?: return@AnimatedVisibility

            // Now Playing renders over the artwork and keeps its own solid chrome; the global
            // card transparency and tinted page backdrop never reach it.
            com.xvox.music.core.design.theme.ProvideXvoxNowPlayingChrome {
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
}

/**
 * One opaque page surface per destination. With a custom background photo it paints the photo
 * (lightly dimmed) instead of a flat colour, so the chosen background reaches every screen —
 * Home and its Liked/Playlist views, Search and Settings — while the Now Playing artwork
 * surface always stays on top and untouched.
 */
@Composable
private fun TabSurface(
    backgroundImage: String,
    backgroundBrightness: Float = 0.8f,
    content: @Composable () -> Unit
) {
    val colors = XvoxTheme.colors
    Box(Modifier.fillMaxSize().background(colors.background)) {
        if (backgroundImage.isNotBlank()) {
            AsyncImage(
                model = Uri.parse(backgroundImage),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // The brightness slider in Appearance dims a bright background photo. Higher value
            // means a brighter image (a thinner dark veil over it).
            val veil = (0.16f + (1f - backgroundBrightness.coerceIn(0.2f, 1f)) * 0.7f).coerceIn(0.12f, 0.9f)
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = veil)))
        }
        content()
    }
}
