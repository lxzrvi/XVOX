package com.xvox.music.features.home

/** Persisted presentation only. It must never rebuild the playback queue or re-scan MediaStore. */
data class HomePresentation(
    val style: String = "mosaic1",
    val direction: String = "horizontal",
    val rows: Int = 4,
    val hideRecents: Boolean = false,
    val recentsPlacement: String = "bottom",
    val merge: Boolean = false,
    val order: List<String> = HomeSections.defaultOrder,
    val hidden: Set<String> = emptySet(),
    val playlistStyle: String = "long",
    val hideSplit: Boolean = false,
    /** Long playlist card height in dp; 0 keeps the original proportional height. */
    val playlistLongHeight: Int = 0,
    /** "horizontal" = full-width cards on a sideways row; "vertical" = stacked cards. */
    val playlistCardOrientation: String = "vertical"
)

object HomeSections {
    const val ALL = "all"
    const val RECENT = "recent"
    const val LIKED = "liked"
    const val SPLIT = "split"
    const val PLAYLISTS = "playlists"
    // XvoxSplit no longer ships as a Home section; the constant stays only so old stored
    // section orders (which may mention "split") are silently cleaned out by [normalize].
    val defaultOrder = listOf(ALL, RECENT, LIKED, PLAYLISTS)
    fun label(id: String): String = when (id) {
        ALL -> "All Songs"
        RECENT -> "Recently Played"
        LIKED -> "Liked Songs"
        SPLIT -> "XvoxSplit"
        else -> "Playlists"
    }
    fun normalize(order: List<String>): List<String> =
        (order.filter { it in defaultOrder } + defaultOrder).distinct()
    fun visible(config: HomePresentation): List<String> {
        val order = if (config.merge) normalize(config.order) else
            if (config.recentsPlacement == "top") listOf(RECENT, ALL) else listOf(ALL, RECENT)
        return order.filterNot { (it == SPLIT && config.hideSplit) || (it == RECENT && config.hideRecents) || (config.merge && it in config.hidden) }
    }
    fun placeRecent(order: List<String>, placement: String): List<String> {
        val result = normalize(order).filterNot { it == RECENT }.toMutableList()
        val index = result.indexOf(ALL) + if (placement == "top") 0 else 1
        result.add(index, RECENT)
        return result
    }
}

fun normalizeHomeStyle(value: String?): String = when (value) {
    "uniform" -> "uniform"
    "mosaic2" -> "mosaic2"
    else -> "mosaic1" // Legacy "mosaic" returns to the original layout.
}
