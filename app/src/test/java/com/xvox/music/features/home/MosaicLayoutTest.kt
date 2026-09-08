package com.xvox.music.features.home

import com.xvox.music.features.home.allsongs.generateClassicMosaicSpecs
import com.xvox.music.features.home.allsongs.generateMosaicSpecs
import com.xvox.music.features.home.allsongs.mosaicPageRows
import com.xvox.music.features.home.allsongs.mosaicRowsForPage
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class MosaicLayoutTest {
    @Test fun mosaicTwoKeepsWideCardsAndFillsAllRows() {
        for (rows in 3..8) repeat(30) { seed ->
            val tiles = generateMosaicSpecs(4, rows, rows * 2, Random(seed))
            assertTrue(tiles.count { it.width >= it.height * 1.3f } >= 2)
            assertEquals((rows * 4).toFloat(), tiles.sumOf { (it.width * it.height).toDouble() }.toFloat(), .0001f)
            assertEquals(rows.toFloat(), tiles.maxOf { it.y + it.height }, .0001f)
        }
    }

    /** Mosaic 1 now speaks a wider shape language: long, short and wide, not just four presets. */
    @Test fun classicMosaicUsesLongShortAndWideShapes() {
        val seen = mutableSetOf<Pair<Float, Float>>()
        repeat(120) { seed ->
            generateClassicMosaicSpecs(4, 5, 13, Random(seed)).forEach { seen.add(it.width to it.height) }
        }
        assertTrue("Only ${seen.size} shapes: $seen", seen.size >= 6)
        assertTrue("No wide tiles", seen.any { it.first >= 3f && it.second == 1f })
        assertTrue("No tall tiles", seen.any { it.second >= 2f && it.first == 1f })
        assertTrue("No large tiles", seen.any { it.first >= 2f && it.second >= 2f })
    }

    /** The mosaic rule: a page is never allowed to leave a hole, on any page including the last. */
    @Test fun everyMosaicOnePageIsFullyCoveredExactlyOnce() {
        for (rows in 1..8) for (count in 1..rows * 4) repeat(4) { seed ->
            val tiles = generateClassicMosaicSpecs(4, rows, count, Random(seed * 31 + count))
            assertEquals(count, tiles.size)
            val covered = Array(rows) { IntArray(4) }
            tiles.forEach { t ->
                assertTrue(t.x >= 0 && t.y >= 0 && t.x + t.width <= 4 && t.y + t.height <= rows)
                for (y in t.y.toInt() until (t.y + t.height).toInt())
                    for (x in t.x.toInt() until (t.x + t.width).toInt()) covered[y][x]++
            }
            covered.forEach { row -> row.forEach { assertEquals(1, it) } }
        }
    }

    @Test fun generatedPagesHaveNoOverlapsGapsOrMissingTiles() {
        for (rows in 1..8) for (cols in 1..6) for (count in 1..rows * cols) repeat(8) { seed ->
            val tiles = generateMosaicSpecs(cols, rows, count, Random(seed))
            assertEquals(count, tiles.size)
            val covered = Array(rows) { IntArray(cols) }
            tiles.forEach { t ->
                assertTrue(t.width >= 1 && t.height >= 1)
                assertTrue(t.x >= 0 && t.y >= 0 && t.x + t.width <= cols && t.y + t.height <= rows)
                for (y in t.y.toInt() until (t.y + t.height).toInt())
                    for (x in t.x.toInt() until (t.x + t.width).toInt()) covered[y][x]++
            }
            covered.forEach { row -> row.forEach { assertEquals(1, it) } }
        }
    }

    /** A page never claims more rows than it can fill, and a paged page always claims them all. */
    @Test fun pageRowsNeverLeaveAnEmptyBand() {
        for (requested in 3..8) for (count in 1..requested * 4) {
            val flowing = mosaicPageRows(count, requested)
            assertTrue(flowing in 1..requested)
            assertTrue("$count tiles cannot fit in 4 x $flowing", count <= 4 * flowing)
            val paged = mosaicRowsForPage(count, requested, fill = true)
            if (count >= requested) assertEquals(requested, paged)
            assertTrue(count <= 4 * paged)
        }
    }

    @Test fun varietyIsNotLimitedToSixPresets() {
        val layouts = (0..399).map { generateMosaicSpecs(4, 6, 12, Random(it)) }.toSet()
        assertTrue("Only ${layouts.size} layouts", layouts.size > 300)
    }

    @Test fun identicalSeedGivesStableLayoutWhileBrowsing() {
        assertEquals(generateMosaicSpecs(4, 4, 9, Random(42)), generateMosaicSpecs(4, 4, 9, Random(42)))
    }

    @Test fun emptyInputsAreSafe() {
        assertTrue(generateMosaicSpecs(4, 4, 0, Random(1)).isEmpty())
        assertTrue(generateMosaicSpecs(0, 4, 10, Random(1)).isEmpty())
    }
}
