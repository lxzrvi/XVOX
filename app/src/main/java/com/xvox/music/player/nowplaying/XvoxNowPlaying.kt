package com.xvox.music.player.nowplaying

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.core.design.theme.XvoxLogoFont
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.miniplayer.XvoxPlayerTransitionMotion
import com.xvox.music.core.ui.overlay.XvoxBox
import com.xvox.music.core.ui.overlay.XvoxBoxPresentation
import com.xvox.music.core.ui.overlay.xvoxBoxScroll
import com.xvox.music.features.home.XvoxSongActions
import com.xvox.music.features.player.styles.XvoxPlayerStyle
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.sections.EqualizerFooterActions
import com.xvox.music.features.settings.sections.EqualizerSettingsSection
import com.xvox.music.features.settings.components.XvoxTransactionalFooterActions
import com.xvox.music.features.settings.sections.HeadsetSettingsDraftSection
import com.xvox.music.features.settings.sections.LyricsSettingsDraftSection
import com.xvox.music.features.settings.sections.PlaybackSettingsDraftSection
import com.xvox.music.features.settings.sections.ThreeDSoundDraftSection
import com.xvox.music.player.nowplaying.components.NowPlayingActions
import com.xvox.music.player.nowplaying.components.NowPlayingOptionsBox
import com.xvox.music.player.nowplaying.lyrics.XvoxArtworkLyrics
import com.xvox.music.player.nowplaying.lyrics.XvoxLyricsViewModel
import com.xvox.music.player.playback.RepeatMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val XvoxSmoothEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/** Cover-matched lyric colour with real tonal separation, without adding a text glow/shadow. */
private fun Color.xvoxReadableCoverLyricColor(): Color {
    val pole = if (luminance() > .48f) Color.Black else Color.White
    val pull = .64f
    return Color(
        red = red * (1f - pull) + pole.red * pull,
        green = green * (1f - pull) + pole.green * pull,
        blue = blue * (1f - pull) + pole.blue * pull,
        alpha = 1f
    )
}

