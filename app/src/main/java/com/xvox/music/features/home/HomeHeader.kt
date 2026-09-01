package com.xvox.music.features.home

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.icons.Icons
import androidx.compose.material3.icons.rounded.Menu
import androidx.compose.material3.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.xvox.music.core.design.theme.XvoxPersonalFont
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.data.preferences.UserPreferences

@Composable
fun HomeHeader(
    profile: UserPreferences,
    onRefresh: () -> Unit
) {
    val colors = XvoxTheme.colors

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(colors.cardElevated),
            contentAlignment = Alignment.Center
        ) {
            if (
                profile.selectedPfp == "CUSTOM" &&
                profile.customPfpUri != null
            ) {
                AsyncImage(
                    model = Uri.parse(profile.customPfpUri),
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                )
            } else {
                Text(
                    text = profile.username
                        .firstOrNull()
                        ?.uppercase()
                        ?: "X",
                    color = colors.primaryText,
                    fontFamily = XvoxPersonalFont,
                    fontSize = 21.sp
                )
            }
        }

        Spacer(
            modifier = Modifier.width(11.dp)
        )

        Text(
            text = profile.username,
            color = colors.primaryText,
            fontFamily = XvoxPersonalFont,
            fontSize = 21.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        IconButton(
            onClick = onRefresh
        ) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = "Refresh",
                tint = colors.primaryText
            )
        }

        IconButton(
            onClick = {
                // Menu destination comes with options/settings.
            }
        ) {
            Icon(
                imageVector = Icons.Rounded.Menu,
                contentDescription = "Menu",
                tint = colors.primaryText
            )
        }
    }
}
