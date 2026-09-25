package com.xvox.music.features.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import com.xvox.music.core.design.theme.XvoxTheme

/**
 * A continuous, compact slider in the Equalizer's thin-track language. It intentionally owns no
 * label or default copy, so callers can place it in sheets without creating excess vertical text.
 */
@Composable
fun XvoxContinuousSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    modifier: Modifier = Modifier,
    defaultValue: Float? = null,
    enabled: Boolean = true,
    contentDescription: String = "Value"
) {
    val colors = XvoxTheme.colors
    val latestChange by rememberUpdatedState(onValueChange)
    val start = valueRange.start
    val end = valueRange.endInclusive
    val span = (end - start).coerceAtLeast(.0001f)
    val current = value.coerceIn(valueRange)
    val fraction = ((current - start) / span).coerceIn(0f, 1f)
    val defaultFraction = defaultValue?.let { ((it.coerceIn(valueRange) - start) / span).coerceIn(0f, 1f) }

    fun setFraction(raw: Float) {
        if (!enabled) return
        latestChange((start + raw.coerceIn(0f, 1f) * span).coerceIn(valueRange))
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(30.dp)
            .semantics {
                this.contentDescription = contentDescription
                progressBarRangeInfo = ProgressBarRangeInfo(current, valueRange, 100)
                setProgress { requested ->
                    if (enabled) {
                        latestChange(requested.coerceIn(valueRange))
                        true
                    } else {
                        false
                    }
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    setFraction(offset.x / size.width.toFloat().coerceAtLeast(1f))
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    setFraction(change.position.x / size.width.toFloat().coerceAtLeast(1f))
                }
            }
    ) {
        val shape = RoundedCornerShape(50)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .height(4.dp)
                .clip(shape)
                .background(colors.progressTrack)
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(maxWidth * fraction)
                .height(4.dp)
                .clip(shape)
                .background(colors.primaryAccent)
        )
        // The marker is constrained to the rail's own 4dp thickness.
        if (defaultFraction != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = maxWidth * defaultFraction - .5.dp)
                    .width(1.dp)
                    .height(4.dp)
                    .background(colors.primaryText.copy(alpha = .42f))
            )
        }
    }
}
