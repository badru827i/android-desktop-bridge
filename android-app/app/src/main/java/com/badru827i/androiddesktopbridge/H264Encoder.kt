package com.badru827i.androiddesktopbridge

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.view.Surface
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Surface-input H.264 encoder. Encoded access units are exposed to the
 * transport layer through [Listener]; this class does not perform USB I/O.
 */
class H264Encoder(
    private val config: ProjectionConfig,
    private val listener: Listener
) {
    interface Listener {
        fun onOutputFormat(format: MediaFormat)
        fun onEncodedData(data: ByteBuffer, info: MediaCodec.BufferInfo)
        fun onEncoderError(error: Exception)
    }

    private var codec: MediaCodec? = null
    private var inputSurface: Surface? = null
    private val running = AtomicBoolean(false)

    fun start(): Surface {
        check(!running.get()) { "Encoder already running" }

        val codecName = findH264Encoder(config.width, config.height, config.fps)
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, config.width, config.height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, config.bitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, config.fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, config.iFrameIntervalSeconds)
            if (android.os.Build.VERSION.SDK_INT >= 21) {
                setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR)
            }
        }

        val created = MediaCodec.createByCodecName(codecName)
        created.setCallback(callback)
        created.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        inputSurface = created.createInputSurface()
        created.start()
        codec = created
        running.set(true)
        return inputSurface!!
    }

    fun stop() {
        if (!running.getAndSet(false)) return
        val c = codec
        codec = null
        runCatching { c?.signalEndOfInputStream() }
        runCatching { c?.stop() }
        runCatching { c?.release() }
        inputSurface?.release()
        inputSurface = null
    }

    private val callback = object : MediaCodec.Callback() {
        override fun onInputBufferAvailable(codec: MediaCodec, index: Int) = Unit

        override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
            if (!running.get()) {
                runCatching { codec.releaseOutputBuffer(index, false) }
                return
            }
            val buffer = codec.getOutputBuffer(index)
            if (buffer != null && info.size > 0) {
                val packet = buffer.duplicate().apply {
                    position(info.offset)
                    limit(info.offset + info.size)
                }.slice()
                listener.onEncodedData(packet, MediaCodec.BufferInfo().also {
                    it.set(0, info.size, info.presentationTimeUs, info.flags)
                })
            }
            runCatching { codec.releaseOutputBuffer(index, false) }
        }

        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
            listener.onOutputFormat(format)
        }

        override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
            listener.onEncoderError(e)
        }
    }

    private fun findH264Encoder(width: Int, height: Int, fps: Int): String {
        val list = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        val candidates = list.codecInfos
            .asSequence()
            .filter { it.isEncoder }
            .filter { info ->
                info.supportedTypes.any { it.equals(MediaFormat.MIMETYPE_VIDEO_AVC, ignoreCase = true) }
            }
            .sortedWith(compareByDescending<MediaCodecInfo> { !it.isSoftwareOnly })

        for (info in candidates) {
            val type = info.supportedTypes.first { it.equals(MediaFormat.MIMETYPE_VIDEO_AVC, ignoreCase = true) }
            val caps = info.getCapabilitiesForType(type)
            val video = caps.videoCapabilities ?: continue
            if (video.isSizeSupported(width, height) && video.getSupportedFrameRatesFor(width, height).contains(fps.toDouble())) {
                return info.name
            }
        }
        error("No H.264 encoder supports ${width}x${height}@${fps}fps on this device")
    }
}
