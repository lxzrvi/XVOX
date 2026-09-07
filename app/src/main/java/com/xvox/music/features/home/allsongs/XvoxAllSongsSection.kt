package com.xvox.music.features.home.allsongs

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.horizontalScrollAxisRange
import androidx.compose.ui.semantics.verticalScrollAxisRange
import androidx.compose.ui.semantics.scrollBy
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.features.home.HomeGeometry
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun XvoxAllSongsSection(
    songs: List<Song>, currentSongId: Long?, isPlaying: Boolean,
    selectedSongIds: Set<Long> = emptySet(),
    onSongClick: (Song) -> Unit, onSongLongClick: (Song) -> Unit, onPrefetch: (Int) -> Unit
) {
    val colors = XvoxTheme.colors
    val context = LocalContext.current
    val prefs = remember { UserPreferencesRepository(context) }
    val style by prefs.homeLayoutStyle.collectAsState(initial = "mosaic")
    val direction by prefs.homeScrollDirection.collectAsState(initial = "horizontal")
    val rows by prefs.homeHorizontalRows.collectAsState(initial = 4)
    val uniform = style == "uniform"
    val vertical = direction == "vertical"
    val rowCount = rows.coerceIn(3, 8)
    val plans = remember(songs, rowCount, uniform) { buildMosaicPagePlans(songs, rowCount, uniform) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val containerHeight = LocalWindowInfo.current.containerSize.height
    val screenHeight = with(density) { containerHeight.toDp() }
    var panY by remember(songs, rowCount, uniform, vertical) { mutableFloatStateOf(0f) }
    var fling by remember { mutableStateOf<Job?>(null) }
    val velocity = remember { VelocityTracker() }

    LaunchedEffect(plans, vertical) {
        if (!vertical) snapshotFlow { listState.firstVisibleItemIndex }.distinctUntilChanged().collect { index ->
            plans.getOrNull(index + 1)?.let { onPrefetch(it.startIndex) }
        }
    }

    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = HomeGeometry.sectionGap),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(if (selectedSongIds.isEmpty()) "All Songs" else "${selectedSongIds.size} selected",
                color = colors.primaryAccent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("${songs.size} songs", color = colors.mutedText, fontSize = 10.sp)
        }
        if (songs.isEmpty()) {
            Text("No songs match your library filters", color = colors.secondaryText, fontSize = 13.sp,
                modifier = Modifier.padding(16.dp))
            return@Column
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val edge = 6.dp
            val gap = 6.dp
            val pageWidth = maxWidth - edge * 2
            val unitWidth = (pageWidth - gap * 3) / 4
            val unitHeight = unitWidth + 38.dp
            val pageHeight = unitHeight * rowCount + gap * (rowCount - 1)
            val viewport = minOf(pageHeight, maxOf(220.dp, screenHeight * 0.43f))
            val maxPanPx = with(density) { (pageHeight - viewport).toPx().coerceAtLeast(0f) }

            @Composable
            fun Page(plan: XvoxMosaicPagePlan, compact: Boolean) {
                val page = remember(songs, plan, rowCount, uniform) { buildMosaicPage(songs, plan, rowCount, uniform) }
                val usedRows = page.tiles.maxOfOrNull { it.y + it.height } ?: 0f
                val actualHeight = if (compact) unitHeight * usedRows + gap * (usedRows - 1).coerceAtLeast(0f) else pageHeight
                Box(Modifier.size(pageWidth, actualHeight)) {
                    page.tiles.forEach { tile ->
                        key(tile.song.id) {
                            val tileModifier = Modifier.offset((unitWidth + gap) * tile.x, (unitHeight + gap) * tile.y)
                                .size(unitWidth * tile.width + gap * (tile.width - 1), unitHeight * tile.height + gap * (tile.height - 1))
                            if (uniform) {
                                XvoxAllSongCard(tile.song, currentSongId == tile.song.id,
                                    currentSongId == tile.song.id && isPlaying,
                                    onClick = { onSongClick(tile.song) }, onLongClick = { onSongLongClick(tile.song) },
                                    modifier = tileModifier, selected = tile.song.id in selectedSongIds)
                            } else {
                                XvoxAllSongMosaicCard(tile.song, tile.width, tile.height,
                                    onClick = { onSongClick(tile.song) }, onLongClick = { onSongLongClick(tile.song) },
                                    modifier = tileModifier, current = currentSongId == tile.song.id,
                                    playing = currentSongId == tile.song.id && isPlaying,
                                    selected = tile.song.id in selectedSongIds, styleIndex = tile.style)
                            }
                        }
                    }
                }
            }

            if (vertical) {
                Column(Modifier.fillMaxWidth().padding(horizontal = edge), verticalArrangement = Arrangement.spacedBy(gap)) {
                    plans.forEach { plan -> key(plan.startIndex) { Page(plan, compact = true) } }
                }
            } else {
                // Both axes belong to this bounded canvas. Recents and the outer Home list never inherit its offset.
                LazyRow(
                    state = listState,
                    userScrollEnabled = false,
                    contentPadding = PaddingValues(horizontal = edge),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    modifier = Modifier.fillMaxWidth().height(viewport).clipToBounds()
                        .semantics {
                            horizontalScrollAxisRange = ScrollAxisRange({ listState.firstVisibleItemIndex.toFloat() }, { plans.size.toFloat() })
                            verticalScrollAxisRange = ScrollAxisRange({ -panY }, { maxPanPx })
                            scrollBy { x, y ->
                                scope.launch { listState.scrollBy(x); panY = (panY - y).coerceIn(-maxPanPx, 0f) }
                                true
                            }
                        }
                        .pointerInput(plans.size, maxPanPx) {
                            detectDragGestures(
                                onDragStart = { fling?.cancel(); velocity.resetTracking() },
                                onDragCancel = { velocity.resetTracking() },
                                onDragEnd = {
                                    val v = velocity.calculateVelocity()
                                    fling = scope.launch {
                                        launch horizontalFling@ {
                                            var previous = 0f
                                            Animatable(0f).animateDecay(-v.x, exponentialDecay()) {
                                                val delta = value - previous
                                                previous = value
                                                if (kotlin.math.abs(listState.dispatchRawDelta(delta)) < kotlin.math.abs(delta) * .5f) this@horizontalFling.cancel()
                                            }
                                        }
                                        launch verticalFling@ {
                                            Animatable(panY).animateDecay(v.y, exponentialDecay()) {
                                                panY = value.coerceIn(-maxPanPx, 0f)
                                                if (value != panY) this@verticalFling.cancel()
                                            }
                                        }
                                    }
                                },
                                onDrag = { change, delta ->
                                    change.consume()
                                    velocity.addPosition(change.uptimeMillis, change.position)
                                    listState.dispatchRawDelta(-delta.x)
                                    panY = (panY + delta.y).coerceIn(-maxPanPx, 0f)
                                }
                            )
                        }
                ) {
                    itemsIndexed(plans, key = { _, p -> p.startIndex }) { _, plan ->
                        // Layout height stays bounded; draw a taller page behind the local viewport.
                        Layout(content = { Page(plan, compact = false) }, modifier = Modifier.width(pageWidth).height(viewport)) { measurable, constraints ->
                            val child = measurable.single().measure(Constraints.fixed(constraints.maxWidth, pageHeight.roundToPx()))
                            layout(constraints.maxWidth, constraints.maxHeight) { child.placeRelative(0, panY.roundToInt()) }
                        }
                    }
                }
            }
        }
        if (!vertical) Text("Drag freely ↔ ↕ ↗ · Only All Songs moves", color = colors.mutedText,
            fontSize = 10.sp, modifier = Modifier.padding(start = 12.dp, top = 8.dp))
    }
}
