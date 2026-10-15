@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.xvox.music.player.nowplaying.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import kotlinx.coroutines.delay

private val XvoxActionEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/** A physical completion for the action-pair swipe after direct finger tracking ends. */
private data class XvoxActionSwipeSettle(
    val id: Int,
    val startOffsetPx: Float,
    val targetOffsetPx: Float,
    val targetPage: Int?
)

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
    onOpenOptions: ((String) -> Unit)? = null,
    /** Hoisted by Now Playing so a reopened surface restores the user's action page. */
    actionPageIndex: Int = 0,
    onActionPageChange: (Int) -> Unit = {},
    /** User-selectable lanes; the transport play circle remains physically centered below. */
    utilityPillSide: String = "left",
    actionClusterSide: String = "right",
    /** Full-screen customizer multiplier for utility/action groups. */
    controlsAlpha: Float = 1f,
    /** Shared lyrics-box material alpha for utility and circular action surfaces. */
    surfaceAlpha: Float = .27f,
    utilityOffsetX: Float = 0f,
    utilityOffsetY: Float = 0f,
    actionsOffsetX: Float = 0f,
    actionsOffsetY: Float = 0f,
    editSession: NowPlayingEditSession? = null
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current
    // Do not clamp at the first/last action page: the pager is a three-stop ring.
    var pageIndex by remember { mutableIntStateOf(((actionPageIndex % 3) + 3) % 3) }
    var showIndicator by remember { mutableStateOf(true) }
    var actionWidthPx by remember { mutableFloatStateOf(0f) }
    var liveSwipeOffsetPx by remember { mutableFloatStateOf(0f) }
    var settledSwipeOffsetPx by remember { mutableFloatStateOf(0f) }
    var actionDragging by remember { mutableStateOf(false) }
    var settleSerial by remember { mutableIntStateOf(0) }
    var actionSettle by remember { mutableStateOf<XvoxActionSwipeSettle?>(null) }

    fun normalizedPage(index: Int): Int = ((index % 3) + 3) % 3
    fun targetForOffset(offset: Float): Int = when {
        offset < 0f -> normalizedPage(pageIndex + 1)
        offset > 0f -> normalizedPage(pageIndex - 1)
        else -> pageIndex
    }
    fun startSettle(start: Float, target: Float, targetPage: Int?) {
        settleSerial += 1
        settledSwipeOffsetPx = start
        actionSettle = XvoxActionSwipeSettle(
            id = settleSerial,
            startOffsetPx = start,
            targetOffsetPx = target,
            targetPage = targetPage
        )
    }
    fun requestActionPage(next: Int) {
        val normalized = normalizedPage(next)
        if (normalized == pageIndex) return
        if (actionWidthPx <= 1f) {
            pageIndex = normalized
            onActionPageChange(normalized)
            return
        }
        // Pick the short route on the three-page ring. Crucially, 0 → 2 is a one-step backward
        // wrap and 2 → 0 is a one-step forward wrap—neither endpoint can ever feel like a stop.
        val forwardDistance = (normalized - pageIndex + 3) % 3
        val backwardDistance = (pageIndex - normalized + 3) % 3
        val forward = forwardDistance <= backwardDistance
        startSettle(
            start = 0f,
            target = if (forward) -actionWidthPx else actionWidthPx,
            targetPage = normalized
        )
    }

    LaunchedEffect(actionPageIndex) {
        val next = ((actionPageIndex % 3) + 3) % 3
        if (next != pageIndex) {
            actionSettle = null
            actionDragging = false
            liveSwipeOffsetPx = 0f
            settledSwipeOffsetPx = 0f
            pageIndex = next
        }
    }

    LaunchedEffect(actionSettle?.id) {
        val settle = actionSettle ?: return@LaunchedEffect
        val animation = Animatable(settle.startOffsetPx)
        animation.animateTo(
            targetValue = settle.targetOffsetPx,
            animationSpec = tween(230, easing = XvoxActionEasing)
        ) {
            settledSwipeOffsetPx = value
        }
        if (actionSettle?.id == settle.id) {
            settle.targetPage?.let { nextPage ->
                pageIndex = nextPage
                onActionPageChange(nextPage)
            }
            settledSwipeOffsetPx = 0f
            actionSettle = null
        }
    }

    LaunchedEffect(pageIndex) {
        showIndicator = true
        delay(2200)
        showIndicator = false
    }

    val dotsAlpha by animateFloatAsState(
        targetValue = if (showIndicator) 1f else 0f,
        animationSpec = tween(350),
        label = "dotsAlpha"
    )

    val utilityPill: @Composable () -> Unit = {
        // Left cluster: Timer / Queue / Info in continuous pill
        NowPlayingEditableTarget(
            session = editSession,
            itemId = NowPlayingEditItem.Utility,
            modifier = Modifier.offset(x = utilityOffsetX.dp, y = ((-6f) + utilityOffsetY).dp)
        ) {
            Row(
                modifier = Modifier
                    .graphicsLayer(alpha = controlsAlpha.coerceIn(0f, 1f))
                    .height(42.dp)
                    .clip(RoundedCornerShape(21.dp))
                    .background(colors.card.copy(alpha = surfaceAlpha.coerceIn(0f, 1f)))
                    .padding(horizontal = 6.dp),
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
        }
    }

    @Composable
    fun ActionPage(page: Int) {
        NowPlayingActionPage(
            pageIndex = page,
            isLiked = isLiked,
            isInPlaylist = isInPlaylist,
            equalizerOn = equalizerOn,
            spaceOn = spaceOn,
            crossfadeOn = crossfadeOn,
            surfaceAlpha = surfaceAlpha,
            onToggleLiked = onToggleLiked,
            onStarPlaylist = onStarPlaylist,
            onToggleEqualizer = onToggleEqualizer,
            onToggleSpace = onToggleSpace,
            onToggleCrossfade = onToggleCrossfade,
            onOpenOptions = onOpenOptions
        )
    }

    val changingActionCluster: @Composable () -> Unit = {
        // Right cluster: 2 action buttons with continuous bidirectional infinite swiping
        NowPlayingEditableTarget(
            session = editSession,
            itemId = NowPlayingEditItem.Actions,
            modifier = Modifier.offset(x = actionsOffsetX.dp, y = actionsOffsetY.dp)
        ) {
            Column(
                modifier = Modifier.graphicsLayer(alpha = controlsAlpha.coerceIn(0f, 1f)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            val visualSwipeOffsetPx = if (actionDragging) liveSwipeOffsetPx else settledSwipeOffsetPx
            val swipeWidth = actionWidthPx.coerceAtLeast(1f)
            val swipeFraction = (kotlin.math.abs(visualSwipeOffsetPx) / swipeWidth).coerceIn(0f, 1f)
            val swipeTargetPage = targetForOffset(visualSwipeOffsetPx)
            // Action pages travel strictly within their existing row. The former downward arc
            // looked diagonal against the six-option control box and made page changes feel bent.
            val showingSwipePair = kotlin.math.abs(visualSwipeOffsetPx) > .25f

            Box(
                modifier = Modifier
                    // Nothing clips the action pair: both controls can travel through the dot
                    // lane and across the physical swipe direction with the user's finger.
                    .height(42.dp)
                    .padding(horizontal = 2.dp)
                    .onSizeChanged { actionWidthPx = it.width.toFloat().coerceAtLeast(1f) }
                    .pointerInput(actionWidthPx) {
                        // pageIndex is deliberately not a key: crossing a full page width mutates
                        // it while this finger is still down, and keying the recognizer to that
                        // state would cancel the gesture exactly at the cyclic handoff.
                        var drag = 0f
                        detectHorizontalDragGestures(
                            onDragStart = {
                                // A new finger always takes ownership immediately, even if the
                                // previous release was still settling toward a neighbour.
                                actionSettle = null
                                settledSwipeOffsetPx = 0f
                                liveSwipeOffsetPx = 0f
                                drag = 0f
                                actionDragging = true
                                showIndicator = true
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                val width = actionWidthPx.coerceAtLeast(1f)
                                drag += dragAmount

                                // Do not stop at a completed edge while the finger remains down.
                                // Each full width becomes the new logical page and leaves only
                                // the residual displacement on screen, so one long swipe can
                                // visibly travel around the three-page ring indefinitely.
                                while (drag <= -width) {
                                    val next = normalizedPage(pageIndex + 1)
                                    pageIndex = next
                                    onActionPageChange(next)
                                    drag += width
                                }
                                while (drag >= width) {
                                    val previous = normalizedPage(pageIndex - 1)
                                    pageIndex = previous
                                    onActionPageChange(previous)
                                    drag -= width
                                }
                                liveSwipeOffsetPx = drag.coerceIn(-width, width)
                            },
                            onDragEnd = {
                                val width = actionWidthPx.coerceAtLeast(1f)
                                val start = liveSwipeOffsetPx
                                // Preserve the established 20px release threshold; only the
                                // visual route is now direct-manipulation rather than post-release.
                                val commit = kotlin.math.abs(start) >= 20f
                                actionDragging = false
                                liveSwipeOffsetPx = 0f
                                if (commit) {
                                    haptics.tap()
                                    startSettle(
                                        start = start,
                                        target = if (start < 0f) -width else width,
                                        targetPage = targetForOffset(start)
                                    )
                                } else {
                                    startSettle(start = start, target = 0f, targetPage = null)
                                }
                            },
                            onDragCancel = {
                                val start = liveSwipeOffsetPx
                                actionDragging = false
                                liveSwipeOffsetPx = 0f
                                startSettle(start = start, target = 0f, targetPage = null)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Outgoing controls follow the finger horizontally, shrink in place, and fade
                // completely at the endpoint. They never dip diagonally into the indicator lane.
                Box(
                    modifier = Modifier.graphicsLayer {
                        translationX = visualSwipeOffsetPx
                        val scale = (1f - .88f * swipeFraction).coerceAtLeast(.12f)
                        scaleX = scale
                        scaleY = scale
                        alpha = (1f - swipeFraction).coerceIn(0f, 1f)
                        transformOrigin = TransformOrigin(.5f, .5f)
                    }
                ) {
                    key("nowPlayingActionCurrent_$pageIndex") { ActionPage(pageIndex) }
                }

                if (showingSwipePair) {
                    // A left drag starts the next pair at the right edge and moves it left with
                    // the finger; a right drag mirrors it. Incoming controls grow and morph in
                    // the same fixed row, with no vertical/diagonal bend or clipping.
                    val incomingStart = if (visualSwipeOffsetPx < 0f) swipeWidth else -swipeWidth
                    Box(
                        modifier = Modifier.graphicsLayer {
                            translationX = incomingStart + visualSwipeOffsetPx
                            val scale = (.12f + .88f * swipeFraction).coerceIn(.12f, 1f)
                            scaleX = scale
                            scaleY = scale
                            alpha = swipeFraction.coerceIn(0f, 1f)
                            transformOrigin = TransformOrigin(.5f, .5f)
                        }
                    ) {
                        key("nowPlayingActionIncoming_$swipeTargetPage") { ActionPage(swipeTargetPage) }
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
                // A committed release highlights its incoming destination while the last short
                // settle finishes, keeping dots and the travelling pair visually in sync.
                val indicatedPage = actionSettle?.targetPage ?: pageIndex
                repeat(3) { dotIdx ->
                    val isSelected = dotIdx == indicatedPage
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
                                requestActionPage(dotIdx)
                            }
                    )
                }
            }
        }
    }
    }

    val utilityOnLeft = utilityPillSide != "right"
    val actionsOnLeft = actionClusterSide == "left"
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        // Restore the intended left/right lanes: the utility pill begins at its left anchor and
        // the six-option action ring stays at its right anchor. A slightly tighter 4dp gutter
        // keeps paired groups visually connected without collapsing their individual targets.
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (utilityOnLeft) key("nowPlayingUtilityPill") { utilityPill() }
        if (actionsOnLeft) key("nowPlayingChangingActions") { changingActionCluster() }
        Spacer(Modifier.weight(1f))
        if (!utilityOnLeft) key("nowPlayingUtilityPill") { utilityPill() }
        if (!actionsOnLeft) key("nowPlayingChangingActions") { changingActionCluster() }
    }
}

/** One of the three two-control action pages; reused by both live swipe layers. */
@Composable
private fun NowPlayingActionPage(
    pageIndex: Int,
    isLiked: Boolean,
    isInPlaylist: Boolean,
    equalizerOn: Boolean,
    spaceOn: Boolean,
    crossfadeOn: Boolean,
    surfaceAlpha: Float,
    onToggleLiked: (() -> Unit)?,
    onStarPlaylist: (() -> Unit)?,
    onToggleEqualizer: (() -> Unit)?,
    onToggleSpace: (() -> Unit)?,
    onToggleCrossfade: (() -> Unit)?,
    onOpenOptions: ((String) -> Unit)?
) {
    val haptics = LocalXvoxHaptics.current
    val overlays = LocalXvoxOverlayController.current
    when (pageIndex) {
        0 -> {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NowPlayingCircleAction(
                    resource = R.drawable.ic_xvox_star,
                    surfaceAlpha = surfaceAlpha,
                    active = isInPlaylist,
                    contentDescription = "Add to playlist",
                    onClick = {
                        haptics.tap()
                        onStarPlaylist?.invoke()
                    }
                )
                NowPlayingCircleAction(
                    resource = if (isLiked) R.drawable.ic_xvox_heart else R.drawable.ic_xvox_heart_outline,
                    surfaceAlpha = surfaceAlpha,
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NowPlayingCircleAction(
                    resource = R.drawable.ic_xvox_equalizer,
                    surfaceAlpha = surfaceAlpha,
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
                    surfaceAlpha = surfaceAlpha,
                    active = spaceOn,
                    contentDescription = "3D Sound",
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
            val bluetoothRouted = rememberActualBluetoothRoute()
            val enableBluetooth = rememberBluetoothEnableRequest()
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NowPlayingCircleAction(
                    resource = R.drawable.ic_xvox_bluetooth,
                    surfaceAlpha = surfaceAlpha,
                    active = bluetoothRouted,
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
                    surfaceAlpha = surfaceAlpha,
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
    /** Shared now-playing material alpha, provided by the Lyrics-matched container. */
    surfaceAlpha: Float = .27f,
    tint: Color? = null,
    active: Boolean = false,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current
    // An active option keeps exactly the same surface as every other option. Only its icon
    // changes to the accent, so toggling one never flashes or recolours the six-pill area.
    val bgColor = colors.card.copy(alpha = surfaceAlpha.coerceIn(0f, 1f))
    val effectiveTint = if (active) colors.primaryAccent else {
        if (colors.isLight) Color.Black else colors.primaryText.copy(alpha = 0.85f)
    }

    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(bgColor, CircleShape)
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
            tint = tint ?: effectiveTint,
            modifier = Modifier.size(19.dp)
        )
    }
}

/** True only while Android reports the selected playback path as Bluetooth, not merely paired. */
@Composable
private fun rememberActualBluetoothRoute(): Boolean {
    val context = androidx.compose.ui.platform.LocalContext.current
    val audioManager = remember(context) {
        context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
    }
    var routed by remember(audioManager) { mutableStateOf(audioManager.xvoxHasActualBluetoothRoute()) }
    // Some OEMs do not emit a device-list callback when the user switches an already connected
    // Bluetooth device between speaker and media output, so refresh the route flag while this
    // compact action page is visible as well.
    LaunchedEffect(audioManager) {
        while (true) {
            routed = audioManager.xvoxHasActualBluetoothRoute()
            delay(750)
        }
    }
    DisposableEffect(audioManager) {
        val callback = object : android.media.AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out android.media.AudioDeviceInfo>) {
                routed = audioManager.xvoxHasActualBluetoothRoute()
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out android.media.AudioDeviceInfo>) {
                routed = audioManager.xvoxHasActualBluetoothRoute()
            }
        }
        audioManager.registerAudioDeviceCallback(callback, null)
        onDispose { audioManager.unregisterAudioDeviceCallback(callback) }
    }
    return routed
}

@Suppress("DEPRECATION")
private fun android.media.AudioManager.xvoxHasActualBluetoothRoute(): Boolean {
    // Device enumeration tells us what is attached, while the manager flags tell us that BT is
    // actually the selected media/communication route. Both must be true for accent treatment.
    if (!isBluetoothA2dpOn && !isBluetoothScoOn) return false
    return getDevices(android.media.AudioManager.GET_DEVICES_OUTPUTS).any { device ->
        when (device.type) {
            android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> true
            else -> android.os.Build.VERSION.SDK_INT >= 31 &&
                (device.type == android.media.AudioDeviceInfo.TYPE_BLE_HEADSET ||
                    device.type == android.media.AudioDeviceInfo.TYPE_BLE_SPEAKER)
        }
    }
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
