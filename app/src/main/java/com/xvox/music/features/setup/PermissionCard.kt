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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        PermissionItem(
            title = "Music access",
            description =
                "Find and play music stored on this device.",
            granted = audioGranted,
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
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (!granted) {
                    Modifier.clickable(
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = colors.cardBorder
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
                onClick = {
                    if (!granted) {
                        onClick()
                    }
                }
            )
        }
    }
}

@Composable
private fun PermissionToggle(
    checked: Boolean,
    onClick: () -> Unit
) {
    val colors = XvoxTheme.colors

    val trackColor =
        if (checked) {
            colors.primaryAccent
        } else {
            colors.accentSoft
        }

    val thumbColor =
        if (checked) {
            colors.background
        } else {
            colors.mutedText
        }

    Box(
        modifier = Modifier
            .size(
                width = 42.dp,
                height = 24.dp
            )
            .clip(
                RoundedCornerShape(12.dp)
            )
            .clickable(
                enabled = !checked,
                onClick = onClick
            )
            .then(
                Modifier
                    .padding(0.dp)
            ),
        contentAlignment =
            if (checked) {
                Alignment.CenterEnd
            } else {
                Alignment.CenterStart
            }
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(
                    RoundedCornerShape(12.dp)
                )
                .then(
                    Modifier
                )
        )

        androidx.compose.foundation.Canvas(
            modifier =
                Modifier.matchParentSize()
        ) {
            drawRoundRect(
                color = trackColor,
                cornerRadius =
                    androidx.compose.ui.geometry.CornerRadius(
                        size.height / 2f
                    )
            )
        }

        Box(
            modifier = Modifier
                .padding(horizontal = 3.dp)
                .size(18.dp)
                .clip(CircleShape)
                .then(
                    Modifier
                )
        ) {
            androidx.compose.foundation.Canvas(
                modifier =
                    Modifier.matchParentSize()
            ) {
                drawCircle(
                    color = thumbColor
                )
            }
        }
    }
}
