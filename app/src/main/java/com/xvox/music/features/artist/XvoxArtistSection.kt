package com.xvox.music.features.artist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val colCount = 5
    val chunked = artists.chunked(colCount)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        chunked.forEach { rowArtists ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowArtists.forEach { artist ->
                    ArtistCircleItem(
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

/**
 * 5-column vertical scroll grid item:
 * Circular artist cover with 1-line artist name (truncated with ellipsis) and equal gaps.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArtistCircleItem(
    artist: XvoxArtist,
    showText: Boolean = true,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors

    Column(
        modifier = modifier
            .xvoxSongPress(
                onClick = onClick,
                onLongClick = onLongClick,
                pressedScale = 0.93f
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(colors.cardElevated),
            contentAlignment = Alignment.Center
        ) {
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
                    requestSize = 160,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_xvox_artist),
                    contentDescription = null,
                    tint = colors.primaryAccent,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        if (showText) {
            Text(
                text = artist.name,
                color = colors.primaryText,
                fontSize = 10.5.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }
    }
}
