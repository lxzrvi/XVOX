package com.xvox.music.core.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val XvoxBoxEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/** One app-wide modal: solid opaque card with 100% adaptive content height (capped at max 90% screen height). */
@Composable
fun XvoxBox(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "XVOX",
    presentation: XvoxBoxPresentation = XvoxBoxPresentation.DEFAULT,
    mini: Boolean = false,
    onAddClick: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null,
    onUndoClick: (() -> Unit)? = null,
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
        animationSpec = tween(150),
        label = "scrimAlpha"
    )

    fun close() {
        if (closing) return
        closing = true
        visible = false
        scope.launch {
            delay(140)
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
                    enter = fadeIn(tween(180, easing = XvoxBoxEasing)),
                    exit = fadeOut(tween(140, easing = XvoxBoxEasing))
                ) {
                    val songOptionsPresentation = presentation == XvoxBoxPresentation.SONG_OPTIONS
                    val shape = when {
                        mini -> RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
                        songOptionsPresentation -> RoundedCornerShape(22.dp)
                        else -> RoundedCornerShape(26.dp)
                    }
                    // Match the compact reference panel in dark and AMOLED themes instead of
                    // inheriting the brighter elevated surface used by generic dialogs.
                    val boxFill = if (songOptionsPresentation && !colors.isLight) {
                        Color(0xFF1C1C1E)
                    } else {
                        colors.cardElevated
                    }

                    Column(
                        Modifier
                            .widthIn(max = if (mini) 520.dp else if (songOptionsPresentation) 420.dp else 560.dp)
                            .fillMaxWidth()
                            .heightIn(max = maxBoxHeight)
                            .wrapContentHeight()
                            .clip(shape)
                            .background(boxFill)
                            .then(
                                if (songOptionsPresentation) {
                                    Modifier.border(0.7.dp, colors.primaryText.copy(alpha = 0.10f), shape)
                                } else {
                                    Modifier
                                }
                            )
                            .clickable(swallowInteraction, indication = null) { }
                            .semantics { paneTitle = title }
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = 18.dp,
                                    end = if (songOptionsPresentation) 12.dp else 8.dp,
                                    top = 6.dp,
                                    bottom = 6.dp
                                ),
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

                            if (songOptionsPresentation) {
                                Row(
                                    modifier = Modifier
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(colors.primaryText.copy(alpha = if (colors.isLight) 0.04f else 0.055f))
                                        .border(0.7.dp, colors.primaryText.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
                                        .padding(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    onSettingsClick?.let { action ->
                                        XvoxBoxHeaderIconButton(
                                            icon = R.drawable.ic_xvox_settings,
                                            contentDescription = "Settings",
                                            tint = colors.primaryAccent,
                                            size = 32.dp,
                                            iconSize = 17.dp,
                                            onClick = action
                                        )
                                    }
                                    onUndoClick?.let { action ->
                                        XvoxBoxHeaderIconButton(
                                            icon = R.drawable.ic_xvox_undo,
                                            contentDescription = "Undo",
                                            tint = colors.primaryAccent,
                                            size = 32.dp,
                                            iconSize = 17.dp,
                                            onClick = action
                                        )
                                    }
                                    onEditClick?.let { action ->
                                        XvoxBoxHeaderIconButton(
                                            icon = if (isEditing) R.drawable.ic_xvox_check else R.drawable.ic_xvox_edit,
                                            contentDescription = if (isEditing) "Save" else "Edit",
                                            tint = colors.primaryAccent,
                                            size = 32.dp,
                                            iconSize = 17.dp,
                                            onClick = action
                                        )
                                    }
                                    onAddClick?.let { action ->
                                        XvoxBoxHeaderIconButton(
                                            icon = R.drawable.ic_xvox_add,
                                            contentDescription = "Add",
                                            tint = colors.primaryAccent,
                                            size = 32.dp,
                                            iconSize = 17.dp,
                                            onClick = action
                                        )
                                    }
                                    XvoxBoxHeaderIconButton(
                                        icon = R.drawable.ic_xvox_close,
                                        contentDescription = "Close $title",
                                        tint = colors.secondaryText,
                                        size = 32.dp,
                                        iconSize = 17.dp,
                                        onClick = ::close
                                    )
                                }
                            } else {
                                onSettingsClick?.let { action ->
                                    XvoxBoxHeaderIconButton(
                                        icon = R.drawable.ic_xvox_settings,
                                        contentDescription = "Settings",
                                        tint = colors.primaryAccent,
                                        size = 42.dp,
                                        iconSize = 19.dp,
                                        onClick = action
                                    )
                                }
                                onUndoClick?.let { action ->
                                    XvoxBoxHeaderIconButton(
                                        icon = R.drawable.ic_xvox_undo,
                                        contentDescription = "Undo",
                                        tint = colors.primaryAccent,
                                        size = 42.dp,
                                        iconSize = 19.dp,
                                        onClick = action
                                    )
                                }
                                onEditClick?.let { action ->
                                    XvoxBoxHeaderIconButton(
                                        icon = if (isEditing) R.drawable.ic_xvox_check else R.drawable.ic_xvox_edit,
                                        contentDescription = if (isEditing) "Save" else "Edit",
                                        tint = colors.primaryAccent,
                                        size = 42.dp,
                                        iconSize = 19.dp,
                                        onClick = action
                                    )
                                }
                                onAddClick?.let { action ->
                                    XvoxBoxHeaderIconButton(
                                        icon = R.drawable.ic_xvox_add,
                                        contentDescription = "Add",
                                        tint = colors.primaryAccent,
                                        size = 48.dp,
                                        iconSize = 20.dp,
                                        onClick = action
                                    )
                                }
                                XvoxBoxHeaderIconButton(
                                    icon = R.drawable.ic_xvox_close,
                                    contentDescription = "Close $title",
                                    tint = colors.primaryText,
                                    size = 48.dp,
                                    iconSize = 20.dp,
                                    onClick = ::close
                                )
                            }
                        }

                        if (!songOptionsPresentation) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(0.7.dp)
                                    .background(colors.cardBorder.copy(alpha = 0.55f))
                            )
                        }

                        // Adaptive content container: hugs exact content height with no forced expansion
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(max = (maxBoxHeight - if (songOptionsPresentation) 54.dp else 64.dp).coerceAtLeast(100.dp))
                                .wrapContentHeight()
                                .padding(if (songOptionsPresentation) 12.dp else 14.dp)
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

@Composable
private fun XvoxBoxHeaderIconButton(
    icon: Int,
    contentDescription: String,
    tint: Color,
    size: Dp,
    iconSize: Dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .xvoxPressScale(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

fun Modifier.xvoxBoxScroll(scrollState: Any? = null): Modifier = this
