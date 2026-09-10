package com.xvox.music.player.nowplaying.lyrics

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.features.home.XvoxSongArtwork
import com.xvox.music.player.nowplaying.XvoxNowPlayingProgress
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * Full-screen lyrics: background is solid (opaque), with an optional moving gradient
 * toggled from the button before Previous.
 */
@Composable
fun XvoxFullscreenLyrics(
    song: Song,
    state: XvoxLyricsUiState,
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    backgroundColor: Color,
    onAttach: (Uri) -> Unit,
    onDelete: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current
    var gradientEnabled by remember { mutableStateOf(false) }

    val view = LocalView.current
    val statusBarPx = remember(view) {
        val window = (view.context as? android.app.Activity)?.window
        if (window == null) 0
        else androidx.core.view.WindowInsetsCompat.toWindowInsetsCompat(view.rootWindowInsets, view)
            .getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars()).top
    }
    var barsVisible by remember { mutableStateOf(true) }
    val animatedBarsPad by animateDpAsState(
        targetValue = if (barsVisible) with(LocalDensity.current) { statusBarPx.toDp() } else 0.dp,
        animationSpec = tween(260, easing = FastOutSlowInEasing),
        label = "barsPad"
    )

    DisposableEffect(view) {
        val window = (view.context as? android.app.Activity)?.window
        val controller = window?.let { androidx.core.view.WindowCompat.getInsetsController(it, view) }
        controller?.let { c ->
            c.systemBarsBehavior =
                androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            c.isAppearanceLightStatusBars = false
        }
        val hide = Runnable {
            barsVisible = false
            controller?.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            Unit
        }
        view.postDelayed(hide, 120)
        onDispose {
            view.removeCallbacks(hide)
            barsVisible = true
            controller?.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
    }
    val custom = state.lyrics?.source == XvoxLyricsSource.USER_LRC || state.lyrics?.source == XvoxLyricsSource.USER_TEXT
    var chromeVisible by remember { mutableStateOf(true) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onAttach)
    }

    LaunchedEffect(chromeVisible) {
        if (!chromeVisible) return@LaunchedEffect
        delay(3000)
        chromeVisible = false
    }

    val infiniteTransition = rememberInfiniteTransition(label = "lyricGradient")
    val gradientPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Solid background base with optional moving gradient layer
        if (gradientEnabled) {
            Canvas(Modifier.fillMaxSize()) {
                val cx = size.width / 2f + (size.width * 0.35f * cos(gradientPhase))
                val cy = size.height / 2f + (size.height * 0.35f * sin(gradientPhase))
                val start = Offset(cx - size.width * 0.5f, cy - size.height * 0.5f)
                val end = Offset(cx + size.width * 0.5f, cy + size.height * 0.5f)
                val colorA = backgroundColor.copy(alpha = 0.85f)
                val colorB = colors.primaryAccent.copy(alpha = 0.55f)
                val colorC = colors.background
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(colorA, colorB, colorC),
                        start = start,
                        end = end
                    )
                )
            }
        } else {
            Box(Modifier.fillMaxSize().background(backgroundColor.copy(alpha = 0.35f)))
        }

        // Lyric stage
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        chromeVisible = true
                    }
                }
        ) {
            when {
                state.loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Loading lyrics…", color = colors.secondaryText, fontSize = 13.sp)
                    }
                }
                state.lyrics != null -> {
                    XvoxSyncedLyrics(
                        lyrics = state.lyrics,
                        position = position,
                        onSeek = onSeek,
                        strongEdgeFade = true,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp)
                    )
                }
                else -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(colors.card.copy(alpha = 0.28f), CircleShape)
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

        if (!chromeVisible) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = animatedBarsPad)
                    .background(colors.background.copy(alpha = 0.4f))
            )
        }

        AnimatedVisibility(
            visible = chromeVisible,
            enter = slideInVertically(tween(300)) { -it } + fadeIn(tween(200)),
            exit = slideOutVertically(tween(240)) { -it } + fadeOut(tween(160)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.background.copy(alpha = 0.75f))
                    .padding(top = animatedBarsPad)
                    .padding(start = 14.dp, top = 10.dp, end = 14.dp, bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    XvoxSongArtwork(
                        artwork = song.artworkUri,
                        requestSize = 128,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 10.dp, end = 8.dp)
                    ) {
                        Text(
                            text = song.title,
                            color = colors.primaryText,
                            fontSize = 13.sp,
                            lineHeight = 16.sp,
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

                    if (custom) {
                        FullscreenCircle(
                            resource = R.drawable.ic_xvox_delete,
                            description = "Remove custom lyrics",
                            onClick = onDelete
                        )
                        Spacer(Modifier.size(6.dp))
                    }

                    LyricsTransport(
                        isPlaying = isPlaying,
                        gradientEnabled = gradientEnabled,
                        onToggleGradient = {
                            haptics.tap()
                            gradientEnabled = !gradientEnabled
                        },
                        onPrevious = onPrevious,
                        onTogglePlay = onTogglePlay,
                        onNext = onNext
                    )

                    Spacer(Modifier.size(6.dp))

                    FullscreenCircle(
                        resource = R.drawable.ic_xvox_close,
                        description = "Close fullscreen lyrics",
                        onClick = onClose
                    )
                }

                XvoxNowPlayingProgress(
                    position = position,
                    duration = duration,
                    onSeek = onSeek,
                    modifier = Modifier.padding(top = 9.dp)
                )
            }
        }
    }
}

@Composable
private fun LyricsTransport(
    isPlaying: Boolean,
    gradientEnabled: Boolean,
    onToggleGradient: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit
) {
    val colors = XvoxTheme.colors

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(colors.card.copy(alpha = 0.38f)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onToggleGradient
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_xvox_sparkle),
                contentDescription = "Toggle moving gradient",
                tint = if (gradientEnabled) colors.primaryAccent else colors.mutedText,
                modifier = Modifier.size(17.dp)
            )
        }
        TransportButton(R.drawable.ic_xvox_skip_previous, onPrevious)
        TransportButton(if (isPlaying) R.drawable.ic_xvox_pause else R.drawable.ic_xvox_play, onTogglePlay)
        TransportButton(R.drawable.ic_xvox_skip_next, onNext)
    }
}

@Composable
private fun TransportButton(
    resource: Int,
    onClick: () -> Unit
) {
    val colors = XvoxTheme.colors

    Box(
        modifier = Modifier
            .size(38.dp)
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
            tint = colors.primaryText,
            modifier = Modifier.size(18.dp)
        )
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
            .size(38.dp)
            .background(colors.card.copy(alpha = 0.32f), CircleShape)
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
            modifier = Modifier.size(18.dp)
        )
    }
}
