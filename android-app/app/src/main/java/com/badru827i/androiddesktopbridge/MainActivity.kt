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

/**
 * Minimal bootstrap activity for device testing.
 * Keep startup free of optional subsystems so a runtime problem in capture,
 * biometrics, USB, or the desktop shell cannot prevent the app from opening.
 */
class MainActivity : Activity() {
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

        val status = TextView(this).apply {
            text = "V0.3 • Safe startup mode"
            textSize = 15f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }
        root.addView(status, ViewGroup.LayoutParams(-1, -2))

        val shellButton = Button(this).apply {
            text = "Open Desktop Shell"
            setOnClickListener {
                try {
                    val shell = Class.forName("com.badru827i.androiddesktopbridge.DesktopShellView")
                        .getConstructor(android.content.Context::class.java)
                        .newInstance(this@MainActivity) as android.view.View
                    setContentView(shell)
                } catch (t: Throwable) {
                    status.text = "Shell error: ${t.javaClass.simpleName}: ${t.message ?: "no message"}"
                }
            }
        }
        root.addView(shellButton, ViewGroup.LayoutParams(-2, -2))

        setContentView(root)
    }

    fun requestScreenCapture() {
        try {
            val manager = getSystemService(android.media.projection.MediaProjectionManager::class.java)
                ?: throw IllegalStateException("MediaProjection service unavailable")
            startActivityForResult(manager.createScreenCaptureIntent(), 7201)
        } catch (t: Throwable) {
            android.widget.Toast.makeText(
                this,
                "Capture error: ${t.javaClass.simpleName}",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    @Deprecated("Use Activity Result APIs when the capture flow is migrated")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 7201) return
        if (resultCode != RESULT_OK || data == null) return
        android.widget.Toast.makeText(this, "Capture permission granted", android.widget.Toast.LENGTH_SHORT).show()
    }
}
