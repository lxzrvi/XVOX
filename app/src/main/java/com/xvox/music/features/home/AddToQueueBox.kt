package com.xvox.music.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics

@Composable
fun AddToQueueBox(
    activeQueueName: String,
    songCount: Int = 1,
    onAddToCurrent: () -> Unit,
    onAddToNew: () -> Unit
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = if (songCount > 1) "Add $songCount songs to queue" else "Choose destination queue",
            color = colors.secondaryText,
            fontSize = 11.5.sp,
            modifier = Modifier.padding(bottom = 2.dp)
        )

        // Option 1: Add to Current Queue
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.card)
                .xvoxPressScale {
                    haptics.tap()
                    onAddToCurrent()
                }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_xvox_queue),
                contentDescription = null,
                tint = colors.primaryAccent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Add to $activeQueueName",
                    color = colors.primaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Append to active playing list",
                    color = colors.secondaryText,
                    fontSize = 11.sp
                )
            }
        }

        // Option 2: Add to New Queue
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.card)
                .xvoxPressScale {
                    haptics.tap()
                    onAddToNew()
                }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_xvox_plus),
                contentDescription = null,
                tint = colors.primaryAccent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Add to new queue",
                    color = colors.primaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Create a separate queue in Queue list",
                    color = colors.secondaryText,
                    fontSize = 11.sp
                )
            }
        }
    }
}
