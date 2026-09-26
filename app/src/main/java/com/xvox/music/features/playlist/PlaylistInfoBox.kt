package com.xvox.music.features.playlist

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.overlay.xvoxBoxScroll
import com.xvox.music.data.preferences.XvoxPlaylist
import java.text.DateFormat
import java.util.Date

@Composable
fun PlaylistInfoBox(
    playlist: XvoxPlaylist,
    songCount: Int
) {
    val colors = XvoxTheme.colors
    val scrollState = rememberScrollState()

    val created = if (playlist.createdAt > 0L) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(playlist.createdAt))
    } else {
        "Unknown"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .xvoxBoxScroll(scrollState)
    ) {
        Text(
            text = playlist.name,
            color = colors.primaryText,
            fontSize = 14.sp
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "$songCount songs",
            color = colors.secondaryText,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Created $created",
            color = colors.secondaryText,
            fontSize = 12.sp
        )
    }
}
