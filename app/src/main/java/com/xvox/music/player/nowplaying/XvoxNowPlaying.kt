package com.xvox.music.player.nowplaying

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
    currentIndex: Int,
    queueSize: Int,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    onClose: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    lyricsViewModel:
        XvoxLyricsViewModel =
        viewModel()
) {
    val colors =
        XvoxTheme.colors

    val lyricsState by
        lyricsViewModel.state
            .collectAsState()

    val density =
        LocalDensity.current

    val scope =
        rememberCoroutineScope()

    val screenHeight =
        with(density) {
            LocalConfiguration.current
                .screenHeightDp.dp
                .toPx()
        }

    val offset =
        remember {
            Animatable(
                screenHeight
            )
        }

    var dragY by remember {
        mutableFloatStateOf(0f)
    }

    var showLyrics by remember {
        mutableStateOf(false)
    }

    val totalOffset =
        (
            offset.value +
                dragY
            )
            .coerceAtLeast(
                0f
            )

    val dismissProgress =
        if (screenHeight > 0f) {
            (
                totalOffset /
                    screenHeight
                )
                .coerceIn(
                    0f,
                    1f
                )
        } else {
            0f
        }

    val cornerRadius =
        30.dp *
            dismissProgress

    /*
     * Lyrics are resolved whenever song changes.
     *
     * User-added source has priority.
     * Embedded metadata is fallback.
     */
    LaunchedEffect(
        song.id
    ) {
        lyricsViewModel.load(
            song
        )

        showLyrics =
            false
    }

    /*
     * Full Now Playing enters from screen bottom.
     */
    LaunchedEffect(Unit) {
        offset.animateTo(
            targetValue = 0f,
            animationSpec =
                XvoxNowPlayingMotion.enter
        )
    }

    fun dismiss() {
        scope.launch {
            offset.snapTo(
                totalOffset
            )

            dragY = 0f

            offset.animateTo(
                targetValue =
                    screenHeight,
                animationSpec =
                    XvoxNowPlayingMotion.exit
            )

            onClose()
        }
    }

    /*
     * Fullscreen lyrics is above the normal player,
     * but uses the same playback state.
     */
    if (
        lyricsState.fullscreen &&
        lyricsState.lyrics != null
    ) {
        XvoxFullscreenLyrics(
            song =
                song,
            lyrics =
                lyricsState.lyrics!!,
            position =
                position,
            duration =
                duration,
            isPlaying =
                isPlaying,
            onPrevious =
                onPrevious,
            onTogglePlay =
                onTogglePlay,
            onNext =
                onNext,
            onSeek =
                onSeek,
            onClose =
                lyricsViewModel::
                    closeFullscreen,
            modifier =
                modifier.fillMaxSize()
        )

        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY =
                    totalOffset

                shape =
                    RoundedCornerShape(
                        cornerRadius
                    )

                clip =
                    dismissProgress >
                        0f
            }
    ) {
        XvoxNowPlayingBackdrop(
            song =
                song
        )

        Column(
            modifier =
                Modifier.fillMaxSize()
        ) {
            /*
             * Header is the drag-down dismissal zone.
             */
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                dragY = 0f
                            },

                            onVerticalDrag = {
                                change,
                                amount ->

                                change.consume()

                                dragY =
                                    (
                                        dragY +
                                            amount
                                        )
                                        .coerceAtLeast(
                                            0f
                                        )
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
                                        offset.snapTo(
                                            totalOffset
                                        )

                                        dragY =
                                            0f

                                        offset.animateTo(
                                            targetValue =
                                                0f,
                                            animationSpec =
                                                XvoxNowPlayingMotion
                                                    .returnToRest
                                        )
                                    }
                                }
                            },

                            onDragCancel = {
                                scope.launch {
                                    offset.snapTo(
                                        totalOffset
                                    )

                                    dragY =
                                        0f

                                    offset.animateTo(
                                        targetValue =
                                            0f,
                                        animationSpec =
                                            XvoxNowPlayingMotion
                                                .returnToRest
                                    )
                                }
                            }
                        )
                    }
            ) {
                XvoxNowPlayingHeader(
                    onClose = {
                        dismiss()
                    },
                    onShare = {},
                    onMore = {}
                )
            }

            /*
             * =================================================
             * ARTWORK / LYRICS AREA
             * =================================================
             *
             * Same exact square is used by both states.
             */
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(
                        start = 12.dp,
                        top = 30.dp,
                        end = 12.dp,
                        bottom = 36.dp
                    ),
                contentAlignment =
                    Alignment.BottomCenter
            ) {
                val artworkShape =
                    RoundedCornerShape(
                        20.dp
                    )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(
                            artworkShape
                        )
                ) {
                    AnimatedContent(
                        targetState =
                            showLyrics,
                        transitionSpec = {
                            fadeIn(
                                tween(190)
                            ).togetherWith(
                                fadeOut(
                                    tween(160)
                                )
                            )
                        },
                        modifier =
                            Modifier.fillMaxSize(),
                        label =
                            "artworkLyrics"
                    ) {
                        lyricsVisible ->

                        if (
                            lyricsVisible
                        ) {
                            XvoxArtworkLyrics(
                                state =
                                    lyricsState,
                                position =
                                    position,
                                onAttach =
                                    lyricsViewModel::
                                        attach,
                                onClose = {
                                    showLyrics =
                                        false
                                },
                                onFullscreen =
                                    lyricsViewModel::
                                        openFullscreen,
                                modifier =
                                    Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(
                                        song.id
                                    ) {
                                        detectTapGestures(
                                            onTap = {
                                                showLyrics =
                                                    true
                                            }
                                        )
                                    }
                            ) {
                                XvoxNowPlayingArtworkPager(
                                    song =
                                        song,
                                    canPrevious =
                                        currentIndex > 0,
                                    canNext =
                                        currentIndex >=
                                            0 &&
                                            currentIndex <
                                            queueSize - 1,
                                    onPrevious =
                                        onPrevious,
                                    onNext =
                                        onNext,
                                    modifier =
                                        Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }

            /*
             * =================================================
             * BOTTOM PANEL
             * =================================================
             *
             * Lyrics no longer consume panel height.
             *
             * More transparent than previous version.
             *
             * Side/bottom outline intentionally absent.
             */
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
                        colors.background
                            .copy(
                                alpha = 0.56f
                            )
                    )
                    .windowInsetsPadding(
                        WindowInsets
                            .navigationBars
                    )
                    .padding(
                        start = 20.dp,
                        top = 17.dp,
                        end = 20.dp,
                        bottom = 8.dp
                    )
            ) {
                NowPlayingActions()

                androidx.compose.foundation.layout.Spacer(
                    modifier =
                        Modifier.size(
                            14.dp
                        )
                )

                Text(
                    text =
                        song.title,
                    color =
                        colors.primaryText,
                    fontSize = 21.sp,
                    lineHeight = 25.sp,
                    fontWeight =
                        FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow =
                        TextOverflow.Ellipsis
                )

                Text(
                    text =
                        song.artist,
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

                androidx.compose.foundation.layout.Spacer(
                    modifier =
                        Modifier.size(
                            9.dp
                        )
                )

                XvoxNowPlayingProgress(
                    position =
                        position,
                    duration =
                        duration,
                    onSeek =
                        onSeek
                )

                androidx.compose.foundation.layout.Spacer(
                    modifier =
                        Modifier.size(
                            6.dp
                        )
                )

                XvoxNowPlayingControls(
                    isPlaying =
                        isPlaying,
                    onShuffle = {},
                    onPrevious =
                        onPrevious,
                    onTogglePlay =
                        onTogglePlay,
                    onNext =
                        onNext,
                    onRepeat = {},
                    modifier =
                        Modifier.fillMaxWidth()
                )

                Text(
                    text = "XVOX",
                    color =
                        colors.secondaryText
                            .copy(
                                alpha = 0.56f
                            ),
                    fontFamily =
                        XvoxLogoFont,
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    letterSpacing =
                        1.4.sp,
                    modifier =
                        Modifier.align(
                            Alignment.CenterHorizontally
                        )
                )
            }
        }
    }
}

