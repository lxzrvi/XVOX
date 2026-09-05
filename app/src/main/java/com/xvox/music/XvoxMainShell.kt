package com.xvox.music

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.miniplayer.XvoxMiniPlayer
import com.xvox.music.core.ui.miniplayer.XvoxMiniPlayerPlacement
import com.xvox.music.core.ui.navigation.XvoxBottomBar
import com.xvox.music.core.ui.navigation.XvoxDestination
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.features.home.HomeScreen
import com.xvox.music.features.home.HomeViewModel
import com.xvox.music.features.home.PlaylistPickerBox
import com.xvox.music.features.home.CreatePlaylistBox
import com.xvox.music.features.search.SearchScreen
import com.xvox.music.features.settings.SettingsScreen
import com.xvox.music.player.nowplaying.XvoxNowPlaying
import com.xvox.music.player.playback.MainPlayerViewModel
import com.xvox.music.player.playback.RepeatMode
import com.xvox.music.features.home.XvoxSongArtwork
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun XvoxMainShell(
    playerViewModel: MainPlayerViewModel =
        viewModel(),
    homeViewModel: HomeViewModel = viewModel(),
) {
    val colors =
        XvoxTheme.colors

    val player by
        playerViewModel
            .state
            .collectAsState()

    val homeState by homeViewModel.state.collectAsState()
    val overlays = LocalXvoxOverlayController.current
    val context = LocalContext.current

    // Full app close from background when timer with closeApp ends
    androidx.compose.runtime.LaunchedEffect(player.sleepTimerShouldCloseApp) {
        if (player.sleepTimerShouldCloseApp) {
            playerViewModel.consumeCloseApp()
            // Close full app from background
            (context as? android.app.Activity)?.finishAffinity()
            kotlin.system.exitProcess(0)
        }
    }

    var destination by
        remember {
            mutableStateOf(
                XvoxDestination.HOME,
            )
        }

    // 9 – Back navigation: Search/Settings → Home, Home → system exit
    BackHandler(enabled = destination != XvoxDestination.HOME) {
        destination = XvoxDestination.HOME
    }

    var homeResetKey by
        remember {
            mutableLongStateOf(0L)
        }

    // Hoisted playlist selection to survive tab switches - preserves liked/playlist section
    // First home tap after tab change returns to same section, second tap resets to All Songs via homeResetKey
    var hoistedSelectedPlaylistId by remember {
        mutableStateOf<String?>(null)
    }

    val currentSong =
        player.queue
            .getOrNull(
                player.currentIndex,
            )
            ?: player.currentSongId
                ?.let { id ->

                    player.queue
                        .firstOrNull {
                            it.id == id
                        }
                }

    // Helper to show playlist picker for miniplayer add button and now playing star
    fun showPlaylistPickerForSong(song: com.xvox.music.core.model.Song) {
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
                songsFor = homeViewModel::playlistSongs,
            )
        }
    }

    fun showMiniPlayerPlaylistPicker() {
        val song = currentSong ?: return
        showPlaylistPickerForSong(song)
    }

    fun showQueueSheet() {
        overlays.showB {
            val listState = androidx.compose.foundation.lazy.rememberLazyListState()
            val scope = rememberCoroutineScope()
            // Free drag: original stays lifted finger-attached, gap smooth, move only on release
            var draggingOriginal by remember { mutableStateOf<Int?>(null) }
            var draggingIndex by remember { mutableStateOf<Int?>(null) }
            var dragOffset by remember { mutableStateOf(0f) }
            var targetIndex by remember { mutableStateOf<Int?>(null) }
            val density = androidx.compose.ui.platform.LocalDensity.current
            val itemHeightPx = with(density) { 60.dp.toPx() }
            // Header approx 52dp functional (36 header + 16 source), no extra outer padding - use available area
            val headerTopPadding = 52.dp
            Box(modifier = Modifier.fillMaxWidth()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.heightIn(max = 520.dp).animateContentSize(animationSpec = tween(220)),
                    contentPadding = PaddingValues(top = headerTopPadding, bottom = 12.dp)
                ) {
                    itemsIndexed(player.queue, key = { _, s -> s.id }) { idx, s ->
                        val isCurrent = idx == player.currentIndex
                        val isDragging = draggingIndex == idx
                        // Compute gap shift for smooth animated space
                        val gapShift = when {
                            draggingOriginal == null || targetIndex == null -> 0f
                            isDragging -> 0f
                            draggingOriginal!! < targetIndex!! && idx in (draggingOriginal!! + 1)..targetIndex!! -> -itemHeightPx
                            draggingOriginal!! > targetIndex!! && idx in targetIndex!! until draggingOriginal!! -> itemHeightPx
                            else -> 0f
                        }
                        val animGap by androidx.compose.animation.core.animateFloatAsState(targetValue = gapShift, animationSpec = tween(220), label = "gap")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isCurrent) colors.card.copy(alpha = 0.97f) else colors.background)
                                .graphicsLayer {
                                    translationY = if (isDragging) dragOffset else animGap
                                    shadowElevation = if (isDragging) 16f else 0f
                                }
                                .zIndex(if (isDragging) 2f else 0f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    if (draggingIndex == null) {
                                        playerViewModel.playQueueIndex(idx)
                                        overlays.hideB()
                                    }
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            XvoxSongArtwork(
                                artwork = s.artworkUri,
                                requestSize = 96,
                                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp))
                            )
                            Column(modifier = Modifier.weight(1f).padding(start = 10.dp, end = 4.dp)) {
                                Text(s.title, color = if (isCurrent) colors.primaryAccent else colors.primaryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(s.artist, color = colors.secondaryText, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (isCurrent && player.isPlaying) {
                                Icon(painter = painterResource(R.drawable.ic_xvox_equalizer), contentDescription = null, tint = colors.primaryAccent, modifier = Modifier.size(16.dp))
                            }
                            // 6 dot long-hold handle – finger follows vertically, full card gap, auto-scroll
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .pointerInput(idx, player.queue.size) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { _: androidx.compose.ui.geometry.Offset ->
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
                                                    // smooth settle: delay a frame then move
                                                    scope.launch {
                                                        delay(80)
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
                                                // Auto-scroll at edges: check viewport
                                                val layout = listState.layoutInfo
                                                val visible = layout.visibleItemsInfo
                                                if (visible.isNotEmpty()) {
                                                    val first = visible.first().index
                                                    val last = visible.last().index
                                                    // near top edge
                                                    if (target <= first + 1 && dragOffset < -10f) {
                                                        scope.launch { listState.animateScrollBy(-itemHeightPx) }
                                                    } else if (target >= last - 1 && dragOffset > 10f) {
                                                        scope.launch { listState.animateScrollBy(itemHeightPx) }
                                                    }
                                                }
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                // 6 dots (2 columns x 3 rows)
                                Column(
                                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    repeat(3) {
                                        Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(2.dp)) {
                                            repeat(2) {
                                                Box(modifier = Modifier.size(3.dp).background(colors.mutedText.copy(alpha = 0.55f), androidx.compose.foundation.shape.CircleShape))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                // Header overlay translucent behind scroll, readable, subtle visibility of scrolled items beneath
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .background(colors.cardElevated.copy(alpha = 0.82f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp).padding(end = 36.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Queue", color = colors.primaryText, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    }
                    Text(
                        text = player.playingSource,
                        color = colors.secondaryText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.2.sp,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp, end = 16.dp)
                    )
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
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    colors.background,
                ),
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
                        currentSongId =
                            player.currentSongId,
                        isPlaying =
                            player.isPlaying,
                        homeResetKey =
                        homeResetKey,
                        selectedPlaylistId = hoistedSelectedPlaylistId,
                        onSelectedPlaylistIdChange = { hoistedSelectedPlaylistId = it },
                        onQueueReady =
                            playerViewModel::setQueue,
                        onPlay =
                            playerViewModel::play,
                        playerViewModel =
                        playerViewModel,
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

        val currentSongId =
            player.currentSongId

        // 2 + 14 – MiniPlayer keyboard aware + Settings slide out
        val miniVisibleBase =
            player.miniPlayerVisible &&
            !player.nowPlayingVisible &&
            currentSongId != null &&
            player.queue.isNotEmpty()
        val miniVisible = miniVisibleBase && destination != XvoxDestination.SETTINGS

        AnimatedVisibility(
            visible = miniVisible,
            enter = slideInVertically(tween(260)) { it } + fadeIn(tween(260)),
            exit = slideOutVertically(tween(260)) { it } + fadeOut(tween(260)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val visSongId = if (miniVisibleBase) currentSongId else null
            if (visSongId != null) {
                val miniModifier = if (isImeVisible) {
                    Modifier
                        .imePadding()
                        .padding(
                            start = XvoxMiniPlayerPlacement.horizontalEdge,
                            end = XvoxMiniPlayerPlacement.horizontalEdge,
                            bottom = 8.dp,
                        )
                } else {
                    Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(
                            start = XvoxMiniPlayerPlacement.horizontalEdge,
                            end = XvoxMiniPlayerPlacement.horizontalEdge,
                            bottom = XvoxMiniPlayerPlacement.miniPlayerBottom,
                        )
                }

                XvoxMiniPlayer(
                    queue = player.queue,
                    currentSongId = visSongId,
                    currentIndex = player.currentIndex,
                    isPlaying = player.isPlaying,
                    position = player.position,
                    duration = player.duration,
                    riseKey = player.miniPlayerRiseKey,
                    togglePlay = playerViewModel::togglePlay,
                    playQueueIndex = playerViewModel::playQueueIndex,
                    stopAndDismiss = playerViewModel::stopPlayback,
                    openPlayer = playerViewModel::openNowPlaying,
                    onLike = {
                        currentSong?.let { song ->
                            val wasLiked = song.id in homeState.likedSongIds
                            homeViewModel.toggleLiked(song)
                            overlays.showP(if (wasLiked) "Removed from liked" else "Added to liked")
                        }
                    },
                    onAdd = { showMiniPlayerPlaylistPicker() },
                    modifier = miniModifier,
                )
            } else {
                Box(Modifier.fillMaxWidth().heightIn(min = 1.dp))
            }
        }

        if (!isImeVisible) {
            XvoxBottomBar(
                selected =
                destination,
                onSelected = { selected ->

                    if (
                        selected ==
                        XvoxDestination.HOME &&
                        destination ==
                        XvoxDestination.HOME
                    ) {
                        homeResetKey++
                        hoistedSelectedPlaylistId = null
                    } else {
                        destination =
                            selected
                    }
                },
                modifier =
                    Modifier
                        .align(
                            Alignment
                                .BottomCenter,
                        ).windowInsetsPadding(
                            WindowInsets
                                .navigationBars,
                        ).padding(
                            bottom =
                                XvoxMiniPlayerPlacement
                                    .navigationHostBottom,
                        ),
            )
        }

        if (
            player.nowPlayingVisible &&
            currentSong != null
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .pointerInput(
                            Unit,
                        ) {
                            detectTapGestures(
                                onTap = {},
                            )
                        },
            )

            XvoxNowPlaying(
                song =
                currentSong,
                queue =
                    player.queue,
                currentIndex =
                    player.currentIndex,
                isPlaying =
                    player.isPlaying,
                position =
                    player.position,
                duration =
                    player.duration,
                onClose =
                    playerViewModel::closeNowPlaying,
                onTogglePlay =
                    playerViewModel::togglePlay,
                onPrevious =
                    playerViewModel::playPrevious,
                onNext =
                    playerViewModel::playNext,
                onPlayQueueIndex =
                    playerViewModel::playQueueIndex,
                onSeek =
                    playerViewModel::seekTo,
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
                onToggleShuffle = {
                    playerViewModel.toggleShuffle()
                    overlays.showP(if (!player.isShuffleEnabled) "Shuffle on" else "Shuffle off")
                },
                onToggleRepeat = {
                    playerViewModel.toggleRepeat()
                    val mode = player.repeatMode
                    // after toggle, show next mode
                    val msg = when (mode) {
                        RepeatMode.OFF -> "Repeat: All"
                        RepeatMode.ALL -> "Repeat: One"
                        RepeatMode.ONE -> "Repeat: Off"
                    }
                    overlays.showP(msg)
                },
                playerStyle = player.playerStyle,
                sleepTimerProgress = player.sleepTimerProgress,
                playingSource = player.playingSource,
                modifier =
                    Modifier
                        .fillMaxSize(),
            )
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
    var showCustom by androidx.compose.runtime.remember { mutableStateOf(false) }
    var minText by androidx.compose.runtime.remember { mutableStateOf("") }
    var secText by androidx.compose.runtime.remember { mutableStateOf("") }
    var pauseMusic by androidx.compose.runtime.remember { mutableStateOf(true) }
    var closeApp by androidx.compose.runtime.remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth().imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header row 36dp to align with XvoxB global X (top 14dp)
        Row(modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Sleep Timer", color = colors.primaryText, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }
        androidx.compose.foundation.layout.Spacer(Modifier.size(4.dp))
        Text(
            if (currentMinutes != null) "Active: $currentMinutes min" else "Off",
            color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.fillMaxWidth()
        )
        androidx.compose.foundation.layout.Spacer(Modifier.size(14.dp))
        val options = listOf(10, 20, 30, 60)
        options.forEach { mins ->
            val selected = currentMinutes == mins
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) colors.primaryAccent.copy(alpha = 0.15f) else colors.card.copy(alpha = 0.97f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSetMinutes(mins) }
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("$mins minutes", color = if (selected) colors.primaryAccent else colors.primaryText, fontSize = 14.sp, modifier = Modifier.weight(1f))
                if (selected) Icon(painter = painterResource(R.drawable.ic_xvox_check), contentDescription = null, tint = colors.primaryAccent, modifier = Modifier.size(16.dp))
            }
        }
        // Custom option - header surface near-opaque, lightly transparent
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (showCustom) colors.primaryAccent.copy(alpha = 0.10f) else colors.card.copy(alpha = 0.97f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { showCustom = !showCustom }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Custom", color = colors.primaryText, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Icon(
                painter = painterResource(if (showCustom) R.drawable.ic_xvox_collapse else R.drawable.ic_xvox_add),
                contentDescription = null, tint = colors.secondaryText, modifier = Modifier.size(16.dp)
            )
        }
        if (showCustom) {
            androidx.compose.foundation.layout.Spacer(Modifier.size(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
            ) {
                // Min box - small boxes
                androidx.compose.material3.OutlinedTextField(
                    value = minText,
                    onValueChange = { v -> if (v.length <= 3 && v.all { it.isDigit() }) minText = v },
                    label = { Text("Min", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                androidx.compose.material3.OutlinedTextField(
                    value = secText,
                    onValueChange = { v -> if (v.length <= 2 && v.all { it.isDigit() }) secText = v },
                    label = { Text("Sec", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
            // Exclusive selection: ya toh pause music ya close full app
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
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.RadioButton(selected = pauseMusic, onClick = { pauseMusic = true; closeApp = false })
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
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.RadioButton(selected = closeApp, onClick = { closeApp = true; pauseMusic = false })
                Text("Close full app", color = colors.primaryText, fontSize = 13.sp)
            }
            androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
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
                    .padding(vertical = 14.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                Text("Start custom timer", color = colors.background, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        // Cancel Timer: only when active, red text, lightly transparent background like other options, no heavy solid
        if (currentMinutes != null) {
            androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.card.copy(alpha = 0.97f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onCancel() }
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Cancel Timer", color = androidx.compose.ui.graphics.Color(0xFFDC2626), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            }
        }
        // End directly, no extra blank space behind button - XvoxB rounded border after this
    }
}

@Composable
private fun PlayerStyleSheetContent(
    currentStyle: com.xvox.music.features.player.styles.XvoxPlayerStyle,
    onSelect: (com.xvox.music.features.player.styles.XvoxPlayerStyle) -> Unit
) {
    val colors = XvoxTheme.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Player Style", color = colors.primaryText, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }
        androidx.compose.foundation.layout.Spacer(Modifier.size(4.dp))
        Text("Choose artwork style", color = colors.secondaryText, fontSize = 12.sp)
        androidx.compose.foundation.layout.Spacer(Modifier.size(14.dp))
        val items = listOf(
            Triple(com.xvox.music.features.player.styles.XvoxPlayerStyle.NORMAL, "Normal", "Square + backdrop palette"),
            Triple(com.xvox.music.features.player.styles.XvoxPlayerStyle.FULL_ART, "Full Art", "Fullscreen cover")
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
                if (selected) Icon(painter = painterResource(R.drawable.ic_xvox_check), contentDescription = null, tint = colors.primaryAccent, modifier = Modifier.size(16.dp))
                else Icon(painter = painterResource(R.drawable.ic_xvox_check), contentDescription = null, tint = colors.mutedText.copy(alpha = 0.35f), modifier = Modifier.size(16.dp))
            }
        }
    }
}
