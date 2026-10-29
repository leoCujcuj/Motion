package com.example.motion.ui

import android.app.Application
import android.content.Context
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.motion.data.MotionRepository
import com.example.motion.model.CueShape
import com.example.motion.model.MotionSettings
import com.example.motion.model.ThemeMode
import com.example.motion.overlay.MotionServiceController
import com.example.motion.overlay.StartResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class MotionUiState(
    val settings: MotionSettings = MotionSettings(),
    val isRunning: Boolean = false,
    val errorMessage: String? = null,
    val isSettingsOpen: Boolean = false,
    val showOverlayPermissionPrompt: Boolean = false
)

private data class DialogVisibilityState(
    val isSettingsOpen: Boolean = false,
    val showOverlayPermissionPrompt: Boolean = false,
    val pendingAutoStartAfterPermission: Boolean = false
)

class MotionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MotionRepository.getInstance(application)
    private val dialogState = MutableStateFlow(DialogVisibilityState())

    val uiState: StateFlow<MotionUiState> = combine(
        repository.settings,
        repository.runtimeState,
        dialogState
    ) { settings, runtime, dialogs ->
        MotionUiState(
            settings = settings,
            isRunning = runtime.isRunning,
            errorMessage = runtime.errorMessage,
            isSettingsOpen = dialogs.isSettingsOpen,
            showOverlayPermissionPrompt = dialogs.showOverlayPermissionPrompt
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MotionUiState(
            settings = repository.settings.value,
            isRunning = repository.runtimeState.value.isRunning,
            errorMessage = repository.runtimeState.value.errorMessage
        )
    )

    fun toggleStartPause(context: Context): Boolean {
        repository.clearError()
        if (repository.runtimeState.value.isRunning) {
            MotionServiceController.stop(context)
            return false
        }

        return when (MotionServiceController.start(context)) {
            StartResult.STARTED -> true
            StartResult.NEEDS_OVERLAY_PERMISSION -> {
                dialogState.update {
                    it.copy(
                        showOverlayPermissionPrompt = true,
                        pendingAutoStartAfterPermission = true
                    )
                }
                false
            }
            StartResult.BLOCKED_FROM_BACKGROUND -> false
        }
    }

    fun onHostResumed(context: Context) {
        val hasPermission = Settings.canDrawOverlays(context)
        if (!hasPermission && repository.runtimeState.value.isRunning) {
            MotionServiceController.stop(context)
        }
        val shouldAutoStart = dialogState.value.pendingAutoStartAfterPermission && hasPermission
        if (shouldAutoStart) {
            dialogState.update {
                it.copy(
                    showOverlayPermissionPrompt = false,
                    pendingAutoStartAfterPermission = false
                )
            }
            MotionServiceController.start(context)
        } else if (hasPermission && dialogState.value.showOverlayPermissionPrompt) {
            dialogState.update { it.copy(showOverlayPermissionPrompt = false) }
        }
    }

    fun openSettingsSheet() {
        dialogState.update { it.copy(isSettingsOpen = true) }
    }

    fun closeSettingsSheet() {
        dialogState.update { it.copy(isSettingsOpen = false) }
    }

    fun requestOverlayPermissionPrompt() {
        dialogState.update {
            it.copy(
                showOverlayPermissionPrompt = true,
                pendingAutoStartAfterPermission = true
            )
        }
    }

    fun dismissOverlayPermissionPrompt() {
        dialogState.update {
            it.copy(
                showOverlayPermissionPrompt = false,
                pendingAutoStartAfterPermission = false
            )
        }
    }

    fun updateColor(colorArgb: Int) {
        repository.updateColor(colorArgb)
    }

    fun updateSizeDp(sizeDp: Float) {
        repository.updateSizeDp(sizeDp)
    }

    fun updateShape(shape: CueShape) {
        repository.updateShape(shape)
    }

    fun updateThemeMode(themeMode: ThemeMode) {
        repository.updateThemeMode(themeMode)
    }

    fun updateMotionTrail(enabled: Boolean) {
        repository.updateMotionTrail(enabled)
    }
}
