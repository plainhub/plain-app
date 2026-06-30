package com.ismartcoding.plain.services.screenmirror

/**
 * Pure H.264 bitstream helpers — no Android types so they're JVM-unit-testable.
 *
 * Wire format: every buffer the encoder pushes over WebSocket is Annex-B — each
 * NAL unit prefixed by the 4-byte start code `00 00 00 01`. The SPS + PPS
 * concatenated with start codes is the codec config the web client receives.
 */
object H264AnnexB {
    val START_CODE_4: ByteArray = byteArrayOf(0x00, 0x00, 0x00, 0x01)
    val START_CODE_3: ByteArray = byteArrayOf(0x00, 0x00, 0x01)

    /**
     * Convert an AVCC (length-prefixed) H.264 buffer to Annex-B (start-code
     * delimited). Some encoders (notably the ExynosC2 H.264 encoder on
     * Pixel) emit Annex-B bytes directly even when the buffer format is
     * nominally AVCC — `00 00 00 01` looks like a valid AVCC length prefix
     * (= 1, the start code itself) so we must sniff first or we silently
     * truncate to the first 5 bytes of every NAL.
     *
     * Sniff heuristic: a real AVCC length prefix is a sane NAL size, i.e.
     * > 0 and <= buffer size minus the 4-byte prefix. If the input starts
     * with `00 00 00 01` or `00 00 01` (Annex-B start code) we treat the
     * buffer as already Annex-B and return it unchanged.
     *
     * Format:
     *   AVCC:   `[u32 length][NAL unit][u32 length][NAL unit]…`
     *   Annex-B: `[00 00 00 01][NAL unit][00 00 00 01][NAL unit]…`
     *
     * Malformed input (length overruns the buffer) is truncated at the
     * last fully-readable NAL rather than throwing — the encoder
     * occasionally emits trailing garbage and we'd rather drop it than crash.
     */
    fun avccToAnnexB(avcc: ByteArray): ByteArray {
        if (avcc.size < 4) return avcc
        // Already Annex-B? Pass through.
        if (avcc[0] == 0.toByte() && avcc[1] == 0.toByte() &&
            ((avcc[2] == 0.toByte() && avcc[3] == 1.toByte()) ||
             avcc[2] == 1.toByte())
        ) return avcc
        val out = ArrayList<Byte>(avcc.size + 16)
        var off = 0
        while (off + 4 <= avcc.size) {
            val len = readUInt32BE(avcc, off)
            if (len <= 0 || off + 4 + len > avcc.size) break
            out.add(0x00); out.add(0x00); out.add(0x00); out.add(0x01)
            for (i in 0 until len) out.add(avcc[off + 4 + i])
            off += 4 + len
        }
        return out.toByteArray()
    }

    /**
     * Ensure the buffer starts with an Annex-B start code. MediaCodec's
     * `csd-0` / `csd-1` buffers sometimes come with the 4-byte start code
     * and sometimes without; we normalize so the resulting codec config is
     * always well-formed.
     */
    fun ensureStartCode(buf: ByteArray): ByteArray {
        if (buf.size >= 4 && buf[0] == 0.toByte() && buf[1] == 0.toByte() &&
            buf[2] == 0.toByte() && buf[3] == 1.toByte()
        ) return buf
        if (buf.size >= 3 && buf[0] == 0.toByte() && buf[1] == 0.toByte() &&
            buf[2] == 1.toByte()
        ) {
            val out = ByteArray(buf.size + 1)
            out[0] = 0; out[1] = 0; out[2] = 0; out[3] = 1
            System.arraycopy(buf, 3, out, 4, buf.size - 3)
            return out
        }
        val out = ByteArray(buf.size + 4)
        out[0] = 0; out[1] = 0; out[2] = 0; out[3] = 1
        System.arraycopy(buf, 0, out, 4, buf.size)
        return out
    }

    /**
     * Build the codec config blob passed to `VideoDecoder.configure({ description })`.
     * Just SPS + PPS in Annex-B, concatenated. The web client extracts the
     * profile/compat/level triplet from the SPS NAL header to derive the codec
     * string (`avc1.PPCCLL`).
     */
    fun joinSpsPps(sps: ByteArray, pps: ByteArray): ByteArray {
        val a = ensureStartCode(sps)
        val b = ensureStartCode(pps)
        val out = ByteArray(a.size + b.size)
        System.arraycopy(a, 0, out, 0, a.size)
        System.arraycopy(b, 0, out, a.size, b.size)
        return out
    }

    private fun readUInt32BE(buf: ByteArray, off: Int): Int {
        return ((buf[off].toInt() and 0xFF) shl 24) or
            ((buf[off + 1].toInt() and 0xFF) shl 16) or
            ((buf[off + 2].toInt() and 0xFF) shl 8) or
            (buf[off + 3].toInt() and 0xFF)
    }
}
