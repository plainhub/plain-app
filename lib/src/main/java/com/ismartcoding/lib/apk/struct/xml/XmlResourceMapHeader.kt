package com.ismartcoding.lib.apk.struct.xml

import com.ismartcoding.lib.apk.struct.ChunkHeader

class XmlResourceMapHeader(chunkType: Int, headerSize: Int, chunkSize: Long) :
    ChunkHeader(chunkType, headerSize, chunkSize)