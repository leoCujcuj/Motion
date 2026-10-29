package com.example.motion.sensor

import kotlin.math.abs
import kotlin.math.hypot

data class MotionCueKinematics(
    val travelX: Float = 0f,
    val travelY: Float = 0f,
    val swayX: Float = 0f,
    val swayY: Float = 0f,
    val velocityX: Float = 0f,
    val velocityY: Float = 0f,
    val isMoving: Boolean = false
)

/**
 * Motor físico con doble filtro pasa-bajos para aislar la gravedad del acelerómetro,
 * suprimir vibraciones de alta frecuencia y proyectar aceleración inercial en coordenadas de pantalla.
 */
class MotionPhysicsEngine(
    private val tauGravitySeconds: Float = 0.80f,
    private val tauSmoothSeconds: Float = 0.12f,
    private val deadzoneMps2: Float = 0.08f
) {
    private var initialized = false
    private var lastSensorTimestampNanos: Long = 0L

    private var gravityX = 0f
    private var gravityY = 0f
    private var gravityZ = 0f

    var filteredAx: Float = 0f
        private set
    var filteredAy: Float = 0f
        private set

    private var cleanAx = 0f
    private var cleanAy = 0f

    private var velocityX = 0f
    private var velocityY = 0f
    private var travelX = 0f
    private var travelY = 0f
    private var swayX = 0f
    private var swayY = 0f

    fun reset() {
        initialized = false
        lastSensorTimestampNanos = 0L
        gravityX = 0f
        gravityY = 0f
        gravityZ = 0f
        filteredAx = 0f
        filteredAy = 0f
        cleanAx = 0f
        cleanAy = 0f
        velocityX = 0f
        velocityY = 0f
        travelX = 0f
        travelY = 0f
        swayX = 0f
        swayY = 0f
    }

    fun onAccelerometerSample(
        rawX: Float,
        rawY: Float,
        rawZ: Float,
        timestampNanos: Long,
        displayRotation: Int = ROTATION_0
    ): Boolean {
        if (!rawX.isFinite() || !rawY.isFinite() || !rawZ.isFinite()) {
            return false
        }

        if (!initialized) {
            gravityX = rawX
            gravityY = rawY
            gravityZ = rawZ
            lastSensorTimestampNanos = timestampNanos
            initialized = true
            return false
        }

        val rawDt = (timestampNanos - lastSensorTimestampNanos) * NANOS_TO_SECONDS
        lastSensorTimestampNanos = timestampNanos
        if (rawDt <= 0f) return isCurrentlyActive()

        val dt = rawDt.coerceIn(MIN_DT_SECONDS, MAX_DT_SECONDS)

        // Filtro pasa-bajos etapa 1: estimación del vector gravitacional
        val alphaGravity = dt / (tauGravitySeconds + dt)
        gravityX += alphaGravity * (rawX - gravityX)
        gravityY += alphaGravity * (rawY - gravityY)
        gravityZ += alphaGravity * (rawZ - gravityZ)

        val linearX = rawX - gravityX
        val linearY = rawY - gravityY
        val linearZ = rawZ - gravityZ

        // Remapeo según rotación actual de la pantalla
        val screenAx: Float
        val screenAy: Float
        when (displayRotation) {
            ROTATION_90 -> {
                screenAx = -linearY
                screenAy = linearX
            }
            ROTATION_180 -> {
                screenAx = -linearX
                screenAy = -linearY
            }
            ROTATION_270 -> {
                screenAx = linearY
                screenAy = -linearX
            }
            else -> {
                screenAx = linearX
                screenAy = linearY
            }
        }

        // Proyección combinada Y + Z para capturar frenado/aceleración con teléfono inclinado
        val effectiveAy = screenAy - Z_PROJECTION_FACTOR * linearZ

        // Filtro pasa-bajos etapa 2: suavizado contra vibraciones del vehículo/mano
        val alphaSmooth = dt / (tauSmoothSeconds + dt)
        filteredAx += alphaSmooth * (screenAx - filteredAx)
        filteredAy += alphaSmooth * (effectiveAy - filteredAy)

        // Zona muerta suave para evitar micro-temblores en reposo
        val magnitude = hypot(filteredAx, filteredAy)
        if (magnitude <= deadzoneMps2) {
            cleanAx = 0f
            cleanAy = 0f
        } else {
            val scale = (magnitude - deadzoneMps2) / magnitude
            cleanAx = filteredAx * scale
            cleanAy = filteredAy * scale
        }

        return isCurrentlyActive()
    }

    fun stepFrame(
        frameDtSeconds: Float,
        density: Float,
        wrapPeriodX: Float,
        wrapPeriodY: Float
    ): MotionCueKinematics {
        val safeDensity = density.coerceAtLeast(1f)
        val dt = frameDtSeconds.coerceIn(MIN_DT_SECONDS, MAX_FRAME_DT_SECONDS)
        val maxSpeedPx = MAX_SPEED_DP_PER_SEC * safeDensity

        // Dirección inercial opuesta a la aceleración del dispositivo:
        // Sensor +X (derecha) -> Canvas -X (izquierda)
        // Sensor +Y (arriba)  -> Canvas +Y (abajo)
        val targetVx = (-cleanAx * GAIN_DP_PER_SEC_PER_MPS2 * safeDensity)
            .coerceIn(-maxSpeedPx, maxSpeedPx)
        val targetVy = (cleanAy * GAIN_DP_PER_SEC_PER_MPS2 * safeDensity)
            .coerceIn(-maxSpeedPx, maxSpeedPx)

        val alphaVel = dt / (VELOCITY_TAU_SECONDS + dt)
        velocityX += alphaVel * (targetVx - velocityX)
        velocityY += alphaVel * (targetVy - velocityY)

        if (cleanAx == 0f && abs(velocityX) < REST_VELOCITY_PX_PER_SEC) {
            velocityX = 0f
        }
        if (cleanAy == 0f && abs(velocityY) < REST_VELOCITY_PX_PER_SEC) {
            velocityY = 0f
        }

        travelX = wrapModulo(travelX + velocityX * dt, wrapPeriodX)
        travelY = wrapModulo(travelY + velocityY * dt, wrapPeriodY)

        val maxSwayPx = MAX_SWAY_DP * safeDensity
        val targetSwayX = softBound(-cleanAx * SWAY_GAIN_DP_PER_MPS2 * safeDensity, maxSwayPx)
        val targetSwayY = softBound(cleanAy * SWAY_GAIN_DP_PER_MPS2 * safeDensity, maxSwayPx)

        val alphaSway = dt / (SWAY_TAU_SECONDS + dt)
        swayX += alphaSway * (targetSwayX - swayX)
        swayY += alphaSway * (targetSwayY - swayY)

        if (cleanAx == 0f && abs(swayX) < REST_SWAY_PX) swayX = 0f
        if (cleanAy == 0f && abs(swayY) < REST_SWAY_PX) swayY = 0f

        return MotionCueKinematics(
            travelX = travelX,
            travelY = travelY,
            swayX = swayX,
            swayY = swayY,
            velocityX = velocityX,
            velocityY = velocityY,
            isMoving = isCurrentlyActive()
        )
    }

    fun isCurrentlyActive(): Boolean {
        return abs(cleanAx) > 0f ||
            abs(cleanAy) > 0f ||
            abs(velocityX) >= REST_VELOCITY_PX_PER_SEC ||
            abs(velocityY) >= REST_VELOCITY_PX_PER_SEC ||
            abs(swayX) >= REST_SWAY_PX ||
            abs(swayY) >= REST_SWAY_PX
    }

    private fun wrapModulo(value: Float, period: Float): Float {
        if (period <= 1f || !period.isFinite()) return 0f
        val rem = value % period
        return if (rem < 0f) rem + period else rem
    }

    private fun softBound(value: Float, limit: Float): Float {
        if (limit <= 0f) return 0f
        return (value * limit) / (limit + abs(value))
    }

    companion object {
        const val ROTATION_0 = 0
        const val ROTATION_90 = 1
        const val ROTATION_180 = 2
        const val ROTATION_270 = 3

        private const val NANOS_TO_SECONDS = 1e-9f
        private const val MIN_DT_SECONDS = 0.001f
        private const val MAX_DT_SECONDS = 0.100f
        private const val MAX_FRAME_DT_SECONDS = 0.050f

        private const val Z_PROJECTION_FACTOR = 0.50f
        private const val GAIN_DP_PER_SEC_PER_MPS2 = 95f
        private const val MAX_SPEED_DP_PER_SEC = 420f
        private const val VELOCITY_TAU_SECONDS = 0.085f

        private const val SWAY_GAIN_DP_PER_MPS2 = 7.5f
        private const val MAX_SWAY_DP = 16f
        private const val SWAY_TAU_SECONDS = 0.14f

        private const val REST_VELOCITY_PX_PER_SEC = 0.45f
        private const val REST_SWAY_PX = 0.20f
    }
}
