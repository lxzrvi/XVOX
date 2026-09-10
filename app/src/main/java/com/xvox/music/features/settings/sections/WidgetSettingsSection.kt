package com.xvox.music.features.settings.sections

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.components.*
import com.xvox.music.widget.WidgetCustomization
import com.xvox.music.widget.XvoxAppWidgetProvider
import kotlin.math.roundToInt

@Composable
fun WidgetSettingsSection(state: SettingsState, viewModel: SettingsViewModel) {
    val context = LocalContext.current
    var editingSize by remember { mutableStateOf(state.widgetPreviewSize) }
    var sizeTab by remember { mutableStateOf("cover") }
    var labelId by remember { mutableStateOf("title") }
    var buttonId by remember { mutableStateOf("all") }
    var expandedGroup by remember { mutableStateOf<String?>("Size") }

    fun toggle(group: String) {
        expandedGroup = if (expandedGroup == group) null else group
    }

    val sizeKey = editingSize
    val c = state.widgetSizes[sizeKey] ?: state.widgetCustomization

    fun editWidget(transform: (WidgetCustomization) -> WidgetCustomization) {
        val updated = transform(c).sanitized()
        viewModel.setWidgetCustomization(updated)
        viewModel.setWidgetCustomizationForSize(sizeKey, updated)
    }

    LaunchedEffect(sizeKey) {
        viewModel.setWidgetPreviewSize(sizeKey)
    }

    fun label(transform: (com.xvox.music.widget.WidgetLabelStyle) -> com.xvox.music.widget.WidgetLabelStyle) {
        val current = c.label(labelId)
        editWidget { it.copy(labels = it.labels + (labelId to transform(current))) }
    }

    fun button(transform: (com.xvox.music.widget.WidgetButtonStyle) -> com.xvox.music.widget.WidgetButtonStyle) {
        if (buttonId == "all") {
            val updated = c.buttons.mapValues { (_, style) -> transform(style) }
            editWidget { it.copy(buttons = updated) }
        } else {
            val current = c.button(buttonId)
            editWidget { it.copy(buttons = it.buttons + (buttonId to transform(current))) }
        }
    }

    SettingsControlsEditor(controls = {
        SettingsAccordionItem(
            title = "Widget size · $editingSize",
            expanded = expandedGroup == "Size",
            onToggle = { toggle("Size") }
        ) {
            SettingsChoiceRow(
                listOf("2x1" to "2×1", "3x1" to "3×1", "4x1" to "4×1", "2x2" to "2×2", "3x2" to "3×2", "4x2" to "4×2", "3x3" to "3×3", "4x4" to "4×4"),
                editingSize
            ) { chosen ->
                editingSize = chosen
                viewModel.setWidgetPreviewSize(chosen)
            }
        }

        SettingsAccordionItem(
            title = "Surface & Transparency",
            expanded = expandedGroup == "Surface",
            onToggle = { toggle("Surface") }
        ) {
            WidgetSlider("Transparency", state.widgetTransparency * 100, 0f..100f, "%") { v -> viewModel.setWidgetTransparency(v / 100) }
            Spacer(Modifier.height(8.dp))
            WidgetSlider("Corner radius", state.widgetCornerRadius.toFloat(), 0f..32f, "dp") { v -> viewModel.setWidgetCornerRadius(v.roundToInt()) }
            Spacer(Modifier.height(8.dp))
            WidgetSlider("Border", c.borderWidth, 0f..4f, "dp") { v -> editWidget { it.copy(borderWidth = (v * 4).roundToInt() / 4f) } }
            Spacer(Modifier.height(8.dp))
            WidgetColourEditor("Border colour", c.borderColor) { value -> editWidget { it.copy(borderColor = value) } }
            Spacer(Modifier.height(8.dp))
            WidgetSlider("Padding X", state.widgetPaddingX.toFloat(), 0f..32f, "dp") { v -> viewModel.setWidgetPaddingX(v.roundToInt()) }
            Spacer(Modifier.height(8.dp))
            WidgetSlider("Padding Y", state.widgetPaddingY.toFloat(), 0f..32f, "dp") { v -> viewModel.setWidgetPaddingY(v.roundToInt()) }
        }

        SettingsAccordionItem(
            title = "Cover artwork",
            expanded = expandedGroup == "Cover",
            onToggle = { toggle("Cover") }
        ) {
            SettingsToggle("Full cover backdrop", null, c.fullCover) { enabled -> editWidget { it.copy(fullCover = enabled) } }
            if (c.fullCover) {
                Spacer(Modifier.height(8.dp))
                WidgetSlider("Shade", c.fullCoverShade * 100, 0f..85f, "%") { v -> editWidget { it.copy(fullCoverShade = v / 100) } }
            }

            Spacer(Modifier.height(8.dp))
            Group("Placement")
            SettingsChoiceRow(listOf("auto" to "Auto", "left" to "Left", "right" to "Right", "top" to "Top", "bottom" to "Bottom", "hidden" to "Hidden"), c.coverPlacement) { value -> editWidget { it.copy(coverPlacement = value) } }
            Spacer(Modifier.height(8.dp))
            SettingsChoiceRow(listOf(0, 32, 48, 64, 80, 96, 120, 144, 180, 220, 260, 300).map { "$it" to if (it == 0) "Auto" else "$it" }, c.coverSize.toString()) { value -> editWidget { it.copy(coverSize = value.toInt()) } }

            Spacer(Modifier.height(8.dp))
            Group("Offset / Gaps")
            WidgetSlider("Margin X", c.coverMarginX.toFloat(), -48f..48f, "dp") { v -> editWidget { it.copy(coverMarginX = v.roundToInt()) } }
            Spacer(Modifier.height(6.dp))
            WidgetSlider("Margin Y", c.coverMarginY.toFloat(), -48f..48f, "dp") { v -> editWidget { it.copy(coverMarginY = v.roundToInt()) } }
            Spacer(Modifier.height(6.dp))
            WidgetSlider("Padding X", c.coverPaddingX.toFloat(), -48f..48f, "dp") { v -> editWidget { it.copy(coverPaddingX = v.roundToInt()) } }
            Spacer(Modifier.height(6.dp))
            WidgetSlider("Padding Y", c.coverPaddingY.toFloat(), -48f..48f, "dp") { v -> editWidget { it.copy(coverPaddingY = v.roundToInt()) } }

            Spacer(Modifier.height(8.dp))
            Group("Shape")
            SettingsToggle("Match widget corners", null, c.coverRadius < 0) { value -> editWidget { it.copy(coverRadius = if (value) -1 else 12) } }
            if (c.coverRadius >= 0) {
                Spacer(Modifier.height(6.dp))
                WidgetSlider("Radius", c.coverRadius.toFloat(), 0f..64f, "dp") { v -> editWidget { it.copy(coverRadius = v.roundToInt()) } }
            }
            Spacer(Modifier.height(6.dp))
            WidgetSlider("Border", c.coverBorderWidth, 0f..4f, "dp") { v -> editWidget { it.copy(coverBorderWidth = (v * 4).roundToInt() / 4f) } }
            Spacer(Modifier.height(6.dp))
            WidgetColourEditor("Border colour", c.coverBorderColor) { value -> editWidget { it.copy(coverBorderColor = value) } }
        }

        SettingsAccordionItem(
            title = "Text & Labels",
            expanded = expandedGroup == "Labels",
            onToggle = { toggle("Labels") }
        ) {
            Group("Target text")
            SettingsChoiceRow(listOf("title" to "Song", "artist" to "Artist", "logo" to "Logo"), labelId) { labelId = it }
            val l = c.label(labelId)
            Spacer(Modifier.height(8.dp))
            SettingsChoiceRow(listOf("auto" to "Auto", "show" to "Show", "hide" to "Hide"), l.visibility) { value -> label { it.copy(visibility = value) } }

            Spacer(Modifier.height(8.dp))
            Group("Position")
            SettingsChoiceRow(listOf("top" to "Top", "center" to "Middle", "bottom" to "Bottom"), c.labelPlacement) { value -> editWidget { it.copy(labelPlacement = value) } }
            Spacer(Modifier.height(6.dp))
            SettingsChoiceRow(listOf("left" to "Left", "center" to "Centre", "right" to "Right"), l.alignment) { value -> label { it.copy(alignment = value) } }
            Spacer(Modifier.height(6.dp))
            WidgetSlider("Nudge X", l.offsetX.toFloat(), -48f..48f, "dp") { v -> label { it.copy(offsetX = v.roundToInt()) } }
            Spacer(Modifier.height(6.dp))
            WidgetSlider("Nudge Y", l.offsetY.toFloat(), -48f..48f, "dp") { v -> label { it.copy(offsetY = v.roundToInt()) } }

            Spacer(Modifier.height(8.dp))
            Group("Type")
            WidgetSlider("Size", l.size.toFloat(), 8f..28f, "sp") { v -> label { it.copy(size = v.roundToInt()) } }
            Spacer(Modifier.height(6.dp))
            SettingsChoiceRow(listOf("inter" to "Inter", "cinzel" to "Cinzel", "hand" to "Hand"), l.font) { value -> label { it.copy(font = value) } }
            Spacer(Modifier.height(6.dp))
            WidgetColourEditor("Colour", l.color) { value -> label { it.copy(color = value) } }

            Spacer(Modifier.height(8.dp))
            Group("Box")
            WidgetColourEditor("Background", l.background) { value -> label { it.copy(background = value) } }
            Spacer(Modifier.height(6.dp))
            WidgetSlider("Border", l.borderWidth, 0f..4f, "dp") { v -> label { it.copy(borderWidth = (v * 4).roundToInt() / 4f) } }
            Spacer(Modifier.height(6.dp))
            WidgetColourEditor("Border colour", l.borderColor) { value -> label { it.copy(borderColor = value) } }
            Spacer(Modifier.height(6.dp))
            WidgetSlider("Radius", l.radius.toFloat(), 0f..48f, "dp") { v -> label { it.copy(radius = v.roundToInt()) } }
        }

        SettingsAccordionItem(
            title = "Buttons & Controls",
            expanded = expandedGroup == "Buttons",
            onToggle = { toggle("Buttons") }
        ) {
            Group("Target button")
            SettingsChoiceRow(listOf("all" to "All", "prev" to "Prev", "play" to "Play", "next" to "Next", "like" to "Like"), buttonId) { buttonId = it }
            val b = c.button(if (buttonId == "all") "play" else buttonId)

            Spacer(Modifier.height(8.dp))
            Group("Position")
            SettingsChoiceRow(listOf("auto" to "Auto", "inline" to "Inline", "top" to "Top", "bottom" to "Bottom"), c.buttonsPlacement) { value -> editWidget { it.copy(buttonsPlacement = value) } }
            Spacer(Modifier.height(6.dp))
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
                WidgetSlider("Nudge X", b.offsetX.toFloat(), -48f..48f, "dp") { v -> button { it.copy(offsetX = v.roundToInt()) } }
                Spacer(Modifier.height(6.dp))
                WidgetSlider("Nudge Y", b.offsetY.toFloat(), -48f..48f, "dp") { v -> button { it.copy(offsetY = v.roundToInt()) } }
            }

            Spacer(Modifier.height(8.dp))
            Group("Shape")
            SettingsChoiceRow(listOf(0, 16, 20, 24, 28, 32, 36, 40, 44, 48).map { "$it" to if (it == 0) "Auto" else "$it" }, b.size.toString()) { value -> button { it.copy(size = value.toInt()) } }
            Spacer(Modifier.height(6.dp))
            WidgetSlider("Inner padding", b.padding.toFloat(), 0f..14f, "dp") { v -> button { it.copy(padding = v.roundToInt()) } }
            Spacer(Modifier.height(6.dp))
            WidgetSlider("Radius", b.radius.toFloat(), 0f..48f, "dp") { v -> button { it.copy(radius = v.roundToInt()) } }

            Spacer(Modifier.height(8.dp))
            Group("Colour")
            WidgetColourEditor("Icon", b.color) { value -> button { it.copy(color = value) } }
            Spacer(Modifier.height(6.dp))
            WidgetColourEditor("Background", b.background) { value -> button { it.copy(background = value) } }
            Spacer(Modifier.height(6.dp))
            WidgetSlider("Border", b.borderWidth, 0f..4f, "dp") { v -> button { it.copy(borderWidth = (v * 4).roundToInt() / 4f) } }
            Spacer(Modifier.height(6.dp))
            WidgetColourEditor("Border colour", b.borderColor) { value -> button { it.copy(borderColor = value) } }

            Spacer(Modifier.height(8.dp))
            Group("Caption")
            SettingsToggle("Show caption", null, b.showLabel) { value -> button { it.copy(showLabel = value) } }
            if (b.showLabel) {
                Spacer(Modifier.height(6.dp))
                WidgetSlider("Caption size", b.labelSize.toFloat(), 6f..14f, "sp") { v -> button { it.copy(labelSize = v.roundToInt()) } }
                Spacer(Modifier.height(6.dp))
                WidgetColourEditor("Caption colour", b.labelColor) { value -> button { it.copy(labelColor = value) } }
            }
        }

        Group("Install & Actions")
        Button(onClick = {
            val manager = AppWidgetManager.getInstance(context)
            if (manager.isRequestPinAppWidgetSupported) manager.requestPinAppWidget(ComponentName(context, XvoxAppWidgetProvider::class.java), null, null)
            else Toast.makeText(context, "Add XVOX from your launcher widget picker", Toast.LENGTH_LONG).show()
        }, modifier = Modifier.fillMaxWidth()) { Text("Add widget to home") }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { XvoxAppWidgetProvider.notifyWidgetUpdate(context) }) { Text("Refresh") }
            TextButton(onClick = { editWidget { WidgetCustomization() }; viewModel.setWidgetPaddingX(10); viewModel.setWidgetPaddingY(8); viewModel.setWidgetCornerRadius(16) }) { Text("Reset") }
        }
    })
}

@Composable
private fun Group(title: String) {
    Text(
        title.uppercase(), color = XvoxTheme.colors.secondaryText, fontSize = 9.sp,
        letterSpacing = 1.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
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
    OutlinedTextField(
        value = text,
        onValueChange = {
            if (it.length <= 9) {
                text = it
                if (it.matches(Regex("#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?"))) onChange(it)
            }
        },
        label = { Text("Custom #RRGGBB or #AARRGGBB", fontSize = 10.sp) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        isError = text.isNotEmpty() && !text.matches(Regex("#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?"))
    )
}
