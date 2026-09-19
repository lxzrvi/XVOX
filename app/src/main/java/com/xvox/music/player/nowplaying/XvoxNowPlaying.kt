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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
    val lyricsState by lyricsViewModel.state.collectAsState()
    val settingsState by settingsViewModel.state.collectAsState()
    val paletteState = rememberXvoxNowPlayingPalette(song, queue, currentIndex)
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val screenHeight = with(density) {
        LocalConfiguration.current.screenHeightDp.dp.toPx()
    }

    val isLandscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isCompact = (settingsState.nowPlayingStyle == "compact" || settingsState.nowPlayingStyle == "immersive") && !isLandscape

    var screenY by rememberSaveable { mutableFloatStateOf(screenHeight) }
    var entered by rememberSaveable { mutableStateOf(false) }
    var internalDisplayMode by rememberSaveable { mutableIntStateOf(0) }
    val currentMode = if (onDisplayModeChange != null) displayMode else internalDisplayMode
    val isLyricsShowing = currentMode >= 1
    val isFullscreen = currentMode == 2
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
        if (isLyricsShowing) {
            onPrevious()
        } else {
            navigationRequest--
        }
    }

    fun requestNext() {
        if (repeatMode == RepeatMode.ONE) return
        if (queue.isEmpty() || currentIndex < 0) return
        val atLast = currentIndex >= queue.lastIndex
        if (atLast && repeatMode != RepeatMode.ALL) return
        if (isLyricsShowing) {
            onNext()
        } else {
            navigationRequest++
        }
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
    val fullscreenProgress by animateFloatAsState(
        targetValue = if (isFullscreen) 1f else 0f,
        animationSpec = tween(240, easing = FastOutSlowInEasing),
        label = "fullscreenProgress"
    )

    val currentPadH = lerp(6.dp, 0.dp, fullscreenProgress)
    val currentCardRadius = lerp(20.dp, 0.dp, fullscreenProgress)
    val currentPadTop = lerp(headerHeightDp + 2.dp, 0.dp, fullscreenProgress)
    val currentPadBottom = lerp(bottomHeightDp + 6.dp, 0.dp, fullscreenProgress)

    val view = androidx.compose.ui.platform.LocalView.current
    DisposableEffect(isLandscape, isFullscreen) {
        val window = (view.context as? android.app.Activity)?.window
        val insetsController = window?.let { androidx.core.view.WindowCompat.getInsetsController(it, view) }
        if (isLandscape || isFullscreen) {
            insetsController?.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController?.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
        } else {
            insetsController?.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
        }
        onDispose {
            if (isLandscape || isFullscreen) {
                insetsController?.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { translationY = screenY }
            .clip(sheetCorner)
            .background(paletteState.color)
            .pointerInput(Unit) {
                // Consume clicks on backdrop to prevent click-through to home screen below
                detectTapGestures { }
            }
    ) {
        XvoxNowPlayingBackdrop(
            dominant = paletteState.color,
            modifier = Modifier.fillMaxSize()
        )

        if (isLandscape) {
            if (isFullscreen) {
                // Fullscreen lyrics overlay across the entire landscape screen
                XvoxArtworkLyrics(
                    state = lyricsState,
                    position = position,
                    onSeek = onSeek,
                    onAttach = lyricsViewModel::attach,
                    onDelete = lyricsViewModel::removeCustom,
                    onClose = { setMode(0) },
                    expanded = true,
                    onToggleExpand = { setMode(1) },
                    onOpenSettings = { activeSettingsBox = "Lyrics" },
                    onDismissNowPlaying = ::dismiss,
                    onSwipeDownDelta = { delta: Float ->
                        screenY = (screenY + delta).coerceAtLeast(0f)
                    },
                    onSwipeDownEnd = {
                        if (screenY > screenHeight * 0.18f) dismiss() else returnToRest()
                    },
                    textColor = paletteState.color,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Landscape 2-Pane Mode: Left (0.65f Artwork/Lyrics) & Right (0.35f Controls Card)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 0.dp, top = 10.dp, end = 12.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Artwork or Lyrics Card (65% width)
                    Box(
                        modifier = Modifier
                            .weight(0.65f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Crossfade(
                            targetState = isLyricsShowing,
                            animationSpec = tween(220, easing = FastOutSlowInEasing),
                            label = "coverLyricsFadeLandscape"
                        ) { lyricsActive ->
                            if (lyricsActive) {
                                XvoxArtworkLyrics(
                                    state = lyricsState,
                                    position = position,
                                    onSeek = onSeek,
                                    onAttach = lyricsViewModel::attach,
                                    onDelete = lyricsViewModel::removeCustom,
                                    onClose = { setMode(0) },
                                    expanded = false,
                                    showCloseButton = true,
                                    onToggleExpand = { setMode(2) },
                                    onOpenSettings = { activeSettingsBox = "Lyrics" },
                                    onDismissNowPlaying = ::dismiss,
                                    textColor = paletteState.color,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(start = 12.dp, end = 6.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                )
                            } else {
                                XvoxNowPlayingArtworkPager(
                                    queue = queue,
                                    currentIndex = currentIndex,
                                    navigationRequest = navigationRequest,
                                    onArtworkTap = { setMode(1) },
                                    onSwipePalette = { base, adjacent, fraction ->
                                        paletteState.blend(base, adjacent, fraction)
                                    },
                                    onSettledPage = onPlayQueueIndex,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(start = 12.dp, end = 6.dp),
                                    pageSpacing = 12.dp,
                                    repeatMode = repeatMode
                                )
                            }
                        }
                    }

                    // Right: Option/Control Card (35% width)
                    val landscapeScroll = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .weight(0.35f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(18.dp))
                            .background(colors.background.copy(alpha = 0.35f))
                            .verticalScroll(landscapeScroll)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top header row inside the right box (useSystemInsets = false)
                        XvoxNowPlayingHeader(
                            onClose = { if (isLyricsShowing) setMode(0) else dismiss() },
                            onShare = { onShare?.invoke() ?: XvoxSongActions.share(context, song) },
                            onMore = { activeSettingsBox = "Style" },
                            playingSource = playingSource,
                            useSystemInsets = false,
                            modifier = Modifier.pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onVerticalDrag = { _, dragAmount ->
                                        screenY = (screenY + dragAmount).coerceAtLeast(0f)
                                    },
                                    onDragEnd = {
                                        if (screenY > screenHeight * 0.18f) {
                                            dismiss()
                                        } else {
                                            returnToRest()
                                        }
                                    },
                                    onDragCancel = { returnToRest() }
                                )
                            }
                        )

                        Spacer(Modifier.height(2.dp))

                        NowPlayingActions(
                            isLiked = isLiked,
                            isInPlaylist = isInPlaylist,
                            onTimer = { onTimer?.invoke() },
                            onQueue = { onQueue?.invoke() },
                            onInfo = { onInfo?.invoke() },
                            onToggleLiked = { onToggleLiked?.invoke() },
                            onStarPlaylist = { onStarPlaylist?.invoke() },
                            timerProgress = sleepTimerProgress,
                            crossfadeOn = settingsState.crossfade,
                            onToggleCrossfade = { settingsViewModel.setCrossfade(!settingsState.crossfade) },
                            equalizerOn = settingsState.equalizerEnabled,
                            spaceOn = settingsState.stereoWidening,
                            lyricsOn = isLyricsShowing,
                            onToggleEqualizer = { settingsViewModel.setEqualizerEnabled(!settingsState.equalizerEnabled) },
                            onToggleSpace = { settingsViewModel.setStereoWidening(!settingsState.stereoWidening) },
                            onToggleLyrics = { setMode(if (isLyricsShowing) 0 else 1) },
                            onOpenOptions = { optionName -> activeSettingsBox = optionName }
                        )

                        Spacer(Modifier.height(2.dp))

                        Column {
                            Text(
                                text = song.title,
                                color = colors.primaryText,
                                fontSize = 15.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = song.artist,
                                color = colors.secondaryText,
                                fontSize = 11.5.sp,
                                lineHeight = 14.sp,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(Modifier.height(2.dp))

                        XvoxNowPlayingProgress(
                            currentSongId = song.id,
                            position = position,
                            duration = duration,
                            onSeek = onSeek,
                            showTime = true
                        )

                        Spacer(Modifier.height(2.dp))

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

                        Text(
                            text = "XVOX",
                            color = colors.primaryAccent.copy(alpha = 0.50f),
                            fontFamily = XvoxLogoFont,
                            fontSize = 11.5.sp,
                            letterSpacing = 2.sp,
                            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp, bottom = 2.dp)
                        )
                    }
                }
            }
        } else {
            // Portrait Mode
            // Middle Container: Cover & Lyrics
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = currentPadTop,
                        bottom = currentPadBottom
                    )
                    .heightIn(min = 200.dp),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(
                    targetState = isLyricsShowing,
                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                    label = "coverLyricsFade"
                ) { lyricsActive ->
                    if (lyricsActive) {
                        XvoxArtworkLyrics(
                            state = lyricsState,
                            position = position,
                            onSeek = onSeek,
                            onAttach = lyricsViewModel::attach,
                            onDelete = lyricsViewModel::removeCustom,
                            onClose = { setMode(0) },
                            expanded = isFullscreen,
                            onToggleExpand = { setMode(if (isFullscreen) 1 else 2) },
                            onOpenSettings = { activeSettingsBox = "Lyrics" },
                            onDismissNowPlaying = ::dismiss,
                            onSwipeDownDelta = { delta ->
                                screenY = (screenY + delta).coerceAtLeast(0f)
                            },
                            onSwipeDownEnd = {
                                if (screenY > screenHeight * 0.18f) {
                                    dismiss()
                                } else {
                                    returnToRest()
                                }
                            },
                            textColor = paletteState.color,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = currentPadH)
                                .clip(RoundedCornerShape(currentCardRadius))
                        )
                    } else {
                        XvoxNowPlayingArtworkPager(
                            queue = queue,
                            currentIndex = currentIndex,
                            navigationRequest = navigationRequest,
                            onArtworkTap = { setMode(1) },
                            onSwipePalette = { base, adjacent, fraction ->
                                paletteState.blend(base, adjacent, fraction)
                            },
                            onSettledPage = onPlayQueueIndex,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = currentPadH),
                            pageSpacing = 12.dp,
                            repeatMode = repeatMode
                        )
                    }
                }
            }

            // Header: smooth slide up & fade out during fullscreen opening
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .onGloballyPositioned { headerHeightDp = with(density) { it.size.height.toDp() } }
                    .graphicsLayer {
                        translationY = -fullscreenProgress * 120.dp.toPx()
                        alpha = (1f - fullscreenProgress * 1.5f).coerceIn(0f, 1f)
                    }
            ) {
                XvoxNowPlayingHeader(
                    onClose = ::dismiss,
                    onShare = { onShare?.invoke() ?: XvoxSongActions.share(context, song) },
                    onMore = { activeSettingsBox = "Style" },
                    playingSource = playingSource,
                    modifier = Modifier.pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { _, dragAmount ->
                                screenY = (screenY + dragAmount).coerceAtLeast(0f)
                            },
                            onDragEnd = {
                                if (screenY > screenHeight * 0.18f) {
                                    dismiss()
                                } else {
                                    returnToRest()
                                }
                            },
                            onDragCancel = { returnToRest() }
                        )
                    }
                )
            }

            // Bottom Controls Area: smooth slide down & fade out during fullscreen opening
            val bottomBoxShape = RoundedCornerShape(topStart = animTopRadius, topEnd = animTopRadius)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .onGloballyPositioned { bottomHeightDp = with(density) { it.size.height.toDp() } }
                    .graphicsLayer {
                        translationY = fullscreenProgress * 300.dp.toPx()
                        alpha = (1f - fullscreenProgress * 1.5f).coerceIn(0f, 1f)
                    }
                    .clip(bottomBoxShape)
                    .background(colors.background.copy(alpha = 0.35f))
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(
                        start = 14.dp,
                        top = animBottomPadTop,
                        end = 14.dp,
                        bottom = animBottomPadBottom
                    )
            ) {
                AnimatedVisibility(
                    visible = !isCompact,
                    enter = expandVertically(tween(280, easing = XvoxSmoothEasing)) + fadeIn(tween(200)),
                    exit = shrinkVertically(tween(240, easing = XvoxSmoothEasing)) + fadeOut(tween(160))
                ) {
                    Column {
                        NowPlayingActions(
                            isLiked = isLiked,
                            isInPlaylist = isInPlaylist,
                            onTimer = { onTimer?.invoke() },
                            onQueue = { onQueue?.invoke() },
                            onInfo = { onInfo?.invoke() },
                            onToggleLiked = { onToggleLiked?.invoke() },
                            onStarPlaylist = { onStarPlaylist?.invoke() },
                            timerProgress = sleepTimerProgress,
                            crossfadeOn = settingsState.crossfade,
                            onToggleCrossfade = { settingsViewModel.setCrossfade(!settingsState.crossfade) },
                            equalizerOn = settingsState.equalizerEnabled,
                            spaceOn = settingsState.stereoWidening,
                            lyricsOn = isLyricsShowing,
                            onToggleEqualizer = { settingsViewModel.setEqualizerEnabled(!settingsState.equalizerEnabled) },
                            onToggleSpace = { settingsViewModel.setStereoWidening(!settingsState.stereoWidening) },
                            onToggleLyrics = { setMode(if (isLyricsShowing) 0 else 1) },
                            onOpenOptions = { optionName -> activeSettingsBox = optionName }
                        )

                        Spacer(Modifier.height(14.dp))
                    }
                }

                Text(
                    text = song.title,
                    color = colors.primaryText,
                    fontSize = if (isCompact) 17.sp else 20.sp,
                    lineHeight = if (isCompact) 21.sp else 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = song.artist,
                    color = colors.secondaryText,
                    fontSize = if (isCompact) 11.sp else 13.sp,
                    lineHeight = if (isCompact) 15.sp else 17.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(if (isCompact) 8.dp else 12.dp))

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

        // Dedicated contextual Settings / Options Boxes on Long-Press of bottom buttons
        if (activeSettingsBox != null) {
            if (activeSettingsBox == "Style") {
                NowPlayingOptionsBox(
                    onDismiss = { activeSettingsBox = null },
                    settingsViewModel = settingsViewModel
                )
            } else {
                val boxTitle = when (activeSettingsBox) {
                    "Equalizer" -> "Equalizer"
                    "3D sound" -> "3D Sound"
                    "Crossfade" -> "Playback & Crossfade"
                    "Bluetooth" -> "Audio Output"
                    "Lyrics" -> "Lyrics Settings"
                    else -> activeSettingsBox!!
                }

                XvoxBox(
                    onDismiss = { activeSettingsBox = null },
                    title = boxTitle
                ) {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                            .xvoxBoxScroll(scrollState)
                    ) {
                        when (activeSettingsBox) {
                            "Equalizer" -> EqualizerSettingsSection(state = settingsState, viewModel = settingsViewModel)
                            "3D sound" -> ThreeDSoundSettingsSection(state = settingsState, viewModel = settingsViewModel)
                            "Crossfade" -> PlaybackSettingsSection(state = settingsState, viewModel = settingsViewModel)
                            "Bluetooth" -> HeadsetSettingsSection(state = settingsState, viewModel = settingsViewModel)
                            "Lyrics" -> LyricsSettingsSection(state = settingsState, viewModel = settingsViewModel)
                        }
                    }
                }
            }
        }
    }
}
