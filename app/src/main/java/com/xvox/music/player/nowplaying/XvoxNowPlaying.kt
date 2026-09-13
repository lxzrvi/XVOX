package com.xvox.music.player.nowplaying

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.core.design.theme.XvoxLogoFont
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.miniplayer.XvoxPlayerTransitionMotion
import com.xvox.music.features.home.XvoxSongActions
import com.xvox.music.features.player.styles.XvoxPlayerStyle
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.sections.LyricsSettingsSection
import com.xvox.music.core.ui.overlay.XvoxBox
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

    val isImmersive = settingsState.nowPlayingStyle == "immersive"

    var screenY by rememberSaveable { mutableFloatStateOf(screenHeight) }
    var entered by remember { mutableStateOf(false) }
    var showLyrics by rememberSaveable { mutableStateOf(false) }
    var lyricsExpanded by rememberSaveable { mutableStateOf(false) }
    var showStyleSheet by remember { mutableStateOf(false) }
    var showLyricsSettingsSheet by remember { mutableStateOf(false) }
    var dismissing by remember { mutableStateOf(false) }
    var navigationRequest by remember { mutableIntStateOf(0) }
    var motionJob by remember { mutableStateOf<Job?>(null) }

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

        if (showLyrics) {
            val target = if (atFirst) queue.lastIndex else currentIndex - 1
            onPlayQueueIndex(target)
        } else {
            navigationRequest--
        }
    }

    fun requestNext() {
        if (repeatMode == RepeatMode.ONE) return
        if (queue.isEmpty() || currentIndex < 0) return
        val atLast = currentIndex >= queue.lastIndex
        if (atLast && repeatMode != RepeatMode.ALL) return

        if (showLyrics) {
            val target = if (atLast) 0 else currentIndex + 1
            onPlayQueueIndex(target)
        } else {
            navigationRequest++
        }
    }

    LaunchedEffect(song.id) {
        lyricsViewModel.load(song)
    }

    BackHandler {
        when {
            showLyricsSettingsSheet -> showLyricsSettingsSheet = false
            showStyleSheet -> showStyleSheet = false
            lyricsExpanded -> lyricsExpanded = false
            showLyrics -> showLyrics = false
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

    // Morphing animations for smooth style switching (Default <-> Immersive)
    val animTopRadius by animateDpAsState(
        targetValue = if (isImmersive) 0.dp else 28.dp,
        animationSpec = tween(320, easing = XvoxSmoothEasing),
        label = "animTopRadius"
    )
    val animBottomPadTop by animateDpAsState(
        targetValue = if (isImmersive) 6.dp else 12.dp,
        animationSpec = tween(320, easing = XvoxSmoothEasing),
        label = "animBottomPadTop"
    )
    val animBottomPadBottom by animateDpAsState(
        targetValue = if (isImmersive) 4.dp else 8.dp,
        animationSpec = tween(320, easing = XvoxSmoothEasing),
        label = "animBottomPadBottom"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { translationY = screenY }
            .clip(sheetCorner)
            .background(paletteState.color)
    ) {
        XvoxNowPlayingBackdrop(
            dominant = paletteState.color,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header: hidden smoothly when lyrics are expanded
            AnimatedVisibility(
                visible = !lyricsExpanded,
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(280, easing = XvoxSmoothEasing)
                ) + fadeIn(tween(200)),
                exit = slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(240, easing = XvoxSmoothEasing)
                ) + fadeOut(tween(160))
            ) {
                XvoxNowPlayingHeader(
                    onClose = ::dismiss,
                    onShare = { onShare?.invoke() ?: XvoxSongActions.share(context, song) },
                    onMore = { showStyleSheet = true },
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

            // Middle Container: Cover & Lyrics have exact identical sizing & edge positioning
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(
                        horizontal = if (lyricsExpanded) 0.dp else 4.dp,
                        vertical = if (lyricsExpanded) 0.dp else 2.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(
                    targetState = showLyrics,
                    animationSpec = tween(260, easing = XvoxSmoothEasing),
                    label = "coverLyricsFade"
                ) { isLyricsShowing ->
                    if (isLyricsShowing) {
                        XvoxArtworkLyrics(
                            state = lyricsState,
                            position = position,
                            onSeek = onSeek,
                            onAttach = lyricsViewModel::attach,
                            onDelete = lyricsViewModel::removeCustom,
                            onClose = {
                                lyricsExpanded = false
                                showLyrics = false
                            },
                            expanded = lyricsExpanded,
                            onToggleExpand = { lyricsExpanded = !lyricsExpanded },
                            onOpenSettings = { showLyricsSettingsSheet = true },
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
                                .padding(
                                    horizontal = if (lyricsExpanded) 0.dp else 6.dp,
                                    vertical = if (lyricsExpanded) 0.dp else 4.dp
                                )
                                .clip(if (lyricsExpanded) RoundedCornerShape(0.dp) else RoundedCornerShape(20.dp))
                        )
                    } else {
                        XvoxNowPlayingArtworkPager(
                            queue = queue,
                            currentIndex = currentIndex,
                            navigationRequest = navigationRequest,
                            onArtworkTap = { showLyrics = true },
                            onSwipePalette = { base, adjacent, fraction ->
                                scope.launch { paletteState.blend(base, adjacent, fraction) }
                            },
                            onSettledPage = onPlayQueueIndex,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            repeatMode = repeatMode
                        )
                    }
                }
            }

            // Bottom Controls Area: hidden smoothly when lyrics are expanded
            val bottomBoxShape = RoundedCornerShape(topStart = animTopRadius, topEnd = animTopRadius)

            AnimatedVisibility(
                visible = !lyricsExpanded,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(280, easing = XvoxSmoothEasing)
                ) + fadeIn(tween(200)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(240, easing = XvoxSmoothEasing)
                ) + fadeOut(tween(160))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
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
                        visible = !isImmersive,
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
                                lyricsOn = showLyrics,
                                onToggleEqualizer = { settingsViewModel.setEqualizerEnabled(!settingsState.equalizerEnabled) },
                                onToggleSpace = { settingsViewModel.setStereoWidening(!settingsState.stereoWidening) },
                                onToggleLyrics = { showLyrics = !showLyrics },
                                onOpenOptions = { showStyleSheet = true }
                            )

                            Spacer(Modifier.height(14.dp))
                        }
                    }

                    Text(
                        text = song.title,
                        color = colors.primaryText,
                        fontSize = if (isImmersive) 17.sp else 20.sp,
                        lineHeight = if (isImmersive) 21.sp else 24.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = song.artist,
                        color = colors.secondaryText,
                        fontSize = if (isImmersive) 11.sp else 13.sp,
                        lineHeight = if (isImmersive) 15.sp else 17.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(if (isImmersive) 8.dp else 12.dp))

                    XvoxNowPlayingProgress(
                        currentSongId = song.id,
                        position = position,
                        duration = duration,
                        onSeek = onSeek,
                        showTime = !isImmersive
                    )

                    Spacer(Modifier.height(if (isImmersive) 4.dp else 8.dp))

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

                    Spacer(Modifier.height(if (isImmersive) 6.dp else 10.dp))

                    Text(
                        text = "XVOX",
                        color = colors.primaryText.copy(alpha = 0.55f),
                        fontFamily = XvoxLogoFont,
                        fontSize = 11.sp,
                        letterSpacing = 2.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Spacer(Modifier.height(if (isImmersive) 2.dp else 4.dp))
                }
            }
        }

        if (showStyleSheet) {
            NowPlayingOptionsBox(
                onDismiss = { showStyleSheet = false },
                settingsViewModel = settingsViewModel
            )
        }

        if (showLyricsSettingsSheet) {
            XvoxBox(
                onDismiss = { showLyricsSettingsSheet = false },
                title = "Lyrics Settings"
            ) {
                LyricsSettingsSection(
                    state = settingsState,
                    viewModel = settingsViewModel
                )
            }
        }
    }
}
