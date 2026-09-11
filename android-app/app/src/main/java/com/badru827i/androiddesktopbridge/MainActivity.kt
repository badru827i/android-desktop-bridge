package com.badru827i.androiddesktopbridge

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast

class MainActivity : Activity() {
    companion object {
        private const val REQUEST_MEDIA_PROJECTION = 7201
    }

    private lateinit var securityAuth: SecurityAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        securityAuth = SecurityAuthManager(this)
        setContentView(DesktopShellView(this))
    }

    /**
     * Authenticate the local user before requesting screen capture.
     * Android's system biometric UI may use fingerprint/face and can fall back
     * to the device PIN, pattern, or password. The app never sees the secret.
     */
    fun requestScreenCapture() {
        securityAuth.authenticate(
            onSuccess = { requestMediaProjectionConsent() },
            onFailure = { message ->
                Toast.makeText(this, "Security check failed: $message", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun requestMediaProjectionConsent() {
        val manager = getSystemService(MediaProjectionManager::class.java)
        startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION)
    }

    @Deprecated("Use Activity Result APIs when the UI is migrated")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_MEDIA_PROJECTION) return

        if (resultCode != RESULT_OK || data == null) {
            Toast.makeText(this, "Screen capture permission was not granted", Toast.LENGTH_SHORT).show()
            return
        }

        val serviceIntent = Intent(this, MediaProjectionCaptureService::class.java).apply {
            action = MediaProjectionCaptureService.ACTION_START
            putExtra(MediaProjectionCaptureService.EXTRA_RESULT_CODE, resultCode)
            putExtra(MediaProjectionCaptureService.EXTRA_RESULT_DATA, data)
            putExtra(MediaProjectionCaptureService.EXTRA_WIDTH, 1280)
            putExtra(MediaProjectionCaptureService.EXTRA_HEIGHT, 720)
            putExtra(MediaProjectionCaptureService.EXTRA_FPS, 30)
            putExtra(MediaProjectionCaptureService.EXTRA_BITRATE, 4_000_000)
        }

        if (android.os.Build.VERSION.SDK_INT >= 26) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        Toast.makeText(this, "Secure MediaProjection started • 720p", Toast.LENGTH_SHORT).show()
    }
}
