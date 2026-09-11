package com.badru827i.androiddesktopbridge

import android.app.ActivityManager
import android.content.Context
import android.os.Build

/** Device-aware capture profile. Never requests a resolution above the selected target without validation. */
enum class PerformanceProfile(val label: String, val width: Int, val height: Int, val fps: Int, val bitrate: Int) {
    LITE("Lite", 854, 480, 30, 2_000_000),
    BALANCED("Balanced", 1280, 720, 30, 4_000_000),
    PERFORMANCE("Performance", 1280, 720, 60, 7_000_000)
}

data class DeviceProfile(
    val totalRamMb: Long,
    val lowMemory: Boolean,
    val recommended: PerformanceProfile
)

object DeviceProfiler {
    fun inspect(context: Context): DeviceProfile {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        val info = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(info)
        val ramMb = info.totalMem / (1024 * 1024)

        val recommended = when {
            info.lowMemory || ramMb < 4_000 -> PerformanceProfile.LITE
            ramMb >= 8_000 && Build.VERSION.SDK_INT >= 30 -> PerformanceProfile.PERFORMANCE
            else -> PerformanceProfile.BALANCED
        }
        return DeviceProfile(ramMb, info.lowMemory, recommended)
    }

    fun clampFps(requested: Int): Int = requested.coerceIn(15, 60)
}
