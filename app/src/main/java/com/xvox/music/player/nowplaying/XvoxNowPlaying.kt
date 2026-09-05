package com.xvox.music.player.nowplaying

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
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
import com.xvox.music.core.ui.miniplayer.XvoxPlayerTransitionMotion
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
import kotlin.math.abs
import kotlin.math.roundToInt

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
    val lyricsState by lyricsViewModel.state.collectAsState()
    val paletteState = rememberXvoxNowPlayingPalette(song, queue, currentIndex)
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val screenHeight =
        with(density) {
            LocalConfiguration.current.screenHeightDp.dp.toPx()
        }

    var screenY by rememberSaveable {
        mutableFloatStateOf(
            screenHeight
        )
    }

    var entered by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(screenHeight) {
        if (
            !entered &&
            screenHeight > 0f
        ) {
            entered = true
            screenY = screenHeight

            animateScreen(
                target = 0f
            )
        }
    }
    var showLyrics by remember {
        mutableStateOf(false)
    }
    var showQuickSettingsSheet by remember {
        mutableStateOf(false)
    }
    var dismissing by remember {
        mutableStateOf(false)
    }
    var navigationRequest by remember {
        mutableIntStateOf(0)
    }
    var motionJob by remember {
        mutableStateOf<Job?>(null)
    }

    fun animateScreen(
        target: Float,
        finished: (() -> Unit)? = null
    ) {
        motionJob?.cancel()
        val start = screenY

        motionJob = scope.launch {
            val animation = Animatable(start)
            animation.animateTo(
                target,
                tween(
                    durationMillis = XvoxPlayerTransitionMotion.Duration,
                    easing = XvoxPlayerTransitionMotion.easing
                )
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
        animateScreen(
            target = screenHeight,
            finished = onClose
        )
    }

    fun returnToRest() {
        animateScreen(0f)
    }

    fun requestPrevious() {
        if (
            queue.isEmpty() ||
            currentIndex < 0
        ) return

        val atFirst =
            currentIndex <= 0

        if (
            atFirst &&
            repeatMode != RepeatMode.ALL
        ) return

        if (showLyrics) {
            val target =
                if (atFirst) queue.lastIndex
                else currentIndex - 1

            onPlayQueueIndex(target)
        } else {
            navigationRequest =
                -(abs(navigationRequest) + 1)
        }
    }

    fun requestNext() {
        if (
            queue.isEmpty() ||
            currentIndex < 0
        ) return

        val atLast =
            currentIndex >= queue.lastIndex

        if (
            atLast &&
            repeatMode != RepeatMode.ALL
        ) return

        if (showLyrics) {
            val target =
                if (atLast) 0
                else currentIndex + 1

            onPlayQueueIndex(target)
        } else {
            navigationRequest =
                abs(navigationRequest) + 1
        }
    }

    LaunchedEffect(song.id) {
        lyricsViewModel.load(song)
    }

    BackHandler {
        when {
            showQuickSettingsSheet ->
                showQuickSettingsSheet = false
            lyricsState.fullscreen ->
                lyricsViewModel.closeFullscreen()
            showLyrics ->
                showLyrics = false
            else ->
                dismiss()
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
            }
    ) {
        XvoxNowPlayingBackdrop(
            dominant = paletteState.color,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = {
                                _,
                                dragAmount ->

                            screenY =
                                (screenY + dragAmount)
                                    .coerceAtLeast(0f)
                        },
                        onDragEnd = {
                            if (
                                screenY >
                                screenHeight * 0.25f
                            ) {
                                dismiss()
                            } else {
                                returnToRest()
                            }
                        },
                        onDragCancel = {
                            returnToRest()
                        }
                    )
                }
        ) {
            XvoxNowPlayingHeader(
                onClose = ::dismiss,
                onShare = {
                    onShare?.invoke()
                },
                onMore = {
                    showQuickSettingsSheet = true
                },
                playingSource = playingSource
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(
                        horizontal = 2.dp,
                        vertical = 4.dp
                    ),
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
                            showLyrics = false
                        },
                        onFullscreen =
                            lyricsViewModel::openFullscreen,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                horizontal = 8.dp,
                                vertical = 8.dp
                            )
                            .clip(
                                RoundedCornerShape(20.dp)
                            )
                    )
                } else {
                    XvoxNowPlayingArtworkPager(
                        queue = queue,
                        currentIndex = currentIndex,
                        navigationRequest = navigationRequest,
                        onArtworkTap = {
                            showLyrics = true
                        },
                        onSwipePalette = {
                                base,
                                adjacent,
                                fraction ->

                            scope.launch {
                                paletteState.blend(
                                    base,
                                    adjacent,
                                    fraction
                                )
                            }
                        },
                        onSettledPage =
                            onPlayQueueIndex,
                        modifier =
                            Modifier.fillMaxSize(),
                        repeatMode =
                            repeatMode
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(
                        RoundedCornerShape(
                            topStart = 28.dp,
                            topEnd = 28.dp
                        )
                    )
                    .background(
                        colors.background.copy(
                            alpha = 0.35f
                        )
                    )
                    .windowInsetsPadding(
                        WindowInsets.navigationBars
                    )
                    .padding(
                        start = 14.dp,
                        top = 12.dp,
                        end = 14.dp,
                        bottom = 8.dp
                    )
            ) {
                NowPlayingActions(
                    isLiked = isLiked,
                    isInPlaylist = isInPlaylist,
                    onTimer = {
                        onTimer?.invoke()
                    },
                    onQueue = {
                        onQueue?.invoke()
                    },
                    onInfo = {
                        onInfo?.invoke()
                    },
                    onToggleLiked = {
                        onToggleLiked?.invoke()
                    },
                    onStarPlaylist = {
                        onStarPlaylist?.invoke()
                    },
                    timerProgress =
                        sleepTimerProgress
                )

                Spacer(
                    Modifier.height(18.dp)
                )

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

                Spacer(
                    Modifier.height(14.dp)
                )

                XvoxNowPlayingProgress(
                    position = position,
                    duration = duration,
                    onSeek = onSeek
                )

                Spacer(
                    Modifier.height(8.dp)
                )

                XvoxNowPlayingControls(
                    isPlaying = isPlaying,
                    isShuffleEnabled = isShuffleEnabled,
                    repeatMode = repeatMode,
                    onShuffle = {
                        onToggleShuffle?.invoke()
                    },
                    onPrevious =
                        ::requestPrevious,
                    onTogglePlay =
                        onTogglePlay,
                    onNext =
                        ::requestNext,
                    onRepeat = {
                        onToggleRepeat?.invoke()
                    },
                    currentIndex =
                        currentIndex,
                    queueSize =
                        queue.size,
                    modifier =
                        Modifier.fillMaxWidth()
                )

                Spacer(
                    Modifier.height(14.dp)
                )

                Text(
                    text = "XVOX",
                    color =
                        colors.primaryText.copy(
                            alpha = 0.55f
                        ),
                    fontFamily =
                        XvoxLogoFont,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    modifier =
                        Modifier.align(
                            Alignment.CenterHorizontally
                        )
                )

                Spacer(
                    Modifier.height(4.dp)
                )
            }
        }

        if (showQuickSettingsSheet) {
            NowPlayingOptionsSheet(
                onDismiss = {
                    showQuickSettingsSheet = false
                },
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
    val context = LocalContext.current
    val prefs = remember {
        UserPreferencesRepository(context)
    }
    val scope = rememberCoroutineScope()

    val eqEnabled by
        prefs.equalizerEnabled.collectAsState(
            initial = false
        )
    val eqPreset by
        prefs.eqPreset.collectAsState(
            initial = "Flat"
        )
    val eqBands by
        prefs.eqBands.collectAsState(
            initial =
                listOf(0, 0, 0, 0, 0)
        )
    val gapless by
        prefs.gaplessPlayback.collectAsState(
            initial = true
        )
    val crossfade by
        prefs.crossfade.collectAsState(
            initial = false
        )
    val crossfadeDuration by
        prefs.crossfadeDuration.collectAsState(
            initial = 3
        )
    val fadeInEnabled by
        prefs.fadeIn.collectAsState(
            initial = false
        )
    val fadeOutEnabled by
        prefs.fadeOut.collectAsState(
            initial = false
        )
    val skipSilence by
        prefs.skipSilence.collectAsState(
            initial = false
        )

    var visible by remember {
        mutableStateOf(false)
    }
    var closing by remember {
        mutableStateOf(false)
    }
    var sheetDragOffset by remember {
        mutableFloatStateOf(0f)
    }

    fun close() {
        if (closing) return
        closing = true
        visible = false

        scope.launch {
            delay(
                XvoxPlayerTransitionMotion
                    .Duration.toLong()
            )
            onDismiss()
        }
    }

    LaunchedEffect(Unit) {
        visible = true
    }

    BackHandler {
        close()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .pointerInput(Unit) {
                detectTapGestures {
                    close()
                }
            }
    ) {
        val maxSheetHeight =
            (maxHeight * 0.85f)
                .coerceAtLeast(
                    280.dp
                )

        AnimatedVisibility(
            visible = visible,
            modifier =
                Modifier.align(
                    Alignment.BottomCenter
                ),
            enter =
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec =
                        tween(
                            XvoxPlayerTransitionMotion.Duration,
                            easing =
                                XvoxPlayerTransitionMotion.easing
                        )
                ) +
                    fadeIn(
                        tween(
                            XvoxPlayerTransitionMotion.Duration,
                            easing =
                                XvoxPlayerTransitionMotion.easing
                        )
                    ),
            exit =
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec =
                        tween(
                            XvoxPlayerTransitionMotion.Duration,
                            easing =
                                XvoxPlayerTransitionMotion.easing
                        )
                ) +
                    fadeOut(
                        tween(
                            XvoxPlayerTransitionMotion.Duration,
                            easing =
                                XvoxPlayerTransitionMotion.easing
                        )
                    )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(
                        max =
                            maxSheetHeight
                    )
                    .graphicsLayer {
                        translationY =
                            sheetDragOffset
                                .coerceAtLeast(0f)
                    }
                    .clip(
                        RoundedCornerShape(
                            topStart = 28.dp,
                            topEnd = 28.dp
                        )
                    )
                    .background(
                        colors.cardElevated
                            .copy(
                                alpha = 0.98f
                            )
                    )
                    .border(
                        1.dp,
                        Color.White.copy(
                            alpha = 0.08f
                        ),
                        RoundedCornerShape(
                            topStart = 28.dp,
                            topEnd = 28.dp
                        )
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                tryAwaitRelease()
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onVerticalDrag = {
                                    change,
                                    dragAmount ->

                                change.consume()
                                sheetDragOffset =
                                    (
                                        sheetDragOffset +
                                            dragAmount
                                        ).coerceAtLeast(
                                        0f
                                    )
                            },
                            onDragEnd = {
                                if (
                                    sheetDragOffset >
                                    90f
                                ) {
                                    close()
                                } else {
                                    sheetDragOffset = 0f
                                }
                            },
                            onDragCancel = {
                                sheetDragOffset = 0f
                            }
                        )
                    }
                    .windowInsetsPadding(
                        WindowInsets.navigationBars
                    )
                    .padding(
                        horizontal = 16.dp,
                        vertical = 10.dp
                    )
            ) {
                Box(
                    modifier = Modifier
                        .align(
                            Alignment.CenterHorizontally
                        )
                        .padding(
                            bottom = 8.dp
                        )
                        .width(44.dp)
                        .height(4.dp)
                        .clip(
                            RoundedCornerShape(
                                2.dp
                            )
                        )
                        .background(
                            colors.cardBorder
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            bottom = 6.dp
                        ),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Text(
                        text =
                            "Audio & Playback Controls",
                        color =
                            colors.primaryText,
                        fontSize = 17.sp,
                        fontWeight =
                            FontWeight.Bold,
                        modifier =
                            Modifier.weight(1f)
                    )

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                colors.card.copy(
                                    alpha = 0.94f
                                )
                            )
                            .clickable {
                                close()
                            },
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Icon(
                            painter =
                                painterResource(
                                    R.drawable
                                        .ic_xvox_close
                                ),
                            contentDescription =
                                "Close",
                            tint =
                                colors.primaryText,
                            modifier =
                                Modifier.size(
                                    15.dp
                                )
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(
                            rememberScrollState()
                        )
                ) {
                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(
                                8.dp
                            )
                    ) {
                        if (onTimer != null) {
                            QuickOptionButton(
                                "Sleep Timer",
                                R.drawable.ic_xvox_timer,
                                Modifier.weight(1f)
                            ) {
                                close()
                                onTimer()
                            }
                        }

                        if (onInfo != null) {
                            QuickOptionButton(
                                "Song Info",
                                R.drawable.ic_xvox_info,
                                Modifier.weight(1f)
                            ) {
                                close()
                                onInfo()
                            }
                        }

                        if (onStarPlaylist != null) {
                            QuickOptionButton(
                                "Add Playlist",
                                R.drawable.ic_xvox_playlist,
                                Modifier.weight(1f)
                            ) {
                                close()
                                onStarPlaylist()
                            }
                        }
                    }

                    Spacer(
                        Modifier.height(16.dp)
                    )

                    Text(
                        text =
                            "Equalizer & DSP",
                        color =
                            colors.primaryText,
                        fontSize = 14.sp,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        Modifier.height(6.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                vertical = 4.dp
                            ),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Column(
                            modifier =
                                Modifier.weight(1f)
                        ) {
                            Text(
                                "Master Equalizer",
                                color =
                                    colors.primaryText,
                                fontSize = 13.sp,
                                fontWeight =
                                    FontWeight.Medium
                            )

                            Text(
                                "Preset: $eqPreset",
                                color =
                                    colors.secondaryText,
                                fontSize = 11.sp
                            )
                        }

                        Switch(
                            checked = eqEnabled,
                            onCheckedChange = {
                                scope.launch {
                                    prefs.setEqualizerEnabled(
                                        it
                                    )
                                }
                            },
                            colors =
                                SwitchDefaults.colors(
                                    checkedThumbColor =
                                        colors.background,
                                    checkedTrackColor =
                                        colors.primaryAccent
                                )
                        )
                    }

                    if (eqEnabled) {
                        Spacer(
                            Modifier.height(6.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(
                                    rememberScrollState()
                                ),
                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    6.dp
                                )
                        ) {
                            val presets =
                                listOf(
                                    "Flat",
                                    "Bass Boost",
                                    "Treble",
                                    "Rock",
                                    "Pop",
                                    "Jazz",
                                    "Electronic",
                                    "Vocal",
                                    "Custom"
                                )

                            presets.forEach { preset ->
                                val selected =
                                    eqPreset == preset

                                Box(
                                    modifier = Modifier
                                        .clip(
                                            RoundedCornerShape(
                                                8.dp
                                            )
                                        )
                                        .background(
                                            if (selected) {
                                                colors.primaryAccent
                                            } else {
                                                colors.cardElevated
                                            }
                                        )
                                        .clickable {
                                            scope.launch {
                                                prefs.setEqPreset(
                                                    preset
                                                )

                                                if (
                                                    preset != "Custom" &&
                                                    AudioEffectsManager
                                                        .PRESETS
                                                        .containsKey(
                                                            preset
                                                        )
                                                ) {
                                                    prefs.setEqBands(
                                                        AudioEffectsManager
                                                            .PRESETS[
                                                            preset
                                                        ] ?: listOf(
                                                            0,
                                                            0,
                                                            0,
                                                            0,
                                                            0
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                        .padding(
                                            horizontal = 10.dp,
                                            vertical = 6.dp
                                        ),
                                    contentAlignment =
                                        Alignment.Center
                                ) {
                                    Text(
                                        text = preset,
                                        color =
                                            if (selected) {
                                                colors.background
                                            } else {
                                                colors.primaryText
                                            },
                                        fontSize = 11.sp,
                                        fontWeight =
                                            if (selected) {
                                                FontWeight.Bold
                                            } else {
                                                FontWeight.Normal
                                            }
                                    )
                                }
                            }
                        }

                        Spacer(
                            Modifier.height(10.dp)
                        )

                        val labels =
                            listOf(
                                "60 Hz",
                                "230 Hz",
                                "910 Hz",
                                "3.6 kHz",
                                "14 kHz"
                            )

                        labels.forEachIndexed {
                                index,
                                label ->

                            val value =
                                eqBands.getOrElse(
                                    index
                                ) {
                                    0
                                }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        vertical = 2.dp
                                    ),
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    color =
                                        colors.secondaryText,
                                    fontSize = 11.sp,
                                    modifier =
                                        Modifier.width(
                                            55.dp
                                        )
                                )

                                XvoxThinLineSlider(
                                    value =
                                        value.toFloat(),
                                    onValueChange = {
                                            newValue ->

                                        val updated =
                                            eqBands.toMutableList()

                                        while (
                                            updated.size <=
                                            index
                                        ) {
                                            updated.add(0)
                                        }

                                        updated[index] =
                                            newValue.roundToInt()

                                        scope.launch {
                                            prefs.setEqBands(
                                                updated
                                            )
                                            prefs.setEqPreset(
                                                "Custom"
                                            )
                                        }
                                    },
                                    valueRange =
                                        -15f..15f,
                                    modifier =
                                        Modifier.weight(
                                            1f
                                        )
                                )

                                Text(
                                    text =
                                        "${if (value > 0) "+" else ""}$value dB",
                                    color =
                                        colors.primaryText,
                                    fontSize = 10.sp,
                                    modifier =
                                        Modifier.width(
                                            44.dp
                                        )
                                )
                            }
                        }
                    }

                    Spacer(
                        Modifier.height(14.dp)
                    )

                    Text(
                        text =
                            "Playback Settings",
                        color =
                            colors.primaryText,
                        fontSize = 14.sp,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        Modifier.height(6.dp)
                    )

                    PlaybackSwitchRow(
                        title =
                            "Gapless Playback",
                        checked =
                            gapless,
                        onCheckedChange = {
                            scope.launch {
                                prefs.setGaplessPlayback(
                                    it
                                )
                            }
                        }
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                vertical = 4.dp
                            ),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Column(
                            modifier =
                                Modifier.weight(1f)
                        ) {
                            Text(
                                "Crossfade Tracks",
                                color =
                                    colors.primaryText,
                                fontSize = 13.sp
                            )

                            if (crossfade) {
                                Text(
                                    "${crossfadeDuration}s duration",
                                    color =
                                        colors.secondaryText,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Switch(
                            checked =
                                crossfade,
                            onCheckedChange = {
                                scope.launch {
                                    prefs.setCrossfade(
                                        it
                                    )
                                }
                            },
                            colors =
                                SwitchDefaults.colors(
                                    checkedThumbColor =
                                        colors.background,
                                    checkedTrackColor =
                                        colors.primaryAccent
                                )
                        )
                    }

                    if (crossfade) {
                        XvoxThinLineSlider(
                            value =
                                crossfadeDuration.toFloat(),
                            onValueChange = {
                                scope.launch {
                                    prefs.setCrossfadeDuration(
                                        it.roundToInt()
                                    )
                                }
                            },
                            valueRange =
                                1f..12f,
                            modifier =
                                Modifier.fillMaxWidth()
                        )
                    }

                    PlaybackSwitchRow(
                        title =
                            "Fade In on Start",
                        checked =
                            fadeInEnabled,
                        onCheckedChange = {
                            scope.launch {
                                prefs.setFadeIn(it)
                            }
                        }
                    )

                    PlaybackSwitchRow(
                        title =
                            "Fade Out on Pause",
                        checked =
                            fadeOutEnabled,
                        onCheckedChange = {
                            scope.launch {
                                prefs.setFadeOut(it)
                            }
                        }
                    )

                    PlaybackSwitchRow(
                        title =
                            "Skip Silence",
                        checked =
                            skipSilence,
                        onCheckedChange = {
                            scope.launch {
                                prefs.setSkipSilence(
                                    it
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaybackSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = XvoxTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = colors.primaryText,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = checked,
            onCheckedChange =
                onCheckedChange,
            colors =
                SwitchDefaults.colors(
                    checkedThumbColor =
                        colors.background,
                    checkedTrackColor =
                        colors.primaryAccent
                )
        )
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

    Row(
        modifier = modifier
            .clip(
                RoundedCornerShape(
                    12.dp
                )
            )
            .background(colors.card)
            .clickable {
                onClick()
            }
            .padding(
                vertical = 10.dp,
                horizontal = 6.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically,
        horizontalArrangement =
            Arrangement.Center
    ) {
        Icon(
            painter =
                painterResource(iconRes),
            contentDescription = null,
            tint =
                colors.primaryAccent,
            modifier =
                Modifier.size(15.dp)
        )

        Spacer(
            Modifier.width(6.dp)
        )

        Text(
            text = title,
            color =
                colors.primaryText,
            fontSize = 11.sp,
            fontWeight =
                FontWeight.SemiBold
        )
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
    timerProgress: Float? = null
) {
    val colors = XvoxTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .background(
                    colors.card.copy(
                        alpha = 0.22f
                    ),
                    RoundedCornerShape(
                        22.dp
                    )
                )
                .padding(
                    horizontal = 3.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            NowPlayingActionIcon(
                R.drawable.ic_xvox_timer,
                onClick = onTimer,
                progress = timerProgress
            )

            NowPlayingActionIcon(
                R.drawable.ic_xvox_queue,
                onClick = onQueue
            )

            NowPlayingActionIcon(
                R.drawable.ic_xvox_info,
                onClick = onInfo
            )
        }

        Spacer(
            Modifier.weight(1f)
        )

        NowPlayingCircleAction(
            R.drawable.ic_xvox_star,
            tint =
                if (isInPlaylist) {
                    colors.primaryAccent
                } else {
                    colors.primaryText
                },
            onClick =
                onStarPlaylist
        )

        Spacer(
            Modifier.size(10.dp)
        )

        NowPlayingCircleAction(
            if (isLiked) {
                R.drawable.ic_xvox_heart
            } else {
                R.drawable.ic_xvox_heart_outline
            },
            tint =
                if (isLiked) {
                    colors.primaryAccent
                } else {
                    colors.primaryText
                },
            onClick =
                onToggleLiked
        )
    }
}

@Composable
private fun NowPlayingActionIcon(
    resource: Int,
    onClick: (() -> Unit)? = null,
    progress: Float? = null
) {
    val colors =
        XvoxTheme.colors

    Box(
        modifier = Modifier
            .size(42.dp)
            .clickable(
                interactionSource =
                    remember {
                        MutableInteractionSource()
                    },
                indication = null,
                enabled =
                    onClick != null,
                onClick = {
                    onClick?.invoke()
                }
            ),
        contentAlignment =
            Alignment.Center
    ) {
        if (progress != null) {
            androidx.compose.foundation.Canvas(
                modifier =
                    Modifier.size(32.dp)
            ) {
                val stroke =
                    2.5.dp.toPx()

                drawArc(
                    color =
                        colors.mutedText.copy(
                            alpha = 0.22f
                        ),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style =
                        androidx.compose.ui.graphics.drawscope.Stroke(
                            width = stroke
                        )
                )

                drawArc(
                    color =
                        colors.primaryAccent,
                    startAngle = -90f,
                    sweepAngle =
                        progress * 360f,
                    useCenter = false,
                    style =
                        androidx.compose.ui.graphics.drawscope.Stroke(
                            width = stroke,
                            cap =
                                androidx.compose.ui.graphics.StrokeCap.Round
                        )
                )
            }
        }

        Icon(
            painter =
                painterResource(resource),
            contentDescription = null,
            tint =
                colors.primaryText,
            modifier =
                Modifier.size(19.dp)
        )
    }
}

@Composable
private fun NowPlayingCircleAction(
    resource: Int,
    tint: Color? = null,
    onClick: (() -> Unit)? = null
) {
    val colors =
        XvoxTheme.colors

    Box(
        modifier = Modifier
            .size(42.dp)
            .background(
                colors.card.copy(
                    alpha = 0.22f
                ),
                CircleShape
            )
            .clickable(
                interactionSource =
                    remember {
                        MutableInteractionSource()
                    },
                indication = null,
                enabled =
                    onClick != null,
                onClick = {
                    onClick?.invoke()
                }
            ),
        contentAlignment =
            Alignment.Center
    ) {
        Icon(
            painter =
                painterResource(resource),
            contentDescription = null,
            tint =
                tint
                    ?: colors.primaryAccent,
            modifier =
                Modifier.size(19.dp)
        )
    }
}
