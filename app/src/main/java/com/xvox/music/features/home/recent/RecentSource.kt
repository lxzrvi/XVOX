package com.xvox.music.features.home.recent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale

/**
 * Where a recently played song came from.
 *
 * Every play records its origin, so the recent card can show a small round source logo in its
 * left corner — Liked, a playlist, All Songs or Search — and say so in the XVOX pill on tap.
 */
object RecentSource {
    const val ALL_SONGS = "All Songs"
    const val LIKED = "Liked Songs"
    const val SPLIT = "XvoxSplit"
    const val RECENT = "Recently Played"

    /** Icon for a source label; anything unrecognised is treated as a playlist. */
    fun icon(source: String?): Int = when (source) {
        null, "", ALL_SONGS, RECENT -> R.drawable.ic_xvox_music_note
        LIKED -> R.drawable.ic_xvox_heart
        SPLIT -> R.drawable.ic_xvox_split
        else -> R.drawable.ic_xvox_playlist
    }

    /** Sentence shown in the XVOX pill. */
    fun describe(source: String?): String = when (source) {
        null, "", ALL_SONGS, RECENT -> "From All Songs"
        LIKED -> "From Liked"
        SPLIT -> "From XvoxSplit"
        else -> "From $source"
    }
}

/** Small round badge: readable, with no border. */
@Composable
fun RecentSourceBadge(source: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(24.dp)
            .background(Color.Black.copy(alpha = 0.55f), CircleShape)
            .xvoxPressScale(pressedScale = 0.88f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(RecentSource.icon(source)),
            contentDescription = RecentSource.describe(source),
            tint = XvoxTheme.colors.primaryAccent,
            modifier = Modifier.size(12.dp)
        )
    }
}
