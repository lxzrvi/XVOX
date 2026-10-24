package com.xvox.music.features.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.xvox.music.core.ui.overlay.XvoxOverlayController

fun showLibraryRefresh(
    overlays: XvoxOverlayController,
    viewModel: HomeViewModel,
) {
    overlays.showBox("Refresh library") {
        LibraryRefreshContent(
            overlays = overlays,
            viewModel = viewModel,
        )
    }
}

@Composable
private fun LibraryRefreshContent(
    overlays: XvoxOverlayController,
    viewModel: HomeViewModel,
) {
    var scanning by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<LibraryRefreshResult?>(null) }
    val liveState by viewModel.state.collectAsState()
    val scanInProgress = scanning || liveState.refreshing

    LibraryRefreshBox(
        // This is the unfiltered, physical-file-verified library count—not merely the cards that
        // happen to be visible after user filters are applied.
        currentTotal = liveState.verifiedDeviceSongCount,
        scanning = scanInProgress,
        result = result,
        onCancel = overlays::hideBox,
        onScan = {
            if (!scanInProgress) {
                scanning = true
                result = null
                // HomeViewModel invokes the callback only after the real MediaStore query has
                // completed (successfully or with an explicit failure). No timer/polling façade.
                viewModel.refresh { refreshed ->
                    result = refreshed
                    scanning = false
                }
            }
        },
    )
}
