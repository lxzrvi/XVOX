package com.xvox.music.features.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale

/** Shared fixed sheet footer for transactional Cancel / optional Reset / Okay flows. */
@Composable
fun XvoxTransactionalFooterActions(
    onCancel: () -> Unit,
    onOkay: () -> Unit,
    onReset: (() -> Unit)? = null,
    resetLabel: String = "Reset",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        XvoxTransactionalFooterButton("Cancel", onCancel, Modifier.weight(1f))
        if (onReset != null) {
            XvoxTransactionalFooterButton(resetLabel, onReset, Modifier.weight(1f))
        }
        XvoxTransactionalFooterButton("Okay", onOkay, Modifier.weight(1f), prominent = true)
    }
}

@Composable
private fun XvoxTransactionalFooterButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier,
    prominent: Boolean = false
) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(shape)
            .background(if (prominent) colors.primaryAccent else colors.cardElevated)
            .border(.8.dp, if (prominent) Color.Transparent else colors.cardBorder, shape)
            .xvoxPressScale(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (prominent) colors.background else colors.primaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
