package com.xvox.music.features.home

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

private val processMosaicSeed =
    System.nanoTime() xor
        System.identityHashCode(
            Any()
        ).toLong()

fun buildMosaicPagePlans(
    songs: List<Song>
): List<MosaicPagePlan> {
    if (songs.isEmpty()) {
        return emptyList()
    }

    val seed =
        songs.fold(
            processMosaicSeed
        ) {
                value,
                song ->

            value * 31L +
                song.id
        }

    val random =
        Random(seed)

    val plans =
        ArrayList<MosaicPagePlan>(
            songs.size / 10 + 1
        )

    var index = 0

    while (index < songs.size) {
        val remaining =
            songs.size - index

        val count =
            when {
                remaining <= 12 ->
                    remaining

                remaining <= 16 ->
                    12

                else ->
                    listOf(
                        9,
                        10,
                        11,
                        12
                    ).random(
                        random
                    )
            }

        plans +=
            MosaicPagePlan(
                startIndex =
                    index,
                songCount =
                    count,
                layoutSeed =
                    random.nextLong()
            )

        index += count
    }

    return plans
}

fun buildMosaicPage(
    songs: List<Song>,
    plan: MosaicPagePlan
): MosaicPage {
    if (
        plan.songCount <= 0 ||
        plan.startIndex !in
        songs.indices
    ) {
        return MosaicPage(
            emptyList()
        )
    }

    val end =
        (
            plan.startIndex +
                plan.songCount
            )
            .coerceAtMost(
                songs.size
            )

    val pageSongs =
        songs.subList(
            plan.startIndex,
            end
        )

    val random =
        Random(
            plan.layoutSeed
        )

    val specs =
        specsFor(
            pageSongs.size,
            random
        )

    require(
        specs.size ==
            pageSongs.size
    )

    return MosaicPage(
        pageSongs.mapIndexed {
                index,
                song ->

            val spec =
                specs[index]

            MosaicTile(
                song = song,
                x = spec.x,
                y = spec.y,
                width =
                    spec.width,
                height =
                    spec.height
            )
        }
    )
}

private fun specsFor(
    count: Int,
    random: Random
): List<Spec> =
    when (count) {
        1 ->
            listOf(
                Spec(
                    0f,
                    0f,
                    4f,
                    3f
                )
            )

        2 ->
            listOf(
                Spec(
                    0f,
                    0f,
                    4f,
                    1.5f
                ),
                Spec(
                    0f,
                    1.5f,
                    4f,
                    1.5f
                )
            )

        3 ->
            if (
                random.nextBoolean()
            ) {
                listOf(
                    Spec(
                        0f,
                        0f,
                        4f,
                        1.5f
                    ),
                    Spec(
                        0f,
                        1.5f,
                        2f,
                        1.5f
                    ),
                    Spec(
                        2f,
                        1.5f,
                        2f,
                        1.5f
                    )
                )
            } else {
                listOf(
                    Spec(
                        0f,
                        0f,
                        2f,
                        1.5f
                    ),
                    Spec(
                        2f,
                        0f,
                        2f,
                        1.5f
                    ),
                    Spec(
                        0f,
                        1.5f,
                        4f,
                        1.5f
                    )
                )
            }

        4 ->
            listOf(
                Spec(
                    0f,
                    0f,
                    2f,
                    1.5f
                ),
                Spec(
                    2f,
                    0f,
                    2f,
                    1.5f
                ),
                Spec(
                    0f,
                    1.5f,
                    2f,
                    1.5f
                ),
                Spec(
                    2f,
                    1.5f,
                    2f,
                    1.5f
                )
            )

        5 ->
            fiveSpecs(
                random
            )

        6 ->
            sixSpecs()

        7 ->
            sevenSpecs(
                random
            )

        8 ->
            eightSpecs()

        9 ->
            nineSpecs(
                random
                    .nextInt(4)
            )

        10 ->
            tenSpecs(
                random
                    .nextInt(4)
            )

        11 ->
            elevenSpecs(
                random
                    .nextInt(6)
            )

        12 ->
            regularSpecs(12)

        else ->
            regularSpecs(
                count
            )
    }

