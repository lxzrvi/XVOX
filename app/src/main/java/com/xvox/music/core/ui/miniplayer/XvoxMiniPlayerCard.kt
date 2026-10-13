package com.xvox.music.core.ui.miniplayer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.blur
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
import com.xvox.music.features.home.XvoxFallbackBrand
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
    val miniEdge = com.xvox.music.core.ui.chrome.parseHexColor(chrome.miniBorder) ?: colors.cardBorder
    // Keep the Mini Player entirely palette-derived.  The editor's transparency range maps to
    // this real alpha (rather than silently clamping it to a different-looking hard-coded tint).
    val miniAlpha = chrome.miniBgAlpha.coerceIn(0f, 1f)
    val miniRadius = chrome.miniCornerRadius.coerceIn(6f, 32f).dp
    val zones by remember(song.id) {
        com.xvox.music.player.playback.XvoxBlendMonitor.state.map {
            if (it.enabled && it.currentId == song.id) it.introZoneMs to it.tailZoneMs else 0L to 0L
        }.distinctUntilChanged()
    }.collectAsState(initial = 0L to 0L)

    val cardShape = RoundedCornerShape(miniRadius)
    // Artwork keeps its full 48dp footprint while following the card's corner language.
    val artworkShape = RoundedCornerShape((chrome.miniCornerRadius - 4f).coerceIn(7f, 28f).dp)
    val miniCoverStyle = when (chrome.miniCoverStyle) {
        "full", "blur" -> chrome.miniCoverStyle
        else -> "default"
    }
    val isFullCover = miniCoverStyle == "full"
    // Default keeps the Mini Player's framed material. Full cover and local blurred-cover modes
    // are artwork surfaces, so an edge around either one would read as an unrelated card border.
    val showMiniBorder = miniCoverStyle == "default"
    // The border participates in the same cover-mode handoff instead of popping on the first
    // settings frame. Its settled states remain Default-only versus borderless Full/Blur.
    val miniBorderVisibility by animateFloatAsState(
        targetValue = if (showMiniBorder) 1f else 0f,
        animationSpec = tween(240, easing = XvoxPlayerTransitionMotion.easing),
        label = "miniBorderCrossfade"
    )
    val miniBlur = chrome.miniCoverBlur.coerceIn(12f, 50f).dp

    val controlInteraction = remember { MutableInteractionSource() }

    val progress = if (duration > 0L) {
        (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(cardShape)
            .background(colors.background.copy(alpha = miniAlpha))
            .border(
                chrome.miniBorderWidth.coerceIn(0f, 4f).dp,
                miniEdge.copy(
                    alpha = chrome.miniBorderAlpha.coerceIn(.34f, 1f) * miniAlpha * miniBorderVisibility
                ),
                cardShape
            )
            .drawWithContent {
                drawContent()
                val radius = miniRadius.toPx()
                val inside = Path().apply {
                    addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(radius)))
                }
                clipPath(inside) {
                    // Match the slim Now Playing progress rail so both player surfaces feel related.
                    val barHeight = 2.5.dp.toPx()
                    if (duration > 0) {
                        val intro = (zones.first.toFloat() / duration).coerceIn(0f, .5f)
                        val tail = (zones.second.toFloat() / duration).coerceIn(0f, .5f)
                        drawRect(com.xvox.music.player.nowplaying.XvoxBlendInColor.copy(alpha = .45f * miniAlpha), Offset.Zero, Size(size.width * intro, barHeight))
                        drawRect(com.xvox.music.player.nowplaying.XvoxBlendOutColor.copy(alpha = .45f * miniAlpha), Offset(size.width * (1 - tail), 0f), Size(size.width * tail, barHeight))
                    }
                    if (progress > 0) drawRect(colors.primaryAccent.copy(alpha = miniAlpha), Offset.Zero, Size(size.width * progress, barHeight))
                }
            }
    ) {
        // An Okay-confirmed cover-style change crosses the actual visual layers rather than
        // swapping a full-cover/blur/default branch in one frame. Mini Player imagery only needs
        // the same 160px cache as its visible 48dp cover, which also avoids a late 512px decode
        // flashing through the transition.
        AnimatedContent(
            targetState = miniCoverStyle,
            transitionSpec = {
                fadeIn(tween(280, easing = XvoxPlayerTransitionMotion.easing))
                    .togetherWith(fadeOut(tween(240, easing = XvoxPlayerTransitionMotion.easing)))
            },
            modifier = Modifier.fillMaxSize(),
            label = "miniCoverStyleCrossfade"
        ) { visualStyle ->
            when (visualStyle) {
                "full" -> {
                    val alphaFraction = chrome.miniBgAlpha.coerceIn(0f, 1f)
                    XvoxSongArtwork(
                        artwork = song.artworkUri,
                        fallbackSeed = song.id.hashCode(),
                        fallbackBrand = XvoxFallbackBrand.MINI_PLAYER,
                        requestSize = 160,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = alphaFraction }
                    )
                    // Solid (0% transparent) cover mode is intentionally un-tinted: the selected
                    // cover remains the actual surface rather than a theme-colour wash.
                    val surfaceTintAlpha = .36f * alphaFraction * (1f - alphaFraction)
                    if (surfaceTintAlpha > .001f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            colors.background.copy(alpha = .75f * surfaceTintAlpha),
                                            Color.Transparent,
                                            colors.background.copy(alpha = surfaceTintAlpha)
                                        )
                                    )
                                )
                        )
                    }
                }
                "blur" -> {
                    // This is local artwork-derived blur only; no app-wide blur/Haze surface.
                    XvoxSongArtwork(
                        artwork = song.artworkUri,
                        fallbackSeed = song.id.hashCode(),
                        fallbackBrand = XvoxFallbackBrand.NONE,
                        requestSize = 160,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = 1.16f
                                scaleY = 1.16f
                                alpha = (.70f + .24f * miniAlpha).coerceIn(0f, 1f)
                            }
                            .blur(miniBlur)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        colors.background.copy(alpha = .36f),
                                        colors.background.copy(alpha = .14f),
                                        colors.background.copy(alpha = .46f)
                                    )
                                )
                            )
                    )
                }
            }
        }

        val titleColor = colors.primaryText
        val artistColor = colors.secondaryText
        val artworkSlotWidth by animateDpAsState(
            targetValue = if (isFullCover) 0.dp else 48.dp,
            animationSpec = tween(260, easing = XvoxPlayerTransitionMotion.easing),
            label = "miniArtworkSlotWidth"
        )
        val artworkSlotAlpha by animateFloatAsState(
            targetValue = if (isFullCover) 0f else 1f,
            animationSpec = tween(220, easing = XvoxPlayerTransitionMotion.easing),
            label = "miniArtworkSlotAlpha"
        )
        val rowStartPadding by animateDpAsState(
            targetValue = if (isFullCover) 12.dp else 4.dp,
            animationSpec = tween(260, easing = XvoxPlayerTransitionMotion.easing),
            label = "miniRowStartPadding"
        )
        val metadataStartPadding by animateDpAsState(
            targetValue = if (isFullCover) 0.dp else 9.dp,
            animationSpec = tween(260, easing = XvoxPlayerTransitionMotion.easing),
            label = "miniMetadataStartPadding"
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                // Reserve the two outlined 38-dp actions plus their gap so title text never
                // slides beneath a control after the mini player was made shorter.
                .padding(start = rowStartPadding, top = 4.dp, end = 94.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Retain the artwork slot through a full-cover handoff and collapse it smoothly;
            // otherwise the metadata would visibly jump while the backdrop is still crossfading.
            Box(
                modifier = Modifier
                    .width(artworkSlotWidth)
                    .height(48.dp)
                    .clip(artworkShape)
                    .graphicsLayer { alpha = artworkSlotAlpha }
            ) {
                AnimatedContent(
                    targetState = song,
                    contentKey = { it.id },
                    transitionSpec = {
                        fadeIn(tween(280, easing = com.xvox.music.core.ui.miniplayer.XvoxPlayerTransitionMotion.easing))
                            .togetherWith(fadeOut(tween(220, easing = com.xvox.music.core.ui.miniplayer.XvoxPlayerTransitionMotion.easing)))
                    },
                    modifier = Modifier.fillMaxSize(),
                    label = "miniArtworkFade"
                ) { visualSong ->
                    XvoxSongArtwork(
                        artwork = visualSong.artworkUri,
                        fallbackSeed = visualSong.id.hashCode(),
                        fallbackBrand = XvoxFallbackBrand.MINI_PLAYER,
                        requestSize = 160,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                AnimatedContent(
                    targetState = song,
                    contentKey = { it.id },
                    transitionSpec = {
                        when {
                            direction > 0 -> {
                                (fadeIn(tween(250, easing = XvoxPlayerTransitionMotion.easing)) +
                                    slideInVertically(animationSpec = tween(320, easing = XvoxPlayerTransitionMotion.easing), initialOffsetY = { it }))
                                    .togetherWith(
                                        fadeOut(tween(210, easing = XvoxPlayerTransitionMotion.easing)) +
                                            slideOutVertically(animationSpec = tween(290, easing = XvoxPlayerTransitionMotion.easing), targetOffsetY = { -it })
                                    )
                            }
                            direction < 0 -> {
                                (fadeIn(tween(250, easing = XvoxPlayerTransitionMotion.easing)) +
                                    slideInVertically(animationSpec = tween(320, easing = XvoxPlayerTransitionMotion.easing), initialOffsetY = { -it }))
                                    .togetherWith(
                                        fadeOut(tween(210, easing = XvoxPlayerTransitionMotion.easing)) +
                                            slideOutVertically(animationSpec = tween(290, easing = XvoxPlayerTransitionMotion.easing), targetOffsetY = { it })
                                    )
                            }
                            else -> {
                                fadeIn(tween(240, easing = XvoxPlayerTransitionMotion.easing))
                                    .togetherWith(fadeOut(tween(190, easing = XvoxPlayerTransitionMotion.easing)))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    label = "miniMetadataSlide"
                ) { visualSong ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = metadataStartPadding, end = 5.dp),
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
                    // Both right-side actions share one clearly distinct, theme-derived 50%
                    // surface. It stays legible in light and dark themes without implying that
                    // Like and Play belong to different visual systems.
                    .background(colors.cardElevated.copy(alpha = .50f * miniAlpha))
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
                    tint = if (isLiked) colors.primaryAccent else colors.primaryText,
                    modifier = Modifier.size(18.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    // Same theme-aware, 50%-transparent surface as Like.
                    .background(colors.cardElevated.copy(alpha = .50f * miniAlpha))
                    .clickable(
                        interactionSource = controlInteraction,
                        indication = null,
                        onClick = togglePlay
                    ),
                contentAlignment = Alignment.Center
            ) {
                XvoxMiniPlayerIcon(
                    icon = if (isPlaying) XvoxMiniIcon.PAUSE else XvoxMiniIcon.PLAY,
                    color = colors.primaryText,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
