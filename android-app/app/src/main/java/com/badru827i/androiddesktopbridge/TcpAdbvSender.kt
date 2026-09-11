package com.badru827i.androiddesktopbridge

import java.io.BufferedOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

/** Sends ADBV packets to a desktop receiver through an ADB-reversed TCP port. */
class TcpAdbvSender(
    private val host: String = "127.0.0.1",
    private val port: Int = 27183
) : AutoCloseable {
    private val queue = LinkedBlockingQueue<ByteArray>(12)
    private val running = AtomicBoolean(true)
    private val worker = Thread({ runWriter() }, "adbridge-tcp-writer").apply { start() }
    private var sequence = 0L

    fun offer(flags: Int, timestampUs: Long, width: Int, height: Int, fps: Int, codecId: Int, data: ByteBuffer, offset: Int, size: Int) {
        if (!running.get() || size <= 0) return
        val payload = ByteArray(size)
        val copy = data.duplicate()
        copy.position(offset)
        copy.limit(offset + size)
        copy.get(payload)
        val packet = AdbvPacket(flags, sequence++, timestampUs, width, height, fps, codecId, payload)
        val encoded = AdbvPacket.encode(packet)
        if (!queue.offer(encoded)) {
            queue.poll()
            queue.offer(encoded)
        }
    }

    private fun runWriter() {
        var socket: Socket? = null
        var output: BufferedOutputStream? = null
        try {
            while (running.get()) {
                val packet = queue.take()
                if (socket == null || socket.isClosed || !socket.isConnected) {
                    runCatching { socket?.close() }
                    socket = runCatching {
                        Socket().apply {
                            tcpNoDelay = true
                            connect(InetSocketAddress(host, port), 1500)
                        }
                    }.getOrNull()
                    output = socket?.getOutputStream()?.let(::BufferedOutputStream)
                }
                val out = output ?: continue
                try {
                    out.write(packet)
                    out.flush()
                } catch (_: Exception) {
                    runCatching { socket?.close() }
                    socket = null
                    output = null
                }
            }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        } finally {
            runCatching { output?.close() }
            runCatching { socket?.close() }
        }
    }

    override fun close() {
        if (running.compareAndSet(true, false)) {
            worker.interrupt()
            queue.clear()
        }
    }
}
