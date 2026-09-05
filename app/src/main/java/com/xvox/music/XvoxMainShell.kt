package com.xvox.music

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.core.design.theme.XvoxPersonalFont
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.miniplayer.XvoxMiniPlayer
import com.xvox.music.core.ui.miniplayer.XvoxMiniPlayerPlacement
import com.xvox.music.core.ui.miniplayer.XvoxPlayerTransitionMotion
import com.xvox.music.core.ui.navigation.XvoxBottomBar
import com.xvox.music.core.ui.navigation.XvoxDestination
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.features.home.CreatePlaylistBox
import com.xvox.music.features.home.HomeGreeting
import com.xvox.music.features.home.HomeProfileAvatar
import com.xvox.music.features.home.HomeScreen
import com.xvox.music.features.home.HomeViewModel
import com.xvox.music.features.home.LibraryRefreshBox
import com.xvox.music.features.home.ProfileEditorBox
import com.xvox.music.features.home.XvoxSongArtwork
import com.xvox.music.features.playlist.XvoxHomeLibraryMode
import com.xvox.music.features.search.SearchScreen
import com.xvox.music.features.settings.SettingsScreen
import com.xvox.music.player.nowplaying.XvoxNowPlaying
import com.xvox.music.player.playback.MainPlayerViewModel
import kotlinx.coroutines.launch

private val MainEase =
    CubicBezierEasing(
        0.2f,
        0.9f,
        0.1f,
        1f
    )

