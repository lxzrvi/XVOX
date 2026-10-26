package com.xvox.music.core.ui.overlay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex

@Composable
fun XvoxOverlayHost(
    controller: XvoxOverlayController,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        controller.listContent?.let { content ->
            key(controller.listKey) {
                XvoxBox(
                    onDismiss = controller::hideBox,
                    title = controller.boxTitle,
                    presentation = controller.boxPresentation,
                    mini = controller.boxMini,
                    onSettingsClick = controller.boxSettingsAction,
                    onEditClick = controller.boxEditAction,
                    onUndoClick = controller.boxUndoAction,
                    headerTitleContent = controller.boxHeaderTitleContent,
                    bottomAction = controller.boxBottomAction,
                    modifier = Modifier.fillMaxSize()
                ) { content() }
            }
        }

        // The app owns one shared pill. It is drawn after sheets, so it always remains on top.
        controller.popup?.let { message ->
            XvoxP(
                message = message,
                onFinished = { controller.clearPopup(message.id) },
                modifier = Modifier.align(Alignment.TopCenter).zIndex(100f)
            )
        }
    }
}
