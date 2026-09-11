package com.badru827i.androiddesktopbridge

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build

/** Runtime USB capability detection.
 *
 * USB-C is a connector type, not a USB generation. Android may expose only a
 * coarse USB version, so this detector stays conservative instead of guessing.
 */
object UsbCapabilityDetector {
    fun detect(context: Context): List<UsbTransportProfile> {
        val manager = context.getSystemService(UsbManager::class.java)
            ?: return listOf(fallback())
        val devices = manager.deviceList.values
        if (devices.isEmpty()) return listOf(fallback())
        return devices.map { profileFor(it) }
    }

    private fun profileFor(device: UsbDevice): UsbTransportProfile {
        val generation = if (Build.VERSION.SDK_INT >= 31) {
            generationFromBcd(device.version)
        } else {
            UsbGeneration.UNKNOWN
        }
        val superspeed = device.interfaces.any { it.endpointCount > 0 }
        return UsbTransportProfile(
            generation = generation,
            role = UsbRole.HOST,
            superspeedCapable = superspeed
        )
    }

    private fun generationFromBcd(version: Int): UsbGeneration {
        val major = (version shr 8) and 0xff
        val minor = version and 0xff
        return when {
            major >= 4 -> UsbGeneration.USB4
            major >= 3 && minor >= 0x10 -> UsbGeneration.USB_3_1_GEN2
            major >= 3 -> UsbGeneration.USB_3_0
            major == 2 -> UsbGeneration.USB_2_0
            else -> UsbGeneration.UNKNOWN
        }
    }

    private fun fallback() = UsbTransportProfile(
        generation = UsbGeneration.UNKNOWN,
        role = UsbRole.UNKNOWN,
        superspeedCapable = false
    )
}
