package com.xvox.music.core.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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

/** Shared modal shell: centred cards by default, or adaptive bottom sheets for compact presentations. */
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
            // Let a bottom-sheet's downward motion finish before unmounting the Dialog.
            delay(if (presentation == XvoxBoxPresentation.DEFAULT && !mini) 140 else 230)
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
                        // Compact presentations deliberately retain equal side margins while
                        // docking to the bottom; the rest keep the centred dialog treatment.
                        horizontal = if (mini) 0.dp else if (presentation != XvoxBoxPresentation.DEFAULT) 16.dp else 20.dp,
                        vertical = if (mini || presentation != XvoxBoxPresentation.DEFAULT) 0.dp else 16.dp
                    ),
                contentAlignment = if (mini || presentation != XvoxBoxPresentation.DEFAULT) Alignment.BottomCenter else Alignment.Center
            ) {
                // Adaptive height up to maximum 90% of screen height
                val maxBoxHeight = maxHeight * if (presentation == XvoxBoxPresentation.DEFAULT && !mini) 0.90f else 0.92f

                AnimatedVisibility(
                    visible = visible,
                    enter = if (presentation == XvoxBoxPresentation.SONG_OPTIONS || presentation == XvoxBoxPresentation.EQUALIZER) {
                        slideInVertically(initialOffsetY = { it }, animationSpec = tween(280, easing = XvoxBoxEasing)) + fadeIn(tween(180, easing = XvoxBoxEasing))
                    } else fadeIn(tween(180, easing = XvoxBoxEasing)),
                    exit = if (presentation == XvoxBoxPresentation.SONG_OPTIONS || presentation == XvoxBoxPresentation.EQUALIZER) {
                        slideOutVertically(targetOffsetY = { it }, animationSpec = tween(220, easing = XvoxBoxEasing)) + fadeOut(tween(140, easing = XvoxBoxEasing))
                    } else fadeOut(tween(140, easing = XvoxBoxEasing))
                ) {
                    val songOptionsPresentation = presentation == XvoxBoxPresentation.SONG_OPTIONS
                    val equalizerPresentation = presentation == XvoxBoxPresentation.EQUALIZER
                    val compactPresentation = songOptionsPresentation || equalizerPresentation
                    val shape = when {
                        mini -> RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
                        songOptionsPresentation || equalizerPresentation -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        else -> RoundedCornerShape(26.dp)
                    }
                    // Song Options and Equalizer share the Settings-page canvas exactly. Their
                    // actionable surfaces use Settings-card colors inside this background.
                    // Match Settings section cards; option controls themselves use cardElevated.
                    val boxFill = if (compactPresentation) colors.card else colors.cardElevated

                    Column(
                        Modifier
                            .widthIn(max = if (mini) 520.dp else if (compactPresentation) 560.dp else 560.dp)
                            .fillMaxWidth()
                            .heightIn(max = maxBoxHeight)
                            .wrapContentHeight()
                            .clip(shape)
                            .background(boxFill)
                            .then(
                                if (compactPresentation) {
                                    Modifier.border(0.7.dp, colors.primaryText.copy(alpha = 0.10f), shape)
                                } else {
                                    Modifier
                                }
                            )
                            .clickable(swallowInteraction, indication = null) { }
                            .semantics { paneTitle = title }
                    ) {
                        @Composable
                        fun SongHeaderAction(
                            icon: Int,
                            contentDescription: String,
                            tint: Color = colors.primaryAccent,
                            onClick: () -> Unit
                        ) {
                            // Song Options intentionally exposes two independent circular actions
                            // rather than merging gear and close into one oversized pill.
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(colors.cardElevated)
                                    .border(0.8.dp, colors.cardBorder.copy(alpha = .78f), CircleShape)
                                    .xvoxPressScale(onClick = onClick),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(icon),
                                    contentDescription = contentDescription,
                                    tint = tint,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        @Composable
                        fun HeaderActions(compact: Boolean) {
                            if (songOptionsPresentation) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    onSettingsClick?.let { action ->
                                        SongHeaderAction(R.drawable.ic_xvox_settings, "Settings", onClick = action)
                                    }
                                    onUndoClick?.let { action ->
                                        SongHeaderAction(R.drawable.ic_xvox_undo, "Undo", onClick = action)
                                    }
                                    onEditClick?.let { action ->
                                        SongHeaderAction(
                                            if (isEditing) R.drawable.ic_xvox_check else R.drawable.ic_xvox_edit,
                                            if (isEditing) "Save" else "Edit",
                                            onClick = action
                                        )
                                    }
                                    onAddClick?.let { action ->
                                        SongHeaderAction(R.drawable.ic_xvox_add, "Add", onClick = action)
                                    }
                                    SongHeaderAction(
                                        icon = R.drawable.ic_xvox_close,
                                        contentDescription = "Close $title",
                                        tint = colors.secondaryText,
                                        onClick = ::close
                                    )
                                }
                            } else if (compact) {
                                // Sheets keep their contextual actions distinct and easy to target.
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    onSettingsClick?.let { action ->
                                        SongHeaderAction(R.drawable.ic_xvox_settings, "Settings", onClick = action)
                                    }
                                    onUndoClick?.let { action ->
                                        SongHeaderAction(R.drawable.ic_xvox_undo, "Undo", onClick = action)
                                    }
                                    onEditClick?.let { action ->
                                        SongHeaderAction(if (isEditing) R.drawable.ic_xvox_check else R.drawable.ic_xvox_edit, if (isEditing) "Save" else "Edit", onClick = action)
                                    }
                                    onAddClick?.let { action ->
                                        SongHeaderAction(R.drawable.ic_xvox_add, "Add", onClick = action)
                                    }
                                    SongHeaderAction(R.drawable.ic_xvox_close, "Close $title", tint = colors.secondaryText, onClick = ::close)
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    onSettingsClick?.let { action ->
                                        XvoxBoxHeaderIconButton(R.drawable.ic_xvox_settings, "Settings", colors.primaryAccent, 42.dp, 19.dp, action)
                                    }
                                    onUndoClick?.let { action ->
                                        XvoxBoxHeaderIconButton(R.drawable.ic_xvox_undo, "Undo", colors.primaryAccent, 42.dp, 19.dp, action)
                                    }
                                    onEditClick?.let { action ->
                                        XvoxBoxHeaderIconButton(if (isEditing) R.drawable.ic_xvox_check else R.drawable.ic_xvox_edit, if (isEditing) "Save" else "Edit", colors.primaryAccent, 42.dp, 19.dp, action)
                                    }
                                    onAddClick?.let { action ->
                                        XvoxBoxHeaderIconButton(R.drawable.ic_xvox_add, "Add", colors.primaryAccent, 48.dp, 20.dp, action)
                                    }
                                    XvoxBoxHeaderIconButton(R.drawable.ic_xvox_close, "Close $title", colors.primaryText, 48.dp, 20.dp, ::close)
                                }
                            }
                        }

                        if (compactPresentation) {
                            // Grab handle is intentionally plain: no border, no extra container.
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 9.dp, bottom = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    Modifier
                                        .width(34.dp)
                                        .height(4.dp)
                                        .clip(CircleShape)
                                        .background(colors.secondaryText.copy(alpha = .42f))
                                )
                            }
                        }

                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = if (compactPresentation) 16.dp else 18.dp,
                                    end = if (compactPresentation) 12.dp else 8.dp,
                                    top = 4.dp,
                                    bottom = 8.dp
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
                                Box(modifier = Modifier.weight(1f)) { headerTitleContent() }
                            } else {
                                Text(
                                    title,
                                    color = colors.primaryText,
                                    fontSize = if (compactPresentation) 16.sp else 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            HeaderActions(compact = songOptionsPresentation || equalizerPresentation)
                        }
                        // A restrained divider separates the heading from controls.
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(0.7.dp)
                                .background(colors.cardBorder.copy(alpha = .55f))
                        )

                        // Reserve footer space before measuring scrollable content. The Equalizer
                        // action row therefore remains pinned while only its controls scroll.
                        // Row height includes its 6dp top/bottom padding and the divider below.
                        val headerReserve = if (compactPresentation) 78.dp else 68.dp
                        val footerReserve = if (bottomAction == null) 0.dp else 68.dp
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(max = (maxBoxHeight - headerReserve - footerReserve).coerceAtLeast(100.dp))
                                .wrapContentHeight()
                                .padding(horizontal = if (compactPresentation) 0.dp else 14.dp, vertical = 12.dp)
                        ) {
                            content()
                        }

                        if (bottomAction != null) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(.7.dp)
                                    .background(colors.cardBorder.copy(alpha = .55f))
                            )
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = if (compactPresentation) 0.dp else 14.dp,
                                        top = 10.dp,
                                        end = if (compactPresentation) 0.dp else 14.dp,
                                        bottom = 12.dp
                                    )
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
