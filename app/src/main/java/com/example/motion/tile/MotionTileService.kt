package com.example.motion.tile

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.motion.MainActivity
import com.example.motion.R
import com.example.motion.data.MotionRepository
import com.example.motion.overlay.MotionServiceController
import com.example.motion.overlay.StartResult

/**
 * TileService nativo para el panel de Ajustes Rápidos de Android / Samsung One UI.
 * - Toque corto: alterna en tiempo real entre Iniciar y Pausar el servicio de overlay.
 * - Pulsación larga: el sistema enruta ACTION_QS_TILE_PREFERENCES hacia MainActivity
 *   abriendo directamente la hoja de ajustes.
 */
class MotionTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        syncTileVisualState()
    }

    override fun onTileAdded() {
        super.onTileAdded()
        syncTileVisualState()
    }

    override fun onClick() {
        super.onClick()
        if (isLocked) {
            unlockAndRun { handleToggleAction() }
        } else {
            handleToggleAction()
        }
    }

    private fun handleToggleAction() {
        val repository = MotionRepository.getInstance(this)
        val currentlyRunning = repository.runtimeState.value.isRunning

        if (currentlyRunning) {
            MotionServiceController.stop(this)
            applyTileState(isActive = false)
            return
        }

        if (!Settings.canDrawOverlays(this)) {
            openActivityFromTile(
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(MainActivity.EXTRA_PROMPT_OVERLAY_PERMISSION, true)
                }
            )
            return
        }

        when (MotionServiceController.start(this)) {
            StartResult.STARTED -> {
                applyTileState(isActive = true)
            }
            StartResult.BLOCKED_FROM_BACKGROUND -> {
                // Fallback seguro para Android 14-16 / One UI cuando el sistema exige
                // contexto de primer plano para iniciar un ForegroundService desde Quick Settings.
                applyTileState(isActive = true)
                openActivityFromTile(
                    Intent(this, QuickTileTrampolineActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_NO_ANIMATION or
                            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    }
                )
            }
            StartResult.NEEDS_OVERLAY_PERMISSION -> {
                applyTileState(isActive = false)
                openActivityFromTile(
                    Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra(MainActivity.EXTRA_PROMPT_OVERLAY_PERMISSION, true)
                    }
                )
            }
        }
    }

    private fun openActivityFromTile(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                REQUEST_CODE_TILE_LAUNCH,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun syncTileVisualState() {
        val isRunning = MotionRepository.getInstance(this).runtimeState.value.isRunning &&
            Settings.canDrawOverlays(this)
        applyTileState(isActive = isRunning)
    }

    private fun applyTileState(isActive: Boolean) {
        val tile = qsTile ?: return
        tile.label = getString(R.string.tile_label)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_motion)
        tile.state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = getString(
                if (isActive) R.string.tile_subtitle_active else R.string.tile_subtitle_paused
            )
        }
        tile.updateTile()
    }

    companion object {
        private const val REQUEST_CODE_TILE_LAUNCH = 205

        fun requestSync(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    requestListeningState(
                        context.applicationContext,
                        ComponentName(context.applicationContext, MotionTileService::class.java)
                    )
                } catch (_: Exception) {
                    // Ignorar si el sistema no tiene registrado el tile activo en el panel
                }
            }
        }
    }
}
