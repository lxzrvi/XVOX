// EXACT CODE REPLACE KARO (poori file) - koi extra kaam nahi. Path: app/src/main/java/com/xvox/music/features/home/allsongs/XvoxAllSongCard.kt
package com.xvox.music.features.home.allsongs

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xvox.music.core.model.Song

/** Standard All Songs grid card: full cover with a soft blended lower half (see [XvoxCoverBlendCard]). */
@Composable
fun XvoxAllSongCard(
    song: Song,
    current: Boolean,
    playing: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    /** Five/six-column portrait cards need smaller metadata. */
    dense: Boolean = false
) {
    val design = com.xvox.music.core.design.theme.LocalXvoxDesign.current
    val shape: androidx.compose.ui.graphics.Shape = remember(design.language) {
        if (design.language == com.xvox.music.core.design.theme.XvoxDesignLanguage.XVOX) RoundedCornerShape(14.dp)
        else com.xvox.music.core.design.theme.XvoxContinuousShape(if (design.language == com.xvox.music.core.design.theme.XvoxDesignLanguage.MATERIAL) 20.dp else 14.dp)
    }
    XvoxCoverBlendCard(
        song = song,
        shape = shape,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        current = current,
        playing = playing,
        selected = selected,
        dense = dense
    )
}
