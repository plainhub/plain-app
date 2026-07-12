package com.ismartcoding.plain.lib.apk.struct.resource

import com.ismartcoding.plain.lib.apk.struct.ChunkHeader
import com.ismartcoding.plain.lib.apk.struct.ChunkType
import com.ismartcoding.plain.lib.apk.utils.Buffers
import com.ismartcoding.plain.lib.apk.utils.Unsigned
import java.nio.ByteBuffer

class ResourceTableHeader(headerSize: Int, chunkSize: Long, @JvmField val buffer: ByteBuffer) :
    ChunkHeader(ChunkType.TABLE, headerSize, chunkSize) {

    // The number of ResTable_package structures. uint32
    var packageCount = 0
        get() = Unsigned.toLong(field).toInt()

    init {
        packageCount = Unsigned.toUInt(Buffers.readUInt(buffer))
    }
}