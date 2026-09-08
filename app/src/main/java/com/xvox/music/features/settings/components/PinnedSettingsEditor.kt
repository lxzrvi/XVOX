package com.xvox.music.features.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xvox.music.core.design.theme.XvoxTheme

/** Used inside XvoxBox: preview stays pinned; only the controls below it scroll. */
@Composable
fun PinnedSettingsEditor(preview: @Composable ColumnScope.() -> Unit, controls: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth().heightIn(max = 720.dp)) {
        val available = maxHeight.coerceAtMost(720.dp)
        Column(Modifier.fillMaxWidth().height(available)) {
            Column(Modifier.fillMaxWidth().heightIn(max = available * .43f).verticalScroll(rememberScrollState()), content = preview)
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(.7.dp).background(XvoxTheme.colors.cardBorder))
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp), content = controls)
        }
    }
}
