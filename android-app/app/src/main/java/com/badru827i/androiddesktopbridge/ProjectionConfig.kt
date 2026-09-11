package com.badru827i.androiddesktopbridge

/** Capture/encode settings used by MediaProjectionCaptureService. */
data class ProjectionConfig(
    val width: Int = 1280,
    val height: Int = 720,
    val fps: Int = 30,
    val bitrate: Int = 4_000_000,
    val iFrameIntervalSeconds: Int = 1
) {
    init {
        require(width > 0 && height > 0)
        require(fps in 1..60)
        require(bitrate > 0)
        require(iFrameIntervalSeconds > 0)
    }
}
