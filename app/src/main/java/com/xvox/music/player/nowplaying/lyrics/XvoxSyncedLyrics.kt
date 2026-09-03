package com.xvox.music.player.nowplaying.lyrics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
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

    val listState =
        rememberLazyListState()

    val activeIndex =
        if (lyrics.synchronized) {
            lyrics.lines.indexOfLast {
                (it.timeMs ?: Long.MAX_VALUE) <=
                    position
            }.coerceAtLeast(0)
        } else {
            -1
        }

    var lastInteraction by remember {
        mutableLongStateOf(0L)
    }

    LaunchedEffect(
        listState.isScrollInProgress
    ) {
        if (listState.isScrollInProgress) {
            lastInteraction =
                System.currentTimeMillis()
        } else if (
            lyrics.synchronized &&
            activeIndex >= 0 &&
            lastInteraction > 0L
        ) {
            delay(3000L)

            if (
                !listState.isScrollInProgress &&
                System.currentTimeMillis() -
                    lastInteraction >= 2900L
            ) {
                listState.centerLine(
                    activeIndex
                )
                lastInteraction = 0L
            }
        }
    }

    LaunchedEffect(
        activeIndex,
        lyrics
    ) {
        if (
            lyrics.synchronized &&
            activeIndex >= 0 &&
            !listState.isScrollInProgress &&
            lastInteraction == 0L
        ) {
            listState.centerLine(
                activeIndex
            )
        }
    }

    BoxWithConstraints(
        modifier = modifier
    ) {
        val verticalSpace =
            maxHeight / 2

        LazyColumn(
            state = listState,
            modifier =
                Modifier.matchParentSize()
        ) {
            item {
                Spacer(
                    Modifier.height(
                        verticalSpace
                    )
                )
            }

            itemsIndexed(
                lyrics.lines,
                key = {
                    index,
                    line ->

                    "$index-${line.timeMs}"
                }
            ) {
                index,
                line ->

                val active =
                    index == activeIndex

                val alpha =
                    lineAlpha(
                        index = index,
                        activeIndex =
                            activeIndex,
                        listState =
                            listState,
                        strong =
                            strongEdgeFade
                    )

                Text(
                    text =
                        line.text.ifBlank {
                            "♪"
                        },
                    color =
                        Color.White.copy(
                            alpha = alpha
                        ),
                    fontSize =
                        if (active) {
                            22.sp
                        } else {
                            17.sp
                        },
                    lineHeight =
                        if (active) {
                            29.sp
                        } else {
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
                        .graphicsLayer {
                            this.alpha = alpha
                        }
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
                            horizontal = 18.dp,
                            vertical = 10.dp
                        )
                )
            }

            item {
                Spacer(
                    Modifier.height(
                        verticalSpace
                    )
                )
            }
        }
    }
}

private suspend fun LazyListState.centerLine(
    lyricIndex: Int
) {
    animateScrollToItem(
        index = lyricIndex + 1,
        scrollOffset =
            -layoutInfo.viewportSize.height /
                2
    )
}

private fun lineAlpha(
    index: Int,
    activeIndex: Int,
    listState: LazyListState,
    strong: Boolean
): Float {
    if (activeIndex < 0) {
        val visible =
            listState.layoutInfo
                .visibleItemsInfo
                .firstOrNull {
                    it.index == index + 1
                }
                ?: return 0.18f

        val viewport =
            listState.layoutInfo
                .viewportSize.height
                .coerceAtLeast(1)

        val center =
            visible.offset +
                visible.size / 2f

        val distance =
            abs(
                center -
                    viewport / 2f
            ) / (viewport / 2f)

        return (
            1f -
                distance *
                if (strong) 0.92f else 0.78f
            )
            .coerceIn(
                if (strong) 0.06f else 0.16f,
                0.90f
            )
    }

    val distance =
        abs(index - activeIndex)

    return if (strong) {
        when (distance) {
            0 -> 1f
            1 -> 0.70f
            2 -> 0.45f
            3 -> 0.27f
            4 -> 0.14f
            else -> 0.07f
        }
    } else {
        when (distance) {
            0 -> 1f
            1 -> 0.78f
            2 -> 0.57f
            3 -> 0.39f
            4 -> 0.26f
            else -> 0.14f
        }
    }
}