@Composable
fun XvoxMainShell(
    homeViewModel: HomeViewModel = viewModel(),
    playerViewModel: MainPlayerViewModel = viewModel()
) {
    val colors =
        XvoxTheme.colors

    val overlays =
        LocalXvoxOverlayController.current

    var destination by remember {
        mutableStateOf(
            XvoxDestination.HOME
        )
    }

    var homeResetKey by remember {
        mutableLongStateOf(0L)
    }

    var hoistedSelectedPlaylistId by remember {
        mutableStateOf<String?>(
            null
        )
    }

    val homeState by
        homeViewModel.state.collectAsState()

    val player by
        playerViewModel.state.collectAsState()

    val currentSong =
        player.currentSongId?.let { id ->
            player.queue.firstOrNull {
                it.id == id
            } ?: homeState.songs.firstOrNull {
                it.id == id
            }
        }

    val isInPlaylist =
        remember(
            currentSong?.id,
            homeState.playlists
        ) {
            val songId =
                currentSong?.id

            if (songId != null) {
                homeState.playlists.any {
                    songId in it.songIds
                }
            } else {
                false
            }
        }

    fun showProfileEditor() {
        overlays.showL {
            ProfileEditorBox(
                profile =
                    homeState.profile,
                onCancel = {
                    overlays.hideL()
                },
                onSave = {
                        name,
                        pfp,
                        uri ->

                    homeViewModel.saveProfile(
                        name,
                        pfp,
                        uri
                    ) {
                        overlays.hideL()
                        overlays.showP(
                            "Profile updated"
                        )
                    }
                }
            )
        }
    }

    fun showRefreshOverlay() {
        overlays.showL {
            LibraryRefreshBox(
                currentTotal =
                    homeState.songs.size,
                scanning =
                    homeState.refreshing,
                result = null,
                onScan = {
                    homeViewModel.refresh {
                            result ->

                        overlays.hideL()

                        overlays.showP(
                            "Library refreshed (${result.totalSongs} songs)"
                        )
                    }
                },
                onCancel = {
                    overlays.hideL()
                }
            )
        }
    }

    fun showAddCurrentSongToPlaylist(
        song: Song
    ) {
        overlays.showL {
            var query by remember {
                mutableStateOf("")
            }

            var selectedPlaylistId by remember {
                mutableStateOf<String?>(
                    null
                )
            }

            val filteredPlaylists =
                remember(
                    homeState.playlists,
                    query
                ) {
                    if (query.isBlank()) {
                        homeState.playlists
                    } else {
                        homeState.playlists.filter {
                            it.name.contains(
                                query,
                                ignoreCase = true
                            )
                        }
                    }
                }

            Column(
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 4.dp,
                            vertical = 2.dp
                        ),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Column(
                        modifier =
                            Modifier.weight(1f)
                    ) {
                        Text(
                            text =
                                "Add to Playlist",
                            color =
                                colors.primaryText,
                            fontSize = 17.sp,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            text = song.title,
                            color =
                                colors.secondaryText,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow =
                                TextOverflow.Ellipsis
                        )
                    }

                    if (
                        selectedPlaylistId !=
                        null
                    ) {
                        Text(
                            text = "1 selected",
                            color =
                                colors.primaryAccent,
                            fontSize = 12.sp,
                            fontWeight =
                                FontWeight.SemiBold
                        )
                    }
                }

                Spacer(
                    Modifier.height(8.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(
                            RoundedCornerShape(
                                10.dp
                            )
                        )
                        .background(
                            colors.card
                        )
                        .padding(
                            horizontal = 10.dp,
                            vertical = 9.dp
                        )
                ) {
                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Icon(
                            painter =
                                painterResource(
                                    R.drawable
                                        .ic_xvox_search
                                ),
                            contentDescription =
                                null,
                            tint =
                                colors.secondaryText,
                            modifier =
                                Modifier.size(
                                    16.dp
                                )
                        )

                        BasicTextField(
                            value = query,
                            onValueChange = {
                                query = it
                            },
                            singleLine = true,
                            textStyle =
                                TextStyle(
                                    color =
                                        colors.primaryText,
                                    fontSize = 13.sp
                                ),
                            cursorBrush =
                                SolidColor(
                                    colors.primaryAccent
                                ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(
                                    start = 8.dp
                                ),
                            decorationBox = {
                                    inner ->

                                if (
                                    query.isEmpty()
                                ) {
                                    Text(
                                        text =
                                            "Search playlists...",
                                        color =
                                            colors.mutedText,
                                        fontSize = 13.sp
                                    )
                                }

                                inner()
                            }
                        )

                        if (
                            query.isNotEmpty()
                        ) {
                            Icon(
                                painter =
                                    painterResource(
                                        R.drawable
                                            .ic_xvox_close
                                    ),
                                contentDescription =
                                    "Clear",
                                tint =
                                    colors.secondaryText,
                                modifier =
                                    Modifier
                                        .size(16.dp)
                                        .clickable(
                                            interactionSource =
                                                remember {
                                                    MutableInteractionSource()
                                                },
                                            indication =
                                                null
                                        ) {
                                            query = ""
                                        }
                            )
                        }
                    }
                }

                Spacer(
                    Modifier.height(8.dp)
                )

                if (
                    homeState.playlists
                        .isEmpty()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Text(
                            text =
                                "No playlists yet",
                            color =
                                colors.mutedText,
                            fontSize = 13.sp
                        )
                    }
                } else if (
                    filteredPlaylists
                        .isEmpty()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Text(
                            text =
                                "No matching playlists",
                            color =
                                colors.mutedText,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier =
                            Modifier.heightIn(
                                max = 320.dp
                            ),
                        verticalArrangement =
                            Arrangement.spacedBy(
                                4.dp
                            )
                    ) {
                        items(
                            items =
                                filteredPlaylists,
                            key = {
                                it.id
                            }
                        ) { playlist ->

                            val alreadyAdded =
                                song.id in
                                    playlist.songIds

                            val selected =
                                selectedPlaylistId ==
                                    playlist.id

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(
                                        RoundedCornerShape(
                                            12.dp
                                        )
                                    )
                                    .background(
                                        if (selected) {
                                            colors.card
                                                .copy(
                                                    alpha =
                                                        0.95f
                                                )
                                        } else {
                                            Color.Transparent
                                        }
                                    )
                                    .clickable(
                                        enabled =
                                            !alreadyAdded,
                                        interactionSource =
                                            remember {
                                                MutableInteractionSource()
                                            },
                                        indication =
                                            null
                                    ) {
                                        selectedPlaylistId =
                                            if (
                                                selected
                                            ) {
                                                null
                                            } else {
                                                playlist.id
                                            }
                                    }
                                    .padding(
                                        horizontal =
                                            8.dp,
                                        vertical =
                                            9.dp
                                    ),
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier =
                                        Modifier.weight(
                                            1f
                                        )
                                ) {
                                    Text(
                                        text =
                                            playlist.name,
                                        color =
                                            when {
                                                alreadyAdded ->
                                                    colors.mutedText

                                                selected ->
                                                    colors.primaryAccent

                                                else ->
                                                    colors.primaryText
                                            },
                                        fontSize =
                                            13.sp,
                                        fontWeight =
                                            FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow =
                                            TextOverflow.Ellipsis
                                    )

                                    Text(
                                        text =
                                            if (
                                                alreadyAdded
                                            ) {
                                                "Already added"
                                            } else {
                                                "${playlist.songIds.size} songs"
                                            },
                                        color =
                                            colors.secondaryText,
                                        fontSize =
                                            11.sp
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(
                                            24.dp
                                        )
                                        .clip(
                                            CircleShape
                                        )
                                        .background(
                                            when {
                                                alreadyAdded ->
                                                    colors.cardElevated

                                                selected ->
                                                    colors.primaryAccent

                                                else ->
                                                    Color.Transparent
                                            }
                                        )
                                        .border(
                                            width =
                                                1.5.dp,
                                            color =
                                                when {
                                                    selected ->
                                                        colors.primaryAccent

                                                    alreadyAdded ->
                                                        colors.mutedText

                                                    else ->
                                                        colors.cardBorder
                                                },
                                            shape =
                                                CircleShape
                                        ),
                                    contentAlignment =
                                        Alignment.Center
                                ) {
                                    if (
                                        selected ||
                                        alreadyAdded
                                    ) {
                                        Icon(
                                            painter =
                                                painterResource(
                                                    R.drawable
                                                        .ic_xvox_check
                                                ),
                                            contentDescription =
                                                null,
                                            tint =
                                                if (
                                                    selected
                                                ) {
                                                    colors.background
                                                } else {
                                                    colors.mutedText
                                                },
                                            modifier =
                                                Modifier.size(
                                                    14.dp
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(
                    Modifier.height(10.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 2.dp,
                            vertical = 4.dp
                        ),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            10.dp
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(
                                RoundedCornerShape(
                                    12.dp
                                )
                            )
                            .background(
                                colors.card
                            )
                            .clickable {
                                overlays.hideL()
                            },
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Text(
                            text = "Cancel",
                            color =
                                colors.primaryText,
                            fontSize = 13.sp,
                            fontWeight =
                                FontWeight.SemiBold
                        )
                    }

                    val canAdd =
                        selectedPlaylistId !=
                            null

                    Box(
                        modifier = Modifier
                            .weight(1.5f)
                            .height(44.dp)
                            .clip(
                                RoundedCornerShape(
                                    12.dp
                                )
                            )
                            .background(
                                if (canAdd) {
                                    colors.primaryAccent
                                } else {
                                    colors.cardElevated
                                }
                            )
                            .clickable(
                                enabled =
                                    canAdd,
                                interactionSource =
                                    remember {
                                        MutableInteractionSource()
                                    },
                                indication =
                                    null
                            ) {
                                val id =
                                    selectedPlaylistId
                                        ?: return@clickable

                                homeViewModel
                                    .addToPlaylist(
                                        id,
                                        song
                                    ) {
                                            updated ->

                                        if (
                                            updated !=
                                            null
                                        ) {
                                            overlays.hideL()

                                            overlays.showP(
                                                "Added to ${updated.name}"
                                            )
                                        }
                                    }
                            },
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Text(
                            text =
                                if (canAdd) {
                                    "Add to Playlist"
                                } else {
                                    "Select Playlist"
                                },
                            color =
                                if (canAdd) {
                                    colors.background
                                } else {
                                    colors.mutedText
                                },
                            fontSize = 13.sp,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    fun showMiniPlayerPlaylistPicker() {
        val song =
            currentSong ?: return

        showAddCurrentSongToPlaylist(
            song
        )
    }

    fun showQueueSheet() {
        overlays.showL {
            val listState =
                rememberLazyListState()

            val scope =
                rememberCoroutineScope()

            val density =
                LocalDensity.current

            val slotHeightPx =
                with(density) {
                    58.dp.toPx()
                }

            val localQueue =
                remember {
                    mutableStateListOf<Song>()
                        .apply {
                            addAll(
                                player.queue
                            )
                        }
                }

            var draggingSongId by remember {
                mutableStateOf<Long?>(null)
            }

            var dragOffsetY by remember {
                mutableFloatStateOf(0f)
            }

            /*
             * -1 = scroll toward top
             *  0 = no edge scroll
             *  1 = scroll toward bottom
             */
            var autoScrollDirection by remember {
                mutableIntStateOf(0)
            }

            /*
             * Continuous edge-hover auto scroll.
             * This continues even when the finger
             * is stationary at an edge.
             */
            LaunchedEffect(
                draggingSongId,
                autoScrollDirection
            ) {
                if (
                    draggingSongId == null ||
                    autoScrollDirection == 0
                ) {
                    return@LaunchedEffect
                }

                while (
                    draggingSongId != null &&
                    autoScrollDirection != 0
                ) {
                    val amount =
                        12f *
                            autoScrollDirection

                    val consumed =
                        listState.scrollBy(
                            amount
                        )

                    /*
                     * Keep dragged card visually
                     * attached to stationary finger
                     * while list moves underneath.
                     */
                    dragOffsetY -= consumed

                    /*
                     * Re-evaluate crossed slots while
                     * auto scrolling.
                     */
                    val songId =
                        draggingSongId
                            ?: break

                    var from =
                        localQueue
                            .indexOfFirst {
                                it.id == songId
                            }

                    if (from < 0) {
                        break
                    }

                    while (
                        dragOffsetY >
                        slotHeightPx / 2f &&
                        from <
                        localQueue.lastIndex
                    ) {
                        val moving =
                            localQueue.removeAt(
                                from
                            )

                        localQueue.add(
                            from + 1,
                            moving
                        )

                        playerViewModel
                            .moveQueueItem(
                                from,
                                from + 1
                            )

                        from++

                        dragOffsetY -=
                            slotHeightPx
                    }

                    while (
                        dragOffsetY <
                        -slotHeightPx / 2f &&
                        from > 0
                    ) {
                        val moving =
                            localQueue.removeAt(
                                from
                            )

                        localQueue.add(
                            from - 1,
                            moving
                        )

                        playerViewModel
                            .moveQueueItem(
                                from,
                                from - 1
                            )

                        from--

                        dragOffsetY +=
                            slotHeightPx
                    }

                    kotlinx.coroutines.delay(
                        16L
                    )
                }
            }

    fun showTimerSheet() {
        overlays.showL {
            TimerSheetContent(
                currentMinutes =
                    player.sleepTimerMinutes,
                onSetMinutes = {
                        minutes ->

                    playerViewModel
                        .setSleepTimer(
                            minutes
                        )

                    overlays.hideL()

                    overlays.showP(
                        "Timer set $minutes min"
                    )
                },
                onCustom = {
                        minutes,
                        seconds,
                        pause,
                        closeApp ->

                    playerViewModel
                        .setCustomSleepTimer(
                            minutes,
                            seconds,
                            pause,
                            closeApp
                        )

                    overlays.hideL()

                    val total =
                        minutes * 60 +
                            seconds

                    if (total > 0) {
                        overlays.showP(
                            "Custom timer ${minutes}m ${seconds}s"
                        )
                    }
                },
                onCancel = {
                    playerViewModel
                        .cancelSleepTimer()

                    overlays.hideL()

                    overlays.showP(
                        "Timer off"
                    )
                }
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
        Column(
            modifier =
                Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color.Transparent
                    )
                    .windowInsetsPadding(
                        WindowInsets.statusBars
                    )
                    .padding(
                        bottom = 6.dp
                    )
            ) {
                Row(
                    modifier = Modifier
                        .padding(
                            horizontal = 14.dp
                        )
                        .height(
                            54.dp
                        ),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    HomeProfileAvatar(
                        profile =
                            homeState.profile,
                        modifier =
                            Modifier.size(
                                42.dp
                            ),
                        onClick = {
                            showProfileEditor()
                        }
                    )

                    Spacer(
                        modifier =
                            Modifier.width(
                                10.dp
                            )
                    )

                    Column(
                        modifier =
                            Modifier.weight(
                                1f
                            )
                    ) {
                        Text(
                            text =
                                homeState.profile.username,
                            color =
                                colors.primaryAccent,
                            fontFamily =
                                XvoxPersonalFont,
                            fontSize =
                                18.sp,
                            lineHeight =
                                19.sp,
                            maxLines = 1,
                            overflow =
                                TextOverflow.Ellipsis
                        )

                        HomeGreeting()
                    }

                    AnimatedVisibility(
                        visible =
                            destination ==
                                XvoxDestination.HOME,
                        enter =
                            slideInVertically(
                                initialOffsetY = {
                                    -it
                                },
                                animationSpec =
                                    tween(
                                        380,
                                        easing =
                                            MainEase
                                    )
                            ) +
                                fadeIn(
                                    tween(
                                        280
                                    )
                                ),
                        exit =
                            slideOutVertically(
                                targetOffsetY = {
                                    -it
                                },
                                animationSpec =
                                    tween(
                                        340,
                                        easing =
                                            MainEase
                                    )
                            ) +
                                fadeOut(
                                    tween(
                                        240
                                    )
                                )
                    ) {
                        val actionShape =
                            RoundedCornerShape(
                                21.dp
                            )

                        Row(
                            modifier = Modifier
                                .height(
                                    42.dp
                                )
                                .clip(
                                    actionShape
                                )
                                .background(
                                    colors.card
                                        .copy(
                                            alpha =
                                                0.72f
                                        )
                                )
                                .border(
                                    width =
                                        0.65.dp,
                                    color =
                                        colors.cardBorder,
                                    shape =
                                        actionShape
                                )
                                .padding(
                                    horizontal =
                                        2.dp
                                ),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            Icon(
                                painter =
                                    painterResource(
                                        R.drawable
                                            .ic_xvox_refresh
                                    ),
                                contentDescription =
                                    "Refresh Library",
                                tint =
                                    colors.primaryText,
                                modifier =
                                    Modifier
                                        .size(
                                            36.dp
                                        )
                                        .xvoxPressScale(
                                            pressedScale =
                                                0.90f
                                        ) {
                                            showRefreshOverlay()
                                        }
                                        .padding(
                                            8.dp
                                        )
                            )

                            Icon(
                                painter =
                                    painterResource(
                                        R.drawable
                                            .ic_xvox_heart
                                    ),
                                contentDescription =
                                    "Liked Songs",
                                tint =
                                    if (
                                        homeState.libraryMode ==
                                        XvoxHomeLibraryMode.LIKED
                                    ) {
                                        colors.primaryAccent
                                    } else {
                                        colors.primaryText
                                    },
                                modifier =
                                    Modifier
                                        .size(
                                            36.dp
                                        )
                                        .xvoxPressScale(
                                            pressedScale =
                                                0.90f
                                        ) {
                                            hoistedSelectedPlaylistId =
                                                null

                                            homeViewModel
                                                .toggleLikedMode()
                                        }
                                        .padding(
                                            8.dp
                                        )
                            )

                            Icon(
                                painter =
                                    painterResource(
                                        if (
                                            homeState.libraryMode ==
                                            XvoxHomeLibraryMode.PLAYLISTS
                                        ) {
                                            R.drawable
                                                .ic_xvox_music_note
                                        } else {
                                            R.drawable
                                                .ic_xvox_playlist
                                        }
                                    ),
                                contentDescription =
                                    "Playlists",
                                tint =
                                    if (
                                        homeState.libraryMode ==
                                        XvoxHomeLibraryMode.PLAYLISTS
                                    ) {
                                        colors.primaryAccent
                                    } else {
                                        colors.primaryText
                                    },
                                modifier =
                                    Modifier
                                        .size(
                                            36.dp
                                        )
                                        .xvoxPressScale(
                                            pressedScale =
                                                0.90f
                                        ) {
                                            hoistedSelectedPlaylistId =
                                                null

                                            homeViewModel
                                                .togglePlaylistMode()
                                        }
                                        .padding(
                                            8.dp
                                        )
                            )
                        }
                    }
                }
            }

            Box(
                modifier =
                    Modifier
                        .weight(
                            1f
                        )
                        .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState =
                        destination,
                    transitionSpec = {
                        (
                            fadeIn(
                                animationSpec =
                                    tween(
                                        220
                                    )
                            ) +
                                slideInHorizontally(
                                    animationSpec =
                                        tween(
                                            220
                                        )
                                ) {
                                    if (
                                        targetState.ordinal >
                                        initialState.ordinal
                                    ) {
                                        50
                                    } else {
                                        -50
                                    }
                                }
                            )
                            .togetherWith(
                                fadeOut(
                                    animationSpec =
                                        tween(
                                            160
                                        )
                                )
                            )
                    },
                    label =
                        "tab_switch_transition",
                    modifier =
                        Modifier.fillMaxSize()
                ) {
                        targetDestination ->

                    when (
                        targetDestination
                    ) {
                        XvoxDestination.HOME -> {
                            HomeScreen(
                                currentSongId =
                                    player.currentSongId,
                                isPlaying =
                                    player.isPlaying,
                                homeResetKey =
                                    homeResetKey,
                                selectedPlaylistId =
                                    hoistedSelectedPlaylistId,
                                onSelectedPlaylistIdChange = {
                                    hoistedSelectedPlaylistId =
                                        it
                                },
                                onQueueReady =
                                    playerViewModel::setQueue,
                                onPlay =
                                    playerViewModel::play,
                                playerViewModel =
                                    playerViewModel
                            )
                        }

                        XvoxDestination.SEARCH -> {
                            SearchScreen(
                                homeViewModel =
                                    homeViewModel,
                                playerViewModel =
                                    playerViewModel,
                                onPlaylistSelected = {
                                        playlistId ->

                                    hoistedSelectedPlaylistId =
                                        playlistId

                                    destination =
                                        XvoxDestination.HOME
                                }
                            )
                        }

                        XvoxDestination.SETTINGS -> {
                            SettingsScreen(
                                homeViewModel =
                                    homeViewModel
                            )
                        }
                    }
                }
            }
        }

        val currentSongId =
            player.currentSongId

        val miniVisibleBase =
            player.miniPlayerVisible &&
                !player.nowPlayingVisible &&
                currentSongId != null &&
                player.queue.isNotEmpty()

        val miniVisible =
            miniVisibleBase &&
                destination !=
                XvoxDestination.SETTINGS

        AnimatedVisibility(
            visible =
                miniVisible,
            enter =
                slideInVertically(
                    animationSpec =
                        tween(
                            durationMillis =
                                XvoxPlayerTransitionMotion.Duration,
                            easing =
                                XvoxPlayerTransitionMotion.easing
                        )
                ) {
                    it
                } +
                    fadeIn(
                        animationSpec =
                            tween(
                                durationMillis =
                                    XvoxPlayerTransitionMotion.Duration,
                                easing =
                                    XvoxPlayerTransitionMotion.easing
                            )
                    ),
            exit =
                slideOutVertically(
                    animationSpec =
                        tween(
                            durationMillis =
                                XvoxPlayerTransitionMotion.Duration,
                            easing =
                                XvoxPlayerTransitionMotion.easing
                        )
                ) {
                    it
                } +
                    fadeOut(
                        animationSpec =
                            tween(
                                durationMillis =
                                    XvoxPlayerTransitionMotion.Duration,
                                easing =
                                    XvoxPlayerTransitionMotion.easing
                            )
                    ),
            modifier =
                Modifier.align(
                    Alignment.BottomCenter
                )
        ) {
            val visibleSongId =
                if (
                    miniVisibleBase
                ) {
                    currentSongId
                } else {
                    null
                }

            if (
                visibleSongId !=
                null
            ) {
                val density =
                    LocalDensity.current

                val imeBottomPx =
                    WindowInsets.ime
                        .getBottom(
                            density
                        )

                val navBottomPx =
                    WindowInsets
                        .navigationBars
                        .getBottom(
                            density
                        )

                val keyboardOpen =
                    imeBottomPx > 0

                val effectiveImeDp =
                    with(density) {
                        (
                            imeBottomPx -
                                navBottomPx
                            )
                            .coerceAtLeast(
                                0
                            )
                            .toDp()
                    }

                val animatedBottomPadding by
                    animateDpAsState(
                        targetValue =
                            if (
                                keyboardOpen
                            ) {
                                effectiveImeDp +
                                    14.dp
                            } else {
                                96.dp
                            },
                        animationSpec =
                            tween(
                                durationMillis =
                                    280,
                                easing =
                                    MainEase
                            ),
                        label =
                            "miniPlayerBottomGap"
                    )

                val miniModifier =
                    Modifier
                        .navigationBarsPadding()
                        .padding(
                            start =
                                XvoxMiniPlayerPlacement.horizontalEdge,
                            end =
                                XvoxMiniPlayerPlacement.horizontalEdge,
                            bottom =
                                animatedBottomPadding
                        )

                XvoxMiniPlayer(
                    queue =
                        player.queue,
                    currentSongId =
                        visibleSongId,
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
                    togglePlay = {
                        playerViewModel
                            .togglePlay()
                    },
                    playQueueIndex = {
                        playerViewModel
                            .playQueueIndex(
                                it
                            )
                    },
                    stopAndDismiss = {
                        playerViewModel
                            .stopPlayback()
                    },
                    openPlayer = {
                        playerViewModel
                            .openNowPlaying()
                    },
                    onLike = {
                        currentSong?.let {
                                song ->

                            val wasLiked =
                                song.id in
                                    homeState.likedSongIds

                            homeViewModel
                                .toggleLiked(
                                    song
                                )

                            overlays.showP(
                                if (
                                    wasLiked
                                ) {
                                    "Removed from liked"
                                } else {
                                    "Added to liked"
                                }
                            )
                        }
                    },
                    onAdd = {
                        showMiniPlayerPlaylistPicker()
                    },
                    modifier =
                        miniModifier
                )
            }
        }

        Box(
            modifier = Modifier
                .align(
                    Alignment.BottomCenter
                )
                .navigationBarsPadding()
                .padding(
                    bottom = 10.dp
                )
        ) {
            XvoxBottomBar(
                selected =
                    destination,
                onSelected = {
                        next ->

                    if (
                        destination ==
                        XvoxDestination.HOME &&
                        next ==
                        XvoxDestination.HOME
                    ) {
                        hoistedSelectedPlaylistId =
                            null

                        homeResetKey =
                            System.currentTimeMillis()
                    }

                    destination =
                        next
                }
            )
        }

        /*
         * XvoxNowPlaying itself performs vertical
         * opening/closing translation.
         *
         * Parent only fades, preventing the previous
         * double vertical animation.
         */
        AnimatedVisibility(
            visible =
                player.nowPlayingVisible &&
                    currentSong != null,
            enter =
                fadeIn(
                    animationSpec =
                        tween(
                            durationMillis =
                                XvoxPlayerTransitionMotion.Duration,
                            easing =
                                XvoxPlayerTransitionMotion.easing
                        )
                ),
            exit =
                fadeOut(
                    animationSpec =
                        tween(
                            durationMillis =
                                XvoxPlayerTransitionMotion.Duration,
                            easing =
                                XvoxPlayerTransitionMotion.easing
                        )
                ),
            modifier =
                Modifier.fillMaxSize()
        ) {
            val playingSong =
                currentSong
                    ?: return@AnimatedVisibility

            XvoxNowPlaying(
                song =
                    playingSong,
                queue =
                    player.queue,
                currentIndex =
                    player.currentIndex,
                isPlaying =
                    player.isPlaying,
                position =
                    player.position,
                duration =
                    player.duration,
                onClose = {
                    playerViewModel
                        .closeNowPlaying()
                },
                onTogglePlay = {
                    playerViewModel
                        .togglePlay()
                },
                onPrevious = {
                    playerViewModel
                        .playPrevious()
                },
                onNext = {
                    playerViewModel
                        .playNext()
                },
                onPlayQueueIndex = {
                    playerViewModel
                        .playQueueIndex(
                            it
                        )
                },
                onSeek = {
                    playerViewModel
                        .seekTo(
                            it
                        )
                },
                isLiked =
                    playingSong.id in
                        homeState.likedSongIds,
                isInPlaylist =
                    isInPlaylist,
                onToggleLiked = {
                    val wasLiked =
                        playingSong.id in
                            homeState.likedSongIds

                    homeViewModel
                        .toggleLiked(
                            playingSong
                        )

                    overlays.showP(
                        if (
                            wasLiked
                        ) {
                            "Removed from liked"
                        } else {
                            "Added to liked"
                        }
                    )
                },
                onTimer = {
                    showTimerSheet()
                },
                onQueue = {
                    showQueueSheet()
                },
                onStarPlaylist = {
                    showAddCurrentSongToPlaylist(
                        playingSong
                    )
                },
                onInfo = {
                    homeViewModel
                        .loadInfo(
                            playingSong
                        ) {
                                info ->

                            overlays.showL {
                                com.xvox.music.features.home.SongInfoBox(
                                    info =
                                        info
                                )
                            }
                        }
                },
                isShuffleEnabled =
                    player.isShuffleEnabled,
                repeatMode =
                    player.repeatMode,
                onToggleShuffle = {
                    val wasEnabled =
                        player.isShuffleEnabled

                    playerViewModel
                        .toggleShuffle()

                    overlays.showP(
                        if (
                            wasEnabled
                        ) {
                            "Shuffle off"
                        } else {
                            "Shuffle on"
                        }
                    )
                },
                onToggleRepeat = {
                    playerViewModel
                        .toggleRepeat()
                },
                playerStyle =
                    player.playerStyle,
                sleepTimerProgress =
                    player.sleepTimerProgress,
                playingSource =
                    player.playingSource,
                modifier =
                    Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun TimerSheetContent(
    currentMinutes: Int?,
    onSetMinutes: (Int) -> Unit,
    onCustom: (
        Int,
        Int,
        Boolean,
        Boolean
    ) -> Unit,
    onCancel: () -> Unit
) {
    val colors =
        XvoxTheme.colors

    var showCustom by remember {
        mutableStateOf(false)
    }

    var minText by remember {
        mutableStateOf("")
    }

    var secText by remember {
        mutableStateOf("")
    }

    var pauseMusic by remember {
        mutableStateOf(true)
    }

    var closeApp by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 4.dp,
                vertical = 4.dp
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 4.dp,
                    vertical = 6.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Text(
                text =
                    "Sleep Timer",
                color =
                    colors.primaryText,
                fontSize = 18.sp,
                fontWeight =
                    FontWeight.Bold,
                modifier =
                    Modifier.weight(
                        1f
                    )
            )

            if (
                currentMinutes !=
                null
            ) {
                Text(
                    text =
                        "$currentMinutes min active",
                    color =
                        colors.primaryAccent,
                    fontSize = 12.sp,
                    fontWeight =
                        FontWeight.SemiBold
                )
            }
        }

        Spacer(
            Modifier.height(
                10.dp
            )
        )

        val presets =
            listOf(
                5,
                10,
                15,
                30,
                45,
                60
            )

        presets
            .chunked(3)
            .forEach { row ->

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                vertical =
                                    4.dp
                            ),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        )
                ) {
                    row.forEach {
                            minutes ->

                        val selected =
                            currentMinutes ==
                                minutes

                        Box(
                            modifier = Modifier
                                .weight(
                                    1f
                                )
                                .clip(
                                    RoundedCornerShape(
                                        12.dp
                                    )
                                )
                                .background(
                                    if (
                                        selected
                                    ) {
                                        colors.primaryAccent
                                    } else {
                                        colors.card
                                            .copy(
                                                alpha =
                                                    0.97f
                                            )
                                    }
                                )
                                .clickable(
                                    interactionSource =
                                        remember {
                                            MutableInteractionSource()
                                        },
                                    indication =
                                        null
                                ) {
                                    onSetMinutes(
                                        minutes
                                    )
                                }
                                .padding(
                                    vertical =
                                        14.dp
                                ),
                            contentAlignment =
                                Alignment.Center
                        ) {
                            Text(
                                text =
                                    "$minutes min",
                                color =
                                    if (
                                        selected
                                    ) {
                                        colors.background
                                    } else {
                                        colors.primaryText
                                    },
                                fontSize =
                                    13.sp,
                                fontWeight =
                                    FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

        Spacer(
            Modifier.size(
                6.dp
            )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(
                        12.dp
                    )
                )
                .background(
                    colors.card.copy(
                        alpha = 0.97f
                    )
                )
                .clickable(
                    interactionSource =
                        remember {
                            MutableInteractionSource()
                        },
                    indication =
                        null
                ) {
                    showCustom =
                        !showCustom
                }
                .padding(
                    horizontal = 14.dp,
                    vertical = 14.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Text(
                text =
                    "Custom time...",
                color =
                    colors.primaryText,
                fontSize = 14.sp,
                fontWeight =
                    FontWeight.SemiBold,
                modifier =
                    Modifier.weight(
                        1f
                    )
            )

            Icon(
                painter =
                    painterResource(
                        if (
                            showCustom
                        ) {
                            R.drawable
                                .ic_xvox_collapse
                        } else {
                            R.drawable
                                .ic_xvox_caret_right
                        }
                    ),
                contentDescription =
                    null,
                tint =
                    colors.secondaryText,
                modifier =
                    Modifier.size(
                        16.dp
                    )
            )
        }

        if (showCustom) {
            Spacer(
                Modifier.size(
                    8.dp
                )
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )
            ) {
                BasicTextField(
                    value =
                        minText,
                    onValueChange = {
                        if (
                            it.length <= 3 &&
                            it.all {
                                    character ->

                                character.isDigit()
                            }
                        ) {
                            minText = it
                        }
                    },
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType =
                                KeyboardType.Number
                        ),
                    textStyle =
                        TextStyle(
                            color =
                                colors.primaryText,
                            fontSize =
                                14.sp
                        ),
                    cursorBrush =
                        SolidColor(
                            colors.primaryAccent
                        ),
                    modifier = Modifier
                        .weight(1f)
                        .clip(
                            RoundedCornerShape(
                                10.dp
                            )
                        )
                        .background(
                            colors.card
                                .copy(
                                    alpha =
                                        0.97f
                                )
                        )
                        .padding(
                            horizontal =
                                12.dp,
                            vertical =
                                12.dp
                        ),
                    decorationBox = {
                            inner ->

                        if (
                            minText.isEmpty()
                        ) {
                            Text(
                                text =
                                    "Minutes",
                                color =
                                    colors.mutedText,
                                fontSize =
                                    14.sp
                            )
                        }

                        inner()
                    }
                )

                BasicTextField(
                    value =
                        secText,
                    onValueChange = {
                        if (
                            it.length <= 2 &&
                            it.all {
                                    character ->

                                character.isDigit()
                            }
                        ) {
                            secText = it
                        }
                    },
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType =
                                KeyboardType.Number
                        ),
                    textStyle =
                        TextStyle(
                            color =
                                colors.primaryText,
                            fontSize =
                                14.sp
                        ),
                    cursorBrush =
                        SolidColor(
                            colors.primaryAccent
                        ),
                    modifier = Modifier
                        .weight(1f)
                        .clip(
                            RoundedCornerShape(
                                10.dp
                            )
                        )
                        .background(
                            colors.card
                                .copy(
                                    alpha =
                                        0.97f
                                )
                        )
                        .padding(
                            horizontal =
                                12.dp,
                            vertical =
                                12.dp
                        ),
                    decorationBox = {
                            inner ->

                        if (
                            secText.isEmpty()
                        ) {
                            Text(
                                text =
                                    "Seconds",
                                color =
                                    colors.mutedText,
                                fontSize =
                                    14.sp
                            )
                        }

                        inner()
                    }
                )
            }

            Spacer(
                Modifier.size(
                    8.dp
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(
                        RoundedCornerShape(
                            10.dp
                        )
                    )
                    .clickable(
                        interactionSource =
                            remember {
                                MutableInteractionSource()
                            },
                        indication =
                            null
                    ) {
                        pauseMusic =
                            true

                        closeApp =
                            false
                    }
                    .padding(
                        vertical = 4.dp
                    ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                RadioButton(
                    selected =
                        pauseMusic,
                    onClick = {
                        pauseMusic =
                            true

                        closeApp =
                            false
                    }
                )

                Text(
                    text =
                        "Pause music",
                    color =
                        colors.primaryText,
                    fontSize =
                        13.sp
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(
                        RoundedCornerShape(
                            10.dp
                        )
                    )
                    .clickable(
                        interactionSource =
                            remember {
                                MutableInteractionSource()
                            },
                        indication =
                            null
                    ) {
                        closeApp =
                            true

                        pauseMusic =
                            false
                    }
                    .padding(
                        vertical = 4.dp
                    ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                RadioButton(
                    selected =
                        closeApp,
                    onClick = {
                        closeApp =
                            true

                        pauseMusic =
                            false
                    }
                )

                Text(
                    text =
                        "Close full app",
                    color =
                        colors.primaryText,
                    fontSize =
                        13.sp
                )
            }

            Spacer(
                Modifier.size(
                    8.dp
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(
                        RoundedCornerShape(
                            12.dp
                        )
                    )
                    .background(
                        colors.primaryAccent
                    )
                    .clickable(
                        interactionSource =
                            remember {
                                MutableInteractionSource()
                            },
                        indication =
                            null
                    ) {
                        val minutes =
                            minText
                                .toIntOrNull()
                                ?: 0

                        val seconds =
                            secText
                                .toIntOrNull()
                                ?: 0

                        if (
                            minutes == 0 &&
                            seconds == 0
                        ) {
                            return@clickable
                        }

                        onCustom(
                            minutes,
                            seconds,
                            pauseMusic,
                            closeApp
                        )
                    }
                    .padding(
                        vertical =
                            12.dp
                    ),
                horizontalArrangement =
                    Arrangement.Center
            ) {
                Text(
                    text =
                        "Start custom timer",
                    color =
                        colors.background,
                    fontSize =
                        14.sp,
                    fontWeight =
                        FontWeight.SemiBold
                )
            }
        }

        if (
            currentMinutes !=
            null
        ) {
            Spacer(
                Modifier.size(
                    8.dp
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(
                        RoundedCornerShape(
                            12.dp
                        )
                    )
                    .background(
                        colors.card.copy(
                            alpha =
                                0.97f
                        )
                    )
                    .clickable(
                        interactionSource =
                            remember {
                                MutableInteractionSource()
                            },
                        indication =
                            null
                    ) {
                        onCancel()
                    }
                    .padding(
                        horizontal =
                            14.dp,
                        vertical =
                            12.dp
                    ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Text(
                    text =
                        "Cancel Timer",
                    color =
                        Color(
                            0xFFDC2626
                        ),
                    fontSize =
                        14.sp,
                    fontWeight =
                        FontWeight.SemiBold,
                    modifier =
                        Modifier.weight(
                            1f
                        )
                )
            }
        }
    }
}
