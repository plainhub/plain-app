package com.ismartcoding.plain.lib.yjs

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.collections.get
import kotlin.collections.iterator

/**
 * Parses a Yjs V1 binary update and extracts the image editor document state.
 *
 * V1 update format:
 *   readVarUint(numClients)
 *   for each client:
 *     readVarUint(numStructs)
 *     readVarUint(clientID)
 *     readVarUint(startClock)
 *     for each struct: info, origin?, right?, parent?, content
 *   readVarUint(numClients)  // deleteSet
 *   for each client: readDsClient, readVarUint(numRanges), ranges...
 */
class YjsDoc(data: ByteArray) {
    private val items = mutableListOf<YjsItem>()
    private val itemsById = mutableMapOf<Pair<Long, Long>, YjsItem>()

    // Root type name → root type info
    private val rootTypes = mutableMapOf<String, RootType>()

    // Item ID → children (for nested Y.Map/Y.Array items)
    private val itemMapEntries = mutableMapOf<Pair<Long, Long>, MutableMap<String, YjsItem>>()
    private val itemArrayItems = mutableMapOf<Pair<Long, Long>, MutableList<YjsItem>>()

    private data class YjsItem(
        val id: Pair<Long, Long>,  // (client, clock)
        val origin: Pair<Long, Long>?,  // left neighbor ID
        val right: Pair<Long, Long>?,   // right neighbor ID
        val parentYKey: String?,        // root type name (if parent is root type)
        val parentId: Pair<Long, Long>?, // parent item ID (if parent is nested type)
        var parentSub: String?,         // Y.Map key (if parent is Y.Map)
        val content: YjsContent,
        var deleted: Boolean,
    )

    private sealed class YjsContent {
        data class StringContent(val value: String) : YjsContent()
        data class AnyContent(val values: List<Any?>) : YjsContent()
        data class TypeContent(val typeRef: Int) : YjsContent()  // 0=Array, 1=Map, 2=Text
        data class JsonContent(val values: List<String>) : YjsContent()
        data class DeletedContent(val len: Int) : YjsContent()
        data class BinaryContent(val data: ByteArray) : YjsContent()
    }

    private class RootType(
        val typeRef: Int,  // 0=Array, 1=Map
        val mapEntries: MutableMap<String, YjsItem> = mutableMapOf(),
        val arrayItems: MutableList<YjsItem> = mutableListOf(),
    )

    init {
        val decoder = YjsDecoder(data)
        parseStructs(decoder)
        repeat(decoder.readVarUint().toInt()) {
            val client = decoder.readVarUint()
            repeat(decoder.readVarUint().toInt()) {
                val clock = decoder.readVarUint()
                val length = decoder.readVarUint()
                for (deletedClock in clock until clock + length) itemsById[Pair(client, deletedClock)]?.deleted = true
            }
        }
        buildDocument()
    }

