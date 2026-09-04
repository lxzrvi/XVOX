package com.xvox.music.features.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song

@Composable
fun DeleteSongBox(
    song: Song,
    onRemoveApp: () -> Unit,
    onDeleteDevice: () -> Unit
) {
    val colors = XvoxTheme.colors

    Column(
        modifier =
            Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Delete",
            color = colors.primaryText,
            fontSize = 18.sp,
            fontWeight =
                FontWeight.Bold
        )

        Spacer(
            Modifier.height(6.dp)
        )

        Text(
            text = song.title,
            color = colors.secondaryText,
            fontSize = 12.sp
        )

        Spacer(
            Modifier.height(16.dp)
        )

        DeleteChoice(
            title = "Remove from XVOX",
            subtitle =
                "Keep the audio file on this device",
            onClick = onRemoveApp
        )

        DeleteChoice(
            title = "Delete from device",
            subtitle =
                "Permanently remove the audio file",
            onClick = onDeleteDevice
        )
    }
}

@Composable
fun ConfirmDeviceDeleteBox(
    song: Song,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = XvoxTheme.colors

    Column(
        modifier =
            Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Delete from device?",
            color = colors.primaryText,
            fontSize = 18.sp,
            fontWeight =
                FontWeight.Bold
        )

        Spacer(
            Modifier.height(8.dp)
        )

        Text(
            text =
                "${song.title} will be permanently removed.",
            color = colors.secondaryText,
            fontSize = 12.sp
        )

        Spacer(
            Modifier.height(18.dp)
        )

        DeleteChoice(
            title = "Cancel",
            subtitle = "",
            onClick = onCancel
        )

        DeleteChoice(
            title = "Delete",
            subtitle = "",
            onClick = onDelete
        )
    }
}

@Composable
private fun DeleteChoice(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val colors = XvoxTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource =
                    remember {
                        MutableInteractionSource()
                    },
                indication = null,
                onClick = onClick
            )
    ) {
        Spacer(
            Modifier.height(9.dp)
        )

        Text(
            text = title,
            color = colors.primaryText,
            fontSize = 14.sp,
            fontWeight =
                FontWeight.SemiBold
        )

        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                color = colors.secondaryText,
                fontSize = 10.sp
            )
        }

        Spacer(
            Modifier.height(9.dp)
        )
    }
}
