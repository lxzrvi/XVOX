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
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.xvox.music.core.ui.chrome.parseHexColor
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.data.preferences.LyricsSettings
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.features.home.XvoxSongArtwork
import com.xvox.music.player.nowplaying.XvoxNowPlayingPaletteState
import com.xvox.music.player.nowplaying.rememberXvoxNowPlayingPalette
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
    val surfaceBg = baseColor.copy(alpha = 0.90f)

    // Readable text color extracted from artwork with balanced contrast
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
        // Boost value / lightness to ensure bright text on dark background
        hsv[1] = hsv[1].coerceIn(0.4f, 0.9f)
        hsv[2] = 0.92f
        Color(android.graphics.Color.HSVToColor(hsv))
    }

    val listState = rememberLazyListState()

    // Auto-hide chrome on user inactivity or scroll
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            chromeVisible = false
        }
    }

    LaunchedEffect(chromeVisible) {
        if (chromeVisible) {
            delay(3800)
            chromeVisible = false
        }
    }

    fun revealChrome() {
        chromeVisible = true
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

    val custom = state.lyrics?.source != null && state.lyrics.source != XvoxLyricsSource.EMBEDDED

    fun cycleGradient() {
        val current = lyricsSettings.gradientAnimation
        val list = listOf("wave", "aurora", "pulse", "orbital", "prism", "off")
        val nextIdx = (list.indexOf(current) + 1) % list.size
        val nextVal = list[nextIdx]
        scope.launch {
            prefs.setLyricsSettings(lyricsSettings.copy(gradientAnimation = nextVal))
        }
        val label = when (nextVal) {
            "wave" -> "Gradient: Wave"
            "aurora" -> "Gradient: Aurora"
            "pulse" -> "Gradient: Radial Pulse"
            "orbital" -> "Gradient: Orbital Glow"
            "prism" -> "Gradient: Prism Drift"
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
                    chromeVisible = !chromeVisible
                }
            }
    ) {
        // Canvas background gradient effects
        when (gradientAnim) {
            "wave" -> {
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width; val h = size.height
                    val cx = w / 2f + (w * 0.35f * cos(phase))
                    val cy = h / 2f + (h * 0.35f * sin(phase))
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                baseColor,
                                baseColor.copy(alpha = 0.6f),
                                colors.primaryAccent.copy(alpha = 0.25f),
                                colors.background
                            ),
                            start = Offset(cx - w * 0.55f, cy - h * 0.55f),
                            end = Offset(cx + w * 0.55f, cy + h * 0.55f)
                        )
                    )
                }
            }
            "aurora" -> {
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width; val h = size.height
                    val p = Path()
                    p.moveTo(0f, h * 0.2f + sin(phase) * 60f)
                    p.cubicTo(
                        w * 0.33f, h * 0.1f + cos(phase) * 80f,
                        w * 0.66f, h * 0.35f + sin(phase * 1.2f) * 70f,
                        w, h * 0.15f + cos(phase * 0.8f) * 60f
                    )
                    p.lineTo(w, h)
                    p.lineTo(0f, h)
                    p.close()
                    drawPath(
                        path = p,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                colors.primaryAccent.copy(alpha = 0.35f),
                                baseColor.copy(alpha = 0.65f),
                                colors.background
                            )
                        )
                    )
                }
            }
            "pulse" -> {
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width; val h = size.height
                    val pulse = (sin(phase * 2f) + 1f) / 2f
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                baseColor.copy(alpha = 0.4f + pulse * 0.4f),
                                colors.primaryAccent.copy(alpha = 0.2f + pulse * 0.2f),
                                colors.background
                            ),
                            center = Offset(w / 2f, h * 0.45f),
                            radius = w * (0.8f + pulse * 0.4f)
                        )
                    )
                }
            }
            "orbital" -> {
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width; val h = size.height
                    val ox = w / 2f + cos(phase) * (w * 0.35f)
                    val oy = h / 2f + sin(phase) * (h * 0.25f)
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                baseColor.copy(alpha = 0.75f),
                                colors.primaryAccent.copy(alpha = 0.3f),
                                Color.Transparent
                            ),
                            center = Offset(ox, oy),
                            radius = w * 0.7f
                        )
                    )
                }
            }
            "prism" -> {
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width; val h = size.height
                    val start = Offset(w * (0.5f + 0.4f * cos(phase)), 0f)
                    val end = Offset(w * (0.5f - 0.4f * cos(phase)), h)
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                baseColor,
                                colors.primaryAccent.copy(alpha = 0.4f),
                                Color(0xFF007AFF).copy(alpha = 0.3f),
                                colors.background
                            ),
                            start = start,
                            end = end
                        )
                    )
                }
            }
            else -> {
                Box(Modifier.fillMaxSize().background(baseColor.copy(alpha = 0.30f)))
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
                            top = animatedBarsPad + 90.dp,
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
                                        revealChrome()
                                        haptics.tap()
                                        onSeek(lyricsSettings.seekPosition(line.timeMs ?: 0L))
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
                            top = animatedBarsPad + 90.dp,
                            bottom = 120.dp,
                            start = 24.dp,
                            end = 24.dp
                        )
                    ) {
                        itemsIndexed(rawLines) { index, rawLine ->
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = (lyricsSettings.lineGap / 2f).dp)) {
                                Text(
                                    text = rawLine.text.ifBlank { "♪" },
                                    color = lyricColor.copy(alpha = 0.85f),
                                    fontSize = lyricsSettings.currentSize.sp,
                                    textAlign = when (lyricsSettings.alignment) {
                                        "start" -> androidx.compose.ui.text.style.TextAlign.Start
                                        "end" -> androidx.compose.ui.text.style.TextAlign.End
                                        else -> androidx.compose.ui.text.style.TextAlign.Center
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

        // Single Unified Top Header Overlay
        val headerBg = colors.surface.copy(alpha = chrome.headerBgAlpha.coerceIn(0f, 1f))
        val headerEdge = parseHexColor(chrome.headerBorder) ?: colors.cardBorder
        val headerPhoto = headerPhotoUri?.takeIf { it.isNotBlank() }

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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = animatedBarsPad)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .height(54.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    XvoxSongArtwork(
                        artwork = song.artworkUri,
                        requestSize = 128,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp)
                    ) {
                        Text(
                            text = song.title,
                            color = colors.primaryText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.artist,
                            color = colors.secondaryText,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Transport: Play / Pause toggle
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.card.copy(alpha = 0.35f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onTogglePlay
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(if (isPlaying) R.drawable.ic_xvox_pause else R.drawable.ic_xvox_play),
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = colors.primaryAccent,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    Spacer(Modifier.size(6.dp))

                    // Effect / Sparkles button (cycles 5 gradients)
                    val gradientIsActive = gradientAnim != "off"
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (gradientIsActive) colors.primaryAccent.copy(alpha = 0.28f)
                                else colors.card.copy(alpha = 0.32f)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = ::cycleGradient
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_xvox_sparkle),
                            contentDescription = "Cycle gradient effect",
                            tint = if (gradientIsActive) colors.primaryAccent else colors.primaryText,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    Spacer(Modifier.size(6.dp))

                    if (onOpenSettings != null) {
                        FullscreenCircle(
                            resource = R.drawable.ic_xvox_settings,
                            description = "Lyrics settings",
                            onClick = onOpenSettings
                        )
                        Spacer(Modifier.size(6.dp))
                    }

                    FullscreenCircle(
                        resource = R.drawable.ic_xvox_close,
                        description = "Close fullscreen lyrics",
                        onClick = onClose
                    )
                }

                Box(
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .height(1.dp)
                        .background(headerEdge.copy(alpha = chrome.headerBorderAlpha.coerceIn(0f, 1f)))
                )
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
    val haptics = LocalXvoxHaptics.current

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(colors.card.copy(alpha = 0.35f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    haptics.tap()
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(resource),
            contentDescription = description,
            tint = colors.primaryText,
            modifier = Modifier.size(17.dp)
        )
    }
}
