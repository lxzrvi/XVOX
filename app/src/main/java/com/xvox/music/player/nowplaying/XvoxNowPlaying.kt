package com.xvox.music.player.nowplaying

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.core.design.theme.XvoxLogoFont
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.miniplayer.XvoxPlayerTransitionMotion
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.core.ui.overlay.XvoxBox
import com.xvox.music.core.ui.overlay.xvoxBoxScroll
import com.xvox.music.features.home.XvoxSongActions
import com.xvox.music.features.player.styles.XvoxPlayerStyle
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.sections.EqualizerSettingsSection
import com.xvox.music.features.settings.sections.HeadsetSettingsSection
import com.xvox.music.features.settings.sections.LyricsSettingsSection
import com.xvox.music.features.settings.sections.PlaybackSettingsSection
import com.xvox.music.features.settings.sections.ThreeDSoundSettingsSection
import com.xvox.music.player.nowplaying.components.NowPlayingActions
import com.xvox.music.player.nowplaying.components.NowPlayingOptionsBox
import com.xvox.music.player.nowplaying.lyrics.XvoxArtworkLyrics
import com.xvox.music.player.nowplaying.lyrics.XvoxFullscreenLyrics
import com.xvox.music.player.nowplaying.lyrics.XvoxLyricsViewModel
import com.xvox.music.player.playback.RepeatMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private val XvoxSmoothEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

