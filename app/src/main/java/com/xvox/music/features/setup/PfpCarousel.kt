package com.xvox.music.features.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
fun PfpCarousel(
    selected: PfpType,
    onSelected: (PfpType) -> Unit,
    onCustomClick: () -> Unit
) {
    val items = PfpType.entries
    val state = rememberLazyListState(
        initialFirstVisibleItemIndex = items.indexOf(selected)
    )

    LaunchedEffect(state) {
        snapshotFlow { state.layoutInfo }
            .map { layout ->
                val center =
                    (layout.viewportStartOffset + layout.viewportEndOffset) / 2

                layout.visibleItemsInfo.minByOrNull {
                    kotlin.math.abs(
                        (it.offset + it.size / 2) - center
                    )
                }?.index
            }
            .distinctUntilChanged()
            .collect { index ->
                if (index != null) {
                    val item = items[index]
                    if (item != PfpType.CUSTOM) {
                        onSelected(item)
                    }
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        LazyRow(
            state = state,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 132.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            flingBehavior = rememberSnapFlingBehavior(state)
        ) {
            itemsIndexed(items) { index, type ->
                val isCentered = type == selected
                val size = if (isCentered) 74.dp else 56.dp

                Box(
                    modifier = Modifier
                        .size(82.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(size)
                            .clip(CircleShape)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable {
                                if (type == PfpType.CUSTOM) {
                                    onCustomClick()
                                } else {
                                    onSelected(type)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = pfpSymbol(type),
                            color = Color.White,
                            fontSize = if (isCentered) 30.sp else 23.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .size(82.dp)
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                )
        )
    }
}

private fun pfpSymbol(type: PfpType): String {
    return when (type) {
        PfpType.HEART -> "♥"
        PfpType.STAR -> "★"
        PfpType.CIRCLE -> "●"
        PfpType.DIAMOND -> "◆"
        PfpType.HEXAGON -> "⬢"
        PfpType.CUSTOM -> "+"
    }
}
