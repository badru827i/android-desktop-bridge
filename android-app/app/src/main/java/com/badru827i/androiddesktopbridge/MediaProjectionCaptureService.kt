package com.badru827i.androiddesktopbridge

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaCodec
import android.media.MediaFormat
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Surface
import androidx.core.app.ServiceCompat
import java.nio.ByteBuffer

/**
 * MediaProjection -> VirtualDisplay -> runtime-selected video encoder.
 *
 * Codec selection is capability-driven; the selected codec is exposed to the
 * transport layer so ADBV packets can carry the correct codec ID.
 */
class MediaProjectionCaptureService : Service() {
    companion object {
        const val ACTION_START = "com.badru827i.androiddesktopbridge.action.START_CAPTURE"
        const val ACTION_STOP = "com.badru827i.androiddesktopbridge.action.STOP_CAPTURE"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        const val EXTRA_WIDTH = "width"
        const val EXTRA_HEIGHT = "height"
        const val EXTRA_FPS = "fps"
        const val EXTRA_BITRATE = "bitrate"
        const val EXTRA_PREFERRED_CODEC = "preferred_codec"

        private const val CHANNEL_ID = "projection"
        private const val NOTIFICATION_ID = 1101
        private const val TAG = "ADBridgeProjection"
    }

    private var projection: MediaProjection? = null
    private var virtualDisplay: android.hardware.display.VirtualDisplay? = null
    private var inputSurface: Surface? = null
    private var encoder: H264Encoder? = null
    private var selectedCodec: VideoCodecProfile? = null
    private var totalEncodedBytes = 0L
    private var totalFrames = 0L

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopCapture()
            ACTION_START -> startCapture(intent)
        }
        return START_NOT_STICKY
    }

    private fun startCapture(intent: Intent) {
        stopCapture()

        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1)
        val resultData = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
        if (resultCode < 0 || resultData == null) {
            Log.e(TAG, "Missing MediaProjection consent result")
            stopSelf()
            return
        }

        val config = ProjectionConfig(
            width = intent.getIntExtra(EXTRA_WIDTH, 1280),
            height = intent.getIntExtra(EXTRA_HEIGHT, 720),
            fps = intent.getIntExtra(EXTRA_FPS, 30),
            bitrate = intent.getIntExtra(EXTRA_BITRATE, 4_000_000),
            preferredCodec = intent.getStringExtra(EXTRA_PREFERRED_CODEC)
                ?.let { value -> VideoCodecProfile.entries.firstOrNull { it.name.equals(value, ignoreCase = true) } }
        )

        startAsForeground()

        try {
            val manager = getSystemService(MediaProjectionManager::class.java)
            projection = manager.getMediaProjection(resultCode, resultData)
            projection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.i(TAG, "MediaProjection stopped by system/user")
                    stopCapture()
                }
            }, mainExecutor)

            selectedCodec = VideoCodecSelector.select(
                width = config.width,
                height = config.height,
                fps = config.fps,
                preferred = config.preferredCodec
            )

            Log.i(TAG, "Selected codec: ${selectedCodec!!.name} (${selectedCodec!!.mimeType}), hardwareAvailable=${VideoCodecSelector.hardwareAvailable(selectedCodec!!)}")

            encoder = H264Encoder(config, object : H264Encoder.Listener {
                override fun onOutputFormat(format: MediaFormat) {
                    Log.i(TAG, "Encoder output format: $format codec=${selectedCodec?.name}")
                }

                override fun onEncodedData(data: ByteBuffer, info: MediaCodec.BufferInfo) {
                    totalEncodedBytes += info.size
                    if ((info.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
                        Log.d(TAG, "Key frame: ${info.size} bytes codec=${selectedCodec?.name}")
                    }
                    totalFrames++
                    // V1.2 transport wraps these access units with the selected codec ID.
                }

                override fun onEncoderError(error: Exception) {
                    Log.e(TAG, "Encoder error", error)
                    stopCapture()
                }
            }, selectedCodec!!)

            inputSurface = encoder!!.start()
            virtualDisplay = projection!!.createVirtualDisplay(
                "AndroidDesktopBridge-720p",
                config.width,
                config.height,
                resources.displayMetrics.densityDpi,
                android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                inputSurface,
                null,
                null
            )
            Log.i(TAG, "Capture started: ${config.width}x${config.height}@${config.fps}, ${config.bitrate}bps, codec=${selectedCodec!!.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start capture", e)
            stopCapture()
        }
    }

    private fun startAsForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= 29) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun stopCapture() {
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
        runCatching { encoder?.stop() }
        encoder = null
        inputSurface = null
        runCatching { projection?.stop() }
        projection = null
        selectedCodec = null
        totalEncodedBytes = 0
        totalFrames = 0
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Desktop Bridge capture", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun buildNotification(): Notification =
        if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setContentTitle("Android Desktop Bridge")
                .setContentText("Screen capture active • adaptive video codec")
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setContentTitle("Android Desktop Bridge")
                .setContentText("Screen capture active • adaptive video codec")
                .setOngoing(true)
                .build()
        }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopCapture()
        super.onDestroy()
    }
}
