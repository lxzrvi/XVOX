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
    val text: String
)

@Stable
class XvoxOverlayController {
    var listKey by mutableLongStateOf(0L)
        private set

    internal var listContent by mutableStateOf<(@Composable () -> Unit)?>(null)
        private set

    internal var boxContent by mutableStateOf<(@Composable () -> Unit)?>(null)
        private set

    internal var popup by mutableStateOf<XvoxPopupMessage?>(null)
        private set

    private var popupId by mutableLongStateOf(0L)

    fun showL(content: @Composable () -> Unit) {
        listKey++
        boxContent = null
        listContent = content
    }

    fun hideL() {
        listContent = null
        boxContent = null
    }

    fun showB(content: @Composable () -> Unit) {
        showL(content)
    }

    fun hideB() {
        hideL()
    }

    fun showP(text: String) {
        popupId++
        popup = XvoxPopupMessage(id = popupId, text = text)
    }

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
