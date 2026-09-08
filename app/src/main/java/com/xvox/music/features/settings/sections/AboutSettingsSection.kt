package com.xvox.music.features.settings.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme

@Composable
fun AboutSettingsSection() {
    val colors = XvoxTheme.colors
    val context = LocalContext.current
    val version = remember(context) { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty() }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("XVOX · your local music, your layout", color = colors.primaryAccent, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text("XVOX is a local Android music player built around a personal library—not a streaming account. Browse your device audio, keep favourites, create playlists and choose how the interface looks and behaves.",
            color = colors.primaryText, fontSize = 12.sp, lineHeight = 17.sp)
        Text("Make it yours", color = colors.primaryText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text("One Size, Mosaic 1 and Mosaic 2 layouts; optional merged Home sections; a persistent mini-player; configurable lyrics; and widgets with per-element text, cover, button, colour and border controls. Compact previews explain layouts without loading a second Home screen.",
            color = colors.secondaryText, fontSize = 11.sp, lineHeight = 16.sp)
        Text("The playback engine", color = colors.primaryText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text("Android Media3 handles decoding and the media session. XvoxMix offers smoothed 5/10-band EQ, manual boost protection, peak limiting, gentle noise/high-frequency controls and a headphone spatial effect. Optional crossfade uses overlapping players and local rhythm/energy analysis. XvoxSplit can download a verified vocal-separation model and prepare real vocal/accompaniment versions locally, with a cancellable background queue and saved collection. Effects alter the audio signal: this is not a claim of bit-perfect output or universal beat matching.",
            color = colors.secondaryText, fontSize = 11.sp, lineHeight = 16.sp)
        Text("Your files stay your files", color = colors.primaryText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text("Playlists, likes, hidden-song flags and preferences are stored locally. Remove from XVOX hides a song; it does not delete the file. Device deletion is a separate confirmed action. Lyrics and artwork come from your files or images/text you choose. Audio analysis runs locally; no song upload is needed.",
            color = colors.secondaryText, fontSize = 11.sp, lineHeight = 16.sp)
        Text("Device limits matter", color = colors.primaryText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text("Available codecs, widget dimensions, notifications and background playback depend on Android and your device/launcher. Spatial perception varies by recording and headphones. Start at a moderate volume; software processing cannot guarantee that a speaker or hearing will be protected at excessive volume.",
            color = colors.secondaryText, fontSize = 11.sp, lineHeight = 16.sp)
        Text("Version ${version.ifBlank { "development" }} · Native Android · Offline library", color = colors.mutedText, fontSize = 10.sp)
    }
}
