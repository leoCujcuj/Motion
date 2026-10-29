package com.example.motion.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Display
import android.view.Surface
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.motion.MainActivity
import com.example.motion.R
import com.example.motion.data.MotionRepository
import com.example.motion.sensor.MotionPhysicsEngine
import com.example.motion.tile.MotionTileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MotionOverlayService : Service(), SensorEventListener {

    private lateinit var repository: MotionRepository
    private lateinit var sensorManager: SensorManager
    private var displayManager: DisplayManager? = null
    private var accelerometer: Sensor? = null

    private val physicsEngine = MotionPhysicsEngine()
    private lateinit var overlayController: MotionOverlayController
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var sensorRegistered = false
    private var screenReceiverRegistered = false
    private var currentDisplayRotation: Int = Surface.ROTATION_0

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    unregisterAccelerometer()
                    overlayController.pauseAnimation()
                }
                Intent.ACTION_SCREEN_ON -> {
                    physicsEngine.reset()
                    refreshDisplayRotation()
                    registerAccelerometer()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = MotionRepository.getInstance(this)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        displayManager = getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        overlayController = MotionOverlayController(this, physicsEngine)

        ensureNotificationChannel()

        serviceScope.launch {
            repository.settings.collect { updatedSettings ->
                overlayController.updateSettings(updatedSettings)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopOverlayAndSelf()
            return START_NOT_STICKY
        }

        if (!Settings.canDrawOverlays(this)) {
            repository.setRunning(false)
            MotionTileService.requestSync(this)
            stopSelf()
            return START_NOT_STICKY
        }

        if (accelerometer == null) {
            repository.setError(getString(R.string.error_no_accelerometer))
            MotionTileService.requestSync(this)
            stopSelf()
            return START_NOT_STICKY
        }

        val startedInForeground = promoteToForeground()
        if (!startedInForeground) {
            repository.setRunning(false)
            MotionTileService.requestSync(this)
            stopSelf()
            return START_NOT_STICKY
        }

        val attached = overlayController.attach(repository.settings.value)
        if (!attached) {
            repository.setRunning(false)
            MotionTileService.requestSync(this)
            stopSelf()
            return START_NOT_STICKY
        }

        refreshDisplayRotation()
        registerScreenReceiverIfNeeded()
        registerAccelerometer()

        repository.setRunning(true)
        MotionTileService.requestSync(this)

        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        refreshDisplayRotation()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
        val values = event.values
        if (values.size < 3) return

        val shouldAnimate = physicsEngine.onAccelerometerSample(
            rawX = values[0],
            rawY = values[1],
            rawZ = values[2],
            timestampNanos = event.timestamp,
            displayRotation = currentDisplayRotation
        )

        if (shouldAnimate) {
            overlayController.notifySensorUpdated()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onDestroy() {
        unregisterScreenReceiverIfNeeded()
        unregisterAccelerometer()
        overlayController.detach()
        serviceScope.cancel()
        repository.setRunning(false)
        MotionTileService.requestSync(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun stopOverlayAndSelf() {
        repository.setRunning(false)
        MotionTileService.requestSync(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun promoteToForeground(): Boolean {
        val notification = buildOngoingNotification()
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun buildOngoingNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            REQUEST_CODE_OPEN_APP,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = Intent(this, MotionOverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val pausePendingIntent = PendingIntent.getService(
            this,
            REQUEST_CODE_STOP_SERVICE,
            pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_qs_motion)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(
                R.drawable.ic_qs_motion,
                getString(R.string.notification_action_pause),
                pausePendingIntent
            )
            .build()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = getString(R.string.notification_channel_desc)
                    setShowBadge(false)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun registerAccelerometer() {
        if (sensorRegistered) return
        val targetSensor = accelerometer ?: return
        sensorRegistered = sensorManager.registerListener(
            this,
            targetSensor,
            SensorManager.SENSOR_DELAY_GAME
        )
    }

    private fun unregisterAccelerometer() {
        if (!sensorRegistered) return
        sensorManager.unregisterListener(this)
        sensorRegistered = false
    }

    private fun registerScreenReceiverIfNeeded() {
        if (screenReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        ContextCompat.registerReceiver(
            this,
            screenStateReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        screenReceiverRegistered = true
    }

    private fun unregisterScreenReceiverIfNeeded() {
        if (!screenReceiverRegistered) return
        try {
            unregisterReceiver(screenStateReceiver)
        } catch (_: Exception) {
            // Ignorar si ya fue desregistrado
        } finally {
            screenReceiverRegistered = false
        }
    }

    private fun refreshDisplayRotation() {
        currentDisplayRotation = displayManager
            ?.getDisplay(Display.DEFAULT_DISPLAY)
            ?.rotation
            ?: Surface.ROTATION_0
    }

    companion object {
        const val ACTION_START = "com.example.motion.action.START_OVERLAY"
        const val ACTION_STOP = "com.example.motion.action.STOP_OVERLAY"

        private const val CHANNEL_ID = "motion_overlay_channel"
        private const val NOTIFICATION_ID = 1001
        private const val REQUEST_CODE_OPEN_APP = 101
        private const val REQUEST_CODE_STOP_SERVICE = 102
    }
}
