package com.xvox.music.features.home

import com.xvox.music.core.model.Song

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

fun buildMosaicPages(
    songs: List<Song>
): List<MosaicPage> {
    if (songs.isEmpty()) {
        return emptyList()
    }

    val fullPages =
        songs.size / 12

    val remainder =
        songs.size % 12

    val pages =
        mutableListOf<MosaicPage>()

    repeat(fullPages) {
        page ->

        val pageSongs =
            songs.subList(
                page * 12,
                page * 12 + 12
            )

        pages +=
            regularPage(pageSongs)
    }

    if (remainder > 0) {
        val tail =
            songs.takeLast(remainder)

        pages +=
            remainderPage(tail)
    }

    return pages
}

private fun regularPage(
    songs: List<Song>
): MosaicPage {
    return MosaicPage(
        songs.mapIndexed {
            index,
            song ->

            val column =
                index % 4

            val row =
                index / 4

            MosaicTile(
                song = song,
                x = column.toFloat(),
                y = row.toFloat(),
                width = 1f,
                height = 1f
            )
        }
    )
}

private fun remainderPage(
    songs: List<Song>
): MosaicPage {
    val tiles =
        when (songs.size) {
            1 ->
                listOf(
                    tile(
                        songs[0],
                        0f,
                        0f,
                        4f,
                        3f
                    )
                )

            2 ->
                listOf(
                    tile(
                        songs[0],
                        0f,
                        0f,
                        4f,
                        1.5f
                    ),
                    tile(
                        songs[1],
                        0f,
                        1.5f,
                        4f,
                        1.5f
                    )
                )

            3 ->
                listOf(
                    tile(
                        songs[0],
                        0f,
                        0f,
                        2f,
                        1.5f
                    ),
                    tile(
                        songs[1],
                        2f,
                        0f,
                        2f,
                        1.5f
                    ),
                    tile(
                        songs[2],
                        0f,
                        1.5f,
                        4f,
                        1.5f
                    )
                )

            4 ->
                listOf(
                    tile(
                        songs[0],
                        0f,
                        0f,
                        2f,
                        1.5f
                    ),
                    tile(
                        songs[1],
                        2f,
                        0f,
                        2f,
                        1.5f
                    ),
                    tile(
                        songs[2],
                        0f,
                        1.5f,
                        2f,
                        1.5f
                    ),
                    tile(
                        songs[3],
                        2f,
                        1.5f,
                        2f,
                        1.5f
                    )
                )

            5 ->
                listOf(
                    tile(
                        songs[0],
                        0f,
                        0f,
                        2f,
                        1f
                    ),
                    tile(
                        songs[1],
                        2f,
                        0f,
                        2f,
                        1f
                    ),
                    tile(
                        songs[2],
                        0f,
                        1f,
                        4f,
                        1f
                    ),
                    tile(
                        songs[3],
                        0f,
                        2f,
                        2f,
                        1f
                    ),
                    tile(
                        songs[4],
                        2f,
                        2f,
                        2f,
                        1f
                    )
                )

            6 ->
                songs.mapIndexed {
                    index,
                    song ->

                    tile(
                        song,
                        (index % 2) * 2f,
                        (index / 2).toFloat(),
                        2f,
                        1f
                    )
                }

            7 ->
                listOf(
                    tile(
                        songs[0],
                        0f,
                        0f,
                        1f,
                        1f
                    ),
                    tile(
                        songs[1],
                        1f,
                        0f,
                        1f,
                        1f
                    ),
                    tile(
                        songs[2],
                        2f,
                        0f,
                        1f,
                        1f
                    ),
                    tile(
                        songs[3],
                        3f,
                        0f,
                        1f,
                        1f
                    ),
                    tile(
                        songs[4],
                        0f,
                        1f,
                        2f,
                        1f
                    ),
                    tile(
                        songs[5],
                        2f,
                        1f,
                        2f,
                        1f
                    ),
                    tile(
                        songs[6],
                        0f,
                        2f,
                        4f,
                        1f
                    )
                )

            8 ->
                songs.mapIndexed {
                    index,
                    song ->

                    tile(
                        song,
                        (index % 4).toFloat(),
                        if (index < 4) {
                            0f
                        } else {
                            1.5f
                        },
                        1f,
                        1.5f
                    )
                }

            else ->
                fillNineToEleven(
                    songs
                )
        }

    return MosaicPage(tiles)
}

private fun fillNineToEleven(
    songs: List<Song>
): List<MosaicTile> {
    return when (songs.size) {
        9 ->
            listOf(
                tile(songs[0], 0f, 0f, 2f, 1f),
                tile(songs[1], 2f, 0f, 2f, 1f),
                tile(songs[2], 0f, 1f, 1f, 1f),
                tile(songs[3], 1f, 1f, 1f, 1f),
                tile(songs[4], 2f, 1f, 1f, 1f),
                tile(songs[5], 3f, 1f, 1f, 1f),
                tile(songs[6], 0f, 2f, 1f, 1f),
                tile(songs[7], 1f, 2f, 1f, 1f),
                tile(songs[8], 2f, 2f, 2f, 1f)
            )

        10 ->
            listOf(
                tile(songs[0], 0f, 0f, 2f, 1f),
                tile(songs[1], 2f, 0f, 1f, 1f),
                tile(songs[2], 3f, 0f, 1f, 1f),
                tile(songs[3], 0f, 1f, 1f, 1f),
                tile(songs[4], 1f, 1f, 1f, 1f),
                tile(songs[5], 2f, 1f, 1f, 1f),
                tile(songs[6], 3f, 1f, 1f, 1f),
                tile(songs[7], 0f, 2f, 1f, 1f),
                tile(songs[8], 1f, 2f, 1f, 1f),
                tile(songs[9], 2f, 2f, 2f, 1f)
            )

        else ->
            listOf(
                tile(songs[0], 0f, 0f, 2f, 1f),
                tile(songs[1], 2f, 0f, 1f, 1f),
                tile(songs[2], 3f, 0f, 1f, 1f)
            ) +
                songs.drop(3)
                    .mapIndexed {
                        index,
                        song ->

                        tile(
                            song,
                            (index % 4).toFloat(),
                            1f +
                                (index / 4),
                            1f,
                            1f
                        )
                    }
    }
}

private fun tile(
    song: Song,
    x: Float,
    y: Float,
    width: Float,
    height: Float
): MosaicTile =
    MosaicTile(
        song,
        x,
        y,
        width,
        height
    )
