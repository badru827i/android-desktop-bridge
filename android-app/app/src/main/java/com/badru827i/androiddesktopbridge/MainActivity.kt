package com.badru827i.androiddesktopbridge

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat

/** Simple launcher for the scrcpy-style USB mirror test. */
class MainActivity : Activity() {
    companion object { private const val REQUEST_MEDIA_PROJECTION = 7201 }

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
            setBackgroundColor(Color.rgb(8, 11, 16))
        }

        val title = TextView(this).apply {
            text = "Android Desktop Bridge"
            textSize = 24f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        root.addView(title, ViewGroup.LayoutParams(-1, -2))

        status = TextView(this).apply {
            text = "V0.4 • scrcpy-style USB mirror"
            textSize = 15f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 16)
        }
        root.addView(status, ViewGroup.LayoutParams(-1, -2))

        val mirrorButton = Button(this).apply {
            text = "Start USB Mirror"
            setOnClickListener { requestScreenCapture() }
        }
        root.addView(mirrorButton, ViewGroup.LayoutParams(-2, -2))

        val shellButton = Button(this).apply {
            text = "Open Desktop Shell"
            setOnClickListener {
                try {
                    val shell = DesktopShellView(this@MainActivity)
                    setContentView(shell)
                } catch (t: Throwable) {
                    status.text = "Shell error: ${t.javaClass.simpleName}"
                }
            }
        }
        root.addView(shellButton, ViewGroup.LayoutParams(-2, -2))

        val help = TextView(this).apply {
            text = "PC test: enable USB debugging, then run adb reverse tcp:27183 tcp:27183 and the ADBV receiver."
            textSize = 13f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
            setPadding(20, 24, 20, 0)
        }
        root.addView(help, ViewGroup.LayoutParams(-1, -2))

        setContentView(root)
    }

    private fun requestScreenCapture() {
        try {
            val manager = getSystemService(android.media.projection.MediaProjectionManager::class.java)
                ?: throw IllegalStateException("MediaProjection service unavailable")
            startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION)
        } catch (t: Throwable) {
            status.text = "Capture error: ${t.javaClass.simpleName}"
        }
    }

    @Deprecated("Use Activity Result APIs when the capture flow is migrated")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_MEDIA_PROJECTION || resultCode != RESULT_OK || data == null) {
            if (requestCode == REQUEST_MEDIA_PROJECTION) status.text = "Mirror cancelled"
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
            putExtra(MediaProjectionCaptureService.EXTRA_PREFERRED_CODEC, "H264")
        }
        ContextCompat.startForegroundService(this, serviceIntent)
        status.text = "USB mirror started • H.264 720p30 • waiting for PC receiver"
    }
}