    private fun parseStructs(decoder: YjsDecoder) {
        val numClients = decoder.readVarUint().toInt()
        for (i in 0 until numClients) {
            val numStructs = decoder.readVarUint().toInt()
            val client = decoder.readVarUint()
            var clock = decoder.readVarUint()
            for (j in 0 until numStructs) {
                val info = decoder.readUint8()
                when {
                    info == 10 -> {
                        val len = decoder.readVarUint().toInt()
                        clock += len
                    }
                    (info and 31) != 0 -> {
                        // Yjs bit flags (lib0 binary.js): BIT8=origin, BIT7=right, BIT6=parentSub
                        val hasOrigin = (info and 128) != 0
                        val hasRight = (info and 64) != 0
                        val hasParentSub = (info and 32) != 0
                        val cantCopyParentInfo = !hasOrigin && !hasRight

                        val origin = if (hasOrigin) {
                            Pair(decoder.readVarUint(), decoder.readVarUint())
                        } else null

                        val right = if (hasRight) {
                            Pair(decoder.readVarUint(), decoder.readVarUint())
                        } else null

                        var parentYKey: String? = null
                        var parentId: Pair<Long, Long>? = null
                        var parentSub: String? = null

                        if (cantCopyParentInfo) {
                            val parentInfo = decoder.readVarUint().toInt()
                            if (parentInfo == 1) {
                                parentYKey = decoder.readVarString()
                            } else {
                                parentId = Pair(decoder.readVarUint(), decoder.readVarUint())
                            }
                            if (hasParentSub) {
                                parentSub = decoder.readVarString()
                            }
                        }

                        val contentType = info and 31
                        val itemId = Pair(client, clock)
                        val content: YjsContent = when (contentType) {
                            1 -> {
                                val len = decoder.readVarUint().toInt()
                                clock += len
                                YjsContent.DeletedContent(len)
                            }
                            2 -> {
                                val len = decoder.readVarUint().toInt()
                                val values = (0 until len).map { decoder.readVarString() }
                                clock += len
                                YjsContent.JsonContent(values)
                            }
                            3 -> {
                                val arr = decoder.readVarUint8Array()
                                clock += 1
                                YjsContent.BinaryContent(arr)
                            }
                            4 -> {
                                val str = decoder.readVarString()
                                clock += str.length.toLong()
                                YjsContent.StringContent(str)
                            }
                            5 -> { // ContentEmbed: JSON string
                                decoder.readVarString()
                                clock += 1
                                YjsContent.DeletedContent(0)
                            }
                            6 -> { // ContentFormat: key + JSON value
                                decoder.readVarString()
                                decoder.readVarString()
                                clock += 1
                                YjsContent.DeletedContent(0)
                            }
                            7 -> {
                                val typeRef = decoder.readVarUint().toInt()
                                clock += 1
                                YjsContent.TypeContent(typeRef)
                            }
                            8 -> { // ContentAny
                                val len = decoder.readVarUint().toInt()
                                val values = (0 until len).map { decoder.readAny() }
                                clock += len
                                YjsContent.AnyContent(values)
                            }
                            9 -> { // ContentDoc
                                decoder.readVarString()
                                decoder.readAny()
                                clock += 1
                                YjsContent.DeletedContent(0)
                            }
                            else -> throw IllegalStateException("Unknown content type: $contentType")
                        }

                        val item = YjsItem(
                            id = itemId,
                            origin = origin,
                            right = right,
                            parentYKey = parentYKey,
                            parentId = parentId,
                            parentSub = parentSub,
                            content = content,
                            deleted = contentType == 1,
                        )
                        items.add(item)
                        for (itemClock in itemId.second until clock) itemsById[Pair(client, itemClock)] = item
                    }
                    else -> {
                        val len = decoder.readVarUint().toInt()
                        clock += len
                    }
                }
            }
        }
    }

    private fun buildDocument() {
        // First: determine root type kinds. Scan all items with parentYKey and
        // infer the root type's kind from parentSub presence:
        //   - any item has parentSub → Y.Map (typeRef=1)
        //   - no item has parentSub  → Y.Array (typeRef=0)
        val rootTypeKinds = mutableMapOf<String, Int>()
        for (item in items) {
            val key = item.parentYKey ?: continue
            if (item.parentSub != null) {
                rootTypeKinds[key] = 1 // Y.Map
            } else if (!rootTypeKinds.containsKey(key)) {
                rootTypeKinds[key] = 0 // Y.Array
            }
        }
        for ((key, typeRef) in rootTypeKinds) {
            rootTypes[key] = RootType(typeRef)
        }

        // Second: assign each item to its DIRECT parent.
        // - If parentYKey != null → parent is a root type
        // - If parentId != null → parent is a nested item (e.g., a layer Y.Map)
        // - If only origin/right → resolve parent through neighbor chain
        for (item in items) {
            if (item.parentSub == null) {
                item.parentSub = resolveParentSub(item)
            }

            val parentYKey = resolveParentYKey(item)
            val parentId = resolveParentId(item)
            val sub = item.parentSub

            if (parentYKey != null) {
                val rootType = rootTypes[parentYKey] ?: continue
                if (sub != null) {
                    if (!item.deleted) rootType.mapEntries[sub] = item
                } else {
                    rootType.arrayItems.add(item)
                }
            } else if (parentId != null) {
                if (sub != null) {
                    if (!item.deleted) itemMapEntries.getOrPut(parentId) { mutableMapOf() }[sub] = item
                } else {
                    itemArrayItems.getOrPut(parentId) { mutableListOf() }.add(item)
                }
            }
        }

        for (rootType in rootTypes.values) orderArray(rootType.arrayItems)
        for (array in itemArrayItems.values) orderArray(array)
    }

