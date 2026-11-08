// EXACT CODE REPLACE KARO (poori file) - koi extra kaam nahi. Path: app/src/main/java/com/xvox/music/core/ui/overlay/XvoxOverlayController.kt
package com.xvox.music.core.ui.overlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

@Immutable
data class XvoxPopupMessage(
    val id: Long,
    val text: String,
    val persistent: Boolean = false
)

/** Presentation variants for the shared overlay shell. */
enum class XvoxBoxPresentation {
    DEFAULT,
    SONG_OPTIONS,
    /** Bottom-sheet audio editor with the supplied Equalizer styling. */
    EQUALIZER,
    /** Profile sheet permits its avatar carousel to reach the sheet's horizontal edges. */
    PROFILE,
    /** Secondary/deeper choice presented as a centred dialog over its parent sheet. */
    CENTERED
}

@Stable
class XvoxOverlayController {
    var listKey by mutableLongStateOf(0L)
        private set

    /** True while a root-hosted sheet is open (not closing): the app layer blurs behind it. */
    var sheetBlurActive by mutableStateOf(false)

    internal var boxTitle by mutableStateOf("XVOX")
        private set

    internal var boxPresentation by mutableStateOf(XvoxBoxPresentation.DEFAULT)
        private set

    internal var boxSettingsAction by mutableStateOf<(() -> Unit)?>(null)
        private set

    /** Optional pencil action shown beside Settings (or in its slot when no gear is present). */
    internal var boxEditAction by mutableStateOf<(() -> Unit)?>(null)
        private set

    internal var boxUndoAction by mutableStateOf<(() -> Unit)?>(null)
        private set

    internal var boxHeaderTitleContent by mutableStateOf<(@Composable () -> Unit)?>(null)
        private set

    /** Optional leading header content, such as an artist avatar beside the standard title. */
    internal var boxHeaderLeadingContent by mutableStateOf<(@Composable () -> Unit)?>(null)
        private set

    /** Optional fixed footer rendered outside the sheet's scrolling content. */
    internal var boxBottomAction by mutableStateOf<(@Composable () -> Unit)?>(null)
        private set

    /** Called for every close path (footer, scrim, back), useful for transactional previews. */
    private var boxOnDismiss: (() -> Unit)? = null

    val isBoxVisible: Boolean get() = listContent != null

    /** A compact bottom-anchored PIP-style popup instead of the centred box. */
    internal var boxMini by mutableStateOf(false)
        private set

    internal var listContent by mutableStateOf<(@Composable () -> Unit)?>(null)
        private set

    internal var popup by mutableStateOf<XvoxPopupMessage?>(null)
        private set

    private var popupId by mutableLongStateOf(0L)

    fun showBox(
        title: String = "XVOX",
        onSettings: (() -> Unit)? = null,
        onEdit: (() -> Unit)? = null,
        onUndo: (() -> Unit)? = null,
        headerLeadingContent: (@Composable () -> Unit)? = null,
        headerTitleContent: (@Composable () -> Unit)? = null,
        bottomAction: (@Composable () -> Unit)? = null,
        onDismiss: (() -> Unit)? = null,
        presentation: XvoxBoxPresentation = XvoxBoxPresentation.DEFAULT,
        content: @Composable () -> Unit
    ) {
        boxMini = false
        boxTitle = title
        // A box opened from a visible box is a deeper choice.  It intentionally becomes centred
        // rather than creating a stack of bottom sheets.
        boxPresentation = if (isBoxVisible && !hideRequested && presentation == XvoxBoxPresentation.DEFAULT) {
            XvoxBoxPresentation.CENTERED
        } else {
            presentation
        }
        boxSettingsAction = onSettings
        boxEditAction = onEdit
        boxUndoAction = onUndo
        boxHeaderLeadingContent = headerLeadingContent
        boxHeaderTitleContent = headerTitleContent
        boxBottomAction = bottomAction
        boxOnDismiss = onDismiss
        hideRequested = false
        listKey++
        listContent = content
    }

    /** Compact PIP-style popup: quick actions that must not take over the whole screen. */
    fun showMiniBox(
        title: String = "XVOX",
        onSettings: (() -> Unit)? = null,
        onEdit: (() -> Unit)? = null,
        onUndo: (() -> Unit)? = null,
        headerLeadingContent: (@Composable () -> Unit)? = null,
        headerTitleContent: (@Composable () -> Unit)? = null,
        presentation: XvoxBoxPresentation = XvoxBoxPresentation.DEFAULT,
        content: @Composable () -> Unit
    ) {
        boxMini = true
        boxTitle = title
        boxPresentation = presentation
        boxSettingsAction = onSettings
        boxEditAction = onEdit
        boxUndoAction = onUndo
        boxHeaderLeadingContent = headerLeadingContent
        boxHeaderTitleContent = headerTitleContent
        boxBottomAction = null
        boxOnDismiss = null
        hideRequested = false
        listKey++
        listContent = content
    }

    /** Bumped by [hideBox]; the visible sheet observes it and plays its normal slide-down exit. */
    internal var closeSignal by mutableIntStateOf(0)
        private set

    /** Animated close: every X / Done / action button ends in the same smooth slide-down. */
    fun hideBox() {
        if (listContent == null) return
        hideRequested = true
        closeSignal++
    }

    /** True between hideBox() and the end of the exit animation: the box is already "gone" logically. */
    private var hideRequested = false

    /** Immediate teardown, called by the sheet AFTER its exit animation finished. */
    internal fun finishHide() {
        hideRequested = false
        val dismiss = boxOnDismiss
        boxOnDismiss = null
        listContent = null
        boxMini = false
        boxPresentation = XvoxBoxPresentation.DEFAULT
        boxSettingsAction = null
        boxEditAction = null
        boxUndoAction = null
        boxHeaderLeadingContent = null
        boxHeaderTitleContent = null
        boxBottomAction = null
        dismiss?.invoke()
    }

    fun showP(text: String) {
        popupId++
        popup = XvoxPopupMessage(id = popupId, text = text)
    }

    fun showPersistentP(text: String): Long {
        popupId++
        popup = XvoxPopupMessage(popupId, text, persistent = true)
        return popupId
    }
    fun dismissP(id: Long) { clearPopup(id) }
    internal fun clearPopup(id: Long) {
        if (popup?.id == id) {
            popup = null
        }
    }
}

val LocalXvoxOverlayController =
    staticCompositionLocalOf<XvoxOverlayController> {
        error("XvoxOverlayController not provided")
    }
