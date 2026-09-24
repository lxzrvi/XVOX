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

/**
 * Fixed shell header. Its canvas and profile title stay below the status bar while scroll progress
 * only moves the avatar and action controls out of view. Reversing a list scroll restores them
 * from the exact same fraction rather than rebuilding or snapping the header.
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
    /** 3 keeps all library actions here; 4 moves Liked and 5 moves Liked + Playlists to nav. */
    extendedSlots: Int = 3,
    /** 0 = fully shown, 1 = avatar/actions have scrolled out while title/canvas remain anchored. */
    collapseFraction: Float = 0f,
    useSystemInsets: Boolean = true
) {
    val colors = XvoxTheme.colors
    val chrome = com.xvox.music.core.ui.chrome.LocalXvoxChromeStyle.current
    val density = LocalDensity.current
    val collapsed = collapseFraction.coerceIn(0f, 1f)
    val controlsAlpha = (1f - collapsed).coerceIn(0f, 1f)
    val controlsShiftPx = with(density) { (72.dp * collapsed).toPx() }

    val layout = extendedSlots.coerceIn(3, 5)
    val libraryActions = buildList {
        if (layout == 3) {
            add(HeaderLibraryAction(XvoxHomeLibraryMode.LIKED, R.drawable.ic_xvox_heart, "Liked Songs", onLikedClick))
        }
        if (layout <= 4) {
            add(HeaderLibraryAction(XvoxHomeLibraryMode.PLAYLISTS, R.drawable.ic_xvox_playlist, "Playlists", onPlaylistClick))
        }
        // Artists always remains in the header, including the five-item navigation layout.
        add(HeaderLibraryAction(XvoxHomeLibraryMode.ARTISTS, R.drawable.ic_xvox_artist, "Artists", onArtistClick))
    }
    val selectedActionIndex = libraryActions.indexOfFirst { it.mode == libraryMode }
    val isLibraryActionSelected = selectedActionIndex >= 0
    val targetIndicatorX = 3.dp + 36.dp * selectedActionIndex.coerceAtLeast(0).toFloat()
    val animatedIndicatorX by animateDpAsState(
        targetValue = targetIndicatorX,
        animationSpec = tween(280, easing = SmoothEase),
        label = "headerLibraryIndicatorX"
    )
    val indicatorAlpha by animateFloatAsState(
        targetValue = if (isLibraryActionSelected) 1f else 0f,
        animationSpec = tween(200),
        label = "headerLibraryIndicatorAlpha"
    )

    val alphaFraction = chrome.headerBgAlpha.coerceIn(0f, 1f)
    val hasCustomHeader = !profile.headerImageUri.isNullOrBlank()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { /* consume backdrop touch */ }
            )
            .background(if (hasCustomHeader) Color.Transparent else colors.cardElevated.copy(alpha = if (alphaFraction > 0f) alphaFraction else 0.85f))
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
            // Only the PFP leaves on scroll. The space stays reserved so the title does not jump.
            Box(
                modifier = Modifier.graphicsLayer {
                    translationY = -controlsShiftPx
                    alpha = controlsAlpha
                }
            ) {
                HomeProfileAvatar(
                    profile = profile,
                    modifier = Modifier.size(42.dp),
                    onClick = onProfileClick
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // This is the sticky title portion of the header.
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
                    animationSpec = tween(320, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f))
                ) + fadeIn(tween(260)),
                exit = slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(280, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f))
                ) + fadeOut(tween(220))
            ) {
                Row(
                    modifier = Modifier.graphicsLayer {
                        translationY = -controlsShiftPx
                        alpha = controlsAlpha
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
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

                    val actionShape = RoundedCornerShape(21.dp)
                    val actionPillWidth = 6.dp + 36.dp * libraryActions.size.toFloat()
                    Box(
                        modifier = Modifier
                            .height(42.dp)
                            .width(actionPillWidth)
                            .clip(actionShape)
                            .background(colors.card.copy(alpha = 0.60f))
                    ) {
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(animatedIndicatorX.roundToPx(), 3.dp.roundToPx()) }
                                .size(36.dp)
                                .graphicsLayer { alpha = indicatorAlpha }
                                .clip(CircleShape)
                                .background(colors.cardElevated.copy(alpha = 0.95f))
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
                                        tint = if (libraryMode == action.mode) colors.primaryAccent else colors.primaryText.copy(alpha = 0.70f),
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
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
