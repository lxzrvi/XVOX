package com.xvox.music.core.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val XvoxBoxEasing = CubicBezierEasing(0.2f, 0.9f, 0.1f, 1f)

/** One app-wide modal: a floating, centred box, never a draggable bottom sheet. */
@Composable
fun XvoxBox(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "XVOX",
    onAddClick: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    bottomAction: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val colors = XvoxTheme.colors
    val scope = rememberCoroutineScope()
    val dismiss by rememberUpdatedState(onDismiss)
    var visible by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }
    fun close() {
        if (closing) return
        closing = true
        visible = false
        scope.launch { delay(180); dismiss() }
    }
    LaunchedEffect(Unit) { visible = true }

    Dialog(
        onDismissRequest = ::close,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(modifier.fillMaxSize()) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.28f))
                    .clickable(remember { MutableInteractionSource() }, indication = null) { close() }
            )
            BoxWithConstraints(
                Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)
                    .imePadding().padding(horizontal = 20.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                val availableHeight = maxHeight
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(180)) + scaleIn(tween(220, easing = XvoxBoxEasing), initialScale = 0.96f),
                    exit = fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.97f)
                ) {
                    val shape = RoundedCornerShape(26.dp)
                    Column(
                        Modifier.widthIn(max = 560.dp).fillMaxWidth().heightIn(max = availableHeight)
                            .clip(shape).background(colors.cardElevated)
                            .border(0.8.dp, colors.cardBorder, shape)
                            .semantics { paneTitle = title }
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 18.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            onBack?.let { back ->
                                Icon(painterResource(R.drawable.ic_xvox_arrow_left), "Back", tint = colors.primaryText,
                                    modifier = Modifier.size(40.dp).xvoxPressScale(onClick = back).padding(10.dp))
                            }
                            Text(title, color = colors.primaryText, fontSize = 18.sp,
                                fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f))
                            if (onAddClick != null) {
                                Icon(painterResource(R.drawable.ic_xvox_add), "Add", tint = colors.primaryAccent,
                                    modifier = Modifier.size(48.dp).xvoxPressScale(onClick = onAddClick).padding(14.dp))
                            }
                            Box(Modifier.size(48.dp).clip(CircleShape).xvoxPressScale { close() },
                                contentAlignment = Alignment.Center) {
                                Icon(painterResource(R.drawable.ic_xvox_close), "Close $title",
                                    tint = colors.primaryText, modifier = Modifier.size(20.dp))
                            }
                        }
                        Box(Modifier.fillMaxWidth().height(0.7.dp).background(colors.cardBorder.copy(alpha = 0.55f)))
                        // Bounded content keeps the header / X visible even for long queues or the keyboard.
                        Box(Modifier.weight(1f, fill = false).fillMaxWidth().padding(14.dp)) { content() }
                        if (bottomAction != null) {
                            Box(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) { bottomAction() }
                        }
                    }
                }
            }
        }
    }
}
