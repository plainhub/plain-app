package com.ismartcoding.plain.lib.screenmirror

/**
 * Binary video frame protocol — wraps raw H.264/Opus NAL data with metadata
 * needed for loss detection, error recovery, and A/V sync.
 *
 * Wire format (14-byte header + payload, big-endian):
 *   offset  size  field
 *   0       1     magic     0x56 ('V')
 *   1       1     flags     bit0: isKeyFrame
 *                           bit1: isConfig (SPS/PPS)
 *                           bit2: isAudio
 *   2       4     frameId   uint32, monotonic per-stream
 *   6       8     timestamp int64, PTS in microseconds
 *   14      N     data      NAL / Opus bytes
 *
 * Lives in commonMain so unit tests run on JVM without Android deps.
 */
object VideoPacket {
    const val MAGIC: Byte = 0x56
    const val HEADER_SIZE = 14

    const val FLAG_KEY_FRAME: Byte = 0x01
    const val FLAG_CONFIG: Byte = 0x02
    const val FLAG_AUDIO: Byte = 0x04

    fun encode(
        frameId: Int,
        timestamp: Long,
        flags: Byte,
        data: ByteArray,
    ): ByteArray {
        val out = ByteArray(HEADER_SIZE + data.size)
        out[0] = MAGIC
        out[1] = flags
        out[2] = ((frameId ushr 24) and 0xFF).toByte()
        out[3] = ((frameId ushr 16) and 0xFF).toByte()
        out[4] = ((frameId ushr 8) and 0xFF).toByte()
        out[5] = (frameId and 0xFF).toByte()
        var ts = timestamp
        for (i in 7 downTo 0) {
            out[6 + i] = (ts and 0xFF).toByte()
            ts = ts ushr 8
        }
        data.copyInto(out, HEADER_SIZE)
        return out
    }
}
