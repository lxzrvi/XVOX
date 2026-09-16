package com.xvox.music.player.playback

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.xvox.music.core.model.Song
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.data.preferences.XvoxLibraryPreferences
import com.xvox.music.features.home.HomeLibraryFilterHelper
import com.xvox.music.features.home.LibraryFilterConfig
import com.xvox.music.media.MediaStoreSongRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

data class ResumeLibrary(val songs: List<Song>, val startIndex: Int)

/** Shared by headset auto-play and widget play. It never ignores the user's library exclusions. */
object PlaybackLibraryLoader {
    suspend fun load(context: Context): ResumeLibrary? = withContext(Dispatchers.IO) {
        val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) return@withContext null
        val prefs = UserPreferencesRepository(context)
        try {
            val raw = MediaStoreSongRepository(context).loadSongs()
            val library = XvoxLibraryPreferences(context)
            val config = LibraryFilterConfig(prefs.sortOrder.first(), prefs.ignoreBelowSec.first(), prefs.ignoreBelowKb.first(), prefs.ignoredFolders.first())
            val eligible = HomeLibraryFilterHelper.filterAndSort(raw, library.hiddenSongIds.first(), config, 0L)
            if (eligible.isEmpty()) return@withContext null
            val byId = eligible.associateBy { it.id }
            // Resume the app's existing source/queue when available rather than silently replacing a playlist.
            val retained = PlaybackController.activeInstance?.currentQueue().orEmpty().mapNotNull { byId[it.id] }
            val songs = retained.ifEmpty { eligible }
            val last = prefs.lastPlayedSongId.first()
            ResumeLibrary(songs, songs.indexOfFirst { it.id == last }.coerceAtLeast(0))
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled } catch (_: Exception) { null }
    }
}
