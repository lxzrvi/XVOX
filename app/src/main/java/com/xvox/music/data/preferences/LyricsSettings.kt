package com.xvox.music.data.preferences

import org.json.JSONObject

data class LyricsSettings(
    val offsetMs: Int = 0,
    val currentSize: Int = 23,
    val otherSize: Int = 15,
    val fadeTop: Float = .22f,
    val fadeBottom: Float = .22f,
    val animation: String = "focus",
    val fadeEqual: Boolean = false,
    val fadeIntensity: Float = 1f,
    /** "left" | "center" | "right" — where lyric lines sit. */
    val alignment: String = "center"
) {
    fun sanitized() = copy(
        offsetMs = offsetMs.coerceIn(-1000, 1000),
        currentSize = currentSize.coerceIn(16, 42),
        otherSize = otherSize.coerceIn(10, 30),
        fadeTop = (fadeTop.takeIf { it.isFinite() } ?: .22f).coerceIn(0f, .45f),
        fadeBottom = (fadeBottom.takeIf { it.isFinite() } ?: .22f).coerceIn(0f, .45f),
        animation = animation.takeIf { it in LyricsSettings.ANIMATIONS } ?: "focus",
        fadeIntensity = (fadeIntensity.takeIf { it.isFinite() } ?: 1f).coerceIn(0f, 1f),
        alignment = alignment.takeIf { it in ALIGNMENTS } ?: "center"
    )
    fun position(playbackMs: Long): Long = (playbackMs - offsetMs).coerceAtLeast(0)
    fun seekPosition(lyricMs: Long): Long = (lyricMs + offsetMs).coerceAtLeast(0)
    fun encode(): String = JSONObject().put("offset", offsetMs).put("current", currentSize).put("other", otherSize)
        .put("top", fadeTop.toDouble()).put("bottom", fadeBottom.toDouble()).put("animation", animation)
        .put("equal", fadeEqual).put("intensity", fadeIntensity.toDouble())
        .put("align", alignment).toString()
    companion object {
        val ANIMATIONS = listOf("fade", "slide", "focus", "glide", "spring", "rise", "pulse", "wave")
        val ALIGNMENTS = listOf("left", "center", "right")
        fun decode(raw: String): LyricsSettings = runCatching {
            val j = JSONObject(raw)
            LyricsSettings(j.optInt("offset", 0), j.optInt("current", 23), j.optInt("other", 15),
                j.optDouble("top", .22).toFloat().takeIf { it.isFinite() } ?: .22f,
                j.optDouble("bottom", .22).toFloat().takeIf { it.isFinite() } ?: .22f,
                j.optString("animation", "focus"),
                j.optBoolean("equal", false),
                j.optDouble("intensity", 1.0).toFloat().takeIf { it.isFinite() } ?: 1f,
                j.optString("align", "center")).sanitized()
        }.getOrDefault(LyricsSettings())
    }
}
