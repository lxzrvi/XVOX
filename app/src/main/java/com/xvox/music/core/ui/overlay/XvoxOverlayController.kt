package com.xvox.music.core.ui.overlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
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
    /** Secondary/deeper choice presented as a centred dialog over its parent sheet. */
    CENTERED
}

@Stable
class XvoxOverlayController {
    var listKey by mutableLongStateOf(0L)
        private set

    internal var boxTitle by mutableStateOf("XVOX")
        private set

    internal var boxPresentation by mutableStateOf(XvoxBoxPresentation.DEFAULT)
        private set

    internal var boxSettingsAction by mutableStateOf<(() -> Unit)?>(null)
        private set

    internal var boxUndoAction by mutableStateOf<(() -> Unit)?>(null)
        private set

    internal var boxHeaderTitleContent by mutableStateOf<(@Composable () -> Unit)?>(null)
        private set

    /** Optional fixed footer rendered outside the sheet's scrolling content. */
    internal var boxBottomAction by mutableStateOf<(@Composable () -> Unit)?>(null)
        private set

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
        onUndo: (() -> Unit)? = null,
        headerTitleContent: (@Composable () -> Unit)? = null,
        bottomAction: (@Composable () -> Unit)? = null,
        presentation: XvoxBoxPresentation = XvoxBoxPresentation.DEFAULT,
        content: @Composable () -> Unit
    ) {
        boxMini = false
        boxTitle = title
        // A box opened from a visible box is a deeper choice.  It intentionally becomes centred
        // rather than creating a stack of bottom sheets.
        boxPresentation = if (isBoxVisible && presentation == XvoxBoxPresentation.DEFAULT) {
            XvoxBoxPresentation.CENTERED
        } else {
            presentation
        }
        boxSettingsAction = onSettings
        boxUndoAction = onUndo
        boxHeaderTitleContent = headerTitleContent
        boxBottomAction = bottomAction
        listKey++
        listContent = content
    }

    /** Compact PIP-style popup: quick actions that must not take over the whole screen. */
    fun showMiniBox(
        title: String = "XVOX",
        onSettings: (() -> Unit)? = null,
        onUndo: (() -> Unit)? = null,
        headerTitleContent: (@Composable () -> Unit)? = null,
        presentation: XvoxBoxPresentation = XvoxBoxPresentation.DEFAULT,
        content: @Composable () -> Unit
    ) {
        boxMini = true
        boxTitle = title
        boxPresentation = presentation
        boxSettingsAction = onSettings
        boxUndoAction = onUndo
        boxHeaderTitleContent = headerTitleContent
        boxBottomAction = null
        listKey++
        listContent = content
    }

    fun hideBox() {
        listContent = null
        boxMini = false
        boxPresentation = XvoxBoxPresentation.DEFAULT
        boxSettingsAction = null
        boxUndoAction = null
        boxHeaderTitleContent = null
        boxBottomAction = null
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
