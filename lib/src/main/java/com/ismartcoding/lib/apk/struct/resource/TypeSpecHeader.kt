package com.ismartcoding.lib.apk.struct.resource

import com.ismartcoding.lib.apk.struct.ChunkHeader
import com.ismartcoding.lib.apk.struct.ChunkType
import com.ismartcoding.lib.apk.utils.Buffers
import com.ismartcoding.lib.apk.utils.Unsigned
import java.nio.ByteBuffer

class TypeSpecHeader(headerSize: Int, chunkSize: Long, buffer: ByteBuffer) :
    ChunkHeader(ChunkType.TABLE_TYPE_SPEC, headerSize, chunkSize) {
    /**
     * The type identifier this chunk is holding.  Type IDs start at 1 (corresponding to the value
     * of the type bits in a resource identifier).  0 is invalid.
     * The id also specifies the name of the Resource type. It is the string at index id - 1 in the
     * typeStrings StringPool chunk in the containing Package chunk.
     * uint8_t
     */
    var id: Byte = 0

    /**
     * Must be 0. uint8_t
     */
    var res0: Byte = 0

    /**
     * Must be 0.uint16_t
     */
    var res1: Short = 0

    /**
     * Number of uint32_t entry configuration masks that follow.
     */
    var entryCount = 0

    init {
        id = Unsigned.toUByte(Buffers.readUByte(buffer));
        res0 = Unsigned.toUByte(Buffers.readUByte(buffer));
        res1 = Unsigned.toUShort(Buffers.readUShort(buffer));
        entryCount = Unsigned.ensureUInt(Buffers.readUInt(buffer));
    }
}