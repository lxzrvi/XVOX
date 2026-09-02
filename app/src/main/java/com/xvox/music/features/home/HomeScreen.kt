package com.xvox.music.features.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.miniplayer.XvoxMiniPlayer

@Composable
fun HomeScreen(
    viewModel: HomeViewModel =
        viewModel()
) {
    val state by
        viewModel.state.collectAsState()

    val colors =
        XvoxTheme.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                colors.background
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                            onHeartClick = {},
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

        if (
            state.miniPlayerVisible
        ) {
            XvoxMiniPlayer(
                queue =
                    state.songs,
                currentSongId =
                    state.currentSongId,
                currentIndex =
                    state.currentIndex,
                isPlaying =
                    state.isPlaying,
                position =
                    state.playbackPosition,
                duration =
                    state.playbackDuration,
                togglePlay =
                    viewModel::togglePlay,
                playQueueIndex =
                    viewModel::playQueueIndex,
                openPlayer = {
                    // Now Playing milestone.
                },
                closePlayer =
                    viewModel::hideMiniPlayer,
                onLike = {
                    // Favorites persistence milestone.
                },
                onAdd = {
                    // Queue/category action milestone.
                },
                modifier = Modifier
                    .align(
                        Alignment.BottomCenter
                    )
                    .windowInsetsPadding(
                        WindowInsets.navigationBars
                    )
                    .padding(
                        start = 14.dp,
                        end = 14.dp,
                        bottom = 100.dp
                    )
            )
        }
    }
}
