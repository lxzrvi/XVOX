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
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.data.preferences.UserPreferences

@Composable
fun HomeGlassHeader(
    profile: UserPreferences,
    showPlaylists: Boolean,
    onRefresh: () -> Unit,
    onHeartClick: () -> Unit,
    onLibraryModeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors =
        XvoxTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to colors.surface
                            .copy(alpha = 0.82f),

                        0.72f to colors.surface
                            .copy(alpha = 0.68f),

                        1f to colors.surface
                            .copy(alpha = 0f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.statusBars
                )
        ) {
            HomeHeader(
                profile = profile,
                showPlaylists =
                    showPlaylists,
                onRefresh = onRefresh,
                onHeartClick =
                    onHeartClick,
                onLibraryModeClick =
                    onLibraryModeClick
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
            )
        }
    }
}
