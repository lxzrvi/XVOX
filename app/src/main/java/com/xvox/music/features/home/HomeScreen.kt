package com.xvox.music.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song

@Composable
fun HomeScreen(
    currentSongId: Long?,
    isPlaying: Boolean,
    onQueueReady: (List<Song>) -> Unit,
    onPlay: (Song) -> Unit,
    viewModel: HomeViewModel =
        viewModel()
) {
    val state by
        viewModel.state
            .collectAsState()

    val colors =
        XvoxTheme.colors

    val statusHeight =
        WindowInsets.statusBars
            .asPaddingValues()
            .calculateTopPadding()

    /*
     * Header controls:
     * 54dp
     *
     * Header lower extension:
     * 8dp
     *
     * Shared content gap:
     * 12dp
     */
    val headerSpace =
        statusHeight +
            54.dp +
            8.dp +
            HomeGeometry.sectionGap

    val screenHeight =
        LocalConfiguration.current
            .screenHeightDp.dp

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
        LazyColumn(
            modifier =
                Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    top =
                        headerSpace
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
                    onSongClick = { song ->
                        viewModel
                            .recordPlayedFromLibrary(
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
                        state.recentlyPlayed,
                    currentSongId =
                        currentSongId,
                    isPlaying =
                        isPlaying,
                    frontTransitionKey =
                        state
                            .recentFrontTransitionKey,
                    onSongClick = { song ->
                        viewModel
                            .recordPlayedFromRecent(
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
                        .heightIn(
                            min =
                                screenHeight
                        )
                )
            }
        }

        HomeGlassHeader(
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
