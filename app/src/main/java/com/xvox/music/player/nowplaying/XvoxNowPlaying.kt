package com.xvox.music.player.nowplaying

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.xvox.music.features.settings.components.XvoxThinLineSlider
import com.xvox.music.player.nowplaying.lyrics.XvoxArtworkLyrics
import com.xvox.music.player.nowplaying.lyrics.XvoxFullscreenLyrics
import com.xvox.music.player.nowplaying.lyrics.XvoxLyricsViewModel
import com.xvox.music.player.playback.RepeatMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val NowPlayingEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

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
    isInPlaylist: Boolean = false,
    lyricsViewModel: XvoxLyricsViewModel = viewModel()
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current
    val lyricsState by lyricsViewModel.state.collectAsState()

    val paletteState = rememberXvoxNowPlayingPalette(song, queue, currentIndex)

    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val screenHeight = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }

    var screenY by rememberSaveable { mutableFloatStateOf(0f) }
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
                tween(durationMillis = durationMs, easing = NowPlayingEasing)
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
            durationMs = 350,
            finished = onClose
        )
    }

    fun returnToRest() {
        val fraction = if (screenHeight > 0f) (screenY / screenHeight).coerceIn(0f, 1f) else 1f
        animateScreen(0f, (350 * fraction).toInt().coerceAtLeast(140))
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
        // Vibrant dominant glow gradient backdrop
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
            // Header (Lyrics icon removed from top right pill)
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
                playingSource = playingSource
            )

            // Center Artwork / Lyrics
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (showLyrics) {
                    XvoxArtworkLyrics(
                        state = lyricsState,
                        position = position,
                        onSeek = onSeek,
                        onAttach = lyricsViewModel::attach,
                        onDelete = lyricsViewModel::removeCustom,
                        onClose = {
                            haptics.tap()
                            showLyrics = false // ONLY closes lyrics
                        },
                        onFullscreen = lyricsViewModel::openFullscreen,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp)
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
                    isInPlaylist = isInPlaylist,
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

                Spacer(Modifier.height(18.dp))

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

        // Height-Adjustable Quick Settings, Equalizer & Playback Bottom Sheet with transparent backdrop
        if (showQuickSettingsSheet) {
            NowPlayingOptionsSheet(
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
private fun NowPlayingOptionsSheet(
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
    val eqBands by prefs.eqBands.collectAsState(initial = listOf(0, 0, 0, 0, 0))
    val gapless by prefs.gaplessPlayback.collectAsState(initial = true)
    val crossfade by prefs.crossfade.collectAsState(initial = false)
    val crossfadeDuration by prefs.crossfadeDuration.collectAsState(initial = 3)
    val fadeIn by prefs.fadeIn.collectAsState(initial = false)
    val fadeOut by prefs.fadeOut.collectAsState(initial = false)
    val skipSilence by prefs.skipSilence.collectAsState(initial = false)

    var visible by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }
    var sheetDragOffset by remember { mutableFloatStateOf(0f) }

    fun close() {
        if (closing) return
        closing = true
        visible = false
        scope.launch {
            delay(280L)
            onDismiss()
        }
    }

    LaunchedEffect(Unit) { visible = true }
    BackHandler { close() }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .pointerInput(Unit) { detectTapGestures { close() } }
    ) {
        val maxSheetHeight = (maxHeight * 0.85f).coerceAtLeast(280.dp)

        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(350, easing = NowPlayingEasing)
            ) + fadeIn(tween(260, easing = NowPlayingEasing)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(320, easing = NowPlayingEasing)
            ) + fadeOut(tween(220, easing = NowPlayingEasing))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxSheetHeight)
                    .graphicsLayer {
                        translationY = sheetDragOffset.coerceAtLeast(0f)
                    }
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(colors.cardElevated.copy(alpha = 0.98f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .pointerInput(Unit) { detectTapGestures(onPress = { tryAwaitRelease() }) }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                sheetDragOffset = (sheetDragOffset + dragAmount).coerceAtLeast(0f)
                            },
                            onDragEnd = {
                                if (sheetDragOffset > 90f) {
                                    close()
                                } else {
                                    sheetDragOffset = 0f
                                }
                            },
                            onDragCancel = { sheetDragOffset = 0f }
                        )
                    }
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                // Top Drag Handle Indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 8.dp)
                        .width(44.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.cardBorder)
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Audio & Playback Controls",
                        color = colors.primaryText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colors.card.copy(alpha = 0.94f))
                            .clickable { close() },
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

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Quick Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (onTimer != null) {
                            QuickOptionButton("Sleep Timer", R.drawable.ic_xvox_timer, Modifier.weight(1f)) {
                                close()
                                onTimer()
                            }
                        }
                        if (onInfo != null) {
                            QuickOptionButton("Song Info", R.drawable.ic_xvox_info, Modifier.weight(1f)) {
                                close()
                                onInfo()
                            }
                        }
                        if (onStarPlaylist != null) {
                            QuickOptionButton("Add Playlist", R.drawable.ic_xvox_playlist, Modifier.weight(1f)) {
                                close()
                                onStarPlaylist()
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Section 1: Equalizer
                    Text(text = "Equalizer & DSP", color = colors.primaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Master Equalizer", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
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

                    if (eqEnabled) {
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val presets = listOf("Flat", "Bass Boost", "Treble", "Rock", "Pop", "Jazz", "Electronic", "Vocal", "Custom")
                            presets.forEach { p ->
                                val isSelected = eqPreset == p
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) colors.primaryAccent else colors.cardElevated)
                                        .clickable {
                                            haptics.tap()
                                            scope.launch {
                                                prefs.setEqPreset(p)
                                                if (p != "Custom" && AudioEffectsManager.PRESETS.containsKey(p)) {
                                                    val bandVals = AudioEffectsManager.PRESETS[p] ?: listOf(0, 0, 0, 0, 0)
                                                    prefs.setEqBands(bandVals)
                                                }
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = p,
                                        color = if (isSelected) colors.background else colors.primaryText,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        val bandLabels = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")
                        bandLabels.forEachIndexed { index, label ->
                            val currentVal = eqBands.getOrElse(index) { 0 }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = label, color = colors.secondaryText, fontSize = 11.sp, modifier = Modifier.width(55.dp))
                                XvoxThinLineSlider(
                                    value = currentVal.toFloat(),
                                    onValueChange = { newVal ->
                                        haptics.sliderTick()
                                        val updated = eqBands.toMutableList()
                                        while (updated.size <= index) updated.add(0)
                                        updated[index] = newVal.roundToInt()
                                        scope.launch {
                                            prefs.setEqBands(updated)
                                            prefs.setEqPreset("Custom")
                                        }
                                    },
                                    valueRange = -15f..15f,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${if (currentVal > 0) "+" else ""}$currentVal dB",
                                    color = colors.primaryText,
                                    fontSize = 10.sp,
                                    modifier = Modifier.width(44.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Section 2: Playback Options
                    Text(text = "Playback Settings", color = colors.primaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Gapless Playback", color = colors.primaryText, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Switch(
                            checked = gapless,
                            onCheckedChange = {
                                haptics.toggle()
                                scope.launch { prefs.setGaplessPlayback(it) }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.background,
                                checkedTrackColor = colors.primaryAccent
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Crossfade Tracks", color = colors.primaryText, fontSize = 13.sp)
                            if (crossfade) Text("${crossfadeDuration}s duration", color = colors.secondaryText, fontSize = 11.sp)
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

                    if (crossfade) {
                        XvoxThinLineSlider(
                            value = crossfadeDuration.toFloat(),
                            onValueChange = {
                                haptics.sliderTick()
                                scope.launch { prefs.setCrossfadeDuration(it.roundToInt()) }
                            },
                            valueRange = 1f..12f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Fade In on Start", color = colors.primaryText, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Switch(
                            checked = fadeIn,
                            onCheckedChange = {
                                haptics.toggle()
                                scope.launch { prefs.setFadeIn(it) }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.background,
                                checkedTrackColor = colors.primaryAccent
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Fade Out on Pause", color = colors.primaryText, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Switch(
                            checked = fadeOut,
                            onCheckedChange = {
                                haptics.toggle()
                                scope.launch { prefs.setFadeOut(it) }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.background,
                                checkedTrackColor = colors.primaryAccent
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Skip Silence", color = colors.primaryText, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Switch(
                            checked = skipSilence,
                            onCheckedChange = {
                                haptics.toggle()
                                scope.launch { prefs.setSkipSilence(it) }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.background,
                                checkedTrackColor = colors.primaryAccent
                            )
                        )
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
            .background(colors.card)
            .clickable {
                haptics.tap()
                onClick()
            }
            .padding(vertical = 10.dp, horizontal = 6.dp),
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
    isInPlaylist: Boolean = false,
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

        NowPlayingCircleAction(
            R.drawable.ic_xvox_star,
            tint = if (isInPlaylist) colors.primaryAccent else colors.primaryText,
            onClick = onStarPlaylist
        )

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
            tint = tint ?: colors.primaryAccent,
            modifier = Modifier.size(19.dp)
        )
    }
}
