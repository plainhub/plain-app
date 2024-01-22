package com.ismartcoding.lib.apk.parser

import com.ismartcoding.lib.apk.exception.ParserException
import com.ismartcoding.lib.apk.struct.ChunkHeader
import com.ismartcoding.lib.apk.struct.ChunkType
import com.ismartcoding.lib.apk.struct.StringPool
import com.ismartcoding.lib.apk.struct.StringPoolHeader
import com.ismartcoding.lib.apk.struct.resource.ResourceTable
import com.ismartcoding.lib.apk.struct.xml.Attribute
import com.ismartcoding.lib.apk.struct.xml.Attributes
import com.ismartcoding.lib.apk.struct.xml.NullHeader
import com.ismartcoding.lib.apk.struct.xml.XmlCData
import com.ismartcoding.lib.apk.struct.xml.XmlHeader
import com.ismartcoding.lib.apk.struct.xml.XmlNamespaceEndTag
import com.ismartcoding.lib.apk.struct.xml.XmlNamespaceStartTag
import com.ismartcoding.lib.apk.struct.xml.XmlNodeEndTag
import com.ismartcoding.lib.apk.struct.xml.XmlNodeHeader
import com.ismartcoding.lib.apk.struct.xml.XmlNodeStartTag
import com.ismartcoding.lib.apk.struct.xml.XmlResourceMapHeader
import com.ismartcoding.lib.apk.utils.Buffers
import com.ismartcoding.lib.apk.utils.Buffers.readUShort
import com.ismartcoding.lib.apk.utils.Locales
import com.ismartcoding.lib.apk.utils.ParseUtils
import com.ismartcoding.lib.apk.utils.ParseUtils.readResValue
import com.ismartcoding.lib.apk.utils.Strings
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale


