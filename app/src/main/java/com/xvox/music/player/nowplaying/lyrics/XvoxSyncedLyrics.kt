package com.xvox.music.player.nowplaying.lyrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.player.nowplaying.lyrics.XvoxLyricLine
import kotlin.math.abs

@Composable
fun XvoxSyncedLyrics(
    lyrics: XvoxLyrics,
    position: Long,
    modifier: Modifier = Modifier
) {
    val activeIndex by remember(
        lyrics,
        position
    ) {
        derivedStateOf {
            if (
                !lyrics.synchronized
            ) {
                0
            } else {
                lyrics.lines
                    .indexOfLast {
                        line ->

                        (
                            line.timeMs
                                ?: Long.MAX_VALUE
                            ) <=
                            position
                    }
                    .coerceAtLeast(0)
            }
        }
    }

    if (
        lyrics.lines.isEmpty()
    ) {
        return
    }

    if (
        !lyrics.synchronized
    ) {
        PlainLyrics(
            lines =
                lyrics.lines,
            modifier =
                modifier
        )

        return
    }

    Box(
        modifier =
            modifier.fillMaxSize(),
        contentAlignment =
            Alignment.Center
    ) {
        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.spacedBy(
                    17.dp
                )
        ) {
            for (
                offset in -3..3
            ) {
                val index =
                    activeIndex +
                        offset

                val line =
                    lyrics.lines
                        .getOrNull(
                            index
                        )
                        ?: continue

                val distance =
                    abs(offset)

                val alpha =
                    when (distance) {
                        0 -> 1f
                        1 -> 0.68f
                        2 -> 0.36f
                        else -> 0.16f
                    }

                val fontSize =
                    if (
                        distance == 0
                    ) {
                        21.sp
                    } else {
                        16.sp
                    }

                Text(
                    text =
                        line.text
                            .ifBlank {
                                "♪"
                            },
                    color =
                        Color.White.copy(
                            alpha = alpha
                        ),
                    fontSize =
                        fontSize,
                    lineHeight =
                        if (
                            distance == 0
                        ) {
                            27.sp
                        } else {
                            21.sp
                        },
                    fontWeight =
                        if (
                            distance == 0
                        ) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Medium
                        },
                    textAlign =
                        TextAlign.Center,
                    modifier =
                        Modifier.padding(
                            horizontal = 20.dp
                        )
                )
            }
        }
    }
}

@Composable
private fun PlainLyrics(
    lines: List<XvoxLyricLine>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(
                    horizontal = 20.dp,
                    vertical = 34.dp
                ),
        verticalArrangement =
            Arrangement.Center,
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {
        lines
            .take(8)
            .forEach {
                line ->

                Text(
                    text =
                        line.text,
                    color =
                        Color.White.copy(
                            alpha = 0.84f
                        ),
                    fontSize =
                        16.sp,
                    lineHeight =
                        22.sp,
                    textAlign =
                        TextAlign.Center
                )
            }
    }
}
