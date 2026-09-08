package com.xvox.music.features.settings.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme

@Composable
fun HowToUseSettingsSection() {
    val colors = XvoxTheme.colors
    val tips = listOf(
        "Start & permissions" to "Allow audio access so XVOX can read your device library. Allow notifications for convenient playback controls. The original five-second minimum intro is retained, and Home waits for the library, visible artwork warm-up and playback connection to be ready.",
        "Play a song" to "Tap once. The card gives a brief press pulse; it does not retain the cover's colour. A large source queue loads in small batches while the selected song starts.",
        "Song options" to "Hold an All Songs card. Liked, playlist and search rows also have an options action. Play next, add to queue, playlist, like, share, information and removal actions are here.",
        "Select several songs" to "Choose Select from song options, then tap more songs. Bulk actions stay below the header. Cancel or Back leaves selection mode.",
        "Home layouts" to "One Size uses equal cards. Mosaic 1 is the original layout; Mosaic 2 uses varied tiles. Horizontal scroll moves between pages; vertical scroll uses lazy pages. Home, playlist, widget, lyrics and sound editors open in boxes with a pinned preview and scrolling controls.",
        "Recently Played" to "Swipe the Recent cards left or right. The carousel stays scrollable while a newly played song is added. Hide it or move it above/below All Songs in Home settings.",
        "Merge Home sections" to "Turn on Merge to show Liked Songs and Playlists on Home. Reorder or hide sections. Only Refresh remains in the header's right-hand pill.",
        "Refresh your library" to "Tap Refresh after adding or removing files. Duration, size, folder and hidden-song filters determine what appears; changing visual settings does not scan your music again.",
        "Liked songs & playlists" to "Use the heart to like a song. Create a playlist, add multiple songs and edit its name or cover. A cover can use one image, four song covers or a custom picture.",
        "Queue" to "Open Queue and hold a row to reorder it. Keep your finger near the top/bottom edge to auto-scroll. The dragged row stays attached to the list gesture even when other rows are recycled.",
        "Mini-player" to "Tap the mini-player to open Now Playing. Play/pause and track gestures remain available. Dismissing/stopping playback is different from merely closing Now Playing.",
        "Now Playing & shuffle" to "Swipe the artwork or use Previous/Next. Tap artwork for lyrics. Shuffle changes queue order without changing the current cover; Repeat cycles off/all/one.",
        "Equalizer" to "Enable XvoxMix and choose 5 or 10 bands. Presets map to the selected band count. Boosts no longer automatically attenuate the whole song; strong peaks are limited for safety.",
        "Boost protection" to "This is a manual gain reduction. 0 dB preserves level; increasing it adds space for heavy boosts. Its effect is shown in the EQ contour. It cannot protect a physical speaker from excessive system volume.",
        "Noise & sharp highs" to "Noise reduction gently attenuates very low-level sound and may soften quiet tails. Soften Highs reduces harsh upper frequencies. Neither control separates vocals or repairs a damaged recording.",
        "3D headphones" to "Use headphones for ear-delay, rear-cue and reflection effects. Adjust depth and orbit speed; balance, app volume and output ceiling affect the schematic preview. Perceived space depends on the recording and listener.",
        "Smart crossfade" to "Choose an overlap duration. Smart energy blending compares the last/first audio envelopes and prefers calmer hand-offs. Beat alignment is used only when credible; clash control hands bass between tracks to reduce competing kicks.",
        "Crossfade display" to "Only the subdued seek track is tinted. A first/manual track does not get an intro-blend zone. During a real overlap, a small Crossfading pill appears—there are no extra song-name bars.",
        "Lyrics files" to "Open lyrics from the artwork, attach LRC for timing or a plain-text file, or use embedded lyrics when available. Full-screen lyrics use the same preferences.",
        "Lyrics timing" to "Settings → Lyrics: positive offset delays the words; negative offset advances them. Tap a timed lyric to seek with the offset applied. Reset timing to return to the file's original timestamps.",
        "Lyrics appearance" to "Set the current and other-line sizes, independent top/bottom fade areas, and Soft Fade, Slide, Focus Zoom, Glide or Spring entry. The preview uses placeholder lines only. These controls are also inside Now Playing's three-dot box.",
        "Headset auto play" to "Enable Auto Play in Playback. XVOX watches actual wired, USB, A2DP and BLE output routes. It resumes the existing source or a permitted filtered library. Android force-stop/background restrictions still apply.",
        "Sleep timer" to "Set a preset or custom timer from the player. Choose the available pause/close behavior and cancel it when no longer needed.",
        "Library filters" to "Use presets or custom seconds/minutes and KB/MB thresholds. Checked folders in the browser are excluded, including descendants. Similar folder names on another volume are not automatically excluded.",
        "Hidden Songs & restore" to "Remove from XVOX hides a file without deleting it. Find it below About in Settings and choose Restore or Restore All. File deletion from the device is different and cannot be undone here.",
        "Widgets & sizes" to "Add XVOX using your launcher or Add Widget. Preview 1–6 columns and rows, then resize in the launcher. Actual cell sizes depend on your launcher. Widgets intentionally have no progress bar.",
        "Widget layout" to "Set margins, padding, alignment, cover placement/size, borders and corners. Full Cover fills the background with artwork while leaving text/buttons configurable. Tiny widgets cap dimensions to remain usable.",
        "Widget text & buttons" to "Edit song, artist and logo labels separately: hide/show, font, size, colour, background and borders. Edit each button's position, order, size, padding, caption, colours and border—or apply a field to all buttons.",
        "Widget playback with app closed" to "Press Play on the widget. The playback service starts in the foreground before requesting audio focus, so an Activity/controller is not required. Another app's exclusive audio focus or muted system/app volume can still prevent audible output.",
        "Back & background" to "Settings/Search Back returns Home. Home's confirmation can stop playback and close the UI. Closing the Activity alone does not tear down an active player. Use Stop when you want music to end.",
        "XvoxSplit setup" to "This optional feature uses a real on-device model to separate vocals from accompaniment. Read the battery/storage warning and allow the 28.3 MB model download. Up to two tracks are prepared first; normal playback continues while the queue is processed in the background.",
        "XvoxSplit progress & safety" to "The pill before the star shows ready/total and the number processing. Tap it for tasks; tap a queued/working row to cancel that track. Three rapid manual track changes within four seconds turn the feature off. Enable again explicitly after settling on a queue.",
        "Save separated versions" to "Hold a ready task: choose Normal, XvoxSplit, or Add/Save to XvoxSplit. The header cycles Liked → XvoxSplit → Home and uses a waveform icon for the split collection. Merge can show/hide this section; its progress pill has a separate visibility toggle.",
        "Appearance & privacy" to "Choose light, dark, AMOLED/system themes, accents and text scale. XVOX plays local files and stores its profile, playlists and preferences on your device; beat/energy analysis is on-device, not uploaded."
    )
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        tips.forEachIndexed { index, (title, text) ->
            Text("${index + 1}. $title", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(text, color = colors.secondaryText, fontSize = 11.sp, lineHeight = 16.sp)
        }
    }
}
