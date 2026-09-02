package com.xvox.music.features.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skydoves.cloudy.Sky
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song

@Composable
fun HomeScreen(
    sky: Sky,
    currentSongId: Long?,
    isPlaying: Boolean,
    onQueueReady: (List<Song>) -> Unit,
    onPlay: (Song) -> Unit,
    viewModel: HomeViewModel =
        viewModel()
) {
    val state by
        viewModel.state.collectAsState()

    val colors =
        XvoxTheme.colors

    val statusHeight =
        WindowInsets.statusBars
            .asPaddingValues()
            .calculateTopPadding()

    val headerSpace =
        statusHeight +
            72.dp

    LaunchedEffect(
        state.songs
    ) {
        if (
            state.songs.isNotEmpty()
        ) {
            onQueueReady(
                state.songs
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                colors.background
            )
    ) {
        AnimatedContent(
            targetState =
                state.loading,
            modifier =
                Modifier.fillMaxSize(),
            transitionSpec = {
                fadeIn() togetherWith
                    fadeOut()
            },
            label = "homeLoad"
        ) { loading ->

            if (loading) {
                HomeSkeleton(
                    modifier =
                        Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier =
                        Modifier.fillMaxSize(),
                    contentPadding =
                        PaddingValues(
                            top =
                                headerSpace,
                            bottom =
                                260.dp
                        )
                ) {
                    item(
                        key = "songs"
                    ) {
                        AllSongsSection(
                            songs =
                                state.songs,
                            currentSongId =
                                currentSongId,
                            isPlaying =
                                isPlaying,
                            onSongClick = {
                                song ->

                                viewModel
                                    .recordPlayed(
                                        song
                                    )

                                onPlay(song)
                            },
                            onPrefetch =
                                viewModel::
                                    prefetchFrom
                        )
                    }

                    item(
                        key = "recent"
                    ) {
                        RecentlyPlayedSection(
                            songs =
                                state
                                    .recentlyPlayed,
                            currentSongId =
                                currentSongId,
                            isPlaying =
                                isPlaying,
                            onSongClick = {
                                song ->

                                viewModel
                                    .recordPlayed(
                                        song
                                    )

                                onPlay(song)
                            }
                        )
                    }

                    item(
                        key = "footer"
                    ) {
                        HomeFooter(
                            modifier = Modifier
                                .fillParentMaxWidth()
                                .height(
                                    390.dp
                                )
                        )
                    }
                }
            }
        }

        HomeGlassHeader(
            sky = sky,
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
    }
}
