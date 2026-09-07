package com.xvox.music.shell

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale

@Composable
fun ExitMusicBox(onYes: () -> Unit, onNo: () -> Unit) {
    val colors = XvoxTheme.colors
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.padding(10.dp).size(64.dp)) {
            drawCircle(colors.primaryAccent.copy(alpha = 0.12f))
            drawCircle(colors.primaryAccent, radius = 2.5.dp.toPx(), center = Offset(size.width * .35f, size.height * .40f))
            drawCircle(colors.primaryAccent, radius = 2.5.dp.toPx(), center = Offset(size.width * .65f, size.height * .40f))
            drawArc(colors.primaryAccent, 205f, 130f, false,
                topLeft = Offset(size.width * .28f, size.height * .57f),
                size = Size(size.width * .44f, size.height * .32f), style = Stroke(2.dp.toPx()))
        }
        Text("Are you sure you want to stop the music?", color = colors.primaryText,
            fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text("Yes stops playback and closes XVOX. No keeps you here.", color = colors.secondaryText,
            fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(colors.card)
                .xvoxPressScale(onClick = onNo).padding(14.dp), contentAlignment = Alignment.Center) {
                Text("No, stay", color = colors.primaryText, fontWeight = FontWeight.Bold)
            }
            Box(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(colors.primaryAccent)
                .xvoxPressScale(onClick = onYes).padding(14.dp), contentAlignment = Alignment.Center) {
                Text("Yes, stop", color = colors.background, fontWeight = FontWeight.Bold)
            }
        }
    }
}
