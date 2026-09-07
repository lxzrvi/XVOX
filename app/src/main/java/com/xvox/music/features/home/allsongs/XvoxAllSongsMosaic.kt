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

fun buildMosaicPagePlans(songs: List<Song>, rows: Int = 4, isUniform: Boolean = false): List<XvoxMosaicPagePlan> {
    val random = Random(songs.fold(XvoxMosaicSession.seed) { seed, song -> seed * 31 + song.id })
    val capacity = 4 * rows.coerceIn(3, 8)
    return buildList {
        var index = 0
        while (index < songs.size) {
            val count = if (isUniform) capacity else random.nextInt((capacity / 3).coerceAtLeast(5), capacity - 1)
            val take = count.coerceAtMost(songs.size - index)
            add(XvoxMosaicPagePlan(index, take, random.nextLong()))
            index += take
        }
    }
}

fun buildMosaicPage(songs: List<Song>, plan: XvoxMosaicPagePlan, rows: Int = 4, isUniform: Boolean = false): MosaicPage {
    if (plan.songCount <= 0 || plan.startIndex !in songs.indices) return MosaicPage(emptyList())
    val page = songs.subList(plan.startIndex, (plan.startIndex + plan.songCount).coerceAtMost(songs.size))
    val random = Random(plan.layoutSeed)
    val compactRows = minOf(rows.coerceIn(3, 8), ceil(page.size / 2.5).toInt().coerceAtLeast(1))
    val specs = if (isUniform || page.size <= 4) regularSpecs(4, page.size)
        else generateMosaicSpecs(4, compactRows, page.size, random)
    return MosaicPage(page.mapIndexed { i, song ->
        val s = specs[i]
        MosaicTile(song, s.x, s.y, s.width, s.height, random.nextInt(24))
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
