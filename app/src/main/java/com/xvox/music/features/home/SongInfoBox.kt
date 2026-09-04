package com.xvox.music.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme

@Composable
fun SongInfoBox(
    info: SongInfo
) {
    val colors = XvoxTheme.colors

    Column(
        modifier =
            Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Song info",
            color = colors.primaryText,
            fontSize = 18.sp,
            fontWeight =
                FontWeight.Bold
        )

        Spacer(
            Modifier.height(12.dp)
        )

        InfoRow(
            "Title",
            info.title
        )
        InfoRow(
            "Artist",
            info.artist
        )
        InfoRow(
            "Album",
            info.album
        )
        InfoRow(
            "Album artist",
            info.albumArtist
        )
        InfoRow(
            "Genre",
            info.genre
        )
        InfoRow(
            "Year",
            info.year
        )
        InfoRow(
            "Duration",
            info.duration
        )
        InfoRow(
            "Format",
            info.format
        )
        InfoRow(
            "Bitrate",
            info.bitrate
        )
        InfoRow(
            "Sample rate",
            info.sampleRate
        )
        InfoRow(
            "Track",
            info.trackNumber
        )
        InfoRow(
            "Location",
            info.location
        )
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    val colors = XvoxTheme.colors

    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(
                14.dp
            )
    ) {
        Text(
            text = label,
            color = colors.secondaryText,
            fontSize = 11.sp,
            modifier =
                Modifier.weight(0.34f)
        )

        Text(
            text = value,
            color = colors.primaryText,
            fontSize = 11.sp,
            maxLines =
                if (
                    label == "Location"
                ) {
                    3
                } else {
                    2
                },
            overflow =
                TextOverflow.Ellipsis,
            modifier =
                Modifier.weight(0.66f)
        )
    }

    Spacer(
        Modifier.height(8.dp)
    )
}