@Composable
private fun NowPlayingActions() {
    val colors =
        XvoxTheme.colors

    Row(
        modifier =
            Modifier.fillMaxWidth(),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        /*
         * Same 42dp height family as header-right capsule.
         * No border.
         */
        Row(
            modifier = Modifier
                .background(
                    colors.card.copy(
                        alpha = 0.31f
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
                resource =
                    R.drawable
                        .ic_xvox_timer
            )

            NowPlayingActionIcon(
                resource =
                    R.drawable
                        .ic_xvox_queue
            )

            NowPlayingActionIcon(
                resource =
                    R.drawable
                        .ic_xvox_info
            )
        }

        androidx.compose.foundation.layout.Spacer(
            modifier =
                Modifier.weight(1f)
        )

        NowPlayingCircleAction(
            resource =
                R.drawable
                    .ic_xvox_star
        )

        androidx.compose.foundation.layout.Spacer(
            modifier =
                Modifier.size(
                    10.dp
                )
        )

        NowPlayingCircleAction(
            resource =
                R.drawable
                    .ic_xvox_heart_outline
        )
    }
}

@Composable
private fun NowPlayingActionIcon(
    resource: Int
) {
    Box(
        modifier =
            Modifier.size(
                42.dp
            ),
        contentAlignment =
            Alignment.Center
    ) {
        Icon(
            painter =
                painterResource(
                    resource
                ),
            contentDescription =
                null,
            tint =
                XvoxTheme.colors
                    .primaryText,
            modifier =
                Modifier.size(
                    19.dp
                )
        )
    }
}

@Composable
private fun NowPlayingCircleAction(
    resource: Int
) {
    val colors =
        XvoxTheme.colors

    Box(
        modifier = Modifier
            .size(42.dp)
            .background(
                colors.card.copy(
                    alpha = 0.31f
                ),
                CircleShape
            ),
        contentAlignment =
            Alignment.Center
    ) {
        Icon(
            painter =
                painterResource(
                    resource
                ),
            contentDescription =
                null,
            tint =
                colors.primaryText,
            modifier =
                Modifier.size(
                    19.dp
                )
        )
    }
}
