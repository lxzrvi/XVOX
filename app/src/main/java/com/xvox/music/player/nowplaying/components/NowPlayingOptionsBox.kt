package com.xvox.music.player.nowplaying.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import com.xvox.music.core.ui.overlay.XvoxBox
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.sections.EqualizerSettingsSection
import com.xvox.music.features.settings.sections.PlaybackSettingsSection

@Composable
fun NowPlayingOptionsBox(
    onDismiss: () -> Unit,
    onTimer: (() -> Unit)? = null,
    onInfo: (() -> Unit)? = null,
    onStarPlaylist: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val state by settingsViewModel.state.collectAsState()
    XvoxBox(onDismiss = onDismiss, title = "Now playing · XvoxMix") {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                onTimer?.let { QuickOptionButton("Sleep timer", R.drawable.ic_xvox_timer, Modifier.weight(1f)) { onDismiss(); it() } }
                onInfo?.let { QuickOptionButton("Song info", R.drawable.ic_xvox_info, Modifier.weight(1f)) { onDismiss(); it() } }
                onStarPlaylist?.let { QuickOptionButton("Playlist", R.drawable.ic_xvox_playlist, Modifier.weight(1f)) { onDismiss(); it() } }
                onShare?.let { QuickOptionButton("Share", R.drawable.ic_xvox_share, Modifier.weight(1f)) { onDismiss(); it() } }
            }
            Spacer(Modifier.height(16.dp))
            EqualizerSettingsSection(state, settingsViewModel)
            Spacer(Modifier.height(20.dp))
            PlaybackSettingsSection(state, settingsViewModel)
        }
    }
}

@Composable
private fun QuickOptionButton(
    title: String,
    iconRes: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = XvoxTheme.colors

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.card)
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = colors.primaryAccent,
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = title,
            color = colors.primaryText,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
