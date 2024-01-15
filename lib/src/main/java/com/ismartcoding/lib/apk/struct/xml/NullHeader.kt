package com.ismartcoding.lib.apk.struct.xml

import com.ismartcoding.lib.apk.struct.ChunkHeader

class NullHeader(chunkType: Int, headerSize: Int, chunkSize: Long) :
    ChunkHeader(chunkType, headerSize, chunkSize)