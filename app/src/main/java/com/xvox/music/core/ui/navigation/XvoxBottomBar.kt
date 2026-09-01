package com.xvox.music.core.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import kotlin.math.abs

private val NavFluidEasing =
    CubicBezierEasing(
        0.20f,
        0.90f,
        0.25f,
        1f
    )

private const val MorphDuration = 520

private val ItemHeight = 57.dp

/*
 * Every destination gets the SAME fixed slot.
 *
 * The active pill lives independently on top
 * of those slots, so activating one item does NOT
 * resize the Row and push the other icons around.
 */
private val SlotWidth = 70.dp
private val SlotGap = 8.dp

private val HomeActiveWidth = 124.dp
private val SearchActiveWidth = 134.dp
private val SettingsActiveWidth = 150.dp


@Composable
fun XvoxBottomBar(
    selected: XvoxDestination,
    onSelected: (XvoxDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    val destinations = XvoxDestination.entries

    var dragDistance by remember {
        mutableFloatStateOf(0f)
    }

    /*
     * The whole bar has a fixed geometry.
     *
     * Nothing inside this Row changes its layout width
     * when selection changes.
     */
    Row(
        modifier = modifier
            .offset(y = 2.dp)
            .clip(CircleShape)
            .background(colors.surface)
            .border(
                width = 0.5.dp,
                color = colors.cardBorder,
                shape = CircleShape
            )
            .padding(4.dp)
            .pointerInput(selected) {

                detectHorizontalDragGestures(

                    onDragStart = {
                        dragDistance = 0f
                    },

                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        dragDistance += amount
                    },

                    onDragEnd = {

                        if (abs(dragDistance) >= 35.dp.toPx()) {

                            val currentIndex =
                                destinations.indexOf(selected)

                            val targetIndex =
                                if (dragDistance > 0f) {
                                    currentIndex + 1
                                } else {
                                    currentIndex - 1
                                }

                            destinations
                                .getOrNull(targetIndex)
                                ?.let(onSelected)
                        }

                        dragDistance = 0f
                    },

                    onDragCancel = {
                        dragDistance = 0f
                    }
                )
            }
    ) {

        /*
         * Fixed slots.
         *
         * IMPORTANT:
         * The width of these slots NEVER changes.
         */
        destinations.forEachIndexed { index, destination ->

            if (index > 0) {
                Spacer(
                    modifier = Modifier.width(SlotGap)
                )
            }

            NavigationSlot(
                destination = destination,
                active = destination == selected,
                onClick = {
                    if (destination != selected) {
                        onSelected(destination)
                    }
                }
            )
        }
    }
}


