package com.xvox.music.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxPersonalFont
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.navigation.XvoxDestination
import com.xvox.music.data.preferences.UserPreferences
import com.xvox.music.features.home.HomeGreeting
import com.xvox.music.features.home.HomeProfileAvatar
import com.xvox.music.features.playlist.XvoxHomeLibraryMode

private val SmoothEase = CubicBezierEasing(0.2f, 0f, 0f, 1f)

@Composable
fun XvoxShellTopHeader(
    profile: UserPreferences,
    destination: XvoxDestination,
    libraryMode: XvoxHomeLibraryMode,
    onProfileClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onLikedClick: () -> Unit,
    onPlaylistClick: () -> Unit,
    onArtistClick: () -> Unit = {},
    useSystemInsets: Boolean = true
) {
    val colors = XvoxTheme.colors
    val chrome = com.xvox.music.core.ui.chrome.LocalXvoxChromeStyle.current

    val alphaFraction = chrome.headerBgAlpha.coerceIn(0f, 1f)
    val hasCustomHeader = !profile.headerImageUri.isNullOrBlank()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (hasCustomHeader) Color.Transparent else colors.surface.copy(alpha = alphaFraction))
    ) {
        if (hasCustomHeader) {
            coil3.compose.AsyncImage(
                model = profile.headerImageUri,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = alphaFraction }
            )
            Box(
                Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.20f * alphaFraction))
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (useSystemInsets) Modifier.windowInsetsPadding(WindowInsets.statusBars) else Modifier)
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .height(54.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeProfileAvatar(
                profile = profile,
                modifier = Modifier.size(42.dp),
                onClick = onProfileClick
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.username,
                    color = colors.primaryAccent,
                    fontFamily = XvoxPersonalFont,
                    fontSize = 18.sp,
                    lineHeight = 19.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val lines = if (profile.showProfileLines) profile.profileLines else emptyList()
                if (!profile.showProfileLines) {
                    // nothing under the name
                } else if (lines.isEmpty()) {
                    HomeGreeting(intervalMs = profile.greetingIntervalMs)
                } else {
                    lines.take(2).forEach { line ->
                        Text(
                            text = line,
                            color = colors.secondaryText,
                            fontSize = 10.sp,
                            lineHeight = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = destination == XvoxDestination.HOME,
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(320, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f))
                ) + fadeIn(tween(260)),
                exit = slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(280, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f))
                ) + fadeOut(tween(220))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Standalone Refresh Icon (Circle)
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(colors.card.copy(alpha = 0.60f))
                            .xvoxPressScale(pressedScale = 0.90f) { onRefreshClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_xvox_refresh),
                            contentDescription = "Refresh Library",
                            tint = colors.primaryText,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    // 2. 3-Item Pill (Liked, Playlists, Artists) with smooth shifting circle indicator
                    val isTabSelected = libraryMode in listOf(
                        XvoxHomeLibraryMode.LIKED,
                        XvoxHomeLibraryMode.PLAYLISTS,
                        XvoxHomeLibraryMode.ARTISTS
                    )

                    val targetIndicatorX = when (libraryMode) {
                        XvoxHomeLibraryMode.LIKED -> 3.dp
                        XvoxHomeLibraryMode.PLAYLISTS -> 39.dp
                        XvoxHomeLibraryMode.ARTISTS -> 75.dp
                        else -> 3.dp
                    }

                    val animatedIndicatorX by animateDpAsState(
                        targetValue = targetIndicatorX,
                        animationSpec = tween(280, easing = SmoothEase),
                        label = "pillIndicatorX"
                    )

                    val animatedIndicatorAlpha by animateFloatAsState(
                        targetValue = if (isTabSelected) 1f else 0f,
                        animationSpec = tween(200),
                        label = "pillIndicatorAlpha"
                    )

                    val actionShape = RoundedCornerShape(21.dp)

                    Box(
                        modifier = Modifier
                            .height(42.dp)
                            .width(114.dp)
                            .clip(actionShape)
                            .background(colors.card.copy(alpha = 0.60f))
                    ) {
                        // Smoothly shifting highlight circle
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(animatedIndicatorX.roundToPx(), 3.dp.roundToPx()) }
                                .size(36.dp)
                                .graphicsLayer { alpha = animatedIndicatorAlpha }
                                .clip(CircleShape)
                                .background(colors.cardElevated.copy(alpha = 0.95f))
                        )

                        // 3 Persistent Icons (Do not change drawables on tap)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .padding(horizontal = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Liked Tab
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = onLikedClick
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_xvox_heart),
                                    contentDescription = "Liked Songs",
                                    tint = if (libraryMode == XvoxHomeLibraryMode.LIKED) colors.primaryAccent else colors.primaryText.copy(alpha = 0.70f),
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            // Playlists Tab
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = onPlaylistClick
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_xvox_playlist),
                                    contentDescription = "Playlists",
                                    tint = if (libraryMode == XvoxHomeLibraryMode.PLAYLISTS) colors.primaryAccent else colors.primaryText.copy(alpha = 0.70f),
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            // Artists Tab
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = onArtistClick
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_xvox_artist),
                                    contentDescription = "Artists",
                                    tint = if (libraryMode == XvoxHomeLibraryMode.ARTISTS) colors.primaryAccent else colors.primaryText.copy(alpha = 0.70f),
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        val headerEdge = com.xvox.music.core.ui.chrome.parseHexColor(chrome.headerBorder)
            ?: colors.cardBorder
        Box(
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(1.dp)
                .background(headerEdge.copy(alpha = chrome.headerBorderAlpha.coerceIn(0f, 1f)))
        )
    }
}
