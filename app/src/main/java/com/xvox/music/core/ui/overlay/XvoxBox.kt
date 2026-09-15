package com.xvox.music.core.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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

/** One app-wide modal: solid opaque card with 100% adaptive content height (capped at max 90% screen height). */
@Composable
fun XvoxBox(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "XVOX",
    mini: Boolean = false,
    onAddClick: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null,
    onEditClick: (() -> Unit)? = null,
    isEditing: Boolean = false,
    headerLeadingContent: (@Composable () -> Unit)? = null,
    headerTitleContent: (@Composable () -> Unit)? = null,
    bottomAction: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val colors = XvoxTheme.colors
    val scope = rememberCoroutineScope()
    val dismiss by rememberUpdatedState(onDismiss)
    var visible by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }
    val swallowInteraction = remember { MutableInteractionSource() }

    val scrimAlpha by animateFloatAsState(
        targetValue = if (visible) 0.40f else 0f,
        animationSpec = tween(50),
        label = "scrimAlpha"
    )

    fun close() {
        if (closing) return
        closing = true
        visible = false
        scope.launch {
            delay(50)
            dismiss()
        }
    }

    LaunchedEffect(Unit) {
        visible = true
    }

    Dialog(
        onDismissRequest = ::close,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(modifier.fillMaxSize()) {
            // Synchronized background scrim dim
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
                    .clickable(remember { MutableInteractionSource() }, indication = null) { close() }
            )

            BoxWithConstraints(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .imePadding()
                    .padding(
                        horizontal = if (mini) 0.dp else 20.dp,
                        vertical = if (mini) 0.dp else 16.dp
                    ),
                contentAlignment = if (mini) Alignment.BottomCenter else Alignment.Center
            ) {
                // Adaptive height up to maximum 90% of screen height
                val maxBoxHeight = maxHeight * 0.90f

                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(220, easing = XvoxBoxEasing)) + if (mini) slideInVertically(tween(220, easing = XvoxBoxEasing)) { it }
                    else scaleIn(tween(220, easing = XvoxBoxEasing), initialScale = 0.95f),
                    exit = fadeOut(tween(200, easing = XvoxBoxEasing)) + if (mini) slideOutVertically(tween(200, easing = XvoxBoxEasing)) { it }
                    else scaleOut(tween(200, easing = XvoxBoxEasing), targetScale = 0.96f)
                ) {
                    val shape = if (mini) RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
                    else RoundedCornerShape(26.dp)
                    val boxFill = colors.cardElevated

                    Column(
                        Modifier
                            .widthIn(max = if (mini) 520.dp else 560.dp)
                            .fillMaxWidth()
                            .heightIn(max = maxBoxHeight)
                            .wrapContentHeight()
                            .clip(shape)
                            .background(boxFill)
                            .clickable(swallowInteraction, indication = null) { }
                            .semantics { paneTitle = title }
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 18.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            onBack?.let { back ->
                                Icon(
                                    painterResource(R.drawable.ic_xvox_arrow_left),
                                    "Back",
                                    tint = colors.primaryText,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .xvoxPressScale(onClick = back)
                                        .padding(10.dp)
                                )
                            }
                            if (headerLeadingContent != null) {
                                headerLeadingContent()
                                Spacer(Modifier.width(10.dp))
                            }
                            if (headerTitleContent != null) {
                                Box(modifier = Modifier.weight(1f)) {
                                    headerTitleContent()
                                }
                            } else {
                                Text(
                                    title,
                                    color = colors.primaryText,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (onSettingsClick != null) {
                                Box(
                                    Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .xvoxPressScale { onSettingsClick() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painterResource(R.drawable.ic_xvox_settings),
                                        "Settings",
                                        tint = colors.primaryAccent,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                            if (onEditClick != null) {
                                Box(
                                    Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .xvoxPressScale { onEditClick() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painterResource(if (isEditing) R.drawable.ic_xvox_check else R.drawable.ic_xvox_edit),
                                        if (isEditing) "Save" else "Edit",
                                        tint = colors.primaryAccent,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                            if (onAddClick != null) {
                                Icon(
                                    painterResource(R.drawable.ic_xvox_add),
                                    "Add",
                                    tint = colors.primaryAccent,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .xvoxPressScale(onClick = onAddClick)
                                        .padding(14.dp)
                                )
                            }
                            Box(
                                Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .xvoxPressScale { close() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painterResource(R.drawable.ic_xvox_close),
                                    "Close $title",
                                    tint = colors.primaryText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(0.7.dp)
                                .background(colors.cardBorder.copy(alpha = 0.55f))
                        )

                        // Adaptive content container: hugs exact content height with no forced expansion
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(max = (maxBoxHeight - 64.dp).coerceAtLeast(100.dp))
                                .wrapContentHeight()
                                .padding(14.dp)
                        ) {
                            content()
                        }

                        if (bottomAction != null) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
                            ) {
                                bottomAction()
                            }
                        }
                    }
                }
            }
        }
    }
}

fun Modifier.xvoxBoxScroll(scrollState: Any? = null): Modifier = this

