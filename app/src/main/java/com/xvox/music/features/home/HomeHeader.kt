package com.xvox.music.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
    showPlaylists: Boolean,
    onRefresh: () -> Unit,
    onHeartClick: () -> Unit,
    onLibraryModeClick: () -> Unit
) {
    val colors = XvoxTheme.colors

    Row(
        modifier = Modifier
            .padding(horizontal = 18.dp)
            .height(54.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HomeProfileAvatar(
            profile = profile,
            modifier = Modifier.size(42.dp)
        )

        Spacer(
            modifier = Modifier.width(10.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
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

        val actionShape =
            RoundedCornerShape(21.dp)

        Row(
            modifier = Modifier
                .height(42.dp)
                .clip(actionShape)
                .background(
                    colors.card.copy(
                        alpha = 0.72f
                    )
                )
                .border(
                    width = 0.65.dp,
                    color = colors.cardBorder,
                    shape = actionShape
                )
                .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeHeaderIcon(
                type = HomeHeaderIconType.SCAN,
                onClick = onRefresh
            )

            HomeHeaderIcon(
                type = HomeHeaderIconType.HEART,
                onClick = onHeartClick
            )

            HomeHeaderIcon(
                type =
                    if (showPlaylists) {
                        HomeHeaderIconType.SONGS
                    } else {
                        HomeHeaderIconType.PLAYLIST
                    },
                onClick = onLibraryModeClick
            )
        }
    }
}
