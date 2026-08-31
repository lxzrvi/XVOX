package com.xvox.music.features.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.math.abs

@Composable
fun PfpCarousel(
    selected: PfpType,
    username: String,
    onSelected: (PfpType) -> Unit
) {
    val items = PfpType.entries
    val state = rememberLazyListState()

    LaunchedEffect(state) {
        snapshotFlow { state.layoutInfo }
            .map { layout ->
                if (layout.visibleItemsInfo.isEmpty()) {
                    null
                } else {
                    val center =
                        (
                            layout.viewportStartOffset +
                                layout.viewportEndOffset
                            ) / 2

                    layout.visibleItemsInfo.minByOrNull {
                        abs(
                            (
                                it.offset +
                                    it.size / 2
                                ) - center
                        )
                    }?.index
                }
            }
            .distinctUntilChanged()
            .collect { index ->
                index?.let {
                    onSelected(items[it])
                }
            }
    }

    val colors = XvoxTheme.colors

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth()
    ) {
        val itemWidth = 74.dp
        val edgePadding =
            (maxWidth - itemWidth) / 2

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
                    Arrangement.spacedBy(10.dp),
                flingBehavior =
                    rememberSnapFlingBehavior(state)
            ) {
                itemsIndexed(items) { _, type ->
                    Box(
                        modifier = Modifier.size(itemWidth),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(colors.cardElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = pfpContent(
                                    type = type,
                                    username = username
                                ),
                                color = colors.primaryText,
                                fontSize = 25.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(74.dp)
                    .border(
                        width = 2.dp,
                        color = colors.primaryAccent,
                        shape = CircleShape
                    )
            )
        }
    }

    Text(
        text = selected.label,
        modifier = Modifier.fillMaxWidth(),
        color = colors.secondaryText,
        fontSize = 12.sp,
        textAlign = TextAlign.Center
    )
}

private fun pfpContent(
    type: PfpType,
    username: String
): String {
    return when (type) {
        PfpType.DEFAULT ->
            username
                .trim()
                .firstOrNull()
                ?.uppercase()
                ?: "X"

        PfpType.HEART -> "♥"
        PfpType.STAR -> "★"
        PfpType.CIRCLE -> "●"
        PfpType.DIAMOND -> "◆"
        PfpType.HEXAGON -> "⬢"
        PfpType.CUSTOM -> "+"
    }
}
