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
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
        "wave", "aurora" -> lyricsSettings.gradientAnimation
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

    var pillVisible by remember { mutableStateOf(!expanded) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    fun pingInteraction() {
        lastInteractionTime = System.currentTimeMillis()
        pillVisible = true
    }

    val listState = rememberLazyListState()

    LaunchedEffect(lastInteractionTime, pillVisible) {
        if (pillVisible) {
            delay(3500)
            if (System.currentTimeMillis() - lastInteractionTime >= 3400) {
                pillVisible = false
            }
        }
    }

    val transition = rememberInfiniteTransition(label = "lyricsCanvasMotion")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "ambientPhase"
    )

    fun cycleGradient() {
        val nextVal = when (gradientAnim) {
            "off" -> "wave"
            "wave" -> "aurora"
            else -> "off"
        }
        scope.launch {
            prefs.setLyricsSettings(lyricsSettings.copy(gradientAnimation = nextVal))
        }
        val label = when (nextVal) {
            "wave" -> "Gradient: Wave"
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

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(if (expanded) colors.background else colors.background.copy(alpha = 0.27f))
            .pointerInput(Unit) {
                detectTapGestures {
                    pingInteraction()
                }
            }
            .then(
                if (expanded) {
                    Modifier.pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount > 22f) {
                                onToggleExpand?.invoke()
                            }
                        }
                    }
                } else Modifier
            )
    ) {
        val verticalCenterPadding = (maxHeight / 2 - 28.dp).coerceAtLeast(36.dp)

        // Dynamic Canvas Gradient Effects (Off, Wave, Aurora natural double-wave)
        if (gradientAnim != "off") {
            when (gradientAnim) {
                "wave" -> {
                    Canvas(Modifier.fillMaxSize()) {
                        val w = size.width; val h = size.height
                        val cx = w / 2f + (w * 0.32f * cos(phase))
                        val cy = h / 2f + (h * 0.28f * sin(phase))
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    textColor.copy(alpha = 0.45f),
                                    colors.primaryAccent.copy(alpha = 0.25f),
                                    Color.Transparent
                                ),
                                center = Offset(cx, cy),
                                radius = w * 0.90f
                            )
                        )
                    }
                }
                "aurora" -> {
                    Canvas(Modifier.fillMaxSize()) {
                        val w = size.width; val h = size.height

                        // Top wave: independent organic vertical undulating path
                        val yOffsetTop = sin(phase * 0.8f) * (h * 0.08f)
                        val pTop = Path()
                        pTop.moveTo(0f, h * 0.24f + yOffsetTop)
                        pTop.cubicTo(
                            w * 0.30f, h * 0.12f + cos(phase) * 55f + yOffsetTop,
                            w * 0.70f, h * 0.32f + sin(phase * 1.2f) * 60f + yOffsetTop,
                            w, h * 0.18f + yOffsetTop
                        )
                        pTop.lineTo(w, 0f)
                        pTop.lineTo(0f, 0f)
                        pTop.close()

                        drawPath(
                            path = pTop,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    colors.primaryAccent.copy(alpha = 0.38f),
                                    textColor.copy(alpha = 0.32f),
                                    Color.Transparent
                                )
                            )
                        )

                        // Bottom wave: distinct offset frequency and vertical flow (not mirrored)
                        val yOffsetBot = cos(phase * 0.7f + 1.2f) * (h * 0.09f)
                        val pBot = Path()
                        pBot.moveTo(0f, h * 0.76f + yOffsetBot)
                        pBot.cubicTo(
                            w * 0.35f, h * 0.88f + sin(phase * 1.4f + 0.8f) * 50f + yOffsetBot,
                            w * 0.65f, h * 0.68f + cos(phase * 1.1f) * 55f + yOffsetBot,
                            w, h * 0.82f + yOffsetBot
                        )
                        pBot.lineTo(w, h)
                        pBot.lineTo(0f, h)
                        pBot.close()

                        drawPath(
                            path = pBot,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    textColor.copy(alpha = 0.32f),
                                    colors.primaryAccent.copy(alpha = 0.35f)
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

                LaunchedEffect(activeIndex) {
                    if (activeIndex in lines.indices) {
                        listState.animateScrollToItem(activeIndex)
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .lyricsEdgeFade(lyricsSettings.fadeTop, lyricsSettings.fadeBottom)
                        .pointerInput(Unit) {
                            detectTapGestures {
                                pingInteraction()
                            }
                        },
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
                                    pingInteraction()
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
                        .lyricsEdgeFade(lyricsSettings.fadeTop, lyricsSettings.fadeBottom)
                        .pointerInput(Unit) {
                            detectTapGestures {
                                pingInteraction()
                            }
                        },
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
                                    pingInteraction()
                                }
                                .padding(vertical = (lyricsSettings.lineGap / 2f).coerceAtLeast(4f).dp)
                        ) {
                            Text(
                                text = rawLine.text.ifBlank { "♪" },
                                color = effectiveTextColor.copy(alpha = 0.85f),
                                fontSize = lyricsSettings.currentSize.sp,
                                lineHeight = (lyricsSettings.currentSize * 1.35f).sp,
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

        // Action pill with Settings button and auto-hide
        AnimatedVisibility(
            visible = pillVisible,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(240, easing = FastOutSlowInEasing)
            ) + fadeIn(tween(180)),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(200, easing = FastOutSlowInEasing)
            ) + fadeOut(tween(140)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(if (expanded) 16.dp else 9.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (custom) {
                    LyricsDeleteButton(
                        onClick = {
                            pingInteraction()
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
                            pingInteraction()
                            cycleGradient()
                        }
                    )

                    if (onToggleExpand != null) {
                        LyricsAction(
                            resource = if (expanded) R.drawable.ic_xvox_fullscreen_exit else R.drawable.ic_xvox_fullscreen,
                            onClick = {
                                pingInteraction()
                                onToggleExpand()
                            }
                        )
                    }

                    if (onOpenSettings != null) {
                        LyricsAction(
                            resource = R.drawable.ic_xvox_settings,
                            onClick = {
                                pingInteraction()
                                onOpenSettings()
                            }
                        )
                    }

                    LyricsAction(
                        resource = R.drawable.ic_xvox_close,
                        onClick = {
                            pingInteraction()
                            onClose()
                        }
                    )
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
