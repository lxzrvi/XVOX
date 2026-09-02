package com.xvox.music

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.navigation.XvoxBottomBar
import com.xvox.music.core.ui.navigation.XvoxDestination
import com.xvox.music.features.home.HomeScreen
import com.xvox.music.features.search.SearchScreen
import com.xvox.music.features.settings.SettingsScreen

@Composable
fun XvoxMainShell() {
    val colors =
        XvoxTheme.colors

    var destination by remember {
        mutableStateOf(
            XvoxDestination.HOME
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                colors.background
            )
    ) {
        Box(
            modifier =
                Modifier.fillMaxSize()
        ) {
            when (destination) {
                XvoxDestination.HOME ->
                    HomeScreen()

                XvoxDestination.SEARCH ->
                    SearchScreen()

                XvoxDestination.SETTINGS ->
                    SettingsScreen()
            }
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
                    bottom = 18.dp
                )
        )
    }
}