private fun fiveSpecs(
    random: Random
): List<Spec> =
    if (
        random.nextBoolean()
    ) {
        listOf(
            Spec(0f, 0f, 4f, 1f),
            Spec(0f, 1f, 2f, 1f),
            Spec(2f, 1f, 2f, 1f),
            Spec(0f, 2f, 2f, 1f),
            Spec(2f, 2f, 2f, 1f)
        )
    } else {
        listOf(
            Spec(0f, 0f, 2f, 1f),
            Spec(2f, 0f, 2f, 1f),
            Spec(0f, 1f, 4f, 1f),
            Spec(0f, 2f, 2f, 1f),
            Spec(2f, 2f, 2f, 1f)
        )
    }

private fun sixSpecs():
    List<Spec> =
    listOf(
        Spec(0f, 0f, 2f, 1f),
        Spec(2f, 0f, 2f, 1f),
        Spec(0f, 1f, 2f, 1f),
        Spec(2f, 1f, 2f, 1f),
        Spec(0f, 2f, 2f, 1f),
        Spec(2f, 2f, 2f, 1f)
    )

private fun sevenSpecs(
    random: Random
): List<Spec> =
    when (
        random.nextInt(3)
    ) {
        0 ->
            listOf(
                Spec(
                    0f,
                    0f,
                    4f,
                    1f
                ),
                Spec(
                    0f,
                    1f,
                    1f,
                    1f
                ),
                Spec(
                    1f,
                    1f,
                    1f,
                    1f
                ),
                Spec(
                    2f,
                    1f,
                    1f,
                    1f
                ),
                Spec(
                    3f,
                    1f,
                    1f,
                    1f
                ),
                Spec(
                    0f,
                    2f,
                    2f,
                    1f
                ),
                Spec(
                    2f,
                    2f,
                    2f,
                    1f
                )
            )

        1 ->
            listOf(
                Spec(
                    0f,
                    0f,
                    1f,
                    1f
                ),
                Spec(
                    1f,
                    0f,
                    1f,
                    1f
                ),
                Spec(
                    2f,
                    0f,
                    1f,
                    1f
                ),
                Spec(
                    3f,
                    0f,
                    1f,
                    1f
                ),
                Spec(
                    0f,
                    1f,
                    4f,
                    1f
                ),
                Spec(
                    0f,
                    2f,
                    2f,
                    1f
                ),
                Spec(
                    2f,
                    2f,
                    2f,
                    1f
                )
            )

        else ->
            listOf(
                Spec(
                    0f,
                    0f,
                    1f,
                    1f
                ),
                Spec(
                    1f,
                    0f,
                    1f,
                    1f
                ),
                Spec(
                    2f,
                    0f,
                    1f,
                    1f
                ),
                Spec(
                    3f,
                    0f,
                    1f,
                    1f
                ),
                Spec(
                    0f,
                    1f,
                    2f,
                    1f
                ),
                Spec(
                    2f,
                    1f,
                    2f,
                    1f
                ),
                Spec(
                    0f,
                    2f,
                    4f,
                    1f
                )
            )
    }

private fun eightSpecs():
    List<Spec> =
    listOf(
        Spec(0f, 0f, 1f, 1.5f),
        Spec(1f, 0f, 1f, 1.5f),
        Spec(2f, 0f, 1f, 1.5f),
        Spec(3f, 0f, 1f, 1.5f),
        Spec(0f, 1.5f, 1f, 1.5f),
        Spec(1f, 1.5f, 1f, 1.5f),
        Spec(2f, 1.5f, 1f, 1.5f),
        Spec(3f, 1.5f, 1f, 1.5f)
    )

private fun nineSpecs(
    variant: Int
): List<Spec> =
    when (variant) {
        0 ->
            listOf(
                Spec(0f, 0f, 2f, 1f),
                Spec(2f, 0f, 2f, 1f),
                Spec(0f, 1f, 1f, 1f),
                Spec(1f, 1f, 1f, 1f),
                Spec(2f, 1f, 1f, 1f),
                Spec(3f, 1f, 1f, 1f),
                Spec(0f, 2f, 1f, 1f),
                Spec(1f, 2f, 1f, 1f),
                Spec(2f, 2f, 2f, 1f)
            )

        1 ->
            listOf(
                Spec(0f, 0f, 1f, 1f),
                Spec(1f, 0f, 1f, 1f),
                Spec(2f, 0f, 2f, 1f),
                Spec(0f, 1f, 2f, 1f),
                Spec(2f, 1f, 1f, 1f),
                Spec(3f, 1f, 1f, 1f),
                Spec(0f, 2f, 1f, 1f),
                Spec(1f, 2f, 1f, 1f),
                Spec(2f, 2f, 2f, 1f)
            )

        2 ->
            listOf(
                Spec(0f, 0f, 4f, 1f),
                Spec(0f, 1f, 1f, 1f),
                Spec(1f, 1f, 1f, 1f),
                Spec(2f, 1f, 1f, 1f),
                Spec(3f, 1f, 1f, 1f),
                Spec(0f, 2f, 1f, 1f),
                Spec(1f, 2f, 1f, 1f),
                Spec(2f, 2f, 1f, 1f),
                Spec(3f, 2f, 1f, 1f)
            )

        else ->
            listOf(
                Spec(0f, 0f, 1f, 1f),
                Spec(1f, 0f, 1f, 1f),
                Spec(2f, 0f, 1f, 1f),
                Spec(3f, 0f, 1f, 1f),
                Spec(0f, 1f, 1f, 1f),
                Spec(1f, 1f, 1f, 1f),
                Spec(2f, 1f, 1f, 1f),
                Spec(3f, 1f, 1f, 1f),
                Spec(0f, 2f, 4f, 1f)
            )
    }

