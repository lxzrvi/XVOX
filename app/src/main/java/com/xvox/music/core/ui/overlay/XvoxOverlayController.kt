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

@Stable
class XvoxOverlayController {
    var listKey by mutableLongStateOf(0L)
        private set

    internal var boxTitle by mutableStateOf("XVOX")
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

    fun showBox(title: String = "XVOX", content: @Composable () -> Unit) {
        boxMini = false
        boxTitle = title
        listKey++
        listContent = content
    }

    /** Compact PIP-style popup: quick actions that must not take over the whole screen. */
    fun showMiniBox(title: String = "XVOX", content: @Composable () -> Unit) {
        boxMini = true
        boxTitle = title
        listKey++
        listContent = content
    }

    fun hideBox() {
        listContent = null
        boxMini = false
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
