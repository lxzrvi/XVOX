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

/**
 * Page planning.
 *
 * The mosaic rule is simple: a page never leaves a hole, and the last page is a real page — not a
 * leftover strip. Song counts are therefore balanced across pages so the final page always has
 * enough tiles to cover its grid at sane proportions.
 */
fun buildMosaicPagePlans(songs: List<Song>, rows: Int = 4, isUniform: Boolean = false, mosaicOne: Boolean = false): List<XvoxMosaicPagePlan> {
    val random = Random(songs.fold(XvoxMosaicSession.seed) { seed, song -> seed * 31 + song.id })
    val safeRows = rows.coerceIn(3, 8)
    val capacity = 4 * safeRows
    if (songs.isEmpty()) return emptyList()

    if (isUniform) {
        return buildList {
            var index = 0
            while (index < songs.size) {
                val take = capacity.coerceAtMost(songs.size - index)
                add(XvoxMosaicPagePlan(index, take, random.nextLong()))
                index += take
            }
        }
    }

    // Balanced split: every page (including the last) gets a comparable, coverable song count.
    val minimum = (safeRows + 2).coerceAtMost(capacity)
    val average = if (mosaicOne) maxOf(minimum, (capacity * .78f).toInt())
    else maxOf(minimum, (capacity * .62f).toInt())
    var pages = ((songs.size + average - 1) / average).coerceAtLeast(1)
    while (pages > 1 && songs.size / pages < minimum) pages--
    var offset = 0
    return List(pages) { page ->
        val count = songs.size / pages + if (page < songs.size % pages) 1 else 0
        XvoxMosaicPagePlan(offset, count, random.nextLong()).also { offset += count }
    }
}

/**
 * Rows a single page should occupy.
 *
 * A page always fills every row it claims. When a page holds fewer songs than the requested grid
 * can carry at reasonable tile sizes, it claims fewer rows instead of leaving the bottom empty.
 */
fun mosaicPageRows(count: Int, requested: Int, cols: Int = 4): Int {
    if (count <= 0) return 0
    val max = requested.coerceIn(1, 8)
    val minimumRows = ceil(count / cols.toDouble()).toInt()          // one cell per tile at least
    val comfortable = ceil(count / (cols * 0.62)).toInt()            // ~1.6 cells per tile
    return comfortable.coerceIn(minimumRows.coerceAtLeast(1), max)
}

/**
 * Rows for one page. In paged (horizontal) mode every page claims the full grid so pages stay the
 * same height and the last one is filled edge to edge; in flowing (vertical) mode a short page
 * simply claims fewer rows instead of padding itself out.
 */
fun mosaicRowsForPage(count: Int, requested: Int, fill: Boolean): Int =
    if (fill && count >= requested.coerceIn(1, 8)) requested.coerceIn(1, 8) else mosaicPageRows(count, requested)

fun buildMosaicPage(
    songs: List<Song>, plan: XvoxMosaicPagePlan, rows: Int = 4,
    isUniform: Boolean = false, mosaicOne: Boolean = false, fillRows: Boolean = false
): MosaicPage {
    if (plan.songCount <= 0 || plan.startIndex !in songs.indices) return MosaicPage(emptyList())
    val page = songs.subList(plan.startIndex, (plan.startIndex + plan.songCount).coerceAtMost(songs.size))
    val random = Random(plan.layoutSeed)
    val pageRows = mosaicRowsForPage(page.size, rows, fillRows)
    val specs = when {
        isUniform -> regularSpecs(4, page.size)
        mosaicOne -> generateClassicMosaicSpecs(4, pageRows, page.size, random)
        else -> generateMosaicSpecs(4, pageRows, page.size, random)
    }
    return MosaicPage(page.mapIndexed { i, song ->
        val s = specs[i]
        MosaicTile(song, s.x, s.y, s.width, s.height, random.nextInt(24))
    })
}

/**
 * Random guillotine partitioning gives thousands of valid layouts, rather than a small template list.
 * Each split conserves area, so every tile is non-overlapping and every song is placed exactly once.
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

/**
 * Shared exact-cover engine.
 *
 * Starts from a full grid of unit cells and merges neighbours until exactly [count] tiles remain.
 * Merging two aligned, adjacent rectangles always produces a rectangle, so coverage is conserved:
 * the page can never end up with a gap, an overlap, or an unused corner on the last page.
 */
