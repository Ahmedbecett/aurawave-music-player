package com.example.data

import com.example.model.LyricLine
import com.example.model.Song

object BuiltInMusicCatalog {

    fun parseLyrics(raw: String?): List<LyricLine> {
        if (raw.isNullOrBlank()) return emptyList()
        val regex = Regex("\\[(\\d{2}):(\\d{2}(?:\\.\\d{1,2})?)\\](.*)")
        val lines = mutableListOf<LyricLine>()
        raw.lines().forEach { line ->
            val match = regex.find(line.trim())
            if (match != null) {
                val min = match.groupValues[1].toLongOrNull() ?: 0L
                val secStr = match.groupValues[2]
                val sec = (secStr.toDoubleOrNull() ?: 0.0) * 1000.0
                val totalMs = (min * 60 * 1000) + sec.toLong()
                val text = match.groupValues[3].trim()
                lines.add(LyricLine(totalMs, text))
            }
        }
        return lines.sortedBy { it.timestampMs }
    }

    // All built-in songs completely removed per user request:
    // Only real user device files and imported songs are loaded!
    val defaultSongs: List<Song> = emptyList()
}
