package com.xvox.music.features.setup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxSuccess
import com.xvox.music.core.design.theme.XvoxTheme

@Composable
fun PermissionCard(
    audioGranted: Boolean,
    notificationGranted: Boolean,
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

        Spacer(Modifier.height(8.dp))

        PermissionItem(
            title = "Music access",
            description =
                "Allows XVOX to find and play music stored on your device.",
            granted = audioGranted,
            onClick = onAudioClick
        )

        Spacer(Modifier.height(8.dp))

        PermissionItem(
            title = "Notifications",
            description =
                "Shows playback controls while music continues in background.",
            granted = notificationGranted,
            onClick = onNotificationClick
        )
    }
}

@Composable
private fun PermissionItem(
    title: String,
    description: String,
    granted: Boolean,
    onClick: () -> Unit
) {
    val colors = XvoxTheme.colors

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (granted) {
                XvoxSuccess
            } else {
                colors.cardBorder
            }
        ),
        colors = CardDefaults.cardColors(
            containerColor = colors.card
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 14.dp,
                    vertical = 11.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
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

                Spacer(Modifier.height(2.dp))

                Text(
                    text = description,
                    color = colors.secondaryText,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
