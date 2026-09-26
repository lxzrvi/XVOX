package com.xvox.music.features.artist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxSongPress
import com.xvox.music.features.home.XvoxSongArtwork

@Composable
fun XvoxArtistGrid(
    artists: List<XvoxArtist>,
    columns: Int = 4,
    rows: Int = 4,
    direction: String = "vertical",
    gap: Int = 12,
    hideText: Boolean = false,
    onArtistClick: (XvoxArtist) -> Unit,
    onArtistLongClick: (XvoxArtist) -> Unit,
    onMergeClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isLandscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val effectiveColumns = if (isLandscape) 8 else columns.coerceIn(2, 8)
    val effectiveRows = rows.coerceIn(1, 8)

    if (direction == "horizontal") {
        val totalPerColumn = effectiveRows
        val chunkedCols = artists.chunked(totalPerColumn)

        LazyRow(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentPadding = PaddingValues(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(gap.dp)
        ) {
            items(chunkedCols) { colArtists ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    colArtists.forEach { artist ->
                        ArtistCircleItem(
                            artist = artist,
                            hideText = hideText,
                            onClick = { onArtistClick(artist) },
                            onLongClick = { onArtistLongClick(artist) },
                            modifier = Modifier.width(68.dp)
                        )
                    }
                }
            }
        }
    } else {
        val chunked = artists.chunked(effectiveColumns)
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            chunked.forEach { rowArtists ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap.dp)
                ) {
                    rowArtists.forEach { artist ->
                        ArtistCircleItem(
                            artist = artist,
                            hideText = hideText,
                            onClick = { onArtistClick(artist) },
                            onLongClick = { onArtistLongClick(artist) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowArtists.size < effectiveColumns) {
                        repeat(effectiveColumns - rowArtists.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArtistCircleItem(
    artist: XvoxArtist,
    hideText: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors

    Column(
        modifier = modifier
            .xvoxSongPress(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(colors.card),
            contentAlignment = Alignment.Center
        ) {
            if (artist.customImageUri != null) {
                AsyncImage(
                    model = artist.customImageUri,
                    contentDescription = artist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (artist.coverSong != null) {
                XvoxSongArtwork(
                    artwork = artist.coverSong.artworkUri,
                    requestSize = 256,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_xvox_artist),
                    contentDescription = null,
                    tint = colors.primaryAccent,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        if (!hideText) {
            Text(
                text = artist.name,
                color = colors.primaryText,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
            )
        }
    }
}

@Composable
fun ArtistSquareItem(
    artist: XvoxArtist,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ArtistCircleItem(
        artist = artist,
        hideText = false,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier
    )
}
