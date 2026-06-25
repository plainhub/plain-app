package com.ismartcoding.plain.lib.apk.struct.resource

import com.ismartcoding.plain.lib.apk.struct.ChunkHeader
import com.ismartcoding.plain.lib.apk.struct.ChunkType
import com.ismartcoding.plain.lib.apk.utils.Buffers
import com.ismartcoding.plain.lib.apk.utils.Unsigned.ensureUInt
import com.ismartcoding.plain.lib.apk.utils.Unsigned
import java.nio.ByteBuffer

class LibraryHeader(headerSize: Int, chunkSize: Long, buffer: ByteBuffer) :
    ChunkHeader(ChunkType.TABLE_LIBRARY, headerSize, chunkSize) {
    /**
     * A package-id to package name mapping for any shared libraries used
     * in this resource table. The package-id's encoded in this resource
     * table may be different than the id's assigned at runtime. We must
     * be able to translate the package-id's based on the package name.
     */
    /**
     * uint32 value, The number of shared libraries linked in this resource table.
     */
    val count: Int

    init {
        count = Unsigned.ensureUInt(Buffers.readUInt(buffer))
    }
}