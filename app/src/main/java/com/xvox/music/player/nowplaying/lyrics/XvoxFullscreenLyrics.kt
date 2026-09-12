package com.xvox.music.player.nowplaying.lyrics

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil3.compose.AsyncImage
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.chrome.LocalXvoxChromeStyle
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.data.preferences.LyricsSettings
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.features.home.XvoxSongArtwork
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun XvoxFullscreenLyrics(
    song: Song,
    state: XvoxLyricsUiState,
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    backgroundColor: Color = Color.Transparent,
    onSeek: (Long) -> Unit,
    onAttach: (Uri) -> Unit,
    onDelete: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    onOpenSettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = XvoxTheme.colors
    val chrome = LocalXvoxChromeStyle.current
    val haptics = LocalXvoxHaptics.current
    val overlays = LocalXvoxOverlayController.current
    val prefs = remember(context) { UserPreferencesRepository(context) }
    val scope = rememberCoroutineScope()

    val headerPhotoUri by prefs.headerImageUri.collectAsState(initial = null)
    val lyricsSettings by prefs.lyricsSettings.collectAsState(initial = LyricsSettings())
    val gradientAnim = lyricsSettings.gradientAnimation

    val view = LocalView.current
    val statusBarPx = remember(view) {
        val window = (view.context as? android.app.Activity)?.window
        if (window == null) 0
        else WindowInsetsCompat.toWindowInsetsCompat(view.rootWindowInsets, view)
            .getInsets(WindowInsetsCompat.Type.statusBars()).top
    }
    var chromeVisible by remember { mutableStateOf(true) }
    var barsVisible by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    fun pingInteraction() {
        lastInteractionTime = System.currentTimeMillis()
        chromeVisible = true
    }

    val animatedBarsPad by animateDpAsState(
        targetValue = if (barsVisible) with(LocalDensity.current) { statusBarPx.toDp() } else 0.dp,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "fullscreenBarsPad"
    )

    DisposableEffect(Unit) {
        val window = (view.context as? android.app.Activity)?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, view) }
        insetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        val hideRunnable = Runnable {
            barsVisible = false
            insetsController?.hide(WindowInsetsCompat.Type.systemBars())
        }
        view.postDelayed(hideRunnable, 120)
        onDispose {
            view.removeCallbacks(hideRunnable)
            barsVisible = true
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onAttach(uri)
            overlays.showP("Custom lyrics applied")
        }
    }

    val baseColor = if (backgroundColor != Color.Transparent) backgroundColor else colors.surface

    val lyricColor = remember(baseColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(
            android.graphics.Color.rgb(
                (baseColor.red * 255).toInt(),
                (baseColor.green * 255).toInt(),
                (baseColor.blue * 255).toInt()
            ),
            hsv
        )
        hsv[1] = hsv[1].coerceIn(0.4f, 0.9f)
        hsv[2] = 0.95f
        Color(android.graphics.Color.HSVToColor(hsv))
    }

    val listState = rememberLazyListState()

    // Auto-hide chrome on user inactivity
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            pingInteraction()
        }
    }

    LaunchedEffect(lastInteractionTime, chromeVisible) {
        if (chromeVisible) {
            delay(3500)
            if (System.currentTimeMillis() - lastInteractionTime >= 3400) {
                chromeVisible = false
            }
        }
    }

    val transition = rememberInfiniteTransition(label = "lyricsAtmosphere")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "ambientPhase"
    )

    fun cycleGradient() {
        val current = lyricsSettings.gradientAnimation
        val list = listOf("wave", "drift", "aurora", "off")
        val nextIdx = (list.indexOf(current) + 1) % list.size
        val nextVal = list[nextIdx]
        scope.launch {
            prefs.setLyricsSettings(lyricsSettings.copy(gradientAnimation = nextVal))
        }
        val label = when (nextVal) {
            "wave" -> "Gradient: Wave"
            "drift" -> "Gradient: Drift"
            "aurora" -> "Gradient: Aurora"
            else -> "Gradient: Off"
        }
        overlays.showP(label)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .pointerInput(Unit) {
                detectTapGestures {
                    pingInteraction()
                }
            }
    ) {
        // Dynamic Canvas moving gradient effects from BOTH Top and Bottom
        when (gradientAnim) {
            "wave" -> {
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width; val h = size.height
                    val cxTop = w / 2f + (w * 0.30f * cos(phase))
                    val cyTop = h * 0.2f + (h * 0.15f * sin(phase))
                    val cxBot = w / 2f - (w * 0.30f * cos(phase))
                    val cyBot = h * 0.8f - (h * 0.15f * sin(phase))

                    // Top wave
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                baseColor.copy(alpha = 0.55f),
                                colors.primaryAccent.copy(alpha = 0.25f),
                                Color.Transparent
                            ),
                            center = Offset(cxTop, cyTop),
                            radius = w * 0.85f
                        )
                    )

                    // Bottom wave
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                baseColor.copy(alpha = 0.50f),
                                colors.primaryAccent.copy(alpha = 0.22f),
                                Color.Transparent
                            ),
                            center = Offset(cxBot, cyBot),
                            radius = w * 0.85f
                        )
                    )
                }
            }
            "drift" -> {
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width; val h = size.height
                    val progress = (sin(phase) + 1f) / 2f
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                baseColor.copy(alpha = 0.55f),
                                colors.primaryAccent.copy(alpha = 0.28f),
                                Color(0xFF007AFF).copy(alpha = 0.18f),
                                baseColor.copy(alpha = 0.45f)
                            ),
                            start = Offset(w * progress * 0.5f, 0f),
                            end = Offset(w * (1f - progress * 0.5f), h)
                        )
                    )
                }
            }
            "aurora" -> {
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width; val h = size.height
                    val pTop = Path()
                    pTop.moveTo(0f, h * 0.22f + sin(phase) * 50f)
                    pTop.cubicTo(
                        w * 0.33f, h * 0.12f + cos(phase) * 60f,
                        w * 0.66f, h * 0.28f + sin(phase * 1.2f) * 55f,
                        w, h * 0.18f + cos(phase * 0.8f) * 45f
                    )
                    pTop.lineTo(w, 0f)
                    pTop.lineTo(0f, 0f)
                    pTop.close()

                    drawPath(
                        path = pTop,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                colors.primaryAccent.copy(alpha = 0.38f),
                                baseColor.copy(alpha = 0.50f),
                                Color.Transparent
                            )
                        )
                    )

                    val pBot = Path()
                    pBot.moveTo(0f, h * 0.78f - sin(phase) * 45f)
                    pBot.cubicTo(
                        w * 0.33f, h * 0.88f - cos(phase) * 55f,
                        w * 0.66f, h * 0.72f - sin(phase * 1.2f) * 50f,
                        w, h * 0.82f - cos(phase * 0.8f) * 40f
                    )
                    pBot.lineTo(w, h)
                    pBot.lineTo(0f, h)
                    pBot.close()

                    drawPath(
                        path = pBot,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                baseColor.copy(alpha = 0.45f),
                                colors.primaryAccent.copy(alpha = 0.32f)
                            )
                        )
                    )
                }
            }
            else -> {
                Box(Modifier.fillMaxSize().background(baseColor.copy(alpha = 0.18f)))
            }
        }

        // Lyric Stage
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.primaryAccent, strokeWidth = 2.dp)
                    }
                }
                state.lyrics != null && state.lyrics.synchronized -> {
                    val lines = state.lyrics.lines
                    val activeIndex = remember(position, lines, lyricsSettings.offsetMs) {
                        val currentMs = lyricsSettings.position(position)
                        val idx = lines.indexOfLast { (it.timeMs ?: 0L) <= currentMs }
                        if (idx >= 0) idx else 0
                    }

                    LaunchedEffect(activeIndex) {
                        if (activeIndex in lines.indices && !listState.isScrollInProgress) {
                            listState.animateScrollToItem(
                                (activeIndex - 2).coerceAtLeast(0)
                            )
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .lyricsEdgeFade(lyricsSettings.fadeTop, lyricsSettings.fadeBottom),
                        contentPadding = PaddingValues(
                            top = animatedBarsPad + 96.dp,
                            bottom = 120.dp,
                            start = 24.dp,
                            end = 24.dp
                        )
                    ) {
                        itemsIndexed(lines) { index, line ->
                            val distance = index - activeIndex
                            val isActive = distance == 0

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        if (!chromeVisible) {
                                            pingInteraction()
                                        } else {
                                            pingInteraction()
                                            haptics.tap()
                                            onSeek(lyricsSettings.seekPosition(line.timeMs ?: 0L))
                                        }
                                    }
                            ) {
                                LyricPresentationLine(
                                    text = line.text,
                                    active = isActive,
                                    distance = distance,
                                    settings = lyricsSettings,
                                    color = lyricColor,
                                    synchronized = true
                                )
                            }
                        }
                    }
                }
                state.lyrics != null && state.lyrics.lines.isNotEmpty() -> {
                    val rawLines = state.lyrics.lines
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .lyricsEdgeFade(lyricsSettings.fadeTop, lyricsSettings.fadeBottom),
                        contentPadding = PaddingValues(
                            top = animatedBarsPad + 96.dp,
                            bottom = 120.dp,
                            start = 24.dp,
                            end = 24.dp
                        )
                    ) {
                        itemsIndexed(rawLines) { index, rawLine ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        pingInteraction()
                                    }
                                    .padding(vertical = (lyricsSettings.lineGap / 2f).dp)
                            ) {
                                Text(
                                    text = rawLine.text.ifBlank { "♪" },
                                    color = lyricColor.copy(alpha = 0.85f),
                                    fontSize = lyricsSettings.currentSize.sp,
                                    textAlign = when (lyricsSettings.alignment) {
                                        "left" -> TextAlign.Start
                                        "right" -> TextAlign.End
                                        else -> TextAlign.Center
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
                else -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(colors.card.copy(alpha = 0.35f))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { launcher.launch(arrayOf("*/*")) }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_xvox_lyrics_add),
                                    contentDescription = "Attach lyrics",
                                    tint = colors.primaryAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Text(
                                text = "No lyrics",
                                color = colors.primaryText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                        }
                    }
                }
            }
        }

        // Single Unified Top Header Overlay with Transport & Progress Bar
        val headerBg = colors.surface.copy(alpha = chrome.headerBgAlpha.coerceIn(0f, 1f))
        val headerPhoto = headerPhotoUri?.takeIf { it.isNotBlank() }
        val topPadding = (animatedBarsPad + 14.dp).coerceAtLeast(24.dp)

        AnimatedVisibility(
            visible = chromeVisible,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(260, easing = FastOutSlowInEasing)
            ) + fadeIn(tween(200)),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(220, easing = FastOutSlowInEasing)
            ) + fadeOut(tween(160)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBg)
            ) {
                if (headerPhoto != null) {
                    AsyncImage(
                        model = headerPhoto,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                    Box(
                        Modifier
                            .matchParentSize()
                            .background(headerBg)
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = topPadding)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .height(54.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        XvoxSongArtwork(
                            artwork = song.artworkUri,
                            requestSize = 128,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = song.title,
                                color = colors.primaryText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.artist,
                                color = colors.secondaryText,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Transport: Previous
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(colors.card.copy(alpha = 0.35f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        pingInteraction()
                                        haptics.tap()
                                        onPrevious()
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_xvox_skip_previous),
                                contentDescription = "Previous",
                                tint = colors.primaryText,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(Modifier.size(4.dp))

                        // Transport: Play / Pause toggle
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(colors.card.copy(alpha = 0.40f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    pingInteraction()
                                    haptics.tap()
                                    onTogglePlay()
                                }
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(if (isPlaying) R.drawable.ic_xvox_pause else R.drawable.ic_xvox_play),
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = colors.primaryAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(Modifier.size(4.dp))

                        // Transport: Next
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(colors.card.copy(alpha = 0.35f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        pingInteraction()
                                        haptics.tap()
                                        onNext()
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_xvox_skip_next),
                                contentDescription = "Next",
                                tint = colors.primaryText,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(Modifier.size(4.dp))

                        // Gradient cycle button
                        val gradientIsActive = gradientAnim != "off"
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (gradientIsActive) colors.primaryAccent.copy(alpha = 0.28f)
                                    else colors.card.copy(alpha = 0.32f)
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        pingInteraction()
                                        cycleGradient()
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_xvox_sparkle),
                                contentDescription = "Cycle gradient",
                                tint = if (gradientIsActive) colors.primaryAccent else colors.primaryText,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(Modifier.size(4.dp))

                        if (onOpenSettings != null) {
                            FullscreenCircle(
                                resource = R.drawable.ic_xvox_settings,
                                description = "Lyrics settings",
                                onClick = {
                                    pingInteraction()
                                    onOpenSettings()
                                }
                            )
                            Spacer(Modifier.size(4.dp))
                        }

                        FullscreenCircle(
                            resource = R.drawable.ic_xvox_close,
                            description = "Close",
                            onClick = onClose
                        )
                    }

                    // Header Progress Bar
                    val progressFraction = (position.toFloat() / duration.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .pointerInput(duration) {
                                detectHorizontalDragGestures { change, _ ->
                                    change.consume()
                                    pingInteraction()
                                    val seekRatio = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                    onSeek((seekRatio * duration).toLong())
                                }
                            }
                            .pointerInput(duration) {
                                detectTapGestures { offset ->
                                    pingInteraction()
                                    val seekRatio = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                    onSeek((seekRatio * duration).toLong())
                                }
                            },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(2.5.dp)
                                .background(colors.cardBorder.copy(alpha = 0.45f))
                        )
                        Box(
                            Modifier
                                .fillMaxWidth(progressFraction)
                                .height(2.5.dp)
                                .background(colors.primaryAccent)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FullscreenCircle(
    resource: Int,
    description: String,
    onClick: () -> Unit
) {
    val colors = XvoxTheme.colors

    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(colors.card.copy(alpha = 0.35f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(resource),
            contentDescription = description,
            tint = colors.primaryText,
            modifier = Modifier.size(16.dp)
        )
    }
}
