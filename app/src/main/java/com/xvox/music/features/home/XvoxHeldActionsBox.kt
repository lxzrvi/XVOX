// EXACT CODE NAYI FILE BANAO (poori file) - koi extra kaam nahi. Path: app/src/main/java/com/xvox/music/features/home/XvoxHeldActionsBox.kt
package com.xvox.music.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import com.xvox.music.core.ui.icons.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxRed
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.blur.xvoxGlassRim
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.overlay.xvoxBoxScroll

/** One row of a held-style menu: label on the left, icon on the right. */
class XvoxHeldAction(
    val label: String,
    val icon: Int,
    val destructive: Boolean = false,
    val onClick: () -> Unit
)

/**
 * Held-item menu for collections (playlist / artist / album), same look as the song long-press:
 * the item lifts off the blurred page as a cover card and its actions hang under it in one
 * frosted plate. Shown through XvoxBoxPresentation.HELD.
 */
@Composable
fun XvoxHeldActionsBox(
    title: String,
    subtitle: String,
    cover: @Composable (Modifier) -> Unit,
    actions: List<XvoxHeldAction>,
    onCoverClick: (() -> Unit)? = null,
    coverCircle: Boolean = false
) {
    val colors = XvoxTheme.colors
    val dark = !colors.isLight
    val plate = (if (dark) Color.Black else Color.White).copy(alpha = .5f)
    val hairline = if (dark) Color.White.copy(alpha = .10f) else Color.Black.copy(alpha = .08f)
    val cardShape = RoundedCornerShape(22.dp)
    val coverShape = if (coverCircle) RoundedCornerShape(98.dp) else RoundedCornerShape(15.dp)

    Column(
        modifier = Modifier.width(300.dp).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .shadow(22.dp, cardShape, clip = false)
                .clip(cardShape)
                .background(plate)
                .xvoxGlassRim(cardShape, hairlineColor = hairline)
                .then(if (onCoverClick != null) Modifier.xvoxPressScale(onClick = onCoverClick) else Modifier)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            cover(Modifier.size(196.dp).clip(coverShape))
            Spacer(Modifier.height(8.dp))
            Text(
                title, color = colors.primaryText, fontSize = 15.sp, lineHeight = 18.sp,
                fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center, modifier = Modifier.width(196.dp)
            )
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle, color = colors.secondaryText, fontSize = 12.sp, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.width(196.dp)
                )
            }
            Spacer(Modifier.height(2.dp))
        }

        Spacer(Modifier.height(12.dp))

        val scroll = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(18.dp, cardShape, clip = false)
                .clip(cardShape)
                .background(plate)
                .xvoxGlassRim(cardShape, hairlineColor = hairline)
                .heightIn(max = 330.dp)
                .verticalScroll(scroll)
                .xvoxBoxScroll(scroll)
        ) {
            actions.forEachIndexed { index, action ->
                val tint = if (action.destructive) XvoxRed else colors.primaryText
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 46.dp)
                        .xvoxPressScale(pressedScale = .985f, onClick = action.onClick)
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        action.label, color = tint, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                    )
                    Icon(painterResource(action.icon), null, tint = tint.copy(alpha = .9f), modifier = Modifier.size(19.dp))
                }
                if (index != actions.lastIndex) {
                    Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp).height(.5.dp).background(hairline))
                }
            }
        }
    }
}
