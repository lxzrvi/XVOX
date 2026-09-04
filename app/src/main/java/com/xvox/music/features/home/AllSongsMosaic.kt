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

private sealed interface MosaicSlot {
    data class Cell(
        val x: Int,
        val y: Int
    ) : MosaicSlot

    data class Wide(
        val x: Int,
        val y: Int,
        val width: Int
    ) : MosaicSlot
}

private val processSeed =
    System.nanoTime()

fun buildMosaicPages(
    songs: List<Song>
): List<MosaicPage> {
    if (songs.isEmpty()) {
        return emptyList()
    }

    val random =
        Random(
            processSeed xor
                songs.fold(0L) {
                    seed,
                    song ->
                    seed * 31L + song.id
                }
        )

    val fullCount =
        songs.size / 12

    val remainder =
        songs.size % 12

    val pages =
        mutableListOf<MosaicPage>()

    repeat(fullCount) { pageIndex ->
        val pageSongs =
            songs.subList(
                pageIndex * 12,
                pageIndex * 12 + 12
            )

        pages +=
            if (
                random.nextFloat() <
                0.58f
            ) {
                randomIntegerPage(
                    pageSongs,
                    random
                )
            } else {
                regularPage(
                    pageSongs
                )
            }
    }

    if (remainder > 0) {
        pages +=
            remainderPage(
                songs.takeLast(
                    remainder
                )
            )
    }

    return pages
}

private fun randomIntegerPage(
    songs: List<Song>,
    random: Random
): MosaicPage {
    val layouts =
        listOf(
            listOf(
                MosaicSlot.Wide(0, 0, 2),
                MosaicSlot.Cell(2, 0),
                MosaicSlot.Cell(3, 0),
                MosaicSlot.Cell(0, 1),
                MosaicSlot.Cell(1, 1),
                MosaicSlot.Cell(2, 1),
                MosaicSlot.Cell(3, 1),
                MosaicSlot.Cell(0, 2),
                MosaicSlot.Cell(1, 2),
                MosaicSlot.Cell(2, 2),
                MosaicSlot.Cell(3, 2)
            ),
            listOf(
                MosaicSlot.Cell(0, 0),
                MosaicSlot.Cell(1, 0),
                MosaicSlot.Wide(2, 0, 2),
                MosaicSlot.Cell(0, 1),
                MosaicSlot.Cell(1, 1),
                MosaicSlot.Cell(2, 1),
                MosaicSlot.Cell(3, 1),
                MosaicSlot.Cell(0, 2),
                MosaicSlot.Cell(1, 2),
                MosaicSlot.Cell(2, 2),
                MosaicSlot.Cell(3, 2)
            ),
            listOf(
                MosaicSlot.Cell(0, 0),
                MosaicSlot.Cell(1, 0),
                MosaicSlot.Cell(2, 0),
                MosaicSlot.Cell(3, 0),
                MosaicSlot.Wide(0, 1, 2),
                MosaicSlot.Wide(2, 1, 2),
                MosaicSlot.Cell(0, 2),
                MosaicSlot.Cell(1, 2),
                MosaicSlot.Cell(2, 2),
                MosaicSlot.Cell(3, 2)
            ),
            listOf(
                MosaicSlot.Cell(0, 0),
                MosaicSlot.Cell(1, 0),
                MosaicSlot.Cell(2, 0),
                MosaicSlot.Cell(3, 0),
                MosaicSlot.Cell(0, 1),
                MosaicSlot.Cell(1, 1),
                MosaicSlot.Cell(2, 1),
                MosaicSlot.Cell(3, 1),
                MosaicSlot.Wide(0, 2, 2),
                MosaicSlot.Wide(2, 2, 2)
            )
        )

    val slots =
        layouts.random(random)

    val required =
        slots.sumOf {
            when (it) {
                is MosaicSlot.Cell ->
                    1

                is MosaicSlot.Wide ->
                    it.width
            }
        }

    if (
        required != 12 ||
        songs.size != 12
    ) {
        return regularPage(songs)
    }

    var songIndex = 0

    val tiles =
        mutableListOf<MosaicTile>()

    slots.forEach { slot ->
        when (slot) {
            is MosaicSlot.Cell -> {
                tiles +=
                    tile(
                        songs[songIndex++],
                        slot.x.toFloat(),
                        slot.y.toFloat(),
                        1f,
                        1f
                    )
            }

            is MosaicSlot.Wide -> {
                tiles +=
                    tile(
                        songs[songIndex++],
                        slot.x.toFloat(),
                        slot.y.toFloat(),
                        slot.width.toFloat(),
                        1f
                    )

                repeat(
                    slot.width - 1
                ) {
                    songIndex++
                }
            }
        }
    }

    return if (
        songIndex <= songs.size
    ) {
        MosaicPage(tiles)
    } else {
        regularPage(songs)
    }
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
                    (index % 4).toFloat(),
                y =
                    (index / 4).toFloat(),
                width = 1f,
                height = 1f
            )
        }
    )

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
                    tile(songs[0], 0f, 0f, 2f, 1f),
                    tile(songs[1], 2f, 0f, 2f, 1f),
                    tile(songs[2], 0f, 1f, 4f, 1f),
                    tile(songs[3], 0f, 2f, 2f, 1f),
                    tile(songs[4], 2f, 2f, 2f, 1f)
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
                    tile(songs[0], 0f, 0f, 1f, 1f),
                    tile(songs[1], 1f, 0f, 1f, 1f),
                    tile(songs[2], 2f, 0f, 1f, 1f),
                    tile(songs[3], 3f, 0f, 1f, 1f),
                    tile(songs[4], 0f, 1f, 2f, 1f),
                    tile(songs[5], 2f, 1f, 2f, 1f),
                    tile(songs[6], 0f, 2f, 4f, 1f)
                )

            8 ->
                listOf(
                    tile(songs[0], 0f, 0f, 1f, 1f),
                    tile(songs[1], 1f, 0f, 1f, 1f),
                    tile(songs[2], 2f, 0f, 1f, 1f),
                    tile(songs[3], 3f, 0f, 1f, 1f),
                    tile(songs[4], 0f, 1f, 1f, 2f),
                    tile(songs[5], 1f, 1f, 1f, 2f),
                    tile(songs[6], 2f, 1f, 1f, 2f),
                    tile(songs[7], 3f, 1f, 1f, 2f)
                )

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

            11 ->
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
                    tile(songs[9], 2f, 2f, 1f, 1f),
                    tile(songs[10], 3f, 2f, 1f, 1f)
                )

            else ->
                regularPage(songs).tiles
        }

    return MosaicPage(tiles)
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
