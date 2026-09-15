package com.xvox.music.features.artist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
    columns: Int = 5,
    rows: Int = 4,
    direction: String = "vertical",
    gap: Int = 8,
    hideText: Boolean = false,
    onArtistClick: (XvoxArtist) -> Unit,
    onArtistLongClick: (XvoxArtist) -> Unit,
    modifier: Modifier = Modifier
) {
    if (direction == "horizontal") {
        val rowCount = rows.coerceIn(1, 6)
        val chunked = artists.chunked(rowCount)
        LazyRow(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(gap.dp)
        ) {
            items(chunked) { colArtists ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(gap.dp),
                    modifier = Modifier.width(108.dp)
                ) {
                    colArtists.forEach { artist ->
                        ArtistSquareItem(
                            artist = artist,
                            showText = !hideText,
                            onClick = { onArtistClick(artist) },
                            onLongClick = { onArtistLongClick(artist) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    } else {
        val colCount = columns.coerceIn(2, 8)
        val chunked = artists.chunked(colCount)

        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(gap.dp)
        ) {
            chunked.forEach { rowArtists ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap.dp)
                ) {
                    rowArtists.forEach { artist ->
                        ArtistSquareItem(
                            artist = artist,
                            showText = !hideText,
                            onClick = { onArtistClick(artist) },
                            onLongClick = { onArtistLongClick(artist) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowArtists.size < colCount) {
                        repeat(colCount - rowArtists.size) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Redesigned Artist Card:
 * Square (1:1 aspect ratio), full cover artwork, with adjustable translucent name overlay at the bottom.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArtistSquareItem(
    artist: XvoxArtist,
    showText: Boolean = true,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.card)
            .xvoxSongPress(
                onClick = onClick,
                onLongClick = onLongClick,
                pressedScale = 0.94f
            ),
        contentAlignment = Alignment.Center
    ) {
        // Full Square Cover Artwork
        if (artist.customImageUri != null) {
            AsyncImage(
                model = artist.customImageUri,
                contentDescription = artist.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        } else if (artist.coverSong != null) {
            XvoxSongArtwork(
                artwork = artist.coverSong.artworkUri,
                requestSize = 256,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_xvox_microphone),
                contentDescription = null,
                tint = colors.primaryAccent,
                modifier = Modifier.size(28.dp)
            )
        }

        // Bottom translucent name overlay (adjustable height for full legibility)
        if (showText) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(colors.cardElevated.copy(alpha = 0.85f))
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = artist.name,
                    color = colors.primaryText,
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
