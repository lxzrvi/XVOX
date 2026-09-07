package com.xvox.music.features.home

import com.xvox.music.features.home.allsongs.generateMosaicSpecs
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class MosaicLayoutTest {
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
