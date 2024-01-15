package com.ismartcoding.lib.apk.struct

import com.ismartcoding.lib.apk.utils.Unsigned

open class ChunkHeader(chunkType: Int, headerSize: Int, chunkSize: Long) {
    /**
     * Type identifier for this chunk.  The meaning of this value depends
     * on the containing chunk.
     */
    @JvmField
    val chunkType: Short

    /**
     * Size of the chunk header (in bytes).  Adding this value to
     * the address of the chunk allows you to find its associated data
     * (if any).
     */
    @JvmField
    val headerSize: Short

    /**
     * Total size of this chunk (in bytes).  This is the chunkSize plus
     * the size of any data associated with the chunk.  Adding this value
     * to the chunk allows you to completely skip its contents (including
     * any child chunks).  If this value is the same as chunkSize, there is
     * no data associated with the chunk.
     */
    val chunkSize: Int

    init {
        this.chunkType = Unsigned.toUShort(chunkType)
        this.headerSize = Unsigned.toUShort(headerSize)
        this.chunkSize = Unsigned.ensureUInt(chunkSize)
    }

    val bodySize: Int
        get() = chunkSize - headerSize
}