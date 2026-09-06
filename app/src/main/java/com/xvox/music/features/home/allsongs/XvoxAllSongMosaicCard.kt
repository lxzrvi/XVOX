package com.xvox.music.features.home.allsongs

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.features.home.XvoxRecentArtworkSize
import com.xvox.music.features.home.XvoxSongArtwork

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun XvoxAllSongMosaicCard(
    song: Song,
    widthUnits: Float,
    heightUnits: Float,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    val colors = XvoxTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.955f else 1f,
        animationSpec = spring(dampingRatio = 0.84f, stiffness = 1500f),
        label = "mosaicCardPress"
    )

    val cardShape = RoundedCornerShape(12.dp)
    val artworkShape = RoundedCornerShape(8.dp)

    val borderWidth = if (selected) 2.dp else 0.7.dp
    val borderColor = if (selected) colors.primaryAccent else colors.cardBorder

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(cardShape)
            .background(if (selected) colors.cardElevated else colors.card)
            .border(width = borderWidth, color = borderColor, shape = cardShape)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            XvoxSongArtwork(
                artwork = song.artworkUri,
                requestSize = XvoxRecentArtworkSize,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(artworkShape)
            )

            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(20.dp)
                        .background(colors.primaryAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_xvox_check),
                        contentDescription = "Selected",
                        tint = colors.background,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = song.title,
                color = colors.primaryText,
                fontSize = if (widthUnits >= 2f) 12.sp else 10.sp,
                lineHeight = if (widthUnits >= 2f) 14.sp else 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = song.artist,
                color = colors.secondaryText,
                fontSize = if (widthUnits >= 2f) 10.sp else 8.sp,
                lineHeight = if (widthUnits >= 2f) 12.sp else 9.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
