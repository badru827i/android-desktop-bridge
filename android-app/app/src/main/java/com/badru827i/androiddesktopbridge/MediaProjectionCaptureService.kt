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
import android.os.Handler
import android.os.Looper
import android.os.IBinder
import android.util.Log
import android.view.Surface
import androidx.core.app.ServiceCompat
import java.nio.ByteBuffer

/** MediaProjection capture service with an ADB-reversed TCP video path. */
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

    private val projectionCallbackHandler = Handler(Looper.getMainLooper())
    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            Log.i(TAG, "MediaProjection stopped by system/user")
            stopCapture()
        }
    }

    private var projection: MediaProjection? = null
    private var virtualDisplay: android.hardware.display.VirtualDisplay? = null
    private var inputSurface: Surface? = null
    private var encoder: H264Encoder? = null
    private var sender: TcpAdbvSender? = null

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
            preferredCodec = VideoCodecProfile.H264
        )

        startAsForeground()
        try {
            val manager = getSystemService(MediaProjectionManager::class.java)
            projection = manager.getMediaProjection(resultCode, resultData)
            projection?.registerCallback(projectionCallback, projectionCallbackHandler)
            sender = TcpAdbvSender()

            encoder = H264Encoder(config, object : H264Encoder.Listener {
                override fun onOutputFormat(format: MediaFormat) {
                    Log.i(TAG, "Encoder output format: $format codec=H264")
                }

                override fun onEncodedData(data: ByteBuffer, info: MediaCodec.BufferInfo) {
                    if (info.size <= 0) return
                    val flags = when {
                        (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0 -> AdbvPacket.FLAG_CONFIG
                        (info.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0 -> AdbvPacket.FLAG_KEYFRAME
                        else -> 0
                    }
                    sender?.offer(
                        flags = flags,
                        timestampUs = info.presentationTimeUs,
                        width = config.width,
                        height = config.height,
                        fps = config.fps,
                        codecId = 1,
                        data = data,
                        offset = info.offset,
                        size = info.size
                    )
                }

                override fun onEncoderError(error: Exception) {
                    Log.e(TAG, "Encoder error", error)
                    stopCapture()
                }
            }, VideoCodecProfile.H264)

            inputSurface = encoder!!.start()
            virtualDisplay = projection!!.createVirtualDisplay(
                "AndroidDesktopBridge-USB-Mirror",
                config.width,
                config.height,
                resources.displayMetrics.densityDpi,
                android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                inputSurface,
                null,
                null
            )
            Log.i(TAG, "Capture started: ${config.width}x${config.height}@${config.fps}, ADBV TCP 27183")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start capture", e)
            stopCapture()
        }
    }

    private fun startAsForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= 29) {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
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
        sender?.close()
        sender = null
        runCatching { projection?.unregisterCallback(projectionCallback) }
        runCatching { projection?.stop() }
        projection = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Desktop Bridge capture", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun buildNotification(): Notification = if (Build.VERSION.SDK_INT >= 26) {
        Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle("Android Desktop Bridge")
            .setContentText("USB mirror active • H.264 ADBV")
            .setOngoing(true)
            .build()
    } else {
        @Suppress("DEPRECATION")
        Notification.Builder(this)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle("Android Desktop Bridge")
            .setContentText("USB mirror active • H.264 ADBV")
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopCapture()
        super.onDestroy()
    }
}
