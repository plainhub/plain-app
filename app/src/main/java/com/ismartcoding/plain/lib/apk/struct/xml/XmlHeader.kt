package com.ismartcoding.plain.lib.apk.struct.xml

import com.ismartcoding.plain.lib.apk.struct.ChunkHeader

class XmlHeader(chunkType: Int, headerSize: Int, chunkSize: Long) :
    ChunkHeader(chunkType, headerSize, chunkSize)