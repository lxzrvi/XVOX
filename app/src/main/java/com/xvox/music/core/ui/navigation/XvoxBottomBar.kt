// EXACT CODE REPLACE KARO (poori file) - koi extra kaam nahi. Path: app/src/main/java/com/xvox/music/core/ui/navigation/XvoxBottomBar.kt
// =============================================================================================
// XVOX LIQUID NAVBAR (KernelSU FloatingBottomBar style: live blur + lens refraction + liquid pill)
// YEH POORI FILE KA EXACT CODE REPLACE KARO. KOI EXTRA KAAM NAHI.
// Path: app/src/main/java/com/xvox/music/core/ui/navigation/XvoxBottomBar.kt
// Call-sites (XvoxMainShell) same rehte hain. Layout / size / offset / settings same.
// =============================================================================================
package com.xvox.music.core.ui.navigation

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.blur.DampedDragAnimation
import com.xvox.music.core.ui.blur.InnerShadow
import com.xvox.music.core.ui.blur.InteractiveHighlight
import com.xvox.music.core.ui.blur.LocalXvoxShellContentSky
import com.xvox.music.core.ui.blur.LocalXvoxSurfaceSky
import com.xvox.music.core.ui.blur.innerShadow
import com.xvox.music.core.ui.blur.lens
import com.xvox.music.core.ui.blur.xvoxGlassEdge
import com.xvox.music.core.ui.blur.xvoxThinEdge
import com.xvox.music.core.ui.blur.rememberCombinedBackdrop
import com.xvox.music.core.ui.blur.vibrancy
import com.xvox.music.core.ui.chrome.LocalXvoxChromeStyle
import com.xvox.music.core.ui.chrome.parseHexColor
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.highlight.BloomStroke
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.highlight.LightPosition
import top.yukonga.miuix.kmp.blur.highlight.LightSource
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.sensor.rememberDeviceTilt
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin

private val LocalLiquidTabScale = staticCompositionLocalOf { { 1f } }
private val LocalLiquidTint = compositionLocalOf { Color.White }

private val iosIndicatorSpecular: Highlight = Highlight(
    width = 1.dp,
    alpha = 1f,
    style = BloomStroke(
        color = Color.White.copy(alpha = 0.12f),
        innerBlurRadius = 2.0.dp,
        primaryLight = LightSource(
            position = LightPosition(0.5f, -0.3f, -0.05f),
            color = Color.White,
            intensity = 1f,
        ),
        secondaryLight = LightSource(
            position = LightPosition(0.5f, 0.8f, -0.5f),
            color = Color.White,
            intensity = 0.4f,
        ),
        dualPeak = true,
    ),
)

// Mirrors miuix-blur HighlightStyle's LIGHT_REF - keep in sync.
private const val LIGHT_REF_X = 0.5f
private const val LIGHT_REF_Y = 0.7f
private const val GRAVITY_DIR_THRESHOLD_SQ = 0.01f
private const val GRAVITY_ANGLE_STEP_RAD = (3.0 * PI / 180.0).toFloat()

/** Tracks gravity for a `dualPeak` highlight's primary light. */
@Composable
private fun rememberQuantizedGravityAngle(): State<Float> {
    val tiltState = rememberDeviceTilt()
    return remember(tiltState) {
        derivedStateOf {
            val tilt = tiltState.value
            val magnitudeSquared = tilt.gravityX * tilt.gravityX + tilt.gravityY * tilt.gravityY
            if (magnitudeSquared > GRAVITY_DIR_THRESHOLD_SQ) {
                (atan2(tilt.gravityY, tilt.gravityX) / GRAVITY_ANGLE_STEP_RAD).roundToInt() * GRAVITY_ANGLE_STEP_RAD
            } else {
                (-PI / 2).toFloat()
            }
        }
    }
}

@Composable
private fun rememberGravityRotatedHighlight(
    base: Highlight,
    extraDegrees: Float = 0f,
): State<Highlight> {
    val baseStyle = base.style as BloomStroke
    val angle = rememberQuantizedGravityAngle()
    return remember(angle, base, extraDegrees) {
        derivedStateOf {
            val basePrimary = baseStyle.primaryLight
            val rad = angle.value + (extraDegrees * PI / 180.0).toFloat()
            base.copy(
                style = baseStyle.copy(
                    primaryLight = basePrimary.copy(
                        position = LightPosition(
                            x = LIGHT_REF_X + cos(rad),
                            y = LIGHT_REF_Y + sin(rad),
                            z = basePrimary.position.z,
                        ),
                    ),
                ),
            )
        }
    }
}

