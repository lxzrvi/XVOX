// EXACT CODE REPLACE KARO (poori file) - koi extra kaam nahi. Path: app/src/main/java/com/xvox/music/core/ui/navigation/XvoxDestination.kt
package com.xvox.music.core.ui.navigation

import androidx.annotation.DrawableRes
import com.xvox.music.R

enum class XvoxDestination(
    val label: String
) {
    HOME("Home"),
    SEARCH("Search"),
    SETTINGS("Settings")
}

/** The three tabs of the floating navbar pill. Search is its own circle; Settings opens from the profile menu. */
enum class XvoxNavTab(
    val label: String,
    @DrawableRes val icon: Int
) {
    HOME("Home", R.drawable.ic_xvox_home),
    ARTIST("Artist", R.drawable.ic_xvox_nav_artist),
    ALBUM("Album", R.drawable.ic_xvox_disc),
    PLAYLIST("Playlist", R.drawable.ic_xvox_nav_library)
}
