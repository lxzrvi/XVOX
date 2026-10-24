package com.xvox.music.features.settings.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max

/**
 * Two equal columns. Cards are distributed to keep column heights close, then any remaining
 * small difference is shared across the shorter column's cards so both columns finish aligned.
 */
@Composable
fun BalancedTwoColumn(
    modifier: Modifier = Modifier,
    gap: Dp = 10.dp,
    content: @Composable () -> Unit
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val gapPx = gap.roundToPx()
        val colW = ((constraints.maxWidth - gapPx) / 2).coerceAtLeast(0)
        val n = measurables.size

        // 1) Measure every card at its natural height.
        val placeables = measurables
            .map { it.measure(Constraints(minWidth = colW, maxWidth = colW)) }
            .toMutableList()
        val natural = IntArray(n) { placeables[it].height }

        // 2) Find the split with the least difference while preserving each column's item order.
        var bestMask = 0
        var bestDiff = Int.MAX_VALUE
        if (n in 1..14) {
            for (mask in 0 until (1 shl n)) {
                var a = 0
                var b = 0
                var ca = 0
                var cb = 0
                for (i in 0 until n) {
                    if (((mask shr i) and 1) == 0) {
                        a += natural[i]
                        ca++
                    } else {
                        b += natural[i]
                        cb++
                    }
                }
                val ha = a + gapPx * max(ca - 1, 0)
                val hb = b + gapPx * max(cb - 1, 0)
                val diff = abs(ha - hb)
                if (diff < bestDiff) {
                    bestDiff = diff
                    bestMask = mask
                }
            }
        }
        val left = (0 until n).filter { ((bestMask shr it) and 1) == 0 }
        val right = (0 until n).filter { ((bestMask shr it) and 1) == 1 }

        fun columnHeight(indices: List<Int>) =
            indices.sumOf { natural[it] } + gapPx * max(indices.size - 1, 0)

        val total = max(columnHeight(left), columnHeight(right))

        // 3) Share the remaining difference inside shorter-column cards, never by stretching gaps.
        val finalH = IntArray(n) { natural[it] }
        fun stretch(indices: List<Int>) {
            if (indices.isEmpty()) return
            val difference = total - columnHeight(indices)
            if (difference <= 0) return
            val each = difference / indices.size
            indices.forEach { finalH[it] += each }
            finalH[indices.last()] += difference - each * indices.size
            indices.forEach { index ->
                placeables[index] = measurables[index].measure(Constraints.fixed(colW, finalH[index]))
            }
        }
        stretch(left)
        stretch(right)

        val width = constraints.maxWidth
        val height = total.coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(width, height) {
            var leftY = 0
            left.forEach { index ->
                placeables[index].placeRelative(0, leftY)
                leftY += finalH[index] + gapPx
            }
            var rightY = 0
            right.forEach { index ->
                placeables[index].placeRelative(colW + gapPx, rightY)
                rightY += finalH[index] + gapPx
            }
        }
    }
}
