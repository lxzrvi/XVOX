// EXACT CODE REPLACE KARO (poori file) - koi extra kaam nahi. Path: app/src/main/java/com/xvox/music/features/playlist/XvoxHomeLibraryMode.kt
package com.xvox.music.features.playlist

enum class XvoxHomeLibraryMode {
    ALL_SONGS,
    SPLIT,
    LIKED,
    PLAYLISTS,
    ARTISTS,
    /** Recently Played has moved from the Home feed into the shell's upper library pill. */
    RECENT,
    /** Folder-wise albums: each device folder is an album with its own song list. */
    ALBUMS
}
