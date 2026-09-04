package com.xvox.music.features.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.xvox.music.core.ui.overlay.XvoxOverlayController

fun showLibraryRefresh(
    overlays:
        XvoxOverlayController,
    viewModel:
        HomeViewModel
) {
    overlays.showB {
        LibraryRefreshContent(
            viewModel =
                viewModel
        )
    }
}

@Composable
private fun LibraryRefreshContent(
    viewModel: HomeViewModel
) {
    var result by
        remember {
            mutableStateOf<
                LibraryRefreshResult?
            >(null)
        }

    LaunchedEffect(Unit) {
        viewModel.refresh {
            refreshed ->

            result =
                refreshed
        }
    }

    LibraryRefreshBox(
        refreshing =
            result == null,
        result =
            result
    )
}
