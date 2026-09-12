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
    val animation: String = "wave",
    val fadeEqual: Boolean = false,
    val fadeIntensity: Float = 1f,
    /** "left" | "center" | "right" — where lyric lines sit. */
    val alignment: String = "center",
    val lineGap: Int = 14,
    val matchCoverColor: Boolean = true,
    val gradientAnimation: String = "wave"
) {
    fun sanitized() = copy(
        offsetMs = offsetMs.coerceIn(-1000, 1000),
        topSize = topSize.coerceIn(10, 36),
        currentSize = currentSize.coerceIn(14, 44),
        bottomSize = bottomSize.coerceIn(10, 36),
        otherSize = otherSize.coerceIn(10, 36),
        fadeTop = (fadeTop.takeIf { it.isFinite() } ?: .22f).coerceIn(0f, .45f),
        fadeBottom = (fadeBottom.takeIf { it.isFinite() } ?: .22f).coerceIn(0f, .45f),
        animation = animation.takeIf { it in ANIMATIONS } ?: "wave",
        fadeIntensity = (fadeIntensity.takeIf { it.isFinite() } ?: 1f).coerceIn(0f, 1f),
        alignment = alignment.takeIf { it in ALIGNMENTS } ?: "center",
        lineGap = lineGap.coerceIn(4, 40),
        gradientAnimation = gradientAnimation.takeIf { it in GRADIENT_ANIMATIONS } ?: "wave"
    )

    fun position(playbackMs: Long): Long = (playbackMs - offsetMs).coerceAtLeast(0)
    fun seekPosition(lyricMs: Long): Long = (lyricMs + offsetMs).coerceAtLeast(0)

    fun encode(): String = JSONObject()
        .put("offset", offsetMs)
        .put("topSize", topSize)
        .put("current", currentSize)
        .put("bottomSize", bottomSize)
        .put("other", otherSize)
        .put("top", fadeTop.toDouble())
        .put("bottom", fadeBottom.toDouble())
        .put("animation", animation)
        .put("equal", fadeEqual)
        .put("intensity", fadeIntensity.toDouble())
        .put("align", alignment)
        .put("lineGap", lineGap)
        .put("matchCover", matchCoverColor)
        .put("gradientAnimation", gradientAnimation)
        .toString()

    companion object {
        val ANIMATIONS = listOf("wave", "string", "spring", "slide", "rise", "fade")
        val GRADIENT_ANIMATIONS = listOf("off", "wave", "aurora", "pulse", "orbital", "prism")
        val ALIGNMENTS = listOf("left", "center", "right")

        fun decode(raw: String): LyricsSettings = runCatching {
            val j = JSONObject(raw)
            val other = j.optInt("other", 14)
            LyricsSettings(
                offsetMs = j.optInt("offset", 0),
                topSize = j.optInt("topSize", other),
                currentSize = j.optInt("current", 23),
                bottomSize = j.optInt("bottomSize", other),
                otherSize = other,
                fadeTop = j.optDouble("top", .22).toFloat().takeIf { it.isFinite() } ?: .22f,
                fadeBottom = j.optDouble("bottom", .22).toFloat().takeIf { it.isFinite() } ?: .22f,
                animation = j.optString("animation", "wave"),
                fadeEqual = j.optBoolean("equal", false),
                fadeIntensity = j.optDouble("intensity", 1.0).toFloat().takeIf { it.isFinite() } ?: 1f,
                alignment = j.optString("align", "center"),
                lineGap = j.optInt("lineGap", 14),
                matchCoverColor = j.optBoolean("matchCover", true),
                gradientAnimation = j.optString("gradientAnimation", "wave")
            ).sanitized()
        }.getOrDefault(LyricsSettings())
    }
}
