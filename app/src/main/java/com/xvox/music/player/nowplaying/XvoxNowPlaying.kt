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
import androidx.compose.ui.draw.clipToBounds
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
import com.xvox.music.features.settings.EqualizerControlsSnapshot
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.sections.EqualizerFooterActions
import com.xvox.music.features.settings.sections.EqualizerSettingsSection
import com.xvox.music.features.settings.components.XvoxTransactionalFooterActions
import com.xvox.music.features.settings.sections.HeadsetSettingsDraftSection
import com.xvox.music.features.settings.sections.LyricsSettingsDraftSection
import com.xvox.music.features.settings.sections.PlaybackSettingsDraftSection
import com.xvox.music.features.settings.sections.ThreeDSoundSettingsSection
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
    // Crossfade, Bluetooth and Lyrics use one local sheet snapshot. Draft gestures recompose the
    // sheet immediately, but no preference/audio setting is touched until Okay is pressed.
    var optionDraft by remember { mutableStateOf(settingsState) }
    fun openSettingsBox(name: String) {
        if (name == "Crossfade" || name == "Bluetooth" || name == "Lyrics") {
            optionDraft = settingsState
        }
        activeSettingsBox = name
    }
    fun applyCrossfadeDraft() {
        val draft = optionDraft
        settingsViewModel.setCrossfade(draft.crossfade)
        settingsViewModel.setGapless(draft.gapless)
        settingsViewModel.setCrossfadeDuration(draft.crossfadeDuration)
        settingsViewModel.setCrossfadeSmart(draft.crossfadeSmart)
        settingsViewModel.setCrossfadeClashControl(draft.crossfadeClashControl)
        settingsViewModel.setCrossfadeBeatSync(draft.crossfadeBeatSync)
    }
    fun applyBluetoothDraft() {
        val draft = optionDraft
        settingsViewModel.setAudioOutputRoute(draft.audioOutputRoute)
        settingsViewModel.setBtConnectAction(draft.btConnectAction)
        settingsViewModel.setPlayOnHeadsetConnect(draft.playOnHeadsetConnect)
        settingsViewModel.setBtDisconnectAction(draft.btDisconnectAction)
        settingsViewModel.setPauseOnHeadphoneDisconnect(draft.pauseOnHeadphoneDisconnect)
    }
    fun applyLyricsDraft() {
        val lyrics = optionDraft.lyrics
        settingsViewModel.updateLyrics { lyrics }
    }
    var equalizerSnapshot by remember { mutableStateOf<EqualizerControlsSnapshot?>(null) }
    LaunchedEffect(activeSettingsBox) {
        equalizerSnapshot = if (activeSettingsBox == "Equalizer") {
            settingsViewModel.snapshotEqualizerControls()
        } else {
            null
        }
    }
    var dismissing by remember { mutableStateOf(false) }
    var navigationRequest by remember { mutableIntStateOf(0) }
    // Separate visual browsing from the audio deck. Covers may move immediately while the
    // audible item stays untouched until the navigation button is released. Keep the target Song
    // ID alongside the pager index: queue reorders (especially Shuffle) invalidate bare indices.
    var previewIndex by rememberSaveable { mutableIntStateOf(currentIndex.coerceIn(0, queue.lastIndex.coerceAtLeast(0))) }
    var previewSongId by rememberSaveable {
        mutableLongStateOf(queue.getOrNull(currentIndex)?.id ?: queue.firstOrNull()?.id ?: -1L)
    }
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
        val targetSong = sourceQueue.getOrNull(index) ?: return false
        previewIndex = index
        previewSongId = targetSong.id
        return true
    }

    LaunchedEffect(currentIndex, queue) {
        // An external player change wins over an old delayed button-release request. When a queue
        // order changes while browsing, resolve the currently visible cover by ID before using it.
        if (!previewGestureActive) cancelPendingPreviewCommit()
        val currentSong = queue.getOrNull(currentIndex)
        val samePreviewIndex = queue.indexOfFirst { it.id == previewSongId }
        when {
            previewGestureActive && samePreviewIndex >= 0 -> previewIndex = samePreviewIndex
            currentSong != null -> {
                previewIndex = currentIndex
                previewSongId = currentSong.id
            }
            previewIndex !in queue.indices && queue.isNotEmpty() -> {
                previewIndex = 0
                previewSongId = queue.first().id
            }
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
        val stablePreviewIndex = queue.indexOfFirst { it.id == previewSongId }
        val from = stablePreviewIndex.takeIf { it in queue.indices }
            ?: previewIndex.takeIf { it in queue.indices }
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
        val targetSongId = previewSongId
        cancelPendingPreviewCommit()
        previewGestureActive = false
        val targetNow = latestQueue.indexOfFirst { it.id == targetSongId }
        if (targetNow !in latestQueue.indices || targetNow == latestCurrentIndex) return

        val requestVersion = previewCommitVersion
        previewCommitJob = scope.launch {
            // Keep audio on the current song until the released cover has rested in place.
            delay(300)
            val stableQueue = latestQueue
            val stableTarget = stableQueue.indexOfFirst { it.id == targetSongId }
            if (
                requestVersion == previewCommitVersion &&
                !previewGestureActive &&
                previewSongId == targetSongId &&
                stableTarget in stableQueue.indices &&
                stableTarget != latestCurrentIndex
            ) {
                onPlayQueueIndex(stableTarget)
            }
            if (requestVersion == previewCommitVersion) previewCommitJob = null
        }
    }

    /** The pager already waited its 300 ms release window; resolve its visual Song by ID once. */
    fun commitSettledPreview(songId: Long) {
        cancelPendingPreviewCommit()
        previewGestureActive = false
        val stableQueue = latestQueue
        val stableTarget = stableQueue.indexOfFirst { it.id == songId }
        if (stableTarget !in stableQueue.indices) return
        previewIndex = stableTarget
        previewSongId = songId
        if (stableTarget != latestCurrentIndex) onPlayQueueIndex(stableTarget)
    }

    fun cancelPreview() {
        cancelPendingPreviewCommit()
        previewGestureActive = false
        val currentSong = latestQueue.getOrNull(latestCurrentIndex)
        if (currentSong != null) {
            previewIndex = latestCurrentIndex
            previewSongId = currentSong.id
        }
    }

    LaunchedEffect(song.id) {
        lyricsViewModel.load(song)
    }

    BackHandler {
        when {
            activeSettingsBox != null -> {
                if (activeSettingsBox == "Equalizer") {
                    equalizerSnapshot?.let(settingsViewModel::restoreEqualizerControls)
                }
                // Draft-backed editors have made no persistent change, so Back is Cancel.
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
    // The control card now has its own 6dp bottom frame; retain the original 6dp cover-to-card
    // clearance as well so it floats with equal side/bottom/adjacent breathing room.
    val currentPadBottom = lerp(bottomHeightDp + 12.dp, 0.dp, fullscreenProgress)

    val view = androidx.compose.ui.platform.LocalView.current
    DisposableEffect(isLandscape) {
        val window = (view.context as? android.app.Activity)?.window
        val insetsController = window?.let { androidx.core.view.WindowCompat.getInsetsController(it, view) }
        if (isLandscape) {
            insetsController?.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController?.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
        } else {
            insetsController?.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            insetsController?.isAppearanceLightStatusBars = false
        }
        onDispose {
            if (isLandscape) {
                insetsController?.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            }
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
                val compactControlsWidth = (maxWidth - compactInset * 2 - compactGutter - compactArtworkWidth)
                    .coerceAtLeast(0.dp)
                val frameInset = lerp(compactInset, 0.dp, fullscreenProgress)
                val artworkWidth = lerp(compactArtworkWidth, maxWidth, fullscreenProgress)
                val artworkRadius = lerp(20.dp, 0.dp, fullscreenProgress)
                val controlsSlidePx = with(density) { (compactControlsWidth + compactInset).toPx() }

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
                                previewSongId = previewSongId.takeIf { it >= 0L },
                                onPreviewIndexChange = { setPreviewTarget(it) },
                                onArtworkTap = { setMode(1) },
                                onSwipePalette = { base, adjacent, fraction ->
                                    if (!holdPagerPaletteDuringShuffle) {
                                        paletteState.blend(base, adjacent, fraction)
                                    }
                                },
                                onSettledPage = { settledSong -> commitSettledPreview(settledSong.id) },
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(0.dp),
                                pageSpacing = 0.dp,
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
                        .padding(top = frameInset, end = frameInset, bottom = frameInset)
                        .width(compactControlsWidth)
                        .fillMaxHeight()
                        .graphicsLayer {
                            translationX = fullscreenProgress * controlsSlidePx
                            alpha = (1f - fullscreenProgress * 1.45f).coerceIn(0f, 1f)
                        }
                ) {
                    // Right: Option/Control Card (40% width), intentionally borderless.
                    val landscapeScroll = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
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
                            onOpenOptions = { optionName -> openSettingsBox(optionName) }
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
                            previewSongId = previewSongId.takeIf { it >= 0L },
                            onPreviewIndexChange = { setPreviewTarget(it) },
                            onArtworkTap = { setMode(1) },
                            onSwipePalette = { base, adjacent, fraction ->
                                if (!holdPagerPaletteDuringShuffle) {
                                    paletteState.blend(base, adjacent, fraction)
                                }
                            },
                            onSettledPage = { settledSong -> commitSettledPreview(settledSong.id) },
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

            // Bottom Controls Area: smooth slide down & fade out during fullscreen opening
            // Match the cover's horizontal frame with a floating bottom-control card, including
            // the same bottom breathing room. Fullscreen interpolates those gaps back to zero.
            val bottomBoxShape = RoundedCornerShape(animTopRadius)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(start = currentPadH, end = currentPadH, bottom = currentPadH)
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
                            onOpenOptions = { optionName -> openSettingsBox(optionName) }
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
                        // Scrim/back/Header close is Cancel for every transactional draft. EQ is
                        // exceptional because it previews live audio and must restore its snapshot.
                        if (activeSettingsBox == "Equalizer") {
                            equalizerSnapshot?.let(settingsViewModel::restoreEqualizerControls)
                        }
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
                                    onCancel = {
                                        equalizerSnapshot?.let(settingsViewModel::restoreEqualizerControls)
                                        activeSettingsBox = null
                                    },
                                    onReset = settingsViewModel::resetEqualizerControls,
                                    onDone = { activeSettingsBox = null }
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
                                    },
                                    onOkay = {
                                        applyCrossfadeDraft()
                                        activeSettingsBox = null
                                    }
                                )
                            }
                        }
                        "Bluetooth" -> {
                            {
                                XvoxTransactionalFooterActions(
                                    onCancel = { activeSettingsBox = null },
                                    onOkay = {
                                        applyBluetoothDraft()
                                        activeSettingsBox = null
                                    }
                                )
                            }
                        }
                        "Lyrics" -> {
                            {
                                XvoxTransactionalFooterActions(
                                    onCancel = { activeSettingsBox = null },
                                    onOkay = {
                                        applyLyricsDraft()
                                        activeSettingsBox = null
                                    }
                                )
                            }
                        }
                        else -> null
                    }
                ) {
                    val scrollState = rememberScrollState()
                    val longEditor = activeSettingsBox == "Equalizer" || activeSettingsBox == "Lyrics"
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            // Long editors claim the supplied viewport; short sheets keep their
                            // natural measured height so the universal box can fit content.
                            .then(if (longEditor) Modifier.fillMaxHeight() else Modifier)
                            .verticalScroll(scrollState)
                            .xvoxBoxScroll(scrollState)
                    ) {
                        when (activeSettingsBox) {
                            "Equalizer" -> EqualizerSettingsSection(
                                state = settingsState,
                                viewModel = settingsViewModel,
                                onCancel = {
                                    equalizerSnapshot?.let(settingsViewModel::restoreEqualizerControls)
                                    activeSettingsBox = null
                                },
                                onDone = { activeSettingsBox = null },
                                showFooter = false
                            )
                            "3D sound" -> ThreeDSoundSettingsSection(state = settingsState, viewModel = settingsViewModel)
                            "Crossfade" -> PlaybackSettingsDraftSection(
                                state = optionDraft,
                                onStateChange = { optionDraft = it }
                            )
                            "Bluetooth" -> HeadsetSettingsDraftSection(
                                state = optionDraft,
                                onStateChange = { optionDraft = it }
                            )
                            "Lyrics" -> LyricsSettingsDraftSection(
                                settings = optionDraft.lyrics,
                                onSettingsChange = { lyrics -> optionDraft = optionDraft.copy(lyrics = lyrics) }
                            )
                        }
                    }
                }
            }
        }
    }
}
