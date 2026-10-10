package com.xvox.music.player.nowplaying.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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

/** Overflow's normal action. Selecting it enters the live player in-place; it never opens a replica sheet. */
@Composable
fun NowPlayingOptionsBox(
    onCustomize: () -> Unit,
    onDismiss: () -> Unit
) {
    XvoxBox(onDismiss = onDismiss, title = "Now Playing") {
        val colors = XvoxTheme.colors
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(horizontal = 4.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.cardElevated)
                .xvoxPressScale(onClick = onCustomize),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "Customize",
                color = colors.primaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