private fun navigationIconRes(destination: XvoxDestination): Int = when (destination) {
    XvoxDestination.HOME -> R.drawable.ic_xvox_home
    XvoxDestination.SEARCH -> R.drawable.ic_xvox_search
    XvoxDestination.SETTINGS -> R.drawable.ic_xvox_settings
}

/** Blur-off only: per-tab 0..1 proximity of the sliding pill (null in blur mode). */
private val LocalLiquidAccent = androidx.compose.runtime.compositionLocalOf<Color?> { null }
private val LocalLiquidTabEmphasis = androidx.compose.runtime.compositionLocalOf<((Int) -> Float)?> { null }

@Composable
private fun RowScope.LiquidTab(
    destination: XvoxDestination,
    index: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scale = LocalLiquidTabScale.current
    val emphasis = LocalLiquidTabEmphasis.current
    val accent = LocalLiquidAccent.current ?: XvoxTheme.colors.primaryAccent
    Box(
        modifier = Modifier
            .semantics(mergeDescendants = true) {
                this.selected = selected
                role = Role.Tab
                onClick {
                    onClick()
                    true
                }
            }
            .fillMaxHeight()
            .weight(1f)
            .graphicsLayer {
                // Blur off: the icon the pill is travelling toward grows a little.
                val e = emphasis?.invoke(index) ?: 0f
                val s = scale() * (1f + .14f * e)
                scaleX = s
                scaleY = s
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(navigationIconRes(destination)),
            contentDescription = destination.label,
            colorFilter = ColorFilter.tint(LocalLiquidTint.current),
            modifier = Modifier.size(XvoxNavigationGeometry.iconSize)
        )
        if (emphasis != null) {
            // Accent copy cross-fades in as the pill approaches: one icon, no clipped duplicate row.
            Image(
                painter = painterResource(navigationIconRes(destination)),
                contentDescription = null,
                colorFilter = ColorFilter.tint(accent),
                modifier = Modifier
                    .size(XvoxNavigationGeometry.iconSize)
                    .graphicsLayer { alpha = emphasis.invoke(index).coerceIn(0f, 1f) }
            )
        }
    }
}

@Composable
fun XvoxBottomBar(
    selected: XvoxDestination,
    onSelected: (XvoxDestination) -> Unit,
    /** Retained for call-site compatibility; the Bottom Bar owns press-and-drag selection. */
    @Suppress("UNUSED_PARAMETER") onLongPressSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current
    val chrome = LocalXvoxChromeStyle.current
    // Sits above the moving page: blur the live page (+ wallpaper underneath it).
    val contentSky = LocalXvoxShellContentSky.current ?: LocalXvoxSurfaceSky.current
    val navBarHeight = chrome.navigationBarHeight.coerceIn(52f, 88f).dp
    val availableWidth = (LocalConfiguration.current.screenWidthDp.dp - 24.dp).coerceAtLeast(190.dp)
    val navBarWidth = chrome.navigationBarWidth.coerceIn(190f, 380f).dp.coerceAtMost(availableWidth)
    val pillHeight = (navBarHeight - 8.dp).coerceAtLeast(44.dp)
    val destinations = XvoxDestination.entries
    val selectedIndex = destinations.indexOf(selected)

    val navSurfaceAlpha = chrome.navBgAlpha.coerceIn(0f, 1f)
    val isDark = colors.background.luminance() < .5f
    val intensity = chrome.glassBlurIntensity.coerceIn(0f, 1f)
    val miniBackdrop = com.xvox.music.core.ui.blur.LocalXvoxMiniBackdrop.current
    val navBackdrop = remember(contentSky?.backdrop, miniBackdrop) {
        val page = contentSky?.backdrop
        when {
            page == null -> null
            miniBackdrop == null -> page
            else -> com.xvox.music.core.ui.blur.CombinedBackdrop(page, miniBackdrop)
        }
    }
    val blurOn = chrome.glassBlurEnabled && intensity > .001f &&
        !com.xvox.music.core.ui.blur.XvoxGpuGuard.safeMode &&
        contentSky != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    // Real frost: 8dp (light) .. 26dp (max) controlled by the global blur intensity.
    val blurDp = 8f + 10f * intensity
    // With blur on, the material is a light tint so the blurred page shows THROUGH the glass.
    val containerColor = colors.background.copy(
        alpha = if (blurOn) navSurfaceAlpha * .5f else navSurfaceAlpha
    )
    val accentColor = parseHexColor(chrome.pillIconColor) ?: colors.primaryAccent
    val inactiveColor = colors.mutedText.copy(alpha = .86f)

    val isLandscape = LocalConfiguration.current.orientation ==
        android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val defaultPlacementY = if (isLandscape) 31.5.dp else 10.5.dp
    val actualHostHeight = if (isLandscape) navBarHeight else navBarHeight + 20.dp
    val topOffset = if (isLandscape) 0.dp else XvoxNavigationGeometry.hostOverflow

    Box(
        modifier = modifier
            .offset(
                x = chrome.navigationBarOffsetX.coerceIn(-220f, 220f).dp,
                y = defaultPlacementY + chrome.navigationBarOffsetY.coerceIn(-260f, 260f).dp
            )
            .width(navBarWidth)
            .height(actualHostHeight)
    ) {
        LiquidBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = topOffset)
                .size(navBarWidth, navBarHeight),
            selectedIndex = selectedIndex,
            tabsCount = destinations.size,
            backdrop = navBackdrop,
            blurOn = blurOn,
            blurDp = blurDp,
            pillHeight = pillHeight,
            isDark = isDark,
            containerColor = containerColor,
            accentColor = accentColor,
            inactiveColor = inactiveColor,
            onTap = { haptics.tap() },
            onTick = { haptics.sliderTick() },
            onSelect = { index -> onSelected(destinations[index]) },
            destinations = destinations
        )
    }
}

