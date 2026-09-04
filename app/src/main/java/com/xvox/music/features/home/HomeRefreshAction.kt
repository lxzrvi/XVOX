package com.xvox.music.features.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.xvox.music.core.ui.overlay.XvoxOverlayController

fun showLibraryRefresh(
    overlays: XvoxOverlayController,
    viewModel: HomeViewModel
) {
    overlays.showB {
        LibraryRefreshContent(
            overlays =
                overlays,
            viewModel =
                viewModel
        )
    }
}

@Composable
private fun LibraryRefreshContent(
    overlays: XvoxOverlayController,
    viewModel: HomeViewModel
) {
    var scanning by
        remember {
            mutableStateOf(false)
        }

    var result by
        remember {
            mutableStateOf<
                LibraryRefreshResult?
            >(null)
        }

    LibraryRefreshBox(
        currentTotal =
            viewModel.state.value
                .songs.size,
        scanning =
            scanning,
        result =
            result,
        onCancel =
            overlays::hideB,
        onScan = {
            if (!scanning) {
                scanning = true

                viewModel.refresh {
                    refreshed ->

                    result =
                        refreshed

                    scanning =
                        false
                }
            }
        }
    )
}
