package com.xvox.music.features.settings.sections

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.net.Uri
import android.widget.FrameLayout
import android.widget.RemoteViews
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
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
import com.xvox.music.widget.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt

@Composable
fun WidgetSettingsSection(state: SettingsState, viewModel: SettingsViewModel,
    playerViewModel: MainPlayerViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val context = LocalContext.current
    val colors = XvoxTheme.colors
    val trackFlow = remember(playerViewModel) { playerViewModel.state.map { p -> p.queue.firstOrNull { it.id == p.currentSongId } to p.isPlaying }.distinctUntilChanged() }
    val track by trackFlow.collectAsState(initial = null to false)
    val c = state.widgetCustomization
    var columns by remember { mutableIntStateOf(3) }; var rows by remember { mutableIntStateOf(1) }
    var panel by remember { mutableStateOf("general") }
    var labelId by remember { mutableStateOf("title") }; var buttonId by remember { mutableStateOf("play") }
    val width = columns * 70 + (columns - 1) * 8; val height = rows * 70 + (rows - 1) * 8
    val display = XvoxWidgetHelper.WidgetDisplayState(track.first?.title ?: "Your favourite track", track.first?.artist ?: "XVOX widget preview",
        track.first?.artworkUri ?: Uri.parse("android.resource://${context.packageName}/${R.drawable.xvox}"), track.second,
        transparency = state.widgetTransparency, theme = state.widgetTheme, customColor = state.widgetCustomColor,
        showLogo = state.widgetShowLogo, cornerRadiusDp = state.widgetCornerRadius, paddingX = state.widgetPaddingX,
        paddingY = state.widgetPaddingY, customization = c)
    fun label(change: (WidgetLabelStyle) -> WidgetLabelStyle) = viewModel.updateWidget { it.copy(labels = it.labels + (labelId to change(it.label(labelId)))) }
    fun button(change: (WidgetButtonStyle) -> WidgetButtonStyle) = viewModel.updateWidget { current ->
        current.copy(buttons = current.buttons.mapValues { (id, value) -> if (buttonId == "all" || id == buttonId) change(value) else value })
    }
    PinnedSettingsEditor(preview = {
        SettingsPreviewFrame("Widget · actual layout") {
            Text("Columns × rows: $columns × $rows", color = colors.primaryText, fontSize = 12.sp)
            key(columns, rows) {
                val views by produceState<RemoteViews?>(null, display) {
                    value = XvoxWidgetHelper.buildRemoteViews(context, display, width, height, interactive = false)
                }
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    UniformPreview(width.dp, height.dp, Modifier.fillMaxWidth().heightIn(max = 170.dp)) {
                        AndroidView(factory = { FrameLayout(it) }, modifier = Modifier.fillMaxSize(), update = { host ->
                            views?.let { rv -> if (host.tag !== rv) { host.removeAllViews(); host.addView(rv.apply(context, host)); host.tag = rv } }
                        })
                    }
                }
            }
            Text("36 preview sizes. Launcher cell sizes vary; small widgets clamp cover/button dimensions to fit. No progress bar.", color = colors.secondaryText, fontSize = 10.sp)
        }
    }, controls = {
            SettingsChoiceRow((1..6).map { "$it" to "$it columns" }, columns.toString()) { columns = it.toInt() }
            SettingsChoiceRow((1..6).map { "$it" to "$it rows" }, rows.toString()) { rows = it.toInt() }

        SettingsChoiceRow(listOf("general" to "Widget", "cover" to "Cover", "labels" to "Text", "buttons" to "Buttons"), panel) { panel = it }
        when (panel) {
            "general" -> {
                WidgetSlider("Margin X", c.marginX.toFloat(), 0f..32f, "dp") { v -> viewModel.updateWidget { it.copy(marginX = v.roundToInt()) } }
                WidgetSlider("Margin Y", c.marginY.toFloat(), 0f..32f, "dp") { v -> viewModel.updateWidget { it.copy(marginY = v.roundToInt()) } }
                WidgetSlider("Padding X", state.widgetPaddingX.toFloat(), 0f..32f, "dp") { viewModel.setWidgetPaddingX(it.roundToInt()) }
                WidgetSlider("Padding Y", state.widgetPaddingY.toFloat(), 0f..28f, "dp") { viewModel.setWidgetPaddingY(it.roundToInt()) }
                Text("Content alignment", color = colors.primaryText, fontSize = 12.sp)
                SettingsChoiceRow(listOf("left", "center", "right").map { it to it }, c.alignment) { value -> viewModel.updateWidget { it.copy(alignment = value) } }
                Text("Vertical alignment", color = colors.primaryText, fontSize = 12.sp)
                SettingsChoiceRow(listOf("top", "center", "bottom").map { it to it }, c.verticalAlignment) { value -> viewModel.updateWidget { it.copy(verticalAlignment = value) } }
                WidgetSlider("Corner radius", state.widgetCornerRadius.toFloat(), 0f..48f, "dp") { viewModel.setWidgetCornerRadius(it.roundToInt()) }
                WidgetSlider("Widget border", c.borderWidth, 0f..4f, "dp") { v -> viewModel.updateWidget { it.copy(borderWidth = (v * 4).roundToInt() / 4f) } }
                WidgetColourEditor("Border colour", c.borderColor) { value -> viewModel.updateWidget { it.copy(borderColor = value) } }
                WidgetSlider("Transparency", state.widgetTransparency * 100, 0f..100f, "%") { viewModel.setWidgetTransparency(it / 100) }
                SettingsChoiceRow(listOf("Dynamic", "Dark", "AMOLED", "Light", "Glass", "Custom").map { it to it }, state.widgetTheme, viewModel::setWidgetTheme)
                if (state.widgetTheme == "Custom") WidgetColourEditor("Widget colour", state.widgetCustomColor, viewModel::setWidgetCustomColor)
            }
            "cover" -> {
                SettingsToggle("Full cover", "Fill the widget background with the cover; labels and every button remain editable.", c.fullCover) { enabled -> viewModel.updateWidget { it.copy(fullCover = enabled) } }
                if (c.fullCover) WidgetSlider("Cover shade", c.fullCoverShade * 100, 0f..85f, "%") { v -> viewModel.updateWidget { it.copy(fullCoverShade = v / 100) } }
                Text("Separate cover placement", color = colors.primaryText, fontSize = 12.sp)
                SettingsChoiceRow(listOf("auto", "left", "right", "top", "bottom", "hidden").map { it to it }, c.coverPlacement) { value -> viewModel.updateWidget { it.copy(coverPlacement = value) } }
                WidgetSlider("Cover margin X", c.coverMarginX.toFloat(), 0f..24f, "dp") { v -> viewModel.updateWidget { it.copy(coverMarginX = v.roundToInt()) } }
                WidgetSlider("Cover margin Y", c.coverMarginY.toFloat(), 0f..24f, "dp") { v -> viewModel.updateWidget { it.copy(coverMarginY = v.roundToInt()) } }
                WidgetSlider("Cover padding X", c.coverPaddingX.toFloat(), 0f..24f, "dp") { v -> viewModel.updateWidget { it.copy(coverPaddingX = v.roundToInt()) } }
                WidgetSlider("Cover padding Y", c.coverPaddingY.toFloat(), 0f..24f, "dp") { v -> viewModel.updateWidget { it.copy(coverPaddingY = v.roundToInt()) } }
                Text("Cover size", color = colors.primaryText, fontSize = 12.sp)
                SettingsChoiceRow(listOf(0, 24, 32, 40, 48, 56, 64, 80, 96, 120, 144, 160).map { "$it" to if (it == 0) "Auto" else "$it dp" }, c.coverSize.toString()) { value -> viewModel.updateWidget { it.copy(coverSize = value.toInt()) } }
                Text("Cover sizes above 48 dp work in taller widgets. A one-row widget still cannot fit a cover taller than its usable height; reduce spacing or select more preview rows.", color = colors.secondaryText, fontSize = 10.sp)
                SettingsToggle("Follow widget corners", "Use matching inset artwork corners", c.coverRadius < 0) { value -> viewModel.updateWidget { it.copy(coverRadius = if (value) -1 else 12) } }
                if (c.coverRadius >= 0) WidgetSlider("Cover radius", c.coverRadius.toFloat(), 0f..64f, "dp") { v -> viewModel.updateWidget { it.copy(coverRadius = v.roundToInt()) } }
                WidgetSlider("Cover border", c.coverBorderWidth, 0f..4f, "dp") { v -> viewModel.updateWidget { it.copy(coverBorderWidth = (v * 4).roundToInt() / 4f) } }
                WidgetColourEditor("Cover border colour", c.coverBorderColor) { value -> viewModel.updateWidget { it.copy(coverBorderColor = value) } }
                if (c.fullCover) Text("Cover margin/padding also inset full-cover artwork. Separate cover size/placement are kept for card mode.", color = colors.secondaryText, fontSize = 10.sp)
            }
            "labels" -> {
                Text("Text placement", color = colors.primaryText, fontSize = 12.sp)
                SettingsChoiceRow(listOf("top", "center", "bottom").map { it to it }, c.labelPlacement) { value -> viewModel.updateWidget { it.copy(labelPlacement = value) } }
                SettingsChoiceRow(listOf("title" to "Song", "artist" to "Artist", "logo" to "Logo"), labelId) { labelId = it }
                val l = c.label(labelId)
                SettingsChoiceRow(listOf("auto" to "Auto", "show" to "Show", "hide" to "Hide"), l.visibility) { value -> label { it.copy(visibility = value) } }
                WidgetSlider("Font size", l.size.toFloat(), 8f..28f, "sp") { v -> label { it.copy(size = v.roundToInt()) } }
                SettingsChoiceRow(listOf("inter" to "Inter", "cinzel" to "Cinzel", "hand" to "Handwritten"), l.font) { value -> label { it.copy(font = value) } }
                SettingsChoiceRow(listOf("left", "center", "right").map { it to it }, l.alignment) { value -> label { it.copy(alignment = value) } }
                WidgetColourEditor("Text colour", l.color) { value -> label { it.copy(color = value) } }
                WidgetColourEditor("Text background", l.background) { value -> label { it.copy(background = value) } }
                WidgetSlider("Text border", l.borderWidth, 0f..4f, "dp") { v -> label { it.copy(borderWidth = (v * 4).roundToInt() / 4f) } }
                WidgetColourEditor("Text border colour", l.borderColor) { value -> label { it.copy(borderColor = value) } }
                WidgetSlider("Text box radius", l.radius.toFloat(), 0f..48f, "dp") { v -> label { it.copy(radius = v.roundToInt()) } }
            }
            else -> {
                Text("Controls placement", color = colors.primaryText, fontSize = 12.sp)
                SettingsChoiceRow(listOf("auto", "inline", "top", "bottom").map { it to it }, c.buttonsPlacement) { value -> viewModel.updateWidget { it.copy(buttonsPlacement = value) } }
                SettingsChoiceRow(listOf("all" to "All", "prev" to "Previous", "play" to "Play", "next" to "Next", "like" to "Like"), buttonId) { buttonId = it }
                val b = c.button(if (buttonId == "all") "play" else buttonId)
                SettingsChoiceRow(listOf("auto", "left", "center", "right", "hidden").map { it to it }, b.position) { value -> button { it.copy(position = value) } }
                SettingsChoiceRow(listOf(0, 16, 20, 24, 28, 32, 36, 40, 44, 48).map { "$it" to if (it == 0) "Auto size" else "$it dp" }, b.size.toString()) { value -> button { it.copy(size = value.toInt()) } }
                SettingsToggle("Button label", "Show or hide the caption for this button", b.showLabel) { value -> button { it.copy(showLabel = value) } }
                if (b.showLabel) {
                    WidgetSlider("Button label size", b.labelSize.toFloat(), 6f..14f, "sp") { v -> button { it.copy(labelSize = v.roundToInt()) } }
                    WidgetColourEditor("Button label colour", b.labelColor) { value -> button { it.copy(labelColor = value) } }
                }
                WidgetSlider("Button inner padding", b.padding.toFloat(), 0f..14f, "dp") { v -> button { it.copy(padding = v.roundToInt()) } }
                WidgetColourEditor("Icon colour", b.color) { value -> button { it.copy(color = value) } }
                WidgetColourEditor("Button background", b.background) { value -> button { it.copy(background = value) } }
                WidgetSlider("Button radius", b.radius.toFloat(), 0f..48f, "dp") { v -> button { it.copy(radius = v.roundToInt()) } }
                WidgetSlider("Button border", b.borderWidth, 0f..4f, "dp") { v -> button { it.copy(borderWidth = (v * 4).roundToInt() / 4f) } }
                WidgetColourEditor("Button border colour", b.borderColor) { value -> button { it.copy(borderColor = value) } }
                if (buttonId != "all") {
                    Text("Order: ${c.buttonOrder.joinToString(" · ")}", color = colors.secondaryText, fontSize = 10.sp)
                    Row {
                        for (direction in listOf(-1, 1)) TextButton(onClick = {
                            viewModel.updateWidget { old ->
                                val order = old.buttonOrder.toMutableList(); val index = order.indexOf(buttonId); val to = index + direction
                                if (index >= 0 && to in order.indices) order.add(to, order.removeAt(index))
                                old.copy(buttonOrder = order)
                            }
                        }) { Text(if (direction < 0) "Move earlier" else "Move later", fontSize = 11.sp) }
                    }
                }
                Text("Top/bottom controls use left/centre/right zones. Inline controls sit around the cover and text. Explicitly hidden buttons stay hidden.", color = colors.secondaryText, fontSize = 10.sp)
            }
        }
        Button(onClick = {
            val manager = AppWidgetManager.getInstance(context)
            if (manager.isRequestPinAppWidgetSupported) manager.requestPinAppWidget(ComponentName(context, XvoxAppWidgetProvider::class.java), null, null)
            else Toast.makeText(context, "Add XVOX from your launcher widget picker", Toast.LENGTH_LONG).show()
        }, modifier = Modifier.fillMaxWidth()) { Text("Add widget") }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { XvoxAppWidgetProvider.notifyWidgetUpdate(context) }) { Text("Refresh widgets") }
            TextButton(onClick = { viewModel.updateWidget { WidgetCustomization() }; viewModel.setWidgetPaddingX(10); viewModel.setWidgetPaddingY(8); viewModel.setWidgetCornerRadius(16) }) { Text("Reset layout") }
        }
    })
}

