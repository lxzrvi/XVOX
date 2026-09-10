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
    var columns by remember { mutableIntStateOf(3) }; var rows by remember { mutableIntStateOf(1) }
    // Every widget size (1x1, 2x1, 3x1 …) owns its own settings, exactly as Home will apply them.
    val sizeKey = com.xvox.music.widget.WidgetCustomization.sizeKey(columns, rows)
    val c = state.widgetSizes[sizeKey] ?: state.widgetCustomization
    fun editWidget(change: (com.xvox.music.widget.WidgetCustomization) -> com.xvox.music.widget.WidgetCustomization) {
        viewModel.setWidgetSizeCustomization(sizeKey, change(c))
    }
    var panel by remember { mutableStateOf("general") }
    var labelId by remember { mutableStateOf("title") }; var buttonId by remember { mutableStateOf("play") }
    val width = columns * 70 + (columns - 1) * 8; val height = rows * 70 + (rows - 1) * 8
    val display = XvoxWidgetHelper.WidgetDisplayState(track.first?.title ?: "Your favourite track", track.first?.artist ?: "XVOX widget preview",
        track.first?.artworkUri ?: Uri.parse("android.resource://${context.packageName}/${R.drawable.xvox}"), track.second,
        transparency = state.widgetTransparency, theme = state.widgetTheme, customColor = state.widgetCustomColor,
        showLogo = state.widgetShowLogo, cornerRadiusDp = state.widgetCornerRadius, paddingX = state.widgetPaddingX,
        paddingY = state.widgetPaddingY, customization = c)
    fun label(change: (WidgetLabelStyle) -> WidgetLabelStyle) = editWidget { it.copy(labels = it.labels + (labelId to change(it.label(labelId)))) }
    fun button(change: (WidgetButtonStyle) -> WidgetButtonStyle) = editWidget { current ->
        current.copy(buttons = current.buttons.mapValues { (id, value) -> if (buttonId == "all" || id == buttonId) change(value) else value })
    }
    PinnedSettingsEditor(preview = {
        SettingsPreviewFrame("Widget · actual size") {
            Text("$columns × $rows · Surface · Cover · Text · Buttons", color = colors.primaryText, fontSize = 11.sp)
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
        }
    }, controls = {
        // Ordered the way the widget is actually built up: size → surface → cover → text →
        // buttons → install. Each panel only shows what belongs to it.
        Group("Preview size")
        SettingsChoiceRow((1..6).map { "$it" to "$it col" }, columns.toString()) { columns = it.toInt() }
        SettingsChoiceRow((1..6).map { "$it" to "$it row" }, rows.toString()) { rows = it.toInt() }

        SettingsChoiceRow(listOf("general" to "Surface", "cover" to "Cover", "labels" to "Text", "buttons" to "Buttons"), panel) { panel = it }

        when (panel) {
            "general" -> {
                Group("Theme")
                SettingsChoiceRow(listOf("Dynamic", "Dark", "AMOLED", "Light", "Glass", "Custom").map { it to it }, state.widgetTheme, viewModel::setWidgetTheme)
                if (state.widgetTheme == "Custom") WidgetColourEditor("Colour", state.widgetCustomColor, viewModel::setWidgetCustomColor)
                WidgetSlider("Transparency", state.widgetTransparency * 100, 0f..100f, "%") { viewModel.setWidgetTransparency(it / 100) }
                WidgetSlider("Corners", state.widgetCornerRadius.toFloat(), 0f..48f, "dp") { viewModel.setWidgetCornerRadius(it.roundToInt()) }

                Group("Spacing")
                WidgetSlider("Margin X", c.marginX.toFloat(), 0f..32f, "dp") { v -> editWidget { it.copy(marginX = v.roundToInt()) } }
                WidgetSlider("Margin Y", c.marginY.toFloat(), 0f..32f, "dp") { v -> editWidget { it.copy(marginY = v.roundToInt()) } }
                WidgetSlider("Padding X", state.widgetPaddingX.toFloat(), 0f..32f, "dp") { viewModel.setWidgetPaddingX(it.roundToInt()) }
                WidgetSlider("Padding Y", state.widgetPaddingY.toFloat(), 0f..28f, "dp") { viewModel.setWidgetPaddingY(it.roundToInt()) }

                Group("Alignment")
                SettingsChoiceRow(listOf("left" to "Left", "center" to "Centre", "right" to "Right"), c.alignment) { value -> editWidget { it.copy(alignment = value) } }
                SettingsChoiceRow(listOf("top" to "Top", "center" to "Middle", "bottom" to "Bottom"), c.verticalAlignment) { value -> editWidget { it.copy(verticalAlignment = value) } }

                Group("Border")
                WidgetSlider("Width", c.borderWidth, 0f..4f, "dp") { v -> editWidget { it.copy(borderWidth = (v * 4).roundToInt() / 4f) } }
                WidgetColourEditor("Colour", c.borderColor) { value -> editWidget { it.copy(borderColor = value) } }
            }

            "cover" -> {
                Group("Mode")
                SettingsToggle("Full cover", null, c.fullCover) { enabled -> editWidget { it.copy(fullCover = enabled) } }
                if (c.fullCover) WidgetSlider("Shade", c.fullCoverShade * 100, 0f..85f, "%") { v -> editWidget { it.copy(fullCoverShade = v / 100) } }

                Group("Placement")
                SettingsChoiceRow(listOf("auto" to "Auto", "left" to "Left", "right" to "Right", "top" to "Top", "bottom" to "Bottom", "hidden" to "Hidden"), c.coverPlacement) { value -> editWidget { it.copy(coverPlacement = value) } }
                SettingsChoiceRow(listOf(0, 24, 32, 40, 48, 56, 64, 80, 96, 120, 144, 160).map { "$it" to if (it == 0) "Auto" else "$it" }, c.coverSize.toString()) { value -> editWidget { it.copy(coverSize = value.toInt()) } }

                // Negative values are intentional: the cover may sit outside the widget box.
                Group("Offset · negative allowed")
                WidgetSlider("Margin X", c.coverMarginX.toFloat(), -32f..24f, "dp") { v -> editWidget { it.copy(coverMarginX = v.roundToInt()) } }
                WidgetSlider("Margin Y", c.coverMarginY.toFloat(), -32f..24f, "dp") { v -> editWidget { it.copy(coverMarginY = v.roundToInt()) } }
                WidgetSlider("Padding X", c.coverPaddingX.toFloat(), -32f..24f, "dp") { v -> editWidget { it.copy(coverPaddingX = v.roundToInt()) } }
                WidgetSlider("Padding Y", c.coverPaddingY.toFloat(), -32f..24f, "dp") { v -> editWidget { it.copy(coverPaddingY = v.roundToInt()) } }

                Group("Shape")
                SettingsToggle("Match widget corners", null, c.coverRadius < 0) { value -> editWidget { it.copy(coverRadius = if (value) -1 else 12) } }
                if (c.coverRadius >= 0) WidgetSlider("Radius", c.coverRadius.toFloat(), 0f..64f, "dp") { v -> editWidget { it.copy(coverRadius = v.roundToInt()) } }
                WidgetSlider("Border", c.coverBorderWidth, 0f..4f, "dp") { v -> editWidget { it.copy(coverBorderWidth = (v * 4).roundToInt() / 4f) } }
                WidgetColourEditor("Border colour", c.coverBorderColor) { value -> editWidget { it.copy(coverBorderColor = value) } }
            }

            "labels" -> {
                Group("Which text")
                SettingsChoiceRow(listOf("title" to "Song", "artist" to "Artist", "logo" to "Logo"), labelId) { labelId = it }
                val l = c.label(labelId)
                SettingsChoiceRow(listOf("auto" to "Auto", "show" to "Show", "hide" to "Hide"), l.visibility) { value -> label { it.copy(visibility = value) } }

                Group("Position")
                SettingsChoiceRow(listOf("top" to "Top", "center" to "Middle", "bottom" to "Bottom"), c.labelPlacement) { value -> editWidget { it.copy(labelPlacement = value) } }
                SettingsChoiceRow(listOf("left" to "Left", "center" to "Centre", "right" to "Right"), l.alignment) { value -> label { it.copy(alignment = value) } }
                // Cover text is allowed to leave the box, so these go negative too.
                WidgetSlider("Nudge X", l.offsetX.toFloat(), -48f..48f, "dp") { v -> label { it.copy(offsetX = v.roundToInt()) } }
                WidgetSlider("Nudge Y", l.offsetY.toFloat(), -48f..48f, "dp") { v -> label { it.copy(offsetY = v.roundToInt()) } }

                Group("Type")
                WidgetSlider("Size", l.size.toFloat(), 8f..28f, "sp") { v -> label { it.copy(size = v.roundToInt()) } }
                SettingsChoiceRow(listOf("inter" to "Inter", "cinzel" to "Cinzel", "hand" to "Hand"), l.font) { value -> label { it.copy(font = value) } }
                WidgetColourEditor("Colour", l.color) { value -> label { it.copy(color = value) } }

                Group("Box")
                WidgetColourEditor("Background", l.background) { value -> label { it.copy(background = value) } }
                WidgetSlider("Border", l.borderWidth, 0f..4f, "dp") { v -> label { it.copy(borderWidth = (v * 4).roundToInt() / 4f) } }
                WidgetColourEditor("Border colour", l.borderColor) { value -> label { it.copy(borderColor = value) } }
                WidgetSlider("Radius", l.radius.toFloat(), 0f..48f, "dp") { v -> label { it.copy(radius = v.roundToInt()) } }
            }

            else -> {
                Group("Which button")
                SettingsChoiceRow(listOf("all" to "All", "prev" to "Prev", "play" to "Play", "next" to "Next", "like" to "Like"), buttonId) { buttonId = it }
                val b = c.button(if (buttonId == "all") "play" else buttonId)

                Group("Position")
                SettingsChoiceRow(listOf("auto" to "Auto", "inline" to "Inline", "top" to "Top", "bottom" to "Bottom"), c.buttonsPlacement) { value -> editWidget { it.copy(buttonsPlacement = value) } }
                SettingsChoiceRow(listOf("auto" to "Auto", "left" to "Left", "center" to "Centre", "right" to "Right", "hidden" to "Hidden"), b.position) { value -> button { it.copy(position = value) } }
                if (buttonId != "all") {
                    Row {
                        for (direction in listOf(-1, 1)) TextButton(onClick = {
                            editWidget { old ->
                                val order = old.buttonOrder.toMutableList(); val index = order.indexOf(buttonId); val to = index + direction
                                if (index >= 0 && to in order.indices) order.add(to, order.removeAt(index))
                                old.copy(buttonOrder = order)
                            }
                        }) { Text(if (direction < 0) "Move left" else "Move right", fontSize = 11.sp) }
                    }
                    // Free placement: nudge the button anywhere, including outside its usual slot.
                    WidgetSlider("Nudge X", b.offsetX.toFloat(), -48f..48f, "dp") { v -> button { it.copy(offsetX = v.roundToInt()) } }
                    WidgetSlider("Nudge Y", b.offsetY.toFloat(), -48f..48f, "dp") { v -> button { it.copy(offsetY = v.roundToInt()) } }
                }

                Group("Shape")
                SettingsChoiceRow(listOf(0, 16, 20, 24, 28, 32, 36, 40, 44, 48).map { "$it" to if (it == 0) "Auto" else "$it" }, b.size.toString()) { value -> button { it.copy(size = value.toInt()) } }
                WidgetSlider("Inner padding", b.padding.toFloat(), 0f..14f, "dp") { v -> button { it.copy(padding = v.roundToInt()) } }
                WidgetSlider("Radius", b.radius.toFloat(), 0f..48f, "dp") { v -> button { it.copy(radius = v.roundToInt()) } }

                Group("Colour")
                WidgetColourEditor("Icon", b.color) { value -> button { it.copy(color = value) } }
                WidgetColourEditor("Background", b.background) { value -> button { it.copy(background = value) } }
                WidgetSlider("Border", b.borderWidth, 0f..4f, "dp") { v -> button { it.copy(borderWidth = (v * 4).roundToInt() / 4f) } }
                WidgetColourEditor("Border colour", b.borderColor) { value -> button { it.copy(borderColor = value) } }

                Group("Caption")
                SettingsToggle("Show caption", null, b.showLabel) { value -> button { it.copy(showLabel = value) } }
                if (b.showLabel) {
                    WidgetSlider("Caption size", b.labelSize.toFloat(), 6f..14f, "sp") { v -> button { it.copy(labelSize = v.roundToInt()) } }
                    WidgetColourEditor("Caption colour", b.labelColor) { value -> button { it.copy(labelColor = value) } }
                }
            }
        }

        Group("Install")
        Button(onClick = {
            val manager = AppWidgetManager.getInstance(context)
            if (manager.isRequestPinAppWidgetSupported) manager.requestPinAppWidget(ComponentName(context, XvoxAppWidgetProvider::class.java), null, null)
            else Toast.makeText(context, "Add XVOX from your launcher widget picker", Toast.LENGTH_LONG).show()
        }, modifier = Modifier.fillMaxWidth()) { Text("Add widget") }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { XvoxAppWidgetProvider.notifyWidgetUpdate(context) }) { Text("Refresh") }
            TextButton(onClick = { editWidget { WidgetCustomization() }; viewModel.setWidgetPaddingX(10); viewModel.setWidgetPaddingY(8); viewModel.setWidgetCornerRadius(16) }) { Text("Reset") }
        }
    })
}

/** Small group heading; the only text left in the widget editor. */
@Composable
private fun Group(title: String) {
    Text(
        title.uppercase(), color = XvoxTheme.colors.secondaryText, fontSize = 9.sp,
        letterSpacing = 1.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        modifier = Modifier.padding(top = 6.dp)
    )
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
