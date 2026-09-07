package com.xvox.music.features.settings.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.miniplayer.XvoxMiniPlayerCard
import com.xvox.music.core.ui.navigation.XvoxBottomBar
import com.xvox.music.core.ui.navigation.XvoxDestination
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.features.home.*
import com.xvox.music.features.home.allsongs.allSongsItems
import com.xvox.music.features.home.allsongs.buildMosaicPagePlans
import com.xvox.music.features.home.recent.RecentTransitionRequest
import com.xvox.music.features.home.recent.XvoxRecentlyPlayedSection
import com.xvox.music.features.playlist.XvoxHomeLibraryMode
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.player.playback.MainPlayerUiState
import com.xvox.music.shell.XvoxShellTopHeader

@Composable
fun HomeSettingsPreview(state: SettingsState, library: HomeUiState, player: MainPlayerUiState) {
    val colors = XvoxTheme.colors
    val overlays = LocalXvoxOverlayController.current
    val density = LocalDensity.current
    val window = LocalWindowInfo.current.containerSize
    val width = with(density) { window.width.toDp() }.coerceAtLeast(320.dp)
    val height = with(density) { window.height.toDp() }.coerceAtLeast(600.dp)
    SettingsPreviewFrame(if (library.songs.isEmpty()) "Home preview · sample library" else "Home preview · your library") {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            UniformPreview(width, height, Modifier.fillMaxWidth().height(380.dp)) {
                HomePreviewSurface(state, library, player)
            }
        }
        Text("Same Home cards, covers and spacing. Scaled evenly—not compressed. Scroll inside to see the section order.",
            color = colors.secondaryText, fontSize = 11.sp)
        Text("Open larger preview ↗", color = colors.primaryAccent, fontSize = 12.sp,
            modifier = Modifier.xvoxPressScale {
                overlays.showBox("Home preview") {
                    UniformPreview(width, height, Modifier.fillMaxWidth().heightIn(max = 720.dp)) {
                        HomePreviewSurface(state, library, player)
                    }
                }
            }.padding(vertical = 6.dp))
    }
}

@Composable
private fun HomePreviewSurface(state: SettingsState, library: HomeUiState, player: MainPlayerUiState) {
    val colors = XvoxTheme.colors
    val context = LocalContext.current
    val songs = remember(library.songs) {
        library.songs.ifEmpty {
            List(16) { i -> Song(-(i + 1L), listOf("Night drive", "Open sky", "Afterglow", "Golden hour")[i % 4],
                "Preview artist", Uri.EMPTY, Uri.parse("android.resource://${context.packageName}/${R.drawable.xvox}"), 210000L) }
        }
    }
    val config = HomePresentation(state.homeLayoutStyle, state.homeScrollDirection, state.homeHorizontalRows,
        state.hideRecentlyPlayed, state.recentsPlacement, state.homeMerge, state.homeSectionOrder, state.homeHiddenSections)
    val plans = remember(songs, config.style, config.rows) {
        buildMosaicPagePlans(songs, config.rows, config.style == "uniform", config.style == "mosaic1")
    }
    val byId = remember(songs) { songs.associateBy { it.id } }
    val current = player.currentSongId?.let(byId::get)
    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)).background(colors.background)
        .border(.7.dp, colors.cardBorder, RoundedCornerShape(18.dp))) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 64.dp, bottom = if (current != null) 180.dp else 100.dp)) {
            HomeSections.visible(config).forEach { section -> when (section) {
                HomeSections.ALL -> allSongsItems(songs, plans, config, player.currentSongId, player.isPlaying, emptySet(), {}, {}, {})
                HomeSections.RECENT -> item(key = "recent") {
                    XvoxRecentlyPlayedSection(library.recentlyPlayed, player.currentSongId, player.isPlaying,
                        RecentTransitionRequest(), {}, {})
                }
                HomeSections.LIKED -> librarySongItems("liked", "Liked Songs", songs.filter { it.id in library.likedSongIds },
                    player.currentSongId, player.isPlaying, emptySet(), {}, {})
                HomeSections.PLAYLISTS -> playlistCollectionItems(library.playlists,
                    { playlist -> playlist.songIds.mapNotNull(byId::get) }, {}, {}, {})
            } }
        }
        XvoxShellTopHeader(library.profile.copy(username = library.profile.username.ifBlank { "Your name" }),
            XvoxDestination.HOME, XvoxHomeLibraryMode.ALL_SONGS, {}, {}, {}, {},
            mergedHome = state.homeMerge, useSystemInsets = false)
        if (current != null) XvoxMiniPlayerCard(current, player.isPlaying, player.position, player.duration, 0, {},
            Modifier.align(Alignment.BottomCenter).padding(start = 6.dp, end = 6.dp, bottom = 96.dp))
        XvoxBottomBar(XvoxDestination.HOME, {}, Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp))
    }
}
