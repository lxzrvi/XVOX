package com.xvox.music.features.settings.sections

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale

/** In-card feature guide with expandable contact and sharing actions. */
@Composable
fun AboutSettingsSection(expanded: Boolean = false) {
    val colors = XvoxTheme.colors
    val context = LocalContext.current
    var actionsExpanded by rememberSaveable { mutableStateOf(expanded) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "XVOX Music Player",
                color = colors.primaryAccent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "v1.02.3",
                color = colors.mutedText,
                fontSize = 11.sp
            )
        }

        Text(
            text = "A fluid local music player built around quick library browsing, continuous playback, adaptive artwork, lyrics, and real-time sound controls.",
            color = colors.secondaryText,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )

        AboutGuideRow(
            title = "Library & Home",
            detail = "Browse All Songs, artists, recents, and playlists. Tap a track to play it; long-press a track or collection to select it, queue it, like it, or open more actions."
        )
        AboutGuideRow(
            title = "Search",
            detail = "Use Search to find songs, artists, and playlists quickly. Open an artist or playlist result to browse its tracks, and long-press a result when you need its action menu."
        )
        AboutGuideRow(
            title = "Mini Player & Now Playing",
            detail = "The Mini Player keeps playback reachable while you browse. Tap it for Now Playing, then swipe the artwork or use previous and next to preview the queue before committing a track."
        )
        AboutGuideRow(
            title = "Queue, favourites & playlists",
            detail = "Manage the active queue from the player, like favourites for fast access, and create or edit playlists through the add-to-playlist actions. Recycle Bin keeps XVOX-removed songs recoverable."
        )
        AboutGuideRow(
            title = "Sound, lyrics & playback",
            detail = "Use the player action controls for queue, timer, equalizer, 3D sound, Bluetooth output, and crossfade. Long-press supported controls for their detailed settings; lyrics open from the player display."
        )
        AboutGuideRow(
            title = "Appearance, widgets & backup",
            detail = "Appearance changes theme, accent, font, text size, and orientation. Widgets opens the launcher-widget studio, while Backup & Restore protects your library preferences."
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.cardElevated.copy(alpha = 0.58f))
                .border(0.8.dp, colors.cardBorder.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                .padding(10.dp)
        ) {
            Text(
                text = "Tip: XVOX keeps detailed controls close to where they are used, so Settings stays focused on appearance, library care, widgets, backup, support, and this guide.",
                color = colors.mutedText,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
        }

        AboutActionToggle(actionsExpanded) { actionsExpanded = !actionsExpanded }
        if (actionsExpanded) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AboutActionButton(
                    label = "Send feedback",
                    modifier = Modifier.weight(1f),
                    onClick = { sendXvoxFeedback(context) }
                )
                AboutActionButton(
                    label = "Share XVOX",
                    modifier = Modifier.weight(1f),
                    onClick = { shareXvoxApp(context) }
                )
            }
        }
        XvoxSettingsFooter()
    }
}

@Composable
private fun AboutActionToggle(expanded: Boolean, onClick: () -> Unit) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.cardElevated)
            .border(.8.dp, colors.cardBorder.copy(alpha = .72f), shape)
            .xvoxPressScale(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Contact & share", color = colors.primaryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text(if (expanded) "Hide" else "Show", color = colors.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AboutActionButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(11.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(colors.primaryAccent)
            .xvoxPressScale(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = colors.background, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun XvoxSettingsFooter() {
    val colors = XvoxTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text("XVOX", color = colors.primaryAccent, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            "Your music, in motion · Settings",
            color = colors.mutedText,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Medium
        )
        Text("© 2026 lxzrvi", color = colors.mutedText.copy(alpha = .78f), fontSize = 8.5.sp)
    }
}

@Composable
private fun AboutGuideRow(title: String, detail: String) {
    val colors = XvoxTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(colors.primaryAccent)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                color = colors.primaryText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = detail,
                color = colors.secondaryText,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
        }
    }
}

internal fun shareXvoxApp(context: Context) {
    runCatching {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(
                Intent.EXTRA_TEXT,
                "🎵 Experience music with XVOX — The next-generation fluid music player featuring instant continuous cover swiping, smart crossfade blend, synchronized lyrics, and pro audio DSP.\n\nDownload & discover XVOX!"
            )
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share XVOX").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}

/** Opens an email-only feedback intent, so unrelated share targets never receive support mail. */
internal fun sendXvoxFeedback(context: Context) {
    runCatching {
        val subject = Uri.encode("XVOX feedback")
        val body = Uri.encode("\n\nDevice / Android version:\nApp version:\n\nWhat happened?\n")
        val intent = Intent(
            Intent.ACTION_SENDTO,
            Uri.parse("mailto:thaparavi382@gmail.com?subject=$subject&body=$body")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Send XVOX feedback").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
