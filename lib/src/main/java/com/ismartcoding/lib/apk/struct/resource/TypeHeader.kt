package com.ismartcoding.lib.apk.struct.resource

import com.ismartcoding.lib.apk.struct.ChunkHeader
import com.ismartcoding.lib.apk.struct.ChunkType
import com.ismartcoding.lib.apk.utils.Buffers.readBytes
import com.ismartcoding.lib.apk.utils.Buffers.readUByte
import com.ismartcoding.lib.apk.utils.Buffers.readUInt
import com.ismartcoding.lib.apk.utils.Buffers.readUShort
import com.ismartcoding.lib.apk.utils.Buffers.skip
import com.ismartcoding.lib.apk.utils.Unsigned
import java.nio.ByteBuffer

class TypeHeader(headerSize: Int, chunkSize: Long, buffer: ByteBuffer) :
    ChunkHeader(ChunkType.TABLE_TYPE, headerSize, chunkSize) {
    // The type identifier this chunk is holding.  Type IDs start at 1 (corresponding to the value
    // of the type bits in a resource identifier).  0 is invalid.
    // uint8_t
    var id: Byte = 0

    // Must be 0. uint8_t
    var res0: Byte = 0

    // Must be 0. uint16_t
    var res1: Short = 0

    // Number of uint32_t entry indices that follow. uint32
    var entryCount = 0

    // Offset from header where ResTable_entry data starts.uint32_t
    var entriesStart = 0

    // Configuration this collection of entries is designed for.
    val config: ResTableConfig

    init {
        id = Unsigned.toUByte(readUByte(buffer));
        res0 = Unsigned.toUByte(readUByte(buffer));
        res1 = Unsigned.toUShort(readUShort(buffer));
        entryCount = Unsigned.ensureUInt(readUInt(buffer));
        entriesStart = Unsigned.ensureUInt(readUInt(buffer));
        config = this.readResTableConfig(buffer);
    }

    private fun readResTableConfig(buffer: ByteBuffer): ResTableConfig {
        val beginPos = buffer.position().toLong()
        val config = ResTableConfig()
        val size = readUInt(buffer)
        // imsi
        config.mcc = buffer.short
        config.mnc = buffer.short
        // read locale
        config.language = String(readBytes(buffer, 2)).replace("\u0000", "")
        config.country = String(readBytes(buffer, 2)).replace("\u0000", "")
        // screen type
        config.orientation = readUByte(buffer).toByte()
        config.touchscreen = readUByte(buffer).toByte()
        config.density = readUShort(buffer).toShort()
        // now just skip the others...
        val endPos = buffer.position().toLong()
        skip(buffer, (size - (endPos - beginPos)).toInt())
        return config
    }


    companion object {
        const val NO_ENTRY = 0xFFFFFFFFL
    }
}