package com.example.model

data class EqualizerBand(
    val index: Int,
    val centerFreqHz: Int,
    val label: String,
    val gainDb: Int // -12 to +12 dB
)

data class EqualizerState(
    val isEnabled: Boolean = true,
    val presetName: String = "عادي",
    val bands: List<EqualizerBand> = defaultBands(),
    val bassBoost: Int = 30, // 0 - 100
    val virtualizer: Int = 25, // 0 - 100
    val reverbPreset: String = "استوديو"
) {
    companion object {
        fun defaultBands(): List<EqualizerBand> = listOf(
            EqualizerBand(0, 60, "60 Hz", 0),
            EqualizerBand(1, 230, "230 Hz", 0),
            EqualizerBand(2, 910, "910 Hz", 0),
            EqualizerBand(3, 3600, "3.6 kHz", 0),
            EqualizerBand(4, 14000, "14 kHz", 0)
        )

        val PRESETS: Map<String, List<Int>> = mapOf(
            "عادي" to listOf(0, 0, 0, 0, 0),
            "روك" to listOf(5, 3, -1, 4, 6),
            "بوب" to listOf(-1, 2, 5, 2, -1),
            "جاز" to listOf(3, 2, 1, 2, 4),
            "كلاسيكي" to listOf(4, 3, 2, 3, 5),
            "تضخيم الباس" to listOf(9, 7, 3, 1, 0),
            "صوت نقي" to listOf(-2, 1, 6, 4, 2),
            "إلكتروني" to listOf(6, 4, 0, 3, 5)
        )
    }
}

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}
