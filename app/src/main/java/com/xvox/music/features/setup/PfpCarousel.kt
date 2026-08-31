package com.xvox.music.features.setup

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxPersonalFont
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
    val colors = XvoxTheme.colors

    LaunchedEffect(state) {
        snapshotFlow { state.layoutInfo }
            .map { layout ->
                val center =
                    (
                        layout.viewportStartOffset +
                            layout.viewportEndOffset
                        ) / 2

                layout.visibleItemsInfo.minByOrNull {
                    abs(
                        it.offset +
                            it.size / 2 -
                            center
                    )
                }?.index
            }
            .distinctUntilChanged()
            .collect { index ->
                index?.let {
                    onSelected(items[it])
                }
            }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth()
    ) {
        val itemWidth = 84.dp
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
                    Arrangement.spacedBy(8.dp),
                flingBehavior =
                    rememberSnapFlingBehavior(state)
            ) {
                itemsIndexed(items) { _, type ->
                    val centered =
                        type == selected

                    val scale =
                        animateFloatAsState(
                            targetValue =
                                if (centered) 1.18f else 0.82f,
                            animationSpec = spring(
                                dampingRatio = 0.7f,
                                stiffness = 450f
                            ),
                            label = "pfpScale"
                        )

                    Box(
                        modifier = Modifier.size(itemWidth),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .graphicsLayer {
                                    scaleX = scale.value
                                    scaleY = scale.value
                                }
                                .clip(CircleShape)
                                .background(
                                    colors.cardElevated
                                ),
                            contentAlignment =
                                Alignment.Center
                        ) {
                            if (type == PfpType.DEFAULT) {
                                Text(
                                    text =
                                        username
                                            .trim()
                                            .firstOrNull()
                                            ?.uppercase()
                                            ?: "X",
                                    color = colors.primaryText,
                                    fontFamily =
                                        XvoxPersonalFont,
                                    fontSize = 29.sp,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                PfpIcon(
                                    type = type,
                                    color =
                                        colors.primaryText,
                                    modifier =
                                        Modifier.size(30.dp)
                                )
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(84.dp)
                    .border(
                        width = 2.dp,
                        color = colors.primaryAccent,
                        shape = CircleShape
                    )
            )
        }
    }

    Spacer(Modifier.height(7.dp))

    Text(
        text = selected.label,
        modifier = Modifier.fillMaxWidth(),
        color = colors.secondaryText,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center
    )
}
