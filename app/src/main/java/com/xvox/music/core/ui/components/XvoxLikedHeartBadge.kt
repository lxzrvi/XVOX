package com.xvox.music.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xvox.music.R

/** Compact red-heart marker shared by liked artist and playlist artwork. */
@Composable
fun XvoxLikedHeartBadge(
    modifier: Modifier = Modifier,
    badgeSize: Dp = 20.dp
) {
    Box(
        modifier = modifier
            .size(badgeSize)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = .45f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_xvox_heart),
            contentDescription = "Liked",
            tint = Color(0xFFE53935),
            modifier = Modifier.size(badgeSize * .6f)
        )
    }
}
