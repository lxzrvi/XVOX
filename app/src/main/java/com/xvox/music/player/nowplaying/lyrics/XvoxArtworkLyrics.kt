package com.xvox.music.player.nowplaying.lyrics

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.data.preferences.LyricsSettings
import com.xvox.music.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun XvoxArtworkLyrics(
    state: XvoxLyricsUiState,
    position: Long,
    onSeek: (Long) -> Unit,
    onAttach: (Uri) -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
    onFullscreen: () -> Unit,
    onOpenSettings: (() -> Unit)? = null,
    textColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    val context = LocalContext.current
    val prefs = remember(context) { UserPreferencesRepository(context.applicationContext) }
    val lyricsSettings by prefs.lyricsSettings.collectAsState(initial = LyricsSettings())
    val gradientAnim = lyricsSettings.gradientAnimation

    val custom =
        state.lyrics?.source ==
            XvoxLyricsSource.USER_LRC ||
            state.lyrics?.source ==
            XvoxLyricsSource.USER_TEXT

    val launcher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            uri?.let(onAttach)
        }

    var pillVisible by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    fun pingInteraction() {
        lastInteractionTime = System.currentTimeMillis()
        pillVisible = true
    }

    LaunchedEffect(lastInteractionTime, pillVisible) {
        if (pillVisible) {
            delay(3500)
            if (System.currentTimeMillis() - lastInteractionTime >= 3400) {
                pillVisible = false
            }
        }
    }

    val transition = rememberInfiniteTransition(label = "artworkLyricsAtmosphere")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing)
        ),
        label = "ambientPhase"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background.copy(alpha = 0.27f))
            .pointerInput(Unit) {
                detectTapGestures {
                    pingInteraction()
                }
            }
    ) {
        // Canvas gradient animation inside artwork lyrics box
        if (gradientAnim != "off") {
            when (gradientAnim) {
                "wave" -> {
                    Canvas(Modifier.fillMaxSize()) {
                        val w = size.width; val h = size.height
                        val cx = w / 2f + (w * 0.30f * cos(phase))
                        val cy = h / 2f + (h * 0.25f * sin(phase))
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    textColor.copy(alpha = 0.35f),
                                    colors.primaryAccent.copy(alpha = 0.20f),
                                    Color.Transparent
                                ),
                                center = Offset(cx, cy),
                                radius = w * 0.85f
                            )
                        )
                    }
                }
                "drift" -> {
                    Canvas(Modifier.fillMaxSize()) {
                        val w = size.width; val h = size.height
                        val progress = (sin(phase) + 1f) / 2f
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    textColor.copy(alpha = 0.32f),
                                    colors.primaryAccent.copy(alpha = 0.20f),
                                    Color.Transparent
                                ),
                                start = Offset(w * progress, 0f),
                                end = Offset(w * (1f - progress), h)
                            )
                        )
                    }
                }
                "aurora" -> {
                    Canvas(Modifier.fillMaxSize()) {
                        val w = size.width; val h = size.height
                        val p = Path()
                        p.moveTo(0f, h * 0.25f + sin(phase) * 35f)
                        p.cubicTo(
                            w * 0.33f, h * 0.15f + cos(phase) * 40f,
                            w * 0.66f, h * 0.35f + sin(phase * 1.2f) * 35f,
                            w, h * 0.2f + cos(phase * 0.8f) * 30f
                        )
                        p.lineTo(w, h)
                        p.lineTo(0f, h)
                        p.close()
                        drawPath(
                            path = p,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    colors.primaryAccent.copy(alpha = 0.25f),
                                    textColor.copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            )
                        )
                    }
                }
            }
        }

        when {
            state.loading -> {
                Text(
                    text = "Loading lyrics…",
                    color = colors.secondaryText,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            state.lyrics != null -> {
                XvoxSyncedLyrics(
                    lyrics = state.lyrics,
                    position = position,
                    onSeek = onSeek,
                    textColor = if (lyricsSettings.matchCoverColor) textColor else Color.White,
                    modifier = Modifier.fillMaxSize()
                )
            }

            else -> {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(54.dp)
                        .background(colors.card.copy(alpha = 0.25f), CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            launcher.launch(arrayOf("*/*"))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_xvox_lyrics_add),
                        contentDescription = "Add lyrics",
                        tint = colors.primaryText,
                        modifier = Modifier.size(23.dp)
                    )
                }
            }
        }

        // Top right actions pill with slide animation
        AnimatedVisibility(
            visible = pillVisible,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(240, easing = FastOutSlowInEasing)
            ) + fadeIn(tween(180)),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(200, easing = FastOutSlowInEasing)
            ) + fadeOut(tween(140)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(9.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (custom) {
                    LyricsDeleteButton(onClick = onDelete)
                    Spacer(Modifier.size(7.dp))
                }

                Row(
                    modifier = Modifier
                        .background(
                            colors.card.copy(alpha = 0.20f),
                            RoundedCornerShape(18.dp)
                        )
                        .padding(horizontal = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LyricsAction(
                        resource = R.drawable.ic_xvox_fullscreen,
                        onClick = onFullscreen
                    )

                    if (onOpenSettings != null) {
                        LyricsAction(
                            resource = R.drawable.ic_xvox_settings,
                            onClick = onOpenSettings
                        )
                    }

                    LyricsAction(
                        resource = R.drawable.ic_xvox_close,
                        onClick = onClose
                    )
                }
            }
        }
    }
}

@Composable
private fun LyricsDeleteButton(onClick: () -> Unit) {
    val colors = XvoxTheme.colors

    Box(
        modifier = Modifier
            .size(36.dp)
            .background(colors.card.copy(alpha = 0.20f), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_xvox_delete),
            contentDescription = "Remove custom lyrics",
            tint = colors.primaryText,
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun LyricsAction(
    resource: Int,
    onClick: () -> Unit
) {
    val colors = XvoxTheme.colors

    Box(
        modifier = Modifier
            .size(36.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(resource),
            contentDescription = null,
            tint = colors.primaryText,
            modifier = Modifier.size(18.dp)
        )
    }
}
