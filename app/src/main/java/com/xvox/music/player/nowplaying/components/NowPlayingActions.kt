@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.xvox.music.player.nowplaying.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import kotlinx.coroutines.delay

/**
 * The Now Playing action bar.
 * Left side: Timer / Queue / Info in a pill.
 * Right side: 2-by-2 action buttons across 3 pages with 3 indicator dots.
 *   Page 0: Star / Like
 *   Page 1: EQ / 3D Sound
 *   Page 2: Bluetooth / Crossfade
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NowPlayingActions(
    isLiked: Boolean = false,
    isInPlaylist: Boolean = false,
    onTimer: (() -> Unit)? = null,
    onQueue: (() -> Unit)? = null,
    onInfo: (() -> Unit)? = null,
    onToggleLiked: (() -> Unit)? = null,
    onStarPlaylist: (() -> Unit)? = null,
    timerProgress: Float? = null,
    crossfadeOn: Boolean = false,
    onToggleCrossfade: (() -> Unit)? = null,
    equalizerOn: Boolean = false,
    spaceOn: Boolean = false,
    lyricsOn: Boolean = false,
    onToggleEqualizer: (() -> Unit)? = null,
    onToggleSpace: (() -> Unit)? = null,
    onToggleLyrics: (() -> Unit)? = null,
    onOpenOptions: ((String) -> Unit)? = null
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current
    val overlays = LocalXvoxOverlayController.current
    var pageIndex by remember { mutableIntStateOf(0) }
    var showIndicator by remember { mutableStateOf(true) }

    LaunchedEffect(pageIndex) {
        showIndicator = true
        kotlinx.coroutines.delay(2200)
        showIndicator = false
    }

    val dotsAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (showIndicator) 1f else 0f,
        animationSpec = tween(400),
        label = "dotsAlpha"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left cluster: Timer / Queue / Info in continuous pill
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(colors.card.copy(alpha = 0.22f))
                .padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            NowPlayingPillActionIcon(
                resource = R.drawable.ic_xvox_timer,
                onClick = onTimer,
                progress = timerProgress
            )
            NowPlayingPillActionIcon(
                resource = R.drawable.ic_xvox_queue,
                onClick = onQueue
            )
            NowPlayingPillActionIcon(
                resource = R.drawable.ic_xvox_info,
                onClick = onInfo
            )
        }

        Spacer(Modifier.weight(1f))

        // Right cluster: 2 action buttons with 3 indicator dots
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .padding(horizontal = 2.dp, vertical = 2.dp)
                    .pointerInput(Unit) {
                        var drag = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { drag = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                drag += dragAmount
                            },
                            onDragEnd = {
                                if (drag <= -24f) {
                                    pageIndex = (pageIndex + 1) % 3
                                    haptics.tap()
                                } else if (drag >= 24f) {
                                    pageIndex = (pageIndex - 1 + 3) % 3
                                    haptics.tap()
                                }
                            },
                            onDragCancel = { }
                        )
                    }
            ) {
                AnimatedContent(
                    targetState = pageIndex,
                    transitionSpec = {
                        val forward = targetState > initialState || (initialState == 2 && targetState == 0)
                        (slideInHorizontally(tween(220)) { if (forward) it else -it } + fadeIn(tween(140)))
                            .togetherWith(slideOutHorizontally(tween(220)) { if (forward) -it else it } + fadeOut(tween(140)))
                    },
                    label = "nowPlaying2by2Cluster"
                ) { targetPage ->
                    when (targetPage) {
                        0 -> {
                            // Page 1: Star + Like
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                NowPlayingCircleAction(
                                    resource = R.drawable.ic_xvox_star,
                                    tint = if (isInPlaylist) colors.primaryAccent else colors.primaryText,
                                    active = isInPlaylist,
                                    contentDescription = "Add to playlist",
                                    onClick = {
                                        haptics.tap()
                                        onStarPlaylist?.invoke()
                                    }
                                )
                                NowPlayingCircleAction(
                                    resource = if (isLiked) R.drawable.ic_xvox_heart else R.drawable.ic_xvox_heart_outline,
                                    tint = if (isLiked) colors.primaryAccent else colors.primaryText,
                                    active = isLiked,
                                    contentDescription = if (isLiked) "Unlike" else "Like",
                                    onClick = {
                                        haptics.tap()
                                        onToggleLiked?.invoke()
                                    }
                                )
                            }
                        }
                        1 -> {
                            // Page 2: EQ + 3D Sound
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                NowPlayingCircleAction(
                                    resource = R.drawable.ic_xvox_equalizer,
                                    tint = if (equalizerOn) colors.primaryAccent else colors.primaryText,
                                    active = equalizerOn,
                                    contentDescription = "Equalizer",
                                    onClick = {
                                        haptics.tap()
                                        onToggleEqualizer?.invoke()
                                        overlays.showP("Equalizer: ${if (!equalizerOn) "ON" else "OFF"}")
                                    },
                                    onLongClick = if (onOpenOptions != null) ({
                                        haptics.heavy()
                                        onOpenOptions("Equalizer")
                                    }) else null
                                )
                                NowPlayingCircleAction(
                                    resource = R.drawable.ic_xvox_waveform,
                                    tint = if (spaceOn) colors.primaryAccent else colors.primaryText,
                                    active = spaceOn,
                                    contentDescription = "3D sound",
                                    onClick = {
                                        haptics.tap()
                                        onToggleSpace?.invoke()
                                        overlays.showP("3D Sound: ${if (!spaceOn) "ON" else "OFF"}")
                                    },
                                    onLongClick = if (onOpenOptions != null) ({
                                        haptics.heavy()
                                        onOpenOptions("3D sound")
                                    }) else null
                                )
                            }
                        }
                        else -> {
                            // Page 3: Bluetooth + Crossfade
                            val bluetoothReady = rememberBluetoothReady()
                            val enableBluetooth = rememberBluetoothEnableRequest()
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                NowPlayingCircleAction(
                                    resource = R.drawable.ic_xvox_bluetooth,
                                    tint = if (bluetoothReady) colors.primaryAccent else colors.primaryText,
                                    active = bluetoothReady,
                                    contentDescription = "Bluetooth / audio output",
                                    onClick = if (onOpenOptions != null) ({
                                        haptics.tap()
                                        enableBluetooth?.invoke()
                                        onOpenOptions("Bluetooth")
                                    }) else null,
                                    onLongClick = if (onOpenOptions != null) ({
                                        haptics.heavy()
                                        onOpenOptions("Bluetooth")
                                    }) else null
                                )
                                NowPlayingCircleAction(
                                    resource = R.drawable.ic_xvox_crossfade,
                                    tint = if (crossfadeOn) colors.primaryAccent else colors.primaryText,
                                    active = crossfadeOn,
                                    contentDescription = "Crossfade",
                                    onClick = {
                                        haptics.tap()
                                        onToggleCrossfade?.invoke()
                                        overlays.showP("Crossfade: ${if (!crossfadeOn) "ON" else "OFF"}")
                                    },
                                    onLongClick = if (onOpenOptions != null) ({
                                        haptics.heavy()
                                        onOpenOptions("Crossfade")
                                    }) else null
                                )
                            }
                        }
                    }
                }
            }

            // 3 indicator dots
            Row(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .graphicsLayer { alpha = dotsAlpha },
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { dotIdx ->
                    val isSelected = dotIdx == pageIndex
                    val dotWidth by animateDpAsState(
                        targetValue = if (isSelected) 12.dp else 4.dp,
                        animationSpec = tween(200),
                        label = "dotWidth"
                    )
                    Box(
                        modifier = Modifier
                            .height(4.dp)
                            .width(dotWidth)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (isSelected) colors.primaryAccent
                                else colors.secondaryText.copy(alpha = 0.35f)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                haptics.tap()
                                pageIndex = dotIdx
                            }
                    )
                }
            }
        }
    }
}

/** Icon inside the shared left pill: no circular background inside. */
@Composable
fun NowPlayingPillActionIcon(
    resource: Int,
    onClick: (() -> Unit)? = null,
    progress: Float? = null
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current

    Box(
        modifier = Modifier
            .size(34.dp)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = onClick != null,
                onClick = {
                    haptics.tap()
                    onClick?.invoke()
                },
                onLongClick = null
            ),
        contentAlignment = Alignment.Center
    ) {
        if (progress != null) {
            Canvas(modifier = Modifier.size(28.dp)) {
                val stroke = 2.dp.toPx()
                drawArc(
                    color = colors.mutedText.copy(alpha = 0.22f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = stroke)
                )
                drawArc(
                    color = colors.primaryAccent,
                    startAngle = -90f,
                    sweepAngle = progress * 360f,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }

        Icon(
            painter = painterResource(resource),
            contentDescription = null,
            tint = colors.primaryText,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun NowPlayingCircleAction(
    resource: Int,
    tint: Color? = null,
    active: Boolean = false,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current

    Box(
        modifier = Modifier
            .size(42.dp)
            .background(
                if (active) colors.primaryAccent.copy(alpha = 0.24f)
                else colors.card.copy(alpha = 0.22f),
                CircleShape
            )
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = onClick != null || onLongClick != null,
                onClick = {
                    haptics.tap()
                    onClick?.invoke()
                },
                onLongClick = if (onLongClick != null) ({
                    haptics.heavy()
                    onLongClick()
                }) else null
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(resource),
            contentDescription = contentDescription,
            tint = tint ?: colors.primaryAccent,
            modifier = Modifier.size(19.dp)
        )
    }
}

/** True once the adapter is on and at least one headset is connected. */
@Composable
private fun rememberBluetoothReady(): Boolean {
    val context = androidx.compose.ui.platform.LocalContext.current
    val manager = remember(context) { context.getSystemService(android.content.Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager }
    var ready by remember { mutableStateOf(currentBluetoothReady(manager)) }
    DisposableEffect(manager) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(c: android.content.Context?, intent: android.content.Intent?) {
                ready = currentBluetoothReady(manager)
            }
        }
        val filter = android.content.IntentFilter().apply {
            addAction(android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        runCatching { context.registerReceiver(receiver, filter) }
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    return ready
}

private fun currentBluetoothReady(manager: android.bluetooth.BluetoothManager?): Boolean {
    val adapter = runCatching { manager?.adapter }.getOrNull() ?: return false
    if (!adapter.isEnabled) return false
    return runCatching {
        adapter.bondedDevices?.any { device ->
            val major = device.bluetoothClass?.majorDeviceClass
            major == android.bluetooth.BluetoothClass.Device.Major.AUDIO_VIDEO ||
                major == android.bluetooth.BluetoothClass.Device.Major.PERIPHERAL
        } == true
    }.getOrDefault(false)
}

/** A one-shot "turn Bluetooth on" request, or null while it is already on / unavailable. */
@Composable
private fun rememberBluetoothEnableRequest(): (() -> Unit)? {
    val context = androidx.compose.ui.platform.LocalContext.current
    return remember(context) {
        val adapter = runCatching {
            (context.getSystemService(android.content.Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager)?.adapter
        }.getOrNull()
        if (adapter == null || adapter.isEnabled) null
        else ({
            runCatching {
                context.startActivity(
                    android.content.Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        })
    }
}
