package com.xvox.music.features.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.core.design.theme.XvoxTheme

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val colors = XvoxTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(
                WindowInsets.statusBars
                    .union(WindowInsets.navigationBars)
            ),
        verticalArrangement =
            Arrangement.Top
    ) {
        AnimatedContent(
            targetState = state.loading,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "homeLoading"
        ) { loading ->
            if (loading) {
                HomeSkeleton(
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 18.dp)
                ) {
                    HomeHeader(
                        profile = state.profile,
                        onRefresh = viewModel::refresh,
                        onMenuClick = {}
                    )

                    AllSongsSection(
                        songs = state.songs,
                        onSongClick = viewModel::play
                    )

                    RecentlyPlayedSection(
                        songs = state.recentlyPlayed,
                        onSongClick = viewModel::play
                    )
                }
            }
        }
    }
}
