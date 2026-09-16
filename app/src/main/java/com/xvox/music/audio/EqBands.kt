package com.xvox.music.audio

import kotlin.math.ln
import kotlin.math.roundToInt

object EqBands {
    val five = doubleArrayOf(60.0, 230.0, 910.0, 3600.0, 14000.0)
    val ten = doubleArrayOf(31.0, 62.0, 125.0, 250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0, 16000.0)
    fun count(value: Int): Int = if (value == 10) 10 else 5
    fun frequencies(count: Int): DoubleArray = if (count == 10) ten else five
    fun convert(values: List<Int>, targetCount: Int): List<Int> {
        val target = frequencies(targetCount)
        val source = frequencies(if (values.size >= 10) 10 else 5)
        if (values.size == target.size) return values.map { it.coerceIn(-12, 12) }
        return target.map { frequency ->
            val hi = source.indexOfFirst { it >= frequency }
            when {
                hi == 0 -> values.firstOrNull() ?: 0
                hi < 0 -> values.lastOrNull() ?: 0
                else -> {
                    val fraction = (ln(frequency) - ln(source[hi - 1])) / (ln(source[hi]) - ln(source[hi - 1]))
                    val a = values.getOrElse(hi - 1) { 0 }; val b = values.getOrElse(hi) { 0 }
                    (a + (b - a) * fraction).roundToInt()
                }
            }.coerceIn(-12, 12)
        }
    }
    fun label(hz: Double): String = if (hz >= 1000) "${(hz / 1000).let { if (it % 1 == 0.0) it.toInt().toString() else it.toString() }}k" else hz.toInt().toString()
}
