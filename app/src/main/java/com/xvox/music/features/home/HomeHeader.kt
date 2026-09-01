package com.xvox.music.features.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxPersonalFont
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.data.preferences.UserPreferences

@Composable
fun HomeHeader(
    profile: UserPreferences,
    onRefresh: () -> Unit,
    onMenuClick: () -> Unit
) {
    val colors =
        XvoxTheme.colors

    Row(
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        HomeProfileAvatar(
            profile = profile,
            modifier =
                Modifier.xvoxPressScale {
                }
        )

        Spacer(
            modifier =
                Modifier.width(11.dp)
        )

        Column(
            modifier =
                Modifier.weight(1f)
        ) {
            Text(
                text =
                    profile.username,
                color =
                    colors.primaryText,
                fontFamily =
                    XvoxPersonalFont,
                fontSize = 21.sp,
                maxLines = 1,
                overflow =
                    TextOverflow.Ellipsis
            )

            HomeGreeting()
        }

        HomeActionIcon(
            type =
                HomeActionType.REFRESH,
            onClick =
                onRefresh
        )

        HomeActionIcon(
            type =
                HomeActionType.MENU,
            onClick =
                onMenuClick
        )
    }
}
