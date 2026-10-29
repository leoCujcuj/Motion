package com.example.motion.data

import android.content.Context
import android.content.SharedPreferences
import com.example.motion.model.CueShape
import com.example.motion.model.MotionRuntimeState
import com.example.motion.model.MotionSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MotionRepository private constructor(appContext: Context) {

    private val prefs: SharedPreferences = appContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<MotionSettings> = _settings.asStateFlow()

    private val _runtimeState = MutableStateFlow(MotionRuntimeState())
    val runtimeState: StateFlow<MotionRuntimeState> = _runtimeState.asStateFlow()

    fun updateColor(colorArgb: Int) {
        val next = _settings.value.copy(colorArgb = colorArgb).validated()
        _settings.value = next
        prefs.edit().putInt(KEY_COLOR_ARGB, next.colorArgb).apply()
    }

    fun updateSizeDp(sizeDp: Float) {
        if (!sizeDp.isFinite()) return
        val next = _settings.value.copy(sizeDp = sizeDp).validated()
        _settings.value = next
        prefs.edit().putFloat(KEY_SIZE_DP, next.sizeDp).apply()
    }

    fun updateShape(shape: CueShape) {
        val next = _settings.value.copy(shape = shape).validated()
        _settings.value = next
        prefs.edit().putString(KEY_SHAPE, next.shape.storageKey).apply()
    }

    fun setRunning(isRunning: Boolean) {
        _runtimeState.update { current ->
            current.copy(
                isRunning = isRunning,
                errorMessage = if (isRunning) null else current.errorMessage
            )
        }
    }

    fun setError(message: String?) {
        _runtimeState.update { current ->
            current.copy(isRunning = false, errorMessage = message)
        }
    }

    fun clearError() {
        _runtimeState.update { current ->
            current.copy(errorMessage = null)
        }
    }

    private fun loadSettings(): MotionSettings {
        val rawColor = prefs.getInt(KEY_COLOR_ARGB, MotionSettings.DEFAULT_COLOR_ARGB)
        val rawSize = prefs.getFloat(KEY_SIZE_DP, MotionSettings.DEFAULT_SIZE_DP)
        val rawShape = prefs.getString(KEY_SHAPE, CueShape.CIRCLE.storageKey)
        return MotionSettings(
            colorArgb = rawColor,
            sizeDp = if (rawSize.isFinite()) rawSize else MotionSettings.DEFAULT_SIZE_DP,
            shape = CueShape.fromStorageKey(rawShape)
        ).validated()
    }

    companion object {
        private const val PREFS_NAME = "motion_preferences"
        private const val KEY_COLOR_ARGB = "color_argb"
        private const val KEY_SIZE_DP = "size_dp"
        private const val KEY_SHAPE = "cue_shape"

        @Volatile
        private var instance: MotionRepository? = null

        fun getInstance(context: Context): MotionRepository {
            return instance ?: synchronized(this) {
                instance ?: MotionRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
