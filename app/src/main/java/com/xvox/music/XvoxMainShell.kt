package com.xvox.music

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.core.design.theme.XvoxPersonalFont
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.core.ui.haptics.rememberXvoxHaptics
import com.xvox.music.core.ui.miniplayer.XvoxMiniPlayer
import com.xvox.music.core.ui.miniplayer.XvoxMiniPlayerPlacement
import com.xvox.music.core.ui.navigation.XvoxBottomBar
import com.xvox.music.core.ui.navigation.XvoxDestination
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.features.home.CreatePlaylistBox
import com.xvox.music.features.home.HomeGreeting
import com.xvox.music.features.home.HomeProfileAvatar
import com.xvox.music.features.home.HomeScreen
import com.xvox.music.features.home.HomeViewModel
import com.xvox.music.features.home.LibraryRefreshBox
import com.xvox.music.features.home.PlaylistPickerBox
import com.xvox.music.features.home.ProfileEditorBox
import com.xvox.music.features.home.XvoxSongArtwork
import com.xvox.music.features.player.styles.XvoxPlayerStyle
import com.xvox.music.features.playlist.XvoxHomeLibraryMode
import com.xvox.music.features.search.SearchScreen
import com.xvox.music.features.settings.SettingsScreen
import com.xvox.music.player.nowplaying.XvoxNowPlaying
import com.xvox.music.player.playback.MainPlayerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val MainEase = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

