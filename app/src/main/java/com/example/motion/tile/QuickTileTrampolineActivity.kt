package com.example.motion.tile

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import com.example.motion.MainActivity
import com.example.motion.overlay.MotionServiceController

/**
 * Actividad transparente efímera utilizada únicamente cuando Android 14+ requiere
 * exención temporal de primer plano al iniciar el servicio desde TileService.
 */
class QuickTileTrampolineActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        disableTransitionAnimations()

        if (Settings.canDrawOverlays(this)) {
            MotionServiceController.start(this)
        } else {
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(MainActivity.EXTRA_PROMPT_OVERLAY_PERMISSION, true)
                }
            )
        }

        finish()
        disableTransitionAnimations()
    }

    private fun disableTransitionAnimations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}
