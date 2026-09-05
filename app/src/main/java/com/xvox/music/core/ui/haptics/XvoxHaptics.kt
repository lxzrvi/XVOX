package com.xvox.music.core.ui.haptics

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

class XvoxHaptics(
    context: Context? = null,
    var enabled: Boolean = false,
    var strength: String = "Off"
) {
    fun tap() {}
    fun click() {}
    fun toggle() {}
    fun sliderTick() {}
    fun heavy() {}
    fun success() {}
}

private val DefaultNoOpHaptics = XvoxHaptics()

val LocalXvoxHaptics: ProvidableCompositionLocal<XvoxHaptics> = staticCompositionLocalOf {
    DefaultNoOpHaptics
}

@Composable
fun rememberXvoxHaptics(
    enabled: Boolean = false,
    strength: String = "Off"
): XvoxHaptics {
    return remember { DefaultNoOpHaptics }
}
