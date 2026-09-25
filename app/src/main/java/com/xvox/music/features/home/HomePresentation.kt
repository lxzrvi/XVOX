package com.xvox.music.features.home

/** Persisted presentation only. It must never rebuild the playback queue or re-scan MediaStore. */
data class HomePresentation(
    val style: String = "uniform",
    val direction: String = "vertical",
    val rows: Int = 4,
    val hideRecents: Boolean = false,
    val recentsPlacement: String = "bottom",
    val merge: Boolean = false,
    val mergedSections: Set<String> = emptySet(),
    val order: List<String> = HomeSections.defaultOrder,
    val hidden: Set<String> = emptySet(),
    val playlistStyle: String = "long",
    val hideSplit: Boolean = false,
    /** Long playlist card height in dp; 0 keeps the original proportional height. */
    val playlistLongHeight: Int = 0,
    /** "horizontal" = full-width cards on a sideways row; "vertical" = stacked cards. */
    val playlistCardOrientation: String = "vertical",
    val playlistRows: Int = 2,
    val artistColumns: Int = 5,
    val artistGap: Int = 8,
    val artistRows: Int = 4,
    val artistHideText: Boolean = false,
    val artistDirection: String = "vertical"
)

object HomeSections {
    const val ALL = "all"
    const val RECENT = "recent"
    const val ARTISTS = "artists"
    const val LIKED = "liked"
    const val SPLIT = "split"
    const val PLAYLISTS = "playlists"

    // Recent history is a dedicated shell-header destination rather than a Home-feed section.
    val defaultOrder = listOf(ALL, ARTISTS, LIKED, PLAYLISTS)

    fun label(id: String): String = when (id) {
        ALL -> "All Songs"
        RECENT -> "Recently Played"
        ARTISTS -> "Artists"
        LIKED -> "Liked Songs"
        SPLIT -> "XvoxSplit"
        else -> "Playlists"
    }

    fun normalize(order: List<String>): List<String> =
        (order.filter { it in defaultOrder } + defaultOrder).distinct()

    /** The Home feed always begins with All Songs; Recent now lives in the shell header. */
    fun visible(@Suppress("UNUSED_PARAMETER") config: HomePresentation): List<String> = listOf(ALL)

    /**
     * Retained as a harmless migration shim for older preference callers. It normalizes legacy
     * orders while deliberately dropping the former Recent feed slot.
     */
    fun placeRecent(order: List<String>, @Suppress("UNUSED_PARAMETER") placement: String): List<String> =
        normalize(order).filterNot { it == RECENT }
}

fun normalizeHomeStyle(value: String?): String = when (value) {
    "mosaic1" -> "mosaic1"
    "mosaic2" -> "mosaic2"
    else -> "uniform" // "uniform" is the Default layout
}
