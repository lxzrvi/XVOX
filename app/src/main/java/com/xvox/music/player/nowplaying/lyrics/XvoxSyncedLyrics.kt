package com.xvox.music.player.nowplaying.lyrics

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
fun XvoxSyncedLyrics(
    lyrics: XvoxLyrics,
    position: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    strongEdgeFade: Boolean = false
) {
    if (lyrics.lines.isEmpty()) return

    val activeIndex =
        if (lyrics.synchronized) {
            lyrics.lines
                .indexOfLast {
                    (it.timeMs ?: Long.MAX_VALUE) <=
                        position
                }
                .coerceAtLeast(0)
        } else {
            -1
        }

    val listState =
        rememberLazyListState()

    var userBrowsing by remember {
        mutableStateOf(false)
    }

    var autoFollowing by remember {
        mutableStateOf(false)
    }

    var interactionToken by remember {
        mutableLongStateOf(0L)
    }

    LaunchedEffect(
        listState.isScrollInProgress,
        autoFollowing
    ) {
        if (
            listState.isScrollInProgress &&
            !autoFollowing
        ) {
            userBrowsing = true
            interactionToken++
        } else if (
            !listState.isScrollInProgress &&
            userBrowsing &&
            !autoFollowing
        ) {
            val token =
                ++interactionToken

            delay(4_000L)

            if (
                token == interactionToken &&
                !listState.isScrollInProgress
            ) {
                userBrowsing = false
            }
        }
    }

    LaunchedEffect(
        activeIndex,
        userBrowsing,
        lyrics
    ) {
        if (
            !lyrics.synchronized ||
            activeIndex < 0 ||
            userBrowsing
        ) {
            return@LaunchedEffect
        }

        autoFollowing = true

        try {
            withFrameNanos { }

            centerLyricExactly(
                state = listState,
                lazyIndex = activeIndex + 1
            )
        } finally {
            autoFollowing = false
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val boundarySpace =
            maxHeight / 2

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    compositingStrategy =
                        CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    drawContent()

                    val stops =
                        if (strongEdgeFade) {
                            arrayOf(
                                0.00f to Color.Transparent,
                                0.07f to Color.White.copy(
                                    alpha = 0.04f
                                ),
                                0.15f to Color.White.copy(
                                    alpha = 0.14f
                                ),
                                0.25f to Color.White.copy(
                                    alpha = 0.38f
                                ),
                                0.37f to Color.White.copy(
                                    alpha = 0.78f
                                ),
                                0.45f to Color.White,
                                0.55f to Color.White,
                                0.63f to Color.White.copy(
                                    alpha = 0.78f
                                ),
                                0.75f to Color.White.copy(
                                    alpha = 0.38f
                                ),
                                0.85f to Color.White.copy(
                                    alpha = 0.14f
                                ),
                                0.93f to Color.White.copy(
                                    alpha = 0.04f
                                ),
                                1.00f to Color.Transparent
                            )
                        } else {
                            arrayOf(
                                0.00f to Color.White.copy(
                                    alpha = 0.03f
                                ),
                                0.09f to Color.White.copy(
                                    alpha = 0.12f
                                ),
                                0.20f to Color.White.copy(
                                    alpha = 0.34f
                                ),
                                0.33f to Color.White.copy(
                                    alpha = 0.70f
                                ),
                                0.43f to Color.White,
                                0.57f to Color.White,
                                0.67f to Color.White.copy(
                                    alpha = 0.70f
                                ),
                                0.80f to Color.White.copy(
                                    alpha = 0.34f
                                ),
                                0.91f to Color.White.copy(
                                    alpha = 0.12f
                                ),
                                1.00f to Color.White.copy(
                                    alpha = 0.03f
                                )
                            )
                        }

                    drawRect(
                        brush =
                            Brush.verticalGradient(
                                colorStops = stops
                            ),
                        blendMode =
                            BlendMode.DstIn
                    )
                }
        ) {
            item(
                key = "lyrics-top"
            ) {
                Spacer(
                    Modifier.height(
                        boundarySpace
                    )
                )
            }

            itemsIndexed(
                items = lyrics.lines,
                key = { index, line ->
                    "$index-${line.timeMs}-${line.text}"
                }
            ) { index, line ->
                val active =
                    lyrics.synchronized &&
                        index == activeIndex

                val lineColor by
                    animateColorAsState(
                        targetValue =
                            if (active) {
                                Color.White
                            } else {
                                Color.White.copy(
                                    alpha =
                                        if (
                                            strongEdgeFade
                                        ) {
                                            0.58f
                                        } else {
                                            0.68f
                                        }
                                )
                            },
                        animationSpec =
                            tween(110),
                        label =
                            "xvoxLyric$index"
                    )

                Text(
                    text =
                        line.text.ifBlank {
                            "♪"
                        },
                    color = lineColor,
                    fontSize =
                        when {
                            strongEdgeFade &&
                                active ->
                                25.sp

                            strongEdgeFade ->
                                18.sp

                            active ->
                                22.sp

                            else ->
                                17.sp
                        },
                    lineHeight =
                        when {
                            strongEdgeFade &&
                                active ->
                                33.sp

                            strongEdgeFade ->
                                26.sp

                            active ->
                                30.sp

                            else ->
                                24.sp
                        },
                    fontWeight =
                        if (active) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        },
                    textAlign =
                        TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            enabled =
                                line.timeMs != null,
                            interactionSource =
                                remember {
                                    MutableInteractionSource()
                                },
                            indication = null
                        ) {
                            line.timeMs?.let(
                                onSeek
                            )
                        }
                        .padding(
                            horizontal =
                                if (
                                    strongEdgeFade
                                ) {
                                    30.dp
                                } else {
                                    20.dp
                                },
                            vertical =
                                if (active) {
                                    10.dp
                                } else {
                                    7.dp
                                }
                        )
                )
            }

            item(
                key = "lyrics-bottom"
            ) {
                Spacer(
                    Modifier.height(
                        boundarySpace
                    )
                )
            }
        }
    }
}

private suspend fun centerLyricExactly(
    state: LazyListState,
    lazyIndex: Int
) {
    var target =
        state.layoutInfo
            .visibleItemsInfo
            .firstOrNull {
                it.index == lazyIndex
            }

    if (target == null) {
        state.scrollToItem(lazyIndex)
        withFrameNanos { }

        target =
            state.layoutInfo
                .visibleItemsInfo
                .firstOrNull {
                    it.index == lazyIndex
                }
                ?: return
    }

    fun correction(): Float? {
        val layout =
            state.layoutInfo

        val item =
            layout.visibleItemsInfo
                .firstOrNull {
                    it.index == lazyIndex
                }
                ?: return null

        val viewportCenter =
            (
                layout.viewportStartOffset +
                    layout.viewportEndOffset
                ) / 2f

        val itemCenter =
            item.offset +
                item.size / 2f

        return itemCenter -
            viewportCenter
    }

    val first =
        correction() ?: return

    if (abs(first) > 0.5f) {
        state.animateScrollBy(
            value = first,
            animationSpec =
                tween(
                    durationMillis = 190,
                    easing =
                        FastOutSlowInEasing
                )
        )
    }

    withFrameNanos { }

    val final =
        correction() ?: return

    if (abs(final) > 0.75f) {
        state.scrollBy(final)
    }
}
