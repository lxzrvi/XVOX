package com.xvox.music.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.data.preferences.XvoxRecentHistoryCapacity

/** Compact persisted Recent-history limit selector, opened from the Recent song-options gear. */
@Composable
fun RecentCapacityBoxContent(
    currentCapacity: Int,
    onSelect: (Int) -> Unit
) {
    val colors = XvoxTheme.colors
    val normalized = XvoxRecentHistoryCapacity.normalize(currentCapacity)
    val options = XvoxRecentHistoryCapacity.finiteOptions + XvoxRecentHistoryCapacity.UNLIMITED

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Keep up to this many songs in Recently Played.",
            color = colors.secondaryText,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
        options.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { capacity ->
                    RecentCapacityChoice(
                        label = XvoxRecentHistoryCapacity.label(capacity),
                        selected = normalized == capacity,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(capacity) }
                    )
                }
                if (row.size == 1) {
                    // Unlimited remains the same width/language as every other selected choice.
                    Box(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun RecentCapacityChoice(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(shape)
            .background(if (selected) colors.primaryAccent else colors.cardElevated)
            .border(
                .8.dp,
                if (selected) colors.primaryAccent else colors.cardBorder.copy(alpha = .80f),
                shape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) colors.background else colors.primaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
