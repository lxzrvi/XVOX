package com.xvox.music.features.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import kotlinx.coroutines.delay

val GreetingLines =
    listOf(
        "What are you listening to today?",
        "What's the mood today?",
        "Need something energetic?",
        "Time for something familiar?",
        "Find your sound.",
        "Press play and disappear.",
        "Something calm today?",
        "Turn the volume up.",
        "Let the music take over.",
        "Find something worth repeating.",
        "Maybe an old favorite?",
        "Your music is waiting.",
        "Pick a track, set the mood.",
        "A good song changes everything.",
        "Time to get lost in sound.",
        "Queue up something good.",
        "Let the next song surprise you.",
        "Your soundtrack starts here.",
        "One track can change the mood.",
        "Play whatever feels right."
    )

/**
 * The lines that rotate under the name when the user has not written their own.
 *
 * @param intervalMs how long each line stays before the next one fades in.
 * @param lines the pool to rotate; the profile editor lists exactly this list.
 */
@Composable
fun HomeGreeting(
    intervalMs: Long = 8_000L,
    lines: List<String> = GreetingLines
) {
    val colors = XvoxTheme.colors
    if (lines.isEmpty()) return

    var index by remember {
        mutableIntStateOf(0)
    }

    LaunchedEffect(intervalMs, lines) {
        while (true) {
            delay(intervalMs.coerceIn(1_500L, 60_000L))
            index =
                (index + 1) %
                    lines.size
        }
    }

    AnimatedContent(
        targetState = index,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "homeGreeting"
    ) { current ->
        Text(
            text = lines[current % lines.size],
            color = colors.secondaryText,
            fontSize = 10.sp,
            lineHeight = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
