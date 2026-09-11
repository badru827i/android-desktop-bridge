package com.badru827i.androiddesktopbridge

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.view.Surface
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

/** Surface-input encoder using the codec selected at runtime. */
class H264Encoder(private val config: ProjectionConfig, private val listener: Listener, private val codecProfile: VideoCodecProfile = VideoCodecProfile.H264) {
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
        val codecName = findEncoder(codecProfile, config.width, config.height, config.fps)
        val format = MediaFormat.createVideoFormat(codecProfile.mimeType, config.width, config.height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, config.bitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, config.fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, config.iFrameIntervalSeconds)
            setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR)
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
            if (!running.get()) { runCatching { codec.releaseOutputBuffer(index, false) }; return }
            val buffer = codec.getOutputBuffer(index)
            if (buffer != null && info.size > 0) {
                val packet = buffer.duplicate().apply { position(info.offset); limit(info.offset + info.size) }.slice()
                listener.onEncodedData(packet, MediaCodec.BufferInfo().also { it.set(0, info.size, info.presentationTimeUs, info.flags) })
            }
            runCatching { codec.releaseOutputBuffer(index, false) }
        }
        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) = listener.onOutputFormat(format)
        override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) = listener.onEncoderError(e)
    }

    private fun findEncoder(profile: VideoCodecProfile, width: Int, height: Int, fps: Int): String {
        val list = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        return list.codecInfos.asSequence()
            .filter { it.isEncoder && !it.isAlias }
            .filter { it.supportedTypes.any { t -> t.equals(profile.mimeType, ignoreCase = true) } }
            .sortedWith(compareByDescending<MediaCodecInfo> { !it.isSoftwareOnly })
            .firstOrNull { info ->
                val type = info.supportedTypes.first { it.equals(profile.mimeType, ignoreCase = true) }
                val video = runCatching { info.getCapabilitiesForType(type).videoCapabilities }.getOrNull() ?: return@firstOrNull false
                video.areSizeAndRateSupported(width, height, fps.toDouble())
            }?.name ?: error("No ${profile.name} encoder supports ${width}x${height}@${fps}fps on this device")
    }
}
