package com.xvox.music.player.nowplaying

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
    lyricsViewModel: XvoxLyricsViewModel = viewModel()
) {
    val colors = XvoxTheme.colors
    val lyricsState by lyricsViewModel.state.collectAsState()

    val paletteState =
        rememberXvoxNowPlayingPalette(
            song = song,
            queue = queue,
            currentIndex = currentIndex
        )

    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val screenHeight =
        with(density) {
            LocalConfiguration.current
                .screenHeightDp.dp
                .toPx()
        }

    val offset = remember {
        Animatable(screenHeight)
    }

    var dragY by remember {
        mutableFloatStateOf(0f)
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

    val totalOffset =
        (offset.value + dragY)
            .coerceAtLeast(0f)

    val dismissProgress =
        if (screenHeight > 0f) {
            (totalOffset / screenHeight)
                .coerceIn(0f, 1f)
        } else {
            0f
        }

    val screenCorner =
        32.dp * dismissProgress

    LaunchedEffect(song.id) {
        lyricsViewModel.load(song)
    }

    LaunchedEffect(Unit) {
        offset.animateTo(
            0f,
            XvoxNowPlayingMotion.enter
        )
    }

    fun dismiss() {
        if (dismissing) return

        dismissing = true

        scope.launch {
            offset.snapTo(totalOffset)
            dragY = 0f

            offset.animateTo(
                screenHeight,
                XvoxNowPlayingMotion.exit
            )

            onClose()
        }
    }

    fun requestPrevious() {
        if (currentIndex > 0) {
            navigationRequest =
                -(kotlin.math.abs(navigationRequest) + 1)
        }
    }

    fun requestNext() {
        if (
            currentIndex >= 0 &&
            currentIndex < queue.lastIndex
        ) {
            navigationRequest =
                kotlin.math.abs(navigationRequest) + 1
        }
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

    if (
        lyricsState.fullscreen &&
        lyricsState.lyrics != null
    ) {
        XvoxFullscreenLyrics(
            song = song,
            lyrics = lyricsState.lyrics!!,
            position = position,
            duration = duration,
            isPlaying = isPlaying,
            backgroundColor = paletteState.color.value,
            onPrevious = ::requestPrevious,
            onTogglePlay = onTogglePlay,
            onNext = ::requestNext,
            onSeek = onSeek,
            onClose =
                lyricsViewModel::closeFullscreen,
            modifier = Modifier.fillMaxSize()
        )

        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = totalOffset
                shape =
                    RoundedCornerShape(screenCorner)
                clip = dismissProgress > 0f
            }
    ) {
        XvoxNowPlayingBackdrop(
            dominant = paletteState.color.value,
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
                                dragY = 0f
                            },
                            onVerticalDrag = { change, amount ->
                                change.consume()

                                dragY =
                                    (dragY + amount)
                                        .coerceAtLeast(0f)
                            },
                            onDragEnd = {
                                if (
                                    dragY >=
                                    XvoxNowPlayingMotion
                                        .DismissThreshold
                                ) {
                                    dismiss()
                                } else {
                                    scope.launch {
                                        offset.snapTo(totalOffset)
                                        dragY = 0f
                                        offset.animateTo(
                                            0f,
                                            XvoxNowPlayingMotion
                                                .returnToRest
                                        )
                                    }
                                }
                            },
                            onDragCancel = {
                                scope.launch {
                                    offset.snapTo(totalOffset)
                                    dragY = 0f
                                    offset.animateTo(
                                        0f,
                                        XvoxNowPlayingMotion
                                            .returnToRest
                                    )
                                }
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
                        top = 34.dp,
                        end = 12.dp,
                        bottom = 50.dp
                    ),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                ) {
                    AnimatedContent(
                        targetState = showLyrics,
                        transitionSpec = {
                            fadeIn(tween(180))
                                .togetherWith(
                                    fadeOut(tween(150))
                                )
                        },
                        modifier = Modifier.fillMaxSize(),
                        label = "artworkLyrics"
                    ) { lyricsVisible ->
                        if (lyricsVisible) {
                            XvoxArtworkLyrics(
                                state = lyricsState,
                                position = position,
                                onAttach =
                                    lyricsViewModel::attach,
                                onClose = {
                                    showLyrics = false
                                },
                                onFullscreen =
                                    lyricsViewModel::openFullscreen,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(
                                        RoundedCornerShape(20.dp)
                                    )
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(song.id) {
                                        detectTapGestures(
                                            onTap = {
                                                showLyrics = true
                                            }
                                        )
                                    }
                            ) {
                                XvoxNowPlayingArtworkPager(
                                    queue = queue,
                                    currentIndex = currentIndex,
                                    navigationRequest =
                                        navigationRequest,
                                    onVisualSong = { visualSong ->
                                        scope.launch {
                                            paletteState.show(
                                                visualSong
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
                        bottom = 16.dp
                    )
            ) {
                NowPlayingActions()

                Spacer(
                    Modifier.size(18.dp)
                )

                Text(
                    text = song.title,
                    color = colors.primaryText,
                    fontSize = 21.sp,
                    lineHeight = 25.sp,
                    fontWeight = FontWeight.ExtraBold,
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

                Spacer(
                    Modifier.size(12.dp)
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
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(
                    Modifier.size(10.dp)
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
                    modifier = Modifier.align(
                        Alignment.CenterHorizontally
                    )
                )

                Spacer(
                    Modifier.size(10.dp)
                )
            }
        }
    }
}

@Composable
private fun NowPlayingActions() {
    val colors = XvoxTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .background(
                    colors.card.copy(
                        alpha = 0.20f
                    ),
                    RoundedCornerShape(22.dp)
                )
                .padding(horizontal = 3.dp),
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
            R.drawable.ic_xvox_heart_outline
        )
    }
}

@Composable
private fun NowPlayingActionIcon(
    resource: Int
) {
    val colors = XvoxTheme.colors

    Box(
        modifier = Modifier.size(42.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(resource),
            contentDescription = null,
            tint = colors.primaryText,
            modifier = Modifier.size(19.dp)
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
                colors.card.copy(alpha = 0.20f),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(resource),
            contentDescription = null,
            tint = colors.primaryText,
            modifier = Modifier.size(19.dp)
        )
    }
}
