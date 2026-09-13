package com.xvox.music.player.nowplaying.lyrics

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.data.preferences.LyricsSettings
import com.xvox.music.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun XvoxArtworkLyrics(
    state: XvoxLyricsUiState,
    position: Long,
    onSeek: (Long) -> Unit,
    onAttach: (Uri) -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
    expanded: Boolean = false,
    onToggleExpand: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    onDismissNowPlaying: (() -> Unit)? = null,
    onSwipeDownDelta: ((Float) -> Unit)? = null,
    onSwipeDownEnd: (() -> Unit)? = null,
    textColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    val context = LocalContext.current
    val haptics = LocalXvoxHaptics.current
    val overlays = LocalXvoxOverlayController.current
    val prefs = remember(context) { UserPreferencesRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val lyricsSettings by prefs.lyricsSettings.collectAsState(initial = LyricsSettings())
    val gradientAnim = when (lyricsSettings.gradientAnimation) {
        "orb", "wave" -> "orb"
        "aurora" -> "aurora"
        else -> "off"
    }

    val custom =
        state.lyrics?.source ==
            XvoxLyricsSource.USER_LRC ||
            state.lyrics?.source ==
            XvoxLyricsSource.USER_TEXT

    val launcher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            uri?.let(onAttach)
        }

    val view = LocalView.current
    DisposableEffect(expanded) {
        val window = (view.context as? android.app.Activity)?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, view) }
        if (expanded) {
            insetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController?.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            if (expanded) {
                insetsController?.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Pill visibility: visible initially on card open, disappears 3s after user inactivity, reappears on user scroll/touch
    var pillVisible by remember { mutableStateOf(!expanded) }
    var lastUserInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isUserTouching by remember { mutableStateOf(false) }

    fun registerUserActivity() {
        if (!expanded) {
            lastUserInteractionTime = System.currentTimeMillis()
            pillVisible = true
        }
    }

    val listState = rememberLazyListState()

    // 3-second auto-hide timer for pill when user is idle
    LaunchedEffect(lastUserInteractionTime, isUserTouching, expanded, pillVisible) {
        if (!expanded && pillVisible && !isUserTouching) {
            delay(3000)
            if (System.currentTimeMillis() - lastUserInteractionTime >= 2950 && !isUserTouching) {
                pillVisible = false
            }
        }
    }

    val transition = rememberInfiniteTransition(label = "lyricsCanvasMotion")
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
        val nextVal = when (gradientAnim) {
            "off" -> "orb"
            "orb" -> "aurora"
            else -> "off"
        }
        scope.launch {
            prefs.setLyricsSettings(lyricsSettings.copy(gradientAnimation = nextVal))
        }
        val label = when (nextVal) {
            "orb" -> "Gradient: Orb"
            "aurora" -> "Gradient: Aurora"
            else -> "Gradient: Off"
        }
        overlays.showP(label)
    }

    val effectiveTextColor = if (lyricsSettings.matchCoverColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(
            android.graphics.Color.rgb(
                (textColor.red * 255).toInt(),
                (textColor.green * 255).toInt(),
                (textColor.blue * 255).toInt()
            ),
            hsv
        )
        hsv[1] = hsv[1].coerceIn(0.4f, 0.9f)
        hsv[2] = 0.96f
        Color(android.graphics.Color.HSVToColor(hsv))
    } else Color.White

    val vibrantCoverColor = remember(textColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(
            android.graphics.Color.rgb(
                (textColor.red * 255).toInt(),
                (textColor.green * 255).toInt(),
                (textColor.blue * 255).toInt()
            ),
            hsv
        )
        if (hsv[1] < 0.15f) {
            Color(0xFFE2E6FF)
        } else {
            hsv[1] = hsv[1].coerceIn(0.60f, 0.95f)
            hsv[2] = hsv[2].coerceIn(0.80f, 1.0f)
            Color(android.graphics.Color.HSVToColor(hsv))
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background.copy(alpha = if (expanded) 0.35f else 0.27f))
            .pointerInput(expanded) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isUserTouching = true
                    registerUserActivity()
                    do {
                        val event = awaitPointerEvent()
                        registerUserActivity()
                    } while (event.changes.any { it.pressed })
                    isUserTouching = false
                    registerUserActivity()
                }
            }
    ) {
        val density = LocalDensity.current
        val maximumSize = maxOf(lyricsSettings.currentSize, maxOf(lyricsSettings.topSize, lyricsSettings.bottomSize))
        val activeLineHeightDp = with(density) { (maximumSize * 1.30f).sp.toDp() } + (lyricsSettings.lineGap / 2f).coerceAtLeast(4f).dp * 2
        // True optical vertical center
        val verticalCenterPadding = ((maxHeight - activeLineHeightDp) / 2f).coerceAtLeast(16.dp)

        // Dynamic Canvas Gradient Effects (Off, Rich Glowing Orb, Seamless Opposite-Moving Aurora)
        if (gradientAnim != "off") {
            when (gradientAnim) {
                "orb" -> {
                    Canvas(Modifier.fillMaxSize()) {
                        val w = size.width; val h = size.height
                        val cx = w / 2f + (w * 0.38f * cos(phase))
                        val cy = h / 2f + (h * 0.32f * sin(phase))
                        val radius = (w * 1.35f).coerceAtLeast(h * 0.9f)
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    vibrantCoverColor.copy(alpha = 0.85f),
                                    vibrantCoverColor.copy(alpha = 0.50f),
                                    vibrantCoverColor.copy(alpha = 0.18f),
                                    Color.Transparent
                                ),
                                center = Offset(cx, cy),
                                radius = radius
                            )
                        )
                    }
                }
                "aurora" -> {
                    Canvas(Modifier.fillMaxSize()) {
                        val w = size.width; val h = size.height
                        val steps = 60

                        // Top wave: Smooth continuous wave starting right from x = 0 (no triangle corner!)
                        val pTop = Path()
                        pTop.moveTo(0f, 0f)
                        for (i in 0..steps) {
                            val x = (w / steps) * i
                            val waveProg = (x / w) * 2f * PI.toFloat()
                            val y = h * 0.22f + sin(waveProg + phase) * (h * 0.035f)
                            pTop.lineTo(x, y)
                        }
                        pTop.lineTo(w, 0f)
                        pTop.close()

                        drawPath(
                            path = pTop,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    vibrantCoverColor.copy(alpha = 0.55f),
                                    vibrantCoverColor.copy(alpha = 0.30f),
                                    Color.Transparent
                                )
                            )
                        )

                        // Bottom wave: Smooth continuous wave in opposite direction starting right from x = 0 (no triangle corner!)
                        val pBot = Path()
                        pBot.moveTo(0f, h)
                        for (i in 0..steps) {
                            val x = (w / steps) * i
                            val waveProg = (x / w) * 2f * PI.toFloat()
                            val y = h * 0.78f + cos(waveProg - phase + 1.57f) * (h * 0.035f)
                            pBot.lineTo(x, y)
                        }
                        pBot.lineTo(w, h)
                        pBot.close()

                        drawPath(
                            path = pBot,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    vibrantCoverColor.copy(alpha = 0.30f),
                                    vibrantCoverColor.copy(alpha = 0.55f)
                                )
                            )
                        )
                    }
                }
            }
        }

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

                // Automatic lyric progression: exact vertical centering
                LaunchedEffect(
                    activeIndex,
                    lyricsSettings.currentSize,
                    lyricsSettings.topSize,
                    lyricsSettings.bottomSize,
                    lyricsSettings.lineGap,
                    expanded
                ) {
                    if (activeIndex in lines.indices && !isUserTouching) {
                        listState.animateScrollToItem(
                            index = activeIndex,
                            scrollOffset = 0
                        )
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .lyricsEdgeFade(lyricsSettings.fadeTop, lyricsSettings.fadeBottom),
                    contentPadding = PaddingValues(
                        top = verticalCenterPadding,
                        bottom = verticalCenterPadding,
                        start = 16.dp,
                        end = 16.dp
                    )
                ) {
                    itemsIndexed(lines, key = { index, _ -> "sync_line_$index" }) { index, line ->
                        val distance = index - activeIndex
                        val isActive = distance == 0

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    registerUserActivity()
                                    haptics.tap()
                                    onSeek(lyricsSettings.seekPosition(line.timeMs ?: 0L))
                                }
                        ) {
                            LyricPresentationLine(
                                text = line.text,
                                active = isActive,
                                distance = distance,
                                settings = lyricsSettings,
                                color = effectiveTextColor,
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
                        top = verticalCenterPadding,
                        bottom = verticalCenterPadding,
                        start = 16.dp,
                        end = 16.dp
                    )
                ) {
                    itemsIndexed(rawLines, key = { index, _ -> "raw_line_$index" }) { _, rawLine ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    registerUserActivity()
                                }
                                .padding(vertical = (lyricsSettings.lineGap / 2f).coerceAtLeast(4f).dp)
                        ) {
                            Text(
                                text = rawLine.text.ifBlank { "♪" },
                                color = effectiveTextColor.copy(alpha = 0.85f),
                                fontSize = lyricsSettings.currentSize.sp,
                                lineHeight = (lyricsSettings.currentSize * 1.30f).sp,
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
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(54.dp)
                        .background(colors.card.copy(alpha = 0.25f), CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            launcher.launch(arrayOf("*/*"))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_xvox_lyrics_add),
                        contentDescription = "Add lyrics",
                        tint = colors.primaryText,
                        modifier = Modifier.size(23.dp)
                    )
                }
            }
        }

        // Invisible top 10% non-scrollable gesture band in fullscreen mode to pull down and dismiss Now Playing
        if (expanded) {
            val topDragHeight = (maxHeight * 0.12f).coerceAtLeast(80.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(topDragHeight)
                    .align(Alignment.TopCenter)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { _, dragAmount ->
                                onSwipeDownDelta?.invoke(dragAmount)
                            },
                            onDragEnd = {
                                onSwipeDownEnd?.invoke()
                            },
                            onDragCancel = {
                                onSwipeDownEnd?.invoke()
                            }
                        )
                    }
            )
        }

        // Action pill: ONLY displayed in regular card mode (never in fullscreen)
        if (!expanded) {
            AnimatedVisibility(
                visible = pillVisible,
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(200, easing = FastOutSlowInEasing)
                ) + fadeIn(tween(160)),
                exit = slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(180, easing = FastOutSlowInEasing)
                ) + fadeOut(tween(140)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(9.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (custom) {
                        LyricsDeleteButton(
                            onClick = {
                                registerUserActivity()
                                onDelete()
                            }
                        )
                        Spacer(Modifier.size(7.dp))
                    }

                    Row(
                        modifier = Modifier
                            .background(
                                colors.card.copy(alpha = 0.32f),
                                RoundedCornerShape(18.dp)
                            )
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val gradientActive = gradientAnim != "off"
                        LyricsAction(
                            resource = R.drawable.ic_xvox_sparkle,
                            tint = if (gradientActive) colors.primaryAccent else colors.primaryText,
                            onClick = {
                                registerUserActivity()
                                cycleGradient()
                            }
                        )

                        if (onToggleExpand != null) {
                            LyricsAction(
                                resource = R.drawable.ic_xvox_fullscreen,
                                onClick = {
                                    registerUserActivity()
                                    onToggleExpand()
                                }
                            )
                        }

                        if (onOpenSettings != null) {
                            LyricsAction(
                                resource = R.drawable.ic_xvox_settings,
                                onClick = {
                                    registerUserActivity()
                                    onOpenSettings()
                                }
                            )
                        }

                        LyricsAction(
                            resource = R.drawable.ic_xvox_close,
                            onClick = {
                                registerUserActivity()
                                onClose()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricsDeleteButton(onClick: () -> Unit) {
    val colors = XvoxTheme.colors

    Box(
        modifier = Modifier
            .size(36.dp)
            .background(colors.card.copy(alpha = 0.28f), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_xvox_delete),
            contentDescription = "Remove custom lyrics",
            tint = colors.primaryText,
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun LyricsAction(
    resource: Int,
    tint: Color? = null,
    onClick: () -> Unit
) {
    val colors = XvoxTheme.colors

    Box(
        modifier = Modifier
            .size(36.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(resource),
            contentDescription = null,
            tint = tint ?: colors.primaryText,
            modifier = Modifier.size(18.dp)
        )
    }
}
