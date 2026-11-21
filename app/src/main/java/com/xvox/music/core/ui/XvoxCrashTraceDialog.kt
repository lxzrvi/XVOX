package com.xvox.music.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.xvox.music.core.design.theme.XvoxTheme

/**
 * Shown once, on the launch after a silent process death. It prints the trace the crash guard
 * captured so the failure can be reported instead of guessed at.
 */
@Composable
fun XvoxCrashTraceDialog(trace: String, onDismiss: () -> Unit) {
    val colors = XvoxTheme.colors
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.card, RoundedCornerShape(24.dp))
                .padding(18.dp)
        ) {
            Text("Last crash", color = colors.primaryText, fontSize = 17.sp)
            Text(
                text = trace.takeLast(2200),
                color = colors.secondaryText,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 340.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 10.dp)
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
            ) {
                Text(
                    "Dismiss",
                    color = colors.primaryAccent,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth()
                        .then(Modifier)
                )
            }
        }
    }
}
