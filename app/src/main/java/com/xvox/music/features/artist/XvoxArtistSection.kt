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
    gap: Int = 8,
    hideText: Boolean = false,
    onArtistClick: (XvoxArtist) -> Unit,
    onArtistLongClick: (XvoxArtist) -> Unit,
    modifier: Modifier = Modifier
) {
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
                    ArtistCircleItem(
                        artist = artist,
                        showText = !hideText,
                        onClick = { onArtistClick(artist) },
                        onLongClick = { onArtistLongClick(artist) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill empty slots so rows stay evenly spaced
                if (rowArtists.size < colCount) {
                    repeat(colCount - rowArtists.size) {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArtistCircleItem(
    artist: XvoxArtist,
    showText: Boolean = true,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors

    Column(
        modifier = modifier
            .xvoxSongPress(
                onClick = onClick,
                onLongClick = onLongClick,
                pressedScale = 0.94f
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(colors.card),
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
                    requestSize = 128,
                    modifier = Modifier.matchParentSize()
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_xvox_microphone),
                    contentDescription = null,
                    tint = colors.primaryAccent,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        if (showText) {
            Text(
                text = artist.name,
                color = colors.primaryText,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp, start = 2.dp, end = 2.dp)
            )
        }
    }
}
