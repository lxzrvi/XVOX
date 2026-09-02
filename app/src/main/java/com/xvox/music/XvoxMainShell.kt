package com.xvox.music

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.miniplayer.XvoxMiniPlayer
import com.xvox.music.core.ui.navigation.XvoxBottomBar
import com.xvox.music.core.ui.navigation.XvoxDestination
import com.xvox.music.features.home.HomeScreen
import com.xvox.music.features.search.SearchScreen
import com.xvox.music.features.settings.SettingsScreen
import com.xvox.music.player.playback.MainPlayerViewModel

@Composable
fun XvoxMainShell(
    playerViewModel:
        MainPlayerViewModel =
        viewModel()
) {
    val colors =
        XvoxTheme.colors

    val player by
        playerViewModel.state
            .collectAsState()

    var destination by remember {
        mutableStateOf(
            XvoxDestination.HOME
        )
    }

    val navigationGap = 28.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                colors.background
            )
    ) {
        when (destination) {
            XvoxDestination.HOME -> {
                HomeScreen(
                    currentSongId =
                        player.currentSongId,
                    isPlaying =
                        player.isPlaying,
                    onQueueReady =
                        playerViewModel::setQueue,
                    onPlay =
                        playerViewModel::play
                )
            }

            XvoxDestination.SEARCH -> {
                SearchScreen()
            }

            XvoxDestination.SETTINGS -> {
                SettingsScreen()
            }
        }

        val currentSongId =
            player.currentSongId

        if (
            player.miniPlayerVisible &&
            currentSongId != null &&
            player.queue.isNotEmpty()
        ) {
            XvoxMiniPlayer(
                queue =
                    player.queue,
                currentSongId =
                    currentSongId,
                currentIndex =
                    player.currentIndex,
                isPlaying =
                    player.isPlaying,
                position =
                    player.position,
                duration =
                    player.duration,
                riseKey =
                    player.miniPlayerRiseKey,
                togglePlay =
                    playerViewModel::
                        togglePlay,
                playQueueIndex =
                    playerViewModel::
                        playQueueIndex,
                stopAndDismiss =
                    playerViewModel::
                        stopPlayback,
                openPlayer = {
                    playerViewModel
                        .hideMiniPlayer()
                },
                onLike = {},
                onAdd = {},
                modifier = Modifier
                    .align(
                        Alignment.BottomCenter
                    )
                    .windowInsetsPadding(
                        WindowInsets
                            .navigationBars
                    )
                    .padding(
                        start = 6.dp,
                        end = 6.dp,
                        bottom = 120.dp
                    )
            )
        }

        XvoxBottomBar(
            selected =
                destination,
            onSelected = {
                destination = it
            },
            modifier = Modifier
                .align(
                    Alignment.BottomCenter
                )
                .windowInsetsPadding(
                    WindowInsets
                        .navigationBars
                )
                .padding(
                    bottom =
                        navigationGap
                )
        )
    }
}
