package com.xvox.music.features.home

import org.junit.Assert.*
import org.junit.Test

class HomePresentationTest {
    @Test fun originalMosaicHasItsOwnPersistedChoice() {
        assertEquals("mosaic1", normalizeHomeStyle("mosaic"))
        assertEquals("mosaic1", normalizeHomeStyle(null))
        assertEquals("mosaic2", normalizeHomeStyle("mosaic2"))
        assertEquals("uniform", normalizeHomeStyle("uniform"))
    }
    @Test fun mergedHomeAddsLikedAndPlaylistsAfterRecentsByDefault() {
        assertEquals(listOf("all", "recent", "liked", "playlists"), HomeSections.visible(HomePresentation(merge = true)))
    }
    @Test fun orderAndVisibilityAreIndependent() {
        val config = HomePresentation(merge = true, order = listOf("playlists", "liked", "all", "recent"), hidden = setOf("liked"))
        assertEquals(listOf("playlists", "all", "recent"), HomeSections.visible(config))
    }
    @Test fun recentsPlacementMovesTheSectionRelativeToAllSongs() {
        assertEquals(listOf("recent", "all", "liked", "playlists"), HomeSections.placeRecent(HomeSections.defaultOrder, "top"))
        assertEquals(listOf("all", "recent", "liked", "playlists"), HomeSections.placeRecent(HomeSections.defaultOrder, "bottom"))
        assertEquals(listOf("all"), HomeSections.visible(HomePresentation(hideRecents = true)))
    }
    @Test fun malformedOrderCannotDuplicateOrLoseSections() {
        val result = HomeSections.normalize(listOf("liked", "liked", "unknown"))
        assertEquals(4, result.size)
        assertEquals(HomeSections.defaultOrder.toSet(), result.toSet())
        assertEquals("liked", result.first())
    }
}