private fun tenSpecs(
    variant: Int
): List<Spec> =
    when (variant) {
        0 ->
            listOf(
                Spec(0f, 0f, 2f, 1f),
                Spec(2f, 0f, 1f, 1f),
                Spec(3f, 0f, 1f, 1f),
                Spec(0f, 1f, 1f, 1f),
                Spec(1f, 1f, 1f, 1f),
                Spec(2f, 1f, 1f, 1f),
                Spec(3f, 1f, 1f, 1f),
                Spec(0f, 2f, 1f, 1f),
                Spec(1f, 2f, 1f, 1f),
                Spec(2f, 2f, 2f, 1f)
            )

        1 ->
            listOf(
                Spec(0f, 0f, 1f, 1f),
                Spec(1f, 0f, 1f, 1f),
                Spec(2f, 0f, 2f, 1f),
                Spec(0f, 1f, 1f, 1f),
                Spec(1f, 1f, 1f, 1f),
                Spec(2f, 1f, 1f, 1f),
                Spec(3f, 1f, 1f, 1f),
                Spec(0f, 2f, 2f, 1f),
                Spec(2f, 2f, 1f, 1f),
                Spec(3f, 2f, 1f, 1f)
            )

        2 ->
            listOf(
                Spec(0f, 0f, 2f, 1f),
                Spec(2f, 0f, 2f, 1f),
                Spec(0f, 1f, 1f, 1f),
                Spec(1f, 1f, 1f, 1f),
                Spec(2f, 1f, 1f, 1f),
                Spec(3f, 1f, 1f, 1f),
                Spec(0f, 2f, 1f, 1f),
                Spec(1f, 2f, 1f, 1f),
                Spec(2f, 2f, 1f, 1f),
                Spec(3f, 2f, 1f, 1f)
            )

        else ->
            listOf(
                Spec(0f, 0f, 1f, 1f),
                Spec(1f, 0f, 1f, 1f),
                Spec(2f, 0f, 1f, 1f),
                Spec(3f, 0f, 1f, 1f),
                Spec(0f, 1f, 1f, 1f),
                Spec(1f, 1f, 1f, 1f),
                Spec(2f, 1f, 1f, 1f),
                Spec(3f, 1f, 1f, 1f),
                Spec(0f, 2f, 2f, 1f),
                Spec(2f, 2f, 2f, 1f)
            )
    }

private fun elevenSpecs(
    variant: Int
): List<Spec> {
    val wideRow =
        when (variant) {
            0, 1 -> 0
            2, 3 -> 1
            else -> 2
        }

    val wideColumn =
        if (
            variant % 2 == 0
        ) {
            0
        } else {
            2
        }

    val result =
        ArrayList<Spec>(11)

    for (row in 0..2) {
        var column = 0

        while (column < 4) {
            if (
                row == wideRow &&
                column ==
                wideColumn
            ) {
                result +=
                    Spec(
                        column
                            .toFloat(),
                        row
                            .toFloat(),
                        2f,
                        1f
                    )

                column += 2
            } else {
                result +=
                    Spec(
                        column
                            .toFloat(),
                        row
                            .toFloat(),
                        1f,
                        1f
                    )

                column++
            }
        }
    }

    return result
}

private fun regularSpecs(
    count: Int
): List<Spec> =
    List(count) {
        index ->

        Spec(
            x =
                (index % 4)
                    .toFloat(),
            y =
                (index / 4)
                    .toFloat(),
            width = 1f,
            height = 1f
        )
    }
