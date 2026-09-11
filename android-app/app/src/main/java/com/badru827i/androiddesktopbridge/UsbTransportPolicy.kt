package com.badru827i.androiddesktopbridge

/**
 * Chooses a safe transport profile from the detected USB capability.
 * The protocol stays identical across USB generations; only bitrate and
 * buffering are adapted to the effective link.
 */
object UsbTransportPolicy {
    fun choose(profile: UsbTransportProfile): UsbTransportSettings {
        val bitrate = profile.recommendedVideoBitrate()
        return when (profile.generation) {
            UsbGeneration.USB_2_0 -> UsbTransportSettings(bitrate, 64 * 1024, 2)
            UsbGeneration.USB_3_0 -> UsbTransportSettings(bitrate, 256 * 1024, 3)
            UsbGeneration.USB_3_1_GEN2 -> UsbTransportSettings(bitrate, 512 * 1024, 3)
            UsbGeneration.USB_3_2_GEN2X2 -> UsbTransportSettings(bitrate, 1024 * 1024, 4)
            UsbGeneration.USB4 -> UsbTransportSettings(bitrate, 1024 * 1024, 4)
            UsbGeneration.UNKNOWN -> UsbTransportSettings(bitrate, 64 * 1024, 2)
        }
    }
}

data class UsbTransportSettings(
    val videoBitrate: Int,
    val chunkSizeBytes: Int,
    val bufferCount: Int
)
