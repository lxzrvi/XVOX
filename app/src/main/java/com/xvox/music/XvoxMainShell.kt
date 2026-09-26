package com.xvox.music

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
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
import com.xvox.music.features.settings.components.XvoxTransactionalFooterActions
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
import com.xvox.music.shell.XvoxConfirmBox
import com.xvox.music.shell.XvoxPlaylistPickerBoxContent
import com.xvox.music.shell.XvoxQueueBoxContent
import com.xvox.music.shell.XvoxShellMiniPlayerHost
import com.xvox.music.shell.XvoxShellTopHeader
import com.xvox.music.shell.XvoxTimerBoxContent
import com.xvox.music.shell.XvoxTimerDraft
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
    // Settings is a forward route from either page; system Back returns to the page that opened it.
    var settingsReturnDestination by rememberSaveable { mutableStateOf(XvoxDestination.HOME) }
    // Sleep-timer input remains locally editable while typing, but every valid selection starts
    // or updates the timer immediately; the footer only closes the ordinary sheet.
    var timerDraft by remember { mutableStateOf<XvoxTimerDraft?>(null) }
    var nowPlayingDisplayMode by rememberSaveable { mutableIntStateOf(0) }
    // Survives closing/reopening Now Playing, unlike an action-page remember inside its subtree.
    var nowPlayingActionsPage by rememberSaveable { mutableIntStateOf(0) }
    var homeResetKey by rememberSaveable { mutableLongStateOf(0L) }
    // A destination return deliberately preserves the chosen library page, but always lands that
    // page at its own top rather than reviving an old deep Home scroll position.
    var homeScrollResetKey by rememberSaveable { mutableLongStateOf(0L) }
    // Search and Settings reset only when they are newly selected, not when Settings Back
    // restores Search as the reverse route.
    var searchTopResetKey by rememberSaveable { mutableLongStateOf(0L) }
    var settingsTopResetKey by rememberSaveable { mutableLongStateOf(0L) }
    var hoistedSelectedPlaylistId by rememberSaveable { mutableStateOf<String?>(null) }
    var profileDraft by remember { mutableStateOf(ProfileEditorDraft.from(homeState.profile, chrome)) }
    // Live mirror for the compact chrome sheet. It makes slider drags coherent even before the
    // asynchronous persistence flow emits its matching composition-local value.
    var miniPlayerNavLive by remember { mutableStateOf(chrome) }
    // The Header is a real first item in Home/Search rather than a shell-translated overlay.

    fun showProfileEditor() {
        val baseline = ProfileEditorDraft.from(homeState.profile, chrome)
        profileDraft = baseline
        overlays.showBox(
            title = "Profile",
            // Any close route discards the local profile draft. The underlying Header has stayed
            // on its persisted presentation throughout this editor transaction.
            onDismiss = {
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
        miniPlayerNavLive = chrome
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
                        // This is a live editor: Cancel only dismisses; it never rolls back.
                        onClick = overlays::hideBox
                    )
                    ProfileSheetFooterButton(
                        text = "Reset",
                        primary = false,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            // Only controls exposed by this compact editor reset. All changes
                            // apply at the moment the control is touched, including this reset.
                            val reset = miniPlayerNavLive.copy(
                                miniCoverStyle = "default",
                                miniBgAlpha = .94f,
                                navBgAlpha = .94f
                            )
                            miniPlayerNavLive = reset
                            settingsViewModel.setChromeStyle { reset }
                        }
                    )
                    ProfileSheetFooterButton(
                        text = "Okay",
                        primary = true,
                        modifier = Modifier.weight(1f),
                        // Okay acknowledges the already-live values and closes the sheet.
                        onClick = overlays::hideBox
                    )
                }
            }
        ) {
            com.xvox.music.features.settings.components.MiniPlayerSettingsBoxContent(
                chrome = miniPlayerNavLive,
                onChromeChange = { updated ->
                    miniPlayerNavLive = updated
                    settingsViewModel.setChromeStyle { updated }
                }
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
                currentIndex = if (isViewingActiveQueue) liveState.currentIndex else -1,
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
        val initiallyActive = player.sleepTimerMinutes != null
        timerDraft = player.sleepTimerMinutes?.let { XvoxTimerDraft(minutes = it) }

        fun applyLiveTimer(draft: XvoxTimerDraft?) {
            val value = draft ?: return
            when {
                value.seconds > 0 -> playerViewModel.setCustomSleepTimer(
                    value.minutes,
                    value.seconds,
                    value.pauseMusic,
                    value.closeApp
                )
                value.minutes > 0 -> playerViewModel.setSleepTimer(value.minutes)
                // An incomplete custom field must not unexpectedly cancel a running timer.
            }
        }

        overlays.showBox(
            title = "Sleep timer",
            bottomAction = {
                XvoxTransactionalFooterActions(
                    onCancel = { overlays.hideBox() },
                    onReset = if (initiallyActive) {
                        {
                            playerViewModel.cancelSleepTimer()
                            overlays.hideBox()
                            overlays.showP("Timer off")
                        }
                    } else null,
                    resetLabel = "Off",
                    // Values have already applied as soon as the user selected or edited them.
                    onOkay = overlays::hideBox
                )
            },
            onDismiss = { timerDraft = null }
        ) {
            XvoxTimerBoxContent(
                currentMinutes = player.sleepTimerMinutes,
                draft = timerDraft,
                onDraftChange = { draft ->
                    timerDraft = draft
                    applyLiveTimer(draft)
                }
            )
        }
    }

    val currentSong = remember(player.queue, player.currentSongId, player.currentIndex, homeState.songs) {
        // Queue position is authoritative for a repeated occurrence; an ID lookup alone would
        // always reopen the first copy in Now Playing.
        player.queue.getOrNull(player.currentIndex)?.takeIf { it.id == player.currentSongId }
            ?: player.queue.firstOrNull { it.id == player.currentSongId }
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

    fun returnToHome(resetScroll: Boolean = true) {
        if (destination != XvoxDestination.HOME) {
            destination = XvoxDestination.HOME
        }
        if (resetScroll) homeScrollResetKey++
    }

    fun selectNavigationDestination(next: XvoxDestination) {
        if (next == XvoxDestination.HOME) {
            if (destination == XvoxDestination.HOME) {
                // A second Home tap is the familiar home action: return to All Songs at its top.
                hoistedSelectedPlaylistId = null
                homeViewModel.setLibraryMode(com.xvox.music.features.playlist.XvoxHomeLibraryMode.ALL_SONGS)
                homeResetKey++
                homeScrollResetKey++
            } else {
                // Returning by navbar preserves the selected Home pill/page but never an old
                // deep scroll position.
                returnToHome(resetScroll = true)
            }
            return
        }
        if (next != destination) {
            when (next) {
                XvoxDestination.SEARCH -> searchTopResetKey++
                XvoxDestination.SETTINGS -> {
                    settingsReturnDestination = destination
                    settingsTopResetKey++
                }
                XvoxDestination.HOME -> Unit
            }
            destination = next
        }
    }

    fun openHomeLibrary(mode: com.xvox.music.features.playlist.XvoxHomeLibraryMode) {
        hoistedSelectedPlaylistId = null
        if (destination != XvoxDestination.HOME) {
            destination = XvoxDestination.HOME
        }
        homeViewModel.setLibraryMode(mode)
        // Every header-pill selection deliberately opens its page at the beginning.
        homeScrollResetKey++
    }

    fun openLikedFromNavigation() =
        openHomeLibrary(com.xvox.music.features.playlist.XvoxHomeLibraryMode.LIKED)

    fun openPlaylistsFromNavigation() =
        openHomeLibrary(com.xvox.music.features.playlist.XvoxHomeLibraryMode.PLAYLISTS)

    fun openRecentFromHeader() =
        openHomeLibrary(com.xvox.music.features.playlist.XvoxHomeLibraryMode.RECENT)

    fun openArtistsFromHeader() =
        openHomeLibrary(com.xvox.music.features.playlist.XvoxHomeLibraryMode.ARTISTS)

    // Home and Search each own an actual Header list item. Search intentionally retains the
    // profile/Header surface while the Home-only refresh/action pill exits upward.
    val homePageHeader: @Composable () -> Unit = {
        AnimatedVisibility(
            visible = destination != XvoxDestination.SETTINGS,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(240)) + fadeIn(tween(150)),
            exit = slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(210)) + fadeOut(tween(120))
        ) {
            XvoxShellTopHeader(
                profile = homeState.profile,
                destination = XvoxDestination.HOME,
                libraryMode = homeState.libraryMode,
                onProfileClick = ::showProfileEditor,
                onRefreshClick = ::showRefreshOverlay,
                onLikedClick = ::openLikedFromNavigation,
                onPlaylistClick = ::openPlaylistsFromNavigation,
                onArtistClick = ::openArtistsFromHeader,
                onRecentClick = ::openRecentFromHeader,
                showHomeControls = destination == XvoxDestination.HOME,
                useSystemInsets = true
            )
        }
    }
    val searchPageHeader: @Composable () -> Unit = {
        AnimatedVisibility(
            visible = destination != XvoxDestination.SETTINGS,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(240)) + fadeIn(tween(150)),
            exit = slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(210)) + fadeOut(tween(120))
        ) {
            XvoxShellTopHeader(
                profile = homeState.profile,
                destination = XvoxDestination.SEARCH,
                libraryMode = homeState.libraryMode,
                onProfileClick = ::showProfileEditor,
                onRefreshClick = {},
                onLikedClick = {},
                onPlaylistClick = {},
                onArtistClick = {},
                onRecentClick = {},
                showHomeControls = false,
                useSystemInsets = true
            )
        }
    }

    BackHandler(enabled = destination != XvoxDestination.HOME) {
        if (destination == XvoxDestination.SETTINGS) {
            val returnDestination = settingsReturnDestination
                .takeIf { it != XvoxDestination.SETTINGS }
                ?: XvoxDestination.HOME
            if (returnDestination == XvoxDestination.HOME) {
                returnToHome(resetScroll = true)
            } else {
                // Settings → Search is the exact reverse route, retaining Search's page/header.
                destination = returnDestination
            }
        } else {
            returnToHome(resetScroll = true)
        }
    }
    BackHandler(enabled = destination == XvoxDestination.HOME && !overlays.isBoxVisible) {
        when {
            hoistedSelectedPlaylistId != null -> {
                // A selected playlist is a Home detail route, not an app-exit state.
                hoistedSelectedPlaylistId = null
                homeScrollResetKey++
            }
            homeState.libraryMode != com.xvox.music.features.playlist.XvoxHomeLibraryMode.ALL_SONGS -> {
                // Only All Songs is the terminal Home destination. Other library pages Back to it.
                homeViewModel.setLibraryMode(com.xvox.music.features.playlist.XvoxHomeLibraryMode.ALL_SONGS)
                homeScrollResetKey++
            }
            else -> overlays.showBox(title = "Want to close app soon :(") {
                XvoxConfirmBox(
                    question = "Want to close app soon :(",
                    confirmLabel = "Okay",
                    cancelLabel = "Cancel",
                    onConfirm = {
                        overlays.hideBox()
                        (context as? android.app.Activity)?.finish()
                    },
                    onCancel = overlays::hideBox
                )
            }
        }
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
                    // Home → Search and either page → Settings enter from the right. Leaving
                    // Settings is therefore the true reverse: its destination enters from left
                    // while Settings travels away to the right.
                    val forward = (targetState == XvoxDestination.SETTINGS && initialState != XvoxDestination.SETTINGS) ||
                        (initialState == XvoxDestination.HOME && targetState == XvoxDestination.SEARCH)
                    val direction = if (forward) 1 else -1
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
                                    scrollResetKey = homeScrollResetKey,
                                    selectedPlaylistId = hoistedSelectedPlaylistId,
                                    onSelectedPlaylistIdChange = { hoistedSelectedPlaylistId = it },
                                    onQueueReady = playerViewModel::setQueue,
                                    onPlay = playerViewModel::play,
                                    playerViewModel = playerViewModel,
                                    header = homePageHeader
                                )
                            }
                            XvoxDestination.SEARCH -> {
                                SearchScreen(
                                    homeViewModel = homeViewModel,
                                    playerViewModel = playerViewModel,
                                    topResetKey = searchTopResetKey,
                                    onPlaylistSelected = { playlistId ->
                                        hoistedSelectedPlaylistId = playlistId
                                        returnToHome(resetScroll = true)
                                    },
                                    header = searchPageHeader
                                )
                            }
                            XvoxDestination.SETTINGS -> {
                                SettingsScreen(
                                    homeViewModel = homeViewModel,
                                    settingsViewModel = settingsViewModel,
                                    topResetKey = settingsTopResetKey
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
                        onLongPressSettings = {}
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
                    onLongPressSettings = {}
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
                            overlays.showBox(
                                title = "Song info",
                                bottomAction = {
                                    XvoxTransactionalFooterActions(
                                        onCancel = overlays::hideBox,
                                        onOkay = overlays::hideBox
                                    )
                                }
                            ) { SongInfoBox(info = info) }
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
                    actionPageIndex = nowPlayingActionsPage,
                    onActionPageChange = { nowPlayingActionsPage = it },
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
