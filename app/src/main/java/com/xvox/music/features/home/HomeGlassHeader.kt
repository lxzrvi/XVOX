package com.xvox.music.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
                colors.background.copy(
                    alpha = 0.78f
                )
            )
            .windowInsetsPadding(
                WindowInsets.statusBars
            )
            .padding(
                bottom = 8.dp
            )
    ) {
        HomeHeader(
            profile = profile,
            showPlaylists =
                showPlaylists,
            onRefresh =
                onRefresh,
            onHeartClick =
                onHeartClick,
            onLibraryModeClick =
                onLibraryModeClick
        )
    }
}
