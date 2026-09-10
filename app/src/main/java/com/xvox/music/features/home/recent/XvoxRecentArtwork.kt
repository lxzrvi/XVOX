package com.xvox.music.features.home.recent

import com.xvox.music.core.ui.effects.xvoxSongPress
import com.xvox.music.features.home.rememberSongCardColor
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.features.home.PlaybackIcon
import com.xvox.music.features.home.PlaybackIconType
import com.xvox.music.features.home.XvoxRecentArtworkSize
import com.xvox.music.features.home.XvoxSongArtwork

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun XvoxRecentArtwork(
    song: Song,
    current: Boolean,
    playing: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    animateEntrance: Boolean = false,
    source: String? = null,
    onSourceClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    val cardColor = rememberSongCardColor(song, current)

    val shape = RoundedCornerShape(3.dp)

    // Press pulls the artwork inwards; the card frame itself does not move.
    var pressed by remember { mutableStateOf(false) }
    val artScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = tween(150),
        label = "recentArtInset"
    )

    // The card is full-cover: the artwork fills it edge to edge. A press never re-crops the
    // image — the artwork itself scales down inwards inside the still card frame, so the cover
    // pulls in evenly and its sides stay exactly where they were.
    Box(
        modifier = modifier
            .clip(shape)
            .background(cardColor)
            .border(
                width = 0.7.dp,
                color = colors.cardBorder,
                shape = shape
            )
            .xvoxSongPress(onClick, onLongClick, pressedScale = 1f, onPressedChange = { pressed = it })
    ) {
        XvoxSongArtwork(
            artwork = song.artworkUri,
            requestSize = XvoxRecentArtworkSize,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { scaleX = artScale; scaleY = artScale }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.78f)
                        )
                    )
                )
        )

        // Origin badge, top-left: tapping it names the collection in the XVOX pill.
        if (onSourceClick != null) {
            RecentSourceBadge(
                source = source,
                onClick = onSourceClick,
                modifier = Modifier.align(Alignment.TopStart).padding(9.dp)
            )
        }

        Text(
            text = song.title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.72f)
                .padding(start = 12.dp, end = 8.dp, bottom = 10.dp)
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(9.dp)
                .height(30.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = if (current && playing) 0.68f else 0.52f))
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = 0.88f,
                        stiffness = 700f
                    )
                )
                .xvoxSongPress(onClick, onLongClick)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            AnimatedContent(
                targetState = current && playing,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(160))
                },
                label = "recentPlayState"
            ) { active ->
                PlaybackIcon(
                    type = if (active) PlaybackIconType.PAUSE else PlaybackIconType.PLAY,
                    color = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }

            if (current && playing) {
                Text(
                    text = "Playing",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}
