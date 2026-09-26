package com.xvox.music.player.nowplaying.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.overlay.XvoxBox

/**
 * Now Playing keeps its artwork and background behavior intentionally fixed. The cover always
 * uses Depth; the background always follows the cover's clustered, adaptively softened dominant
 * colour. There are no alternate background choices to drift away from that model.
 */
@Composable
fun NowPlayingOptionsBox(onDismiss: () -> Unit) {
    XvoxBox(
        onDismiss = onDismiss,
        title = "Now Playing"
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FixedNowPlayingChoice(
                title = "Artwork · Depth",
                subtitle = "Depth is fixed for every cover change"
            )
            FixedNowPlayingChoice(
                title = "Background · Adaptive dominant color",
                subtitle = "Cover pixels are clustered; extreme black, white, and harsh colour are softened only when needed"
            )
        }
    }
}

@Composable
private fun FixedNowPlayingChoice(title: String, subtitle: String) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(15.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.cardElevated)
            .border(.9.dp, colors.primaryAccent.copy(alpha = .72f), shape)
            .padding(13.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = colors.secondaryText, fontSize = 11.sp)
        }
        Icon(
            painter = painterResource(R.drawable.ic_xvox_check),
            contentDescription = "$title active",
            tint = colors.primaryAccent,
            modifier = Modifier.align(Alignment.CenterEnd).size(18.dp)
        )
    }
}
