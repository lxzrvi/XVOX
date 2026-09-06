package com.xvox.music.features.home.allsongs

import com.xvox.music.core.model.Song
import kotlin.random.Random

data class MosaicTile(
    val song: Song,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

data class MosaicPage(
    val tiles: List<MosaicTile>
)

data class Spec(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

private val processMosaicSeed = System.currentTimeMillis() xor (System.nanoTime().shl(16))

fun buildMosaicPagePlans(
    songs: List<Song>,
    rows: Int = 4,
    isUniform: Boolean = false
): List<XvoxMosaicPagePlan> {
    if (songs.isEmpty()) return emptyList()

    val seed = songs.fold(processMosaicSeed) { value, song -> value * 31L + song.id }
    val random = Random(seed)
    val plans = ArrayList<XvoxMosaicPagePlan>(songs.size / (rows * 3).coerceAtLeast(1) + 1)
    var index = 0

    val maxPerPage = 4 * rows.coerceIn(3, 8)

    while (index < songs.size) {
        val remaining = songs.size - index
        val count = when {
            remaining <= maxPerPage -> remaining
            isUniform -> maxPerPage
            else -> {
                val minCount = (maxPerPage - 4).coerceAtLeast(maxPerPage / 2)
                (minCount..maxPerPage).random(random)
            }
        }

        plans += XvoxMosaicPagePlan(
            startIndex = index,
            songCount = count,
            layoutSeed = random.nextLong()
        )
        index += count
    }

    return plans
}

fun buildMosaicPage(
    songs: List<Song>,
    plan: XvoxMosaicPagePlan,
    rows: Int = 4,
    isUniform: Boolean = false
): MosaicPage {
    if (plan.songCount <= 0 || plan.startIndex !in songs.indices) {
        return MosaicPage(emptyList())
    }

    val end = (plan.startIndex + plan.songCount).coerceAtMost(songs.size)
    val pageSongs = songs.subList(plan.startIndex, end)
    val random = Random(plan.layoutSeed)
    val safeRows = rows.coerceIn(3, 8)

    val specs = if (isUniform) {
        regularSpecs(4, pageSongs.size)
    } else {
        generateMosaicSpecs(4, safeRows, pageSongs.size, random)
    }

    return MosaicPage(
        pageSongs.mapIndexed { index, song ->
            val spec = specs.getOrElse(index) {
                Spec((index % 4).toFloat(), (index / 4).toFloat(), 1f, 1f)
            }
            MosaicTile(
                song = song,
                x = spec.x,
                y = spec.y,
                width = spec.width,
                height = spec.height
            )
        }
    )
}

fun generateMosaicSpecs(cols: Int, rows: Int, count: Int, random: Random): List<Spec> {
    if (count <= 0) return emptyList()
    val totalSlots = cols * rows
    if (count >= totalSlots) {
        return regularSpecs(cols, count)
    }

    val neededReduction = totalSlots - count

    for (attempt in 0 until 50) {
        val grid = Array(rows) { BooleanArray(cols) { false } }
        val curSpecs = mutableListOf<Spec>()
        var curReduction = 0

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (grid[r][c]) continue
                val remRed = neededReduction - curReduction
                var placed = false

                if (remRed >= 3 && r + 1 < rows && c + 1 < cols &&
                    !grid[r][c + 1] && !grid[r + 1][c] && !grid[r + 1][c + 1]
                ) {
                    if (random.nextFloat() < 0.4f || remRed >= 3 * (rows - r)) {
                        grid[r][c] = true
                        grid[r][c + 1] = true
                        grid[r + 1][c] = true
                        grid[r + 1][c + 1] = true
                        curSpecs.add(Spec(c.toFloat(), r.toFloat(), 2f, 2f))
                        curReduction += 3
                        placed = true
                    }
                }

                if (!placed && remRed >= 1 && c + 1 < cols && !grid[r][c + 1]) {
                    if (random.nextFloat() < 0.5f || remRed >= 1) {
                        grid[r][c] = true
                        grid[r][c + 1] = true
                        curSpecs.add(Spec(c.toFloat(), r.toFloat(), 2f, 1f))
                        curReduction += 1
                        placed = true
                    }
                }

                if (!placed && remRed >= 1 && r + 1 < rows && !grid[r + 1][c]) {
                    if (random.nextFloat() < 0.5f || remRed >= 1) {
                        grid[r][c] = true
                        grid[r + 1][c] = true
                        curSpecs.add(Spec(c.toFloat(), r.toFloat(), 1f, 2f))
                        curReduction += 1
                        placed = true
                    }
                }

                if (!placed) {
                    grid[r][c] = true
                    curSpecs.add(Spec(c.toFloat(), r.toFloat(), 1f, 1f))
                }
            }
        }

        if (curSpecs.size == count) {
            return curSpecs
        }
    }

    return regularSpecs(cols, count)
}

fun regularSpecs(cols: Int, count: Int): List<Spec> = List(count) { index ->
    Spec(
        x = (index % cols).toFloat(),
        y = (index / cols).toFloat(),
        width = 1f,
        height = 1f
    )
}
