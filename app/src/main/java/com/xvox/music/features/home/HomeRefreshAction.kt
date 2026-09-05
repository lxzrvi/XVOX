package com.xvox.music.features.home

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
    overlays.showL {
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
            mutableStateOf<
                LibraryRefreshResult?,
            >(null)
        }

    val scope = rememberCoroutineScope()

    LibraryRefreshBox(
        currentTotal =
            viewModel.state.value
                .songs.size,
        scanning =
        scanning,
        result =
        result,
        onCancel =
            overlays::hideL,
        onScan = {
            if (!scanning) {
                scanning = true

                scope.launch {
                    val start = System.currentTimeMillis()

                    var pendingResult: LibraryRefreshResult? = null

                    // Trigger refresh and capture result
                    viewModel.refresh { refreshed ->
                        pendingResult = refreshed
                    }

                    // Ensure minimum 3 seconds visible loading to avoid flash
                    // Refresh itself typically takes 1-2 sec, we ensure total 3000ms
                    // Poll until viewModel not refreshing or pendingResult set
                    while (pendingResult == null) {
                        delay(100L)
                        // If refresh finished quickly, pendingResult will be set via callback
                        // If viewModel still refreshing, wait
                        if (!viewModel.state.value.refreshing && pendingResult == null) {
                            // Edge: callback not yet delivered, wait a bit
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