    private fun orderArray(array: MutableList<YjsItem>) {
        val entries = array.associateBy { it.id }
        val successors = entries.keys.associateWith { mutableSetOf<Pair<Long, Long>>() }
        val incoming = entries.keys.associateWith { 0 }.toMutableMap()
        fun link(left: YjsItem?, right: YjsItem?) {
            if (left == null || right == null || left.id !in entries || right.id !in entries) return
            if (successors.getValue(left.id).add(right.id)) incoming[right.id] = incoming.getValue(right.id) + 1
        }
        for (item in array) {
            link(itemsById[item.origin], item)
            link(item, itemsById[item.right])
        }
        for (siblings in array.groupBy { Pair(it.origin, it.right) }.values) {
            val ordered = siblings.sortedWith(compareBy({ it.id.first }, { it.id.second }))
            for (index in 1 until ordered.size) link(ordered[index - 1], ordered[index])
        }
        val ready = array.filter { incoming.getValue(it.id) == 0 }.toMutableList()
        val ordered = mutableListOf<YjsItem>()
        while (ready.isNotEmpty()) {
            ready.sortWith(compareBy({ it.id.first }, { it.id.second }))
            val item = ready.removeAt(0)
            ordered.add(item)
            for (next in successors.getValue(item.id)) {
                incoming[next] = incoming.getValue(next) - 1
                if (incoming.getValue(next) == 0) ready.add(entries.getValue(next))
            }
        }
        check(ordered.size == array.size) { "Invalid Yjs array ordering" }
        array.clear()
        array.addAll(ordered.filterNot { it.deleted })
    }

    private fun resolveParentYKey(item: YjsItem): String? {
        if (item.parentYKey != null) return item.parentYKey
        if (item.parentId != null) return null
        if (item.origin != null) {
            val leftItem = itemsById[item.origin] ?: return null
            return resolveParentYKey(leftItem)
        }
        if (item.right != null) {
            val rightItem = itemsById[item.right] ?: return null
            return resolveParentYKey(rightItem)
        }
        return null
    }

    private fun resolveParentSub(item: YjsItem): String? {
        if (item.parentSub != null) return item.parentSub
        if (item.origin != null) {
            val leftItem = itemsById[item.origin] ?: return null
            return resolveParentSub(leftItem)
        }
        if (item.right != null) {
            val rightItem = itemsById[item.right] ?: return null
            return resolveParentSub(rightItem)
        }
        return null
    }

    private fun resolveParentId(item: YjsItem): Pair<Long, Long>? {
        if (item.parentId != null) return item.parentId
        if (item.origin != null) {
            val leftItem = itemsById[item.origin] ?: return null
            return resolveParentId(leftItem)
        }
        if (item.right != null) {
            val rightItem = itemsById[item.right] ?: return null
            return resolveParentId(rightItem)
        }
        return null
    }

    fun getMetaValue(key: String): Any? {
        val meta = rootTypes["meta"] ?: return null
        val item = meta.mapEntries[key] ?: return null
        return extractValue(item)
    }

    fun getCanvasSize(): CanvasSize {
        val w = (getMetaValue("canvasWidth") as? Number)?.toInt() ?: 1920
        val h = (getMetaValue("canvasHeight") as? Number)?.toInt() ?: 1080
        return CanvasSize(w, h)
    }

    fun getBgColor(): String = (getMetaValue("bgColor") as? String) ?: "transparent"
    fun getSourceImage(): String? = getMetaValue("sourceImage") as? String
    fun getImages(): Map<String, String> = rootTypes["images"]?.mapEntries?.mapValues { (_, item) -> extractValue(item) as String } ?: emptyMap()
    fun getImgAlpha(): Double = (getMetaValue("imgAlpha") as? Number)?.toDouble() ?: 100.0

