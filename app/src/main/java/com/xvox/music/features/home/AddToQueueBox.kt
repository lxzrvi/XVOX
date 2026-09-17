package com.xvox.music.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.xvox.music.core.ui.overlay.xvoxBoxScroll
import com.xvox.music.player.playback.XvoxSavedQueue

@Composable
fun AddToQueueBox(
    activeQueueName: String,
    savedQueues: List<XvoxSavedQueue> = emptyList(),
    songCount: Int = 1,
    onAddToCurrent: () -> Unit,
    onAddToSaved: (XvoxSavedQueue) -> Unit = {},
    onAddToNew: () -> Unit
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .verticalScroll(scrollState)
            .xvoxBoxScroll(scrollState)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = if (songCount > 1) "Add $songCount songs to queue" else "Choose destination queue",
            color = colors.secondaryText,
            fontSize = 11.5.sp,
            modifier = Modifier.padding(bottom = 2.dp)
        )

        // Option 1: Queue 1 (Default / Active queue)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.card)
                .xvoxPressScale {
                    haptics.tap()
                    onAddToCurrent()
                }
                .padding(horizontal = 14.dp, vertical = 13.dp),
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
                    text = "Queue 1",
                    color = colors.primaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Current queue",
                    color = colors.primaryAccent,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Option 2+: Numbered Saved Queues (Queue 2, Queue 3...)
        savedQueues.forEachIndexed { index, saved ->
            val queueNumber = index + 2
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.card)
                    .xvoxPressScale {
                        haptics.tap()
                        onAddToSaved(saved)
                    }
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_xvox_queue),
                    contentDescription = null,
                    tint = colors.secondaryText,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.size(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Queue $queueNumber",
                        color = colors.primaryText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${saved.name} • ${saved.songs.size} songs",
                        color = colors.secondaryText,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Option 3: Add to New Queue
        val nextQueueNumber = savedQueues.size + 2
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.card)
                .xvoxPressScale {
                    haptics.tap()
                    onAddToNew()
                }
                .padding(horizontal = 14.dp, vertical = 13.dp),
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
                    text = "Create Queue $nextQueueNumber",
                    color = colors.secondaryText,
                    fontSize = 11.sp
                )
            }
        }
    }
}
