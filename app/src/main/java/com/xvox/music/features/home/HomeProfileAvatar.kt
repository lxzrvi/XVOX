package com.xvox.music.features.home

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.xvox.music.core.design.theme.XvoxPersonalFont
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.data.preferences.UserPreferences
import com.xvox.music.features.setup.PfpIcon
import com.xvox.music.features.setup.PfpType

@Composable
fun HomeProfileAvatar(
    profile: UserPreferences,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors

    val type =
        runCatching {
            PfpType.valueOf(
                profile.selectedPfp
            )
        }.getOrDefault(
            PfpType.DEFAULT
        )

    Box(
        modifier = modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(colors.card),
        contentAlignment = Alignment.Center
    ) {
        when {
            type == PfpType.CUSTOM &&
                profile.customPfpUri != null -> {

                AsyncImage(
                    model = Uri.parse(
                        profile.customPfpUri
                    ),
                    contentDescription =
                        "Profile picture",
                    contentScale =
                        ContentScale.Crop,
                    modifier =
                        Modifier.fillMaxSize()
                )
            }

            type == PfpType.DEFAULT -> {
                Text(
                    text =
                        profile.username
                            .firstOrNull()
                            ?.uppercase()
                            ?: "X",
                    color =
                        colors.primaryText,
                    fontFamily =
                        XvoxPersonalFont,
                    fontSize = 21.sp
                )
            }

            else -> {
                PfpIcon(
                    type = type,
                    color =
                        colors.primaryText,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(9.dp)
                )
            }
        }
    }
}