@Composable
private fun LiquidBar(
    modifier: Modifier,
    selectedIndex: Int,
    tabsCount: Int,
    backdrop: Backdrop?,
    blurOn: Boolean,
    blurDp: Float,
    pillHeight: androidx.compose.ui.unit.Dp,
    isDark: Boolean,
    containerColor: Color,
    accentColor: Color,
    inactiveColor: Color,
    onTap: () -> Unit,
    onTick: () -> Unit,
    onSelect: (Int) -> Unit,
    destinations: List<XvoxDestination>
) {
    val pillShape = remember { CircleShape }
    val tabsBackdrop = rememberLayerBackdrop()
    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val animationScope = rememberCoroutineScope()
    val useBlur = blurOn && backdrop != null

    var tabWidthPx by remember { mutableFloatStateOf(0f) }
    var totalWidthPx by remember { mutableFloatStateOf(0f) }

    val offsetAnimation = remember { Animatable(0f) }
    val rubberBandPx = with(density) { 4.dp.toPx() }
    val panelOffset by remember(rubberBandPx) {
        derivedStateOf {
            if (totalWidthPx == 0f) {
                0f
            } else {
                val fraction = (offsetAnimation.value / totalWidthPx).fastCoerceIn(-1f, 1f)
                rubberBandPx * fraction.sign * EaseOut.transform(abs(fraction))
            }
        }
    }

    var currentIndex by remember { mutableIntStateOf(selectedIndex) }
    val onSelectedUpdated by rememberUpdatedState(onSelect)
    val onTapUpdated by rememberUpdatedState(onTap)
    val onTickUpdated by rememberUpdatedState(onTick)
    // [0] = a true swipe happened in this gesture, [1] = last index that ticked.
    val gesture = remember { floatArrayOf(0f, -1f) }

    fun indexAt(positionX: Float): Int {
        if (tabWidthPx == 0f) return currentIndex
        val horizontalPaddingPx = with(density) { 4.dp.toPx() }
        val logicalX = if (isLtr) positionX else totalWidthPx - positionX
        return ((logicalX - horizontalPaddingPx) / tabWidthPx)
            .toInt()
            .coerceIn(0, tabsCount - 1)
    }

    val dampedDragAnimation = remember(animationScope, tabsCount, density, isLtr) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = selectedIndex.toFloat(),
            valueRange = 0f..(tabsCount - 1).toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 78f / 56f,
            canDrag = { offset -> offset.x in 0f..totalWidthPx },
            onDragStarted = { position ->
                gesture[0] = 0f
                gesture[1] = indexAt(position.x).toFloat()
                updateValue(indexAt(position.x).toFloat())
            },
            onDragStopped = {
                val targetIndex = targetValue.roundToInt().coerceIn(0, tabsCount - 1)
                val swiped = gesture[0] > 0f
                // A completed tap always reaches the callback (re-selecting returns a tab to its
                // root); a swipe that ends on its starting tab stays a no-op.
                if (!swiped || currentIndex != targetIndex) {
                    if (!swiped) onTapUpdated()
                    currentIndex = targetIndex
                    onSelectedUpdated(targetIndex)
                }
                updateValue(targetIndex.toFloat())
                animationScope.launch {
                    offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                }
            },
            onDragCancelled = {
                updateValue(currentIndex.toFloat())
                animationScope.launch {
                    offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                }
            },
            onDrag = { _, dragAmount ->
                if (tabWidthPx > 0f && dragAmount.x != 0f) {
                    gesture[0] = 1f
                    val next = (targetValue + dragAmount.x / tabWidthPx * if (isLtr) 1f else -1f)
                        .coerceIn(0f, (tabsCount - 1).toFloat())
                    val nextIndex = next.roundToInt()
                    if (nextIndex.toFloat() != gesture[1]) {
                        gesture[1] = nextIndex.toFloat()
                        onTickUpdated()
                    }
                    updateValue(next)
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                }
            }
        )
    }

    LaunchedEffect(selectedIndex) {
        if (currentIndex != selectedIndex) {
            currentIndex = selectedIndex
            dampedDragAnimation.animateToValue(selectedIndex.toFloat())
        }
    }

    fun activateTab(index: Int) {
        if (index !in 0 until tabsCount) return
        currentIndex = index
        onSelectedUpdated(index)
        dampedDragAnimation.animateToValue(index.toFloat())
    }

    val interactiveHighlight = remember(animationScope, tabWidthPx, dampedDragAnimation) {
        InteractiveHighlight(
            animationScope = animationScope,
            position = { size, _ ->
                Offset(
                    if (isLtr) (dampedDragAnimation.value + 0.5f) * tabWidthPx + panelOffset
                    else size.width - (dampedDragAnimation.value + 0.5f) * tabWidthPx + panelOffset,
                    size.height / 2f
                )
            }
        )
    }

    val baseHighlight = rememberGravityRotatedHighlight(iosIndicatorSpecular, extraDegrees = -45f)
    val pillHighlight = rememberGravityRotatedHighlight(iosIndicatorSpecular, extraDegrees = 90f)
    val combinedBackdrop = rememberCombinedBackdrop(backdrop ?: tabsBackdrop, tabsBackdrop)

    val emphasisFn: ((Int) -> Float)? = if (useBlur) null else { i -> if (i == selectedIndex) (1f - kotlin.math.abs(dampedDragAnimation.value - i)).coerceIn(0f, 1f) else 0f }
    val tabs: @Composable RowScope.() -> Unit = {
        destinations.forEachIndexed { index, destination ->
            LiquidTab(
                destination = destination,
                index = index,
                selected = index == selectedIndex,
                onClick = { activateTab(index) }
            )
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.CenterStart) {
        Row(
            Modifier
                .onGloballyPositioned { coords ->
                    totalWidthPx = coords.size.width.toFloat()
                    val contentWidthPx = totalWidthPx - with(density) { 8.dp.toPx() }
                    tabWidthPx = (contentWidthPx / tabsCount).coerceAtLeast(0f)
                }
                .selectableGroup()
                .graphicsLayer { translationX = panelOffset }
                .dropShadow(
                    shape = pillShape,
                    shadow = Shadow(
                        radius = 10.dp,
                        color = Color.Black,
                        alpha = if (isDark) 0.2f else 0.1f,
                    ),
                )
                .then(
                    if (useBlur) {
                        Modifier.drawBackdrop(
                            backdrop = backdrop!!,
                            shape = { pillShape },
                            effects = {
                                padding = maxOf(padding, 24.dp.toPx())
                                // Container = blur only (the per-frame lens pass lives on the pill).
                                blur(blurDp.dp.toPx(), blurDp.dp.toPx())
                            },
                            highlight = { baseHighlight.value.copy(alpha = 0.75f) },
                            layerBlock = {
                                val width = size.width.coerceAtLeast(1f)
                                val s = lerp(1f, 1f + 16.dp.toPx() / width, dampedDragAnimation.pressProgress)
                                scaleX = s
                                scaleY = s
                            },
                            onDrawSurface = { drawRect(containerColor) },
                        )
                    } else {
                        Modifier.background(containerColor, pillShape)
                            .xvoxThinEdge(pillShape)
                    }
                )
                .then(
                    if (useBlur) {
                        interactiveHighlight.modifier.then(interactiveHighlight.gestureModifier)
                    } else Modifier
                )
                .then(dampedDragAnimation.modifier)
                .fillMaxSize()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalLiquidTint provides inactiveColor, LocalLiquidTabEmphasis provides emphasisFn, LocalLiquidAccent provides accentColor) {
                tabs()
            }
        }

        if (useBlur) {
            // Accent-coloured copy of the tabs: the travelling pill magnifies THIS layer, so the
            // active icon looks lit and refracted inside the liquid pill.
            CompositionLocalProvider(
                LocalLiquidTabScale provides {
                    lerp(1f, 1.2f, dampedDragAnimation.pressProgress)
                },
                LocalLiquidTint provides accentColor,
            ) {
                Row(
                    Modifier
                        .clearAndSetSemantics {}
                        .alpha(0f)
                        .layerBackdrop(tabsBackdrop)
                        .graphicsLayer { translationX = panelOffset }
                        .drawBackdrop(
                            backdrop = backdrop!!,
                            shape = { pillShape },
                            effects = {
                                blur(blurDp.dp.toPx(), blurDp.dp.toPx())
                                // The refraction pass is only visible while the pill is pressed/dragged;
                                // skipping it at rest removes a per-frame shader from every tab change.
                                if (dampedDragAnimation.pressProgress > .02f) {
                                    lens(
                                        refractionHeight = 24.dp.toPx(),
                                        refractionAmount = 24.dp.toPx(),
                                    )
                                }
                            },
                            onDrawSurface = { drawRect(containerColor) },
                        )
                        .then(interactiveHighlight.modifier)
                        .width(with(density) { totalWidthPx.toDp() })
                        .height(pillHeight)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = { tabs() }
                )
            }
        }

        if (tabWidthPx > 0f) {
            val tabWidthDp = with(density) { tabWidthPx.toDp() }
            if (useBlur) {
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .graphicsLayer {
                            val progressOffset = dampedDragAnimation.value * tabWidthPx
                            translationX = if (isLtr) progressOffset + panelOffset else -progressOffset + panelOffset
                        }
                        .drawBackdrop(
                            backdrop = combinedBackdrop,
                            shape = { pillShape },
                            effects = {
                                val progress = dampedDragAnimation.pressProgress
                                lens(
                                    refractionHeight = 10.dp.toPx() * progress,
                                    refractionAmount = 14.dp.toPx() * progress,
                                    depthEffect = true,
                                    chromaticAberration = 0f,
                                )
                            },
                            highlight = { pillHighlight.value.copy(alpha = dampedDragAnimation.pressProgress) },
                            layerBlock = {
                                scaleX = dampedDragAnimation.scaleX
                                scaleY = dampedDragAnimation.scaleY
                                val velocity = dampedDragAnimation.velocity / 10f
                                scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                                scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                            },
                            onDrawSurface = {
                                val progress = dampedDragAnimation.pressProgress
                                drawRect(
                                    color = if (!isDark) Color.Black.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.1f),
                                    alpha = 1f - progress,
                                )
                                drawRect(Color.Black.copy(alpha = 0.03f * progress))
                            },
                        )
                        .innerShadow(shape = pillShape) {
                            InnerShadow(
                                radius = 8.dp * dampedDragAnimation.pressProgress,
                                color = Color.Black.copy(alpha = 0.15f),
                                alpha = dampedDragAnimation.pressProgress,
                            )
                        }
                        .height(pillHeight)
                        .width(tabWidthDp)
                )
            } else {
                // Blur off / unsupported: same pill motion, flat accent tint (KernelSU fallback).
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .graphicsLayer {
                            val progressOffset = dampedDragAnimation.value * tabWidthPx
                            translationX = if (isLtr) progressOffset + panelOffset else -progressOffset + panelOffset
                        }
                        .clip(pillShape)
                        .background(accentColor.copy(alpha = 0.15f), pillShape)
                        .height(pillHeight)
                        .width(tabWidthDp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    // (No clipped duplicate icon row any more: icons themselves cross-fade to accent.)
                }
            }
        }
    }
}
