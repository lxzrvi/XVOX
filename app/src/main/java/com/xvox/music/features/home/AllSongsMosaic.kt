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

private val processSeed =
    System.nanoTime()

fun buildMosaicPages(
    songs: List<Song>
): List<MosaicPage> {
    if (songs.isEmpty()) {
        return emptyList()
    }

    val seed =
        songs.fold(processSeed) {
                value,
                song ->
            value * 31L + song.id
        }

    val random = Random(seed)
    val pages =
        mutableListOf<MosaicPage>()

    var index = 0

    while (index < songs.size) {
        val remaining =
            songs.size - index

        if (remaining <= 12) {
            pages +=
                finalPage(
                    songs.subList(
                        index,
                        songs.size
                    )
                )

            break
        }

        val candidates =
            listOf(9, 10, 11, 12)
                .filter { count ->
                    val after =
                        remaining - count

                    after == 0 ||
                        after >= 1
                }

        val count =
            if (
                remaining <= 16
            ) {
                when (remaining) {
                    13 -> 12
                    14 -> 12
                    15 -> 12
                    16 -> 12
                    else -> 12
                }
            } else {
                candidates.random(random)
            }

        val pageSongs =
            songs.subList(
                index,
                index + count
            )

        pages +=
            integerPage(
                pageSongs,
                random
            )

        index += count
    }

    return pages
}

private fun integerPage(
    songs: List<Song>,
    random: Random
): MosaicPage {
    return when (songs.size) {
        9 ->
            ninePage(
                songs,
                random.nextBoolean()
            )

        10 ->
            tenPage(
                songs,
                random.nextBoolean()
            )

        11 ->
            elevenPage(
                songs,
                random.nextInt(4)
            )

        12 ->
            regularPage(
                songs.shuffled(random)
            )

        else ->
            finalPage(songs)
    }
}

private fun ninePage(
    songs: List<Song>,
    flip: Boolean
): MosaicPage {
    val layout =
        if (!flip) {
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
        } else {
            listOf(
                Spec(0f, 0f, 1f, 1f),
                Spec(1f, 0f, 1f, 1f),
                Spec(2f, 0f, 2f, 1f),
                Spec(0f, 1f, 2f, 1f),
                Spec(2f, 1f, 1f, 1f),
                Spec(3f, 1f, 1f, 1f),
                Spec(0f, 2f, 2f, 1f),
                Spec(2f, 2f, 2f, 1f)
            ) +
                Spec(
                    0f,
                    0f,
                    0f,
                    0f
                )
        }

    return if (!flip) {
        fromSpecs(
            songs,
            layout
        )
    } else {
        fromSpecs(
            songs,
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
        )
    }
}

private fun tenPage(
    songs: List<Song>,
    flip: Boolean
): MosaicPage {
    val specs =
        if (!flip) {
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
        } else {
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
        }

    return fromSpecs(
        songs,
        specs
    )
}

private fun elevenPage(
    songs: List<Song>,
    variant: Int
): MosaicPage {
    val wideX =
        when (variant) {
            0 -> 0
            1 -> 2
            2 -> 0
            else -> 2
        }

    val wideY =
        if (variant < 2) {
            0
        } else {
            2
        }

    val specs =
        mutableListOf<Spec>()

    for (row in 0..2) {
        var column = 0

        while (column < 4) {
            if (
                row == wideY &&
                column == wideX
            ) {
                specs +=
                    Spec(
                        column.toFloat(),
                        row.toFloat(),
                        2f,
                        1f
                    )

                column += 2
            } else {
                specs +=
                    Spec(
                        column.toFloat(),
                        row.toFloat(),
                        1f,
                        1f
                    )

                column++
            }
        }
    }

    return fromSpecs(
        songs,
        specs
    )
}

private fun regularPage(
    songs: List<Song>
): MosaicPage =
    MosaicPage(
        songs.mapIndexed {
                index,
                song ->

            tile(
                song,
                (index % 4).toFloat(),
                (index / 4).toFloat(),
                1f,
                1f
            )
        }
    )

private fun finalPage(
    songs: List<Song>
): MosaicPage {
    if (songs.size in 9..12) {
        return when (songs.size) {
            9 -> ninePage(
                songs,
                false
            )

            10 -> tenPage(
                songs,
                false
            )

            11 -> elevenPage(
                songs,
                0
            )

            else -> regularPage(
                songs
            )
        }
    }

    val specs =
        when (songs.size) {
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
                listOf(
                    Spec(0f, 0f, 2f, 1f),
                    Spec(2f, 0f, 2f, 1f),
                    Spec(0f, 1f, 4f, 1f),
                    Spec(0f, 2f, 2f, 1f),
                    Spec(2f, 2f, 2f, 1f)
                )

            6 ->
                listOf(
                    Spec(0f, 0f, 2f, 1f),
                    Spec(2f, 0f, 2f, 1f),
                    Spec(0f, 1f, 2f, 1f),
                    Spec(2f, 1f, 2f, 1f),
                    Spec(0f, 2f, 2f, 1f),
                    Spec(2f, 2f, 2f, 1f)
                )

            7 ->
                listOf(
                    Spec(0f, 0f, 1f, 1f),
                    Spec(1f, 0f, 1f, 1f),
                    Spec(2f, 0f, 1f, 1f),
                    Spec(3f, 0f, 1f, 1f),
                    Spec(0f, 1f, 2f, 1f),
                    Spec(2f, 1f, 2f, 1f),
                    Spec(0f, 2f, 4f, 1f)
                )

            8 ->
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

            else ->
                emptyList()
        }

    return fromSpecs(
        songs,
        specs
    )
}

private data class Spec(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

private fun fromSpecs(
    songs: List<Song>,
    specs: List<Spec>
): MosaicPage {
    require(
        songs.size == specs.size
    )

    return MosaicPage(
        songs.zip(specs)
            .map {
                (song, spec) ->

                tile(
                    song,
                    spec.x,
                    spec.y,
                    spec.width,
                    spec.height
                )
            }
    )
}

private fun tile(
    song: Song,
    x: Float,
    y: Float,
    width: Float,
    height: Float
): MosaicTile =
    MosaicTile(
        song = song,
        x = x,
        y = y,
        width = width,
        height = height
    )
