package com.example.motion.model

data class ColorPreset(
    val name: String,
    val argb: Int
)

data class MotionSettings(
    val colorArgb: Int = DEFAULT_COLOR_ARGB,
    val sizeDp: Float = DEFAULT_SIZE_DP,
    val shape: CueShape = CueShape.CIRCLE,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
) {
    fun validated(): MotionSettings = copy(
        colorArgb = colorArgb or OPAQUE_ALPHA_MASK,
        sizeDp = sizeDp.coerceIn(MIN_SIZE_DP, MAX_SIZE_DP),
        themeMode = themeMode
    )

    companion object {
        const val MIN_SIZE_DP = 4f
        const val MAX_SIZE_DP = 18f
        const val DEFAULT_SIZE_DP = 8.5f
        const val DEFAULT_COLOR_ARGB = 0xFF007AFF.toInt()
        private const val OPAQUE_ALPHA_MASK = 0xFF000000.toInt()

        val APPLE_PALETTE: List<ColorPreset> = listOf(
            ColorPreset("Azul iOS", 0xFF007AFF.toInt()),
            ColorPreset("Carbón", 0xFF1C1C1E.toInt()),
            ColorPreset("Blanco", 0xFFF5F5F7.toInt()),
            ColorPreset("Gris espacial", 0xFF8E8E93.toInt()),
            ColorPreset("Índigo", 0xFF5856D6.toInt()),
            ColorPreset("Cian", 0xFF32ADE6.toInt()),
            ColorPreset("Verde", 0xFF34C759.toInt()),
            ColorPreset("Ámbar", 0xFFFF9500.toInt()),
            ColorPreset("Coral", 0xFFFF2D55.toInt())
        )
    }
}
