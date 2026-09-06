package com.xvox.music.features.settings.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.features.home.HomeViewModel
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel

@Composable
fun LibraryFilterSettingsSection(
    state: SettingsState,
    viewModel: SettingsViewModel,
    homeViewModel: HomeViewModel
) {
    val colors = XvoxTheme.colors
    val folders by homeViewModel.folders.collectAsState()
    var previewFolder by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Ignore Audio Shorter Than",
            color = colors.secondaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val secOptions = listOf(0 to "None", 15 to "15s", 30 to "30s", 60 to "60s")
            secOptions.forEach { (sec, label) ->
                val isSelected = state.ignoreBelowSec == sec
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isSelected) colors.primaryAccent else colors.cardElevated)
                        .clickable { viewModel.setIgnoreBelowSec(sec) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) colors.background else colors.primaryText,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Ignore Files Smaller Than",
            color = colors.secondaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val kbOptions = listOf(0 to "None", 100 to "100 KB", 500 to "500 KB", 1024 to "1 MB")
            kbOptions.forEach { (kb, label) ->
                val isSelected = state.ignoreBelowKb == kb
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isSelected) colors.primaryAccent else colors.cardElevated)
                        .clickable { viewModel.setIgnoreBelowKb(kb) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) colors.background else colors.primaryText,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Device Audio Folders",
            color = colors.secondaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (folders.isEmpty()) {
            Text(
                text = "No audio folders detected on device",
                color = colors.mutedText,
                fontSize = 11.sp,
                modifier = Modifier.padding(vertical = 6.dp)
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                folders.forEach { folder ->
                    val isIgnored = folder.name in state.ignoredFolders
                    val isPreviewing = previewFolder == folder.name

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.cardElevated)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (isIgnored) colors.cardBorder else colors.primaryAccent)
                                    .clickable { viewModel.toggleIgnoredFolder(folder.name) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (!isIgnored) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_xvox_check),
                                        contentDescription = null,
                                        tint = colors.background,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.width(10.dp))

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        previewFolder = if (isPreviewing) null else folder.name
                                    }
                            ) {
                                Text(
                                    text = folder.name,
                                    color = if (isIgnored) colors.mutedText else colors.primaryText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${folder.songCount} songs • Tap to view songs",
                                    color = colors.secondaryText,
                                    fontSize = 10.sp
                                )
                            }

                            Text(
                                text = if (isIgnored) "Excluded" else "Included",
                                color = if (isIgnored) colors.secondaryText else colors.primaryAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        AnimatedVisibility(
                            visible = isPreviewing,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, start = 34.dp)
                            ) {
                                folder.songs.take(10).forEach { song ->
                                    Text(
                                        text = "• ${song.title}",
                                        color = colors.secondaryText,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        maxLines = 1,
                                        modifier = Modifier.padding(vertical = 1.dp)
                                    )
                                }
                                if (folder.songs.size > 10) {
                                    Text(
                                        text = "... and ${folder.songs.size - 10} more",
                                        color = colors.mutedText,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
