package com.xvox.music.features.settings.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme

/**
 * Detailed guide explaining all XVOX features, controls, audio DSP engines,
 * gesture interactions, and contextual customization.
 */
@Composable
fun HowToUseSettingsSection() {
    val colors = XvoxTheme.colors

    val sections = listOf(
        "Playback & Now Playing" to listOf(
            "Play & Pause" to "Tap any song tile to start playback immediately.",
            "Fast Forward / Rewind" to "Hold Next for continuous speed boost; hold Previous for step rewind.",
            "Queue Reorder" to "Open the queue pill, touch and hold the drag handle to slide songs up or down without drops.",
            "Action Indicator" to "Three dynamic dots indicate page change and active actions then fade automatically.",
            "Sheet Gesture" to "Swipe down on Now Playing or tap the top bar to smoothly minimize to Mini Player."
        ),
        "Lyrics & Fullscreen Visuals" to listOf(
            "Synced Lyrics" to "Tap song artwork to open real-time synchronized lyrics.",
            "Fullscreen Visualizer" to "Tap fullscreen icon to enter immersive lyrics mode with animated canvas backgrounds.",
            "5 Gradient Animations" to "Cycle through Wave, Aurora, Radial Pulse, Orbital Glow, and Prism Drift effects using the sparkle button.",
            "Custom Offset & Style" to "Adjust timing offset in milliseconds, tweak line gap, and pick custom colors for active & background lines."
        ),
        "Equalizer & Audio DSP" to listOf(
            "Multi-Band EQ" to "Adjust 5-band or 10-band equalizer sliders. Volume headroom prevents distortion without dampening overall volume.",
            "Reverb Intensity" to "Reverb Amount slider scales decay and wet mix smoothly for any selected hall/room acoustic profile.",
            "Grain Control & Noise Gating" to "Smooth harsh high-frequency grain and reduce background noise with real-time DSP.",
            "3D Binaural Orbit" to "Enable 3D Sound and adjust orbit speed to move the stereo stage continuously in a 360-degree orbit."
        ),
        "Decentralized Contextual Layouts" to listOf(
            "Recent Layout Box" to "Long press any Recently Played card to customize recent cards count, style, and transitions.",
            "All Songs Layout Box" to "Long press All Songs card or header to configure list/grid layout, scroll direction, and sorting.",
            "Artist Layout Box" to "Long press an artist to change columns, spacing, crop custom artist photos, or play all songs next.",
            "Mini Player & Nav Bar" to "Long press Mini Player to access floating appearance and layout settings directly.",
            "Header & Avatar" to "Open profile avatar to set your username, rotating greetings, and crop header background photos."
        ),
        "Audio Routing & Device Switching" to listOf(
            "Live Output Route" to "Switch between phone speakers and Bluetooth headset instantly during playback without interrupting the track.",
            "Auto Route Option" to "Automatically routes audio to connected headphones or speakers when plugged in."
        )
    )

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        sections.forEach { (sectionTitle, items) ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface.copy(alpha = 0.5f))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = sectionTitle,
                    color = colors.primaryAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                items.forEach { (title, description) ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Text(
                            text = title,
                            color = colors.primaryText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = description,
                            color = colors.secondaryText,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                    }
                }
            }
        }
    }
}
