package com.xvox.music.player.nowplaying.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.overlay.XvoxBox

/** The overflow menu intentionally has one job: enter the constrained Now Playing customizer. */
@Composable
fun NowPlayingOptionsBox(
    onCustomize: () -> Unit,
    onDismiss: () -> Unit
) {
    XvoxBox(
        onDismiss = onDismiss,
        title = "Customize"
    ) {
        val colors = XvoxTheme.colors
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Arrange the real Now Playing controls on a guided alignment grid.",
                color = colors.secondaryText,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.cardElevated)
                    .xvoxPressScale(onClick = onCustomize),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Open Customize",
                    color = colors.primaryText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
