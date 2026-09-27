package com.xvox.music.features.home.recent

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
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

        // Play/Pause indicator morphs from its 30dp icon capsule into only the width required
        // for “Playing”. A single measured outer width avoids the old competing layout pulses.
        val playbackActive = current && playing
        val badgeWidth by animateDpAsState(
            targetValue = if (playbackActive) 64.dp else 30.dp,
            animationSpec = tween(220),
            label = "recentPlayingBadgeWidth"
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(9.dp)
                .width(badgeWidth)
                .height(30.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = if (playbackActive) 0.68f else 0.52f))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            AnimatedContent(
                targetState = playbackActive,
                transitionSpec = {
                    (fadeIn(tween(130)) + scaleIn(initialScale = .88f, animationSpec = tween(150)))
                        .togetherWith(fadeOut(tween(110)) + scaleOut(targetScale = .88f, animationSpec = tween(120)))
                },
                label = "recentPlayPauseMorph"
            ) { active ->
                PlaybackIcon(
                    type = if (active) PlaybackIconType.PAUSE else PlaybackIconType.PLAY,
                    color = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
            AnimatedVisibility(
                visible = playbackActive,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(100))
            ) {
                Text(
                    text = "Playing",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}
