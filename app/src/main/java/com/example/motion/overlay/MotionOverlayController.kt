package com.example.motion.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.hardware.input.InputManager
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import com.example.motion.model.MotionSettings
import com.example.motion.sensor.MotionPhysicsEngine

class MotionOverlayController(
    private val context: Context,
    private val physicsEngine: MotionPhysicsEngine
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: MotionOverlayView? = null

    val isAttached: Boolean
        get() = overlayView != null

    fun attach(initialSettings: MotionSettings): Boolean {
        if (overlayView != null) return true
        if (!Settings.canDrawOverlays(context)) return false

        val view = MotionOverlayView(context, physicsEngine).apply {
            updateSettings(initialSettings)
        }

        val layoutParams = createOverlayLayoutParams()

        return try {
            windowManager.addView(view, layoutParams)
            overlayView = view
            true
        } catch (_: Exception) {
            overlayView = null
            false
        }
    }

    fun updateSettings(settings: MotionSettings) {
        overlayView?.updateSettings(settings)
    }

    fun notifySensorUpdated() {
        overlayView?.notifySensorUpdated()
    }

    fun pauseAnimation() {
        overlayView?.pauseAnimation()
    }

    fun detach() {
        val current = overlayView ?: return
        current.pauseAnimation()
        try {
            windowManager.removeViewImmediate(current)
        } catch (_: Exception) {
            // Ignorar si WindowManager ya liberó la vista
        } finally {
            overlayView = null
        }
    }

    private fun createOverlayLayoutParams(): WindowManager.LayoutParams {
        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            windowType,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            // En Android 12+, mantener alpha debajo de maximumObscuringOpacityForTouch
            // garantiza que el sistema nunca bloquee toques hacia apps subyacentes.
            alpha = computeTrustedPassThroughAlpha()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    private fun computeTrustedPassThroughAlpha(): Float {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val inputManager = context.getSystemService(Context.INPUT_SERVICE) as? InputManager
            val maxAllowed = inputManager?.maximumObscuringOpacityForTouch ?: DEFAULT_SAFE_ALPHA
            return (maxAllowed * 0.95f).coerceIn(0.15f, DEFAULT_SAFE_ALPHA)
        }
        return 0.92f
    }

    companion object {
        private const val DEFAULT_SAFE_ALPHA = 0.78f
    }
}
