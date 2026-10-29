package com.example.motion

import com.example.motion.model.CueShape
import com.example.motion.model.MotionSettings
import com.example.motion.model.ThemeMode
import com.example.motion.sensor.MotionPhysicsEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionPhysicsEngineTest {

    @Test
    fun stationaryDeviceWithGravityRemainsAtRest() {
        val engine = MotionPhysicsEngine()
        var timestamp = 1_000_000_000L

        repeat(20) {
            val active = engine.onAccelerometerSample(
                rawX = 0f,
                rawY = 0f,
                rawZ = 9.81f,
                timestampNanos = timestamp,
                displayRotation = MotionPhysicsEngine.ROTATION_0
            )
            assertFalse(active)
            timestamp += 20_000_000L // 20 ms
        }

        val kinematics = engine.stepFrame(
            frameDtSeconds = 0.016f,
            density = 3f,
            wrapPeriodX = 200f,
            wrapPeriodY = 200f
        )
        assertFalse(kinematics.isMoving)
        assertEquals(0f, kinematics.velocityX, 0.001f)
        assertEquals(0f, kinematics.velocityY, 0.001f)
    }

    @Test
    fun linearAccelerationProducesOppositeInertialVelocity() {
        val engine = MotionPhysicsEngine()
        var timestamp = 1_000_000_000L

        // Inicialización en reposo
        engine.onAccelerometerSample(0f, 0f, 9.81f, timestamp)

        // Aceleración sostenida hacia +X (derecha) y +Y (arriba)
        repeat(8) {
            timestamp += 20_000_000L
            engine.onAccelerometerSample(
                rawX = 2.5f,
                rawY = 3.0f,
                rawZ = 9.81f,
                timestampNanos = timestamp,
                displayRotation = MotionPhysicsEngine.ROTATION_0
            )
        }

        val state = engine.stepFrame(
            frameDtSeconds = 0.016f,
            density = 3f,
            wrapPeriodX = 300f,
            wrapPeriodY = 300f
        )

        assertTrue(state.isMoving)
        // +X sensor -> -X en Canvas (izquierda)
        assertTrue(state.velocityX < 0f)
        // +Y sensor -> +Y en Canvas (abajo)
        assertTrue(state.velocityY > 0f)
    }

    @Test
    fun landscapeRotationRemapsSensorAxesAccurately() {
        val engine = MotionPhysicsEngine()
        var timestamp = 1_000_000_000L
        engine.onAccelerometerSample(0f, 0f, 9.81f, timestamp, MotionPhysicsEngine.ROTATION_90)

        repeat(6) {
            timestamp += 20_000_000L
            engine.onAccelerometerSample(
                rawX = 2.8f,
                rawY = 0f,
                rawZ = 9.81f,
                timestampNanos = timestamp,
                displayRotation = MotionPhysicsEngine.ROTATION_90
            )
        }

        // En ROTATION_90, +X físico mapea a +Y de pantalla
        assertTrue(engine.filteredAy > 0.5f)
    }

    @Test
    fun settingsValidationClampsBoundsAndShapes() {
        val clampedSmall = MotionSettings(sizeDp = 0.5f).validated()
        val clampedLarge = MotionSettings(sizeDp = 99f).validated()

        assertEquals(MotionSettings.MIN_SIZE_DP, clampedSmall.sizeDp, 0.001f)
        assertEquals(MotionSettings.MAX_SIZE_DP, clampedLarge.sizeDp, 0.001f)
        assertEquals(CueShape.SUBTLE_LINE, CueShape.fromStorageKey("subtle_line"))
        assertEquals(CueShape.SQUARE, CueShape.fromStorageKey("square"))
        assertEquals(CueShape.CIRCLE, CueShape.fromStorageKey("unknown"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorageKey("unknown"))
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromStorageKey("light"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromStorageKey("dark"))
        assertTrue(MotionSettings().showMotionTrail)
        assertFalse(MotionSettings(showMotionTrail = false).validated().showMotionTrail)
    }
}
