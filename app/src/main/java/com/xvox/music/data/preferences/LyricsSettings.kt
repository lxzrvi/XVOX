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
    val fadeEqual: Boolean = false,
    val fadeIntensity: Float = 1f,
    /** "left" | "center" | "right" — where lyric lines sit. */
    val alignment: String = "center",
    /** Vertical space between lines (dp). */
    val lineGap: Int = 14,
    /** "orb" | "aurora" | "off" — moving canvas backdrop in lyrics card. */
    val gradientAnimation: String = "orb",
    /** When true, current line takes vivid cover palette color. */
    val matchCoverColor: Boolean = true,
    /** When true, auto-scroll and line-by-line synced playback is active. */
    val timeSync: Boolean = true,
    /** Font weight scale (400 to 800). */
    val fontWeight: Int = 600
) {
    fun normalized(): LyricsSettings = copy(
        offsetMs = offsetMs.coerceIn(-1000, 1000),
        topSize = topSize.coerceIn(10, 36),
        currentSize = currentSize.coerceIn(14, 44),
        bottomSize = bottomSize.coerceIn(10, 36),
        otherSize = otherSize.coerceIn(10, 36),
        fadeTop = (fadeTop.takeIf { it.isFinite() } ?: .22f).coerceIn(0f, .45f),
        fadeBottom = (fadeBottom.takeIf { it.isFinite() } ?: .22f).coerceIn(0f, .45f),
        animation = animation.takeIf { it in ANIMATIONS } ?: "rise",
        fadeIntensity = (fadeIntensity.takeIf { it.isFinite() } ?: 1f).coerceIn(0f, 1f),
        alignment = alignment.takeIf { it in ALIGNMENTS } ?: "center",
        lineGap = lineGap.coerceIn(4, 40),
        gradientAnimation = gradientAnimation.takeIf { it in GRADIENT_ANIMATIONS } ?: "orb",
        matchCoverColor = matchCoverColor,
        timeSync = timeSync,
        fontWeight = fontWeight.coerceIn(300, 900)
    )

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
        .put("align", alignment)
        .put("gap", lineGap)
        .put("gradient", gradientAnimation)
        .put("matchColor", matchCoverColor)
        .put("timeSync", timeSync)
        .put("fontWeight", fontWeight)
        .toString()

    companion object {
        val ANIMATIONS = listOf("rise", "glide", "pop", "off", "wave", "drift", "aurora", "classic")
        val GRADIENT_ANIMATIONS = listOf("orb", "aurora", "off", "wave")
        val ALIGNMENTS = listOf("left", "center", "right")

        fun decode(raw: String): LyricsSettings = runCatching {
            val j = JSONObject(raw)
            val other = j.optInt("other", 14)
            val animRaw = j.optString("animation", "rise")
            val mappedAnim = when (animRaw) {
                "wave" -> "rise"
                "drift" -> "glide"
                "aurora" -> "pop"
                "classic" -> "off"
                else -> animRaw
            }
            val gradRaw = j.optString("gradient", "orb")
            val mappedGrad = when (gradRaw) {
                "wave" -> "orb"
                else -> gradRaw
            }
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
                alignment = j.optString("align", "center"),
                lineGap = j.optInt("gap", 14),
                gradientAnimation = mappedGrad,
                matchCoverColor = j.optBoolean("matchColor", true),
                timeSync = j.optBoolean("timeSync", true),
                fontWeight = j.optInt("fontWeight", 600)
            ).normalized()
        }.getOrElse { LyricsSettings() }
    }
}
