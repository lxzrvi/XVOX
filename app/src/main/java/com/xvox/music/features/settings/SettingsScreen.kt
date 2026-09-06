package com.xvox.music.features.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.features.home.HomeFooter
import com.xvox.music.features.home.HomeGeometry
import com.xvox.music.features.home.HomeViewModel
import com.xvox.music.features.settings.sections.AppearanceSettingsSection
import com.xvox.music.features.settings.sections.BatteryOptimizationSection
import com.xvox.music.features.settings.sections.EqualizerSettingsSection
import com.xvox.music.features.settings.sections.PlaybackSettingsSection
import com.xvox.music.features.settings.sections.WidgetSettingsSection

enum class SettingsSubPage {
    MAIN,
    APPEARANCE,
    XVOX_MIX,
    PLAYBACK,
    BATTERY_OPT,
    WIDGET,
    ABOUT
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val colors = XvoxTheme.colors
    val state by settingsViewModel.state.collectAsState()
    var currentPage by remember { mutableStateOf(SettingsSubPage.MAIN) }

    BackHandler(enabled = currentPage != SettingsSubPage.MAIN) {
        currentPage = SettingsSubPage.MAIN
    }

    AnimatedContent(
        targetState = currentPage,
        transitionSpec = {
            fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(120))
        },
        label = "settings_page_transition",
        modifier = modifier.fillMaxSize()
    ) { targetPage ->
        if (targetPage == SettingsSubPage.MAIN) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "settings_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 12.dp,
                                end = 12.dp,
                                bottom = HomeGeometry.sectionGap
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Settings",
                            color = colors.primaryAccent,
                            fontSize = 16.sp,
                            lineHeight = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                item(key = "menu_appearance") {
                    SettingsMenuCard(
                        title = "Appearance",
                        subtitle = "Themes, accent colors, grid layout & font size",
                        iconRes = R.drawable.ic_xvox_sparkle,
                        onClick = { currentPage = SettingsSubPage.APPEARANCE }
                    )
                }

                item(key = "menu_xvox_mix") {
                    SettingsMenuCard(
                        title = "XvoxMix (Equalizer)",
                        subtitle = "Hardware EQ, vertical bands, 3D surround & balance",
                        iconRes = R.drawable.ic_xvox_equalizer,
                        onClick = { currentPage = SettingsSubPage.XVOX_MIX }
                    )
                }

                item(key = "menu_playback") {
                    SettingsMenuCard(
                        title = "Playback",
                        subtitle = "Crossfade transition & headset connect actions",
                        iconRes = R.drawable.ic_xvox_disc,
                        onClick = { currentPage = SettingsSubPage.PLAYBACK }
                    )
                }

                item(key = "menu_battery") {
                    SettingsMenuCard(
                        title = "Don't Kill App",
                        subtitle = "Background playback & battery optimization exemption",
                        iconRes = R.drawable.ic_xvox_timer,
                        onClick = { currentPage = SettingsSubPage.BATTERY_OPT }
                    )
                }

                item(key = "menu_widget") {
                    SettingsMenuCard(
                        title = "Widget Customizer",
                        subtitle = "Home screen widget styling, transparency & corners",
                        iconRes = R.drawable.ic_xvox_settings,
                        onClick = { currentPage = SettingsSubPage.WIDGET }
                    )
                }

                item(key = "menu_about") {
                    SettingsMenuCard(
                        title = "About XVOX",
                        subtitle = "Modern lossless local music engine v1.0",
                        iconRes = R.drawable.ic_xvox_info,
                        onClick = { currentPage = SettingsSubPage.ABOUT }
                    )
                }

                item(key = "settings_footer") {
                    Spacer(Modifier.height(16.dp))
                    HomeFooter(modifier = Modifier.fillMaxWidth().height(180.dp).padding(bottom = 20.dp))
                }

                item(key = "settings_inset") { Spacer(Modifier.height(80.dp)) }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "sub_page_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(colors.card)
                                .clickable { currentPage = SettingsSubPage.MAIN },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_xvox_arrow_left),
                                contentDescription = "Back",
                                tint = colors.primaryText,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(Modifier.width(10.dp))

                        Text(
                            text = when (targetPage) {
                                SettingsSubPage.APPEARANCE -> "Appearance"
                                SettingsSubPage.XVOX_MIX -> "XvoxMix"
                                SettingsSubPage.PLAYBACK -> "Playback"
                                SettingsSubPage.BATTERY_OPT -> "Don't Kill App"
                                SettingsSubPage.WIDGET -> "Widget Customizer"
                                SettingsSubPage.ABOUT -> "About XVOX"
                                else -> "Settings"
                            },
                            color = colors.primaryText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                item(key = "sub_page_content") {
                    Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                        when (targetPage) {
                            SettingsSubPage.APPEARANCE -> AppearanceSettingsSection(state = state, viewModel = settingsViewModel)
                            SettingsSubPage.XVOX_MIX -> EqualizerSettingsSection(state = state, viewModel = settingsViewModel)
                            SettingsSubPage.PLAYBACK -> PlaybackSettingsSection(state = state, viewModel = settingsViewModel)
                            SettingsSubPage.BATTERY_OPT -> BatteryOptimizationSection()
                            SettingsSubPage.WIDGET -> WidgetSettingsSection(state = state, viewModel = settingsViewModel)
                            SettingsSubPage.ABOUT -> AboutSection()
                            else -> {}
                        }
                    }
                }

                item(key = "sub_page_inset") { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun SettingsMenuCard(
    title: String,
    subtitle: String,
    iconRes: Int,
    onClick: () -> Unit
) {
    val colors = XvoxTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.card)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(colors.cardElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = colors.primaryAccent,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.primaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = colors.secondaryText,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }

        Icon(
            painter = painterResource(R.drawable.ic_xvox_caret_right),
            contentDescription = null,
            tint = colors.mutedText,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun AboutSection() {
    val colors = XvoxTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.card)
            .padding(16.dp)
    ) {
        Text(
            text = "XVOX Local Music",
            color = colors.primaryText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Clean, high-performance offline music player crafted with Jetpack Compose, official Inter typography, custom XvoxMix DSP, and Media3 audio pipeline.",
            color = colors.secondaryText,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Version 1.0 (Inter Edition)",
            color = colors.mutedText,
            fontSize = 11.sp
        )
    }
}
