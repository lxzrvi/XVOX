package com.xvox.music.features.settings.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.chrome.parseHexColor
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.components.*
import com.xvox.music.widget.WidgetCustomization
import kotlin.math.roundToInt

@Composable
fun WidgetSettingsSection(state: SettingsState, viewModel: SettingsViewModel) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current
    var editingCategory by remember { mutableStateOf("Horizontal") }
    var editingSize by remember { mutableStateOf(state.widgetPreviewSize) }
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
            // Category Tabs: Row, Horizontal, Box
            SettingsChoiceRow(
                listOf("Horizontal" to "Horizontal", "Row" to "Row", "Box" to "Box"),
                editingCategory
            ) { category ->
                editingCategory = category
                val defaultForCat = when (category) {
                    "Row" -> "1x3"
                    "Box" -> "2x2"
                    else -> "3x1"
                }
                editingSize = defaultForCat
                viewModel.setWidgetPreviewSize(defaultForCat)
            }

            Spacer(Modifier.height(10.dp))

            val sizeList = when (editingCategory) {
                "Row" -> listOf("1x2" to "1×2", "1x3" to "1×3", "1x4" to "1×4", "2x3" to "2×3", "2x4" to "2×4")
                "Box" -> listOf("1x1" to "1×1", "2x2" to "2×2", "3x3" to "3×3", "4x4" to "4×4")
                else -> listOf("1x1" to "1×1", "2x1" to "2×1", "3x1" to "3×1", "4x1" to "4×1", "5x1" to "5×1")
            }

            SettingsChoiceRow(sizeList, editingSize) { chosen ->
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
            Group("Surface offsets & Margins")
            WidgetSlider("Margin X", c.coverMarginX.toFloat(), -96f..96f, "dp") { v -> editWidget { it.copy(coverMarginX = v.roundToInt()) } }
            Spacer(Modifier.height(6.dp))
            WidgetSlider("Margin Y", c.coverMarginY.toFloat(), -96f..96f, "dp") { v -> editWidget { it.copy(coverMarginY = v.roundToInt()) } }
            Spacer(Modifier.height(6.dp))
            WidgetSlider("Padding X", c.coverPaddingX.toFloat(), -96f..96f, "dp") { v -> editWidget { it.copy(coverPaddingX = v.roundToInt()) } }
            Spacer(Modifier.height(6.dp))
            WidgetSlider("Padding Y", c.coverPaddingY.toFloat(), -96f..96f, "dp") { v -> editWidget { it.copy(coverPaddingY = v.roundToInt()) } }

            Spacer(Modifier.height(8.dp))
            Group("Shape")
            SettingsToggle("Match widget corners", null, c.coverRadius < 0) { value -> editWidget { it.copy(coverRadius = if (value) -1 else 12) } }
            if (c.coverRadius >= 0) {
                Spacer(Modifier.height(6.dp))
                WidgetSlider("Radius", c.coverRadius.toFloat(), 0f..64f, "dp") { v -> editWidget { it.copy(coverRadius = v.roundToInt()) } }
            }
        }

        SettingsAccordionItem(
            title = "Text labels",
            expanded = expandedGroup == "Labels",
            onToggle = { toggle("Labels") }
        ) {
            SettingsChoiceRow(listOf("title" to "Title", "artist" to "Artist", "logo" to "Logo"), labelId) { labelId = it }
            val l = c.label(labelId)

            Spacer(Modifier.height(8.dp))
            Group("Visibility & Size")
            SettingsChoiceRow(listOf("auto" to "Auto", "show" to "Always show", "hide" to "Hide"), l.visibility) { v -> label { it.copy(visibility = v) } }
            Spacer(Modifier.height(6.dp))
            WidgetSlider("Size", l.size.toFloat(), 8f..28f, "sp") { v -> label { it.copy(size = v.roundToInt()) } }

            Spacer(Modifier.height(8.dp))
            Group("Placement & Surface Offset")
            SettingsChoiceRow(listOf("left" to "Left", "center" to "Center", "right" to "Right"), l.alignment) { v -> label { it.copy(alignment = v) } }
            Spacer(Modifier.height(6.dp))
            WidgetSlider("Offset X", l.offsetX.toFloat(), -96f..96f, "dp") { v -> label { it.copy(offsetX = v.roundToInt()) } }
            Spacer(Modifier.height(6.dp))
            WidgetSlider("Offset Y", l.offsetY.toFloat(), -96f..96f, "dp") { v -> label { it.copy(offsetY = v.roundToInt()) } }
        }

        SettingsAccordionItem(
            title = "Control buttons",
            expanded = expandedGroup == "Buttons",
            onToggle = { toggle("Buttons") }
        ) {
            SettingsChoiceRow(listOf("all" to "All", "prev" to "Prev", "play" to "Play", "next" to "Next", "like" to "Like"), buttonId) { buttonId = it }
            val b = c.button(if (buttonId == "all") "play" else buttonId)

            Spacer(Modifier.height(8.dp))
            Group("Placement & Size")
            SettingsChoiceRow(listOf("auto" to "Auto", "left" to "Left", "center" to "Center", "right" to "Right", "hidden" to "Hide"), b.position) { v -> button { it.copy(position = v) } }
            Spacer(Modifier.height(6.dp))
            WidgetSlider("Size", (if (b.size == 0) 32 else b.size).toFloat(), 20f..48f, "dp") { v -> button { it.copy(size = v.roundToInt()) } }

            Spacer(Modifier.height(8.dp))
            Group("Surface Offset")
            WidgetSlider("Offset X", b.offsetX.toFloat(), -96f..96f, "dp") { v -> button { it.copy(offsetX = v.roundToInt()) } }
            Spacer(Modifier.height(6.dp))
            WidgetSlider("Offset Y", b.offsetY.toFloat(), -96f..96f, "dp") { v -> button { it.copy(offsetY = v.roundToInt()) } }
        }

        SettingsChoiceRow(listOf("reset_all" to "Reset size", "reset_base" to "Reset defaults"), "") { key ->
            if (key == "reset_all") {
                viewModel.setWidgetCustomizationForSize(sizeKey, WidgetCustomization())
            } else {
                viewModel.setWidgetCustomization(WidgetCustomization())
            }
        }
    })
}

@Composable
private fun Group(text: String) {
    Text(
        text,
        color = XvoxTheme.colors.primaryAccent,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun WidgetSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, unit: String, onChange: (Float) -> Unit) {
    val colors = XvoxTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.width(110.dp))
        XvoxThinLineSlider(value = value.coerceIn(range.start, range.endInclusive), onValueChange = onChange, valueRange = range, modifier = Modifier.weight(1f))
        Text("${value.roundToInt()} $unit", color = colors.primaryText, fontSize = 11.sp, modifier = Modifier.width(48.dp))
    }
}

@Composable
private fun WidgetColourEditor(label: String, current: String, onSelect: (String) -> Unit) {
    val colors = XvoxTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.width(110.dp))
        Row(
            Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Auto", "Accent", "#FFFFFF", "#000000", "#1E1E28", "#FF453A", "#30D158", "#0A84FF", "#BF5AF2", "#FF9F0A").forEach { code ->
                val active = current == code
                val c = when (code) {
                    "Auto" -> colors.cardElevated
                    "Accent" -> colors.primaryAccent
                    else -> parseHexColor(code) ?: Color.Transparent
                }
                Box(
                    Modifier.size(24.dp).clip(CircleShape).background(c)
                        .border(if (active) 2.dp else .7.dp, if (active) colors.primaryText else colors.cardBorder, CircleShape)
                        .clickable { onSelect(code) }
                )
            }
        }
    }
}
