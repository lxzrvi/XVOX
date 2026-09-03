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

    fun parse(
        raw: String,
        source:
            XvoxLyricsSource
    ): XvoxLyrics {
        val normalized =
            raw
                .replace(
                    "\r\n",
                    "\n"
                )
                .replace(
                    '\r',
                    '\n'
                )
                .trim()

        if (
            normalized.isEmpty()
        ) {
            return XvoxLyrics(
                lines =
                    emptyList(),
                synchronized =
                    false,
                source =
                    source
            )
        }

        val timed =
            mutableListOf<XvoxLyricLine>()

        normalized
            .lineSequence()
            .forEach {
                original ->

                val line =
                    original.trim()

                if (
                    line.isEmpty() ||
                    metadataTag
                        .matches(line)
                ) {
                    return@forEach
                }

                val matches =
                    timestamp
                        .findAll(line)
                        .toList()

                if (
                    matches.isEmpty()
                ) {
                    return@forEach
                }

                val lyricText =
                    timestamp
                        .replace(
                            line,
                            ""
                        )
                        .trim()

                matches.forEach {
                    match ->

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
                        when (
                            fraction.length
                        ) {
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

                            else ->
                                0L
                        }

                    timed +=
                        XvoxLyricLine(
                            timeMs =
                                minutes *
                                    60_000L +
                                    seconds *
                                        1_000L +
                                    fractionMs,
                            text =
                                lyricText
                        )
                }
            }

        if (
            timed.isNotEmpty()
        ) {
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
                .map {
                    it.trim()
                }
                .filter {
                    it.isNotEmpty()
                }
                .map {
                    XvoxLyricLine(
                        timeMs = null,
                        text = it
                    )
                }
                .toList()

        return XvoxLyrics(
            lines =
                plainLines,
            synchronized =
                false,
            source =
                source
        )
    }
}
