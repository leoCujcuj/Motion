package com.example.motion.ui.components

import android.app.StatusBarManager
import android.content.ComponentName
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motion.R
import com.example.motion.model.CueShape
import com.example.motion.model.MotionSettings
import com.example.motion.tile.MotionTileService
import com.example.motion.ui.icons.MotionIcons
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotionSettingsSheet(
    settings: MotionSettings,
    onShapeSelected: (CueShape) -> Unit,
    onSizeChanged: (Float) -> Unit,
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    var tileFeedbackMessage by remember { mutableStateOf<String?>(null) }

    val sheetContainerColor = if (isDark) {
        Color(0xFF1C1C1E).copy(alpha = 0.94f)
    } else {
        Color(0xFFF2F2F7).copy(alpha = 0.95f)
    }

    val cardColor = if (isDark) {
        Color(0xFF2C2C2E).copy(alpha = 0.90f)
    } else {
        Color(0xFFFFFFFF).copy(alpha = 0.92f)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = sheetContainerColor,
        scrimColor = Color.Black.copy(alpha = 0.28f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(38.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.4).sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            // Vista previa animada en vivo
            LiveCuePreviewCard(
                settings = settings,
                cardColor = cardColor
            )

            // Selector segmentado estilo iOS para Forma
            SettingsSectionCard(cardColor = cardColor) {
                Text(
                    text = stringResource(R.string.settings_shape_label),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                AppleSegmentedShapePicker(
                    selectedShape = settings.shape,
                    onShapeSelected = onShapeSelected,
                    isDark = isDark
                )
            }

            // Slider de Tamaño en tiempo real
            SettingsSectionCard(cardColor = cardColor) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.settings_size_label),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f dp", settings.sizeDp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Slider(
                    value = settings.sizeDp,
                    onValueChange = onSizeChanged,
                    valueRange = MotionSettings.MIN_SIZE_DP..MotionSettings.MAX_SIZE_DP,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
                    )
                )
            }

            // Paleta Apple + Selector continuo de Tono
            SettingsSectionCard(cardColor = cardColor) {
                Text(
                    text = stringResource(R.string.settings_color_label),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                val rows = remember { MotionSettings.APPLE_PALETTE.chunked(5) }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    rows.forEach { rowPresets ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            rowPresets.forEach { preset ->
                                ColorSwatchItem(
                                    colorArgb = preset.argb,
                                    contentDesc = preset.name,
                                    isSelected = settings.colorArgb == preset.argb,
                                    onClick = { onColorSelected(preset.argb) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.settings_custom_color_label),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                CustomHueSpectrumBar(
                    currentColorArgb = settings.colorArgb,
                    onColorChanged = onColorSelected
                )
            }

            // Integración con Ajustes Rápidos de Samsung One UI
            SettingsSectionCard(cardColor = cardColor) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                    ) {
                        Icon(
                            imageVector = MotionIcons.QuickTile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_quick_tile_title),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.settings_quick_tile_subtitle),
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .clickable(role = Role.Button) {
                                val statusBarManager =
                                    context.getSystemService(StatusBarManager::class.java)
                                statusBarManager?.requestAddTileService(
                                    ComponentName(context, MotionTileService::class.java),
                                    context.getString(R.string.tile_label),
                                    android.graphics.drawable.Icon.createWithResource(
                                        context,
                                        R.drawable.ic_qs_motion
                                    ),
                                    context.mainExecutor
                                ) { resultCode ->
                                    if (resultCode == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED) {
                                        tileFeedbackMessage =
                                            context.getString(R.string.settings_quick_tile_added)
                                    }
                                }
                            }
                            .padding(vertical = 11.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_quick_tile_add),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    tileFeedbackMessage?.let { feedback ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = feedback,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    cardColor: Color,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardColor)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        content()
    }
}

@Composable
private fun LiveCuePreviewCard(
    settings: MotionSettings,
    cardColor: Color
) {
    val infinite = rememberInfiniteTransition(label = "preview_motion")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "preview_phase"
    )

    val isDark = isSystemInDarkTheme()
    val cueColor = Color(settings.colorArgb)
    val luminance = 0.299f * cueColor.red + 0.587f * cueColor.green + 0.114f * cueColor.blue
    val haloColor = if (luminance > 0.55f) Color.Black.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.28f)

    SettingsSectionCard(cardColor = cardColor) {
        Text(
            text = stringResource(R.string.settings_preview_label),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (isDark) Color(0xFF141416) else Color(0xFFEAECEF)
                )
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val count = 5
                val cuePx = settings.sizeDp.dp.toPx()
                val padX = 24.dp.toPx()
                val usableW = (size.width - padX * 2f).coerceAtLeast(1f)
                val spacing = usableW / count
                val cy = size.height * 0.5f

                for (i in 0 until count) {
                    val localX = (i * spacing + phase * spacing) % usableW
                    val edgeDist = minOf(localX, usableW - localX)
                    val fade = (edgeDist / (spacing * 0.65f)).coerceIn(0.15f, 1f)
                    val cx = padX + localX

                    when (settings.shape) {
                        CueShape.CIRCLE -> {
                            val r = cuePx * 0.5f
                            drawCircle(
                                color = haloColor.copy(alpha = haloColor.alpha * fade),
                                radius = r + 1.2.dp.toPx(),
                                center = Offset(cx, cy),
                                style = Stroke(width = 1.2.dp.toPx())
                            )
                            drawCircle(
                                color = cueColor.copy(alpha = 0.92f * fade),
                                radius = r,
                                center = Offset(cx, cy)
                            )
                        }
                        CueShape.SQUARE -> {
                            val topLeft = Offset(cx - cuePx * 0.5f, cy - cuePx * 0.5f)
                            val sqSize = Size(cuePx, cuePx)
                            val cr = CornerRadius(cuePx * 0.26f, cuePx * 0.26f)
                            drawRoundRect(
                                color = cueColor.copy(alpha = 0.92f * fade),
                                topLeft = topLeft,
                                size = sqSize,
                                cornerRadius = cr
                            )
                            drawRoundRect(
                                color = haloColor.copy(alpha = haloColor.alpha * fade),
                                topLeft = topLeft,
                                size = sqSize,
                                cornerRadius = cr,
                                style = Stroke(width = 1.1.dp.toPx())
                            )
                        }
                        CueShape.SUBTLE_LINE -> {
                            val halfLen = cuePx * 1.35f
                            val strokeW = (cuePx * 0.30f).coerceAtLeast(2.2.dp.toPx())
                            drawLine(
                                color = cueColor.copy(alpha = 0.55f * fade),
                                start = Offset(cx - halfLen, cy),
                                end = Offset(cx + halfLen, cy),
                                strokeWidth = strokeW,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppleSegmentedShapePicker(
    selectedShape: CueShape,
    onShapeSelected: (CueShape) -> Unit,
    isDark: Boolean
) {
    val trackColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFE5E5EA)
    val activeSegmentColor = if (isDark) Color(0xFF3A3A3C) else Color.White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(trackColor)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        CueShape.entries.forEach { shape ->
            val selected = shape == selectedShape
            val bg by animateColorAsState(
                targetValue = if (selected) activeSegmentColor else Color.Transparent,
                animationSpec = tween(durationMillis = 180),
                label = "segment_bg"
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(bg)
                    .clickable(role = Role.RadioButton) { onShapeSelected(shape) }
                    .padding(vertical = 9.dp, horizontal = 4.dp)
            ) {
                Text(
                    text = stringResource(shape.labelRes),
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ColorSwatchItem(
    colorArgb: Int,
    contentDesc: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = Color(colorArgb)
    val luminance = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
    val checkTint = if (luminance > 0.62f) Color(0xFF1C1C1E) else Color.White

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
                },
                shape = CircleShape
            )
            .clickable(
                role = Role.RadioButton,
                onClickLabel = contentDesc,
                onClick = onClick
            )
    ) {
        if (isSelected) {
            Icon(
                imageVector = MotionIcons.Check,
                contentDescription = null,
                tint = checkTint,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun CustomHueSpectrumBar(
    currentColorArgb: Int,
    onColorChanged: (Int) -> Unit
) {
    val initialHue = remember(currentColorArgb) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(currentColorArgb, hsv)
        hsv[0]
    }
    var thumbFraction by remember { mutableFloatStateOf((initialHue / 360f).coerceIn(0f, 1f)) }

    val spectrumBrush = remember {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFFFF3B30),
                Color(0xFFFF9500),
                Color(0xFFFFCC00),
                Color(0xFF34C759),
                Color(0xFF32ADE6),
                Color(0xFF007AFF),
                Color(0xFF5856D6),
                Color(0xFFFF2D55),
                Color(0xFFFF3B30)
            )
        )
    }

    fun applyFraction(fraction: Float) {
        val clamped = fraction.coerceIn(0f, 1f)
        thumbFraction = clamped
        val hue = clamped * 360f
        val argb = android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.84f, 0.96f))
        onColorChanged(argb)
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .clip(CircleShape)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    if (size.width > 0) {
                        applyFraction(offset.x / size.width.toFloat())
                    }
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    if (size.width > 0) {
                        applyFraction(change.position.x / size.width.toFloat())
                    }
                }
            }
    ) {
        val barHeight = 14.dp.toPx()
        val barTop = (size.height - barHeight) * 0.5f
        drawRoundRect(
            brush = spectrumBrush,
            topLeft = Offset(0f, barTop),
            size = Size(size.width, barHeight),
            cornerRadius = CornerRadius(barHeight * 0.5f, barHeight * 0.5f)
        )

        val thumbRadius = 11.dp.toPx()
        val thumbX = (thumbFraction * size.width).coerceIn(thumbRadius, size.width - thumbRadius)
        val center = Offset(thumbX, size.height * 0.5f)

        drawCircle(
            color = Color.White,
            radius = thumbRadius,
            center = center
        )
        drawCircle(
            color = Color(currentColorArgb),
            radius = thumbRadius - 3.dp.toPx(),
            center = center
        )
        drawCircle(
            color = Color.Black.copy(alpha = 0.20f),
            radius = thumbRadius,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
    }
}
