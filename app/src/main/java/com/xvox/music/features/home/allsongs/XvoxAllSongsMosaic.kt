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
    if (!isUniform && !mosaicOne && songs.isNotEmpty()) {
        val minimum = rows.coerceIn(3, 8) + 2
        val average = maxOf(minimum, (capacity * .62f).toInt())
        var pages = ((songs.size + average - 1) / average).coerceAtLeast(1)
        while (pages > 1 && songs.size / pages < minimum) pages--
        var offset = 0
        return List(pages) { page ->
            val count = songs.size / pages + if (page < songs.size % pages) 1 else 0
            XvoxMosaicPagePlan(offset, count, random.nextLong()).also { offset += count }
        }
    }
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
        isUniform || (mosaicOne && page.size <= 4) -> regularSpecs(4, page.size)
        mosaicOne -> generateClassicMosaicSpecs(4, rows.coerceIn(3, 8), page.size, random)
        else -> generateMosaicSpecs(4, mosaicRows(songs.size, rows), page.size, random)
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
private fun generateUnbiasedMosaicSpecs(cols: Int, rows: Int, count: Int, random: Random): List<Spec> {
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

/** A small whole library shrinks naturally; full pages always fill their selected row budget. */
fun mosaicRows(songCount: Int, requested: Int): Int = if (songCount < requested + 2)
    minOf(requested, ((songCount + 2) / 3).coerceAtLeast(1)) else requested

fun generateMosaicSpecs(cols: Int, rows: Int, count: Int, random: Random): List<Spec> {
    if (cols != 4 || rows < 2 || count < rows || count >= cols * rows) return generateUnbiasedMosaicSpecs(cols, rows, count, random)
    data class Tile(val spec: Spec, val wideAnchor: Boolean = false)
    repeat(24) {
        val tiles = regularSpecs(cols, cols * rows).map { Tile(it) }.toMutableList()
        if (cols * rows - count >= 2) {
            val firstRow = random.nextInt(rows)
            val secondRow = (firstRow + 1 + random.nextInt(rows - 1)) % rows
            for ((row, col) in listOf(firstRow to 0, secondRow to 2)) {
                val a = tiles.indexOfFirst { it.spec.x == col.toFloat() && it.spec.y == row.toFloat() }
                val b = tiles.indexOfFirst { it.spec.x == (col + 1).toFloat() && it.spec.y == row.toFloat() }
                tiles.removeAt(maxOf(a, b)); tiles.removeAt(minOf(a, b))
                tiles.add(Tile(Spec(col.toFloat(), row.toFloat(), 2f, 1f), true))
            }
        }
        while (tiles.size > count) {
            data class Merge(val a: Int, val b: Int, val tile: Tile, val weight: Double)
            val candidates = mutableListOf<Merge>()
            for (a in tiles.indices) for (b in a + 1 until tiles.size) {
                val x = tiles[a].spec; val y = tiles[b].spec
                val horizontal = x.y == y.y && x.height == y.height && (x.x + x.width == y.x || y.x + y.width == x.x)
                val vertical = x.x == y.x && x.width == y.width && (x.y + x.height == y.y || y.y + y.height == x.y)
                if (!horizontal && !vertical) continue
                val merged = if (horizontal) Spec(minOf(x.x, y.x), x.y, x.width + y.width, x.height)
                    else Spec(x.x, minOf(x.y, y.y), x.width, x.height + y.height)
                val anchored = tiles[a].wideAnchor || tiles[b].wideAnchor
                if (anchored && merged.width < merged.height * 1.3f) continue
                if (merged.width == 1f && merged.height > 2f) continue
                val ratio = merged.width / merged.height
                val weight = when { ratio >= 1.5f -> 2.5; ratio >= 1f -> 1.1; else -> .35 }
                candidates.add(Merge(a, b, Tile(merged, anchored), weight))
            }
            if (candidates.isEmpty()) break
            var pick = random.nextDouble() * candidates.sumOf { it.weight }
            val chosen = candidates.firstOrNull { pick -= it.weight; pick <= 0 } ?: candidates.last()
            tiles.removeAt(chosen.b); tiles.removeAt(chosen.a); tiles.add(chosen.tile)
        }
        if (tiles.size == count) return tiles.map { it.spec }.sortedWith(compareBy<Spec> { it.y }.thenBy { it.x })
    }
    // Guaranteed gap-free fallback, biased to wide horizontal strips rather than tall slivers.
    val counts = IntArray(rows) { 1 }
    repeat(count - rows) { counts[counts.indices.filter { counts[it] < cols }.random(random)]++ }
    return buildList {
        counts.forEachIndexed { row, n ->
            val cuts = (1 until cols).shuffled(random).take(n - 1).sorted() + cols
            var x = 0
            cuts.forEach { end -> add(Spec(x.toFloat(), row.toFloat(), (end - x).toFloat(), 1f)); x = end }
        }
    }
}
