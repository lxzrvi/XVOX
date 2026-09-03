package com.xvox.music.player.nowplaying

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxLogoFont
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.player.nowplaying.lyrics.XvoxArtworkLyrics
import com.xvox.music.player.nowplaying.lyrics.XvoxFullscreenLyrics
import com.xvox.music.player.nowplaying.lyrics.XvoxLyricsViewModel
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
    lyricsViewModel:
        XvoxLyricsViewModel = viewModel()
) {
    val colors = XvoxTheme.colors
    val lyricsState by
        lyricsViewModel.state.collectAsState()

    val paletteState =
        rememberXvoxNowPlayingPalette(
            song,
            queue,
            currentIndex
        )

    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val screenHeight =
        with(density) {
            LocalConfiguration.current
                .screenHeightDp.dp.toPx()
        }

    var screenY by remember {
        mutableFloatStateOf(screenHeight)
    }

    var showLyrics by remember {
        mutableStateOf(false)
    }

    var dismissing by remember {
        mutableStateOf(false)
    }

    var navigationRequest by remember {
        mutableIntStateOf(0)
    }

    var motionJob by remember {
        mutableStateOf<Job?>(null)
    }

    val dismissProgress =
        if (screenHeight > 0f) {
            (screenY / screenHeight)
                .coerceIn(0f, 1f)
        } else {
            0f
        }

    val screenCorner =
        32.dp * dismissProgress

    fun animateScreen(
        target: Float,
        durationMs: Int,
        finished: (() -> Unit)? = null
    ) {
        motionJob?.cancel()
        val start = screenY

        motionJob =
            scope.launch {
                val animation =
                    Animatable(start)

                animation.animateTo(
                    target,
                    tween(
                        durationMillis =
                            durationMs,
                        easing =
                            XvoxNowPlayingMotion.easing
                    )
                ) {
                    screenY = value
                }

                screenY = target
                motionJob = null
                finished?.invoke()
            }
    }

    fun dismiss() {
        if (dismissing) return
        dismissing = true

        animateScreen(
            target = screenHeight,
            durationMs =
                XvoxNowPlayingMotion
                    .exitDuration(
                        screenY,
                        screenHeight
                    ),
            finished = onClose
        )
    }

    fun returnToRest() {
        val fraction =
            if (screenHeight > 0f) {
                (screenY / screenHeight)
                    .coerceIn(0f, 1f)
            } else {
                1f
            }

        animateScreen(
            0f,
            (
                XvoxNowPlayingMotion.FullDuration *
                    fraction
                )
                .toInt()
                .coerceAtLeast(120)
        )
    }

    fun requestPrevious() {
        if (currentIndex <= 0) return

        if (showLyrics) {
            onPlayQueueIndex(
                currentIndex - 1
            )
        } else {
            navigationRequest =
                -(
                    kotlin.math.abs(
                        navigationRequest
                    ) + 1
                    )
        }
    }

    fun requestNext() {
        if (
            currentIndex < 0 ||
            currentIndex >= queue.lastIndex
        ) return

        if (showLyrics) {
            onPlayQueueIndex(
                currentIndex + 1
            )
        } else {
            navigationRequest =
                kotlin.math.abs(
                    navigationRequest
                ) + 1
        }
    }

    LaunchedEffect(song.id) {
        lyricsViewModel.load(song)
    }

    LaunchedEffect(Unit) {
        animateScreen(
            0f,
            XvoxNowPlayingMotion.FullDuration
        )
    }

    BackHandler {
        when {
            lyricsState.fullscreen ->
                lyricsViewModel.closeFullscreen()

            showLyrics ->
                showLyrics = false

            else ->
                dismiss()
        }
    }

    if (lyricsState.fullscreen) {
        XvoxFullscreenLyrics(
            song = song,
            state = lyricsState,
            position = position,
            duration = duration,
            isPlaying = isPlaying,
            backgroundColor =
                paletteState.color,
            onAttach =
                lyricsViewModel::attach,
            onDelete =
                lyricsViewModel::removeCustom,
            onPrevious = ::requestPrevious,
            onTogglePlay = onTogglePlay,
            onNext = ::requestNext,
            onSeek = onSeek,
            onClose =
                lyricsViewModel::closeFullscreen,
            modifier =
                Modifier.fillMaxSize()
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = screenY
                shape =
                    RoundedCornerShape(
                        screenCorner
                    )
                clip =
                    dismissProgress > 0f
            }
    ) {
        XvoxNowPlayingBackdrop(
            dominant = paletteState.color,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(dismissing) {
                        if (dismissing) {
                            return@pointerInput
                        }

                        detectVerticalDragGestures(
                            onDragStart = {
                                motionJob?.cancel()
                                motionJob = null
                            },
                            onVerticalDrag = {
                                    change,
                                    amount ->

                                change.consume()

                                screenY =
                                    (screenY + amount)
                                        .coerceIn(
                                            0f,
                                            screenHeight
                                        )
                            },
                            onDragEnd = {
                                if (
                                    screenY >=
                                    XvoxNowPlayingMotion
                                        .DismissThreshold
                                ) {
                                    dismiss()
                                } else {
                                    returnToRest()
                                }
                            },
                            onDragCancel = {
                                returnToRest()
                            }
                        )
                    }
            ) {
                XvoxNowPlayingHeader(
                    onClose = ::dismiss,
                    onShare = {},
                    onMore = {}
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(
                        start = 12.dp,
                        end = 12.dp,
                        bottom = 31.dp
                    ),
                contentAlignment =
                    Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                ) {
                    if (showLyrics) {
                        XvoxArtworkLyrics(
                            state = lyricsState,
                            position = position,
                            onSeek = onSeek,
                            onAttach =
                                lyricsViewModel::attach,
                            onDelete =
                                lyricsViewModel::removeCustom,
                            onClose = {
                                showLyrics = false
                            },
                            onFullscreen =
                                lyricsViewModel::openFullscreen,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(
                                    RoundedCornerShape(
                                        20.dp
                                    )
                                )
                        )
                    } else {
                        XvoxNowPlayingArtworkPager(
                            queue = queue,
                            currentIndex =
                                currentIndex,
                            navigationRequest =
                                navigationRequest,
                            onArtworkTap = {
                                showLyrics = true
                            },
                            onSwipePalette = {
                                    base,
                                    adjacent,
                                    fraction ->

                                scope.launch {
                                    paletteState.blend(
                                        base,
                                        adjacent,
                                        fraction
                                    )
                                }
                            },
                            onSettledPage =
                                onPlayQueueIndex,
                            modifier =
                                Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(
                        RoundedCornerShape(
                            topStart = 28.dp,
                            topEnd = 28.dp
                        )
                    )
                    .background(
                        colors.background.copy(
                            alpha = 0.27f
                        )
                    )
                    .windowInsetsPadding(
                        WindowInsets.navigationBars
                    )
                    .padding(
                        start = 12.dp,
                        top = 12.dp,
                        end = 12.dp,
                        bottom = 6.dp
                    )
            ) {
                NowPlayingActions()

                Spacer(
                    Modifier.size(38.dp)
                )

                Text(
                    text = song.title,
                    color = colors.primaryText,
                    fontSize = 21.sp,
                    lineHeight = 25.sp,
                    fontWeight =
                        FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow =
                        TextOverflow.Ellipsis
                )

                Text(
                    text = song.artist,
                    color =
                        colors.secondaryText,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    fontWeight =
                        FontWeight.Medium,
                    maxLines = 1,
                    overflow =
                        TextOverflow.Ellipsis
                )

                Spacer(
                    Modifier.size(19.dp)
                )

                XvoxNowPlayingProgress(
                    position = position,
                    duration = duration,
                    onSeek = onSeek
                )

                Spacer(
                    Modifier.size(8.dp)
                )

                XvoxNowPlayingControls(
                    isPlaying = isPlaying,
                    onShuffle = {},
                    onPrevious = ::requestPrevious,
                    onTogglePlay = onTogglePlay,
                    onNext = ::requestNext,
                    onRepeat = {},
                    modifier =
                        Modifier.fillMaxWidth()
                )

                Spacer(
                    Modifier.size(29.dp)
                )

                Text(
                    text = "XVOX",
                    color =
                        colors.primaryText.copy(
                            alpha = 0.62f
                        ),
                    fontFamily = XvoxLogoFont,
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    letterSpacing = 1.4.sp,
                    modifier =
                        Modifier.align(
                            Alignment.CenterHorizontally
                        )
                )

                Spacer(
                    Modifier.size(3.dp)
                )
            }
        }
    }
}

@Composable
private fun NowPlayingActions() {
    val colors = XvoxTheme.colors

    Row(
        modifier =
            Modifier.fillMaxWidth(),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .background(
                    colors.card.copy(
                        alpha = 0.20f
                    ),
                    RoundedCornerShape(
                        22.dp
                    )
                )
                .padding(
                    horizontal = 3.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            NowPlayingActionIcon(
                R.drawable.ic_xvox_timer
            )
            NowPlayingActionIcon(
                R.drawable.ic_xvox_queue
            )
            NowPlayingActionIcon(
                R.drawable.ic_xvox_info
            )
        }

        Spacer(
            Modifier.weight(1f)
        )

        NowPlayingCircleAction(
            R.drawable.ic_xvox_star
        )

        Spacer(
            Modifier.size(10.dp)
        )

        NowPlayingCircleAction(
            R.drawable.ic_xvox_heart
        )
    }
}

@Composable
private fun NowPlayingActionIcon(
    resource: Int
) {
    val colors = XvoxTheme.colors

    Box(
        modifier =
            Modifier.size(42.dp),
        contentAlignment =
            Alignment.Center
    ) {
        Icon(
            painter =
                painterResource(resource),
            contentDescription = null,
            tint = colors.primaryText,
            modifier =
                Modifier.size(19.dp)
        )
    }
}

@Composable
private fun NowPlayingCircleAction(
    resource: Int
) {
    val colors = XvoxTheme.colors

    Box(
        modifier = Modifier
            .size(42.dp)
            .background(
                colors.card.copy(
                    alpha = 0.20f
                ),
                CircleShape
            ),
        contentAlignment =
            Alignment.Center
    ) {
        Icon(
            painter =
                painterResource(resource),
            contentDescription = null,
            tint = colors.primaryText,
            modifier =
                Modifier.size(19.dp)
        )
    }
}
