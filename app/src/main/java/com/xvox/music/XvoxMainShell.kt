package com.xvox.music

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.xvox.music.features.home.ProfileEditorDraft
import com.xvox.music.features.home.SongInfoBox
import com.xvox.music.features.home.XvoxSongActions
import com.xvox.music.features.home.showCreatePlaylistOverlay
import com.xvox.music.features.home.showLibraryRefresh
import com.xvox.music.features.search.SearchScreen
import com.xvox.music.features.settings.SettingsScreen
import com.xvox.music.player.nowplaying.XvoxArtworkPaletteLoader
import com.xvox.music.player.nowplaying.xvoxArtworkPaletteKey
import com.xvox.music.player.nowplaying.XvoxNowPlaying
import com.xvox.music.player.playback.MainPlayerViewModel
import com.xvox.music.shell.XvoxPlaylistPickerBoxContent
import com.xvox.music.shell.XvoxQueueBoxContent
import com.xvox.music.shell.XvoxShellMiniPlayerHost
import com.xvox.music.shell.XvoxShellTopHeader
import com.xvox.music.shell.XvoxTimerBoxContent
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun XvoxMainShell(
    homeViewModel: HomeViewModel = viewModel(),
    playerViewModel: MainPlayerViewModel = viewModel(),
    settingsViewModel: com.xvox.music.features.settings.SettingsViewModel = viewModel(),
    backgroundBrightness: Float = 0.8f
) {
    val colors = XvoxTheme.colors
    val chrome = com.xvox.music.core.ui.chrome.LocalXvoxChromeStyle.current
    val navigationBarHeight = chrome.navigationBarHeight.coerceIn(52f, 88f).dp
    val homeState by homeViewModel.state.collectAsState()
    val player by playerViewModel.state.collectAsState()
    val homePreferences = remember { com.xvox.music.data.preferences.UserPreferencesRepository(homeViewModel.getApplication<android.app.Application>()) }
    val backgroundImage by homePreferences.themeBackgroundImage.collectAsState(initial = "")
    val overlays = LocalXvoxOverlayController.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
    var profileDraft by remember { mutableStateOf(ProfileEditorDraft.from(homeState.profile, chrome)) }
    var profileEditorOpen by remember { mutableStateOf(false) }
    var miniPlayerNavDraft by remember { mutableStateOf(chrome) }
    // The Header is no longer shell-translated from a page scroll callback.  Each screen receives
    // it as the first item of its own list, guaranteeing one real coordinate space.
    BackHandler(enabled = destination != XvoxDestination.HOME) {
        destination = XvoxDestination.HOME
    }

    fun showProfileEditor() {
        val baseline = ProfileEditorDraft.from(homeState.profile, chrome)
        profileDraft = baseline
        profileEditorOpen = true
        overlays.showBox(
            title = "Profile",
            // Any close route—including back or an outside tap—rolls the live Header preview
            // back to the persisted profile/chrome presentation.
            onDismiss = {
                profileEditorOpen = false
                profileDraft = baseline
            },
            bottomAction = {
                val canSave = profileDraft.username.isNotBlank() &&
                    (profileDraft.selectedPfp != com.xvox.music.features.setup.PfpType.CUSTOM.name ||
                        profileDraft.customPfpUri != null)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProfileSheetFooterButton(
                        text = "Cancel",
                        primary = false,
                        modifier = Modifier.weight(1f),
                        onClick = overlays::hideBox
                    )
                    ProfileSheetFooterButton(
                        text = "Reset",
                        primary = false,
                        modifier = Modifier.weight(1f),
                        onClick = { profileDraft = baseline }
                    )
                    ProfileSheetFooterButton(
                        text = "Save",
                        primary = true,
                        enabled = canSave,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val saved = profileDraft
                            // The whole profile/header edit is committed only from this fixed
                            // footer.  Cancel and Reset never need to roll a persisted value back.
                            scope.launch {
                                homePreferences.setHeaderImageUri(saved.headerImageUri)
                                homePreferences.setShowProfileLines(saved.showProfileLines)
                                settingsViewModel.setChromeStyle {
                                    it.copy(
                                        headerDimEnabled = saved.headerDimEnabled,
                                        headerDimAmount = saved.headerDimAmount.coerceIn(0f, 1f)
                                    )
                                }
                                homeViewModel.saveProfile(saved.username.trim(), saved.selectedPfp, saved.customPfpUri) {
                                    overlays.hideBox()
                                    overlays.showP("Profile updated")
                                }
                            }
                        }
                    )
                }
            }
        ) {
            ProfileEditorBox(
                profile = homeState.profile,
                draft = profileDraft,
                onDraftChange = { profileDraft = it }
            )
        }
    }

    fun showRefreshOverlay() {
        showLibraryRefresh(overlays, homeViewModel)
    }

    fun showMiniPlayerSettings() {
        val baseline = chrome
        miniPlayerNavDraft = baseline
        overlays.showBox(
            title = "Mini Player / Navbar Settings",
            bottomAction = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProfileSheetFooterButton(
                        text = "Cancel",
                        primary = false,
                        modifier = Modifier.weight(1f),
                        onClick = overlays::hideBox
                    )
                    ProfileSheetFooterButton(
                        text = "Reset",
                        primary = false,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            miniPlayerNavDraft = miniPlayerNavDraft.copy(
                                miniCoverStyle = "default",
                                miniCornerRadius = 15f,
                                miniBgAlpha = 1f,
                                navigationBarHeight = 62f,
                                navigationBarWidth = 246f,
                                navBgAlpha = .88f,
                                navigationImageUri = "",
                                miniPlayerOffsetX = 0f,
                                miniPlayerOffsetY = 0f,
                                navigationBarOffsetX = 0f,
                                navigationBarOffsetY = 0f
                            )
                        }
                    )
                    ProfileSheetFooterButton(
                        text = "Okay",
                        primary = true,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val saved = miniPlayerNavDraft
                            settingsViewModel.setChromeStyle { saved }
                            overlays.hideBox()
                            overlays.showP("Mini Player / Navbar settings saved")
                        }
                    )
                }
            }
        ) {
            com.xvox.music.features.settings.components.MiniPlayerSettingsBoxContent(
                chrome = miniPlayerNavDraft,
                onChromeChange = { miniPlayerNavDraft = it }
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
                    playerViewModel.playQueueIndex(index, keepPlayingState = true)
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
    // Start palette work while the Mini Player is on screen. This shared cache is then ready when
    // the sequential 50ms handoff mounts Now Playing, avoiding an unrelated fallback flash.
    val nowPlayingPaletteLoader = remember(context) { XvoxArtworkPaletteLoader(context) }
    LaunchedEffect(currentSong?.xvoxArtworkPaletteKey()) {
        currentSong?.let { selected ->
            nowPlayingPaletteLoader.load(selected.artworkUri, selected.xvoxArtworkPaletteKey())
        }
    }

    val isInPlaylist = remember(homeState.playlists, player.currentSongId) {
        val currentId = player.currentSongId ?: return@remember false
        homeState.playlists.any { playlist ->
            playlist.songIds.contains(currentId)
        }
    }

    fun selectNavigationDestination(next: XvoxDestination) {
        if (next == XvoxDestination.HOME) {
            hoistedSelectedPlaylistId = null
            homeResetKey = System.currentTimeMillis()
            homeViewModel.setLibraryMode(com.xvox.music.features.playlist.XvoxHomeLibraryMode.ALL_SONGS)
        }
        if (next != destination) tabEpoch++
        destination = next
    }

    fun openLikedFromNavigation() {
        hoistedSelectedPlaylistId = null
        val enteringHome = destination != XvoxDestination.HOME
        if (enteringHome) tabEpoch++
        destination = XvoxDestination.HOME
        if (enteringHome) homeViewModel.setLibraryMode(com.xvox.music.features.playlist.XvoxHomeLibraryMode.LIKED)
        else homeViewModel.toggleLikedMode()
    }

    fun openPlaylistsFromNavigation() {
        hoistedSelectedPlaylistId = null
        val enteringHome = destination != XvoxDestination.HOME
        if (enteringHome) tabEpoch++
        destination = XvoxDestination.HOME
        if (enteringHome) homeViewModel.setLibraryMode(com.xvox.music.features.playlist.XvoxHomeLibraryMode.PLAYLISTS)
        else homeViewModel.togglePlaylistMode()
    }

    fun openRecentFromHeader() {
        hoistedSelectedPlaylistId = null
        val enteringHome = destination != XvoxDestination.HOME
        if (enteringHome) tabEpoch++
        destination = XvoxDestination.HOME
        if (enteringHome) homeViewModel.setLibraryMode(com.xvox.music.features.playlist.XvoxHomeLibraryMode.RECENT)
        else homeViewModel.toggleRecentMode()
    }

    // Profile editing is transactionally staged, but its Header image, avatar, and dimness are
    // intentionally rendered from this draft immediately.  The overlay's onDismiss restores the
    // persisted presentation for Cancel/back/scrim paths.
    val headerProfile = if (profileEditorOpen) {
        homeState.profile.copy(
            username = profileDraft.username,
            selectedPfp = profileDraft.selectedPfp,
            customPfpUri = profileDraft.customPfpUri,
            showProfileLines = profileDraft.showProfileLines,
            headerImageUri = profileDraft.headerImageUri
        )
    } else {
        homeState.profile
    }
    val pageHeader: @Composable () -> Unit = {
        XvoxShellTopHeader(
            profile = headerProfile,
            destination = destination,
            libraryMode = homeState.libraryMode,
            onProfileClick = ::showProfileEditor,
            onRefreshClick = ::showRefreshOverlay,
            onLikedClick = ::openLikedFromNavigation,
            onPlaylistClick = ::openPlaylistsFromNavigation,
            onArtistClick = {
                hoistedSelectedPlaylistId = null
                homeViewModel.toggleArtistMode()
            },
            onRecentClick = ::openRecentFromHeader,
            headerDimEnabledOverride = if (profileEditorOpen) profileDraft.headerDimEnabled else null,
            headerDimAmountOverride = if (profileEditorOpen) profileDraft.headerDimAmount else null,
            useSystemInsets = true
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        val density = LocalDensity.current
        val bottomInset = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() } +
            if (isLandscape) navigationBarHeight + 20.dp
            else if (player.miniPlayerVisible && destination != XvoxDestination.SETTINGS) navigationBarHeight + 116.dp
            else navigationBarHeight + 40.dp

        val tabState = rememberSaveableStateHolder()
        // Each page owns a real Header item now; the shell does not reserve or translate one.
        CompositionLocalProvider(LocalXvoxTopInset provides 0.dp, LocalXvoxBottomInset provides bottomInset) {
            AnimatedContent(
                targetState = destination,
                transitionSpec = {
                    val isHeaderStableSwitch =
                        (initialState == XvoxDestination.HOME && targetState == XvoxDestination.SEARCH) ||
                            (initialState == XvoxDestination.SEARCH && targetState == XvoxDestination.HOME)
                    if (isHeaderStableSwitch) {
                        // Home and Search deliberately share an unanimated Header identity. Do
                        // not slide/fade that strip (or its close search-bar relationship) when
                        // moving between the two destinations.
                        androidx.compose.animation.EnterTransition.None
                            .togetherWith(androidx.compose.animation.ExitTransition.None)
                            .using(null)
                    } else {
                        val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                        ((slideInHorizontally(tween(300)) { it * direction } + fadeIn(tween(160)))
                            togetherWith (slideOutHorizontally(tween(300)) { -it * direction } + fadeOut(tween(160))))
                            .using(null)
                    }
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
                                    header = pageHeader
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
                                    header = pageHeader
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


        val currentSongId = player.currentSongId
        // Player surfaces hand off sequentially: each existing 320ms motion clears before the
        // next surface enters.
        val miniVisibleBase = player.miniPlayerVisible && currentSongId != null && player.queue.isNotEmpty()
        val miniVisible = if (isLandscape) (player.miniPlayerVisible && currentSongId != null && player.queue.isNotEmpty()) else (miniVisibleBase && destination != XvoxDestination.SETTINGS)
        var landscapeQuickActionsVisible by remember(currentSongId) { mutableStateOf(false) }
        LaunchedEffect(miniVisible) {
            if (!miniVisible) landscapeQuickActionsVisible = false
        }

        if (isLandscape) {
            if (landscapeQuickActionsVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) { detectTapGestures { landscapeQuickActionsVisible = false } }
                )
            }
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
                            .height(122.dp),
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
                            onAddToPlaylist = { currentSong?.let(::showAddCurrentSongToPlaylist) },
                            onOpenMiniPlayerSettings = ::showMiniPlayerSettings,
                            quickActionsVisible = landscapeQuickActionsVisible,
                            onQuickActionsVisibleChange = { landscapeQuickActionsVisible = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }

                Box(
                    modifier = Modifier.height(navigationBarHeight),
                    contentAlignment = Alignment.Center
                ) {
                    XvoxBottomBar(
                        selected = destination,
                        onSelected = ::selectNavigationDestination,
                        onLongPressSettings = ::showMiniPlayerSettings
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
                onAddToPlaylist = { currentSong?.let(::showAddCurrentSongToPlaylist) },
                onOpenMiniPlayerSettings = ::showMiniPlayerSettings,
                navigationBarHeight = navigationBarHeight
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = XvoxMiniPlayerPlacement.navigationHostBottom)
            ) {
                XvoxBottomBar(
                    selected = destination,
                    onSelected = ::selectNavigationDestination,
                    onLongPressSettings = ::showMiniPlayerSettings
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
                    onDismissStart = { playerViewModel.beginNowPlayingDismissal() },
                    displayMode = nowPlayingDisplayMode,
                    onDisplayModeChange = { nowPlayingDisplayMode = it },
                    onTogglePlay = { playerViewModel.togglePlay() },
                    // Now Playing commits the cover selected on button release.
                    onPlayQueueIndex = { playerViewModel.playQueueIndex(it, keepPlayingState = true) },
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


/** Shared fixed-footer button language used by transactional profile and chrome editors. */
@Composable
private fun ProfileSheetFooterButton(
    text: String,
    primary: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(19.dp)
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(shape)
            .background(
                when {
                    primary && enabled -> colors.primaryAccent
                    else -> colors.cardElevated
                }
            )
            .then(
                if (primary && enabled) Modifier
                else Modifier.border(.8.dp, colors.cardBorder.copy(alpha = .80f), shape)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = text,
            color = when {
                primary && enabled -> colors.background
                enabled -> colors.primaryText
                else -> colors.mutedText
            },
            fontSize = 12.sp,
            fontWeight = if (primary) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.SemiBold
        )
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
