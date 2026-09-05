package com.xvox.music.player.nowplaying.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.miniplayer.XvoxPlayerTransitionMotion
import com.xvox.music.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun NowPlayingOptionsSheet(
    onDismiss: () -> Unit,
    onTimer: (() -> Unit)? = null,
    onInfo: (() -> Unit)? = null,
    onStarPlaylist: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null
) {
    val colors = XvoxTheme.colors
    val context = LocalContext.current
    val prefs = remember { UserPreferencesRepository(context) }
    val scope = rememberCoroutineScope()

    val eqEnabled by prefs.equalizerEnabled.collectAsState(initial = false)
    val eqPreset by prefs.eqPreset.collectAsState(initial = "Flat")
    val eqBands by prefs.eqBands.collectAsState(initial = listOf(0, 0, 0, 0, 0))
    val gapless by prefs.gaplessPlayback.collectAsState(initial = true)
    val crossfade by prefs.crossfade.collectAsState(initial = false)
    val crossfadeDuration by prefs.crossfadeDuration.collectAsState(initial = 3)
    val fadeInEnabled by prefs.fadeIn.collectAsState(initial = false)
    val fadeOutEnabled by prefs.fadeOut.collectAsState(initial = false)
    val skipSilence by prefs.skipSilence.collectAsState(initial = false)

    var visible by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }
    var sheetDragOffset by remember { mutableFloatStateOf(0f) }

    fun close() {
        if (closing) return
        closing = true
        visible = false

        scope.launch {
            delay(XvoxPlayerTransitionMotion.Duration.toLong())
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
                detectTapGestures { close() }
            }
    ) {
        val maxSheetHeight = (maxHeight * 0.85f).coerceAtLeast(280.dp)

        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(XvoxPlayerTransitionMotion.Duration, easing = XvoxPlayerTransitionMotion.easing)
            ) + fadeIn(tween(XvoxPlayerTransitionMotion.Duration, easing = XvoxPlayerTransitionMotion.easing)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(XvoxPlayerTransitionMotion.Duration, easing = XvoxPlayerTransitionMotion.easing)
            ) + fadeOut(tween(XvoxPlayerTransitionMotion.Duration, easing = XvoxPlayerTransitionMotion.easing))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxSheetHeight)
                    .graphicsLayer { translationY = sheetDragOffset.coerceAtLeast(0f) }
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
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

                    NowPlayingEqualizerSection(
                        eqEnabled = eqEnabled,
                        eqPreset = eqPreset,
                        eqBands = eqBands,
                        onToggleEq = { scope.launch { prefs.setEqualizerEnabled(it) } },
                        onSelectPreset = { scope.launch { prefs.setEqPreset(it) } },
                        onBandsChange = { bands ->
                            scope.launch {
                                prefs.setEqBands(bands)
                                prefs.setEqPreset("Custom")
                            }
                        }
                    )

                    Spacer(Modifier.height(14.dp))

                    NowPlayingPlaybackSettingsSection(
                        gapless = gapless,
                        crossfade = crossfade,
                        crossfadeDuration = crossfadeDuration,
                        fadeInEnabled = fadeInEnabled,
                        fadeOutEnabled = fadeOutEnabled,
                        skipSilence = skipSilence,
                        onToggleGapless = { scope.launch { prefs.setGaplessPlayback(it) } },
                        onToggleCrossfade = { scope.launch { prefs.setCrossfade(it) } },
                        onCrossfadeDurationChange = { scope.launch { prefs.setCrossfadeDuration(it) } },
                        onToggleFadeIn = { scope.launch { prefs.setFadeIn(it) } },
                        onToggleFadeOut = { scope.launch { prefs.setFadeOut(it) } },
                        onToggleSkipSilence = { scope.launch { prefs.setSkipSilence(it) } }
                    )
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

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.card)
            .clickable { onClick() }
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
        Text(
            text = title,
            color = colors.primaryText,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
