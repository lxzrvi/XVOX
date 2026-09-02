package com.xvox.music.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.skydoves.cloudy.Sky
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.XvoxGlassStyle
import com.xvox.music.core.ui.effects.xvoxGlass
import com.xvox.music.data.preferences.UserPreferences

@Composable
fun HomeGlassHeader(
    sky: Sky,
    profile: UserPreferences,
    showPlaylists: Boolean,
    onRefresh: () -> Unit,
    onHeartClick: () -> Unit,
    onLibraryModeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {

        /*
         * =====================================================
         * HEADER GLASS
         * =====================================================
         *
         * Light backdrop blur.
         *
         * Low radius + low tint = less visual latency
         * and background remains clearly visible.
         */
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .xvoxGlass(
                    sky = sky,
                    style = XvoxGlassStyle.HEADER
                )
                .windowInsetsPadding(
                    WindowInsets.statusBars
                )
        ) {
            HomeHeader(
                sky = sky,
                profile = profile,
                showPlaylists = showPlaylists,
                onRefresh = onRefresh,
                onHeartClick = onHeartClick,
                onLibraryModeClick = onLibraryModeClick
            )
        }

        /*
         * =====================================================
         * SOFT BOTTOM FADE
         * =====================================================
         *
         * Keep this very subtle.
         * A strong gradient makes the header feel like a
         * separate solid layer and reduces the glass effect.
         */
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colors.surface.copy(
                                alpha = 0.14f
                            ),
                            colors.surface.copy(
                                alpha = 0f
                            )
                        )
                    )
                )
        )
    }
}