    fun getImgOffset(): Point {
        val x = (getMetaValue("imgOffsetX") as? Number)?.toDouble() ?: 0.0
        val y = (getMetaValue("imgOffsetY") as? Number)?.toDouble() ?: 0.0
        return Point(x, y)
    }

    fun getLayers(): List<EditorLayer> {
        val layersType = rootTypes["layers"] ?: return emptyList()
        return layersType.arrayItems.map { item ->
            check(item.content is YjsContent.TypeContent && item.content.typeRef == 1) { "Invalid image editor layer" }
            readLayer(item.id)
        }
    }

    private fun readLayer(layerItemId: Pair<Long, Long>): EditorLayer {
        val entries = checkNotNull(itemMapEntries[layerItemId]) { "Invalid image editor layer" }
        val layerMap = mutableMapOf<String, Any?>()
        for ((sub, item) in entries) {
            layerMap[sub] = when {
                item.content is YjsContent.TypeContent && item.content.typeRef == 1 ->
                    readNestedMap(item.id)
                else -> extractValue(item)
            }
        }

        val type = enumValueOf<EditorLayerType>(str(layerMap["type"]))
        val id = layerMap["id"] as String
        val visible = layerMap["visible"] as Boolean
        val name = layerMap["name"] as String

        return when (type) {
            EditorLayerType.ARROW -> ArrowLayer(
                id = id, visible = visible, name = name,
                x1 = num(layerMap["x1"]), y1 = num(layerMap["y1"]),
                x2 = num(layerMap["x2"]), y2 = num(layerMap["y2"]),
                color = str(layerMap["color"]), lineWidth = num(layerMap["lineWidth"]),
                rotation = num(layerMap["rotation"]),
            )
            EditorLayerType.RECT -> RectLayer(
                id = id, visible = visible, name = name,
                x = num(layerMap["x"]), y = num(layerMap["y"]),
                w = num(layerMap["w"]), h = num(layerMap["h"]),
                color = str(layerMap["color"]), lineWidth = num(layerMap["lineWidth"]),
            )
            EditorLayerType.ELLIPSE -> EllipseLayer(
                id = id, visible = visible, name = name,
                cx = num(layerMap["cx"]), cy = num(layerMap["cy"]),
                rx = num(layerMap["rx"]), ry = num(layerMap["ry"]),
                color = str(layerMap["color"]), lineWidth = num(layerMap["lineWidth"]),
            )
            EditorLayerType.HIGHLIGHT -> HighlightLayer(
                id = id, visible = visible, name = name,
                x = num(layerMap["x"]), y = num(layerMap["y"]),
                w = num(layerMap["w"]), h = num(layerMap["h"]),
                color = str(layerMap["color"]),
            )
            EditorLayerType.MOSAIC -> MosaicLayer(
                id = id, visible = visible, name = name,
                x = num(layerMap["x"]), y = num(layerMap["y"]),
                w = num(layerMap["w"]), h = num(layerMap["h"]),
                blockSize = num(layerMap["blockSize"]),
            )
            EditorLayerType.TEXT -> TextLayer(
                id = id, visible = visible, name = name,
                x = num(layerMap["x"]), y = num(layerMap["y"]),
                text = str(layerMap["text"]),
                fontSize = num(layerMap["fontSize"]),
                color = str(layerMap["color"]),
                fontFamily = str(layerMap["fontFamily"]),
                fontWeight = enumValueOf<EditorFontWeight>(str(layerMap["fontWeight"])),
                fontStyle = enumValueOf<EditorFontStyle>(str(layerMap["fontStyle"])),
                textAlign = enumValueOf<EditorTextAlign>(str(layerMap["textAlign"])),
                letterSpacing = num(layerMap["letterSpacing"]),
                textDecoration = enumValueOf<EditorTextDecoration>(str(layerMap["textDecoration"])),
                textStroke = readTextStroke(layerMap["textStroke"]),
                textShadow = readTextShadow(layerMap["textShadow"]),
                glow = layerMap["glow"] as? Boolean ?: false,
                rotation = num(layerMap["rotation"]),
                maxWidth = num(layerMap["maxWidth"]),
            )
            EditorLayerType.IMAGE -> ImageLayer(
                id = id, visible = visible, name = name,
                x = num(layerMap["x"]), y = num(layerMap["y"]),
                w = num(layerMap["w"]), h = num(layerMap["h"]),
                opacity = num(layerMap["opacity"]),
                rotation = num(layerMap["rotation"]),
            )
            EditorLayerType.FREEHAND -> {
                val points = readPoints(layerItemId)
                FreehandLayer(
                    id = id, visible = visible, name = name,
                    points = points,
                    color = str(layerMap["color"]),
                    lineWidth = num(layerMap["lineWidth"]),
                )
            }
            EditorLayerType.STICKER -> StickerLayer(
                id = id, visible = visible, name = name,
                x = num(layerMap["x"]), y = num(layerMap["y"]),
                w = num(layerMap["w"]), h = num(layerMap["h"]),
                color = str(layerMap["color"]),
                text = str(layerMap["text"]),
                fontSize = num(layerMap["fontSize"]),
                fontWeight = enumValueOf<EditorFontWeight>(str(layerMap["fontWeight"])),
                fontStyle = enumValueOf<EditorFontStyle>(str(layerMap["fontStyle"])),
                rotation = num(layerMap["rotation"]),
            )
        }
    }

