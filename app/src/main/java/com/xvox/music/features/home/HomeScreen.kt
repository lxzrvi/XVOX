package com.xvox.music.features.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
    viewModel: HomeViewModel =
        viewModel()
) {
    val state by
        viewModel.state.collectAsState()

    val colors =
        XvoxTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                colors.background
            )
            .windowInsetsPadding(
                WindowInsets.statusBars
                    .union(
                        WindowInsets
                            .navigationBars
                    )
            )
    ) {
        AnimatedContent(
            targetState =
                state.loading,
            transitionSpec = {
                fadeIn(
                    tween(260)
                ) togetherWith
                    fadeOut(
                        tween(180)
                    )
            },
            label = "homeLoad"
        ) { loading ->

            if (loading) {
                HomeSkeleton(
                    modifier =
                        Modifier.fillMaxSize()
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = 4.dp
                        )
                ) {
                    HomeHeader(
                        profile =
                            state.profile,
                        showPlaylists =
                            state.showPlaylists,
                        onRefresh =
                            viewModel::refresh,
                        onHeartClick = {
                        },
                        onLibraryModeClick =
                            viewModel::
                                toggleLibraryMode
                    )

                    AllSongsSection(
                        songs =
                            state.songs,
                        currentSongId =
                            state.currentSongId,
                        isPlaying =
                            state.isPlaying,
                        onSongClick =
                            viewModel::play,
                        onPrefetch =
                            viewModel::prefetchFrom
                    )

                    RecentlyPlayedSection(
                        songs =
                            state.recentlyPlayed,
                        currentSongId =
                            state.currentSongId,
                        isPlaying =
                            state.isPlaying,
                        onSongClick =
                            viewModel::play
                    )
                }
            }
        }
    }
}
