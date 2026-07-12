package com.ismartcoding.plain.lib.apk.struct.resource

import com.ismartcoding.plain.lib.apk.struct.ChunkHeader
import com.ismartcoding.plain.lib.apk.struct.ChunkType

class NullHeader(headerSize: Int, chunkSize: Long) :
    ChunkHeader(ChunkType.NULL, headerSize, chunkSize)