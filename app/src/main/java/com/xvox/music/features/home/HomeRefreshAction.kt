package com.xvox.music.features.home

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.xvox.music.core.ui.overlay.XvoxOverlayController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun showLibraryRefresh(
    overlays: XvoxOverlayController,
    viewModel: HomeViewModel,
) {
    overlays.showBox("Refresh library") {
        LibraryRefreshContent(
            overlays =
            overlays,
            viewModel =
            viewModel,
        )
    }
}

@Composable
private fun LibraryRefreshContent(
    overlays: XvoxOverlayController,
    viewModel: HomeViewModel,
) {
    var scanning by
        remember {
            mutableStateOf(false)
        }

    var result by
        remember {
            mutableStateOf<LibraryRefreshResult?>(null)
        }

    val scope = rememberCoroutineScope()
    val liveState by viewModel.state.collectAsState()

    LibraryRefreshBox(
        currentTotal =
            liveState.songs.size,
        scanning =
        scanning,
        result =
        result,
        onCancel =
            overlays::hideBox,
        onScan = {
            if (!scanning) {
                scanning = true

                scope.launch {
                    val start = System.currentTimeMillis()

                    var pendingResult: LibraryRefreshResult? = null

                    viewModel.refresh { refreshed ->
                        pendingResult = refreshed
                    }

                    while (pendingResult == null) {
                        delay(100L)
                        if (!viewModel.state.value.refreshing && pendingResult == null) {
                            delay(200L)
                        }
                    }

                    val elapsed = System.currentTimeMillis() - start
                    val remaining = 3000L - elapsed
                    if (remaining > 0) {
                        delay(remaining)
                    }

                    result = pendingResult
                    scanning = false
                }
            }
        },
    )
}
