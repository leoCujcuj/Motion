package com.example.motion.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motion.R
import com.example.motion.ui.icons.MotionIcons

@Composable
fun AppleStartPauseButton(
    isRunning: Boolean,
    accentColorArgb: Int,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "button_press_scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "active_halo")
    val haloPulse by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.13f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_pulse_scale"
    )

    val activeTint = remember(accentColorArgb) {
        val raw = Color(accentColorArgb)
        val luminance = 0.299f * raw.red + 0.587f * raw.green + 0.114f * raw.blue
        if (luminance > 0.85f || luminance < 0.08f) {
            Color(0xFF007AFF)
        } else {
            raw
        }
    }

    val buttonTopColor by animateColorAsState(
        targetValue = if (isRunning) {
            activeTint
        } else if (isDark) {
            Color(0xFF262629)
        } else {
            Color(0xFFFFFFFF)
        },
        animationSpec = tween(durationMillis = 320),
        label = "button_top_color"
    )

    val buttonBottomColor by animateColorAsState(
        targetValue = if (isRunning) {
            activeTint.copy(
                red = (activeTint.red * 0.82f).coerceIn(0f, 1f),
                green = (activeTint.green * 0.82f).coerceIn(0f, 1f),
                blue = (activeTint.blue * 0.88f).coerceIn(0f, 1f)
            )
        } else if (isDark) {
            Color(0xFF1A1A1D)
        } else {
            Color(0xFFF4F4F8)
        },
        animationSpec = tween(durationMillis = 320),
        label = "button_bottom_color"
    )

    val contentTint by animateColorAsState(
        targetValue = if (isRunning) {
            Color.White
        } else if (isDark) {
            Color(0xFFF5F5F7)
        } else {
            Color(0xFF1C1C1E)
        },
        animationSpec = tween(durationMillis = 280),
        label = "content_tint"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(236.dp)
        ) {
            // Halo ambiental respirante cuando el servicio está activo
            if (isRunning) {
                Box(
                    modifier = Modifier
                        .size(204.dp)
                        .scale(haloPulse)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    activeTint.copy(alpha = 0.28f),
                                    activeTint.copy(alpha = 0.0f)
                                )
                            ),
                            shape = CircleShape
                        )
                )
            }

            // Botón principal táctil estilo Apple
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(184.dp)
                    .scale(pressScale)
                    .shadow(
                        elevation = if (isRunning) 22.dp else 12.dp,
                        shape = CircleShape,
                        ambientColor = if (isRunning) activeTint.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.16f),
                        spotColor = if (isRunning) activeTint.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.16f)
                    )
                    .clip(CircleShape)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(buttonTopColor, buttonBottomColor)
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isDark || isRunning) 0.28f else 0.90f),
                                Color.Black.copy(alpha = if (isDark) 0.35f else 0.06f)
                            )
                        ),
                        shape = CircleShape
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.Button,
                        onClickLabel = stringResource(
                            if (isRunning) R.string.action_pause else R.string.action_start
                        )
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggle()
                    }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isRunning) MotionIcons.Pause else MotionIcons.Play,
                        contentDescription = null,
                        tint = contentTint,
                        modifier = Modifier
                            .size(40.dp)
                            .offset(x = if (isRunning) 0.dp else 2.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(
                            if (isRunning) R.string.action_pause else R.string.action_start
                        ),
                        color = contentTint,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.3).sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Píldora sutil de estado
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(
                    if (isRunning) {
                        activeTint.copy(alpha = if (isDark) 0.18f else 0.12f)
                    } else {
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f)
                    }
                )
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = stringResource(
                    if (isRunning) R.string.status_active else R.string.status_paused
                ),
                color = if (isRunning) {
                    activeTint
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.1).sp
            )
        }
    }
}
