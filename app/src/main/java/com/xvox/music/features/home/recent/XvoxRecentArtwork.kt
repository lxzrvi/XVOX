package com.xvox.music.features.home.recent

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.effects.xvoxSongPress
import com.xvox.music.features.home.PlaybackIcon
import com.xvox.music.features.home.PlaybackIconType
import com.xvox.music.features.home.XvoxRecentArtworkSize
import com.xvox.music.features.home.XvoxSongArtwork
import com.xvox.music.features.home.rememberSongCardColor

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun XvoxRecentArtwork(
    song: Song,
    current: Boolean,
    playing: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    animateEntrance: Boolean = false,
    selected: Boolean = false,
    source: String? = null,
    modifier: Modifier = Modifier
) {
    val cardColor = rememberSongCardColor(song, current, selected)
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .xvoxSongPress(onClick, onLongClick, pressedScale = 0.95f)
            .clip(shape)
            .background(cardColor)
    ) {
        XvoxSongArtwork(
            artwork = song.artworkUri,
            fallbackSeed = song.id.hashCode(),
            requestSize = XvoxRecentArtworkSize,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.82f)
                        )
                    )
                )
        )

        // Bottom text: Song Title and Artist Name with tight spacing
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.72f)
                .padding(start = 12.dp, end = 8.dp, bottom = 10.dp)
        ) {
            Text(
                text = song.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = song.artist,
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // One interruptible progress owns both width and icon state. Unlike nested
        // AnimatedVisibility/AnimatedContent transitions, rapid song changes retarget the old
        // badge back to a circle and the new badge to a pill without retaining stale exit nodes.
        val playbackActive = current && playing
        val badgeProgress by animateFloatAsState(
            targetValue = if (playbackActive) 1f else 0f,
            animationSpec = tween(220, easing = FastOutSlowInEasing),
            label = "recentPlayingBadgeProgress"
        )
        val badgeShape = RoundedCornerShape(percent = 50)
        val badgeWidth = 30.dp + 40.dp * badgeProgress
        // Delay label visibility until there is real room for it. The physical pill outline is
        // already rounded throughout the whole expansion, so its right edge never appears flat or
        // clipped while a circle turns into the Playing capsule.
        val labelAlpha = ((badgeProgress - .18f) / .82f).coerceIn(0f, 1f)

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(9.dp)
                .width(badgeWidth)
                .height(30.dp)
                .clip(badgeShape)
                .background(Color.Black.copy(alpha = .52f + .16f * badgeProgress))
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
                    .size(14.dp),
                contentAlignment = Alignment.Center
            ) {
                PlaybackIcon(
                    type = PlaybackIconType.PLAY,
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = (1f - badgeProgress).coerceIn(0f, 1f) }
                )
                PlaybackIcon(
                    type = PlaybackIconType.PAUSE,
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = badgeProgress.coerceIn(0f, 1f) }
                )
            }
            Text(
                text = "Playing",
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 26.dp)
                    .graphicsLayer {
                        alpha = labelAlpha
                        translationX = (-5f * (1f - badgeProgress))
                    }
            )
        }
    }
}
