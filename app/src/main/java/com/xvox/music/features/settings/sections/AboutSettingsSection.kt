package com.xvox.music.features.settings.sections

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme

/** Short by design: the mark, one line, and the version. */
@Composable
fun AboutSettingsSection() {
    val colors = XvoxTheme.colors
    val context = LocalContext.current
    val version = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    }
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.xvox_mark),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colors.primaryText),
            modifier = Modifier.size(44.dp)
        )
        Text("XVOX", color = colors.primaryAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("Your local music, your layout.", color = colors.secondaryText, fontSize = 12.sp)
        Text("Everything stays on this device.", color = colors.secondaryText, fontSize = 12.sp)
        Text("Version ${version.ifBlank { "development" }}", color = colors.mutedText, fontSize = 10.sp)
    }
}
