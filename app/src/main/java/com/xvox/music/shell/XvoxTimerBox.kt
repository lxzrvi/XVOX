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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.overlay.xvoxBoxScroll

/** Unsaved timer choice owned by XvoxMainShell until the sheet's Okay footer is tapped. */
data class XvoxTimerDraft(
    val minutes: Int,
    val seconds: Int = 0,
    val pauseMusic: Boolean = true,
    val closeApp: Boolean = false
)

/**
 * Content only: the parent XvoxBox supplies the transactional Cancel / [Off] / Okay footer.
 * Presets and custom input update [draft] locally and never start/cancel a timer by themselves.
 */
@Composable
fun XvoxTimerBoxContent(
    currentMinutes: Int?,
    draft: XvoxTimerDraft?,
    onDraftChange: (XvoxTimerDraft?) -> Unit
) {
    val colors = XvoxTheme.colors
    val scrollState = rememberScrollState()
    var showCustom by remember { mutableStateOf(false) }
    var minText by remember { mutableStateOf("") }
    var secText by remember { mutableStateOf("") }
    var pauseMusic by remember { mutableStateOf(true) }
    var closeApp by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .xvoxBoxScroll(scrollState)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (currentMinutes != null) {
            Text(
                text = "$currentMinutes min active",
                color = colors.primaryAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        } else {
            Text(
                text = "Choose when playback should stop",
                color = colors.secondaryText,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        val presets = listOf(5, 10, 15, 30, 45, 60)
        presets.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { minutes ->
                    val selected = draft?.minutes == minutes && draft.seconds == 0
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) colors.primaryAccent else colors.card.copy(alpha = .97f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onDraftChange(XvoxTimerDraft(minutes = minutes))
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.card.copy(alpha = .97f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { showCustom = !showCustom }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Custom time…",
                color = colors.primaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                painter = painterResource(if (showCustom) R.drawable.ic_xvox_collapse else R.drawable.ic_xvox_caret_right),
                contentDescription = null,
                tint = colors.secondaryText,
                modifier = Modifier.size(16.dp)
            )
        }

        if (showCustom) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BasicTextField(
                    value = minText,
                    onValueChange = { if (it.length <= 3 && it.all(Char::isDigit)) minText = it },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(color = colors.primaryText, fontSize = 14.sp),
                    cursorBrush = SolidColor(colors.primaryAccent),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.card.copy(alpha = .97f))
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    decorationBox = { inner ->
                        if (minText.isEmpty()) Text("Minutes", color = colors.mutedText, fontSize = 14.sp)
                        inner()
                    }
                )
                BasicTextField(
                    value = secText,
                    onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) secText = it },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(color = colors.primaryText, fontSize = 14.sp),
                    cursorBrush = SolidColor(colors.primaryAccent),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.card.copy(alpha = .97f))
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    decorationBox = { inner ->
                        if (secText.isEmpty()) Text("Seconds", color = colors.mutedText, fontSize = 14.sp)
                        inner()
                    }
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
                        pauseMusic = true
                        closeApp = false
                    }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = pauseMusic, onClick = { pauseMusic = true; closeApp = false })
                Text("Pause music", color = colors.primaryText, fontSize = 13.sp)
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
                RadioButton(selected = closeApp, onClick = { closeApp = true; pauseMusic = false })
                Text("Close full app", color = colors.primaryText, fontSize = 13.sp)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.cardElevated)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        val minutes = minText.toIntOrNull() ?: 0
                        val seconds = secText.toIntOrNull() ?: 0
                        if (minutes > 0 || seconds > 0) {
                            onDraftChange(
                                XvoxTimerDraft(
                                    minutes = minutes,
                                    seconds = seconds.coerceIn(0, 59),
                                    pauseMusic = pauseMusic,
                                    closeApp = closeApp
                                )
                            )
                        }
                    }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if ((draft?.seconds ?: 0) > 0) "Custom timer selected" else "Use custom time",
                    color = colors.primaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
