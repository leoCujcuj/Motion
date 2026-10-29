package com.example.motion.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motion.R
import com.example.motion.model.CueShape
import com.example.motion.model.ThemeMode
import com.example.motion.ui.components.AppleStartPauseButton
import com.example.motion.ui.components.MotionSettingsSheet
import com.example.motion.ui.components.OverlayPermissionDialog
import com.example.motion.ui.icons.MotionIcons
import com.example.motion.ui.theme.LocalIsDarkTheme

@Composable
fun MotionMainScreen(
    uiState: MotionUiState,
    onToggleStartPause: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismissSettings: () -> Unit,
    onShapeSelected: (CueShape) -> Unit,
    onSizeChanged: (Float) -> Unit,
    onColorSelected: (Int) -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onConfirmOverlayPermission: () -> Unit,
    onDismissOverlayPermission: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Ícono sutil de ajustes (tuerca) en la esquina superior derecha
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 14.dp, end = 20.dp)
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    if (isDark) {
                        Color.White.copy(alpha = 0.08f)
                    } else {
                        Color.Black.copy(alpha = 0.05f)
                    }
                )
                .border(
                    width = 0.8.dp,
                    color = if (isDark) {
                        Color.White.copy(alpha = 0.12f)
                    } else {
                        Color.Black.copy(alpha = 0.06f)
                    },
                    shape = CircleShape
                )
                .clickable(
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.content_desc_settings),
                    onClick = onOpenSettings
                )
        ) {
            Icon(
                imageVector = MotionIcons.Gear,
                contentDescription = stringResource(R.string.content_desc_settings),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        // Único botón grande central de Iniciar / Pausar
        AppleStartPauseButton(
            isRunning = uiState.isRunning,
            accentColorArgb = uiState.settings.colorArgb,
            onToggle = onToggleStartPause,
            modifier = Modifier.align(Alignment.Center)
        )

        // Mensaje discreto de error de hardware si aplica
        uiState.errorMessage?.let { error ->
            Text(
                text = error,
                color = Color(0xFFFF3B30),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 32.dp, vertical = 40.dp)
            )
        }

        // Hoja modal inferior translúcida estilo iOS
        if (uiState.isSettingsOpen) {
            MotionSettingsSheet(
                settings = uiState.settings,
                onShapeSelected = onShapeSelected,
                onSizeChanged = onSizeChanged,
                onColorSelected = onColorSelected,
                onThemeModeSelected = onThemeModeSelected,
                onDismiss = onDismissSettings
            )
        }

        if (uiState.showOverlayPermissionPrompt) {
            OverlayPermissionDialog(
                onConfirmOpenSettings = onConfirmOverlayPermission,
                onDismiss = onDismissOverlayPermission
            )
        }
    }
}
