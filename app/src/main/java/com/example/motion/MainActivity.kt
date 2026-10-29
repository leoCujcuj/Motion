package com.example.motion

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.service.quicksettings.TileService
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.example.motion.ui.MotionMainScreen
import com.example.motion.ui.MotionViewModel
import com.example.motion.ui.theme.MotionTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MotionViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // El servicio funciona aunque el usuario rechace notificaciones;
        // si acepta, podrá pausar también desde la notificación silenciosa.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)

        setContent {
            MotionTheme {
                val uiState by viewModel.uiState.collectAsState()

                MotionMainScreen(
                    uiState = uiState,
                    onToggleStartPause = {
                        requestNotificationPermissionIfNeeded()
                        viewModel.toggleStartPause(this)
                    },
                    onOpenSettings = viewModel::openSettingsSheet,
                    onDismissSettings = viewModel::closeSettingsSheet,
                    onShapeSelected = viewModel::updateShape,
                    onSizeChanged = viewModel::updateSizeDp,
                    onColorSelected = viewModel::updateColor,
                    onConfirmOverlayPermission = {
                        openOverlayPermissionSettings()
                    },
                    onDismissOverlayPermission = viewModel::dismissOverlayPermissionPrompt
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.onHostResumed(this)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return

        // Pulsación larga en el Tile de Ajustes Rápidos de Samsung / Android
        if (intent.action == TileService.ACTION_QS_TILE_PREFERENCES ||
            intent.getBooleanExtra(EXTRA_OPEN_SETTINGS, false)
        ) {
            viewModel.openSettingsSheet()
        }

        if (intent.getBooleanExtra(EXTRA_PROMPT_OVERLAY_PERMISSION, false)) {
            intent.removeExtra(EXTRA_PROMPT_OVERLAY_PERMISSION)
            if (!Settings.canDrawOverlays(this)) {
                viewModel.requestOverlayPermissionPrompt()
            } else {
                viewModel.toggleStartPause(this)
            }
        }
    }

    private fun openOverlayPermissionSettings() {
        val overlayIntent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        try {
            startActivity(overlayIntent)
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    companion object {
        const val EXTRA_OPEN_SETTINGS = "com.example.motion.extra.OPEN_SETTINGS"
        const val EXTRA_PROMPT_OVERLAY_PERMISSION =
            "com.example.motion.extra.PROMPT_OVERLAY_PERMISSION"
    }
}
