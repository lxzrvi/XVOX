package com.xvox.music

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.core.ui.haptics.rememberXvoxHaptics
import com.xvox.music.core.ui.navigation.XvoxBottomBar
import com.xvox.music.core.ui.navigation.XvoxDestination
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.features.home.CreatePlaylistBox
import com.xvox.music.features.home.HomeScreen
import com.xvox.music.features.home.HomeViewModel
import com.xvox.music.features.home.PlaylistPickerBox
import com.xvox.music.features.home.XvoxSongArtwork
import com.xvox.music.features.player.styles.PlayerStyleSheetContent
import com.xvox.music.features.search.SearchScreen
import com.xvox.music.features.settings.SettingsScreen
import com.xvox.music.player.mini.XvoxMiniPlayer
import com.xvox.music.player.mini.XvoxMiniPlayerPlacement
import com.xvox.music.player.nowplaying.TimerSheetContent
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
    var homeResetKey by remember { mutableIntStateOf(0) }
    var hoistedSelectedPlaylistId by remember { mutableStateOf<String?>(null) }

    val homeState by homeViewModel.state.collectAsState()
    val player by playerViewModel.state.collectAsState()

    val currentSong =
        player.currentSongId?.let { id ->
            player.queue.firstOrNull { it.id == id }
                ?: homeState.songs.firstOrNull { it.id == id }
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
            val scope = rememberCoroutineScope()
            var draggingOriginal by remember { mutableStateOf<Int?>(null) }
            var draggingIndex by remember { mutableStateOf<Int?>(null) }
            var dragOffset by remember { mutableFloatStateOf(0f) }
            var targetIndex by remember { mutableStateOf<Int?>(null) }
            val density = LocalDensity.current
            val itemHeightPx = with(density) { 62.dp.toPx() }

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

                        val gapShift = when {
                            draggingOriginal == null || targetIndex == null -> 0f
                            isDragging -> 0f
                            draggingOriginal!! < targetIndex!! && idx in (draggingOriginal!! + 1)..targetIndex!! -> -itemHeightPx
                            draggingOriginal!! > targetIndex!! && idx in targetIndex!! until draggingOriginal!! -> itemHeightPx
                            else -> 0f
                        }
                        val animGap by animateFloatAsState(targetValue = gapShift, animationSpec = tween(180), label = "gap")

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isCurrent) colors.card.copy(alpha = 0.95f) else colors.cardElevated.copy(alpha = 0.40f))
                                .graphicsLayer {
                                    translationY = if (isDragging) dragOffset else animGap
                                    shadowElevation = if (isDragging) 18f else 0f
                                }
                                .zIndex(if (isDragging) 4f else 0f)
                                .clickable {
                                    if (draggingIndex == null) {
                                        playerViewModel.playQueueIndex(idx)
                                        overlays.hideB()
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            XvoxSongArtwork(
                                artwork = s.artworkUri,
                                requestSize = 96,
                                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(8.dp))
                            )
                            Column(modifier = Modifier.weight(1f).padding(start = 10.dp, end = 6.dp)) {
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
                            if (isCurrent && player.isPlaying) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_xvox_equalizer),
                                    contentDescription = null,
                                    tint = colors.primaryAccent,
                                    modifier = Modifier.size(16.dp).padding(end = 6.dp)
                                )
                            }

                            // Tactile Drag Handle
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .pointerInput(idx, player.queue.size) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                haptics.heavy()
                                                draggingOriginal = idx
                                                draggingIndex = idx
                                                targetIndex = idx
                                                dragOffset = 0f
                                            },
                                            onDragEnd = {
                                                val from = draggingOriginal
                                                val to = targetIndex
                                                draggingIndex = null
                                                draggingOriginal = null
                                                targetIndex = null
                                                dragOffset = 0f
                                                if (from != null && to != null && from != to) {
                                                    scope.launch {
                                                        delay(50)
                                                        playerViewModel.moveQueueItem(from, to)
                                                    }
                                                }
                                            },
                                            onDragCancel = {
                                                draggingIndex = null
                                                draggingOriginal = null
                                                targetIndex = null
                                                dragOffset = 0f
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffset += dragAmount.y
                                                val orig = draggingOriginal ?: idx
                                                val offsetIndex = kotlin.math.round(dragOffset / itemHeightPx).toInt()
                                                val target = (orig + offsetIndex).coerceIn(0, player.queue.lastIndex)
                                                targetIndex = target
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_xvox_queue),
                                    contentDescription = "Reorder",
                                    tint = colors.secondaryText,
                                    modifier = Modifier.size(17.dp)
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
                        com.xvox.music.features.player.styles.XvoxPlayerStyle.NORMAL -> "Normal"
                        com.xvox.music.features.player.styles.XvoxPlayerStyle.FULL_ART -> "Full Art"
                    }
                    overlays.showP("$name style")
                }
            )
        }
    }

    val density = LocalDensity.current
    val navBarsBottom = WindowInsets.navigationBars.getBottom(density)
    val imeBottom = WindowInsets.ime.getBottom(density)
    val isKeyboardOpen = imeBottom > navBarsBottom

    CompositionLocalProvider(LocalXvoxHaptics provides haptics) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
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

            val currentSongId = player.currentSongId
            val miniVisibleBase = player.miniPlayerVisible &&
                !player.nowPlayingVisible &&
                currentSongId != null &&
                player.queue.isNotEmpty()
            val miniVisible = miniVisibleBase && destination != XvoxDestination.SETTINGS

            // Keyboard-smooth miniplayer placement: stays perfectly at resting spot on keyboard close
            val targetBottom = if (isKeyboardOpen) 8.dp else XvoxMiniPlayerPlacement.miniPlayerBottom
            val animatedBottom by animateDpAsState(targetValue = targetBottom, animationSpec = tween(180), label = "mini_bottom")

            AnimatedVisibility(
                visible = miniVisible,
                enter = slideInVertically(tween(260, easing = MainEase)) { it } + fadeIn(tween(240)),
                exit = slideOutVertically(tween(240, easing = MainEase)) { it } + fadeOut(tween(200)),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                val visSongId = if (miniVisibleBase) currentSongId else null
                if (visSongId != null) {
                    val miniModifier = Modifier
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(
                            start = XvoxMiniPlayerPlacement.horizontalEdge,
                            end = XvoxMiniPlayerPlacement.horizontalEdge,
                            bottom = animatedBottom,
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
                        modifier = miniModifier,
                    )
                } else {
                    Box(Modifier.fillMaxWidth().heightIn(min = 1.dp))
                }
            }

            if (!isKeyboardOpen) {
                XvoxBottomBar(
                    selected = destination,
                    onSelected = { selected ->
                        haptics.tap()
                        if (selected == XvoxDestination.HOME && destination == XvoxDestination.HOME) {
                            homeResetKey++
                            hoistedSelectedPlaylistId = null
                        } else {
                            destination = selected
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(bottom = XvoxMiniPlayerPlacement.navigationHostBottom),
                )
            }

            // NowPlaying Presentation with silky smooth slide transition
            AnimatedVisibility(
                visible = player.nowPlayingVisible && currentSong != null,
                enter = slideInVertically(tween(300, easing = MainEase)) { it } + fadeIn(tween(220)),
                exit = slideOutVertically(tween(260, easing = MainEase)) { it } + fadeOut(tween(200)),
                modifier = Modifier.fillMaxSize()
            ) {
                if (currentSong != null) {
                    XvoxNowPlaying(
                        song = currentSong,
                        queue = player.queue,
                        currentIndex = player.currentIndex,
                        isPlaying = player.isPlaying,
                        position = player.position,
                        duration = player.duration,
                        onClose = playerViewModel::closeNowPlaying,
                        onTogglePlay = playerViewModel::togglePlay,
                        onPrevious = playerViewModel::playPrevious,
                        onNext = playerViewModel::playNext,
                        onPlayQueueIndex = playerViewModel::playQueueIndex,
                        onSeek = playerViewModel::seekTo,
                        isLiked = currentSong.id in homeState.likedSongIds,
                        onToggleLiked = {
                            val wasLiked = currentSong.id in homeState.likedSongIds
                            homeViewModel.toggleLiked(currentSong)
                            overlays.showP(if (wasLiked) "Removed from liked" else "Added to liked")
                        },
                        onTimer = { showTimerSheet() },
                        onQueue = { showQueueSheet() },
                        onInfo = {
                            homeViewModel.loadInfo(currentSong) { info ->
                                overlays.showB {
                                    com.xvox.music.features.home.SongInfoBox(info)
                                }
                            }
                        },
                        onShare = {
                            com.xvox.music.features.home.XvoxSongActions.share(context, currentSong)
                        },
                        onMore = { showPlayerStyleSheet() },
                        onStarPlaylist = {
                            showPlaylistPickerForSong(currentSong)
                        },
                        isShuffleEnabled = player.isShuffleEnabled,
                        repeatMode = player.repeatMode,
                        onToggleShuffle = playerViewModel::toggleShuffle,
                        onToggleRepeat = playerViewModel::cycleRepeatMode,
                        playerStyle = player.playerStyle,
                        sleepTimerProgress = player.sleepTimerProgress,
                        playingSource = player.playingSource,
                    )
                }
            }
        }
    }
}
