package com.xvox.music.data.preferences

import org.json.JSONObject

data class LyricsSettings(
    val offsetMs: Int = 0,
    val topSize: Int = 14,
    val currentSize: Int = 23,
    val bottomSize: Int = 14,
    val otherSize: Int = 14,
    val fadeTop: Float = .22f,
    val fadeBottom: Float = .22f,
    val animation: String = "rise",
    /** Retained for older equal-fade records; the new UI exposes [fadeEnabled] directly. */
    val fadeEqual: Boolean = false,
    /** A fixed, readable fade strength retained for compatibility rather than exposed as a rail. */
    val fadeIntensity: Float = 1f,
    /** Toggle for dimming non-active lyric lines. */
    val fadeEnabled: Boolean = false,
    /** Toggle for active-line emphasis (weight/scale/context contrast). */
    val focusActiveLine: Boolean = true,
    /** "left" | "center" | "right" — where lyric lines sit. */
    val alignment: String = "center",
    /** Vertical space between lines (dp). */
    val lineGap: Int = 14,
    /** "orb" | "aurora" | "off" — moving canvas backdrop in lyrics card. */
    val gradientAnimation: String = "orb",
    /**
     * Lyric-only text colour: "cover", "black", or "white". This supersedes the older
     * matchColor toggle while keeping its wire value readable for existing installations.
     */
    val textColorMode: String = "cover",
    /** Legacy compatibility mirror for records written before [textColorMode] existed. */
    val matchCoverColor: Boolean = true,
    /** When true, auto-scroll and line-by-line synced playback is active. */
    val timeSync: Boolean = true,
    /** Font weight scale (400 to 800). */
    val fontWeight: Int = 600,
    /** Whether top, active and bottom lyric lines retain their own independent size choices. */
    val individualLineSizes: Boolean = false,
    /** Retained only to read old records; fullscreen lyrics is permanently optimized style 20. */
    val fullscreenAnimationStyle: Int = 20
) {
    fun normalized(): LyricsSettings {
        // A legacy matchColor=false record had opted out of cover matching. White is the stable
        // theme-neutral migration target; newer records always carry one of these three keys.
        val resolvedTextColorMode = textColorMode.takeIf { it in TEXT_COLOR_MODES }
            ?: if (matchCoverColor) "cover" else "white"
        return copy(
            offsetMs = offsetMs.coerceIn(-1000, 1000),
            // 50 sp is intentionally supported in every line role; it is an exposed editor preset.
            topSize = topSize.coerceIn(10, 50),
            currentSize = currentSize.coerceIn(14, 50),
            bottomSize = bottomSize.coerceIn(10, 50),
            otherSize = otherSize.coerceIn(10, 50),
            fadeTop = (fadeTop.takeIf { it.isFinite() } ?: .22f).coerceIn(0f, .45f),
            fadeBottom = (fadeBottom.takeIf { it.isFinite() } ?: .22f).coerceIn(0f, .45f),
            animation = animation.takeIf { it in ANIMATIONS } ?: "rise",
            fadeIntensity = (fadeIntensity.takeIf { it.isFinite() } ?: 1f).coerceIn(0f, 1f),
            fadeEnabled = fadeEnabled,
            focusActiveLine = focusActiveLine,
            alignment = alignment.takeIf { it in ALIGNMENTS } ?: "center",
            lineGap = lineGap.coerceIn(4, 40),
            gradientAnimation = gradientAnimation.takeIf { it in GRADIENT_ANIMATIONS } ?: "orb",
            textColorMode = resolvedTextColorMode,
            // Keep old readers useful if a downgraded build encounters a newly saved record.
            matchCoverColor = resolvedTextColorMode == "cover",
            timeSync = timeSync,
            fontWeight = fontWeight.coerceIn(300, 900),
            // Older selectable presets intentionally migrate to the locked product motion.
            fullscreenAnimationStyle = 20
        )
    }

    fun sanitized(): LyricsSettings = normalized()

    fun position(pos: Long): Long = pos + offsetMs
    fun seekPosition(pos: Long): Long = (pos - offsetMs).coerceAtLeast(0L)

    fun encode(): String = JSONObject()
        .put("offset", offsetMs)
        .put("topSize", topSize)
        .put("currentSize", currentSize)
        .put("bottomSize", bottomSize)
        .put("other", otherSize)
        .put("top", fadeTop.toDouble())
        .put("bottom", fadeBottom.toDouble())
        .put("animation", animation)
        .put("equal", fadeEqual)
        .put("intensity", fadeIntensity.toDouble())
        .put("fadeEnabled", fadeEnabled)
        .put("focusActive", focusActiveLine)
        .put("align", alignment)
        .put("gap", lineGap)
        .put("gradient", gradientAnimation)
        .put("textColor", textColorMode)
        // Retain this legacy mirror for a safe downgrade path.
        .put("matchColor", textColorMode == "cover")
        .put("timeSync", timeSync)
        .put("fontWeight", fontWeight)
        .put("individualSizes", individualLineSizes)
        .put("fullscreenStyle", 20)
        .toString()

    companion object {
        // The product now exposes exactly these three Line Transition buttons. Older labels are
        // migrated during decode rather than remaining as invisible fourth/fifth modes.
        val ANIMATIONS = listOf("rise", "glide", "pop")
        val GRADIENT_ANIMATIONS = listOf("orb", "aurora", "off", "wave")
        val ALIGNMENTS = listOf("left", "center", "right")
        val TEXT_COLOR_MODES = listOf("cover", "black", "white")

        fun decode(raw: String): LyricsSettings = runCatching {
            val j = JSONObject(raw)
            val other = j.optInt("other", 14)
            val animRaw = j.optString("animation", "rise")
            val mappedAnim = when (animRaw) {
                "wave", "off", "classic" -> "rise"
                "drift" -> "glide"
                "aurora" -> "pop"
                else -> animRaw
            }
            val gradRaw = j.optString("gradient", "orb")
            val mappedGrad = when (gradRaw) {
                "wave" -> "orb"
                else -> gradRaw
            }
            val savedTextColor = j.optString("textColor", "").takeIf { it in TEXT_COLOR_MODES }
                ?: if (j.optBoolean("matchColor", true)) "cover" else "white"
            LyricsSettings(
                offsetMs = j.optInt("offset", 0),
                topSize = j.optInt("topSize", other),
                currentSize = j.optInt("currentSize", 23),
                bottomSize = j.optInt("bottomSize", other),
                otherSize = other,
                fadeTop = j.optDouble("top", .22).toFloat().takeIf { it.isFinite() } ?: .22f,
                fadeBottom = j.optDouble("bottom", .22).toFloat().takeIf { it.isFinite() } ?: .22f,
                animation = mappedAnim,
                fadeEqual = j.optBoolean("equal", false),
                fadeIntensity = j.optDouble("intensity", 1.0).toFloat().takeIf { it.isFinite() } ?: 1f,
                // Existing records did not have explicit controls; preserve their familiar
                // readable treatment until the user changes either new toggle.
                fadeEnabled = j.optBoolean("fadeEnabled", j.optBoolean("equal", false)),
                focusActiveLine = j.optBoolean("focusActive", true),
                alignment = j.optString("align", "center"),
                lineGap = j.optInt("gap", 14),
                gradientAnimation = mappedGrad,
                textColorMode = savedTextColor,
                matchCoverColor = savedTextColor == "cover",
                timeSync = j.optBoolean("timeSync", true),
                fontWeight = j.optInt("fontWeight", 600),
                // Older installations had no explicit flag; keep their previous master-size behavior.
                individualLineSizes = j.optBoolean("individualSizes", false),
                fullscreenAnimationStyle = 20
            ).normalized()
        }.getOrElse { LyricsSettings() }
    }
}
