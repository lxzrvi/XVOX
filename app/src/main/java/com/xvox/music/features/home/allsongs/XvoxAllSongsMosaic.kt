package com.xvox.music.features.home.allsongs

import com.xvox.music.core.model.Song
import kotlin.math.ceil
import kotlin.random.Random

data class MosaicTile(val song: Song, val x: Float, val y: Float, val width: Float, val height: Float, val style: Int = 0)
data class MosaicPage(val tiles: List<MosaicTile>)
data class Spec(val x: Float, val y: Float, val width: Float, val height: Float)

// Stable during this process (scroll, tab switches and rotations); fresh on a real app restart.
object XvoxMosaicSession {
    var seed: Long = System.nanoTime() xor System.currentTimeMillis()
        private set
    fun begin() { seed = System.nanoTime() xor System.currentTimeMillis() }
}

fun buildMosaicPagePlans(songs: List<Song>, rows: Int = 4, isUniform: Boolean = false, mosaicOne: Boolean = false): List<XvoxMosaicPagePlan> {
    val random = Random(songs.fold(XvoxMosaicSession.seed) { seed, song -> seed * 31 + song.id })
    val capacity = 4 * rows.coerceIn(3, 8)
    return buildList {
        var index = 0
        while (index < songs.size) {
            val count = when {
                mosaicOne && songs.size - index <= capacity -> songs.size - index
                isUniform -> capacity
                mosaicOne -> random.nextInt((capacity - 4).coerceAtLeast(capacity / 2), capacity + 1)
                else -> random.nextInt((capacity / 3).coerceAtLeast(5), capacity - 1)
            }
            val take = count.coerceAtMost(songs.size - index)
            add(XvoxMosaicPagePlan(index, take, random.nextLong()))
            index += take
        }
    }
}

fun buildMosaicPage(songs: List<Song>, plan: XvoxMosaicPagePlan, rows: Int = 4, isUniform: Boolean = false, mosaicOne: Boolean = false): MosaicPage {
    if (plan.songCount <= 0 || plan.startIndex !in songs.indices) return MosaicPage(emptyList())
    val page = songs.subList(plan.startIndex, (plan.startIndex + plan.songCount).coerceAtMost(songs.size))
    val random = Random(plan.layoutSeed)
    val compactRows = minOf(rows.coerceIn(3, 8), ceil(page.size / 2.5).toInt().coerceAtLeast(1))
    val specs = when {
        isUniform || page.size <= 4 -> regularSpecs(4, page.size)
        mosaicOne -> generateClassicMosaicSpecs(4, rows.coerceIn(3, 8), page.size, random)
        else -> generateMosaicSpecs(4, compactRows, page.size, random)
    }
    return MosaicPage(page.mapIndexed { i, song ->
        val s = specs[i]
        MosaicTile(song, s.x, s.y, s.width, s.height, if (mosaicOne) 0 else random.nextInt(24))
    })
}

/**
 * Random guillotine partitioning gives thousands of valid layouts, rather than a small template list.
 * Each split conserves area, so every tile is non-overlapping and every song is placed exactly once.
 * Shapes include portrait strips, wide banners, squares and large artwork tiles up to 4 x 8 units.
 */
fun generateMosaicSpecs(cols: Int, rows: Int, count: Int, random: Random): List<Spec> {
    if (count <= 0 || cols <= 0 || rows <= 0) return emptyList()
    if (count >= cols * rows) return regularSpecs(cols, count)
    val result = mutableListOf(Spec(0f, 0f, cols.toFloat(), rows.toFloat()))
    while (result.size < count) {
        val candidates = result.indices.filter { result[it].width > 1 || result[it].height > 1 }
        if (candidates.isEmpty()) break
        // Vary both the selected region and the split point; no repeated 5/6-layout cycle.
        val index = if (random.nextBoolean()) candidates.random(random)
            else candidates.maxBy { result[it].width * result[it].height }
        val old = result.removeAt(index)
        val vertical = old.width > 1 && (old.height <= 1 || random.nextBoolean())
        if (vertical) {
            val split = random.nextInt(1, old.width.toInt()).toFloat()
            result.add(Spec(old.x, old.y, split, old.height))
            result.add(Spec(old.x + split, old.y, old.width - split, old.height))
        } else {
            val split = random.nextInt(1, old.height.toInt()).toFloat()
            result.add(Spec(old.x, old.y, old.width, split))
            result.add(Spec(old.x, old.y + split, old.width, old.height - split))
        }
    }
    return result.sortedWith(compareBy<Spec> { it.y }.thenBy { it.x })
}

fun regularSpecs(cols: Int, count: Int): List<Spec> = List(count.coerceAtLeast(0)) { i ->
    Spec((i % cols.coerceAtLeast(1)).toFloat(), (i / cols.coerceAtLeast(1)).toFloat(), 1f, 1f)
}

/** Original xvox Mosaic 1 tiling, retained without changing its proportions. */
fun generateClassicMosaicSpecs(cols: Int, rows: Int, count: Int, random: Random): List<Spec> {
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
