package com.xvox.music.features.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxSuccess
import com.xvox.music.core.design.theme.XvoxTheme

/**
 * First-install permissions are two direct action cards, not switches. Android remains the only
 * authority for grant state; a card simply opens its real system permission request when needed.
 */
@Composable
fun PermissionCard(
    audioGranted: Boolean,
    notificationGranted: Boolean,
    audioPending: Boolean = false,
    notificationPending: Boolean = false,
    onAudioClick: () -> Unit,
    onNotificationClick: () -> Unit
) {
    val colors = XvoxTheme.colors

    Column(Modifier.fillMaxWidth()) {
        Text(
            text = "Permissions",
            color = colors.primaryText,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        // Keep both requests in one balanced row. They remain independently tappable without the
        // misleading on/off affordance of a settings toggle.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PermissionActionCard(
                title = "Music",
                description = "Find songs on this device",
                granted = audioGranted,
                pending = audioPending,
                onClick = onAudioClick,
                modifier = Modifier.weight(1f)
            )
            PermissionActionCard(
                title = "Notifications",
                description = "Show playback controls",
                granted = notificationGranted,
                pending = notificationPending,
                onClick = onNotificationClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PermissionActionCard(
    title: String,
    description: String,
    granted: Boolean,
    pending: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(14.dp)
    val borderColor = if (granted) XvoxSuccess else colors.cardBorder.copy(alpha = .72f)
    Box(
        modifier = modifier
            .heightIn(min = 108.dp)
            .clip(shape)
            .background(colors.cardElevated.copy(alpha = colors.cardElevated.alpha * .82f))
            .border(if (granted) 1.dp else .7.dp, borderColor, shape)
            .then(if (!granted && !pending) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = title,
                color = colors.primaryText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                text = description,
                color = colors.secondaryText,
                fontSize = 10.sp,
                lineHeight = 13.sp,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            when {
                pending -> CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = colors.primaryAccent,
                    strokeWidth = 2.dp
                )
                granted -> Text(
                    text = "Granted",
                    color = XvoxSuccess,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
                else -> Text(
                    text = "Give permission",
                    color = colors.primaryAccent,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
