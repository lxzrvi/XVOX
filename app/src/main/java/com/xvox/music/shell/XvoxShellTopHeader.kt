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
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalDensity
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

private data class HeaderLibraryAction(
    val mode: XvoxHomeLibraryMode,
    val icon: Int,
    val label: String,
    val onClick: () -> Unit
)

/** Height of the profile/header controls below the system status-bar area. */
val XvoxShellTopHeaderBodyHeight = 66.dp

/**
 * Header artwork deliberately starts at screen y=0, behind the status bar.  Callers place this
 * composable as a real first item in their page list, so it moves, leaves, and returns only with
 * that page's own scroll rather than through a separately translated shell viewport.
 */
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
    onRecentClick: () -> Unit = {},
    /** Optional transactional preview supplied by the profile editor. */
    headerDimEnabledOverride: Boolean? = null,
    headerDimAmountOverride: Float? = null,
    useSystemInsets: Boolean = true
) {
    val colors = XvoxTheme.colors
    val chrome = com.xvox.music.core.ui.chrome.LocalXvoxChromeStyle.current
    val headerEdge = com.xvox.music.core.ui.chrome.parseHexColor(chrome.headerBorder) ?: colors.cardBorder
    val density = LocalDensity.current
    val statusBarHeight = with(density) {
        if (useSystemInsets) WindowInsets.statusBars.getTop(this).toDp() else 0.dp
    }
    val headerHeight = statusBarHeight + XvoxShellTopHeaderBodyHeight
    val libraryActions = listOf(
        HeaderLibraryAction(XvoxHomeLibraryMode.RECENT, R.drawable.ic_xvox_recent, "Recently Played", onRecentClick),
        HeaderLibraryAction(XvoxHomeLibraryMode.LIKED, R.drawable.ic_xvox_heart, "Liked Songs", onLikedClick),
        HeaderLibraryAction(XvoxHomeLibraryMode.PLAYLISTS, R.drawable.ic_xvox_playlist, "Playlists", onPlaylistClick),
        HeaderLibraryAction(XvoxHomeLibraryMode.ARTISTS, R.drawable.ic_xvox_artist, "Artists", onArtistClick)
    )
    val selectedActionIndex = libraryActions.indexOfFirst { it.mode == libraryMode }
    val selected = selectedActionIndex >= 0
    val targetIndicatorX = 3.dp + 36.dp * selectedActionIndex.coerceAtLeast(0).toFloat()
    val animatedIndicatorX by animateDpAsState(
        targetValue = targetIndicatorX,
        animationSpec = tween(280, easing = SmoothEase),
        label = "headerLibraryIndicatorX"
    )
    val indicatorAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(200),
        label = "headerLibraryIndicatorAlpha"
    )
    val dimEnabled = headerDimEnabledOverride ?: chrome.headerDimEnabled
    val dimAmount = headerDimAmountOverride ?: chrome.headerDimAmount
    val headerDimAlpha = if (dimEnabled) dimAmount.coerceIn(0f, 1f) else 0f
    // Use a palette-owned dark tone rather than a hard-coded overlay colour.
    val headerDimColor = if (colors.isLight) colors.primaryText else colors.background
    val hasCustomHeader = !profile.headerImageUri.isNullOrBlank()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(headerHeight)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { /* consume backdrop touch */ }
            )
            .background(if (hasCustomHeader) Color.Transparent else colors.cardElevated)
    ) {
        if (hasCustomHeader) {
            coil3.compose.AsyncImage(
                model = profile.headerImageUri,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
            )
        }
        // Apply the transactional dim preview to both custom artwork and the default palette
        // Header, so every slider movement has an immediate visible result.
        if (headerDimAlpha > .001f) {
            Box(Modifier.matchParentSize().background(headerDimColor.copy(alpha = headerDimAlpha)))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (useSystemInsets) Modifier.windowInsetsPadding(WindowInsets.statusBars) else Modifier)
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .height(54.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeProfileAvatar(profile = profile, modifier = Modifier.size(42.dp), onClick = onProfileClick)
            Spacer(Modifier.width(10.dp))

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
                if (profile.showProfileLines) {
                    if (lines.isEmpty()) {
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
            }

            AnimatedVisibility(
                visible = destination == XvoxDestination.HOME,
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(320, easing = CubicBezierEasing(.16f, 1f, .3f, 1f))
                ) + fadeIn(tween(260)),
                exit = slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(280, easing = CubicBezierEasing(.16f, 1f, .3f, 1f))
                ) + fadeOut(tween(220))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Header controls use the same translucent, thin-edged language as navigation.
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(colors.card.copy(alpha = .46f))
                            .xvoxPressScale(pressedScale = .90f, onClick = onRefreshClick),
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

                    val actionShape = RoundedCornerShape(21.dp)
                    val actionPillWidth = 6.dp + 36.dp * libraryActions.size.toFloat()
                    Box(
                        modifier = Modifier
                            .height(42.dp)
                            .width(actionPillWidth)
                            .clip(actionShape)
                            .background(colors.card.copy(alpha = .46f))
                    ) {
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(animatedIndicatorX.roundToPx(), 3.dp.roundToPx()) }
                                .size(36.dp)
                                .graphicsLayer { alpha = indicatorAlpha }
                                .clip(CircleShape)
                                .background(colors.cardElevated.copy(alpha = .82f))
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .padding(horizontal = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            libraryActions.forEach { action ->
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .clickable(
                                            interactionSource = remember(action.mode) { MutableInteractionSource() },
                                            indication = null,
                                            onClick = action.onClick
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(action.icon),
                                        contentDescription = action.label,
                                        tint = if (libraryMode == action.mode) colors.primaryAccent else colors.primaryText.copy(alpha = .70f),
                                        modifier = Modifier.size(if (action.mode == XvoxHomeLibraryMode.LIKED) 18.dp else 19.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(1.dp)
                .background(headerEdge.copy(alpha = chrome.headerBorderAlpha.coerceIn(0f, 1f)))
        )
    }
}
