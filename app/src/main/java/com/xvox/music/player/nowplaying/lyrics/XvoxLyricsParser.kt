package com.xvox.music.player.nowplaying.lyrics

object XvoxLyricsParser {

    private val timestamp =
        Regex(
            """\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]"""
        )

    private val metadataTag =
        Regex(
            """^\[(ar|ti|al|by|offset|re|ve):.*]$""",
            RegexOption.IGNORE_CASE
        )

    private val subtitleCue = Regex(
        """^\s*(?:(\d{1,2}):)?(\d{1,2}):(\d{2})(?:[.,](\d{1,3}))?\s*-->\s*.*$"""
    )

    private val assTimestamp = Regex(
        """^(\d+):(\d{2}):(\d{2})(?:\.(\d{1,3}))?$"""
    )

    fun parse(
        raw: String,
        source: XvoxLyricsSource
    ): XvoxLyrics {
        val normalized =
            normalize(raw)

        if (normalized.isEmpty()) {
            return XvoxLyrics(
                lines = emptyList(),
                synchronized = false,
                source = source
            )
        }

        val timed =
            mutableListOf<XvoxLyricLine>()

        normalized
            .lineSequence()
            .forEach { original ->
                val line =
                    original.trim()

                if (
                    line.isEmpty() ||
                    metadataTag.matches(line)
                ) {
                    return@forEach
                }

                val matches =
                    timestamp
                        .findAll(line)
                        .toList()

                if (matches.isEmpty()) {
                    return@forEach
                }

                val lyricText =
                    timestamp.replace(
                        line,
                        ""
                    ).trim()

                matches.forEach { match ->
                    val minutes =
                        match.groupValues[1]
                            .toLongOrNull()
                            ?: 0L

                    val seconds =
                        match.groupValues[2]
                            .toLongOrNull()
                            ?: 0L

                    val fraction =
                        match.groupValues[3]

                    val fractionMs =
                        when (fraction.length) {
                            1 ->
                                fraction
                                    .toLongOrNull()
                                    ?.times(100L)
                                    ?: 0L

                            2 ->
                                fraction
                                    .toLongOrNull()
                                    ?.times(10L)
                                    ?: 0L

                            3 ->
                                fraction
                                    .toLongOrNull()
                                    ?: 0L

                            else -> 0L
                        }

                    timed +=
                        XvoxLyricLine(
                            timeMs =
                                minutes * 60_000L +
                                    seconds * 1_000L +
                                    fractionMs,
                            text = lyricText
                        )
                }
            }

        if (timed.isNotEmpty()) {
            return XvoxLyrics(
                lines =
                    timed.sortedBy {
                        it.timeMs
                    },
                synchronized = true,
                source = source
            )
        }

        val plainLines =
            normalized
                .lineSequence()
                .map { it.trim() }
                .filter {
                    it.isNotEmpty() &&
                        !metadataTag.matches(it)
                }
                .map {
                    XvoxLyricLine(
                        timeMs = null,
                        text = it
                    )
                }
                .toList()

        return XvoxLyrics(
            lines = plainLines,
            synchronized = false,
            source = source
        )
    }

    private fun normalize(
        raw: String
    ): String {
        var text =
            raw
                .removePrefix("\uFEFF")
                .replace("\\r\\n", "\n")
                .replace("\\n", "\n")
                .replace("\\r", "\n")
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .trim()

        text = subtitleToLrc(text) ?: assToLrc(text) ?: text

        if (
            !text.contains('\n') &&
            text.count {
                it == '['
            } > 2
        ) {
            text =
                text.replace(
                    Regex(
                        """\s+/\s+(?=\[)"""
                    ),
                    "\n"
                )
        }

        return text
    }

    /** Converts readable SRT/VTT cue blocks into the LRC timestamps used by the player. */
    private fun subtitleToLrc(text: String): String? {
        if (!text.contains("-->")) return null

        val converted = mutableListOf<String>()
        var currentTimestamp: String? = null
        val cueText = mutableListOf<String>()

        fun flushCue() {
            val timestamp = currentTimestamp ?: return
            val lyric = cueText
                .joinToString(" ")
                .replace(Regex("<[^>]+>"), "")
                .trim()
            if (lyric.isNotBlank()) converted += "$timestamp$lyric"
            currentTimestamp = null
            cueText.clear()
        }

        text.lineSequence().forEach { sourceLine ->
            val line = sourceLine.trim()
            val cue = subtitleCue.matchEntire(line)
            when {
                cue != null -> {
                    flushCue()
                    currentTimestamp = lrcTimestamp(
                        hours = cue.groupValues[1],
                        minutes = cue.groupValues[2],
                        seconds = cue.groupValues[3],
                        fraction = cue.groupValues[4]
                    )
                }

                line.isBlank() -> flushCue()
                currentTimestamp != null &&
                    !line.equals("WEBVTT", ignoreCase = true) &&
                    !line.startsWith("NOTE", ignoreCase = true) &&
                    !line.matches(Regex("""\d+""")) -> cueText += line
            }
        }
        flushCue()
        return converted.takeIf { it.isNotEmpty() }?.joinToString("\n")
    }

    /** Converts common ASS/SSA Dialogue rows into portable LRC lines. */
    private fun assToLrc(text: String): String? {
        val converted = text.lineSequence().mapNotNull { sourceLine ->
            val line = sourceLine.trim()
            if (!line.startsWith("Dialogue:", ignoreCase = true)) return@mapNotNull null

            val fields = line.substringAfter(':', "").split(',', limit = 10)
            if (fields.size < 10) return@mapNotNull null
            val timestamp = assTimestamp.matchEntire(fields[1].trim()) ?: return@mapNotNull null
            val lyric = fields[9]
                .replace("\\N", " ")
                .replace("\\n", " ")
                .replace(Regex("""\{[^}]*}"""), "")
                .trim()
            if (lyric.isBlank()) return@mapNotNull null

            lrcTimestamp(
                hours = timestamp.groupValues[1],
                minutes = timestamp.groupValues[2],
                seconds = timestamp.groupValues[3],
                fraction = timestamp.groupValues[4]
            ) + lyric
        }.toList()

        return converted.takeIf { it.isNotEmpty() }?.joinToString("\n")
    }

    private fun lrcTimestamp(
        hours: String,
        minutes: String,
        seconds: String,
        fraction: String
    ): String {
        val totalMinutes = (hours.toLongOrNull() ?: 0L) * 60L + (minutes.toLongOrNull() ?: 0L)
        val normalizedSeconds = (seconds.toLongOrNull() ?: 0L).coerceIn(0L, 59L)
        val normalizedFraction = fraction.filter(Char::isDigit).take(3).ifBlank { "0" }
        return "[$totalMinutes:${normalizedSeconds.toString().padStart(2, '0')}.$normalizedFraction]"
    }
}