@Composable
fun XvoxMainShell(
    homeViewModel: HomeViewModel = viewModel(),
    playerViewModel: MainPlayerViewModel = viewModel(),
) {
    val colors = XvoxTheme.colors
    val haptics = rememberXvoxHaptics()
    val overlays = LocalXvoxOverlayController.current
    val context = LocalContext.current

    var destination by remember { mutableStateOf(XvoxDestination.HOME) }
    var homeResetKey by remember { mutableLongStateOf(0L) }
    var hoistedSelectedPlaylistId by remember { mutableStateOf<String?>(null) }

    val homeState by homeViewModel.state.collectAsState()
    val player by playerViewModel.state.collectAsState()

    val currentSong =
        player.currentSongId?.let { id ->
            player.queue.firstOrNull { it.id == id }
                ?: homeState.songs.firstOrNull { it.id == id }
        }

    fun showProfileEditor() {
        overlays.showB {
            ProfileEditorBox(
                profile = homeState.profile,
                onCancel = { overlays.hideB() },
                onSave = { name, pfp, uri ->
                    homeViewModel.saveProfile(name, pfp, uri)
                    overlays.hideB()
                    overlays.showP("Profile updated")
                }
            )
        }
    }

    fun showRefreshOverlay() {
        overlays.showB {
            LibraryRefreshBox(
                currentTotal = homeState.songs.size,
                scanning = homeState.refreshing,
                result = null,
                onScan = {
                    haptics.heavy()
                    homeViewModel.refresh { result ->
                        overlays.hideB()
                        overlays.showP("Library refreshed (${result.totalSongs} songs)")
                    }
                },
                onCancel = { overlays.hideB() }
            )
        }
    }

    // Helper to show playlist picker for miniplayer add button and now playing star
    fun showPlaylistPickerForSong(song: Song) {
        overlays.showB {
            PlaylistPickerBox(
                song = song,
                playlists = homeState.playlists,
                onCreate = {
                    overlays.showB {
                        CreatePlaylistBox(
                            songs = homeState.songs,
                            initialSong = song,
                            onCreate = { name, ids ->
                                homeViewModel.createPlaylist(name, ids) { playlist ->
                                    overlays.hideB()
                                    if (playlist != null) {
                                        overlays.showP("Playlist created")
                                    }
                                }
                            }
                        )
                    }
                },
                onAdd = { playlist ->
                    homeViewModel.addToPlaylist(playlist.id, song) { updated ->
                        if (updated != null) {
                            overlays.hideB()
                            overlays.showP("Added to ${updated.name}")
                        }
                    }
                },
                onRemove = { playlist ->
                    homeViewModel.removeFromPlaylist(playlist.id, song) { updated ->
                        if (updated != null) {
                            overlays.hideB()
                            overlays.showP("Removed from ${updated.name}")
                        }
                    }
                },
                songs = homeState.songs,
                songsFor = { pl -> homeViewModel.playlistSongs(pl) }
            )
        }
    }

    fun showMiniPlayerPlaylistPicker() {
        val song = currentSong ?: return
        showPlaylistPickerForSong(song)
    }

    fun showQueueSheet() {
        overlays.showB {
            val listState = rememberLazyListState()
            val density = LocalDensity.current
            val itemHeightPx = with(density) { 60.dp.toPx() }

            var draggingIndex by remember { mutableStateOf<Int?>(null) }
            var dragOffsetY by remember { mutableFloatStateOf(0f) }
            var targetDropIndex by remember { mutableStateOf<Int?>(null) }

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Playing Queue (${player.queue.size})",
                        color = colors.primaryText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    state = listState,
                    modifier = Modifier.heightIn(max = 480.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    itemsIndexed(player.queue, key = { _, s -> "q_${s.id}" }) { idx, s ->
                        val isCurrent = idx == player.currentIndex
                        val isDragging = draggingIndex == idx

                        // Real-time neighboring card dynamic displacement to make room
                        val shiftY by animateFloatAsState(
                            targetValue = when {
                                isDragging -> dragOffsetY
                                draggingIndex != null && targetDropIndex != null -> {
                                    val dragIdx = draggingIndex!!
                                    val targetIdx = targetDropIndex!!
                                    when {
                                        dragIdx < targetIdx && idx > dragIdx && idx <= targetIdx -> -itemHeightPx
                                        dragIdx > targetIdx && idx < dragIdx && idx >= targetIdx -> itemHeightPx
                                        else -> 0f
                                    }
                                }
                                else -> 0f
                            },
                            animationSpec = spring(
                                dampingRatio = 0.85f,
                                stiffness = 1200f
                            ),
                            label = "queueItemShift_$idx"
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isDragging) colors.cardElevated.copy(alpha = 0.98f)
                                    else if (isCurrent) colors.card.copy(alpha = 0.95f)
                                    else colors.cardElevated.copy(alpha = 0.40f)
                                )
                                .graphicsLayer {
                                    translationY = shiftY
                                    scaleX = if (isDragging) 1.025f else 1f
                                    scaleY = if (isDragging) 1.025f else 1f
                                    shadowElevation = if (isDragging) 24f else 0f
                                }
                                .zIndex(if (isDragging) 10f else 1f)
                                .pointerInput(idx, player.queue.size) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            haptics.heavy()
                                            draggingIndex = idx
                                            targetDropIndex = idx
                                            dragOffsetY = 0f
                                        },
                                        onDragEnd = {
                                            val from = draggingIndex
                                            val to = targetDropIndex
                                            draggingIndex = null
                                            targetDropIndex = null
                                            dragOffsetY = 0f
                                            if (from != null && to != null && from != to) {
                                                haptics.success()
                                                playerViewModel.moveQueueItem(from, to)
                                            }
                                        },
                                        onDragCancel = {
                                            draggingIndex = null
                                            targetDropIndex = null
                                            dragOffsetY = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffsetY += dragAmount.y
                                            val offsetIndex = kotlin.math.round(dragOffsetY / itemHeightPx).toInt()
                                            targetDropIndex = (idx + offsetIndex).coerceIn(0, player.queue.lastIndex)
                                        }
                                    )
                                }
                                .clickable {
                                    if (draggingIndex == null) {
                                        playerViewModel.playQueueIndex(idx)
                                        overlays.hideB()
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            XvoxSongArtwork(
                                artwork = s.artworkUri,
                                requestSize = 96,
                                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp))
                            )
                            Column(modifier = Modifier.weight(1f).padding(start = 12.dp, end = 6.dp)) {
                                Text(
                                    text = s.title,
                                    color = if (isCurrent) colors.primaryAccent else colors.primaryText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = s.artist,
                                    color = colors.secondaryText,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (isCurrent) {
                                Text(
                                    text = "Playing",
                                    color = colors.primaryAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun showTimerSheet() {
        overlays.showB {
            TimerSheetContent(
                currentMinutes = player.sleepTimerMinutes,
                onSetMinutes = { mins ->
                    playerViewModel.setSleepTimer(mins)
                    overlays.hideB()
                    overlays.showP("Timer set $mins min")
                },
                onCustom = { mins, secs, pause, closeApp ->
                    playerViewModel.setCustomSleepTimer(mins, secs, pause, closeApp)
                    overlays.hideB()
                    val total = mins * 60 + secs
                    if (total > 0) overlays.showP("Custom timer ${mins}m ${secs}s")
                },
                onCancel = {
                    playerViewModel.cancelSleepTimer()
                    overlays.hideB()
                    overlays.showP("Timer off")
                }
            )
        }
    }

    fun showPlayerStyleSheet() {
        overlays.showB {
            PlayerStyleSheetContent(
                currentStyle = player.playerStyle,
                onSelect = { style ->
                    playerViewModel.setPlayerStyle(style)
                    overlays.hideB()
                    val name = when (style) {
                        XvoxPlayerStyle.NORMAL -> "Normal"
                        XvoxPlayerStyle.FULL_ART -> "Full Art"
                    }
                    overlays.showP("$name style")
                }
            )
        }
    }

    CompositionLocalProvider(LocalXvoxHaptics provides haptics) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // FIXED TOP HEADER (Does NOT animate when switching tabs!)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.background)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 14.dp)
                            .height(54.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HomeProfileAvatar(
                            profile = homeState.profile,
                            modifier = Modifier.size(42.dp),
                            onClick = {
                                haptics.tap()
                                showProfileEditor()
                            }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = homeState.profile.username,
                                color = colors.primaryAccent,
                                fontFamily = XvoxPersonalFont,
                                fontSize = 18.sp,
                                lineHeight = 19.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            HomeGreeting()
                        }

                        // Right Pill Button: Visible ON HOME ONLY, hidden on Search & Settings
                        AnimatedVisibility(
                            visible = destination == XvoxDestination.HOME,
                            enter = fadeIn(tween(180)),
                            exit = fadeOut(tween(140))
                        ) {
                            val actionShape = RoundedCornerShape(21.dp)
                            Row(
                                modifier = Modifier
                                    .height(42.dp)
                                    .clip(actionShape)
                                    .background(colors.card.copy(alpha = 0.72f))
                                    .border(width = 0.65.dp, color = colors.cardBorder, shape = actionShape)
                                    .padding(horizontal = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Scan / Refresh Pill Icon
                                Icon(
                                    painter = painterResource(R.drawable.ic_xvox_refresh),
                                    contentDescription = "Refresh Library",
                                    tint = colors.primaryText,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .xvoxPressScale(pressedScale = 0.90f) {
                                            haptics.tap()
                                            showRefreshOverlay()
                                        }
                                        .padding(8.dp)
                                )

                                // Heart / Liked Songs Toggle Pill Icon
                                Icon(
                                    painter = painterResource(R.drawable.ic_xvox_heart),
                                    contentDescription = "Liked Songs",
                                    tint = if (homeState.libraryMode == XvoxHomeLibraryMode.LIKED) colors.primaryAccent else colors.primaryText,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .xvoxPressScale(pressedScale = 0.90f) {
                                            haptics.tap()
                                            hoistedSelectedPlaylistId = null
                                            homeViewModel.toggleLikedMode()
                                        }
                                        .padding(8.dp)
                                )

                                // Playlist / Songs Toggle Pill Icon
                                Icon(
                                    painter = painterResource(
                                        if (homeState.libraryMode == XvoxHomeLibraryMode.PLAYLISTS) R.drawable.ic_xvox_music_note else R.drawable.ic_xvox_playlist
                                    ),
                                    contentDescription = "Playlists",
                                    tint = if (homeState.libraryMode == XvoxHomeLibraryMode.PLAYLISTS) colors.primaryAccent else colors.primaryText,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .xvoxPressScale(pressedScale = 0.90f) {
                                            haptics.tap()
                                            hoistedSelectedPlaylistId = null
                                            homeViewModel.togglePlaylistMode()
                                        }
                                        .padding(8.dp)
                                )
                            }
                        }
                    }
                }

                // TAB CONTENT (Animates horizontally below the static header)
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    AnimatedContent(
                        targetState = destination,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(220)) + slideInHorizontally(animationSpec = tween(220)) { if (targetState.ordinal > initialState.ordinal) 50 else -50 })
                                .togetherWith(fadeOut(animationSpec = tween(160)))
                        },
                        label = "tab_switch_transition",
                        modifier = Modifier.fillMaxSize()
                    ) { targetDest ->
                        when (targetDest) {
                            XvoxDestination.HOME -> {
                                HomeScreen(
                                    currentSongId = player.currentSongId,
                                    isPlaying = player.isPlaying,
                                    homeResetKey = homeResetKey,
                                    selectedPlaylistId = hoistedSelectedPlaylistId,
                                    onSelectedPlaylistIdChange = { hoistedSelectedPlaylistId = it },
                                    onQueueReady = playerViewModel::setQueue,
                                    onPlay = playerViewModel::play,
                                    playerViewModel = playerViewModel,
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
            val miniVisibleBase = player.miniPlayerVisible &&
                !player.nowPlayingVisible &&
                currentSongId != null &&
                player.queue.isNotEmpty()
            val miniVisible = miniVisibleBase && destination != XvoxDestination.SETTINGS

            // Keyboard-smooth miniplayer placement: sit flush above keyboard when open
            AnimatedVisibility(
                visible = miniVisible,
                enter = slideInVertically(tween(300, easing = MainEase)) { it } + fadeIn(tween(260)),
                exit = slideOutVertically(tween(260, easing = MainEase)) { it } + fadeOut(tween(200)),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                val visSongId = if (miniVisibleBase) currentSongId else null
                if (visSongId != null) {
                    val density = LocalDensity.current
                    val isKeyboardOpen = WindowInsets.ime.getBottom(density) > 0
                    val bottomGap = if (isKeyboardOpen) 8.dp else XvoxMiniPlayerPlacement.miniPlayerBottom

                    val miniModifier = Modifier
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(
                            start = XvoxMiniPlayerPlacement.horizontalEdge,
                            end = XvoxMiniPlayerPlacement.horizontalEdge,
                            bottom = bottomGap,
                        )

                    XvoxMiniPlayer(
                        queue = player.queue,
                        currentSongId = visSongId,
                        currentIndex = player.currentIndex,
                        isPlaying = player.isPlaying,
                        position = player.position,
                        duration = player.duration,
                        riseKey = player.miniPlayerRiseKey,
                        togglePlay = {
                            haptics.click()
                            playerViewModel.togglePlay()
                        },
                        playQueueIndex = {
                            haptics.tap()
                            playerViewModel.playQueueIndex(it)
                        },
                        stopAndDismiss = {
                            haptics.heavy()
                            playerViewModel.stopPlayback()
                        },
                        openPlayer = {
                            haptics.tap()
                            playerViewModel.openNowPlaying()
                        },
                        onLike = {
                            currentSong?.let { song ->
                                val wasLiked = song.id in homeState.likedSongIds
                                homeViewModel.toggleLiked(song)
                                if (wasLiked) haptics.tap() else haptics.success()
                                overlays.showP(if (wasLiked) "Removed from liked" else "Added to liked")
                            }
                        },
                        onAdd = {
                            haptics.tap()
                            showMiniPlayerPlaylistPicker()
                        },
                        modifier = miniModifier
                    )
                }
            }

            // STATIC BOTTOM NAVBAR (Does NOT animate when switching tabs!)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            ) {
                XvoxBottomBar(
                    selected = destination,
                    onSelected = { next ->
                        haptics.tap()
                        if (destination == XvoxDestination.HOME && next == XvoxDestination.HOME) {
                            hoistedSelectedPlaylistId = null
                            homeResetKey = System.currentTimeMillis()
                        }
                        destination = next
                    }
                )
            }

            // NOW PLAYING FULLSCREEN POPUP
            if (player.nowPlayingVisible && currentSong != null) {
                XvoxNowPlaying(
                    song = currentSong,
                    queue = player.queue,
                    currentIndex = player.currentIndex,
                    isPlaying = player.isPlaying,
                    position = player.position,
                    duration = player.duration,
                    onClose = {
                        haptics.tap()
                        playerViewModel.closeNowPlaying()
                    },
                    onTogglePlay = {
                        haptics.click()
                        playerViewModel.togglePlay()
                    },
                    onPrevious = {
                        haptics.click()
                        playerViewModel.playPrevious()
                    },
                    onNext = {
                        haptics.click()
                        playerViewModel.playNext()
                    },
                    onPlayQueueIndex = {
                        haptics.tap()
                        playerViewModel.playQueueIndex(it)
                    },
                    onSeek = {
                        playerViewModel.seekTo(it)
                    },
                    isLiked = currentSong.id in homeState.likedSongIds,
                    onToggleLiked = {
                        val wasLiked = currentSong.id in homeState.likedSongIds
                        homeViewModel.toggleLiked(currentSong)
                        if (wasLiked) haptics.tap() else haptics.success()
                        overlays.showP(if (wasLiked) "Removed from liked" else "Added to liked")
                    },
                    onTimer = {
                        haptics.tap()
                        showTimerSheet()
                    },
                    onQueue = {
                        haptics.tap()
                        showQueueSheet()
                    },
                    onStarPlaylist = {
                        haptics.tap()
                        showPlaylistPickerForSong(currentSong)
                    },
                    onInfo = {
                        haptics.tap()
                        homeViewModel.loadInfo(currentSong) { info ->
                            overlays.showL {
                                com.xvox.music.features.home.SongInfoBox(
                                    info = info
                                )
                            }
                        }
                    },
                    isShuffleEnabled = player.isShuffleEnabled,
                    repeatMode = player.repeatMode,
                    onToggleShuffle = {
                        haptics.toggle()
                        playerViewModel.toggleShuffle()
                        overlays.showP(if (!player.isShuffleEnabled) "Shuffle on" else "Shuffle off")
                    },
                    onToggleRepeat = {
                        haptics.toggle()
                        playerViewModel.toggleRepeat()
                    },
                    playerStyle = player.playerStyle,
                    sleepTimerProgress = player.sleepTimerProgress,
                    playingSource = player.playingSource,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun TimerSheetContent(
    currentMinutes: Int?,
    onSetMinutes: (Int) -> Unit,
    onCustom: (Int, Int, Boolean, Boolean) -> Unit,
    onCancel: () -> Unit
) {
    val colors = XvoxTheme.colors
    var showCustom by remember { mutableStateOf(false) }
    var minText by remember { mutableStateOf("") }
    var secText by remember { mutableStateOf("") }
    var pauseMusic by remember { mutableStateOf(true) }
    var closeApp by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Sleep Timer", color = colors.primaryText, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (currentMinutes != null) {
                Text("$currentMinutes min active", color = colors.primaryAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(10.dp))

        val presets = listOf(5, 10, 15, 30, 45, 60)
        presets.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { mins ->
                    val selected = currentMinutes == mins
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) colors.primaryAccent else colors.card.copy(alpha = 0.97f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onSetMinutes(mins) }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "$mins min",
                            color = if (selected) colors.background else colors.primaryText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(Modifier.size(6.dp))

        // Custom Timer Expandable Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.card.copy(alpha = 0.97f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { showCustom = !showCustom }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Custom time...", color = colors.primaryText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Icon(
                painter = painterResource(if (showCustom) R.drawable.ic_xvox_collapse else R.drawable.ic_xvox_caret_right),
                contentDescription = null,
                tint = colors.secondaryText,
                modifier = Modifier.size(16.dp)
            )
        }

        if (showCustom) {
            Spacer(Modifier.size(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BasicTextField(
                    value = minText,
                    onValueChange = { if (it.length <= 3 && it.all { c -> c.isDigit() }) minText = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(color = colors.primaryText, fontSize = 14.sp),
                    cursorBrush = SolidColor(colors.primaryAccent),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.card.copy(alpha = 0.97f))
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    decorationBox = { inner ->
                        if (minText.isEmpty()) Text("Minutes", color = colors.mutedText, fontSize = 14.sp)
                        inner()
                    }
                )
                BasicTextField(
                    value = secText,
                    onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) secText = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(color = colors.primaryText, fontSize = 14.sp),
                    cursorBrush = SolidColor(colors.primaryAccent),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.card.copy(alpha = 0.97f))
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    decorationBox = { inner ->
                        if (secText.isEmpty()) Text("Seconds", color = colors.mutedText, fontSize = 14.sp)
                        inner()
                    }
                )
            }

            Spacer(Modifier.size(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        pauseMusic = true
                        closeApp = false
                    }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = pauseMusic, onClick = { pauseMusic = true; closeApp = false })
                Text("Pause music", color = colors.primaryText, fontSize = 13.sp)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        closeApp = true
                        pauseMusic = false
                    }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = closeApp, onClick = { closeApp = true; pauseMusic = false })
                Text("Close full app", color = colors.primaryText, fontSize = 13.sp)
            }

            Spacer(Modifier.size(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.primaryAccent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        val m = minText.toIntOrNull() ?: 0
                        val s = secText.toIntOrNull() ?: 0
                        if (m == 0 && s == 0) return@clickable
                        onCustom(m, s, pauseMusic, closeApp)
                    }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Start custom timer", color = colors.background, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        if (currentMinutes != null) {
            Spacer(Modifier.size(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.card.copy(alpha = 0.97f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onCancel() }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Cancel Timer", color = Color(0xFFDC2626), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PlayerStyleSheetContent(
    currentStyle: XvoxPlayerStyle,
    onSelect: (XvoxPlayerStyle) -> Unit
) {
    val colors = XvoxTheme.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Player Style", color = colors.primaryText, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.size(4.dp))
        Text("Choose artwork style", color = colors.secondaryText, fontSize = 12.sp)
        Spacer(Modifier.size(14.dp))

        val items = listOf(
            Triple(XvoxPlayerStyle.NORMAL, "Normal", "Square + backdrop palette"),
            Triple(XvoxPlayerStyle.FULL_ART, "Full Art", "Fullscreen cover")
        )

        items.forEach { (style, name, desc) ->
            val selected = currentStyle == style
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) colors.primaryAccent.copy(alpha = 0.15f) else colors.card.copy(alpha = 0.97f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect(style) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(name, color = if (selected) colors.primaryAccent else colors.primaryText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(desc, color = colors.secondaryText, fontSize = 11.sp)
                }
                if (selected) {
                    Icon(painter = painterResource(R.drawable.ic_xvox_check), contentDescription = null, tint = colors.primaryAccent, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