class BinaryXmlParser(
    buffer: ByteBuffer,
    @JvmField val resourceTable: ResourceTable,
    @JvmField val xmlStreamer: XmlStreamer,
    locale: Locale
) {
    /**
     * By default the data buffer Chunks is buffer little-endian byte order both at runtime and when stored buffer
     * files.
     */
    private val byteOrder = ByteOrder.LITTLE_ENDIAN
    private var stringPool: StringPool? = null

    // some attribute name stored by resource id
    private var resourceMap: Array<String?>? = null

    private val buffer: ByteBuffer

    /**
     * default locale.
     */
    private val locale: Locale

    /**
     * Parse binary xml.
     */
    fun parse() {
        val firstChunkHeader = readChunkHeader() ?: return
        when (firstChunkHeader.chunkType) {
            ChunkType.XML.toShort(), ChunkType.NULL.toShort() -> {}
            ChunkType.STRING_POOL.toShort() -> {}
            else -> {}
        }

        // read string pool chunk
        val stringPoolChunkHeader = readChunkHeader() ?: return
        ParseUtils.checkChunkType(ChunkType.STRING_POOL, stringPoolChunkHeader.chunkType)
        stringPool = buffer.let { ParseUtils.readStringPool(it, stringPoolChunkHeader as StringPoolHeader) }

        // read on chunk, check if it was an optional XMLResourceMap chunk
        var chunkHeader = readChunkHeader()
        // 此处不能写作 会报空指针异常 var chunkHeader = readChunkHeader() ?: return
        if (chunkHeader == null) {
            return
        }
        if (chunkHeader.chunkType.toInt() == ChunkType.XML_RESOURCE_MAP) {
            val resourceIds = readXmlResourceMap(chunkHeader as XmlResourceMapHeader)
            resourceMap = arrayOfNulls(resourceIds.size)
            for (i in resourceIds.indices) {
                resourceMap!![i] = Attribute.AttrIds.getString(
                    resourceIds[i]
                )
            }
            // 此处不能写作 会报空指针异常 chunkHeader = readChunkHeader()!!
            chunkHeader = readChunkHeader()
        }
        while (chunkHeader != null) {
            /*if (chunkHeader.chunkType == ChunkType.XML_END_NAMESPACE) {
                    break;
                }*/
            val beginPos = buffer.position().toLong()
            when (chunkHeader.chunkType) {
                ChunkType.XML_END_NAMESPACE.toShort() -> {
                    val xmlNamespaceEndTag = readXmlNamespaceEndTag()
                    xmlStreamer.onNamespaceEnd(xmlNamespaceEndTag)
                }

                ChunkType.XML_START_NAMESPACE.toShort() -> {
                    val namespaceStartTag = readXmlNamespaceStartTag()
                    xmlStreamer.onNamespaceStart(namespaceStartTag)
                }

                ChunkType.XML_START_ELEMENT.toShort() -> {
                    readXmlNodeStartTag()
                }

                ChunkType.XML_END_ELEMENT.toShort() -> {
                    readXmlNodeEndTag()
                }

                ChunkType.XML_CDATA.toShort() -> {
                    readXmlCData()
                }

                else -> if (chunkHeader.chunkType >= ChunkType.XML_FIRST_CHUNK &&
                    chunkHeader.chunkType <= ChunkType.XML_LAST_CHUNK
                ) {
                    Buffers.skip(buffer, chunkHeader.bodySize)
                } else {
                    throw ParserException("Unexpected chunk type:" + chunkHeader.chunkType)
                }
            }
            Buffers.position(buffer, beginPos + chunkHeader.bodySize)
            // 此处不能写作 会报空指针异常 chunkHeader = readChunkHeader()!!
            chunkHeader = readChunkHeader()
        }
    }

    private fun readXmlCData(): XmlCData {
        val xmlCData = XmlCData()
        val dataRef = buffer.int
        if (dataRef > 0) {
            xmlCData.data = stringPool?.get(dataRef)
        }
        xmlCData.typedData = readResValue(buffer, stringPool)
        return xmlCData
    }

    private fun readXmlNodeEndTag(): XmlNodeEndTag {
        val xmlNodeEndTag = XmlNodeEndTag()
        val nsRef = buffer.int
        val nameRef = buffer.int
        if (nsRef > 0) {
            xmlNodeEndTag.namespace = stringPool!![nsRef]
        }
        xmlNodeEndTag.name = stringPool!![nameRef]
        xmlStreamer.onEndTag(xmlNodeEndTag)
        return xmlNodeEndTag
    }

    private fun readXmlNodeStartTag(): XmlNodeStartTag {
        val nsRef = buffer.int
        val nameRef = buffer.int
        val namespace = if (nsRef > 0) stringPool?.get(nsRef) else null
        val name = stringPool?.get(nameRef)
        // read attributes.
        // attributeStart and attributeSize are always 20 (0x14)
        // read attributes.
        // attributeStart and attributeSize are always 20 (0x14)
        val attributeStart = readUShort(buffer)
        val attributeSize = readUShort(buffer)
        val attributeCount = readUShort(buffer)
        val idIndex = readUShort(buffer)
        val classIndex = readUShort(buffer)
        val styleIndex = readUShort(buffer)
        // read attributes
        // read attributes
        val attributes = Attributes(attributeCount)
        for (count in 0 until attributeCount) {
            val attribute = readAttribute()
            val attributeName = attribute.name
            var value = attribute.toStringValue(resourceTable, locale)
            if (value != null && intAttributes.contains(attributeName) && Strings.isNumeric(value)) {
                try {
                    value = getFinalValueAsString(attributeName, value)
                } catch (ignore: Exception) {
                }
            }
            attribute.value = value
            attributes[count] = attribute
        }
        val xmlNodeStartTag = XmlNodeStartTag(namespace, name, attributes)
        xmlStreamer.onStartTag(xmlNodeStartTag)
        return xmlNodeStartTag
    }

    init {
        this.buffer = buffer.duplicate()
        this.buffer.order(byteOrder)
        this.locale = locale
    }

    //trans int attr value to string
    private fun getFinalValueAsString(attributeName: String?, str: String?): String? {
        val value = str!!.toInt()
        return when (attributeName) {
            "screenOrientation" -> AttributeValues.getScreenOrientation(value)
            "configChanges" -> AttributeValues.getConfigChanges(value)
            "windowSoftInputMode" -> AttributeValues.getWindowSoftInputMode(value)
            "launchMode" -> AttributeValues.getLaunchMode(value)
            "installLocation" -> AttributeValues.getInstallLocation(value)
            "protectionLevel" -> AttributeValues.getProtectionLevel(value)
            else -> str
        }
    }

    private fun readAttribute(): Attribute {
        val namespaceRef = buffer.int
        val nameRef = buffer.int
        var name = stringPool!![nameRef]
        if (name!!.isEmpty() && resourceMap != null && nameRef < resourceMap!!.size) {
            // some processed apk file make the string pool value empty, if it is a xmlmap attr.
            name = resourceMap!![nameRef]
        }
        var namespace = if (namespaceRef > 0) stringPool!![namespaceRef] else null
        if (namespace == null || namespace.isEmpty() || "http://schemas.android.com/apk/res/android" == namespace) {
            //TODO parse namespaces better
            //workaround for a weird case that there is no namespace found: https://github.com/hsiafan/apk-parser/issues/122
            // LogCat.d("Got a weird namespace, so setting as empty (namespace isn't supposed to be a URL): " + attribute.getName());
            namespace = "android"
        }
        val rawValueRef = buffer.int
        val rawValue = if (rawValueRef > 0) stringPool!![rawValueRef] else null
        val resValue = readResValue(buffer, stringPool)
        return Attribute(namespace, name!!, rawValue, resValue)
    }

    private fun readXmlNamespaceStartTag(): XmlNamespaceStartTag {
        val prefixRef = buffer.int
        val uriRef = buffer.int
        val nameSpace = XmlNamespaceStartTag()
        if (prefixRef > 0) {
            nameSpace.prefix = stringPool!![prefixRef]
        }
        if (uriRef > 0) {
            nameSpace.uri = stringPool!![uriRef]
        }
        return nameSpace
    }

    private fun readXmlNamespaceEndTag(): XmlNamespaceEndTag {
        val prefixRef = buffer.int
        val prefix = if (prefixRef <= 0) null else stringPool!![prefixRef]
        val uriRef = buffer.int
        val uri = if (uriRef <= 0) null else stringPool!![uriRef]
        return XmlNamespaceEndTag(prefix!!, uri!!)
    }

    private fun readXmlResourceMap(chunkHeader: XmlResourceMapHeader): LongArray {
        val count = chunkHeader.bodySize / 4
        val resourceIds = LongArray(count)
        for (i in 0 until count) {
            resourceIds[i] = Buffers.readUInt(buffer)
        }
        return resourceIds
    }

    private fun readChunkHeader(): ChunkHeader? {
        // finished
        if (!buffer.hasRemaining()) {
            return null
        }
        val begin = buffer.position().toLong()
        val chunkType = Buffers.readUShort(buffer)
        val headerSize = Buffers.readUShort(buffer)
        val chunkSize = Buffers.readUInt(buffer)
        return when (chunkType) {
            ChunkType.XML -> XmlHeader(
                chunkType,
                headerSize,
                chunkSize
            )

            ChunkType.STRING_POOL -> {
                val stringPoolHeader = StringPoolHeader(headerSize, chunkSize, buffer)
                Buffers.position(buffer, begin + headerSize)
                stringPoolHeader
            }

            ChunkType.XML_RESOURCE_MAP -> {
                Buffers.position(buffer, begin + headerSize)
                XmlResourceMapHeader(chunkType, headerSize, chunkSize)
            }

            ChunkType.XML_START_NAMESPACE, ChunkType.XML_END_NAMESPACE, ChunkType.XML_START_ELEMENT, ChunkType.XML_END_ELEMENT, ChunkType.XML_CDATA -> {
                val header = XmlNodeHeader(chunkType, headerSize, chunkSize, buffer)
                Buffers.position(buffer, begin + headerSize)
                header
            }

            ChunkType.NULL -> NullHeader(
                chunkType,
                headerSize,
                chunkSize
            )

            else -> throw ParserException("Unexpected chunk type:$chunkType")
        }
    }

    companion object {
        private val intAttributes: Set<String?> = HashSet(
            mutableListOf<String?>(
                "screenOrientation", "configChanges", "windowSoftInputMode",
                "launchMode", "installLocation", "protectionLevel"
            )
        )
    }
}