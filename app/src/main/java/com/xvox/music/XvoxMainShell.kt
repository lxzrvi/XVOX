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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.miniplayer.XvoxMiniPlayer
import com.xvox.music.core.ui.miniplayer.XvoxMiniPlayerPlacement
import com.xvox.music.core.ui.navigation.XvoxBottomBar
import com.xvox.music.core.ui.navigation.XvoxDestination
import com.xvox.music.features.home.HomeScreen
import com.xvox.music.features.search.SearchScreen
import com.xvox.music.features.settings.SettingsScreen
import com.xvox.music.player.nowplaying.XvoxNowPlaying
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

    /*
     * Resolve the actual Song object from the queue.
     *
     * currentIndex is preferred because it is O(1).
     * ID fallback handles temporary index synchronization.
     */
    val currentSong =
        player.queue.getOrNull(
            player.currentIndex
        )
            ?: player.currentSongId
                ?.let {
                    currentId ->

                    player.queue
                        .firstOrNull {
                            song ->

                            song.id ==
                                currentId
                        }
                }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                colors.background
            )
    ) {
        /*
         * ====================================================
         * MAIN DESTINATION CONTENT
         * ====================================================
         */

        Box(
            modifier =
                Modifier.fillMaxSize()
        ) {
            when (destination) {
                XvoxDestination.HOME -> {
                    HomeScreen(
                        currentSongId =
                            player.currentSongId,
                        isPlaying =
                            player.isPlaying,
                        onQueueReady =
                            playerViewModel::
                                setQueue,
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
        }

        /*
         * ====================================================
         * NORMAL SHELL OVERLAYS
         * ====================================================
         *
         * MiniPlayer + Navbar are hidden while Now Playing
         * occupies the screen.
         */

        if (
            !player.nowPlayingVisible
        ) {
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

                    /*
                     * MiniPlayer TAP / swipe UP:
                     *
                     * MiniPlayer exits first through its own
                     * motion and then opens Now Playing.
                     */
                    openPlayer =
                        playerViewModel::
                            openNowPlaying,

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
                            start =
                                XvoxMiniPlayerPlacement
                                    .horizontalEdge,

                            end =
                                XvoxMiniPlayerPlacement
                                    .horizontalEdge,

                            bottom =
                                XvoxMiniPlayerPlacement
                                    .miniPlayerBottom
                        )
                )
            }

            /*
             * Navbar remains shell-level and independent
             * from Home/library state.
             */
            XvoxBottomBar(
                selected =
                    destination,

                onSelected = {
                    selected ->

                    destination =
                        selected
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
                            XvoxMiniPlayerPlacement
                                .navigationHostBottom
                    )
            )
        }

        /*
         * ====================================================
         * NOW PLAYING
         * ====================================================
         *
         * Uses the SAME player state/controller.
         *
         * No duplicate ExoPlayer.
         */

        if (
            player.nowPlayingVisible &&
            currentSong != null
        ) {
            XvoxNowPlaying(
                song =
                    currentSong,

                currentIndex =
                    player.currentIndex,

                queueSize =
                    player.queue.size,

                isPlaying =
                    player.isPlaying,

                position =
                    player.position,

                duration =
                    player.duration,

                /*
                 * Arrow-down / successful downward gesture:
                 *
                 * Now Playing exits.
                 * Playback continues.
                 * MiniPlayer returns.
                 */
                onClose =
                    playerViewModel::
                        closeNowPlaying,

                onTogglePlay =
                    playerViewModel::
                        togglePlay,

                onPrevious =
                    playerViewModel::
                        playPrevious,

                onNext =
                    playerViewModel::
                        playNext,

                onSeek =
                    playerViewModel::
                        seekTo,

                modifier =
                    Modifier.fillMaxSize()
            )
        }
    }
}