private fun mergeToExactCover(
    cols: Int, rows: Int, count: Int, random: Random,
    attempts: Int = 20,
    seedTiles: (MutableList<MergeTile>, Random) -> Unit = { _, _ -> },
    allowed: (Float, Float) -> Boolean,
    weight: (Float, Float) -> Double
): List<Spec>? {
    if (count < 1 || count > cols * rows) return null
    repeat(attempts) {
        val tiles = regularSpecs(cols, cols * rows).mapTo(mutableListOf()) { MergeTile(it, false) }
        seedTiles(tiles, random)
        var stuck = false
        while (tiles.size > count && !stuck) {
            var total = 0.0
            val candidates = ArrayList<Merge>(tiles.size * 2)
            for (a in tiles.indices) for (b in a + 1 until tiles.size) {
                val x = tiles[a].spec; val y = tiles[b].spec
                val horizontal = x.y == y.y && x.height == y.height && (x.x + x.width == y.x || y.x + y.width == x.x)
                val vertical = x.x == y.x && x.width == y.width && (x.y + x.height == y.y || y.y + y.height == x.y)
                if (!horizontal && !vertical) continue
                val merged = if (horizontal) Spec(minOf(x.x, y.x), x.y, x.width + y.width, x.height)
                else Spec(x.x, minOf(x.y, y.y), x.width, x.height + y.height)
                if (!allowed(merged.width, merged.height)) continue
                val anchored = tiles[a].wideAnchor || tiles[b].wideAnchor
                if (anchored && merged.width < merged.height * 1.3f) continue
                val w = weight(merged.width, merged.height)
                if (w <= 0.0) continue
                total += w
                candidates.add(Merge(a, b, MergeTile(merged, anchored), w))
            }
            if (candidates.isEmpty()) { stuck = true; break }
            var pick = random.nextDouble() * total
            val chosen = candidates.firstOrNull { pick -= it.weight; pick <= 0 } ?: candidates.last()
            tiles.removeAt(chosen.b); tiles.removeAt(chosen.a); tiles.add(chosen.tile)
        }
        if (tiles.size == count) return tiles.map { it.spec }.sortedWith(compareBy<Spec> { it.y }.thenBy { it.x })
    }
    return null
}

private data class MergeTile(val spec: Spec, val wideAnchor: Boolean = false)
private data class Merge(val a: Int, val b: Int, val tile: MergeTile, val weight: Double)

/**
 * Mosaic 1.
 *
 * The original XVOX feel, with a wider shape vocabulary: squares, wide banners, tall portraits,
 * long 3-wide strips and large 2x2/3x2 hero tiles. Coverage is exact, so the last page fills
 * completely instead of trailing off.
 */
fun generateClassicMosaicSpecs(cols: Int, rows: Int, count: Int, random: Random): List<Spec> {
    if (count <= 0 || cols <= 0 || rows <= 0) return emptyList()
    if (count >= cols * rows) return regularSpecs(cols, count)

    // Long, short and wide all get a real share; nothing degenerates into slivers.
    val allowed: (Float, Float) -> Boolean = { w, h ->
        val area = w * h
        w <= 4f && h <= 3f && area <= 8f && !(w == 1f && h > 3f) && !(h == 1f && w > 4f)
    }
    val weight: (Float, Float) -> Double = { w, h ->
        val ratio = w / h
        when {
            w == 2f && h == 2f -> 1.5   // hero squares
            ratio >= 2.5f -> 1.6        // long wide strips
            ratio >= 1.4f -> 2.4        // wide cards
            ratio > 0.8f -> 1.2         // near square
            ratio >= 0.5f -> 1.5        // portraits
            else -> 0.5                 // tall slivers stay rare
        }
    }
    mergeToExactCover(cols, rows, count, random, allowed = allowed, weight = weight)?.let { return it }
    // Relaxed pass: keep exact coverage even for awkward counts.
    mergeToExactCover(cols, rows, count, random, attempts = 12,
        allowed = { w, h -> w <= 4f && h <= 4f }, weight = { _, _ -> 1.0 })?.let { return it }
    return generateUnbiasedMosaicSpecs(cols, rows, count, random)
}

/** A small whole library shrinks naturally; full pages always fill their selected row budget. */
fun mosaicRows(songCount: Int, requested: Int): Int = if (songCount < requested + 2)
    minOf(requested, ((songCount + 2) / 3).coerceAtLeast(1)) else requested

/** Mosaic 2: balanced wide/square/portrait mix with two protected wide anchors. */
fun generateMosaicSpecs(cols: Int, rows: Int, count: Int, random: Random): List<Spec> {
    if (cols != 4 || rows < 2 || count < rows || count >= cols * rows) return generateUnbiasedMosaicSpecs(cols, rows, count, random)
    val seed: (MutableList<MergeTile>, Random) -> Unit = { tiles, rnd ->
        if (cols * rows - count >= 2) {
            val firstRow = rnd.nextInt(rows)
            val secondRow = (firstRow + 1 + rnd.nextInt(rows - 1)) % rows
            for ((row, col) in listOf(firstRow to 0, secondRow to 2)) {
                val a = tiles.indexOfFirst { it.spec.x == col.toFloat() && it.spec.y == row.toFloat() }
                val b = tiles.indexOfFirst { it.spec.x == (col + 1).toFloat() && it.spec.y == row.toFloat() }
                if (a >= 0 && b >= 0) {
                    tiles.removeAt(maxOf(a, b)); tiles.removeAt(minOf(a, b))
                    tiles.add(MergeTile(Spec(col.toFloat(), row.toFloat(), 2f, 1f), true))
                }
            }
        }
    }
    mergeToExactCover(cols, rows, count, random, attempts = 24, seedTiles = seed,
        allowed = { w, h -> !(w == 1f && h > 2f) },
        weight = { w, h -> val ratio = w / h; when { ratio >= 1.5f -> 2.5; ratio >= 1f -> 1.1; else -> .35 } }
    )?.let { return it }

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
