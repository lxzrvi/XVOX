package com.xvox.music.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxPersonalFont
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxGlass
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.navigation.XvoxDestination
import com.xvox.music.data.preferences.UserPreferences
import com.xvox.music.features.home.HomeGreeting
import com.xvox.music.features.home.HomeProfileAvatar
import com.xvox.music.features.playlist.XvoxHomeLibraryMode

private val MainEase = CubicBezierEasing(0.2f, 0f, 0f, 1f)

@Composable
fun XvoxShellTopHeader(
    profile: UserPreferences,
    destination: XvoxDestination,
    libraryMode: XvoxHomeLibraryMode,
    onProfileClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onLikedClick: () -> Unit,
    onPlaylistClick: () -> Unit
) {
    val colors = XvoxTheme.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(bottom = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp)
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
                HomeGreeting()
            }

            AnimatedVisibility(
                visible = destination == XvoxDestination.HOME,
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(380, easing = MainEase)
                ) + fadeIn(tween(280)),
                exit = slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(340, easing = MainEase)
                ) + fadeOut(tween(240))
            ) {
                val actionShape = RoundedCornerShape(21.dp)

                Row(
                    modifier = Modifier
                        .height(42.dp)
                        .xvoxGlass(
                            shape = actionShape,
                            tint = colors.card.copy(alpha = 0.68f),
                            solidFallback = colors.card
                        )
                        .padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_xvox_refresh),
                        contentDescription = "Refresh Library",
                        tint = colors.primaryText,
                        modifier = Modifier
                            .size(36.dp)
                            .xvoxPressScale(pressedScale = 0.90f) { onRefreshClick() }
                            .padding(8.dp)
                    )

                    Icon(
                        painter = painterResource(R.drawable.ic_xvox_heart),
                        contentDescription = "Liked Songs",
                        tint = if (libraryMode == XvoxHomeLibraryMode.LIKED) colors.primaryAccent else colors.primaryText,
                        modifier = Modifier
                            .size(36.dp)
                            .xvoxPressScale(pressedScale = 0.90f) { onLikedClick() }
                            .padding(8.dp)
                    )

                    Icon(
                        painter = painterResource(
                            if (libraryMode == XvoxHomeLibraryMode.PLAYLISTS) {
                                R.drawable.ic_xvox_music_note
                            } else {
                                R.drawable.ic_xvox_playlist
                            }
                        ),
                        contentDescription = "Playlists",
                        tint = if (libraryMode == XvoxHomeLibraryMode.PLAYLISTS) colors.primaryAccent else colors.primaryText,
                        modifier = Modifier
                            .size(36.dp)
                            .xvoxPressScale(pressedScale = 0.90f) { onPlaylistClick() }
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}
