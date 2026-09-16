package com.xvox.music.features.home

import org.junit.Assert.*
import org.junit.Test

class FolderPathsTest {
    @Test fun selectedFolderHidesItsDescendants() {
        assertTrue(FolderPaths.contains("/storage/emulated/0/Music/", "/storage/emulated/0/Music/Albums/Live"))
    }
    @Test fun matchingDoesNotExcludeSimilarlyNamedSiblings() {
        assertFalse(FolderPaths.contains("/storage/emulated/0/Music", "/storage/emulated/0/Music2"))
        assertFalse(FolderPaths.isExcluded("/storage/sdcard/Podcasts", "Podcasts", setOf("/storage/emulated/0/Podcasts")))
    }
    @Test fun duplicateSeparatorsAreNormalized() {
        assertEquals("/storage/emulated/0/Music", FolderPaths.normalize("/storage//emulated/0/Music/"))
    }
    @Test fun oldNameExclusionsAreMigratedWithoutBreakingUserPreferences() {
        assertTrue(FolderPaths.isExcluded("/storage/emulated/0/Voice", "Voice", setOf("Voice")))
        assertFalse(FolderPaths.isExcluded("/storage/emulated/0/Music", "Music", setOf("Voice")))
    }
}
