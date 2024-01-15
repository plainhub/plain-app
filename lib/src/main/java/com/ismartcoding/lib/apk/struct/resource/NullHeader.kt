package com.ismartcoding.lib.apk.struct.resource

import com.ismartcoding.lib.apk.struct.ChunkHeader
import com.ismartcoding.lib.apk.struct.ChunkType

class NullHeader(headerSize: Int, chunkSize: Long) :
    ChunkHeader(ChunkType.NULL, headerSize, chunkSize)