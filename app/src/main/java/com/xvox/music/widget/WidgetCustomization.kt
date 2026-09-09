package com.xvox.music.widget

import org.json.JSONObject

data class WidgetLabelStyle(
    val visibility: String = "auto", val size: Int = 13, val alignment: String = "left", val font: String = "inter",
    val color: String = "Auto", val background: String = "Transparent", val borderColor: String = "Auto",
    val borderWidth: Float = 0f, val radius: Int = 6,
    /** Nudge in dp. Negative values are allowed, so cover text can sit outside the box. */
    val offsetX: Int = 0, val offsetY: Int = 0
)
data class WidgetButtonStyle(
    val position: String = "auto", val size: Int = 0, val color: String = "Auto",
    val background: String = "Auto", val borderColor: String = "Auto", val borderWidth: Float = 0f, val radius: Int = 12,
    val showLabel: Boolean = false, val labelSize: Int = 8, val labelColor: String = "Auto", val padding: Int = 6,
    /** Free nudge in dp. Negative values are allowed, so a button may float anywhere. */
    val offsetX: Int = 0, val offsetY: Int = 0
)
data class WidgetCustomization(
    val marginX: Int = 0, val marginY: Int = 0, val alignment: String = "center",
    val coverPlacement: String = "auto", val coverSize: Int = 0, val coverRadius: Int = -1,
    val coverBorderWidth: Float = 0f, val coverBorderColor: String = "Auto",
    val labelPlacement: String = "center", val buttonsPlacement: String = "auto",
    val fullCover: Boolean = false, val fullCoverShade: Float = .35f,
    val borderWidth: Float = .7f, val borderColor: String = "Auto",
    val buttonOrder: List<String> = buttonIds,
    val labels: Map<String, WidgetLabelStyle> = defaultLabels(),
    val buttons: Map<String, WidgetButtonStyle> = defaultButtons(),
    val verticalAlignment: String = "center",
    val coverMarginX: Int = 0, val coverMarginY: Int = 0, val coverPaddingX: Int = 0, val coverPaddingY: Int = 0
) {
    fun label(id: String) = labels[id] ?: defaultLabels().getValue(id)
    fun button(id: String) = buttons[id] ?: WidgetButtonStyle()
    // Cover margin/padding accept negative values on purpose: the artwork and its text are
    // allowed to bleed outside the widget box.
    fun sanitized() = copy(coverMarginX = coverMarginX.coerceIn(-32, 24), coverMarginY = coverMarginY.coerceIn(-32, 24),
        coverPaddingX = coverPaddingX.coerceIn(-32, 24), coverPaddingY = coverPaddingY.coerceIn(-32, 24),marginX = marginX.coerceIn(0, 32), marginY = marginY.coerceIn(0, 32),
        verticalAlignment = verticalAlignment.takeIf { it in setOf("top", "center", "bottom") } ?: "center",
        alignment = alignment.takeIf { it in setOf("left", "center", "right") } ?: "center",
        coverPlacement = coverPlacement.takeIf { it in setOf("auto", "left", "right", "top", "bottom", "hidden") } ?: "auto",
        coverSize = coverSize.coerceIn(0, 160), coverRadius = coverRadius.coerceIn(-1, 64),
        coverBorderWidth = (coverBorderWidth.takeIf { it.isFinite() } ?: 0f).coerceIn(0f, 4f), borderWidth = (borderWidth.takeIf { it.isFinite() } ?: 0f).coerceIn(0f, 4f),
        fullCoverShade = (fullCoverShade.takeIf { it.isFinite() } ?: .35f).coerceIn(0f, .85f),
        labelPlacement = labelPlacement.takeIf { it in setOf("top", "center", "bottom") } ?: "center",
        buttonsPlacement = buttonsPlacement.takeIf { it in setOf("auto", "inline", "top", "bottom") } ?: "auto",
        buttonOrder = (buttonOrder.filter { it in buttonIds } + buttonIds).distinct(),
        labels = defaultLabels().mapValues { (id, fallback) -> label(id).let { it.copy(
            visibility = it.visibility.takeIf { v -> v in setOf("auto", "show", "hide") } ?: "auto",
            size = it.size.coerceIn(8, 28), radius = it.radius.coerceIn(0, 48), borderWidth = (it.borderWidth.takeIf { value -> value.isFinite() } ?: 0f).coerceIn(0f, 4f),
            alignment = it.alignment.takeIf { v -> v in setOf("left", "center", "right") } ?: "left",
            offsetX = it.offsetX.coerceIn(-48, 48), offsetY = it.offsetY.coerceIn(-48, 48),
            font = it.font.takeIf { v -> v in setOf("inter", "cinzel", "hand") } ?: fallback.font) } },
        buttons = defaultButtons().mapValues { (id, _) -> button(id).let { it.copy(
            position = it.position.takeIf { v -> v in setOf("auto", "left", "center", "right", "hidden") } ?: "auto",
            size = it.size.coerceIn(0, 48), radius = it.radius.coerceIn(0, 48), borderWidth = (it.borderWidth.takeIf { value -> value.isFinite() } ?: 0f).coerceIn(0f, 4f),
            labelSize = it.labelSize.coerceIn(6, 14), padding = it.padding.coerceIn(0, 14),
            offsetX = it.offsetX.coerceIn(-48, 48), offsetY = it.offsetY.coerceIn(-48, 48)) } })
    fun encode(): String {
        val j = JSONObject().put("mx", marginX).put("my", marginY).put("align", alignment)
            .put("cover", coverPlacement).put("coverSize", coverSize).put("coverRadius", coverRadius)
            .put("coverBorder", coverBorderWidth.toDouble()).put("coverBorderColor", coverBorderColor)
            .put("labelsAt", labelPlacement).put("buttonsAt", buttonsPlacement).put("full", fullCover)
            .put("shade", fullCoverShade.toDouble()).put("border", borderWidth.toDouble()).put("borderColor", borderColor)
        j.put("order", buttonOrder.joinToString(",")).put("vertical", verticalAlignment).put("cmx", coverMarginX).put("cmy", coverMarginY).put("cpx", coverPaddingX).put("cpy", coverPaddingY)
        val ls = JSONObject(); labels.forEach { (id, s) -> ls.put(id, JSONObject().put("visible", s.visibility).put("size", s.size)
            .put("align", s.alignment).put("font", s.font).put("color", s.color).put("bg", s.background)
            .put("borderColor", s.borderColor).put("border", s.borderWidth.toDouble()).put("radius", s.radius)
            .put("ox", s.offsetX).put("oy", s.offsetY)) }
        val bs = JSONObject(); buttons.forEach { (id, s) -> bs.put(id, JSONObject().put("position", s.position).put("size", s.size)
            .put("color", s.color).put("bg", s.background).put("borderColor", s.borderColor)
            .put("border", s.borderWidth.toDouble()).put("radius", s.radius).put("label", s.showLabel)
            .put("labelSize", s.labelSize).put("labelColor", s.labelColor).put("padding", s.padding)
            .put("ox", s.offsetX).put("oy", s.offsetY)) }
        return j.put("labels", ls).put("buttons", bs).toString()
    }
    companion object {
        val buttonIds = listOf("prev", "play", "next", "like")
        val labelIds = listOf("title", "artist", "logo")
        fun defaultLabels() = mapOf("title" to WidgetLabelStyle(), "artist" to WidgetLabelStyle(size = 10), "logo" to WidgetLabelStyle(size = 12, font = "cinzel"))
        fun defaultButtons() = buttonIds.associateWith { WidgetButtonStyle() }
        fun decode(raw: String): WidgetCustomization = runCatching {
            val j = JSONObject(raw)
            val labels = defaultLabels().mapValues { (id, d) ->
                val s = j.optJSONObject("labels")?.optJSONObject(id) ?: JSONObject()
                WidgetLabelStyle(s.optString("visible", d.visibility), s.optInt("size", d.size), s.optString("align", d.alignment),
                    s.optString("font", d.font), s.optString("color", d.color), s.optString("bg", d.background),
                    s.optString("borderColor", d.borderColor), s.optDouble("border", d.borderWidth.toDouble()).toFloat(), s.optInt("radius", d.radius),
                    s.optInt("ox", 0), s.optInt("oy", 0))
            }
            val buttons = defaultButtons().mapValues { (id, d) ->
                val s = j.optJSONObject("buttons")?.optJSONObject(id) ?: JSONObject()
                WidgetButtonStyle(s.optString("position", d.position), s.optInt("size", d.size), s.optString("color", d.color),
                    s.optString("bg", d.background), s.optString("borderColor", d.borderColor), s.optDouble("border", 0.0).toFloat(), s.optInt("radius", d.radius), s.optBoolean("label", false),
                    s.optInt("labelSize", 8), s.optString("labelColor", "Auto"), s.optInt("padding", 6),
                    s.optInt("ox", 0), s.optInt("oy", 0))
            }
            WidgetCustomization(j.optInt("mx"), j.optInt("my"), j.optString("align", "center"), j.optString("cover", "auto"),
                j.optInt("coverSize"), j.optInt("coverRadius", -1), j.optDouble("coverBorder", 0.0).toFloat(), j.optString("coverBorderColor", "Auto"),
                j.optString("labelsAt", "center"), j.optString("buttonsAt", "auto"), j.optBoolean("full"), j.optDouble("shade", .35).toFloat(),
                j.optDouble("border", .7).toFloat(), j.optString("borderColor", "Auto"), j.optString("order", buttonIds.joinToString(",")).split(","), labels, buttons, j.optString("vertical", "center"), j.optInt("cmx"), j.optInt("cmy"), j.optInt("cpx"), j.optInt("cpy")).sanitized()
        }.getOrDefault(WidgetCustomization())
    }
}
