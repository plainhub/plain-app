package com.ismartcoding.plain.lib.apk.struct.xml

import com.ismartcoding.plain.lib.apk.struct.ChunkHeader
import com.ismartcoding.plain.lib.apk.utils.Buffers.readUInt
import com.ismartcoding.plain.lib.apk.utils.Buffers
import java.nio.ByteBuffer

class XmlNodeHeader(chunkType: Int, headerSize: Int, chunkSize: Long, buffer: ByteBuffer) :
    ChunkHeader(chunkType, headerSize, chunkSize) {
    /**
     * Line number in original source file at which this element appeared.
     */
    var lineNum = 0

    /**
     * Optional XML comment string pool ref, -1 if none
     */
    var commentRef = 0

    init {
        lineNum = Buffers.readUInt(buffer).toInt()
        commentRef = Buffers.readUInt(buffer).toInt()
    }
}