package com.xvox.music.features.settings.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import kotlin.math.roundToInt

/** Measure the actual UI at its real size, then scale BOTH axes by the same factor. Never squash it. */
@Composable
fun UniformPreview(width: Dp, height: Dp, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content = { Box(Modifier.requiredSize(width, height)) { content() } }, modifier = modifier.clipToBounds()) { children, constraints ->
        val realWidth = width.roundToPx().coerceAtLeast(1)
        val realHeight = height.roundToPx().coerceAtLeast(1)
        val maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else realWidth
        val maxHeight = if (constraints.hasBoundedHeight) constraints.maxHeight else realHeight
        val scale = minOf(maxWidth.toFloat() / realWidth, maxHeight.toFloat() / realHeight)
        val previewHeight = constraints.constrainHeight((realHeight * scale).roundToInt())
        val previewWidth = constraints.constrainWidth((realWidth * scale).roundToInt())
        val child = children.single().measure(Constraints.fixed(realWidth, realHeight))
        layout(previewWidth, previewHeight) {
            child.placeWithLayer(((previewWidth - realWidth * scale) / 2).roundToInt(), ((previewHeight - realHeight * scale) / 2).roundToInt()) {
                scaleX = scale; scaleY = scale
                transformOrigin = TransformOrigin(0f, 0f)
            }
        }
    }
}
