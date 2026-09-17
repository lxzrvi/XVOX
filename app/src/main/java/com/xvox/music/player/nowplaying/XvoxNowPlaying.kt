package com.xvox.music.player.nowplaying

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.features.player.styles.XvoxPlayerStyle
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.player.nowplaying.components.NowPlayingActions
import com.xvox.music.player.nowplaying.components.NowPlayingOptionsBox
import com.xvox.music.player.nowplaying.lyrics.XvoxArtworkLyrics
import com.xvox.music.player.nowplaying.lyrics.XvoxFullscreenLyrics
import com.xvox.music.player.nowplaying.lyrics.XvoxLyricsViewModel
import com.xvox.music.player.playback.RepeatMode
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val DRAG_DISMISS_THRESHOLD_DP = 140f
private const val VELOCITY_DISMISS_THRESHOLD = 1100f
private val EnterEasing = CubicBezierEasing(0.05f, 0.9f, 0.1f, 1f)
private val ExitEasing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

@Composable
fun XvoxNowPlaying(
    song: Song,
    queue: List<Song>,
    currentIndex: Int,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    onClose: () -> Unit,
    displayMode: Int = 0,
    onDisplayModeChange: ((Int) -> Unit)? = null,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPlayQueueIndex: ((Int) -> Unit)? = null,
    onSeek: (Long) -> Unit,
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
    settingsViewModel: SettingsViewModel = viewModel(),
    lyricsViewModel: XvoxLyricsViewModel = viewModel()
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val overlays = LocalXvoxOverlayController.current
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val settingsState by settingsViewModel.state.collectAsState()
    val isCompactRequested = settingsState.nowPlayingStyle == "compact" || settingsState.nowPlayingStyle == "immersive"
    val isCompact = isCompactRequested && !isLandscape

    var isLyricsExpanded by remember { mutableStateOf(false) }
    var activeSettingsBox by remember { mutableStateOf<String?>(null) }
    var navigationRequest by remember { mutableIntStateOf(0) }

    val lyricsState by lyricsViewModel.state.collectAsState()
    LaunchedEffect(song.id) {
        lyricsViewModel.load(song)
    }

    // Dynamic Palette
    val paletteState = rememberXvoxNowPlayingPalette(song, queue, currentIndex)
    val backgroundBase = paletteState.color

    // Entry / Exit Animation
    val enterAnim = remember { Animatable(0f) }
    var isDismissing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        enterAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 380, easing = EnterEasing)
        )
    }

    fun dismiss() {
        if (isDismissing) return
        isDismissing = true
        coroutineScope.launch {
            enterAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 260, easing = ExitEasing)
            )
            onClose()
        }
    }

    BackHandler {
        if (isLyricsExpanded) {
            isLyricsExpanded = false
        } else if (displayMode != 0) {
            onDisplayModeChange?.invoke(0)
        } else {
            dismiss()
        }
    }

    // Drag to Dismiss
    val dragOffsetY = remember { Animatable(0f) }
    val dismissThresholdPx = with(density) { DRAG_DISMISS_THRESHOLD_DP.dp.toPx() }

    val draggableState = rememberDraggableState { delta ->
        coroutineScope.launch {
            val newOffset = (dragOffsetY.value + delta).coerceAtLeast(0f)
            dragOffsetY.snapTo(newOffset)
        }
    }

    fun handleDragEnd(velocity: Float) {
        coroutineScope.launch {
            if (dragOffsetY.value > dismissThresholdPx || velocity > VELOCITY_DISMISS_THRESHOLD) {
                isDismissing = true
                dragOffsetY.animateTo(
                    targetValue = 2000f,
                    animationSpec = tween(durationMillis = 220, easing = ExitEasing)
                )
                onClose()
            } else {
                dragOffsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 280, easing = EnterEasing)
                )
            }
        }
    }

    fun setMode(newMode: Int) {
        haptics.tap()
        onDisplayModeChange?.invoke(newMode)
    }

    val fraction = enterAnim.value
    val sheetOffsetY = if (isDismissing) dragOffsetY.value else (1f - fraction) * 400f
    val sheetAlpha = fraction.coerceIn(0f, 1f)
    val sheetScale = 0.94f + (0.06f * fraction)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = sheetAlpha
                scaleX = sheetScale
                scaleY = sheetScale
                translationY = sheetOffsetY + dragOffsetY.value
            }
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        backgroundBase.copy(alpha = 0.85f),
                        colors.background.copy(alpha = 0.98f),
                        colors.background
                    )
                )
            )
    ) {
        if (isLandscape) {
            // Landscape 2-pane Layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Left pane: Artwork bounded to screen width
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    XvoxNowPlayingArtworkPager(
                        queue = queue,
                        currentIndex = currentIndex,
                        navigationRequest = navigationRequest,
                        onArtworkTap = { setMode(1) },
                        onSwipePalette = { curr, adj, offset ->
                            paletteState.blend(curr, adj, abs(offset))
                        },
                        onSettledPage = { newIndex ->
                            if (newIndex != currentIndex) {
                                onPlayQueueIndex?.invoke(newIndex)
                            }
                        },
                        repeatMode = repeatMode,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Right pane: Header buttons + Title + Controls + Actions
                Column(
                    modifier = Modifier
                        .weight(1.15f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.card.copy(alpha = 0.45f))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Right pane Top Header (Close, Source, Options)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(colors.cardElevated)
                                .xvoxPressScale { dismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_xvox_collapse),
                                contentDescription = "Close",
                                tint = colors.primaryText,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = playingSource,
                            color = colors.secondaryText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (onShare != null) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(colors.cardElevated)
                                        .xvoxPressScale { onShare() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_xvox_share),
                                        contentDescription = "Share",
                                        tint = colors.primaryText,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(colors.cardElevated)
                                    .xvoxPressScale {
                                        Toast.makeText(context, "Can't enable compact in landscape mode", Toast.LENGTH_SHORT).show()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_xvox_more),
                                    contentDescription = "Options",
                                    tint = colors.secondaryText.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Title & Artist
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                color = colors.primaryText,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.artist,
                                color = colors.secondaryText,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (onToggleLiked != null) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(colors.cardElevated)
                                    .xvoxPressScale { onToggleLiked() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (isLiked) R.drawable.ic_xvox_heart
                                        else R.drawable.ic_xvox_heart_outline
                                    ),
                                    contentDescription = "Like",
                                    tint = if (isLiked) colors.primaryAccent else colors.primaryText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Progress Bar
                    XvoxNowPlayingProgress(
                        position = position,
                        duration = duration,
                        onSeek = onSeek,
                        currentSongId = song.id,
                        showTime = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Controls
                    XvoxNowPlayingControls(
                        isPlaying = isPlaying,
                        isShuffleEnabled = isShuffleEnabled,
                        repeatMode = repeatMode,
                        onShuffle = { onToggleShuffle?.invoke() },
                        onPrevious = {
                            navigationRequest--
                            onPrevious()
                        },
                        onTogglePlay = onTogglePlay,
                        onNext = {
                            navigationRequest++
                            onNext()
                        },
                        onRepeat = { onToggleRepeat?.invoke() },
                        currentIndex = currentIndex,
                        queueSize = queue.size,
                        positionMs = position,
                        durationMs = duration,
                        onScrubTo = onSeek,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Actions Row
                    NowPlayingActions(
                        isLiked = isLiked,
                        isInPlaylist = isInPlaylist,
                        onTimer = onTimer,
                        onQueue = onQueue,
                        onInfo = onInfo,
                        onToggleLiked = onToggleLiked,
                        onStarPlaylist = onStarPlaylist,
                        timerProgress = sleepTimerProgress,
                        lyricsOn = displayMode == 1,
                        onToggleLyrics = { setMode(if (displayMode == 1) 0 else 1) },
                        onOpenOptions = { activeSettingsBox = it }
                    )
                }
            }
        } else {
            // Portrait Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .draggable(
                        state = draggableState,
                        orientation = Orientation.Vertical,
                        onDragStopped = { velocity -> handleDragEnd(velocity) }
                    )
            ) {
                XvoxNowPlayingHeader(
                    onClose = ::dismiss,
                    modifier = Modifier.fillMaxWidth(),
                    onShare = onShare,
                    onMore = { activeSettingsBox = "style" },
                    playingSource = playingSource
                )

                // Artwork / Lyrics Content Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = if (isCompact) 14.dp else 18.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = displayMode,
                        transitionSpec = {
                            (fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.95f))
                                .togetherWith(fadeOut(tween(250)) + scaleOut(tween(250), targetScale = 0.95f))
                        },
                        label = "nowPlayingModeContent"
                    ) { mode ->
                        if (mode == 1) {
                            XvoxArtworkLyrics(
                                state = lyricsState,
                                position = position,
                                onSeek = onSeek,
                                onAttach = { uri -> lyricsViewModel.attach(uri) },
                                onDelete = { lyricsViewModel.removeCustom() },
                                onClose = { setMode(0) },
                                expanded = isLyricsExpanded,
                                onToggleExpand = { isLyricsExpanded = !isLyricsExpanded },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            XvoxNowPlayingArtworkPager(
                                queue = queue,
                                currentIndex = currentIndex,
                                navigationRequest = navigationRequest,
                                onArtworkTap = { setMode(1) },
                                onSwipePalette = { curr, adj, offset ->
                                    paletteState.blend(curr, adj, abs(offset))
                                },
                                onSettledPage = { newIndex ->
                                    if (newIndex != currentIndex) {
                                        onPlayQueueIndex?.invoke(newIndex)
                                    }
                                },
                                repeatMode = repeatMode,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // Metadata + Controls + Actions
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Title & Artist + Like
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = song.title,
                                color = colors.primaryText,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.artist,
                                color = colors.secondaryText,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (onToggleLiked != null) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(colors.cardElevated)
                                        .xvoxPressScale { onToggleLiked() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            if (isLiked) R.drawable.ic_xvox_heart
                                            else R.drawable.ic_xvox_heart_outline
                                        ),
                                        contentDescription = "Like",
                                        tint = if (isLiked) colors.primaryAccent else colors.primaryText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(colors.cardElevated)
                                    .xvoxPressScale { setMode(if (displayMode == 1) 0 else 1) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_xvox_lyrics),
                                    contentDescription = "Lyrics",
                                    tint = if (displayMode == 1) colors.primaryAccent else colors.primaryText,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }

                    // Progress Bar
                    XvoxNowPlayingProgress(
                        position = position,
                        duration = duration,
                        onSeek = onSeek,
                        currentSongId = song.id,
                        showTime = !isCompact,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Playback Controls
                    XvoxNowPlayingControls(
                        isPlaying = isPlaying,
                        isShuffleEnabled = isShuffleEnabled,
                        repeatMode = repeatMode,
                        onShuffle = { onToggleShuffle?.invoke() },
                        onPrevious = {
                            navigationRequest--
                            onPrevious()
                        },
                        onTogglePlay = onTogglePlay,
                        onNext = {
                            navigationRequest++
                            onNext()
                        },
                        onRepeat = { onToggleRepeat?.invoke() },
                        currentIndex = currentIndex,
                        queueSize = queue.size,
                        positionMs = position,
                        durationMs = duration,
                        onScrubTo = onSeek,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 2-page Actions Row
                    NowPlayingActions(
                        isLiked = isLiked,
                        isInPlaylist = isInPlaylist,
                        onTimer = onTimer,
                        onQueue = onQueue,
                        onInfo = onInfo,
                        onToggleLiked = onToggleLiked,
                        onStarPlaylist = onStarPlaylist,
                        timerProgress = sleepTimerProgress,
                        lyricsOn = displayMode == 1,
                        onToggleLyrics = { setMode(if (displayMode == 1) 0 else 1) },
                        onOpenOptions = { activeSettingsBox = it }
                    )
                }
            }
        }

        // Fullscreen Lyrics Overlay
        AnimatedVisibility(
            visible = isLyricsExpanded,
            enter = fadeIn(tween(280)) + scaleIn(tween(280), initialScale = 0.96f),
            exit = fadeOut(tween(220)) + scaleOut(tween(220), targetScale = 0.96f),
            modifier = Modifier.fillMaxSize()
        ) {
            XvoxFullscreenLyrics(
                song = song,
                state = lyricsState,
                position = position,
                duration = duration,
                isPlaying = isPlaying,
                backgroundColor = paletteState.color,
                onSeek = onSeek,
                onAttach = { uri -> lyricsViewModel.attach(uri) },
                onDelete = { lyricsViewModel.removeCustom() },
                onPrevious = {
                    navigationRequest--
                    onPrevious()
                },
                onTogglePlay = onTogglePlay,
                onNext = {
                    navigationRequest++
                    onNext()
                },
                onClose = { isLyricsExpanded = false },
                onOpenSettings = { activeSettingsBox = "style" },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Options Box (Style selection)
        if (activeSettingsBox == "style") {
            NowPlayingOptionsBox(
                onDismiss = { activeSettingsBox = null },
                settingsViewModel = settingsViewModel
            )
        }
    }
}
