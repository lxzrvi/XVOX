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
 * Do barabar columns. Har card sirf EK baar measure hota hai (Compose me double measure crash karta hai).
 * Cards ko aise baanta jata hai ki dono columns ki height lagbhag same ho, aur bacha hua chhota farak
 * shorter column ke gaps me barabar baant diya jata hai, taaki dono columns ek hi line pe khatam hon.
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
        val cardConstraints = Constraints(
            minWidth = colW,
            maxWidth = colW,
            minHeight = 0,
            maxHeight = Constraints.Infinity
        )

        // Har card ek hi baar measure
        val placeables = measurables.map { it.measure(cardConstraints) }
        val n = placeables.size
        val heights = IntArray(n) { placeables[it].height }

        fun colHeight(idx: List<Int>) =
            idx.sumOf { heights[it] } + gapPx * max(idx.size - 1, 0)

        // Best split: dono columns ki height ka farak sabse kam
        var bestMask = 0
        var bestDiff = Int.MAX_VALUE
        if (n in 1..14) {
            for (mask in 0 until (1 shl n)) {
                val l = (0 until n).filter { ((mask shr it) and 1) == 0 }
                val r = (0 until n).filter { ((mask shr it) and 1) == 1 }
                val diff = abs(colHeight(l) - colHeight(r))
                if (diff < bestDiff) {
                    bestDiff = diff
                    bestMask = mask
                }
            }
        } else {
            // Bahut zyada cards ho toh simple greedy
            var hl = 0
            var hr = 0
            for (i in 0 until n) {
                if (hl <= hr) {
                    hl += heights[i]
                } else {
                    hr += heights[i]
                    bestMask = bestMask or (1 shl i)
                }
            }
        }
        val left = (0 until n).filter { ((bestMask shr it) and 1) == 0 }
        val right = (0 until n).filter { ((bestMask shr it) and 1) == 1 }
        val total = max(colHeight(left), colHeight(right))

        // Shorter column ka bacha farak uske gaps me barabar (cards stretch nahi hote)
        fun place(
            scope: androidx.compose.ui.layout.Placeable.PlacementScope,
            idx: List<Int>,
            x: Int
        ) {
            val extra = total - colHeight(idx)
            val gaps = max(idx.size - 1, 0)
            val base = if (gaps > 0 && extra > 0) extra / gaps else 0
            val rem = if (gaps > 0 && extra > 0) extra % gaps else 0
            var y = 0
            idx.forEachIndexed { pos, i ->
                with(scope) { placeables[i].placeRelative(x, y) }
                y += heights[i] + gapPx + base + (if (pos < rem) 1 else 0)
            }
        }

        val width = constraints.maxWidth
        val height = total.coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(width, height) {
            place(this, left, 0)
            place(this, right, colW + gapPx)
        }
    }
}
