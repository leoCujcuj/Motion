package com.example.motion.model

import androidx.annotation.StringRes
import com.example.motion.R

enum class CueShape(
    val storageKey: String,
    @param:StringRes val labelRes: Int
) {
    CIRCLE("circle", R.string.shape_circles),
    SQUARE("square", R.string.shape_squares),
    SUBTLE_LINE("subtle_line", R.string.shape_subtle_lines);

    companion object {
        fun fromStorageKey(key: String?): CueShape {
            return entries.firstOrNull { it.storageKey == key } ?: CIRCLE
        }
    }
}
