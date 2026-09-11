package com.badru827i.androiddesktopbridge

/** Video codecs supported by the bridge wire protocol. */
enum class VideoCodecProfile(val mimeType: String, val wireId: Int) {
    H264(android.media.MediaFormat.MIMETYPE_VIDEO_AVC, 1),
    H265(android.media.MediaFormat.MIMETYPE_VIDEO_HEVC, 2),
    H266("video/vvc", 3)
}

/** Runtime selection: H.266 -> H.265 -> H.264, limited by actual device capability. */
object VideoCodecSelector {
    fun select(width: Int, height: Int, fps: Int, preferred: VideoCodecProfile? = null): VideoCodecProfile {
        val order = buildList {
            preferred?.let(::add)
            add(VideoCodecProfile.H266); add(VideoCodecProfile.H265); add(VideoCodecProfile.H264)
        }.distinct()
        return order.firstOrNull { isUsableEncoder(it, width, height, fps) }
            ?: error("No supported encoder for ${width}x${height}@${fps}fps")
    }

    fun isUsableEncoder(profile: VideoCodecProfile, width: Int, height: Int, fps: Int): Boolean {
        val list = android.media.MediaCodecList(android.media.MediaCodecList.REGULAR_CODECS)
        return list.codecInfos.asSequence().filter { it.isEncoder && !it.isAlias }
            .filter { info -> info.supportedTypes.any { it.equals(profile.mimeType, ignoreCase = true) } }
            .any { info ->
                val type = info.supportedTypes.first { it.equals(profile.mimeType, ignoreCase = true) }
                val video = runCatching { info.getCapabilitiesForType(type).videoCapabilities }.getOrNull() ?: return@any false
                video.areSizeAndRateSupported(width, height, fps.toDouble())
            }
    }

    fun hardwareAvailable(profile: VideoCodecProfile): Boolean {
        val list = android.media.MediaCodecList(android.media.MediaCodecList.REGULAR_CODECS)
        return list.codecInfos.asSequence().filter { it.isEncoder && !it.isAlias }
            .filter { it.supportedTypes.any { type -> type.equals(profile.mimeType, ignoreCase = true) } }
            .any { !it.isSoftwareOnly }
    }
}
