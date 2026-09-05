package com.xvox.music.features.home.recent

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song

@Composable
fun XvoxRecentlyPlayedSection(
    songs: List<Song>,
    currentSongId: Long?,
    isPlaying: Boolean,
    transition: RecentTransitionRequest,
    onSongClick: (Song) -> Unit,
    onSongOptions: (Song) -> Unit,
) {
    val colors =
        XvoxTheme.colors

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    top = 40.dp,
                ),
    ) {
        Text(
            text =
                "Recently Played",
            color =
                colors.primaryText,
            fontSize = 16.sp,
            lineHeight = 19.sp,
            fontWeight =
                FontWeight.SemiBold,
            modifier =
                Modifier.padding(
                    horizontal =
                        12.dp,
                ),
        )

        Spacer(
            Modifier.height(
                10.dp,
            ),
        )

        // Keep same area height even when empty - XvoxRecentCarousel handles empty with fixed height 122.dp
        // Show "Nothing played yet" centered inside same bounds
        XvoxRecentCarousel(
            songs = songs,
            currentSongId =
            currentSongId,
            isPlaying =
            isPlaying,
            transition =
            transition,
            onSongClick =
            onSongClick,
            onSongOptions =
            onSongOptions,
        )
    }
}
