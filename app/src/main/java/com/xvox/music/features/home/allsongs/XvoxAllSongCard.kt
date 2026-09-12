package com.xvox.music.features.home.allsongs

import com.xvox.music.core.ui.effects.xvoxSongPress
import com.xvox.music.features.home.rememberSongCardColor
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.features.home.XvoxGridArtworkSize
import com.xvox.music.features.home.XvoxSongArtwork

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun XvoxAllSongCard(
    song: Song,
    current: Boolean,
    playing: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    val colors = XvoxTheme.colors
    val cardColor = rememberSongCardColor(song, current, selected)
    val cardShape = RoundedCornerShape(11.dp)
    val artworkShape = RoundedCornerShape(6.dp)

    val borderWidth = if (selected) 2.dp else 0.7.dp
    val borderColor = if (selected) colors.primaryAccent else colors.cardBorder

    Column(
        modifier = modifier
            .xvoxSongPress(onClick = onClick, onLongClick = onLongClick, pressedScale = 0.95f, hapticOnTap = false)
            .clip(cardShape)
            .background(cardColor)
            .border(width = borderWidth, color = borderColor, shape = cardShape)
            .padding(5.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            XvoxSongArtwork(
                artwork = song.artworkUri,
                requestSize = XvoxGridArtworkSize,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(artworkShape)
            )

            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(18.dp)
                        .background(colors.primaryAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_xvox_check),
                        contentDescription = "Selected",
                        tint = colors.background,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = song.title,
                // The card of the song that is currently playing wears the accent colour.
                color = if (current) colors.primaryAccent else colors.primaryText,
                fontSize = 10.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = song.artist,
                color = colors.secondaryText,
                fontSize = 8.sp,
                lineHeight = 9.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
