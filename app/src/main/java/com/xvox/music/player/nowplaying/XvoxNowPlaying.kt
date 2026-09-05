package com.xvox.music.player.nowplaying

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
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
import com.xvox.music.features.player.styles.XvoxPlayerStyle
import com.xvox.music.player.nowplaying.components.NowPlayingActions
import com.xvox.music.player.nowplaying.components.NowPlayingOptionsSheet
import com.xvox.music.player.nowplaying.lyrics.XvoxArtworkLyrics
import com.xvox.music.player.nowplaying.lyrics.XvoxFullscreenLyrics
import com.xvox.music.player.nowplaying.lyrics.XvoxLyricsViewModel
import com.xvox.music.player.playback.RepeatMode
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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
    lyricsViewModel: XvoxLyricsViewModel = viewModel()
) {
    val colors = XvoxTheme.colors
    val lyricsState by lyricsViewModel.state.collectAsState()
    val paletteState = rememberXvoxNowPlayingPalette(song, queue, currentIndex)
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val screenHeight = with(density) {
        LocalConfiguration.current.screenHeightDp.dp.toPx()
    }

    var screenY by rememberSaveable { mutableFloatStateOf(screenHeight) }
    var entered by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showQuickSettingsSheet by remember { mutableStateOf(false) }
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
        if (queue.isEmpty() || currentIndex < 0) return
        val atFirst = currentIndex <= 0
        if (atFirst && repeatMode != RepeatMode.ALL) return

        if (showLyrics) {
            val target = if (atFirst) queue.lastIndex else currentIndex - 1
            onPlayQueueIndex(target)
        } else {
            navigationRequest = -(abs(navigationRequest) + 1)
        }
    }

    fun requestNext() {
        if (queue.isEmpty() || currentIndex < 0) return
        val atLast = currentIndex >= queue.lastIndex
        if (atLast && repeatMode != RepeatMode.ALL) return

        if (showLyrics) {
            val target = if (atLast) 0 else currentIndex + 1
            onPlayQueueIndex(target)
        } else {
            navigationRequest = abs(navigationRequest) + 1
        }
    }

    LaunchedEffect(song.id) {
        lyricsViewModel.load(song)
    }

    BackHandler {
        when {
            showQuickSettingsSheet -> showQuickSettingsSheet = false
            lyricsState.fullscreen -> lyricsViewModel.closeFullscreen()
            showLyrics -> showLyrics = false
            else -> dismiss()
        }
    }

    if (lyricsState.fullscreen) {
        XvoxFullscreenLyrics(
            song = song,
            state = lyricsState,
            position = position,
            duration = duration,
            isPlaying = isPlaying,
            backgroundColor = paletteState.color,
            onAttach = lyricsViewModel::attach,
            onDelete = lyricsViewModel::removeCustom,
            onPrevious = ::requestPrevious,
            onTogglePlay = onTogglePlay,
            onNext = ::requestNext,
            onSeek = onSeek,
            onClose = lyricsViewModel::closeFullscreen,
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { translationY = screenY }
    ) {
        XvoxNowPlayingBackdrop(
            dominant = paletteState.color,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, dragAmount ->
                            screenY = (screenY + dragAmount).coerceAtLeast(0f)
                        },
                        onDragEnd = {
                            if (screenY > screenHeight * 0.25f) {
                                dismiss()
                            } else {
                                returnToRest()
                            }
                        },
                        onDragCancel = { returnToRest() }
                    )
                }
        ) {
            XvoxNowPlayingHeader(
                onClose = ::dismiss,
                onShare = { onShare?.invoke() },
                onMore = { showQuickSettingsSheet = true },
                playingSource = playingSource
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (showLyrics) {
                    XvoxArtworkLyrics(
                        state = lyricsState,
                        position = position,
                        onSeek = onSeek,
                        onAttach = lyricsViewModel::attach,
                        onDelete = lyricsViewModel::removeCustom,
                        onClose = { showLyrics = false },
                        onFullscreen = lyricsViewModel::openFullscreen,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(20.dp))
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
                        modifier = Modifier.fillMaxSize(),
                        repeatMode = repeatMode
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(colors.background.copy(alpha = 0.35f))
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 8.dp)
            ) {
                NowPlayingActions(
                    isLiked = isLiked,
                    isInPlaylist = isInPlaylist,
                    onTimer = { onTimer?.invoke() },
                    onQueue = { onQueue?.invoke() },
                    onInfo = { onInfo?.invoke() },
                    onToggleLiked = { onToggleLiked?.invoke() },
                    onStarPlaylist = { onStarPlaylist?.invoke() },
                    timerProgress = sleepTimerProgress
                )

                Spacer(Modifier.height(18.dp))

                Text(
                    text = song.title,
                    color = colors.primaryText,
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = song.artist,
                    color = colors.secondaryText,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(14.dp))

                XvoxNowPlayingProgress(
                    position = position,
                    duration = duration,
                    onSeek = onSeek
                )

                Spacer(Modifier.height(8.dp))

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
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "XVOX",
                    color = colors.primaryText.copy(alpha = 0.55f),
                    fontFamily = XvoxLogoFont,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(Modifier.height(4.dp))
            }
        }

        if (showQuickSettingsSheet) {
            NowPlayingOptionsSheet(
                onDismiss = { showQuickSettingsSheet = false },
                onTimer = onTimer,
                onInfo = onInfo,
                onStarPlaylist = onStarPlaylist,
                onShare = onShare
            )
        }
    }
}
