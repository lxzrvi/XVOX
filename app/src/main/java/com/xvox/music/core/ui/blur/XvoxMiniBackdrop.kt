// EXACT CODE REPLACE KARO (poori file) - koi extra kaam nahi. Path: app/src/main/java/com/xvox/music/core/ui/blur/XvoxMiniBackdrop.kt
package com.xvox.music.core.ui.blur

import androidx.compose.runtime.staticCompositionLocalOf
import top.yukonga.miuix.kmp.blur.LayerBackdrop

/**
 * Layer that records the Mini Player card. The glass navbar adds it to its own backdrop, so the card
 * stays visible (live-blurred) while it slides down behind the navbar. The card itself samples only
 * the page layer, never this one, so nothing is ever drawn into a layer it also reads.
 */
val LocalXvoxMiniBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }
