package com.badru827i.androiddesktopbridge

/** Video codecs supported by the bridge protocol. */
enum class VideoCodecProfile(
    val mimeType: String,
    val wireId: Int
) {
    H264(android.media.MediaFormat.MIMETYPE_VIDEO_AVC, 1),
    H265(android.media.MediaFormat.MIMETYPE_VIDEO_HEVC, 2),
    H266("video/vvc", 3)
}

/** Runtime codec selection. H.266 is opportunistic because Android devices may not expose a VVC encoder. */
object VideoCodecSelector {
    fun select(
        width: Int,
        height: Int,
        fps: Int,
        preferred: VideoCodecProfile? = null
    ): VideoCodecProfile {
        val order = buildList {
            preferred?.let { add(it) }
            add(VideoCodecProfile.H266)
            add(VideoCodecProfile.H265)
            add(VideoCodecProfile.H264)
        }.distinct()

        return order.firstOrNull { isUsableEncoder(it, width, height, fps) }
            ?: error("No supported hardware/software video encoder for ${width}x${height}@${fps}fps")
    }

    fun isUsableEncoder(codecProfile: VideoCodecProfile, width: Int, height: Int, fps: Int): Boolean {
        val list = android.media.MediaCodecList(android.media.MediaCodecList.REGULAR_CODECS)
        return list.codecInfos.asSequence()
            .filter { it.isEncoder && !it.isAlias }
            .filter { it.supportedTypes.any { type -> type.equals(codecProfile.mimeType, ignoreCase = true) } }
            .any { info ->
                val type = info.supportedTypes.first { it.equals(codecProfile.mimeType, ignoreCase = true) }
                val caps = runCatching { info.getCapabilitiesForType(type) }.getOrNull() ?: return@any false
                val video = caps.videoCapabilities ?: return@any false
                video.isSizeSupported(width, height) &&
                    runCatching { video.getSupportedFrameRatesFor(width, height).contains(fps.toDouble()) }.getOrDefault(false)
            }
    }

    fun hardwareAvailable(codecProfile: VideoCodecProfile): Boolean {
        val list = android.media.MediaCodecList(android.media.MediaCodecList.REGULAR_CODECS)
        return list.codecInfos.asSequence()
            .filter { it.isEncoder && !it.isAlias }
            .filter { it.supportedTypes.any { type -> type.equals(codecProfile.mimeType, ignoreCase = true) } }
            .any { !it.isSoftwareOnly }
    }
}
