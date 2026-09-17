package com.xvox.music.features.home.allsongs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.effects.xvoxSongPress
import com.xvox.music.data.preferences.XvoxAllSongsConfig
import com.xvox.music.features.home.XvoxSongArtwork
import com.xvox.music.features.home.rememberSongCardColor

@Composable
fun XvoxAllSongsSection(
    songs: List<Song>,
    config: XvoxAllSongsConfig,
    currentSongId: Long?,
    isPlaying: Boolean,
    selectedSongIds: Set<Long> = emptySet(),
    onSongClick: (Song) -> Unit,
    onSongLongClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    if (songs.isEmpty()) return
    val isLandscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val effectiveColumns = if (isLandscape) 8 else config.columns.coerceIn(1, 8)

    val layoutMode = if (isLandscape) "mosaic" else config.layoutMode
    val tileShape = config.tileShape
    val gap = config.gapDp.dp

    if (layoutMode == "mosaic") {
        XvoxAllSongsMosaicGrid(
            songs = songs,
            columns = effectiveColumns,
            gap = gap,
            currentSongId = currentSongId,
            isPlaying = isPlaying,
            selectedSongIds = selectedSongIds,
            onSongClick = onSongClick,
            onSongLongClick = onSongLongClick,
            modifier = modifier
        )
    } else {
        XvoxAllSongsStandardGrid(
            songs = songs,
            columns = effectiveColumns,
            gap = gap,
            currentSongId = currentSongId,
            isPlaying = isPlaying,
            selectedSongIds = selectedSongIds,
            onSongClick = onSongClick,
            onSongLongClick = onSongLongClick,
            tileShape = tileShape,
            modifier = modifier
        )
    }
}

@Composable
private fun XvoxAllSongsStandardGrid(
    songs: List<Song>,
    columns: Int,
    gap: androidx.compose.ui.unit.Dp,
    currentSongId: Long?,
    isPlaying: Boolean,
    selectedSongIds: Set<Long>,
    onSongClick: (Song) -> Unit,
    onSongLongClick: (Song) -> Unit,
    tileShape: String,
    modifier: Modifier = Modifier
) {
    val chunked = remember(songs, columns) { songs.chunked(columns) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(gap)
    ) {
        chunked.forEach { rowSongs ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap)
            ) {
                rowSongs.forEach { song ->
                    val isCurrent = song.id == currentSongId
                    val isSelected = song.id in selectedSongIds

                    XvoxAllSongGridItem(
                        song = song,
                        isCurrent = isCurrent,
                        isPlaying = isCurrent && isPlaying,
                        isSelected = isSelected,
                        onClick = { onSongClick(song) },
                        onLongClick = { onSongLongClick(song) },
                        tileShape = tileShape,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (rowSongs.size < columns) {
                    repeat(columns - rowSongs.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun XvoxAllSongsMosaicGrid(
    songs: List<Song>,
    columns: Int,
    gap: androidx.compose.ui.unit.Dp,
    currentSongId: Long?,
    isPlaying: Boolean,
    selectedSongIds: Set<Long>,
    onSongClick: (Song) -> Unit,
    onSongLongClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val chunked = remember(songs, columns) { songs.chunked(columns) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(gap)
    ) {
        chunked.forEach { rowSongs ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap)
            ) {
                rowSongs.forEach { song ->
                    val isCurrent = song.id == currentSongId
                    val isSelected = song.id in selectedSongIds

                    XvoxAllSongMosaicCard(
                        song = song,
                        widthUnits = 1f,
                        heightUnits = 1f,
                        onClick = { onSongClick(song) },
                        onLongClick = { onSongLongClick(song) },
                        modifier = Modifier.weight(1f),
                        current = isCurrent,
                        playing = isCurrent && isPlaying,
                        selected = isSelected
                    )
                }

                if (rowSongs.size < columns) {
                    repeat(columns - rowSongs.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun XvoxAllSongGridItem(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    tileShape: String = "rounded",
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    val cardColor = rememberSongCardColor(song = song, current = isCurrent, selected = isSelected)
    val shape = when (tileShape) {
        "sharp" -> RoundedCornerShape(4.dp)
        "pill" -> RoundedCornerShape(20.dp)
        else -> RoundedCornerShape(12.dp)
    }

    Column(
        modifier = modifier
            .clip(shape)
            .xvoxSongPress(onClick = onClick, onLongClick = onLongClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(shape),
            contentAlignment = Alignment.Center
        ) {
            XvoxSongArtwork(
                artwork = song.artworkUri,
                requestSize = 140,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = song.title,
            color = if (isCurrent) colors.primaryAccent else colors.primaryText,
            fontSize = 11.5.sp,
            lineHeight = 14.sp,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = song.artist,
            color = colors.secondaryText,
            fontSize = 9.5.sp,
            lineHeight = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
