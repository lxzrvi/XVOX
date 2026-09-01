package com.xvox.music.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxPersonalFont
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.data.preferences.UserPreferences

@Composable
fun HomeHeader(
    profile: UserPreferences,
    onRefresh: () -> Unit,
    onMenuClick: () -> Unit
) {
    val colors = XvoxTheme.colors

    Row(
        modifier = Modifier
            .padding(
                start = 18.dp,
                end = 18.dp
            )
            .height(58.dp),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        HomeProfileAvatar(
            profile = profile,
            modifier = Modifier.size(42.dp)
        )

        Spacer(
            modifier = Modifier.width(10.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement =
                Arrangement.Center
        ) {
            Text(
                text = profile.username,
                color = colors.primaryText,
                fontFamily = XvoxPersonalFont,
                fontSize = 18.sp,
                lineHeight = 19.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            HomeGreeting()
        }

        HomeActionPill(
            onRefresh = onRefresh,
            onMenuClick = onMenuClick
        )
    }
}

@Composable
private fun HomeActionPill(
    onRefresh: () -> Unit,
    onMenuClick: () -> Unit
) {
    val colors = XvoxTheme.colors

    val shape =
        RoundedCornerShape(22.dp)

    Row(
        modifier = Modifier
            .height(42.dp)
            .clip(shape)
            .background(colors.card)
            .border(
                width = 1.dp,
                color = colors.cardBorder,
                shape = shape
            )
            .padding(horizontal = 3.dp),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Box(
            contentAlignment =
                Alignment.Center
        ) {
            HomeActionIcon(
                type = HomeActionType.REFRESH,
                onClick = onRefresh,
                modifier = Modifier.size(36.dp)
            )
        }

        Box(
            contentAlignment =
                Alignment.Center
        ) {
            HomeActionIcon(
                type = HomeActionType.MENU,
                onClick = onMenuClick,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}
