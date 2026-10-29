package com.example.motion.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.Choreographer
import android.view.View
import com.example.motion.model.CueShape
import com.example.motion.model.MotionSettings
import com.example.motion.sensor.MotionCueKinematics
import com.example.motion.sensor.MotionPhysicsEngine
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

class MotionOverlayView(
    context: Context,
    private val physicsEngine: MotionPhysicsEngine
) : View(context), Choreographer.FrameCallback {

    private var currentSettings: MotionSettings = MotionSettings()
    private var kinematics: MotionCueKinematics = MotionCueKinematics()

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val tempRect = RectF()

    private var frameLoopScheduled = false
    private var lastFrameTimeNanos = 0L

    init {
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        applySettingsToPaints(currentSettings)
    }

    fun updateSettings(settings: MotionSettings) {
        currentSettings = settings
        applySettingsToPaints(settings)
        invalidate()
    }

    fun notifySensorUpdated() {
        ensureFrameLoopRunning()
    }

    fun pauseAnimation() {
        if (frameLoopScheduled) {
            Choreographer.getInstance().removeFrameCallback(this)
            frameLoopScheduled = false
        }
        lastFrameTimeNanos = 0L
    }

    override fun onDetachedFromWindow() {
        pauseAnimation()
        super.onDetachedFromWindow()
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!isAttachedToWindow) {
            frameLoopScheduled = false
            lastFrameTimeNanos = 0L
            return
        }

        val dtSeconds = if (lastFrameTimeNanos == 0L) {
            DEFAULT_FRAME_DT_SECONDS
        } else {
            ((frameTimeNanos - lastFrameTimeNanos) * 1e-9f).coerceIn(0.001f, 0.050f)
        }
        lastFrameTimeNanos = frameTimeNanos

        val density = resources.displayMetrics.density
        val verticalSpan = (height - 2f * dpToPx(VERTICAL_SAFE_INSET_DP, density)).coerceAtLeast(100f)
        val horizontalSpan = (width - 2f * dpToPx(HORIZONTAL_LANE_INSET_DP, density)).coerceAtLeast(100f)

        val spacingY = verticalSpan / SIDE_CUE_COUNT
        val spacingX = horizontalSpan / HORIZONTAL_CUE_COUNT

        kinematics = physicsEngine.stepFrame(
            frameDtSeconds = dtSeconds,
            density = density,
            wrapPeriodX = spacingX,
            wrapPeriodY = spacingY
        )

        invalidate()

        if (kinematics.isMoving) {
            Choreographer.getInstance().postFrameCallback(this)
        } else {
            frameLoopScheduled = false
            lastFrameTimeNanos = 0L
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val density = resources.displayMetrics.density
        val cueSizePx = dpToPx(currentSettings.sizeDp, density)
        val sideInsetPx = dpToPx(SIDE_MARGIN_DP, density)
        val topBottomInsetPx = dpToPx(TOP_BOTTOM_MARGIN_DP, density)
        val verticalSafeInsetPx = dpToPx(VERTICAL_SAFE_INSET_DP, density)
        val horizontalLaneInsetPx = dpToPx(HORIZONTAL_LANE_INSET_DP, density)

        val verticalSpan = (h - 2f * verticalSafeInsetPx).coerceAtLeast(1f)
        val spacingY = verticalSpan / SIDE_CUE_COUNT
        val fadeZoneY = min(dpToPx(FADE_ZONE_DP, density), verticalSpan * 0.25f)

        val leftColumnX = (sideInsetPx + kinematics.swayX).coerceIn(cueSizePx, w * 0.25f)
        val rightColumnX = (w - sideInsetPx + kinematics.swayX).coerceIn(w * 0.75f, w - cueSizePx)

        // Columnas periféricas izquierda y derecha
        for (i in 0 until SIDE_CUE_COUNT) {
            val localY = (i * spacingY + kinematics.travelY) % verticalSpan
            val normalizedLocalY = if (localY < 0f) localY + verticalSpan else localY
            val y = verticalSafeInsetPx + normalizedLocalY
            val edgeAlpha = computeEdgeFade(normalizedLocalY, verticalSpan, fadeZoneY)

            drawCueItem(
                canvas = canvas,
                cx = leftColumnX,
                cy = y,
                sizePx = cueSizePx,
                alphaFactor = edgeAlpha,
                defaultAngleRad = Math.PI.toFloat() * 0.5f,
                density = density
            )
            drawCueItem(
                canvas = canvas,
                cx = rightColumnX,
                cy = y,
                sizePx = cueSizePx,
                alphaFactor = edgeAlpha,
                defaultAngleRad = Math.PI.toFloat() * 0.5f,
                density = density
            )
        }

        // Carriles periféricos superior e inferior para giros laterales
        val horizontalSpan = (w - 2f * horizontalLaneInsetPx).coerceAtLeast(1f)
        if (horizontalSpan > cueSizePx * 6f) {
            val spacingX = horizontalSpan / HORIZONTAL_CUE_COUNT
            val fadeZoneX = min(dpToPx(FADE_ZONE_DP, density), horizontalSpan * 0.28f)
            val topLaneY = (topBottomInsetPx + kinematics.swayY).coerceIn(cueSizePx, h * 0.20f)
            val bottomLaneY = (h - topBottomInsetPx + kinematics.swayY).coerceIn(h * 0.80f, h - cueSizePx)

            for (i in 0 until HORIZONTAL_CUE_COUNT) {
                val localX = (i * spacingX + kinematics.travelX) % horizontalSpan
                val normalizedLocalX = if (localX < 0f) localX + horizontalSpan else localX
                val x = horizontalLaneInsetPx + normalizedLocalX
                val edgeAlpha = computeEdgeFade(normalizedLocalX, horizontalSpan, fadeZoneX)

                drawCueItem(
                    canvas = canvas,
                    cx = x,
                    cy = topLaneY,
                    sizePx = cueSizePx,
                    alphaFactor = edgeAlpha,
                    defaultAngleRad = 0f,
                    density = density
                )
                drawCueItem(
                    canvas = canvas,
                    cx = x,
                    cy = bottomLaneY,
                    sizePx = cueSizePx,
                    alphaFactor = edgeAlpha,
                    defaultAngleRad = 0f,
                    density = density
                )
            }
        }
    }

    private fun drawCueItem(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        sizePx: Float,
        alphaFactor: Float,
        defaultAngleRad: Float,
        density: Float
    ) {
        if (alphaFactor <= 0.02f) return

        // Dibujar estela de partículas translúcidas si está habilitada
        drawMotionTrail(canvas, cx, cy, sizePx, alphaFactor, density)

        val baseColor = currentSettings.colorArgb
        val baseAlpha = (235f * alphaFactor).toInt().coerceIn(0, 255)
        val haloBaseAlpha = (52f * alphaFactor).toInt().coerceIn(0, 255)

        when (currentSettings.shape) {
            CueShape.CIRCLE -> {
                val radius = sizePx * 0.5f
                haloPaint.strokeWidth = dpToPx(1.1f, density)
                haloPaint.alpha = haloBaseAlpha
                fillPaint.color = baseColor
                fillPaint.alpha = baseAlpha

                canvas.drawCircle(cx, cy, radius + haloPaint.strokeWidth * 0.45f, haloPaint)
                canvas.drawCircle(cx, cy, radius, fillPaint)
            }

            CueShape.SQUARE -> {
                val half = sizePx * 0.5f
                val corner = sizePx * 0.26f
                tempRect.set(cx - half, cy - half, cx + half, cy + half)

                haloPaint.strokeWidth = dpToPx(1.1f, density)
                haloPaint.alpha = haloBaseAlpha
                fillPaint.color = baseColor
                fillPaint.alpha = baseAlpha

                canvas.drawRoundRect(tempRect, corner, corner, fillPaint)
                canvas.drawRoundRect(tempRect, corner, corner, haloPaint)
            }

            CueShape.SUBTLE_LINE -> {
                val speed = hypot(kinematics.velocityX, kinematics.velocityY)
                val speedNormalized = (speed / (320f * density)).coerceIn(0f, 1f)
                val halfLength = sizePx * (0.95f + 0.90f * speedNormalized)
                val angle = if (speed > 14f * density) {
                    atan2(kinematics.velocityY, kinematics.velocityX)
                } else {
                    defaultAngleRad
                }

                val dx = cos(angle) * halfLength
                val dy = sin(angle) * halfLength

                linePaint.color = baseColor
                linePaint.strokeWidth = (sizePx * 0.30f).coerceAtLeast(dpToPx(2.2f, density))
                linePaint.alpha = (135f * alphaFactor).toInt().coerceIn(0, 200)

                canvas.drawLine(cx - dx, cy - dy, cx + dx, cy + dy, linePaint)
            }
        }
    }

    private fun drawMotionTrail(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        sizePx: Float,
        alphaFactor: Float,
        density: Float
    ) {
        if (!currentSettings.showMotionTrail || alphaFactor <= 0.04f) return
        val vx = kinematics.velocityX
        val vy = kinematics.velocityY
        val speed = hypot(vx, vy)
        val minSpeed = 6f * density
        if (speed < minSpeed) return

        val speedNormalized = (speed / (280f * density)).coerceIn(0.12f, 1f)
        val dirX = -vx / speed
        val dirY = -vy / speed
        val baseSpacing = sizePx * (0.85f + 1.15f * speedNormalized)

        val trailCount = 3
        val baseColor = currentSettings.colorArgb

        for (k in 1..trailCount) {
            val progress = k.toFloat() / (trailCount + 1)
            // Degradado progresivo de translucidez y tamaño decreciente
            val trailAlphaFactor = (alphaFactor * (0.48f - progress * 0.32f)).coerceIn(0f, 1f)
            val trailAlpha = (235f * trailAlphaFactor).toInt().coerceIn(0, 255)
            if (trailAlpha <= 4) continue

            val trailSizePx = sizePx * (1f - progress * 0.42f)
            val trailX = cx + dirX * baseSpacing * k
            val trailY = cy + dirY * baseSpacing * k

            fillPaint.color = baseColor
            fillPaint.alpha = trailAlpha

            when (currentSettings.shape) {
                CueShape.CIRCLE -> {
                    canvas.drawCircle(trailX, trailY, trailSizePx * 0.5f, fillPaint)
                }
                CueShape.SQUARE -> {
                    val half = trailSizePx * 0.5f
                    val corner = trailSizePx * 0.26f
                    tempRect.set(trailX - half, trailY - half, trailX + half, trailY + half)
                    canvas.drawRoundRect(tempRect, corner, corner, fillPaint)
                }
                CueShape.SUBTLE_LINE -> {
                    val halfLen = trailSizePx * 0.70f
                    val strokeW = (trailSizePx * 0.25f).coerceAtLeast(dpToPx(1.5f, density))
                    linePaint.color = baseColor
                    linePaint.strokeWidth = strokeW
                    linePaint.alpha = trailAlpha
                    val angle = atan2(vy, vx)
                    val dx = cos(angle) * halfLen
                    val dy = sin(angle) * halfLen
                    canvas.drawLine(trailX - dx, trailY - dy, trailX + dx, trailY + dy, linePaint)
                }
            }
        }
    }

    private fun applySettingsToPaints(settings: MotionSettings) {
        val color = settings.colorArgb
        fillPaint.color = color
        linePaint.color = color

        // Halo de contraste inverso para mantener legibilidad sobre fondos blancos o negros
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        val luminance = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
        haloPaint.color = if (luminance > 0.55f) Color.BLACK else Color.WHITE
    }

    private fun computeEdgeFade(position: Float, span: Float, fadeZone: Float): Float {
        if (fadeZone <= 1f) return 1f
        val distFromStart = position
        val distFromEnd = span - position
        val nearest = min(distFromStart, distFromEnd)
        return (nearest / fadeZone).coerceIn(0f, 1f)
    }

    private fun ensureFrameLoopRunning() {
        if (!frameLoopScheduled && isAttachedToWindow) {
            frameLoopScheduled = true
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    private fun dpToPx(dp: Float, density: Float): Float = dp * density

    companion object {
        private const val SIDE_CUE_COUNT = 5
        private const val HORIZONTAL_CUE_COUNT = 2
        private const val SIDE_MARGIN_DP = 20f
        private const val TOP_BOTTOM_MARGIN_DP = 44f
        private const val VERTICAL_SAFE_INSET_DP = 72f
        private const val HORIZONTAL_LANE_INSET_DP = 84f
        private const val FADE_ZONE_DP = 42f
        private const val DEFAULT_FRAME_DT_SECONDS = 0.016f
    }
}
