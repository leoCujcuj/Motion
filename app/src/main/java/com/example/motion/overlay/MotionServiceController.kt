package com.example.motion.overlay

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.motion.data.MotionRepository

enum class StartResult {
    STARTED,
    NEEDS_OVERLAY_PERMISSION,
    BLOCKED_FROM_BACKGROUND
}

object MotionServiceController {

    fun start(context: Context): StartResult {
        val appContext = context.applicationContext
        if (!Settings.canDrawOverlays(appContext)) {
            MotionRepository.getInstance(appContext).setRunning(false)
            return StartResult.NEEDS_OVERLAY_PERMISSION
        }

        val serviceIntent = Intent(appContext, MotionOverlayService::class.java).apply {
            action = MotionOverlayService.ACTION_START
        }

        return try {
            ContextCompat.startForegroundService(appContext, serviceIntent)
            StartResult.STARTED
        } catch (_: IllegalStateException) {
            // En Android 12-16, si se invoca desde TileService sin exención de primer plano
            StartResult.BLOCKED_FROM_BACKGROUND
        } catch (_: SecurityException) {
            StartResult.BLOCKED_FROM_BACKGROUND
        }
    }

    fun stop(context: Context) {
        val appContext = context.applicationContext
        MotionRepository.getInstance(appContext).setRunning(false)
        val stopIntent = Intent(appContext, MotionOverlayService::class.java)
        appContext.stopService(stopIntent)
    }
}
