package com.xvox.music.player.nowplaying

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.R
import com.xvox.music.audio.AudioEffectsManager
import com.xvox.music.core.design.theme.XvoxLogoFont
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.features.player.styles.XvoxPlayerStyle
import com.xvox.music.player.nowplaying.lyrics.XvoxArtworkLyrics
import com.xvox.music.player.nowplaying.lyrics.XvoxFullscreenLyrics
import com.xvox.music.player.nowplaying.lyrics.XvoxLyricsViewModel
import com.xvox.music.player.playback.RepeatMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun XvoxNowPlaying(
    song: Song,
    queue: List<Song>,
    currentIndex: Int,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    onClose: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPlayQueueIndex: (Int) -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    isLiked: Boolean = false,
    onToggleLiked: (() -> Unit)? = null,
    onTimer: (() -> Unit)? = null,
    onQueue: (() -> Unit)? = null,
    onInfo: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onMore: (() -> Unit)? = null,
    onStarPlaylist: (() -> Unit)? = null,
    isShuffleEnabled: Boolean = false,
    repeatMode: RepeatMode = RepeatMode.OFF,
    onToggleShuffle: (() -> Unit)? = null,
    onToggleRepeat: (() -> Unit)? = null,
    playerStyle: XvoxPlayerStyle = XvoxPlayerStyle.NORMAL,
    sleepTimerProgress: Float? = null,
    playingSource: String = "All Songs",
    lyricsViewModel: XvoxLyricsViewModel = viewModel()
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current
    val lyricsState by lyricsViewModel.state.collectAsState()

    val paletteState = rememberXvoxNowPlayingPalette(song, queue, currentIndex)

    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefs = remember { UserPreferencesRepository(context) }

    val screenHeight = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }

    // Preserve screen position across theme and configuration changes without sliding in again
    var screenY by rememberSaveable { mutableFloatStateOf(0f) }
    var hasAnimatedIn by rememberSaveable { mutableStateOf(false) }

    var showLyrics by remember { mutableStateOf(false) }
    var showQuickSettingsSheet by remember { mutableStateOf(false) }
    var dismissing by remember { mutableStateOf(false) }
    var navigationRequest by remember { mutableIntStateOf(0) }
    var motionJob by remember { mutableStateOf<Job?>(null) }

    val dismissProgress = if (screenHeight > 0f) (screenY / screenHeight).coerceIn(0f, 1f) else 0f
    val screenCorner = 32.dp * dismissProgress

    fun animateScreen(
        target: Float,
        durationMs: Int,
        finished: (() -> Unit)? = null
    ) {
        motionJob?.cancel()
        val start = screenY
        motionJob = scope.launch {
            val animation = Animatable(start)
            animation.animateTo(
                target,
                tween(durationMillis = durationMs, easing = XvoxNowPlayingMotion.easing)
            ) {
                screenY = value
            }
            screenY = target
            motionJob = null
            finished?.invoke()
        }
    }

    fun dismiss() {
        if (dismissing) return
        dismissing = true
        haptics.tap()
        animateScreen(
            target = screenHeight,
            durationMs = XvoxNowPlayingMotion.exitDuration(screenY, screenHeight),
            finished = onClose
        )
    }

    fun returnToRest() {
        val fraction = if (screenHeight > 0f) (screenY / screenHeight).coerceIn(0f, 1f) else 1f
        animateScreen(
            0f,
            (XvoxNowPlayingMotion.FullDuration * fraction).toInt().coerceAtLeast(120)
        )
    }

    fun requestPrevious() {
        haptics.click()
        if (queue.isEmpty() || currentIndex < 0) return
        val atFirst = currentIndex <= 0
        if (atFirst && repeatMode == RepeatMode.OFF) return
        val target = if (atFirst && repeatMode == RepeatMode.ALL) queue.lastIndex else currentIndex - 1
        if (showLyrics || atFirst) {
            onPlayQueueIndex(target)
        } else {
            navigationRequest = -(kotlin.math.abs(navigationRequest) + 1)
        }
    }

    fun requestNext() {
        haptics.click()
        if (queue.isEmpty() || currentIndex < 0) return
        val atLast = currentIndex >= queue.lastIndex
        if (atLast && repeatMode == RepeatMode.OFF) return
        val target = if (atLast && repeatMode == RepeatMode.ALL) 0 else currentIndex + 1
        if (showLyrics || atLast) {
            onPlayQueueIndex(target)
        } else {
            navigationRequest = kotlin.math.abs(navigationRequest) + 1
        }
    }

    LaunchedEffect(song.id) {
        lyricsViewModel.load(song)
    }

    LaunchedEffect(Unit) {
        if (!hasAnimatedIn) {
            screenY = screenHeight
            animateScreen(0f, XvoxNowPlayingMotion.FullDuration) {
                hasAnimatedIn = true
            }
        }
    }

    BackHandler {
        when {
            showQuickSettingsSheet -> showQuickSettingsSheet = false
            lyricsState.fullscreen -> lyricsViewModel.closeFullscreen()
            showLyrics -> showLyrics = false
            else -> dismiss()
        }
    }

    if (lyricsState.fullscreen) {
        XvoxFullscreenLyrics(
            song = song,
            state = lyricsState,
            position = position,
            duration = duration,
            isPlaying = isPlaying,
            backgroundColor = paletteState.color,
            onAttach = lyricsViewModel::attach,
            onDelete = lyricsViewModel::removeCustom,
            onPrevious = ::requestPrevious,
            onTogglePlay = onTogglePlay,
            onNext = ::requestNext,
            onSeek = onSeek,
            onClose = lyricsViewModel::closeFullscreen,
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = screenY
                shape = RoundedCornerShape(screenCorner)
                clip = dismissProgress > 0f
            }
    ) {
        // Backdrop with dominant color
        XvoxNowPlayingBackdrop(
            dominant = paletteState.color,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, dragAmount ->
                            val nextY = screenY + dragAmount
                            screenY = nextY.coerceAtLeast(0f)
                        },
                        onDragEnd = {
                            if (screenY > screenHeight * 0.28f) {
                                dismiss()
                            } else {
                                returnToRest()
                            }
                        },
                        onDragCancel = { returnToRest() }
                    )
                }
        ) {
            // Header
            XvoxNowPlayingHeader(
                onClose = ::dismiss,
                onShare = {
                    haptics.tap()
                    onShare?.invoke()
                },
                onMore = {
                    haptics.tap()
                    showQuickSettingsSheet = true
                },
                onLyrics = {
                    haptics.tap()
                    showLyrics = !showLyrics
                },
                showLyricsButton = true,
                playingSource = playingSource
            )

            // Center Artwork / Lyrics
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                if (showLyrics) {
                    XvoxArtworkLyrics(
                        state = lyricsState,
                        position = position,
                        onSeek = onSeek,
                        onAttach = lyricsViewModel::attach,
                        onDelete = lyricsViewModel::removeCustom,
                        onClose = { showLyrics = false },
                        onFullscreen = lyricsViewModel::openFullscreen,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp))
                    )
                } else {
                    XvoxNowPlayingArtworkPager(
                        queue = queue,
                        currentIndex = currentIndex,
                        navigationRequest = navigationRequest,
                        onArtworkTap = {
                            haptics.tap()
                            showLyrics = true
                        },
                        onSwipePalette = { base, adjacent, fraction ->
                            scope.launch { paletteState.blend(base, adjacent, fraction) }
                        },
                        onSettledPage = onPlayQueueIndex,
                        modifier = Modifier.fillMaxSize(),
                        repeatMode = repeatMode
                    )
                }
            }

            // Bottom Player Sheet (Actions, Title, Progress, Controls)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(colors.background.copy(alpha = 0.35f))
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 8.dp)
            ) {
                NowPlayingActions(
                    isLiked = isLiked,
                    onTimer = {
                        haptics.tap()
                        onTimer?.invoke()
                    },
                    onQueue = {
                        haptics.tap()
                        onQueue?.invoke()
                    },
                    onInfo = {
                        haptics.tap()
                        onInfo?.invoke()
                    },
                    onToggleLiked = {
                        haptics.success()
                        onToggleLiked?.invoke()
                    },
                    onStarPlaylist = {
                        haptics.tap()
                        onStarPlaylist?.invoke()
                    },
                    timerProgress = sleepTimerProgress,
                )

                Spacer(Modifier.height(20.dp))

                Text(
                    text = song.title,
                    color = colors.primaryText,
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = song.artist,
                    color = colors.secondaryText,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(14.dp))

                XvoxNowPlayingProgress(
                    position = position,
                    duration = duration,
                    onSeek = {
                        haptics.sliderTick()
                        onSeek(it)
                    }
                )

                Spacer(Modifier.height(8.dp))

                XvoxNowPlayingControls(
                    isPlaying = isPlaying,
                    isShuffleEnabled = isShuffleEnabled,
                    repeatMode = repeatMode,
                    onShuffle = {
                        haptics.toggle()
                        onToggleShuffle?.invoke()
                    },
                    onPrevious = ::requestPrevious,
                    onTogglePlay = {
                        haptics.click()
                        onTogglePlay()
                    },
                    onNext = ::requestNext,
                    onRepeat = {
                        haptics.toggle()
                        onToggleRepeat?.invoke()
                    },
                    currentIndex = currentIndex,
                    queueSize = queue.size,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "XVOX",
                    color = colors.primaryText.copy(alpha = 0.55f),
                    fontFamily = XvoxLogoFont,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(Modifier.height(4.dp))
            }
        }

        // Quick Settings Sheet from Three Dots
        if (showQuickSettingsSheet) {
            NowPlayingQuickSettingsSheet(
                onDismiss = { showQuickSettingsSheet = false },
                onTimer = onTimer,
                onInfo = onInfo,
                onStarPlaylist = onStarPlaylist,
                onShare = onShare
            )
        }
    }
}

