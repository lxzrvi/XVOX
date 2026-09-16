package com.xvox.music.core.ui.miniplayer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.features.home.XvoxSongArtwork
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
fun XvoxMiniPlayerCard(
    song: Song,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    direction: Int,
    togglePlay: () -> Unit,
    isLiked: Boolean = false,
    onLike: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    val chrome = com.xvox.music.core.ui.chrome.LocalXvoxChromeStyle.current
    val zones by remember(song.id) {
        com.xvox.music.player.playback.XvoxBlendMonitor.state.map {
            if (it.enabled && it.currentId == song.id) it.introZoneMs to it.tailZoneMs else 0L to 0L
        }.distinctUntilChanged()
    }.collectAsState(initial = 0L to 0L)

    val cardShape = RoundedCornerShape(15.dp)
    val artworkShape = RoundedCornerShape(11.dp)
    val isFullCover = chrome.miniCoverStyle == "full"

    val controlInteraction = remember { MutableInteractionSource() }

    val progress = if (duration > 0L) {
        (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(cardShape)
            .background(colors.surface.copy(alpha = chrome.miniBgAlpha.coerceIn(0f, 1f)))
            .drawWithContent {
                drawContent()
                val radius = 15.dp.toPx()
                val inside = Path().apply {
                    addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(radius)))
                }
                clipPath(inside) {
                    val barHeight = 3.dp.toPx()
                    if (duration > 0) {
                        val intro = (zones.first.toFloat() / duration).coerceIn(0f, .5f)
                        val tail = (zones.second.toFloat() / duration).coerceIn(0f, .5f)
                        drawRect(com.xvox.music.player.nowplaying.XvoxBlendInColor.copy(alpha = .45f), Offset.Zero, Size(size.width * intro, barHeight))
                        drawRect(com.xvox.music.player.nowplaying.XvoxBlendOutColor.copy(alpha = .45f), Offset(size.width * (1 - tail), 0f), Size(size.width * tail, barHeight))
                    }
                    if (progress > 0) drawRect(colors.primaryAccent, Offset.Zero, Size(size.width * progress, barHeight))
                }
            }
    ) {
        if (isFullCover) {
            val alphaFraction = chrome.miniBgAlpha.coerceIn(0f, 1f)
            XvoxSongArtwork(
                artwork = song.artworkUri,
                requestSize = 512,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = alphaFraction }
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.30f * alphaFraction),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.40f * alphaFraction)
                            )
                        )
                    )
            )
        }

        val titleColor = if (isFullCover) Color.White else colors.primaryText
        val artistColor = if (isFullCover) Color.White.copy(alpha = 0.82f) else colors.secondaryText

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = if (isFullCover) 14.dp else 4.dp, top = 4.dp, end = 88.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isFullCover) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(artworkShape)
                ) {
                    AnimatedContent(
                        targetState = song,
                        contentKey = { it.id },
                        transitionSpec = {
                            fadeIn(tween(180)).togetherWith(fadeOut(tween(140)))
                        },
                        modifier = Modifier.fillMaxSize(),
                        label = "miniArtworkFade"
                    ) { visualSong ->
                        XvoxSongArtwork(
                            artwork = visualSong.artworkUri,
                            requestSize = 160,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                AnimatedContent(
                    targetState = song,
                    contentKey = { it.id },
                    transitionSpec = {
                        when {
                            direction > 0 -> {
                                (fadeIn(tween(150)) + slideInVertically(animationSpec = tween(200), initialOffsetY = { it }))
                                    .togetherWith(fadeOut(tween(120)) + slideOutVertically(animationSpec = tween(180), targetOffsetY = { -it }))
                            }
                            direction < 0 -> {
                                (fadeIn(tween(150)) + slideInVertically(animationSpec = tween(200), initialOffsetY = { -it }))
                                    .togetherWith(fadeOut(tween(120)) + slideOutVertically(animationSpec = tween(180), targetOffsetY = { it }))
                            }
                            else -> {
                                fadeIn(tween(140)).togetherWith(fadeOut(tween(100)))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    label = "miniMetadataSlide"
                ) { visualSong ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = if (isFullCover) 0.dp else 9.dp, end = 5.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = visualSong.title,
                            color = titleColor,
                            fontSize = 12.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = visualSong.artist,
                            color = artistColor,
                            fontSize = 9.sp,
                            lineHeight = 11.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isFullCover) Color.Black.copy(alpha = 0.45f) else colors.cardElevated.copy(alpha = 0.68f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onLike
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(if (isLiked) R.drawable.ic_xvox_heart else R.drawable.ic_xvox_heart_outline),
                    contentDescription = "Like",
                    tint = if (isLiked) Color.White else (if (isFullCover) Color.White else colors.primaryText),
                    modifier = Modifier.size(18.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isFullCover) Color.Black.copy(alpha = 0.45f) else colors.cardElevated.copy(alpha = 0.68f))
                    .clickable(
                        interactionSource = controlInteraction,
                        indication = null,
                        onClick = togglePlay
                    ),
                contentAlignment = Alignment.Center
            ) {
                XvoxMiniPlayerIcon(
                    icon = if (isPlaying) XvoxMiniIcon.PAUSE else XvoxMiniIcon.PLAY,
                    color = if (isFullCover) Color.White else colors.primaryText,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
