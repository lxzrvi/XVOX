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

private val processMosaicSeed =
    System.nanoTime() xor
        Runtime.getRuntime()
            .freeMemory()

fun buildMosaicPages(
    songs: List<Song>
): List<MosaicPage> {
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

    val pages =
        mutableListOf<MosaicPage>()

    var index = 0

    while (index < songs.size) {
        val remaining =
            songs.size - index

        if (remaining <= 12) {
            pages +=
                finalPage(
                    songs =
                        songs.subList(
                            index,
                            songs.size
                        ),
                    random =
                        random
                )

            break
        }

        val count =
            if (remaining <= 16) {
                12
            } else {
                listOf(
                    9,
                    10,
                    11,
                    12
                ).random(random)
            }

        val pageSongs =
            songs.subList(
                index,
                index + count
            )

        pages +=
            integerPage(
                songs =
                    pageSongs,
                random =
                    random
            )

        index += count
    }

    return pages
}

private fun integerPage(
    songs: List<Song>,
    random: Random
): MosaicPage =
    when (songs.size) {
        9 ->
            ninePage(
                songs,
                random.nextInt(4)
            )

        10 ->
            tenPage(
                songs,
                random.nextInt(4)
            )

        11 ->
            elevenPage(
                songs,
                random.nextInt(6)
            )

        12 ->
            regularPage(
                songs
            )

        else ->
            finalPage(
                songs,
                random
            )
    }

private fun ninePage(
    songs: List<Song>,
    variant: Int
): MosaicPage {
    val specs =
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

    return fromSpecs(
        songs,
        specs
    )
}

private fun tenPage(
    songs: List<Song>,
    variant: Int
): MosaicPage {
    val specs =
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

    return fromSpecs(
        songs,
        specs
    )
}

private fun elevenPage(
    songs: List<Song>,
    variant: Int
): MosaicPage {
    val wideRow =
        when (variant) {
            0, 1 -> 0
            2, 3 -> 1
            else -> 2
        }

    val wideColumn =
        if (variant % 2 == 0) {
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
                row == wideRow &&
                column == wideColumn
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
                song = song,
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
    )

private fun finalPage(
    songs: List<Song>,
    random: Random
): MosaicPage {
    if (songs.size in 9..12) {
        return when (songs.size) {
            9 ->
                ninePage(
                    songs,
                    random.nextInt(4)
                )

            10 ->
                tenPage(
                    songs,
                    random.nextInt(4)
                )

            11 ->
                elevenPage(
                    songs,
                    random.nextInt(6)
                )

            else ->
                regularPage(songs)
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
                when (
                    random.nextInt(3)
                ) {
                    0 ->
                        listOf(
                            Spec(0f, 0f, 4f, 1f),
                            Spec(0f, 1f, 1f, 1f),
                            Spec(1f, 1f, 1f, 1f),
                            Spec(2f, 1f, 1f, 1f),
                            Spec(3f, 1f, 1f, 1f),
                            Spec(0f, 2f, 2f, 1f),
                            Spec(2f, 2f, 2f, 1f)
                        )

                    1 ->
                        listOf(
                            Spec(0f, 0f, 1f, 1f),
                            Spec(1f, 0f, 1f, 1f),
                            Spec(2f, 0f, 1f, 1f),
                            Spec(3f, 0f, 1f, 1f),
                            Spec(0f, 1f, 4f, 1f),
                            Spec(0f, 2f, 2f, 1f),
                            Spec(2f, 2f, 2f, 1f)
                        )

                    else ->
                        listOf(
                            Spec(0f, 0f, 1f, 1f),
                            Spec(1f, 0f, 1f, 1f),
                            Spec(2f, 0f, 1f, 1f),
                            Spec(3f, 0f, 1f, 1f),
                            Spec(0f, 1f, 2f, 1f),
                            Spec(2f, 1f, 2f, 1f),
                            Spec(0f, 2f, 4f, 1f)
                        )
                }

            8 ->
                listOf(
                    Spec(
                        0f,
                        0f,
                        1f,
                        1.5f
                    ),
                    Spec(
                        1f,
                        0f,
                        1f,
                        1.5f
                    ),
                    Spec(
                        2f,
                        0f,
                        1f,
                        1.5f
                    ),
                    Spec(
                        3f,
                        0f,
                        1f,
                        1.5f
                    ),
                    Spec(
                        0f,
                        1.5f,
                        1f,
                        1.5f
                    ),
                    Spec(
                        1f,
                        1.5f,
                        1f,
                        1.5f
                    ),
                    Spec(
                        2f,
                        1.5f,
                        1f,
                        1.5f
                    ),
                    Spec(
                        3f,
                        1.5f,
                        1f,
                        1.5f
                    )
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
        songs.size ==
            specs.size
    )

    return MosaicPage(
        songs.zip(specs)
            .map {
                (song, spec) ->

                tile(
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