@Composable
private fun NowPlayingQuickSettingsSheet(
    onDismiss: () -> Unit,
    onTimer: (() -> Unit)? = null,
    onInfo: (() -> Unit)? = null,
    onStarPlaylist: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current
    val context = LocalContext.current
    val prefs = remember { UserPreferencesRepository(context) }
    val scope = rememberCoroutineScope()

    val eqEnabled by prefs.equalizerEnabled.collectAsState(initial = false)
    val eqPreset by prefs.eqPreset.collectAsState(initial = "Flat")
    val gapless by prefs.gaplessPlayback.collectAsState(initial = true)
    val crossfade by prefs.crossfade.collectAsState(initial = false)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(colors.card)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick Audio Settings",
                    color = colors.primaryText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colors.cardElevated)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_xvox_close),
                        contentDescription = "Close",
                        tint = colors.primaryText,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Equalizer Quick Switch
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Equalizer Engine", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Preset: $eqPreset", color = colors.secondaryText, fontSize = 11.sp)
                }
                Switch(
                    checked = eqEnabled,
                    onCheckedChange = {
                        haptics.toggle()
                        scope.launch { prefs.setEqualizerEnabled(it) }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.background,
                        checkedTrackColor = colors.primaryAccent
                    )
                )
            }

            Spacer(Modifier.height(8.dp))

            // Crossfade Toggle
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Crossfade Tracks", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Seamless beat sync blend", color = colors.secondaryText, fontSize = 11.sp)
                }
                Switch(
                    checked = crossfade,
                    onCheckedChange = {
                        haptics.toggle()
                        scope.launch { prefs.setCrossfade(it) }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.background,
                        checkedTrackColor = colors.primaryAccent
                    )
                )
            }

            Spacer(Modifier.height(14.dp))

            // Quick Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (onTimer != null) {
                    QuickOptionButton("Sleep Timer", R.drawable.ic_xvox_timer, Modifier.weight(1f)) {
                        onDismiss()
                        onTimer()
                    }
                }
                if (onInfo != null) {
                    QuickOptionButton("Song Info", R.drawable.ic_xvox_info, Modifier.weight(1f)) {
                        onDismiss()
                        onInfo()
                    }
                }
                if (onStarPlaylist != null) {
                    QuickOptionButton("Add Playlist", R.drawable.ic_xvox_playlist, Modifier.weight(1f)) {
                        onDismiss()
                        onStarPlaylist()
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickOptionButton(
    title: String,
    iconRes: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.cardElevated)
            .clickable {
                haptics.tap()
                onClick()
            }
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = colors.primaryAccent,
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(text = title, color = colors.primaryText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun NowPlayingActions(
    isLiked: Boolean = false,
    onTimer: (() -> Unit)? = null,
    onQueue: (() -> Unit)? = null,
    onInfo: (() -> Unit)? = null,
    onToggleLiked: (() -> Unit)? = null,
    onStarPlaylist: (() -> Unit)? = null,
    timerProgress: Float? = null,
) {
    val colors = XvoxTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .background(colors.card.copy(alpha = 0.22f), RoundedCornerShape(22.dp))
                .padding(horizontal = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NowPlayingActionIcon(R.drawable.ic_xvox_timer, onClick = onTimer, progress = timerProgress)
            NowPlayingActionIcon(R.drawable.ic_xvox_queue, onClick = onQueue)
            NowPlayingActionIcon(R.drawable.ic_xvox_info, onClick = onInfo)
        }

        Spacer(Modifier.weight(1f))

        NowPlayingCircleAction(R.drawable.ic_xvox_star, onClick = onStarPlaylist)

        Spacer(Modifier.size(10.dp))

        NowPlayingCircleAction(
            if (isLiked) R.drawable.ic_xvox_heart else R.drawable.ic_xvox_heart_outline,
            tint = if (isLiked) colors.primaryAccent else colors.primaryText,
            onClick = onToggleLiked
        )
    }
}

@Composable
private fun NowPlayingActionIcon(
    resource: Int,
    onClick: (() -> Unit)? = null,
    progress: Float? = null
) {
    val colors = XvoxTheme.colors

    Box(
        modifier = Modifier
            .size(42.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = onClick != null,
                onClick = { onClick?.invoke() }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (progress != null) {
            androidx.compose.foundation.Canvas(modifier = Modifier.size(32.dp)) {
                val stroke = 2.5.dp.toPx()
                drawArc(
                    color = colors.mutedText.copy(alpha = 0.22f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
                )
                drawArc(
                    color = colors.primaryAccent,
                    startAngle = -90f,
                    sweepAngle = progress * 360f,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
            }
        }
        Icon(
            painter = painterResource(resource),
            contentDescription = null,
            tint = colors.primaryText,
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable
private fun NowPlayingCircleAction(
    resource: Int,
    tint: Color? = null,
    onClick: (() -> Unit)? = null
) {
    val colors = XvoxTheme.colors

    Box(
        modifier = Modifier
            .size(42.dp)
            .background(colors.card.copy(alpha = 0.22f), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = onClick != null,
                onClick = { onClick?.invoke() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(resource),
            contentDescription = null,
            tint = tint ?: colors.primaryText,
            modifier = Modifier.size(19.dp)
        )
    }
}
