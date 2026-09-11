package com.badru827i.androiddesktopbridge

import java.nio.ByteBuffer
import java.nio.ByteOrder

data class AdbvPacket(val flags: Int,val sequence: Long,val timestampUs: Long,val width: Int,val height: Int,val fps: Int,val codecId: Int,val payload: ByteArray) {
 companion object {
  const val MAGIC="ADBV"; const val VERSION=1; const val HEADER_SIZE=32; const val FLAG_KEYFRAME=1; const val FLAG_CONFIG=2; const val FLAG_EOS=4
  fun encode(p: AdbvPacket): ByteArray {
   require(p.payload.size<=16*1024*1024); require(p.codecId in 1..3)
   val b=ByteBuffer.allocate(HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN)
   b.put(MAGIC.toByteArray(Charsets.US_ASCII)); b.put(VERSION.toByte()); b.put(p.flags.toByte()); b.putShort(HEADER_SIZE.toShort()); b.putInt(p.sequence.toInt()); b.putLong(p.timestampUs); b.putInt(p.payload.size); b.putShort(p.width.toShort()); b.putShort(p.height.toShort()); b.putShort(p.fps.toShort()); b.putShort(p.codecId.toShort())
   return b.array()+p.payload
  }
 }
}