@Composable
fun XvoxNowPlaying(
    song: Song,
    queue: List<Song>,
    currentIndex: Int,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    onClose: () -> Unit,
    /** Begins dismissal housekeeping; the Mini Player is restored only after this surface exits. */
    onDismissStart: () -> Unit = {},
    onTogglePlay: () -> Unit,
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
    /** Durable selection for the swipable right-side action cluster. */
    actionPageIndex: Int = 0,
    onActionPageChange: (Int) -> Unit = {},
    lyricsViewModel: XvoxLyricsViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val colors = XvoxTheme.colors
    val context = LocalContext.current
    val lyricsState by lyricsViewModel.state.collectAsState()
    val settingsState by settingsViewModel.state.collectAsState()
    val paletteState = rememberXvoxNowPlayingPalette(song, queue, currentIndex)
    // This is intentionally scoped to lyric text. All visible Now Playing chrome keeps its
    // theme-derived colors; cover analysis is not allowed to recolour controls or metadata.
    val lyricsTextColor = when (settingsState.lyrics.textColorMode) {
        "black" -> Color.Black
        "white" -> Color.White
        // Preserve the cover hue in lyric-only matching mode, but pull it toward a contrast pole
        // so glyphs stay visibly distinct from the adaptive cover backdrop without a glow.
        else -> paletteState.color.xvoxReadableCoverLyricColor()
    }
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

    var activeSettingsBox by rememberSaveable { mutableStateOf<String?>(null) }
    // Ordinary setting sheets are live: this local value only supplies immediate UI state while
    // the persisted Settings flow catches up. Closing, Cancel, and Okay never roll back a change.
    var optionDraft by remember { mutableStateOf(settingsState) }
    fun openSettingsBox(name: String) {
        optionDraft = settingsState
        activeSettingsBox = name
    }
    fun applyCrossfadeDraft(draft: com.xvox.music.features.settings.SettingsState = optionDraft) {
        settingsViewModel.setCrossfade(draft.crossfade)
        settingsViewModel.setGapless(draft.gapless)
        settingsViewModel.setCrossfadeDuration(draft.crossfadeDuration)
        settingsViewModel.setCrossfadeSmart(draft.crossfadeSmart)
        settingsViewModel.setCrossfadeClashControl(draft.crossfadeClashControl)
        settingsViewModel.setCrossfadeBeatSync(draft.crossfadeBeatSync)
    }
    fun applyThreeDSoundDraft(draft: com.xvox.music.features.settings.SettingsState = optionDraft) {
        settingsViewModel.setStereoWidening(draft.stereoWidening)
        settingsViewModel.setSurroundWidth(draft.surroundWidth)
        settingsViewModel.setSurroundDepth(draft.surroundDepth)
        settingsViewModel.setSurroundPanSpeed(draft.surroundPanSpeed)
        settingsViewModel.setHrtf(draft.hrtf)
        settingsViewModel.setBalance(draft.balance)
    }
    fun applyBluetoothDraft(draft: com.xvox.music.features.settings.SettingsState = optionDraft) {
        settingsViewModel.setAudioOutputRoute(draft.audioOutputRoute)
        settingsViewModel.setBtConnectAction(draft.btConnectAction)
        settingsViewModel.setPlayOnHeadsetConnect(draft.playOnHeadsetConnect)
        settingsViewModel.setBtDisconnectAction(draft.btDisconnectAction)
        settingsViewModel.setPauseOnHeadphoneDisconnect(draft.pauseOnHeadphoneDisconnect)
    }
    fun applyLyricsDraft(draft: com.xvox.music.features.settings.SettingsState = optionDraft) {
        settingsViewModel.updateLyrics { draft.lyrics }
    }
    var dismissing by remember { mutableStateOf(false) }
    var navigationRequest by remember { mutableIntStateOf(0) }
    // Separate visual browsing from the audio deck. Covers may move immediately while the
    // audible item stays untouched until the navigation button is released. Queue indices are
    // occurrence identities here: two copies of one Song.id must remain independently swipeable.
    var previewIndex by rememberSaveable { mutableIntStateOf(currentIndex.coerceIn(0, queue.lastIndex.coerceAtLeast(0))) }
    var previewGestureActive by remember { mutableStateOf(false) }
    var previewCommitJob by remember { mutableStateOf<Job?>(null) }
    var previewCommitVersion by remember { mutableIntStateOf(0) }
    var holdPagerPaletteDuringShuffle by remember { mutableStateOf(false) }
    var motionJob by remember { mutableStateOf<Job?>(null) }
    val latestQueue by rememberUpdatedState(queue)
    val latestCurrentIndex by rememberUpdatedState(currentIndex)

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
        previewCommitVersion++
        previewCommitJob?.cancel()
        previewCommitJob = null
        onDismissStart()
        animateScreen(target = screenHeight) {
            // Match the opening handoff in reverse: the full player is entirely below the
            // viewport, then the Mini Player receives a clean 50ms visible-free beat to rise.
            scope.launch {
                delay(XvoxPlayerTransitionMotion.HandoffDelay)
                onClose()
            }
        }
    }

    fun returnToRest() {
        animateScreen(0f)
    }

    fun cancelPendingPreviewCommit() {
        previewCommitVersion++
        previewCommitJob?.cancel()
        previewCommitJob = null
    }

    fun setPreviewTarget(index: Int, sourceQueue: List<Song> = queue): Boolean {
        if (index !in sourceQueue.indices) return false
        previewIndex = index
        return true
    }

    LaunchedEffect(currentIndex, queue) {
        // External playback changes win over an old release request. A held preview retains its
        // actual occurrence index; outside a gesture the audible occurrence is authoritative.
        if (!previewGestureActive) cancelPendingPreviewCommit()
        when {
            previewGestureActive && previewIndex in queue.indices -> Unit
            currentIndex in queue.indices -> previewIndex = currentIndex
            queue.isNotEmpty() -> previewIndex = 0
        }
    }

    // Shuffle must keep the current song's already-selected palette pinned while the pager
    // remaps pages to the new queue order. Release the hold only after that short reflow settles.
    LaunchedEffect(holdPagerPaletteDuringShuffle, queue, currentIndex, song.id) {
        if (holdPagerPaletteDuringShuffle) {
            paletteState.pin(song)
            delay(XvoxPlayerTransitionMotion.Duration.toLong() + 40L)
            holdPagerPaletteDuringShuffle = false
        }
    }

    fun movePreview(direction: Int): Boolean {
        if (queue.isEmpty() || repeatMode == RepeatMode.ONE) return false
        cancelPendingPreviewCommit()
        val from = previewIndex.takeIf { it in queue.indices }
            ?: currentIndex.takeIf { it in queue.indices }
            ?: return false
        val target = when {
            direction > 0 && from < queue.lastIndex -> from + 1
            direction < 0 && from > 0 -> from - 1
            direction > 0 && repeatMode == RepeatMode.ALL && queue.size > 1 -> 0
            direction < 0 && repeatMode == RepeatMode.ALL && queue.size > 1 -> queue.lastIndex
            else -> return false
        }
        previewGestureActive = true
        setPreviewTarget(target)
        navigationRequest += direction.coerceIn(-1, 1)
        return true
    }

    fun commitPreview() {
        val targetOccurrence = previewIndex
        cancelPendingPreviewCommit()
        previewGestureActive = false
        if (targetOccurrence !in latestQueue.indices || targetOccurrence == latestCurrentIndex) return

        val requestVersion = previewCommitVersion
        previewCommitJob = scope.launch {
            // Keep audio on the current song until the released cover has rested in place.
            delay(300)
            if (
                requestVersion == previewCommitVersion &&
                !previewGestureActive &&
                previewIndex == targetOccurrence &&
                targetOccurrence in latestQueue.indices &&
                targetOccurrence != latestCurrentIndex
            ) {
                onPlayQueueIndex(targetOccurrence)
            }
            if (requestVersion == previewCommitVersion) previewCommitJob = null
        }
    }

    /** The pager has already waited its 300 ms release window; commit that exact occurrence. */
    fun commitSettledPreview(index: Int, settledSong: Song) {
        cancelPendingPreviewCommit()
        previewGestureActive = false
        if (index !in latestQueue.indices) return
        // The song check protects against a queue replacement that happened during the release.
        if (latestQueue[index].id != settledSong.id) return
        previewIndex = index
        if (index != latestCurrentIndex) onPlayQueueIndex(index)
    }

    fun cancelPreview() {
        cancelPendingPreviewCommit()
        previewGestureActive = false
        if (latestCurrentIndex in latestQueue.indices) previewIndex = latestCurrentIndex
    }

    LaunchedEffect(song.id) {
        lyricsViewModel.load(song)
    }

    BackHandler {
        when {
            activeSettingsBox != null -> {
                // Ordinary sheet controls are already applied live; Back simply closes.
                activeSettingsBox = null
            }
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
        animationSpec = tween(340, easing = XvoxPlayerTransitionMotion.easing),
        label = "fullscreenProgress"
    )

    val currentPadH = lerp(6.dp, 0.dp, fullscreenProgress)
    val currentCardRadius = lerp(20.dp, 0.dp, fullscreenProgress)
    val currentPadTop = lerp(headerHeightDp + 2.dp, 0.dp, fullscreenProgress)
    // The artwork keeps a small relationship gap above the controls. The control card itself is
    // edge-flush; during fullscreen this final bottom reservation collapses on the same clock.
    val currentPadBottom = lerp(bottomHeightDp + 6.dp, 0.dp, fullscreenProgress)

    val view = androidx.compose.ui.platform.LocalView.current
    // Portrait status chrome follows the fullscreen lyrics morph rather than disappearing for
    // ordinary lyrics-sheet mode. Landscape remains edge-to-edge while this player is mounted.
    DisposableEffect(isLandscape, isFullscreen) {
        val window = (view.context as? android.app.Activity)?.window
        val insetsController = window?.let { androidx.core.view.WindowCompat.getInsetsController(it, view) }
        val hideStatus = isLandscape || isFullscreen
        if (hideStatus) {
            insetsController?.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController?.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
        } else {
            insetsController?.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            insetsController?.isAppearanceLightStatusBars = false
        }
        onDispose {
            // Returning from fullscreen or leaving Now Playing restores portrait status chrome in
            // the same lifecycle turn as the geometry's reverse animation.
            if (hideStatus) insetsController?.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { translationY = screenY }
            .clip(sheetCorner)
            .background(colors.background)
            .pointerInput(Unit) {
                // Consume clicks on backdrop to prevent click-through to home screen below
                detectTapGestures { }
            }
    ) {
        XvoxNowPlayingBackdrop(
            dominant = paletteState.color,
            style = settingsState.nowPlayingBackgroundStyle,
            isCoverTransitionInProgress = paletteState.isCoverTransitionInProgress,
            modifier = Modifier.fillMaxSize()
        )

        if (isLandscape) {
            // One persistent lyrics/artwork surface owns both the compact left card and the
            // fullscreen stage. Its bounds interpolate from the originating card to the screen,
            // while the adjacent controls are physically pushed right instead of being swapped.
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val compactInset = 10.dp
                val compactGutter = 10.dp
                val compactArtworkWidth = ((maxWidth - compactInset * 2 - compactGutter) * .60f)
                    .coerceAtLeast(0.dp)
                // The control card is deliberately edge-flush. Reserve only the cover's leading
                // inset plus one explicit gutter so its left edge never collides with the pager.
                val compactControlsWidth = (maxWidth - compactInset - compactGutter - compactArtworkWidth)
                    .coerceAtLeast(0.dp)
                val frameInset = lerp(compactInset, 0.dp, fullscreenProgress)
                val artworkWidth = lerp(compactArtworkWidth, maxWidth, fullscreenProgress)
                val artworkRadius = lerp(20.dp, 0.dp, fullscreenProgress)
                // Travel past the right edge rather than merely fading in place during lyric
                // fullscreen, so the adjacent card is visibly pushed out by the expanding pager.
                // Match the pager's right-edge travel so the explicit compact gutter stays
                // visually consistent until the card has cleared the screen.
                val controlsSlidePx = with(density) {
                    (maxWidth - compactArtworkWidth - compactInset).coerceAtLeast(0.dp).toPx()
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = frameInset, top = frameInset, bottom = frameInset)
                        .width(artworkWidth)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(artworkRadius))
                        // Once the expanding surface begins covering the second pane it receives
                        // touch priority, including the fullscreen pull-down band.
                        .zIndex(if (isFullscreen) 1f else 0f),
                    contentAlignment = Alignment.Center
                ) {
                    Crossfade(
                        targetState = isLyricsShowing,
                        animationSpec = tween(280, easing = XvoxPlayerTransitionMotion.easing),
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
                                expanded = isFullscreen,
                                showCloseButton = !isFullscreen,
                                onToggleExpand = { setMode(if (isFullscreen) 1 else 2) },
                                onOpenSettings = { openSettingsBox("Lyrics") },
                                onDismissNowPlaying = ::dismiss,
                                onSwipeDownDelta = { delta: Float ->
                                    screenY = (screenY + delta).coerceAtLeast(0f)
                                },
                                onSwipeDownEnd = {
                                    if (screenY > screenHeight * 0.18f) dismiss() else returnToRest()
                                },
                                backgroundColor = paletteState.color,
                                textColor = lyricsTextColor,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            XvoxNowPlayingArtworkPager(
                                queue = queue,
                                currentIndex = currentIndex,
                                navigationRequest = navigationRequest,
                                previewIndex = previewIndex,
                                forceCurrentIndex = holdPagerPaletteDuringShuffle,
                                onPreviewIndexChange = { setPreviewTarget(it) },
                                onArtworkTap = { setMode(1) },
                                onSwipePalette = { base, adjacent, fraction ->
                                    if (!holdPagerPaletteDuringShuffle) {
                                        paletteState.blend(base, adjacent, fraction)
                                    }
                                },
                                onSettledPage = { settledIndex, settledSong -> commitSettledPreview(settledIndex, settledSong) },
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(0.dp),
                                pageSpacing = 0.dp,
                                artworkCornerRadius = 0.dp,
                                verticalPaging = true,
                                repeatMode = repeatMode
                            )
                        }
                    }
                }

                // Right: the control card stays mounted throughout the morph, preserving state
                // and giving the reverse animation the same smooth, no-cut path.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .width(compactControlsWidth)
                        .fillMaxHeight()
                        .graphicsLayer {
                            translationX = fullscreenProgress * controlsSlidePx
                            alpha = 1f - fullscreenProgress
                        }
                ) {
                    // Right: Option/Control Card (40% width), intentionally borderless.
                    val landscapeScroll = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp, topEnd = 0.dp, bottomEnd = 0.dp))
                            .background(colors.background.copy(alpha = 0.35f))
                            .verticalScroll(landscapeScroll)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top header row inside the right box (useSystemInsets = false)
                        XvoxNowPlayingHeader(
                            onClose = { if (isLyricsShowing) setMode(0) else dismiss() },
                            onShare = { onShare?.invoke() ?: XvoxSongActions.share(context, song) },
                            onMore = { openSettingsBox("Style") },
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
                            onOpenOptions = { optionName -> openSettingsBox(optionName) },
                            actionPageIndex = actionPageIndex,
                            onActionPageChange = onActionPageChange
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
                            onShuffle = {
                                cancelPreview()
                                holdPagerPaletteDuringShuffle = true
                                paletteState.pin(song)
                                onToggleShuffle?.invoke()
                            },
                            onPreviewPrevious = { movePreview(-1) },
                            onTogglePlay = onTogglePlay,
                            onPreviewNext = { movePreview(1) },
                            onCommitPreview = ::commitPreview,
                            onCancelPreview = ::cancelPreview,
                            onRepeat = { onToggleRepeat?.invoke() },
                            previewIndex = previewIndex,
                            queueSize = queue.size,
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
                    animationSpec = tween(320, easing = XvoxPlayerTransitionMotion.easing),
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
                            onOpenSettings = { openSettingsBox("Lyrics") },
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
                            backgroundColor = paletteState.color,
                            textColor = lyricsTextColor,
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
                            previewIndex = previewIndex,
                            forceCurrentIndex = holdPagerPaletteDuringShuffle,
                            onPreviewIndexChange = { setPreviewTarget(it) },
                            onArtworkTap = { setMode(1) },
                            onSwipePalette = { base, adjacent, fraction ->
                                if (!holdPagerPaletteDuringShuffle) {
                                    paletteState.blend(base, adjacent, fraction)
                                }
                            },
                            onSettledPage = { settledIndex, settledSong -> commitSettledPreview(settledIndex, settledSong) },
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
                        // Travel fully beyond the top edge rather than stopping part-way under
                        // the fullscreen lyric surface.
                        translationY = -fullscreenProgress * (headerHeightDp + 28.dp).toPx()
                        alpha = 1f - fullscreenProgress
                    }
            ) {
                XvoxNowPlayingHeader(
                    onClose = ::dismiss,
                    onShare = { onShare?.invoke() ?: XvoxSongActions.share(context, song) },
                    onMore = { openSettingsBox("Style") },
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

            // Bottom controls are physically flush to the left, right, and bottom screen edges.
            // The single fullscreen clock carries this whole card beyond the bottom edge while
            // the lyrics surface expands through the newly released space; only top corners round.
            val bottomBoxShape = RoundedCornerShape(
                topStart = animTopRadius,
                topEnd = animTopRadius,
                bottomStart = 0.dp,
                bottomEnd = 0.dp
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(start = 0.dp, end = 0.dp, bottom = 0.dp)
                    .onGloballyPositioned { bottomHeightDp = with(density) { it.size.height.toDp() } }
                    .graphicsLayer {
                        // Push the entire bottom card below the screen during fullscreen, not
                        // merely past its normal controls' center line.
                        translationY = fullscreenProgress * (bottomHeightDp + 34.dp).toPx()
                        alpha = 1f - fullscreenProgress
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
                            onOpenOptions = { optionName -> openSettingsBox(optionName) },
                            actionPageIndex = actionPageIndex,
                            onActionPageChange = onActionPageChange
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
                    onShuffle = {
                        cancelPreview()
                        holdPagerPaletteDuringShuffle = true
                        paletteState.pin(song)
                        onToggleShuffle?.invoke()
                    },
                    onPreviewPrevious = { movePreview(-1) },
                    onTogglePlay = onTogglePlay,
                    onPreviewNext = { movePreview(1) },
                    onCommitPreview = ::commitPreview,
                    onCancelPreview = ::cancelPreview,
                    onRepeat = { onToggleRepeat?.invoke() },
                    previewIndex = previewIndex,
                    queueSize = queue.size,
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
                NowPlayingOptionsBox(onDismiss = { activeSettingsBox = null })
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
                    onDismiss = {
                        // These are live option sheets: any dismiss route is just a close.
                        activeSettingsBox = null
                    },
                    title = boxTitle,
                    presentation = if (activeSettingsBox == "Equalizer") {
                        XvoxBoxPresentation.EQUALIZER
                    } else {
                        XvoxBoxPresentation.DEFAULT
                    },
                    bottomAction = when (activeSettingsBox) {
                        "Equalizer" -> {
                            {
                                EqualizerFooterActions(
                                    onCancel = { activeSettingsBox = null },
                                    onReset = settingsViewModel::resetEqualizerControls,
                                    onDone = { activeSettingsBox = null }
                                )
                            }
                        }
                        "3D sound" -> {
                            {
                                XvoxTransactionalFooterActions(
                                    onCancel = { activeSettingsBox = null },
                                    onReset = {
                                        optionDraft = optionDraft.copy(
                                            stereoWidening = false,
                                            surroundWidth = .78f,
                                            surroundDepth = .65f,
                                            surroundPanSpeed = 6,
                                            hrtf = .6f,
                                            balance = 0f
                                        )
                                        applyThreeDSoundDraft()
                                    },
                                    onOkay = { activeSettingsBox = null }
                                )
                            }
                        }
                        "Crossfade" -> {
                            {
                                XvoxTransactionalFooterActions(
                                    onCancel = { activeSettingsBox = null },
                                    onReset = {
                                        optionDraft = optionDraft.copy(
                                            crossfade = false,
                                            gapless = true,
                                            crossfadeDuration = 3,
                                            crossfadeSmart = true,
                                            crossfadeClashControl = .7f,
                                            crossfadeBeatSync = true
                                        )
                                        applyCrossfadeDraft()
                                    },
                                    onOkay = { activeSettingsBox = null }
                                )
                            }
                        }
                        "Bluetooth" -> {
                            {
                                XvoxTransactionalFooterActions(
                                    onCancel = { activeSettingsBox = null },
                                    onOkay = { activeSettingsBox = null }
                                )
                            }
                        }
                        "Lyrics" -> {
                            {
                                XvoxTransactionalFooterActions(
                                    onCancel = { activeSettingsBox = null },
                                    onOkay = { activeSettingsBox = null }
                                )
                            }
                        }
                        else -> null
                    }
                ) {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            // The universal sheet constrains only genuine screen overflow. Every
                            // editor therefore begins at its own content height, including EQ.
                            .verticalScroll(scrollState)
                            .xvoxBoxScroll(scrollState)
                    ) {
                        when (activeSettingsBox) {
                            "Equalizer" -> EqualizerSettingsSection(
                                state = settingsState,
                                viewModel = settingsViewModel,
                                onCancel = { activeSettingsBox = null },
                                onDone = { activeSettingsBox = null },
                                showFooter = false
                            )
                            "3D sound" -> ThreeDSoundDraftSection(
                                state = optionDraft,
                                onStateChange = { updated ->
                                    optionDraft = updated
                                    applyThreeDSoundDraft(updated)
                                }
                            )
                            "Crossfade" -> PlaybackSettingsDraftSection(
                                state = optionDraft,
                                onStateChange = { updated ->
                                    optionDraft = updated
                                    applyCrossfadeDraft(updated)
                                }
                            )
                            "Bluetooth" -> HeadsetSettingsDraftSection(
                                state = optionDraft,
                                onStateChange = { updated ->
                                    optionDraft = updated
                                    applyBluetoothDraft(updated)
                                }
                            )
                            "Lyrics" -> LyricsSettingsDraftSection(
                                settings = optionDraft.lyrics,
                                onSettingsChange = { lyrics ->
                                    optionDraft = optionDraft.copy(lyrics = lyrics)
                                    applyLyricsDraft(optionDraft)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
