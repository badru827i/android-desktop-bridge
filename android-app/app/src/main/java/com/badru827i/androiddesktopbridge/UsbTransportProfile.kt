package com.badru827i.androiddesktopbridge

/**
 * USB capability model for Android Desktop Bridge.
 *
 * The app does not assume that USB-C means a particular USB generation.
 * Capability is detected at runtime and the lowest supported link capability
 * becomes the effective transport ceiling.
 */
enum class UsbGeneration(val label: String, val nominalMbps: Int) {
    USB_2_0("USB 2.0", 480),
    USB_3_0("USB 3.0", 5000),
    USB_3_1_GEN2("USB 3.1 Gen 2", 10000),
    USB_3_2_GEN2X2("USB 3.2 Gen 2x2", 20000),
    USB4("USB4", 40000),
    UNKNOWN("Unknown", 0)
}

enum class UsbRole {
    HOST,
    DEVICE,
    UNKNOWN
}

data class UsbTransportProfile(
    val generation: UsbGeneration,
    val role: UsbRole,
    val superspeedCapable: Boolean,
    val maxPayloadBytes: Int = 1024 * 1024
) {
    /** Conservative video bitrate ceiling for the detected link. */
    fun recommendedVideoBitrate(): Int = when (generation) {
        UsbGeneration.USB_2_0 -> 8_000_000
        UsbGeneration.USB_3_0 -> 20_000_000
        UsbGeneration.USB_3_1_GEN2 -> 40_000_000
        UsbGeneration.USB_3_2_GEN2X2 -> 60_000_000
        UsbGeneration.USB4 -> 80_000_000
        UsbGeneration.UNKNOWN -> 6_000_000
    }

    fun description(): String =
        "${generation.label} • role=$role • superspeed=$superspeedCapable"
}
