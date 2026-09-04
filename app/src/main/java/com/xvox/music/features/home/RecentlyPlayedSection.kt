package com.xvox.music.features.home

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
fun RecentlyPlayedSection(
    songs: List<Song>,
    currentSongId: Long?,
    isPlaying: Boolean,
    transition: RecentTransitionRequest,
    onSongClick: (Song) -> Unit,
    onSongOptions: (Song) -> Unit
) {
    if (songs.isEmpty()) {
        return
    }

    val colors =
        XvoxTheme.colors

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    top = 40.dp
                )
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
                        12.dp
                )
        )

        Spacer(
            modifier =
                Modifier.height(
                    8.dp
                )
        )

        RecentCarousel(
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
                onSongOptions
        )
    }
}
