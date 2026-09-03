package com.xvox.music.player.nowplaying

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
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
    modifier: Modifier = Modifier
) {
    val colors =
        XvoxTheme.colors

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

    val totalOffset =
        (
            offset.value +
                dragY
            )
            .coerceAtLeast(0f)

    val dismissProgress =
        (
            totalOffset /
                screenHeight
            )
            .coerceIn(
                0f,
                1f
            )

    val corner =
        30f *
            dismissProgress

    LaunchedEffect(Unit) {
        offset.animateTo(
            0f,
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
                screenHeight,
                XvoxNowPlayingMotion.exit
            )

            onClose()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY =
                    totalOffset

                shape =
                    RoundedCornerShape(
                        corner.dp
                    )

                clip =
                    corner > 0f
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
                                    offset.snapTo(
                                        totalOffset
                                    )

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
                    onClose = {
                        dismiss()
                    },
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
                        top = 30.dp,
                        end = 12.dp,
                        bottom = 18.dp
                    ),
                contentAlignment =
                    Alignment.BottomCenter
            ) {
                XvoxNowPlayingArtworkPager(
                    song = song,
                    canPrevious =
                        currentIndex > 0,
                    canNext =
                        currentIndex >= 0 &&
                            currentIndex <
                            queueSize - 1,
                    onPrevious =
                        onPrevious,
                    onNext =
                        onNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )
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
                            alpha = 0.74f
                        )
                    )
                    .border(
                        0.65.dp,
                        colors.cardBorder,
                        RoundedCornerShape(
                            topStart = 28.dp,
                            topEnd = 28.dp
                        )
                    )
                    .windowInsetsPadding(
                        WindowInsets.navigationBars
                    )
                    .padding(
                        start = 20.dp,
                        top = 18.dp,
                        end = 20.dp,
                        bottom = 10.dp
                    )
            ) {
                NowPlayingActions()

                androidx.compose.foundation.layout.Spacer(
                    Modifier.size(15.dp)
                )

                Text(
                    text =
                        song.title,
                    color =
                        Color.White,
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
                        Color.White.copy(
                            alpha = 0.68f
                        ),
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    fontWeight =
                        FontWeight.Medium,
                    maxLines = 1,
                    overflow =
                        TextOverflow.Ellipsis
                )

                androidx.compose.foundation.layout.Spacer(
                    Modifier.size(10.dp)
                )

                XvoxNowPlayingLyricsPreview(
                    onLyricsSelected = {},
                    onFullscreen = {}
                )

                androidx.compose.foundation.layout.Spacer(
                    Modifier.size(9.dp)
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
                    Modifier.size(7.dp)
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
        Row(
            modifier = Modifier
                .background(
                    colors.card.copy(
                        alpha = 0.30f
                    ),
                    RoundedCornerShape(
                        24.dp
                    )
                )
                .border(
                    0.65.dp,
                    colors.cardBorder,
                    RoundedCornerShape(
                        24.dp
                    )
                )
                .padding(
                    horizontal = 4.dp
                )
        ) {
            ActionIcon(
                R.drawable.ic_xvox_timer
            )

            ActionIcon(
                R.drawable.ic_xvox_queue
            )

            ActionIcon(
                R.drawable.ic_xvox_info
            )
        }

        androidx.compose.foundation.layout.Spacer(
            Modifier.weight(1f)
        )

        CircleAction(
            R.drawable.ic_xvox_star
        )

        androidx.compose.foundation.layout.Spacer(
            Modifier.size(10.dp)
        )

        CircleAction(
            R.drawable
                .ic_xvox_heart_outline
        )
    }
}

@Composable
private fun ActionIcon(
    resource: Int
) {
    Box(
        modifier =
            Modifier.size(38.dp),
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
            tint = Color.White,
            modifier =
                Modifier.size(19.dp)
        )
    }
}

@Composable
private fun CircleAction(
    resource: Int
) {
    val colors =
        XvoxTheme.colors

    Box(
        modifier = Modifier
            .size(44.dp)
            .background(
                colors.card.copy(
                    alpha = 0.30f
                ),
                CircleShape
            )
            .border(
                0.65.dp,
                colors.cardBorder,
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
            tint = Color.White,
            modifier =
                Modifier.size(19.dp)
        )
    }
}
