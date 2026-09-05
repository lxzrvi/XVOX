package com.xvox.music.core.ui.overlay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun XvoxOverlayHost(
    controller: XvoxOverlayController,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        controller.listContent?.let { content ->
            key(controller.listKey) {
                XvoxL(
                    onDismiss = controller::hideL,
                    modifier = Modifier.fillMaxSize()
                ) {
                    content()
                }
            }
        }

        controller.popup?.let { message ->
            XvoxP(
                message = message,
                onFinished = { controller.clearPopup(message.id) },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}
