package com.xvox.music.features.settings.sections

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.net.Uri
import android.os.Build
import android.util.SizeF
import android.widget.FrameLayout
import android.widget.RemoteViews
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.components.*
import com.xvox.music.player.playback.MainPlayerViewModel
import com.xvox.music.widget.XvoxAppWidgetProvider
import com.xvox.music.widget.XvoxWidgetHelper
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.roundToInt

@Composable
fun WidgetSettingsSection(state: SettingsState, viewModel: SettingsViewModel,
    playerViewModel: MainPlayerViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val colors = XvoxTheme.colors
    val context = LocalContext.current
    val previewTrack = remember(playerViewModel) { playerViewModel.state.map { player ->
        player.queue.firstOrNull { it.id == player.currentSongId } to player.isPlaying
    }.distinctUntilChanged() }
    val track by previewTrack.collectAsState(initial = null to false)
    val song = track.first
    var variant by remember { mutableStateOf("3×1") }
    val variants = remember { linkedMapOf(
        "1×2" to SizeF(80f, 150f), "1×3" to SizeF(80f, 230f), "1×4" to SizeF(80f, 310f),
        "2×1" to SizeF(160f, 70f), "3×1" to SizeF(240f, 70f), "4×1" to SizeF(320f, 70f), "2×2" to SizeF(160f, 160f)) }
    val size = variants.getValue(variant)
    val display = XvoxWidgetHelper.WidgetDisplayState(
        songTitle = song?.title ?: "Your favourite track", songArtist = song?.artist ?: "XVOX preview",
        artworkUri = song?.artworkUri ?: Uri.parse("android.resource://${context.packageName}/${R.drawable.xvox}"),
        isPlaying = track.second, currentPosition = (song?.duration ?: 240000L) / 3, duration = song?.duration ?: 240000L,
        transparency = state.widgetTransparency, theme = state.widgetTheme, customColor = state.widgetCustomColor,
        showLogo = state.widgetShowLogo, cornerRadiusDp = state.widgetCornerRadius,
        paddingX = state.widgetPaddingX, paddingY = state.widgetPaddingY)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsPreviewFrame("Widget · actual layout preview") {
            SettingsChoiceRow(variants.keys.map { it to it }, variant) { variant = it }
            Text("Columns × rows · previews use the same RemoteViews as your launcher", color = colors.secondaryText, fontSize = 10.sp)
            key(variant) {
            val views by produceState<RemoteViews?>(null, display, size) {
                value = XvoxWidgetHelper.buildRemoteViews(context, display, size.width.roundToInt(), size.height.roundToInt(), interactive = false)
            }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                UniformPreview(size.width.dp, size.height.dp, Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                    AndroidView(factory = { FrameLayout(it) }, modifier = Modifier.fillMaxSize(), update = { host ->
                        val remote = views
                        if (remote != null && host.tag !== remote) {
                            host.removeAllViews()
                            host.addView(remote.apply(context, host))
                            host.tag = remote
                        }
                    })
                }
            }
            }
        }
        Text("Padding X: ${state.widgetPaddingX} dp", color = colors.primaryText, fontSize = 12.sp)
        XvoxThinLineSlider(state.widgetPaddingX.toFloat(), { viewModel.setWidgetPaddingX(it.roundToInt()) }, 0f..32f, defaultValue = 10f)
        Text("Padding Y: ${state.widgetPaddingY} dp", color = colors.primaryText, fontSize = 12.sp)
        XvoxThinLineSlider(state.widgetPaddingY.toFloat(), { viewModel.setWidgetPaddingY(it.roundToInt()) }, 0f..28f, defaultValue = 8f)
        Text("Padding is inside the rounded border. Tiny sizes cap it to keep artwork and controls usable.", color = colors.secondaryText, fontSize = 11.sp)
        Text("Corner radius: ${state.widgetCornerRadius} dp", color = colors.primaryText, fontSize = 12.sp)
        XvoxThinLineSlider(state.widgetCornerRadius.toFloat(), { viewModel.setWidgetCornerRadius(it.roundToInt()) }, 0f..48f, defaultValue = 24f)
        Text("Transparency: ${(state.widgetTransparency * 100).roundToInt()}%", color = colors.primaryText, fontSize = 12.sp)
        XvoxThinLineSlider(state.widgetTransparency, viewModel::setWidgetTransparency, 0f..1f, defaultValue = .25f)
        SettingsChoiceRow(listOf("Dynamic", "AMOLED", "Dark", "Light", "Glass").map { it to it }, state.widgetTheme, viewModel::setWidgetTheme)
        SettingsToggle("Show X logo", "Hidden automatically only when the chosen widget is too small", state.widgetShowLogo, viewModel::setWidgetShowLogo)
        Button(onClick = {
            val manager = AppWidgetManager.getInstance(context)
            val provider = ComponentName(context, XvoxAppWidgetProvider::class.java)
            if (Build.VERSION.SDK_INT >= 26 && manager.isRequestPinAppWidgetSupported) manager.requestPinAppWidget(provider, null, null)
            else Toast.makeText(context, "Add XVOX from your launcher's widget picker", Toast.LENGTH_LONG).show()
        }, modifier = Modifier.fillMaxWidth()) { Text("Add widget to Home screen") }
        Button(onClick = { XvoxAppWidgetProvider.notifyWidgetUpdate(context) }, modifier = Modifier.fillMaxWidth()) { Text("Refresh active widgets") }
    }
}
