package com.badru827i.androiddesktopbridge

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import kotlin.math.max

/**
 * Lightweight V1 desktop shell prototype.
 * It intentionally uses a single custom View: no heavy blur, no animations,
 * and no external UI framework.
 */
class DesktopShellView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val taskbar = RectF()
    private val launcher = RectF()
    private var launcherOpen = false
    private var windowOpen = false

    init {
        isFocusable = true
        paint.typeface = android.graphics.Typeface.create("sans", android.graphics.Typeface.NORMAL)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val barH = max(64f, h * 0.09f)

        canvas.drawColor(android.graphics.Color.rgb(8, 11, 16))
        paint.color = android.graphics.Color.rgb(15, 22, 31)
        canvas.drawRect(0f, 0f, w, h - barH, paint)

        paint.color = android.graphics.Color.rgb(22, 31, 42)
        canvas.drawCircle(w * 0.78f, h * 0.30f, h * 0.22f, paint)
        paint.color = android.graphics.Color.rgb(12, 18, 26)
        canvas.drawCircle(w * 0.18f, h * 0.40f, h * 0.18f, paint)

        paint.color = android.graphics.Color.WHITE
        paint.textSize = max(18f, h * 0.035f)
        canvas.drawText("Android Desktop Bridge", 28f, 42f, paint)
        paint.color = android.graphics.Color.LTGRAY
        paint.textSize = max(12f, h * 0.024f)
        canvas.drawText("V1.1 • 720p MediaProjection pipeline", 28f, 68f, paint)

        if (windowOpen) drawDemoWindow(canvas, w, h, barH)

        taskbar.set(0f, h - barH, w, h)
        paint.color = android.graphics.Color.rgb(10, 14, 20)
        canvas.drawRect(taskbar, paint)

        launcher.set(16f, h - barH + 12f, 150f, h - 12f)
        paint.color = android.graphics.Color.rgb(31, 42, 55)
        canvas.drawRoundRect(launcher, 14f, 14f, paint)
        paint.color = android.graphics.Color.WHITE
        paint.textSize = max(14f, barH * 0.28f)
        canvas.drawText("☰  Apps", 34f, h - barH / 2f + 6f, paint)

        paint.color = android.graphics.Color.LTGRAY
        paint.textSize = max(12f, barH * 0.23f)
        canvas.drawText("Capture: ready", w - 155f, h - barH / 2f + 5f, paint)

        if (launcherOpen) drawLauncher(canvas, w, h, barH)
    }

    private fun drawDemoWindow(canvas: Canvas, w: Float, h: Float, barH: Float) {
        val left = w * 0.12f
        val top = h * 0.16f
        val right = w * 0.88f
        val bottom = h - barH - h * 0.08f
        paint.color = android.graphics.Color.rgb(27, 35, 46)
        canvas.drawRoundRect(RectF(left, top, right, bottom), 18f, 18f, paint)
        paint.color = android.graphics.Color.rgb(18, 24, 32)
        canvas.drawRect(left, top, right, top + 54f, paint)
        paint.color = android.graphics.Color.WHITE
        paint.textSize = 18f
        canvas.drawText("Demo App", left + 22f, top + 34f, paint)
        paint.color = android.graphics.Color.LTGRAY
        paint.textSize = 14f
        canvas.drawText("Window manager prototype", left + 22f, top + 88f, paint)
        canvas.drawText("MediaProjection is available from Apps.", left + 22f, top + 116f, paint)
    }

    private fun drawLauncher(canvas: Canvas, w: Float, h: Float, barH: Float) {
        val panel = RectF(24f, 70f, minOf(w - 24f, 430f), h - barH - 20f)
        paint.color = android.graphics.Color.rgb(18, 24, 32)
        canvas.drawRoundRect(panel, 20f, 20f, paint)
        paint.color = android.graphics.Color.WHITE
        paint.textSize = 20f
        canvas.drawText("Apps", panel.left + 24f, panel.top + 38f, paint)
        paint.color = android.graphics.Color.LTGRAY
        paint.textSize = 15f
        canvas.drawText("Chrome", panel.left + 24f, panel.top + 84f, paint)
        canvas.drawText("Camera", panel.left + 24f, panel.top + 122f, paint)
        canvas.drawText("Settings", panel.left + 24f, panel.top + 160f, paint)
        paint.color = android.graphics.Color.WHITE
        canvas.drawText("Start screen capture", panel.left + 24f, panel.top + 198f, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP) return true
        val x = event.x
        val y = event.y
        val barH = max(64f, height * 0.09f)

        if (launcher.contains(x, y)) {
            launcherOpen = !launcherOpen
            invalidate()
            return true
        }

        if (launcherOpen && x < 430f) {
            val panelTop = 70f
            val captureRowTop = panelTop + 165f
            val captureRowBottom = panelTop + 225f
            if (y in captureRowTop..captureRowBottom) {
                launcherOpen = false
                val activity = context as? MainActivity
                if (activity != null) {
                    activity.requestScreenCapture()
                } else {
                    Toast.makeText(context, "Open screen capture from MainActivity", Toast.LENGTH_SHORT).show()
                }
                invalidate()
                return true
            }

            if (y in 95f..(height - barH - 20f)) {
                windowOpen = true
                launcherOpen = false
                Toast.makeText(context, "Demo window opened", Toast.LENGTH_SHORT).show()
                invalidate()
                return true
            }
        }
        return true
    }
}
