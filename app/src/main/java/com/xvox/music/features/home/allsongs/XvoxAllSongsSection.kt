package com.xvox.music.features.home.allsongs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.features.home.HomeGeometry
import com.xvox.music.core.model.Song
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun XvoxAllSongsSection(
    songs: List<Song>,
    currentSongId: Long?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    onSongLongClick: (Song) -> Unit,
    onPrefetch: (Int) -> Unit,
) {
    val colors =
        XvoxTheme.colors

    val plans =
        remember(songs) {
            buildMosaicPagePlans(
                songs,
            )
        }

    val state =
        rememberLazyListState()

    LaunchedEffect(
        state,
        plans,
    ) {
        delay(300L)

        snapshotFlow {
            state.firstVisibleItemIndex
        }.distinctUntilChanged()
            .collect { pageIndex ->

                val target =
                    plans.getOrNull(
                        pageIndex + 1,
                    )
                        ?: return@collect

                onPrefetch(
                    target.startIndex,
                )
            }
    }

    Column(
        modifier =
            Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 12.dp,
                        end = 12.dp,
                        bottom =
                            HomeGeometry
                                .sectionGap,
                    ),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment =
                Alignment.CenterVertically,
        ) {
            Text(
                text = "All Songs",
                color =
                    colors.primaryAccent,
                fontSize = 16.sp,
                lineHeight = 19.sp,
                fontWeight =
                    FontWeight.SemiBold,
            )

            Text(
                text =
                    "Total ${songs.size} songs",
                color =
                    colors.mutedText,
                fontSize = 9.sp,
            )
        }

        BoxWithConstraints(
            modifier =
                Modifier.fillMaxWidth(),
        ) {
            val edge =
                6.dp

            val gap =
                6.dp

            val contentWidth =
                maxWidth - edge * 2

            val unitWidth =
                (
                    contentWidth -
                        gap * 3
                ) / 4

            val unitHeight =
                unitWidth +
                    38.dp

            val pageHeight =
                unitHeight * 3 +
                    gap * 2

            LazyRow(
                state = state,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            pageHeight,
                        ),
                contentPadding =
                    PaddingValues(
                        horizontal = edge,
                    ),
                horizontalArrangement =
                    Arrangement.spacedBy(gap),
            ) {
                itemsIndexed(
                    items = plans,
                    key = {
                        _,
                        plan,
                        ->

                        plan.startIndex
                    },
                    contentType = {
                        _,
                        _,
                        ->

                        "mosaic_page"
                    },
                ) {
                    _,
                    plan,
                    ->

                    val page =
                        remember(
                            songs,
                            plan,
                        ) {
                            buildMosaicPage(
                                songs,
                                plan,
                            )
                        }

                    Box(
                        modifier =
                            Modifier.size(
                                width =
                                contentWidth,
                                height =
                                pageHeight,
                            ),
                    ) {
                        page.tiles
                            .forEach { tile ->

                                val tileWidth =
                                    unitWidth *
                                        tile.width +
                                        gap *
                                        (
                                            tile.width -
                                                1f
                                        )

                                val tileHeight =
                                    unitHeight *
                                        tile.height +
                                        gap *
                                        (
                                            tile.height -
                                                1f
                                        )

                                val x =
                                    (
                                        unitWidth +
                                            gap
                                    ) *
                                        tile.x

                                val y =
                                    (
                                        unitHeight +
                                            gap
                                    ) *
                                        tile.y

                                val tileModifier =
                                    Modifier
                                        .offset(
                                            x = x,
                                            y = y,
                                        ).size(
                                            width =
                                            tileWidth,
                                            height =
                                            tileHeight,
                                        )

                                if (
                                    tile.width ==
                                    1f &&
                                    tile.height ==
                                    1f
                                ) {
                                    XvoxAllSongCard(
                                        song =
                                            tile.song,
                                        current =
                                            currentSongId ==
                                                tile.song.id,
                                        playing =
                                            currentSongId ==
                                                tile.song.id &&
                                                isPlaying,
                                        onClick = {
                                            onSongClick(
                                                tile.song,
                                            )
                                        },
                                        onLongClick = {
                                            onSongLongClick(
                                                tile.song,
                                            )
                                        },
                                        modifier =
                                        tileModifier,
                                    )
                                } else {
                                    XvoxAllSongMosaicCard(
                                        song =
                                            tile.song,
                                        widthUnits =
                                            tile.width,
                                        heightUnits =
                                            tile.height,
                                        onClick = {
                                            onSongClick(
                                                tile.song,
                                            )
                                        },
                                        onLongClick = {
                                            onSongLongClick(
                                                tile.song,
                                            )
                                        },
                                        modifier =
                                        tileModifier,
                                    )
                                }
                            }
                    }
                }
            }
        }
    }
}
