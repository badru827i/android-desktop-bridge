package com.badru827i.androiddesktopbridge

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    companion object { private const val REQUEST_MEDIA_PROJECTION = 7201 }
    private lateinit var securityAuth: SecurityAuthManager
    private lateinit var deviceProfile: DeviceProfile

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        try {
            securityAuth = SecurityAuthManager(this)
            deviceProfile = DeviceProfiler.inspect(this)
            setContentView(DesktopShellView(this))
        } catch (t: Throwable) {
            showStartupError(t)
        }
    }

    private fun showStartupError(error: Throwable) {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.rgb(8, 11, 16))
        }
        val title = TextView(this).apply {
            text = "Android Desktop Bridge\nStartup error"
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        val details = TextView(this).apply {
            text = "\\n${error.javaClass.name}: ${error.message ?: "no message"}\\n\\nThis screen is only for debugging the V1 build."
            textSize = 14f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }
        root.addView(title)
        root.addView(details)
        setContentView(root)
    }

    fun requestScreenCapture() {
        securityAuth.authenticate(
            onSuccess = { requestMediaProjectionConsent() },
            onFailure = { message -> Toast.makeText(this, "Security check failed: $message", Toast.LENGTH_SHORT).show() }
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

        val p = deviceProfile.recommended
        val serviceIntent = Intent(this, MediaProjectionCaptureService::class.java).apply {
            action = MediaProjectionCaptureService.ACTION_START
            putExtra(MediaProjectionCaptureService.EXTRA_RESULT_CODE, resultCode)
            putExtra(MediaProjectionCaptureService.EXTRA_RESULT_DATA, data)
            putExtra(MediaProjectionCaptureService.EXTRA_WIDTH, p.width)
            putExtra(MediaProjectionCaptureService.EXTRA_HEIGHT, p.height)
            putExtra(MediaProjectionCaptureService.EXTRA_FPS, p.fps)
            putExtra(MediaProjectionCaptureService.EXTRA_BITRATE, p.bitrate)
        }
        if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(serviceIntent) else startService(serviceIntent)
        Toast.makeText(this, "Secure capture: ${p.label} • ${p.width}x${p.height}@${p.fps}", Toast.LENGTH_SHORT).show()
    }
}
