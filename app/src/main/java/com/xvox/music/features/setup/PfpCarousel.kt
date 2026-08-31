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
import androidx.compose.ui.graphics.graphicsLayer
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
    val items = PfpType.entries
    val customIndex = items.indexOf(PfpType.CUSTOM)
    val state = rememberLazyListState()
    val colors = XvoxTheme.colors

    LaunchedEffect(state) {
        snapshotFlow {
            state.isScrollInProgress
        }
            .distinctUntilChanged()
            .filter { !it }
            .collect {
                val layout = state.layoutInfo

                val center =
                    (
                        layout.viewportStartOffset +
                            layout.viewportEndOffset
                        ) / 2

                val centeredItem =
                    layout.visibleItemsInfo.minByOrNull {
                        abs(
                            it.offset +
                                it.size / 2 -
                                center
                        )
                    }

                centeredItem?.index?.let { index ->
                    val type = items[index]

                    if (type != selected) {
                        onSelected(type)
                    }
                }
            }
    }

    LaunchedEffect(customPfpUri) {
        if (customPfpUri != null) {
            state.animateScrollToItem(
                index = customIndex
            )

            onSelected(PfpType.CUSTOM)
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth()
    ) {
        val itemWidth = 92.dp

        val edgePadding =
            ((maxWidth - itemWidth) / 2)
                .coerceAtLeast(0.dp)

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            LazyRow(
                state = state,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    horizontal = edgePadding
                ),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp),
                flingBehavior =
                    rememberSnapFlingBehavior(state)
            ) {
                itemsIndexed(
                    items = items,
                    key = { _, type ->
                        type.name
                    }
                ) { index, type ->

                    val scale by remember {
                        derivedStateOf {
                            val layout =
                                state.layoutInfo

                            val viewportCenter =
                                (
                                    layout.viewportStartOffset +
                                        layout.viewportEndOffset
                                    ) / 2f

                            val info =
                                layout.visibleItemsInfo
                                    .firstOrNull {
                                        it.index == index
                                    }

                            if (info == null) {
                                0.82f
                            } else {
                                val itemCenter =
                                    info.offset +
                                        info.size / 2f

                                val distance =
                                    abs(
                                        itemCenter -
                                            viewportCenter
                                    )

                                (
                                    1.12f -
                                        distance / 520f
                                    ).coerceIn(
                                    0.82f,
                                    1.12f
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier.size(itemWidth),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(66.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .clip(CircleShape)
                                .background(
                                    colors.cardElevated
                                )
                                .then(
                                    if (type == PfpType.CUSTOM) {
                                        Modifier.clickable {
                                            onSelected(
                                                PfpType.CUSTOM
                                            )
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
                                type == PfpType.DEFAULT -> {
                                    Text(
                                        text =
                                            username
                                                .trim()
                                                .firstOrNull()
                                                ?.uppercase()
                                                ?: "X",
                                        color =
                                            colors.primaryText,
                                        fontFamily =
                                            XvoxPersonalFont,
                                        fontSize = 31.sp,
                                        textAlign =
                                            TextAlign.Center
                                    )
                                }

                                type == PfpType.CUSTOM &&
                                    customPfpUri != null -> {
                                    AsyncImage(
                                        model = customPfpUri,
                                        contentDescription =
                                            "Custom profile picture",
                                        modifier = Modifier
                                            .size(66.dp)
                                            .clip(CircleShape),
                                        contentScale =
                                            ContentScale.Crop
                                    )
                                }

                                else -> {
                                    PfpIcon(
                                        type = type,
                                        color =
                                            colors.primaryText,
                                        modifier =
                                            Modifier.size(31.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(88.dp)
                    .border(
                        width = 2.dp,
                        color = colors.primaryAccent,
                        shape = CircleShape
                    )
            )
        }
    }

    Spacer(
        modifier = Modifier.height(7.dp)
    )

    Text(
        text = selected.label,
        modifier = Modifier.fillMaxWidth(),
        color = colors.secondaryText,
        fontSize = 12.sp,
        textAlign = TextAlign.Center
    )
}
