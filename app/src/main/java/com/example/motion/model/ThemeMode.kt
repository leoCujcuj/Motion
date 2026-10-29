package com.example.motion.model

import androidx.annotation.StringRes
import com.example.motion.R

enum class ThemeMode(
    val storageKey: String,
    @param:StringRes val labelRes: Int
) {
    SYSTEM("system", R.string.theme_system),
    LIGHT("light", R.string.theme_light),
    DARK("dark", R.string.theme_dark);

    companion object {
        fun fromStorageKey(key: String?): ThemeMode {
            return entries.firstOrNull { it.storageKey == key } ?: SYSTEM
        }
    }
}
