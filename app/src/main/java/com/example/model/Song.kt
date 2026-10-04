package com.example.model

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val dataUri: String = "",
    val albumArtRes: Int? = null,
    val albumArtUri: String? = null,
    val genre: String = "عام",
    val year: Int = 2026,
    val lyrics: String? = null,
    val isFavorite: Boolean = false,
    val isBundled: Boolean = false,
    val synthPatternId: Int = 0
)

data class LyricLine(
    val timestampMs: Long,
    val text: String
)