@Composable
private fun NavigationSlot(
    destination: XvoxDestination,
    active: Boolean,
    onClick: () -> Unit
) {
    val colors = XvoxTheme.colors

    val interactionSource =
        remember {
            MutableInteractionSource()
        }

    /*
     * ONLY the visual state changes.
     *
     * The slot itself stays exactly 70.dp wide.
     */
    val progress by
        animateFloatAsState(
            targetValue = if (active) 1f else 0f,
            animationSpec =
                tween(
                    durationMillis = MorphDuration,
                    easing = NavFluidEasing
                ),
            label = "navProgress"
        )

    val activeWidth =
        when (destination) {

            XvoxDestination.HOME ->
                HomeActiveWidth

            XvoxDestination.SEARCH ->
                SearchActiveWidth

            XvoxDestination.SETTINGS ->
                SettingsActiveWidth
        }

    /*
     * Active pill grows INSIDE the fixed slot.
     *
     * It does NOT affect Row measurement.
     */
    val pillWidth =
        SlotWidth +
            (
                activeWidth -
                    SlotWidth
                ) *
            progress

    /*
     * Active pill background fades independently,
     * but follows the same fluid progress.
     */
    val pillAlpha =
        smoothFade(progress)

    /*
     * Text appears as ONE COMPLETE TEXT OBJECT.
     *
     * There is NO width clipping.
     * Therefore characters cannot appear one-by-one.
     */
    val textProgress =
        smoothFade(
            (
                (progress - 0.16f) /
                    0.68f
            ).coerceIn(0f, 1f)
        )

    val background by
        animateColorAsState(
            targetValue =
                if (active) {
                    colors.cardElevated
                } else {
                    Color.Transparent
                },
            animationSpec =
                tween(
                    durationMillis = 360,
                    easing = NavFluidEasing
                ),
            label = "navPillColor"
        )

    val foreground by
        animateColorAsState(
            targetValue =
                if (active) {
                    colors.primaryText
                } else {
                    colors.mutedText
                },
            animationSpec =
                tween(
                    durationMillis = 300,
                    easing = NavFluidEasing
                ),
            label = "navForeground"
        )

    Box(
        modifier = Modifier
            .width(SlotWidth)
            .height(ItemHeight)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.CenterStart
    ) {

        /*
         * ACTIVE PILL
         *
         * This is visually wider than the slot,
         * but because it is drawn as an overlay,
         * it doesn't push any neighboring item.
         */
        Box(
            modifier =
                Modifier
                    .width(pillWidth)
                    .fillMaxHeight()
                    .clip(
                        RoundedCornerShape(28.5.dp)
                    )
                    .background(
                        background.copy(
                            alpha = pillAlpha
                        )
                    )
        )

        /*
         * CONTENT stays independent from pill geometry.
         */
        NavigationContentV2(
            destination = destination,
            active = active,
            progress = progress,
            textProgress = textProgress,
            color = foreground
        )
    }
}


@Composable
private fun NavigationContentV2(
    destination: XvoxDestination,
    active: Boolean,
    progress: Float,
    textProgress: Float,
    color: Color
) {
    /*
     * Icon is always at the same fixed location.
     *
     * No horizontal movement when switching tabs.
     */
    XvoxNavigationIcon(
        destination = destination,
        color = color,
        modifier =
            Modifier
                .padding(start = 24.dp)
                .size(22.dp)
    )

    /*
     * Text is positioned AFTER the icon,
     * but it is NOT width-revealed.
     *
     * It simply fades and slides in as a complete object.
     */
    val textStart =
        when (destination) {

            XvoxDestination.HOME ->
                44.dp

            XvoxDestination.SEARCH ->
                53.dp

            XvoxDestination.SETTINGS ->
                66.dp
        }

    val textOffset =
        (1f - textProgress) *
            5.dp.value

    Box(
        modifier =
            Modifier
                .offset(
                    x = textStart + 12.dp
                )
                .graphicsLayer {

                    alpha = textProgress

                    translationX =
                        textOffset

                    /*
                     * Almost imperceptible scale.
                     *
                     * Prevents the text from looking
                     * like it suddenly pops into existence.
                     */
                    val scale =
                        0.985f +
                            0.015f *
                            textProgress

                    scaleX = scale
                    scaleY = scale
                }
    ) {

        Text(
            text = destination.label,
            color = color,
            fontSize = 14.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}


/*
 * Soft fade curve.
 *
 * 0   = invisible
 * 1   = completely visible
 *
 * No hard reveal edge.
 */
private fun smoothFade(
    value: Float
): Float {
    val v =
        value.coerceIn(
            0f,
            1f
        )

    /*
     * Quintic smoothstep.
     *
     * Much softer than a simple linear fade.
     */
    return v *
        v *
        v *
        (
            v *
                (
                    v * 6f -
                        15f
                ) +
                10f
            )
}


/*
 * Kept for compatibility if you were using
 * smoothStep elsewhere in this file.
 */
private fun smoothStep(
    value: Float
): Float {
    val v =
        value.coerceIn(
            0f,
            1f
        )

    return v *
        v *
        (3f - 2f * v)
}
