package com.xvox.music.features.setup

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme

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

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Permissions",
            color = colors.primaryText,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        PermissionItem(
            title = "Music access",
            description =
                "Find and play music stored on this device.",
            granted = audioGranted,
            pending = audioPending,
            onClick = onAudioClick
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        PermissionItem(
            title = "Notifications",
            description =
                "Show playback controls while XVOX plays in background.",
            granted = notificationGranted,
            pending = notificationPending,
            onClick = onNotificationClick
        )
    }
}

@Composable
private fun PermissionItem(
    title: String,
    description: String,
    granted: Boolean,
    pending: Boolean,
    onClick: () -> Unit
) {
    val colors = XvoxTheme.colors

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (!granted && !pending) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = .7.dp,
            color = colors.cardBorder.copy(alpha = .68f)
        ),
        colors = CardDefaults.cardColors(
            // Match the app's own elevated/settings surfaces instead of introducing an opaque
            // first-install card that ignores the active UI treatment.
            containerColor = colors.cardElevated.copy(alpha = colors.cardElevated.alpha * .82f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 14.dp,
                    vertical = 11.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = colors.primaryText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(
                    modifier = Modifier.height(2.dp)
                )

                Text(
                    text = description,
                    color = colors.secondaryText,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }

            PermissionToggle(
                checked = granted,
                pending = pending,
                onClick = {
                    if (!granted && !pending) onClick()
                }
            )
        }
    }
}

@Composable
private fun PermissionToggle(
    checked: Boolean,
    pending: Boolean,
    onClick: () -> Unit
) {
    val colors = XvoxTheme.colors
    // The request state never drives [checked]. Android's grant result does, so an off switch
    // cannot briefly lie to the user while the system permission dialog is open.
    Box(contentAlignment = Alignment.Center) {
        Switch(
            checked = checked,
            enabled = !pending,
            onCheckedChange = { if (!checked && !pending) onClick() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.background,
                checkedTrackColor = colors.primaryAccent,
                uncheckedThumbColor = colors.secondaryText,
                uncheckedTrackColor = colors.cardElevated,
                disabledUncheckedThumbColor = colors.secondaryText.copy(alpha = .66f),
                disabledUncheckedTrackColor = colors.cardBorder.copy(alpha = .72f)
            )
        )
        if (pending) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = colors.primaryAccent,
                strokeWidth = 2.dp
            )
        }
    }
}
