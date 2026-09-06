package com.xvox.music.features.home

import com.xvox.music.core.model.Song
import kotlin.random.Random

data class LibraryFilterConfig(
    val sort: String,
    val sec: Int,
    val kb: Int,
    val ignored: Set<String>
)

object HomeLibraryFilterHelper {

    fun filterAndSort(
        songs: List<Song>,
        hiddenSongIds: Set<Long>,
        config: LibraryFilterConfig,
        randomSeed: Long
    ): List<Song> {
        val minDurationMs = config.sec * 1000L
        val minSizeBytes = config.kb * 1024L

        val filtered = songs.filter { song ->
            song.id !in hiddenSongIds &&
                song.duration >= minDurationMs &&
                (minSizeBytes <= 0L || song.sizeBytes >= minSizeBytes) &&
                song.folderName !in config.ignored
        }

        return when (config.sort) {
            "Z-A" -> filtered.sortedByDescending { it.title.lowercase() }
            "Random" -> filtered.shuffled(Random(randomSeed))
            else -> filtered.sortedBy { it.title.lowercase() }
        }
    }

    fun groupFolders(songs: List<Song>): List<FolderInfo> {
        return songs.groupBy { it.folderName }.map { (name, list) ->
            FolderInfo(name = name, songCount = list.size, songs = list)
        }.sortedBy { it.name.lowercase() }
    }
}