@Composable
private fun WidgetSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, unit: String, onChange: (Float) -> Unit) {
    Text("$label: ${if (unit == "dp" && range.endInclusive <= 4) "%.2f".format(value) else value.roundToInt()} $unit", color = XvoxTheme.colors.primaryText, fontSize = 12.sp)
    XvoxThinLineSlider(value, onChange, range)
}

@Composable
private fun WidgetColourEditor(label: String, value: String, onChange: (String) -> Unit) {
    val colors = XvoxTheme.colors
    var text by remember(label, value) { mutableStateOf(if (value.startsWith("#")) value else "") }
    Text(label, color = colors.primaryText, fontSize = 12.sp)
    SettingsChoiceRow(listOf("Auto" to "Auto", "Transparent" to "None", "#FFFFFF" to "White", "#181818" to "Black",
        "#79BDED" to "Blue", "#E6AB6C" to "Gold"), value, onChange)
    OutlinedTextField(value = text, onValueChange = {
        if (it.length <= 9) {
            text = it
            if (it.matches(Regex("#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?"))) onChange(it)
        }
    }, label = { Text("Custom #RRGGBB or #AARRGGBB", fontSize = 10.sp) }, singleLine = true,
        modifier = Modifier.fillMaxWidth(), isError = text.isNotEmpty() && !text.matches(Regex("#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?")))
}
