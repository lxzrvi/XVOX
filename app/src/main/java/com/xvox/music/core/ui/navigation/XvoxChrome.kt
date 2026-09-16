package com.xvox.music.core.ui.navigation

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/** Shared glass opacity and scroll insets. Content scrolls behind the floating chrome. */
object XvoxChrome {
    const val GlassAlpha = 0.88f
}
val LocalXvoxTopInset = staticCompositionLocalOf { 84.dp }
val LocalXvoxBottomInset = staticCompositionLocalOf { 180.dp }
