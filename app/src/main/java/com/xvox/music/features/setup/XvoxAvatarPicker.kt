package com.xvox.music.features.setup

import android.net.Uri
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxPersonalFont
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale

/**
 * The one avatar strip used by both the setup screen and the profile editor.
 *
 * Custom pictures are not a single replaceable slot any more: each one the user keeps is stacked
 * in the row next to the built-in avatars, the add button always stays at the end, and every
 * custom picture carries its own delete badge. The list is persisted, so it survives restarts.
 */
@Composable
fun XvoxAvatarPicker(
    username: String,
    selectedType: PfpType,
    selectedCustomUri: String?,
    customUris: List<String>,
    onSelectBuiltIn: (PfpType) -> Unit,
    onSelectCustom: (String) -> Unit,
    onAddCustom: () -> Unit,
    onDeleteCustom: (String) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 54.dp
) {
    val colors = XvoxTheme.colors
    val builtIns = PfpType.entries.filter { it != PfpType.CUSTOM }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 6.dp)
    ) {
        items(builtIns, key = { it.name }) { type ->
            val isSelected = selectedType == type && selectedCustomUri == null
            AvatarSlot(size, isSelected, onClick = { onSelectBuiltIn(type) }) {
                if (type == PfpType.DEFAULT) {
                    Text(
                        text = username.trim().firstOrNull()?.uppercase() ?: "X",
                        color = if (isSelected) colors.primaryAccent else colors.primaryText,
                        fontFamily = XvoxPersonalFont,
                        fontSize = (size.value * 0.37f).sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    PfpIcon(
                        type = type,
                        color = if (isSelected) colors.primaryAccent else colors.primaryText,
                        modifier = Modifier.size(size * 0.44f)
                    )
                }
            }
        }

        // Kept pictures, each with its own delete badge.
        itemsIndexed(customUris, key = { _, uri -> uri }) { _, uri ->
            val isSelected = selectedType == PfpType.CUSTOM && selectedCustomUri == uri
            Box(contentAlignment = Alignment.TopEnd) {
                AvatarSlot(size, isSelected, onClick = { onSelectCustom(uri) }) {
                    AsyncImage(
                        model = Uri.parse(uri),
                        contentDescription = "Custom picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(size).clip(CircleShape)
                    )
                }
                Box(
                    modifier = Modifier
                        .offset(x = 3.dp, y = (-3).dp)
                        .size(20.dp)
                        .background(colors.background, CircleShape)
                        .border(0.8.dp, colors.cardBorder, CircleShape)
                        .xvoxPressScale(pressedScale = 0.85f) { onDeleteCustom(uri) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_xvox_close),
                        contentDescription = "Remove this picture",
                        tint = colors.primaryText,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }

        // The add button never goes away, however many pictures are stacked.
        item(key = "add_custom_pfp") {
            AvatarSlot(size, selected = false, onClick = onAddCustom) {
                Icon(
                    painter = painterResource(R.drawable.ic_xvox_add),
                    contentDescription = "Add a picture",
                    tint = colors.primaryText,
                    modifier = Modifier.size(size * 0.36f)
                )
            }
        }
    }
}

@Composable
private fun AvatarSlot(size: Dp, selected: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    val colors = XvoxTheme.colors
    val width by animateDpAsState(if (selected) 2.dp else 0.8.dp, tween(180), label = "avatar_ring")
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.cardElevated)
            .border(width, if (selected) colors.primaryAccent else colors.cardBorder, CircleShape)
            .xvoxPressScale(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}
