package com.xvox.music.features.setup

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.xvox.music.core.design.theme.XvoxPersonalFont
import com.xvox.music.core.design.theme.XvoxTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlin.math.abs

@Composable
fun PfpCarousel(
    selected: PfpType,
    username: String,
    customPfpUri: Uri?,
    onSelected: (PfpType) -> Unit,
    onAddClick: () -> Unit
) {
    val items =
        PfpType.entries

    val colors =
        XvoxTheme.colors

    val selectedIndex =
        items.indexOf(
            selected
        ).coerceAtLeast(0)

    val state =
        rememberLazyListState(
            initialFirstVisibleItemIndex =
                selectedIndex
        )

    val centeredIndex by
        remember {
            derivedStateOf {
                val layout =
                    state.layoutInfo

                if (
                    layout.visibleItemsInfo
                        .isEmpty()
                ) {
                    selectedIndex
                } else {
                    val center =
                        (
                            layout.viewportStartOffset +
                                layout.viewportEndOffset
                            ) / 2f

                    layout.visibleItemsInfo
                        .minByOrNull {
                            item ->

                            abs(
                                item.offset +
                                    item.size /
                                        2f -
                                    center
                            )
                        }
                        ?.index
                        ?.coerceIn(
                            0,
                            items.lastIndex
                        )
                        ?: selectedIndex
                }
            }
        }

    val centeredType =
        items.getOrElse(
            centeredIndex
        ) {
            PfpType.DEFAULT
        }

    LaunchedEffect(
        selected
    ) {
        val target =
            items.indexOf(
                selected
            )

        if (
            target >= 0 &&
            target !=
                centeredIndex &&
            !state.isScrollInProgress
        ) {
            state.animateScrollToItem(
                target
            )
        }
    }

    LaunchedEffect(state) {
        snapshotFlow {
            state.isScrollInProgress
        }
            .distinctUntilChanged()
            .filter {
                !it
            }
            .collect {
                val type =
                    items.getOrNull(
                        centeredIndex
                    )

                if (
                    type != null &&
                    type != selected
                ) {
                    onSelected(type)
                }
            }
    }

    BoxWithConstraints(
        modifier =
            Modifier.fillMaxWidth()
    ) {
        val slotWidth =
            122.dp

        val sidePfpSize =
            88.dp

        val centerPfpSize =
            102.dp

        val centerRingSize =
            116.dp

        val edgePadding =
            (
                (
                    maxWidth -
                        slotWidth
                    ) / 2
                )
                .coerceAtLeast(
                    0.dp
                )

        Box(
            modifier =
                Modifier.fillMaxWidth(),
            contentAlignment =
                Alignment.Center
        ) {
            LazyRow(
                state = state,
                modifier =
                    Modifier.fillMaxWidth(),
                contentPadding =
                    PaddingValues(
                        horizontal =
                            edgePadding
                    ),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        4.dp
                    ),
                flingBehavior =
                    rememberSnapFlingBehavior(
                        state
                    )
            ) {
                itemsIndexed(
                    items = items,
                    key = {
                            _,
                            type ->

                        type.name
                    }
                ) {
                    index,
                    type ->

                    val centered =
                        index ==
                            centeredIndex

                    val pfpSize =
                        if (centered) {
                            centerPfpSize
                        } else {
                            sidePfpSize
                        }

                    val customClickable =
                        type ==
                            PfpType.CUSTOM &&
                            centered &&
                            !state
                                .isScrollInProgress

                    Box(
                        modifier =
                            Modifier.size(
                                slotWidth
                            ),
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(
                                        pfpSize
                                    )
                                    .clip(
                                        CircleShape
                                    )
                                    .background(
                                        colors
                                            .cardElevated
                                    )
                                    .border(
                                        width =
                                            0.7.dp,
                                        color =
                                            colors
                                                .cardBorder,
                                        shape =
                                            CircleShape
                                    )
                                    .then(
                                        if (
                                            customClickable
                                        ) {
                                            Modifier
                                                .clickable {
                                                    onAddClick()
                                                }
                                        } else {
                                            Modifier
                                        }
                                    ),
                            contentAlignment =
                                Alignment.Center
                        ) {
                            when {
                                type ==
                                    PfpType.DEFAULT -> {

                                    Text(
                                        text =
                                            username
                                                .trim()
                                                .firstOrNull()
                                                ?.uppercase()
                                                ?: "X",
                                        color =
                                            colors
                                                .primaryText,
                                        fontFamily =
                                            XvoxPersonalFont,
                                        fontSize =
                                            if (
                                                centered
                                            ) {
                                                45.sp
                                            } else {
                                                39.sp
                                            },
                                        textAlign =
                                            TextAlign.Center
                                    )
                                }

                                type ==
                                    PfpType.CUSTOM &&
                                    customPfpUri !=
                                    null -> {

                                    AsyncImage(
                                        model =
                                            customPfpUri,
                                        contentDescription =
                                            "Custom profile picture",
                                        contentScale =
                                            ContentScale.Crop,
                                        modifier =
                                            Modifier
                                                .size(
                                                    pfpSize
                                                )
                                                .clip(
                                                    CircleShape
                                                )
                                    )
                                }

                                else -> {
                                    PfpIcon(
                                        type =
                                            type,
                                        color =
                                            colors
                                                .primaryText,
                                        modifier =
                                            Modifier
                                                .size(
                                                    if (
                                                        centered
                                                    ) {
                                                        48.dp
                                                    } else {
                                                        42.dp
                                                    }
                                                )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Box(
                modifier =
                    Modifier
                        .size(
                            centerRingSize
                        )
                        .border(
                            width = 2.dp,
                            color =
                                colors
                                    .primaryAccent,
                            shape =
                                CircleShape
                        )
            )
        }
    }

    Spacer(
        modifier =
            Modifier.height(
                8.dp
            )
    )

    Text(
        text =
            centeredType.label,
        modifier =
            Modifier.fillMaxWidth(),
        color =
            colors.secondaryText,
        fontSize = 12.sp,
        textAlign =
            TextAlign.Center
    )
}
