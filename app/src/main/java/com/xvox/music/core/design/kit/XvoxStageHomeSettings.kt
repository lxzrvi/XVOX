package com.xvox.music.core.design.kit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.data.preferences.UserPreferencesRepository
import io.github.neoruaa.glasmio.glass.render.Backdrop

/**
 * DIAGNOSTIC BUILD: the three Settings sub-pages are empty, so anything that still fails belongs to
 * the Settings shell (main list, route state, chrome) and not to a page's content.
 */
@Composable
private fun PlaceholderPage(label: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(label, color = XvoxTheme.colors.secondaryText, fontSize = 15.sp)
    }
}

@Composable
fun XvoxStageHomeLayoutPage(prefs: UserPreferencesRepository, backdrop: Backdrop, onBack: () -> Unit) {
    PlaceholderPage("Home layout")
}

@Composable
fun XvoxStageLibraryPage(prefs: UserPreferencesRepository, backdrop: Backdrop, onBack: () -> Unit) {
    PlaceholderPage("Library and scanning")
}

@Composable
fun XvoxStageInterfacePage(prefs: UserPreferencesRepository, backdrop: Backdrop, onBack: () -> Unit) {
    PlaceholderPage("Interface")
}
