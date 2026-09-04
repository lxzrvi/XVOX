package com.xvox.music.features.home.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.data.preferences.XvoxPlaylist
import java.text.DateFormat
import java.util.Date

@Composable
fun PlaylistActionsBox(
    playlist: XvoxPlaylist,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onInfo: () -> Unit
) {
    val colors = XvoxTheme.colors

    Column(
        modifier =
            Modifier.fillMaxWidth()
    ) {
        Text(
            text = playlist.name,
            color = colors.primaryText,
            fontSize = 18.sp,
            fontWeight =
                FontWeight.Bold
        )

        Spacer(
            Modifier.height(16.dp)
        )

        PlaylistAction(
            "Rename",
            onRename
        )
        PlaylistAction(
            "Delete playlist",
            onDelete
        )
        PlaylistAction(
            "Playlist info",
            onInfo
        )
    }
}

@Composable
fun RenamePlaylistBox(
    playlist: XvoxPlaylist,
    onRename: (String) -> Unit
) {
    val colors = XvoxTheme.colors

    var name by remember(
        playlist.id
    ) {
        mutableStateOf(
            playlist.name
        )
    }

    Column(
        modifier =
            Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Rename playlist",
            color = colors.primaryText,
            fontSize = 18.sp,
            fontWeight =
                FontWeight.Bold
        )

        Spacer(
            Modifier.height(16.dp)
        )

        BasicTextField(
            value = name,
            onValueChange = {
                name = it
            },
            singleLine = true,
            textStyle =
                TextStyle(
                    color =
                        colors.primaryText,
                    fontSize = 14.sp
                ),
            cursorBrush =
                SolidColor(
                    colors.primaryText
                ),
            modifier =
                Modifier.fillMaxWidth()
        )

        Spacer(
            Modifier.height(18.dp)
        )

        PlaylistAction(
            title = "Save",
            onClick = {
                if (name.isNotBlank()) {
                    onRename(name)
                }
            }
        )
    }
}

@Composable
fun PlaylistInfoBox(
    playlist: XvoxPlaylist,
    songCount: Int
) {
    val colors = XvoxTheme.colors

    val created =
        if (playlist.createdAt > 0L) {
            DateFormat
                .getDateTimeInstance(
                    DateFormat.MEDIUM,
                    DateFormat.SHORT
                )
                .format(
                    Date(
                        playlist.createdAt
                    )
                )
        } else {
            "Unknown"
        }

    Column(
        modifier =
            Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Playlist info",
            color =
                colors.primaryText,
            fontSize = 18.sp,
            fontWeight =
                FontWeight.Bold
        )

        Spacer(
            Modifier.height(14.dp)
        )

        Text(
            text = playlist.name,
            color =
                colors.primaryText,
            fontSize = 14.sp
        )

        Spacer(
            Modifier.height(8.dp)
        )

        Text(
            text =
                "$songCount songs",
            color =
                colors.secondaryText,
            fontSize = 12.sp
        )

        Spacer(
            Modifier.height(6.dp)
        )

        Text(
            text =
                "Created $created",
            color =
                colors.secondaryText,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun PlaylistAction(
    title: String,
    onClick: () -> Unit
) {
    val colors = XvoxTheme.colors

    Text(
        text = title,
        color = colors.primaryText,
        fontSize = 14.sp,
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
            .padding(
                vertical = 13.dp
            )
    )
}