    private fun readNestedMap(mapItemId: Pair<Long, Long>): Map<String, Any?> {
        val entries = itemMapEntries[mapItemId] ?: return emptyMap()
        val result = mutableMapOf<String, Any?>()
        for ((sub, item) in entries) {
            result[sub] = extractValue(item)
        }
        return result
    }

    private fun readPoints(layerItemId: Pair<Long, Long>): List<Point> {
        val entries = itemMapEntries[layerItemId] ?: return emptyList()
        val pointsItem = entries["points"] ?: return emptyList()
        val pointItems = itemArrayItems[pointsItem.id] ?: return emptyList()
        val points = mutableListOf<Point>()
        for (item in pointItems) {
            val values = when (item.content) {
                is YjsContent.AnyContent -> item.content.values
                else -> listOf(extractValue(item))
            }
            for (value in values) {
                if (value is List<*>) {
                    if (value.size >= 2) {
                        points.add(Point(num(value[0]), num(value[1])))
                    }
                }
            }
        }
        return points
    }

    private fun readTextStroke(value: Any?): TextStroke {
        val map = value as Map<*, *>
        return TextStroke(
            width = num(map["width"]),
            color = str(map["color"]),
        )
    }

    private fun readTextShadow(value: Any?): TextShadow {
        val map = value as Map<*, *>
        return TextShadow(
            offsetX = num(map["offsetX"]),
            offsetY = num(map["offsetY"]),
            blur = num(map["blur"]),
            color = str(map["color"]),
        )
    }

    private fun extractValue(item: YjsItem): Any? {
        return when (item.content) {
            is YjsContent.StringContent -> item.content.value
            is YjsContent.AnyContent -> item.content.values.firstOrNull()
            is YjsContent.JsonContent -> {
                val json = item.content.values.firstOrNull() ?: return null
                Json.parseToJsonElement(json).let { jsonElementToJsonValue(it) }
            }
            is YjsContent.TypeContent -> {
                item.id
            }
            else -> null
        }
    }

    private fun jsonElementToJsonValue(element: JsonElement): Any? {
        return when (element) {
            is JsonPrimitive -> {
                when {
                    element.isString -> element.content
                    element.content == "true" -> true
                    element.content == "false" -> false
                    else -> element.content.toDoubleOrNull() ?: element.content
                }
            }
            is JsonObject -> {
                element.entries.associate { (k, v) -> k to jsonElementToJsonValue(v) }
            }
            is JsonArray -> {
                element.map { jsonElementToJsonValue(it) }
            }
        }
    }

    private fun num(value: Any?): Double = (value as Number).toDouble()

    private fun str(value: Any?): String = value as String
}
