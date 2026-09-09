package com.xvox.music.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale

/**
 * One confirmation shape for the whole app: exit, delete a song, delete a playlist.
 * No mascot, no illustration — just the question and two clear answers.
 */
@Composable
fun XvoxConfirmBox(
    question: String,
    confirmLabel: String,
    cancelLabel: String = "Cancel",
    detail: String? = null,
    danger: Boolean = false,
    emoji: String? = null,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val colors = XvoxTheme.colors
    val confirmColor = if (danger) Color(0xFFEF4444) else colors.primaryAccent
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Plain emoji, no circle around it.
        if (!emoji.isNullOrBlank()) {
            Text(emoji, fontSize = 36.sp, modifier = Modifier.padding(bottom = 6.dp))
        }
        Text(
            question, color = colors.primaryText, fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
        if (!detail.isNullOrBlank()) {
            Text(
                detail, color = colors.secondaryText, fontSize = 12.sp, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(colors.card)
                    .xvoxPressScale(onClick = onCancel).padding(14.dp),
                contentAlignment = Alignment.Center
            ) { Text(cancelLabel, color = colors.primaryText, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(confirmColor)
                    .xvoxPressScale(onClick = onConfirm).padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    confirmLabel,
                    color = if (danger) Color.White else colors.background,
                    fontWeight = FontWeight.Bold, fontSize = 13.sp
                )
            }
        }
    }
}

/** Kept as the exit-specific wrapper so existing call sites stay unchanged. */
@Composable
fun ExitMusicBox(onYes: () -> Unit, onNo: () -> Unit) = XvoxConfirmBox(
    question = "Stop the music and close XVOX?",
    confirmLabel = "Yes, stop",
    cancelLabel = "No, stay",
    emoji = ":(",
    onConfirm = onYes,
    onCancel = onNo
)