@Composable
fun XvoxNowPlaying(
    song: Song,
    queue: List<Song>,
    currentIndex: Int,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    onClose: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPlayQueueIndex: (Int) -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    displayMode: Int = 0,
    onDisplayModeChange: ((Int) -> Unit)? = null,
    isLiked: Boolean = false,
    onToggleLiked: (() -> Unit)? = null,
    onTimer: (() -> Unit)? = null,
    onQueue: (() -> Unit)? = null,
    onInfo: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onMore: (() -> Unit)? = null,
    onStarPlaylist: (() -> Unit)? = null,
    isShuffleEnabled: Boolean = false,
    repeatMode: RepeatMode = RepeatMode.OFF,
    onToggleShuffle: (() -> Unit)? = null,
    onToggleRepeat: (() -> Unit)? = null,
    playerStyle: XvoxPlayerStyle = XvoxPlayerStyle.NORMAL,
    sleepTimerProgress: Float? = null,
    playingSource: String = "All Songs",
    isInPlaylist: Boolean = false,
    lyricsViewModel: XvoxLyricsViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val colors = XvoxTheme.colors
    val context = LocalContext.current
    val overlays = LocalXvoxOverlayController.current
    val lyricsState by lyricsViewModel.state.collectAsState()
    val settingsState by settingsViewModel.state.collectAsState()
    val paletteState = rememberXvoxNowPlayingPalette(song, queue, currentIndex)
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val screenHeight = with(density) {
        LocalConfiguration.current.screenHeightDp.dp.toPx()
    }

    val isLandscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isCompact = !isLandscape && (settingsState.nowPlayingStyle == "compact" || settingsState.nowPlayingStyle == "immersive")

    var screenY by rememberSaveable { mutableFloatStateOf(screenHeight) }
    var entered by remember { mutableStateOf(false) }
    var internalDisplayMode by rememberSaveable { mutableIntStateOf(0) }
    val currentMode = if (onDisplayModeChange != null) displayMode else internalDisplayMode
    val setMode: (Int) -> Unit = { mode ->
        internalDisplayMode = mode
        onDisplayModeChange?.invoke(mode)
    }

    var activeSettingsBox by remember { mutableStateOf<String?>(null) }
    var dismissing by remember { mutableStateOf(false) }
    var navigationRequest by remember { mutableIntStateOf(0) }
    var motionJob by remember { mutableStateOf<Job?>(null) }

    var headerHeightDp by remember { mutableStateOf(56.dp) }
    var bottomHeightDp by remember { mutableStateOf(if (isCompact) 175.dp else 245.dp) }

    fun animateScreen(target: Float, finished: (() -> Unit)? = null) {
        motionJob?.cancel()
        val start = screenY
        motionJob = scope.launch {
            val animation = Animatable(start)
            animation.animateTo(
                target,
                tween(
                    durationMillis = XvoxPlayerTransitionMotion.Duration,
                    easing = XvoxPlayerTransitionMotion.easing
                )
            ) {
                screenY = value
            }
            screenY = target
            motionJob = null
            finished?.invoke()
        }
    }

    LaunchedEffect(screenHeight) {
        if (!entered && screenHeight > 0f) {
            entered = true
            screenY = screenHeight
            animateScreen(target = 0f)
        }
    }

    fun dismiss() {
        if (dismissing) return
        dismissing = true
        animateScreen(target = screenHeight, finished = onClose)
    }

    fun returnToRest() {
        animateScreen(0f)
    }

    fun requestPrevious() {
        if (repeatMode == RepeatMode.ONE) return
        if (queue.isEmpty() || currentIndex < 0) return
        val atFirst = currentIndex <= 0
        if (atFirst && repeatMode != RepeatMode.ALL) return
        navigationRequest--
    }

    fun requestNext() {
        if (repeatMode == RepeatMode.ONE) return
        if (queue.isEmpty() || currentIndex < 0) return
        val atLast = currentIndex >= queue.lastIndex
        if (atLast && repeatMode != RepeatMode.ALL) return
        navigationRequest++
    }

    LaunchedEffect(song.id) {
        lyricsViewModel.load(song)
    }

    BackHandler {
        when {
            activeSettingsBox != null -> activeSettingsBox = null
            currentMode == 2 -> setMode(1)
            currentMode == 1 -> setMode(0)
            else -> dismiss()
        }
    }

    val isSlidingDown = screenY > 1f
    val slideFraction = (screenY / screenHeight.coerceAtLeast(1f)).coerceIn(0f, 1f)
    val cornerRadiusDp = if (isSlidingDown) (28.dp * slideFraction).coerceIn(0.dp, 28.dp) else 0.dp
    val sheetCorner = RoundedCornerShape(
        topStart = cornerRadiusDp,
        topEnd = cornerRadiusDp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )

    // Morphing animations for smooth style switching (Default <-> Compact)
    val animTopRadius by animateDpAsState(
        targetValue = if (isCompact) 20.dp else 28.dp,
        animationSpec = tween(320, easing = XvoxSmoothEasing),
        label = "animTopRadius"
    )
    val animBottomPadTop by animateDpAsState(
        targetValue = if (isCompact) 6.dp else 12.dp,
        animationSpec = tween(320, easing = XvoxSmoothEasing),
        label = "animBottomPadTop"
    )
    val animBottomPadBottom by animateDpAsState(
        targetValue = if (isCompact) 4.dp else 8.dp,
        animationSpec = tween(320, easing = XvoxSmoothEasing),
        label = "animBottomPadBottom"
    )

    // Synchronized Fullscreen Morphing Progress (0f = card, 1f = fullscreen)
    val isFullscreen = currentMode == 2
    val isLyricsShowing = currentMode >= 1

    val fullscreenProgress by animateFloatAsState(
        targetValue = if (isFullscreen) 1f else 0f,
        animationSpec = tween(240, easing = FastOutSlowInEasing),
        label = "fullscreenProgress"
    )

    val currentPadH = lerp(11.dp, 0.dp, fullscreenProgress)
    val currentCardRadius = lerp(20.dp, 0.dp, fullscreenProgress)
    val currentPadTop = lerp(headerHeightDp + 4.dp, 0.dp, fullscreenProgress)
    val currentPadBottom = lerp(bottomHeightDp + 12.dp, 0.dp, fullscreenProgress)

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { translationY = screenY }
            .clip(sheetCorner)
            .background(paletteState.color)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        motionJob?.cancel()
                        screenY = (screenY + dragAmount).coerceAtLeast(0f)
                    },
                    onDragEnd = {
                        val threshold = screenHeight * 0.18f
                        if (screenY > threshold) dismiss() else returnToRest()
                    },
                    onDragCancel = { returnToRest() }
                )
            }
    ) {
        if (isLandscape) {
            // Responsive 2-Pane Landscape Mode
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Pane: Prominent Artwork Carousel & Song Info
                Box(
                    modifier = Modifier
                        .weight(1.15f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(22.dp))
                        .background(colors.card)
                        .padding(12.dp)
                ) {
                    Crossfade(
                        targetState = isLyricsShowing,
                        animationSpec = tween(220, easing = FastOutSlowInEasing),
                        label = "landscapeCoverLyricsCrossfade"
                    ) { showLyrics ->
                        if (showLyrics) {
                            XvoxArtworkLyrics(
                                state = lyricsState,
                                position = position,
                                onSeek = onSeek,
                                onAttach = { uri -> lyricsViewModel.attachUserLyrics(song, uri) },
                                onDelete = { lyricsViewModel.clearUserLyrics(song) },
                                onClose = { setMode(0) },
                                expanded = true,
                                onToggleExpand = { setMode(if (currentMode == 1) 2 else 1) },
                                onOpenSettings = { activeSettingsBox = "Lyrics" },
                                onDismissNowPlaying = ::dismiss,
                                textColor = Color.White,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .clip(RoundedCornerShape(18.dp))
                                ) {
                                    XvoxNowPlayingArtworkPager(
                                        queue = queue,
                                        currentIndex = currentIndex,
                                        navigationRequest = navigationRequest,
                                        onArtworkTap = { setMode(1) },
                                        onSwipePalette = paletteState::onSwipe,
                                        onSettledPage = { page -> onPlayQueueIndex(page) },
                                        repeatMode = repeatMode,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Spacer(Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(
                                            text = song.title,
                                            color = colors.primaryText,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = song.artist,
                                            color = colors.secondaryText,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    NowPlayingActions(
                                        isLiked = isLiked,
                                        onLike = onToggleLiked ?: {},
                                        onOpenLyrics = { setMode(1) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Right Pane: Top Header Actions + Duration Progress + Controls Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(22.dp))
                        .background(colors.card)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top Header Actions inside Right Pane
                        XvoxNowPlayingHeader(
                            source = playingSource,
                            isInPlaylist = isInPlaylist,
                            onDismiss = ::dismiss,
                            onTimer = onTimer,
                            onQueue = onQueue,
                            onInfo = onInfo,
                            onShare = onShare,
                            onStarPlaylist = onStarPlaylist,
                            onMore = onMore,
                            onOpenOptionsBox = { activeSettingsBox = "SongOptions" },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Duration Seek Progress
                        XvoxNowPlayingProgress(
                            currentSongId = song.id,
                            position = position,
                            duration = duration,
                            onSeek = onSeek,
                            showTime = true
                        )

                        // Controls Cluster
                        XvoxNowPlayingControls(
                            isPlaying = isPlaying,
                            isShuffleEnabled = isShuffleEnabled,
                            repeatMode = repeatMode,
                            onShuffle = { onToggleShuffle?.invoke() },
                            onPrevious = ::requestPrevious,
                            onTogglePlay = onTogglePlay,
                            onNext = ::requestNext,
                            onRepeat = { onToggleRepeat?.invoke() },
                            currentIndex = currentIndex,
                            queueSize = queue.size,
                            positionMs = position,
                            durationMs = duration,
                            onScrubTo = onSeek,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Branding
                        Text(
                            text = "XVOX",
                            color = colors.primaryText.copy(alpha = 0.55f),
                            fontFamily = XvoxLogoFont,
                            fontSize = 11.sp,
                            letterSpacing = 2.sp,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        } else {
            // Portrait Layout
            // Layer 1: Artwork Cover Carousel
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = currentPadH,
                        end = currentPadH,
                        top = currentPadTop,
                        bottom = currentPadBottom
                    )
                    .clip(RoundedCornerShape(currentCardRadius))
            ) {
                XvoxNowPlayingArtworkPager(
                    queue = queue,
                    currentIndex = currentIndex,
                    navigationRequest = navigationRequest,
                    onArtworkTap = { setMode(1) },
                    onSwipePalette = paletteState::onSwipe,
                    onSettledPage = { page -> onPlayQueueIndex(page) },
                    repeatMode = repeatMode,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Layer 2: Synchronized Morphing Lyrics
            if (isLyricsShowing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = currentPadH,
                            end = currentPadH,
                            top = currentPadTop,
                            bottom = currentPadBottom
                        )
                        .clip(RoundedCornerShape(currentCardRadius))
                ) {
                    XvoxArtworkLyrics(
                        state = lyricsState,
                        position = position,
                        onSeek = onSeek,
                        onAttach = { uri -> lyricsViewModel.attachUserLyrics(song, uri) },
                        onDelete = { lyricsViewModel.clearUserLyrics(song) },
                        onClose = { setMode(0) },
                        expanded = isFullscreen,
                        onToggleExpand = { setMode(if (isFullscreen) 1 else 2) },
                        onOpenSettings = { activeSettingsBox = "Lyrics" },
                        onDismissNowPlaying = ::dismiss,
                        onSwipeDownDelta = { delta ->
                            screenY = (screenY + delta).coerceAtLeast(0f)
                        },
                        onSwipeDownEnd = {
                            val threshold = screenHeight * 0.18f
                            if (screenY > threshold) dismiss() else returnToRest()
                        },
                        textColor = Color.White,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Layer 3: Top Navigation Header
            if (!isFullscreen) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .onGloballyPositioned { coordinates ->
                            headerHeightDp = with(density) { coordinates.size.height.toDp() }
                        }
                ) {
                    XvoxNowPlayingHeader(
                        source = playingSource,
                        isInPlaylist = isInPlaylist,
                        onDismiss = ::dismiss,
                        onTimer = onTimer,
                        onQueue = onQueue,
                        onInfo = onInfo,
                        onShare = onShare,
                        onStarPlaylist = onStarPlaylist,
                        onMore = onMore,
                        onOpenOptionsBox = { activeSettingsBox = "SongOptions" },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Layer 4: Morphing Bottom Controls Box
            if (!isFullscreen) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .onGloballyPositioned { coordinates ->
                            bottomHeightDp = with(density) { coordinates.size.height.toDp() }
                        }
                        .clip(
                            RoundedCornerShape(
                                topStart = animTopRadius,
                                topEnd = animTopRadius,
                                bottomStart = 0.dp,
                                bottomEnd = 0.dp
                            )
                        )
                        .background(colors.card)
                        .padding(
                            start = 14.dp,
                            end = 14.dp,
                            top = animBottomPadTop,
                            bottom = animBottomPadBottom
                        )
                ) {
                    if (!isCompact) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                Text(
                                    text = song.title,
                                    color = colors.primaryText,
                                    fontSize = 15.5.sp,
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(Modifier.height(2.dp))

                                Text(
                                    text = song.artist,
                                    color = colors.secondaryText,
                                    fontSize = 12.sp,
                                    lineHeight = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            NowPlayingActions(
                                isLiked = isLiked,
                                onLike = onToggleLiked ?: {},
                                onOpenLyrics = { setMode(1) }
                            )
                        }

                        Spacer(Modifier.height(6.dp))
                    }

                    XvoxNowPlayingProgress(
                        currentSongId = song.id,
                        position = position,
                        duration = duration,
                        onSeek = onSeek,
                        showTime = !isCompact
                    )

                    Spacer(Modifier.height(if (isCompact) 4.dp else 8.dp))

                    XvoxNowPlayingControls(
                        isPlaying = isPlaying,
                        isShuffleEnabled = isShuffleEnabled,
                        repeatMode = repeatMode,
                        onShuffle = { onToggleShuffle?.invoke() },
                        onPrevious = ::requestPrevious,
                        onTogglePlay = onTogglePlay,
                        onNext = ::requestNext,
                        onRepeat = { onToggleRepeat?.invoke() },
                        currentIndex = currentIndex,
                        queueSize = queue.size,
                        positionMs = position,
                        durationMs = duration,
                        onScrubTo = onSeek,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(if (isCompact) 6.dp else 10.dp))

                    Text(
                        text = "XVOX",
                        color = colors.primaryText.copy(alpha = 0.55f),
                        fontFamily = XvoxLogoFont,
                        fontSize = 11.sp,
                        letterSpacing = 2.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Spacer(Modifier.height(if (isCompact) 2.dp else 4.dp))
                }
            }
        }

        // Fullscreen Lyrics Overlay
        AnimatedVisibility(
            visible = currentMode == 2,
            enter = fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)),
            exit = fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing))
        ) {
            XvoxFullscreenLyrics(
                song = song,
                state = lyricsState,
                position = position,
                duration = duration,
                isPlaying = isPlaying,
                backgroundColor = paletteState.color,
                onSeek = onSeek,
                onAttach = { uri -> lyricsViewModel.attachUserLyrics(song, uri) },
                onDelete = { lyricsViewModel.clearUserLyrics(song) },
                onPrevious = ::requestPrevious,
                onTogglePlay = onTogglePlay,
                onNext = ::requestNext,
                onClose = { setMode(1) },
                onOpenSettings = { activeSettingsBox = "Lyrics" }
            )
        }

        // Dedicated contextual Settings / Options Boxes
        when (activeSettingsBox) {
            "SongOptions" -> {
                NowPlayingOptionsBox(
                    song = song,
                    isLiked = isLiked,
                    onToggleLiked = { onToggleLiked?.invoke() },
                    onQueue = { onQueue?.invoke() },
                    onTimer = { onTimer?.invoke() },
                    onOpenBox = { boxName -> activeSettingsBox = boxName },
                    onDismiss = { activeSettingsBox = null }
                )
            }
            "Lyrics" -> {
                XvoxBox(title = "Lyrics settings", onDismiss = { activeSettingsBox = null }) {
                    val scroll = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scroll)
                            .xvoxBoxScroll(scroll)
                    ) {
                        LyricsSettingsSection(settingsState, settingsViewModel)
                    }
                }
            }
            "Equalizer" -> {
                XvoxBox(title = "Equalizer", onDismiss = { activeSettingsBox = null }) {
                    val scroll = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scroll)
                            .xvoxBoxScroll(scroll)
                    ) {
                        EqualizerSettingsSection(settingsState, settingsViewModel)
                    }
                }
            }
            "3D sound" -> {
                XvoxBox(title = "3D sound", onDismiss = { activeSettingsBox = null }) {
                    val scroll = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scroll)
                            .xvoxBoxScroll(scroll)
                    ) {
                        ThreeDSoundSettingsSection(settingsState, settingsViewModel)
                    }
                }
            }
            "Crossfade" -> {
                XvoxBox(title = "Playback & Crossfade", onDismiss = { activeSettingsBox = null }) {
                    val scroll = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scroll)
                            .xvoxBoxScroll(scroll)
                    ) {
                        PlaybackSettingsSection(settingsState, settingsViewModel)
                    }
                }
            }
            "Headset" -> {
                XvoxBox(title = "Headset controls", onDismiss = { activeSettingsBox = null }) {
                    val scroll = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scroll)
                            .xvoxBoxScroll(scroll)
                    ) {
                        HeadsetSettingsSection(settingsState, settingsViewModel)
                    }
                }
            }
        }
    }
}
