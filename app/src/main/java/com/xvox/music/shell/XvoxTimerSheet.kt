package com.xvox.music.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme

@Composable
fun TimerSheetContent(
    currentMinutes: Int?,
    onSetMinutes: (Int) -> Unit,
    onCustom: (Int, Int, Boolean, Boolean) -> Unit,
    onCancel: () -> Unit
) {
    val colors = XvoxTheme.colors
    var showCustom by remember { mutableStateOf(false) }
    var minText by remember { mutableStateOf("") }
    var secText by remember { mutableStateOf("") }
    var pauseMusic by remember { mutableStateOf(true) }
    var closeApp by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sleep Timer",
                color = colors.primaryText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            if (currentMinutes != null) {
                Text(
                    text = "$currentMinutes min active",
                    color = colors.primaryAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        val presets = listOf(5, 10, 15, 30, 45, 60)
        presets.chunked(3).forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { minutes ->
                    val selected = currentMinutes == minutes
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) colors.primaryAccent else colors.card.copy(alpha = 0.97f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onSetMinutes(minutes)
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$minutes min",
                            color = if (selected) colors.background else colors.primaryText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(Modifier.size(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.card.copy(alpha = 0.97f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    showCustom = !showCustom
                }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Custom time...",
                color = colors.primaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )

            Icon(
                painter = painterResource(
                    if (showCustom) R.drawable.ic_xvox_collapse else R.drawable.ic_xvox_caret_right
                ),
                contentDescription = null,
                tint = colors.secondaryText,
                modifier = Modifier.size(16.dp)
            )
        }

        if (showCustom) {
            Spacer(Modifier.size(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BasicTextField(
                    value = minText,
                    onValueChange = {
                        if (it.length <= 3 && it.all { ch -> ch.isDigit() }) {
                            minText = it
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(color = colors.primaryText, fontSize = 14.sp),
                    cursorBrush = SolidColor(colors.primaryAccent),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.card.copy(alpha = 0.97f))
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    decorationBox = { inner ->
                        if (minText.isEmpty()) {
                            Text(text = "Minutes", color = colors.mutedText, fontSize = 14.sp)
                        }
                        inner()
                    }
                )

                BasicTextField(
                    value = secText,
                    onValueChange = {
                        if (it.length <= 2 && it.all { ch -> ch.isDigit() }) {
                            secText = it
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(color = colors.primaryText, fontSize = 14.sp),
                    cursorBrush = SolidColor(colors.primaryAccent),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.card.copy(alpha = 0.97f))
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    decorationBox = { inner ->
                        if (secText.isEmpty()) {
                            Text(text = "Seconds", color = colors.mutedText, fontSize = 14.sp)
                        }
                        inner()
                    }
                )
            }

            Spacer(Modifier.size(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        pauseMusic = true
                        closeApp = false
                    }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = pauseMusic,
                    onClick = {
                        pauseMusic = true
                        closeApp = false
                    }
                )
                Text(
                    text = "Pause music",
                    color = colors.primaryText,
                    fontSize = 13.sp
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        closeApp = true
                        pauseMusic = false
                    }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = closeApp,
                    onClick = {
                        closeApp = true
                        pauseMusic = false
                    }
                )
                Text(
                    text = "Close full app",
                    color = colors.primaryText,
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.size(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.primaryAccent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        val minutes = minText.toIntOrNull() ?: 0
                        val seconds = secText.toIntOrNull() ?: 0
                        if (minutes == 0 && seconds == 0) return@clickable
                        onCustom(minutes, seconds, pauseMusic, closeApp)
                    }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Start custom timer",
                    color = colors.background,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (currentMinutes != null) {
            Spacer(Modifier.size(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.card.copy(alpha = 0.97f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onCancel()
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cancel Timer",
                    color = Color(0xFFDC2626),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
