package com.xvox.music.core.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
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

/**
 * Backwards-compatible name for the application-wide option sheet.  Every overlay deliberately
 * routes through [XvoxSheet] so Profile, pickers, confirmations, Song Options, and Equalizer all
 * share the same full-width, bottom-touching behaviour.
 */
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
) = XvoxSheet(
    onDismiss = onDismiss,
    modifier = modifier,
    title = title,
    presentation = presentation,
    mini = mini,
    onAddClick = onAddClick,
    onBack = onBack,
    onSettingsClick = onSettingsClick,
    onUndoClick = onUndoClick,
    onEditClick = onEditClick,
    isEditing = isEditing,
    headerLeadingContent = headerLeadingContent,
    headerTitleContent = headerTitleContent,
    bottomAction = bottomAction,
    content = content
)

/**
 * Universal XVOX option sheet.
 *
 * It deliberately owns only the sheet chrome and a bounded content viewport. Existing pages keep
 * their own LazyColumn/verticalScroll state, which means they scroll internally once the sheet
 * has reached the status-bar boundary rather than leaking an unbounded vertical constraint.
 */
@Composable
fun XvoxSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "XVOX",
    presentation: XvoxBoxPresentation = XvoxBoxPresentation.DEFAULT,
    /** Retained for source compatibility; compact boxes are sheets too. */
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
    val scrimColor = if (colors.isLight) colors.primaryText else colors.background
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val dismiss by rememberUpdatedState(onDismiss)
    var visible by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }
    val swallowInteraction = remember { MutableInteractionSource() }

    fun close() {
        if (closing) return
        closing = true
        visible = false
        scope.launch {
            // Keep the host mounted until its downward exit has cleared the visible screen.
            delay(235)
            dismiss()
        }
    }

    LaunchedEffect(Unit) { visible = true }

    Dialog(
        onDismissRequest = ::close,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(scrimColor.copy(alpha = .40f))
                    .clickable(remember { MutableInteractionSource() }, indication = null) { close() }
            )

            BoxWithConstraints(
                modifier = Modifier
                    .matchParentSize()
                    .imePadding(),
                contentAlignment = Alignment.BottomCenter
            ) {
                val statusTopPx = WindowInsets.statusBars.getTop(density)
                val maxSheetHeight = with(density) {
                    (constraints.maxHeight - statusTopPx).coerceAtLeast(1).toDp()
                }
                val maxSheetHeightPx = with(density) { maxSheetHeight.toPx() }
                val equalizerPresentation = presentation == XvoxBoxPresentation.EQUALIZER
                val songOptionsPresentation = presentation == XvoxBoxPresentation.SONG_OPTIONS

                // A zero target means "fit its content". Equalizer intentionally begins around
                // sixty percent of the usable screen; dragging the pill upward can grow any sheet
                // until its top reaches the status-bar boundary.
                var requestedHeightPx by remember(presentation) { mutableFloatStateOf(0f) }
                var measuredHeightPx by remember { mutableIntStateOf(0) }
                var dragStartHeightPx by remember { mutableFloatStateOf(0f) }
                var dragDeltaPx by remember { mutableFloatStateOf(0f) }

                LaunchedEffect(maxSheetHeightPx, equalizerPresentation) {
                    if (equalizerPresentation && requestedHeightPx <= 0f) {
                        requestedHeightPx = maxSheetHeightPx * .60f
                    } else if (requestedHeightPx > maxSheetHeightPx) {
                        requestedHeightPx = maxSheetHeightPx
                    }
                }

                val requestedHeight: Dp? = requestedHeightPx
                    .takeIf { it > 0f }
                    ?.let { with(density) { it.coerceIn(1f, maxSheetHeightPx).toDp() } }
                val sheetShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
                val sheetSizing = if (requestedHeight != null) {
                    Modifier.height(requestedHeight)
                } else {
                    Modifier.heightIn(max = maxSheetHeight)
                }

                AnimatedVisibility(
                    visible = visible,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(280, easing = XvoxBoxEasing)
                    ) + fadeIn(tween(180, easing = XvoxBoxEasing)),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(220, easing = XvoxBoxEasing)
                    ) + fadeOut(tween(140, easing = XvoxBoxEasing))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(sheetSizing)
                            .clip(sheetShape)
                            .background(colors.card)
                            .clickable(swallowInteraction, indication = null) { }
                            .onGloballyPositioned { measuredHeightPx = it.size.height }
                            .semantics { paneTitle = title }
                    ) {
                        // This is a real drag pill, not a decorative handle. Downward release
                        // closes from wherever the sheet is currently expanded; upward movement
                        // grows the sheet and leaves the bounded body to scroll at its maximum.
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                                .pointerInput(maxSheetHeightPx, measuredHeightPx, presentation) {
                                    detectVerticalDragGestures(
                                        onDragStart = {
                                            dragDeltaPx = 0f
                                            dragStartHeightPx = maxOf(
                                                measuredHeightPx.toFloat(),
                                                requestedHeightPx,
                                                if (equalizerPresentation) maxSheetHeightPx * .60f else 0f
                                            )
                                        },
                                        onVerticalDrag = { change, amount ->
                                            change.consume()
                                            dragDeltaPx += amount
                                            if (dragDeltaPx < 0f) {
                                                requestedHeightPx = (dragStartHeightPx - dragDeltaPx)
                                                    .coerceIn(1f, maxSheetHeightPx)
                                            }
                                        },
                                        onDragEnd = {
                                            val dismissThreshold = with(density) { 52.dp.toPx() }
                                            if (dragDeltaPx > dismissThreshold) {
                                                close()
                                            } else if (dragDeltaPx < 0f) {
                                                requestedHeightPx = requestedHeightPx.coerceAtMost(maxSheetHeightPx)
                                            }
                                            dragDeltaPx = 0f
                                        },
                                        onDragCancel = { dragDeltaPx = 0f }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(colors.secondaryText.copy(alpha = .42f))
                            )
                        }

                        XvoxSheetHeader(
                            title = title,
                            songOptionsPresentation = songOptionsPresentation,
                            onBack = onBack,
                            onAddClick = onAddClick,
                            onSettingsClick = onSettingsClick,
                            onUndoClick = onUndoClick,
                            onEditClick = onEditClick,
                            isEditing = isEditing,
                            headerLeadingContent = headerLeadingContent,
                            headerTitleContent = headerTitleContent,
                            onClose = ::close
                        )

                        // Song Options specifically has no header separator. Its cards carry the
                        // rhythm instead, while other sheets retain one unobtrusive guide line.
                        if (!songOptionsPresentation) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(.7.dp)
                                    .background(colors.cardBorder.copy(alpha = .50f))
                            )
                        }

                        val bodyHorizontal = when {
                            songOptionsPresentation -> 12.dp
                            equalizerPresentation -> 16.dp
                            else -> 16.dp
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .heightIn(min = 0.dp)
                                .padding(horizontal = bodyHorizontal, vertical = 10.dp)
                        ) {
                            content()
                        }

                        bottomAction?.let { footer ->
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(.7.dp)
                                    .background(colors.cardBorder.copy(alpha = .50f))
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                footer()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun XvoxSheetHeader(
    title: String,
    songOptionsPresentation: Boolean,
    onBack: (() -> Unit)?,
    onAddClick: (() -> Unit)?,
    onSettingsClick: (() -> Unit)?,
    onUndoClick: (() -> Unit)?,
    onEditClick: (() -> Unit)?,
    isEditing: Boolean,
    headerLeadingContent: (@Composable () -> Unit)?,
    headerTitleContent: (@Composable () -> Unit)?,
    onClose: () -> Unit
) {
    val colors = XvoxTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 1.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        onBack?.let { back ->
            Icon(
                painter = painterResource(R.drawable.ic_xvox_arrow_left),
                contentDescription = "Back",
                tint = colors.primaryText,
                modifier = Modifier
                    .size(40.dp)
                    .xvoxPressScale(onClick = back)
                    .padding(10.dp)
            )
        }
        headerLeadingContent?.let {
            it()
            Spacer(Modifier.width(10.dp))
        }
        if (headerTitleContent != null) {
            Box(modifier = Modifier.weight(1f)) { headerTitleContent() }
        } else {
            Text(
                text = title,
                color = colors.primaryText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            onSettingsClick?.let { XvoxSheetHeaderAction(R.drawable.ic_xvox_settings, "Settings", it) }
            onUndoClick?.let { XvoxSheetHeaderAction(R.drawable.ic_xvox_undo, "Undo", it) }
            onEditClick?.let {
                XvoxSheetHeaderAction(
                    if (isEditing) R.drawable.ic_xvox_check else R.drawable.ic_xvox_edit,
                    if (isEditing) "Save" else "Edit",
                    it
                )
            }
            onAddClick?.let { XvoxSheetHeaderAction(R.drawable.ic_xvox_add, "Add", it) }
            XvoxSheetHeaderAction(
                icon = R.drawable.ic_xvox_close,
                description = "Close $title",
                onClick = onClose,
                tint = if (songOptionsPresentation) colors.secondaryText else colors.primaryText
            )
        }
    }
}

@Composable
private fun XvoxSheetHeaderAction(
    icon: Int,
    description: String,
    onClick: () -> Unit,
    tint: Color = XvoxTheme.colors.primaryAccent
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .xvoxPressScale(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}

/** Legacy hook retained for callers that already own their scroll state. */
fun Modifier.xvoxBoxScroll(scrollState: Any? = null): Modifier = this
