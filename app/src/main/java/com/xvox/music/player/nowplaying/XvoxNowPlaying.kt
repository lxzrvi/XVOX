package com.xvox.music.player.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.model.Song
import com.xvox.music.features.home.SongArtwork
import com.xvox.music.features.home.RecentArtworkSize

@Composable
fun XvoxNowPlaying(
    song: Song,
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
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var drag = 0f

                detectVerticalDragGestures(
                    onDragStart = {
                        drag = 0f
                    },
                    onVerticalDrag = {
                        change,
                        amount ->

                        change.consume()
                        drag += amount
                    },
                    onDragEnd = {
                        if (drag > 80f) {
                            onClose()
                        }
                    }
                )
            }
    ) {
        XvoxNowPlayingBackdrop()

        Column(
            modifier =
                Modifier.fillMaxSize()
        ) {
            XvoxNowPlayingHeader(
                onClose = onClose,
                onShare = {},
                onMore = {}
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(
                        horizontal = 12.dp,
                        top = 18.dp,
                        bottom = 28.dp
                    ),
                contentAlignment =
                    Alignment.Center
            ) {
                SongArtwork(
                    artwork =
                        song.artworkUri,
                    requestSize =
                        RecentArtworkSize,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(
                            RoundedCornerShape(
                                20.dp
                            )
                        )
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
                        Color(0xFF120805)
                            .copy(
                                alpha = 0.82f
                            )
                    )
                    .windowInsetsPadding(
                        WindowInsets
                            .navigationBars
                    )
                    .padding(
                        start = 20.dp,
                        end = 20.dp,
                        top = 20.dp,
                        bottom = 12.dp
                    )
            ) {
                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(
                                RoundedCornerShape(
                                    22.dp
                                )
                            )
                            .background(
                                Color.White.copy(
                                    alpha = 0.09f
                                )
                            )
                            .padding(
                                horizontal = 5.dp
                            )
                    ) {
                        SmallTextAction("◷")
                        SmallVectorAction(
                            R.drawable
                                .ic_xvox_queue
                        )
                        SmallTextAction("ⓘ")
                    }

                    androidx.compose.foundation.layout.Spacer(
                        Modifier.weight(1f)
                    )

                    CircleAction(
                        resource =
                            R.drawable
                                .ic_xvox_add
                    )

                    androidx.compose.foundation.layout.Spacer(
                        Modifier.size(10.dp)
                    )

                    CircleAction(
                        resource =
                            R.drawable
                                .ic_xvox_heart
                    )
                }

                androidx.compose.foundation.layout.Spacer(
                    Modifier.size(20.dp)
                )

                Text(
                    text = song.title,
                    color = Color.White,
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
                            alpha = 0.65f
                        ),
                    fontSize = 13.sp,
                    fontWeight =
                        FontWeight.Medium,
                    maxLines = 1,
                    overflow =
                        TextOverflow.Ellipsis
                )

                androidx.compose.foundation.layout.Spacer(
                    Modifier.size(14.dp)
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
                    Modifier.size(14.dp)
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
                        Color.White.copy(
                            alpha = 0.45f
                        ),
                    fontSize = 11.sp,
                    letterSpacing =
                        2.sp,
                    fontWeight =
                        FontWeight.Black,
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
private fun CircleAction(
    resource: Int
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(
                Color.White.copy(
                    alpha = 0.09f
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
            contentDescription = null,
            tint = Color.White,
            modifier =
                Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SmallVectorAction(
    resource: Int
) {
    Box(
        modifier =
            Modifier.size(34.dp),
        contentAlignment =
            Alignment.Center
    ) {
        Icon(
            painter =
                painterResource(
                    resource
                ),
            contentDescription = null,
            tint = Color.White,
            modifier =
                Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SmallTextAction(
    value: String
) {
    Box(
        modifier =
            Modifier.size(34.dp),
        contentAlignment =
            Alignment.Center
    ) {
        Text(
            text = value,
            color = Color.White,
            fontSize = 18.sp
        )
    }
}
