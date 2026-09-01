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

private val Greetings =
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

@Composable
fun HomeGreeting() {
    val colors = XvoxTheme.colors

    var index by remember {
        mutableIntStateOf(0)
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(8_000)
            index =
                (index + 1) %
                    Greetings.size
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
            text = Greetings[current],
            color = colors.secondaryText,
            fontSize = 10.sp,
            lineHeight = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
