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

private data class Spec(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

private val processMosaicSeed = System.currentTimeMillis() xor (System.nanoTime().shl(16))

fun buildMosaicPagePlans(songs: List<Song>, fourRows: Boolean = true): List<XvoxMosaicPagePlan> {
    if (songs.isEmpty()) return emptyList()

    val seed = songs.fold(processMosaicSeed) { value, song -> value * 31L + song.id }
    val random = Random(seed)
    val plans = ArrayList<XvoxMosaicPagePlan>(songs.size / 10 + 1)
    var index = 0

    val maxPerPage = if (fourRows) 16 else 12

    while (index < songs.size) {
        val remaining = songs.size - index
        val count = when {
            remaining <= maxPerPage -> remaining
            fourRows -> listOf(12, 13, 14, 15, 16).random(random)
            else -> listOf(9, 10, 11, 12).random(random)
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

fun buildMosaicPage(songs: List<Song>, plan: XvoxMosaicPagePlan, fourRows: Boolean = true): MosaicPage {
    if (plan.songCount <= 0 || plan.startIndex !in songs.indices) {
        return MosaicPage(emptyList())
    }

    val end = (plan.startIndex + plan.songCount).coerceAtMost(songs.size)
    val pageSongs = songs.subList(plan.startIndex, end)
    val random = Random(plan.layoutSeed)
    val specs = if (fourRows) specsFor4Rows(pageSongs.size, random) else specsFor3Rows(pageSongs.size, random)

    require(specs.size == pageSongs.size)

    return MosaicPage(
        pageSongs.mapIndexed { index, song ->
            val spec = specs[index]
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

private fun specsFor4Rows(count: Int, random: Random): List<Spec> = when (count) {
    1 -> listOf(Spec(0f, 0f, 4f, 4f))
    2 -> listOf(Spec(0f, 0f, 4f, 2f), Spec(0f, 2f, 4f, 2f))
    3 -> listOf(Spec(0f, 0f, 4f, 2f), Spec(0f, 2f, 2f, 2f), Spec(2f, 2f, 2f, 2f))
    4 -> listOf(
        Spec(0f, 0f, 2f, 2f), Spec(2f, 0f, 2f, 2f),
        Spec(0f, 2f, 2f, 2f), Spec(2f, 2f, 2f, 2f)
    )
    5 -> listOf(
        Spec(0f, 0f, 4f, 2f),
        Spec(0f, 2f, 2f, 1f), Spec(2f, 2f, 2f, 1f),
        Spec(0f, 3f, 2f, 1f), Spec(2f, 3f, 2f, 1f)
    )
    6 -> listOf(
        Spec(0f, 0f, 2f, 2f), Spec(2f, 0f, 2f, 2f),
        Spec(0f, 2f, 2f, 1f), Spec(2f, 2f, 2f, 1f),
        Spec(0f, 3f, 2f, 1f), Spec(2f, 3f, 2f, 1f)
    )
    7 -> listOf(
        Spec(0f, 0f, 2f, 2f), Spec(2f, 0f, 2f, 1f), Spec(2f, 1f, 2f, 1f),
        Spec(0f, 2f, 1f, 1f), Spec(1f, 2f, 1f, 1f), Spec(2f, 2f, 2f, 1f),
        Spec(0f, 3f, 4f, 1f)
    )
    8 -> listOf(
        Spec(0f, 0f, 2f, 1f), Spec(2f, 0f, 2f, 1f),
        Spec(0f, 1f, 2f, 1f), Spec(2f, 1f, 2f, 1f),
        Spec(0f, 2f, 2f, 1f), Spec(2f, 2f, 2f, 1f),
        Spec(0f, 3f, 2f, 1f), Spec(2f, 3f, 2f, 1f)
    )
    9 -> listOf(
        Spec(0f, 0f, 2f, 2f), Spec(2f, 0f, 2f, 1f), Spec(2f, 1f, 2f, 1f),
        Spec(0f, 2f, 1f, 1f), Spec(1f, 2f, 1f, 1f), Spec(2f, 2f, 1f, 1f), Spec(3f, 2f, 1f, 1f),
        Spec(0f, 3f, 2f, 1f), Spec(2f, 3f, 2f, 1f)
    )
    10 -> listOf(
        Spec(0f, 0f, 2f, 1f), Spec(2f, 0f, 2f, 1f),
        Spec(0f, 1f, 1f, 1f), Spec(1f, 1f, 1f, 1f), Spec(2f, 1f, 1f, 1f), Spec(3f, 1f, 1f, 1f),
        Spec(0f, 2f, 1f, 1f), Spec(1f, 2f, 1f, 1f), Spec(2f, 2f, 2f, 1f),
        Spec(0f, 3f, 4f, 1f)
    )
    11 -> listOf(
        Spec(0f, 0f, 2f, 1f), Spec(2f, 0f, 2f, 1f),
        Spec(0f, 1f, 1f, 1f), Spec(1f, 1f, 1f, 1f), Spec(2f, 1f, 1f, 1f), Spec(3f, 1f, 1f, 1f),
        Spec(0f, 2f, 1f, 1f), Spec(1f, 2f, 1f, 1f), Spec(2f, 2f, 1f, 1f), Spec(3f, 2f, 1f, 1f),
        Spec(0f, 3f, 4f, 1f)
    )
    12 -> listOf(
        Spec(0f, 0f, 2f, 1f), Spec(2f, 0f, 2f, 1f),
        Spec(0f, 1f, 1f, 1f), Spec(1f, 1f, 1f, 1f), Spec(2f, 1f, 1f, 1f), Spec(3f, 1f, 1f, 1f),
        Spec(0f, 2f, 1f, 1f), Spec(1f, 2f, 1f, 1f), Spec(2f, 2f, 1f, 1f), Spec(3f, 2f, 1f, 1f),
        Spec(0f, 3f, 2f, 1f), Spec(2f, 3f, 2f, 1f)
    )
    13 -> listOf(
        Spec(0f, 0f, 2f, 1f), Spec(2f, 0f, 1f, 1f), Spec(3f, 0f, 1f, 1f),
        Spec(0f, 1f, 1f, 1f), Spec(1f, 1f, 1f, 1f), Spec(2f, 1f, 1f, 1f), Spec(3f, 1f, 1f, 1f),
        Spec(0f, 2f, 1f, 1f), Spec(1f, 2f, 1f, 1f), Spec(2f, 2f, 1f, 1f), Spec(3f, 2f, 1f, 1f),
        Spec(0f, 3f, 2f, 1f), Spec(2f, 3f, 2f, 1f)
    )
    14 -> listOf(
        Spec(0f, 0f, 2f, 1f), Spec(2f, 0f, 1f, 1f), Spec(3f, 0f, 1f, 1f),
        Spec(0f, 1f, 1f, 1f), Spec(1f, 1f, 1f, 1f), Spec(2f, 1f, 1f, 1f), Spec(3f, 1f, 1f, 1f),
        Spec(0f, 2f, 1f, 1f), Spec(1f, 2f, 1f, 1f), Spec(2f, 2f, 1f, 1f), Spec(3f, 2f, 1f, 1f),
        Spec(0f, 3f, 1f, 1f), Spec(1f, 3f, 1f, 1f), Spec(2f, 3f, 2f, 1f)
    )
    15 -> listOf(
        Spec(0f, 0f, 2f, 1f), Spec(2f, 0f, 1f, 1f), Spec(3f, 0f, 1f, 1f),
        Spec(0f, 1f, 1f, 1f), Spec(1f, 1f, 1f, 1f), Spec(2f, 1f, 1f, 1f), Spec(3f, 1f, 1f, 1f),
        Spec(0f, 2f, 1f, 1f), Spec(1f, 2f, 1f, 1f), Spec(2f, 2f, 1f, 1f), Spec(3f, 2f, 1f, 1f),
        Spec(0f, 3f, 1f, 1f), Spec(1f, 3f, 1f, 1f), Spec(2f, 3f, 1f, 1f), Spec(3f, 3f, 1f, 1f)
    )
    else -> regularSpecs(count)
}

private fun specsFor3Rows(count: Int, random: Random): List<Spec> = when (count) {
    1 -> listOf(Spec(0f, 0f, 4f, 3f))
    2 -> listOf(Spec(0f, 0f, 4f, 1.5f), Spec(0f, 1.5f, 4f, 1.5f))
    3 -> if (random.nextBoolean()) {
        listOf(Spec(0f, 0f, 4f, 1.5f), Spec(0f, 1.5f, 2f, 1.5f), Spec(2f, 1.5f, 2f, 1.5f))
    } else {
        listOf(Spec(0f, 0f, 2f, 1.5f), Spec(2f, 0f, 2f, 1.5f), Spec(0f, 1.5f, 4f, 1.5f))
    }
    4 -> listOf(
        Spec(0f, 0f, 2f, 1.5f), Spec(2f, 0f, 2f, 1.5f),
        Spec(0f, 1.5f, 2f, 1.5f), Spec(2f, 1.5f, 2f, 1.5f)
    )
    5 -> listOf(
        Spec(0f, 0f, 4f, 1f),
        Spec(0f, 1f, 2f, 1f), Spec(2f, 1f, 2f, 1f),
        Spec(0f, 2f, 2f, 1f), Spec(2f, 2f, 2f, 1f)
    )
    6 -> listOf(
        Spec(0f, 0f, 2f, 1f), Spec(2f, 0f, 2f, 1f),
        Spec(0f, 1f, 2f, 1f), Spec(2f, 1f, 2f, 1f),
        Spec(0f, 2f, 2f, 1f), Spec(2f, 2f, 2f, 1f)
    )
    7 -> listOf(
        Spec(0f, 0f, 4f, 1f),
        Spec(0f, 1f, 1f, 1f), Spec(1f, 1f, 1f, 1f), Spec(2f, 1f, 1f, 1f), Spec(3f, 1f, 1f, 1f),
        Spec(0f, 2f, 2f, 1f), Spec(2f, 2f, 2f, 1f)
    )
    8 -> listOf(
        Spec(0f, 0f, 1f, 1.5f), Spec(1f, 0f, 1f, 1.5f), Spec(2f, 0f, 1f, 1.5f), Spec(3f, 0f, 1f, 1.5f),
        Spec(0f, 1.5f, 1f, 1.5f), Spec(1f, 1.5f, 1f, 1.5f), Spec(2f, 1.5f, 1f, 1.5f), Spec(3f, 1.5f, 1f, 1.5f)
    )
    9 -> listOf(
        Spec(0f, 0f, 2f, 1f), Spec(2f, 0f, 2f, 1f),
        Spec(0f, 1f, 1f, 1f), Spec(1f, 1f, 1f, 1f), Spec(2f, 1f, 1f, 1f), Spec(3f, 1f, 1f, 1f),
        Spec(0f, 2f, 1f, 1f), Spec(1f, 2f, 1f, 1f), Spec(2f, 2f, 2f, 1f)
    )
    10 -> listOf(
        Spec(0f, 0f, 2f, 1f), Spec(2f, 0f, 1f, 1f), Spec(3f, 0f, 1f, 1f),
        Spec(0f, 1f, 1f, 1f), Spec(1f, 1f, 1f, 1f), Spec(2f, 1f, 1f, 1f), Spec(3f, 1f, 1f, 1f),
        Spec(0f, 2f, 1f, 1f), Spec(1f, 2f, 1f, 1f), Spec(2f, 2f, 2f, 1f)
    )
    11 -> listOf(
        Spec(0f, 0f, 2f, 1f), Spec(2f, 0f, 2f, 1f),
        Spec(0f, 1f, 1f, 1f), Spec(1f, 1f, 1f, 1f), Spec(2f, 1f, 1f, 1f), Spec(3f, 1f, 1f, 1f),
        Spec(0f, 2f, 1f, 1f), Spec(1f, 2f, 1f, 1f), Spec(2f, 2f, 1f, 1f), Spec(3f, 2f, 1f, 1f)
    )
    else -> regularSpecs(count)
}

private fun regularSpecs(count: Int): List<Spec> = List(count) { index ->
    Spec(
        x = (index % 4).toFloat(),
        y = (index / 4).toFloat(),
        width = 1f,
        height = 1f
    )
}
