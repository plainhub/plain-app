package com.ismartcoding.plain.yjs

/**
 * Low-level binary encoder for lib0 encoding format used by Yjs V1 updates.
 */
class YjsEncoder {
    private val buffer = mutableListOf<Byte>()

    fun toByteArray(): ByteArray = buffer.toByteArray()

    fun writeUint8(value: Int) {
        buffer.add((value and 0xFF).toByte())
    }

    fun writeVarUint(value: Long) {
        var v = value
        while (v >= 0x80) {
            buffer.add(((v and 0x7F) or 0x80).toByte())
            v = v ushr 7
        }
        buffer.add(v.toByte())
    }

    fun writeVarInt(value: Long) {
        val signed = if (value < 0) (-value - 1) shl 1 or 1L else value shl 1
        writeVarUint(signed)
    }

    fun writeVarString(value: String) {
        val bytes = value.encodeToByteArray()
        writeVarUint(bytes.size.toLong())
        for (b in bytes) buffer.add(b)
    }

    fun writeVarUint8Array(arr: ByteArray) {
        writeVarUint(arr.size.toLong())
        for (b in arr) buffer.add(b)
    }

    fun writeFloat32(value: Float) {
        val bits = value.toRawBits()
        writeUint8((bits ushr 24) and 0xFF)
        writeUint8((bits ushr 16) and 0xFF)
        writeUint8((bits ushr 8) and 0xFF)
        writeUint8(bits and 0xFF)
    }

    fun writeFloat64(value: Double) {
        val bits = value.toRawBits()
        val hi = (bits ushr 32).toInt()
        val lo = bits.toInt()
        writeUint8((hi ushr 24) and 0xFF)
        writeUint8((hi ushr 16) and 0xFF)
        writeUint8((hi ushr 8) and 0xFF)
        writeUint8(hi and 0xFF)
        writeUint8((lo ushr 24) and 0xFF)
        writeUint8((lo ushr 16) and 0xFF)
        writeUint8((lo ushr 8) and 0xFF)
        writeUint8(lo and 0xFF)
    }

    fun writeAny(value: Any?) {
        when (value) {
            null -> writeUint8(126)  // null
            is Boolean -> writeUint8(if (value) 120 else 121)  // true / false
            is Int -> { writeUint8(125); writeVarInt(value.toLong()) }  // integer
            is Long -> { writeUint8(125); writeVarInt(value) }  // integer
            is Float -> { writeUint8(124); writeFloat32(value) }  // float32
            is Double -> { writeUint8(123); writeFloat64(value) }  // float64
            is String -> { writeUint8(119); writeVarString(value) }  // string
            is Map<*, *> -> {
                writeUint8(118)  // object
                writeVarUint(value.size.toLong())
                for ((k, v) in value) {
                    writeVarString(k.toString())
                    writeAny(v)
                }
            }
            is List<*> -> {
                writeUint8(117)  // array
                writeVarUint(value.size.toLong())
                for (v in value) writeAny(v)
            }
            else -> { writeUint8(119); writeVarString(value.toString()) }  // fallback to string
        }
    }
}

/**
 * Encodes an image editor document state as a Yjs V1 binary update.
 *
 * Creates a fresh document with a single client. The web side can load this
 * with `Y.applyUpdate(ydoc, state)`.
 *
 * V1 update format:
 *   writeVarUint(numClients)
 *   for each client:
 *     writeVarUint(numStructs)
 *     writeVarUint(clientID)
 *     writeVarUint(startClock)
 *     for each struct: info, origin?, right?, parent?, parentSub?, content
 *   writeVarUint(numClients)  // deleteSet (0 = empty)
 */
object YjsDocEncoder {
    private const val CLIENT_ID = 1L

    // Info bit flags (Yjs/lib0: BIT8=origin, BIT7=right, BIT6=parentSub)
    private const val BIT_ORIGIN = 128
    private const val BIT_RIGHT = 64
    private const val BIT_PARENT_SUB = 32

    // Content type refs (Yjs internals.js contentRefs array)
    private const val CONTENT_STRING = 4
    private const val CONTENT_ANY = 8
    private const val CONTENT_TYPE = 7

    private data class ItemSpec(
        val contentRef: Int,
        val contentWriter: (YjsEncoder) -> Unit,
        val contentLength: Long,
        val parentYKey: String? = null,
        val parentId: Pair<Long, Long>? = null,
        val parentSub: String? = null,
    )

    fun encode(
        canvasSize: CanvasSize,
        bgColor: String,
        sourceImage: String?,
        imgOffset: Point,
        imgAlpha: Double,
        layers: List<EditorLayer>,
        images: Map<String, String> = emptyMap(),
    ): ByteArray {
        val items = mutableListOf<ItemSpec>()
        var clock = 0L

        // Root types: meta (Y.Map=1), layers (Y.Array=0), images (Y.Map=1)
        val metaRootId = Pair(CLIENT_ID, clock)
        items.add(ItemSpec(CONTENT_TYPE, { it.writeVarUint(1) }, 1, parentYKey = "meta"))
        clock += 1

        val layersRootId = Pair(CLIENT_ID, clock)
        items.add(ItemSpec(CONTENT_TYPE, { it.writeVarUint(0) }, 1, parentYKey = "layers"))
        clock += 1

        val imagesRootId = Pair(CLIENT_ID, clock)
        items.add(ItemSpec(CONTENT_TYPE, { it.writeVarUint(1) }, 1, parentYKey = "images"))
        clock += 1

        // Meta entries (Y.Map keys on root "meta" type)
        val metaEntries = linkedMapOf<String, Any?>(
            "canvasWidth" to canvasSize.width,
            "canvasHeight" to canvasSize.height,
            "bgColor" to bgColor,
            "imgOffsetX" to imgOffset.x,
            "imgOffsetY" to imgOffset.y,
            "imgAlpha" to imgAlpha,
        )
        if (sourceImage != null) {
            metaEntries["sourceImage"] = sourceImage
        }
        for ((key, value) in metaEntries) {
            items.add(makeValueItem(value, parentYKey = "meta", parentSub = key, assignClock = { clock += it }))
        }

        // Layer entries (each is a ContentType Y.Map inside the layers Y.Array)
        for (layer in layers) {
            val layerId = Pair(CLIENT_ID, clock)
            items.add(ItemSpec(CONTENT_TYPE, { it.writeVarUint(1) }, 1, parentYKey = "layers"))
            clock += 1

            val props = layerToMap(layer)
            for ((key, value) in props) {
                if (key == "points" && layer is FreehandLayer) {
                    // Freehand points: create a Y.Array (ContentType=0) as a nested type
                    val pointsArrId = Pair(CLIENT_ID, clock)
                    items.add(ItemSpec(CONTENT_TYPE, { it.writeVarUint(0) }, 1, parentId = layerId, parentSub = "points"))
                    clock += 1

                    // Each point is a ContentAny with a single [x, y] array value
                    for (p in layer.points) {
                        items.add(ItemSpec(
                            CONTENT_ANY,
                            { enc ->
                                enc.writeVarUint(1)
                                enc.writeAny(listOf(p.x, p.y))
                            },
                            1,
                            parentId = pointsArrId,
                        ))
                        clock += 1
                    }
                } else if (value is Map<*, *>) {
                    // Nested Y.Map (e.g., textStroke, textShadow)
                    val nestedId = Pair(CLIENT_ID, clock)
                    items.add(ItemSpec(CONTENT_TYPE, { it.writeVarUint(1) }, 1, parentId = layerId, parentSub = key))
                    clock += 1

                    for ((nk, nv) in value) {
                        val nkStr = nk.toString()
                        val item = makeValueItem(nv, parentId = nestedId, parentSub = nkStr, assignClock = { clock += it })
                        items.add(item)
                    }
                } else {
                    val item = makeValueItem(value, parentId = layerId, parentSub = key, assignClock = { clock += it })
                    items.add(item)
                }
            }
        }

        // Image entries (Y.Map keys on root "images" type)
        for ((layerId, dataUrl) in images) {
            items.add(ItemSpec(
                CONTENT_STRING,
                { it.writeVarString(dataUrl) },
                dataUrl.length.toLong(),
                parentYKey = "images",
                parentSub = layerId,
            ))
        }

        // Write V1 update
        val encoder = YjsEncoder()
        encoder.writeVarUint(1) // single client
        encoder.writeVarUint(items.size.toLong())
        encoder.writeVarUint(CLIENT_ID)
        encoder.writeVarUint(0) // start clock

        for (item in items) {
            writeItem(encoder, item)
        }

        // Empty deleteSet
        encoder.writeVarUint(0)

        return encoder.toByteArray()
    }

    private fun makeValueItem(
        value: Any?,
        parentYKey: String? = null,
        parentId: Pair<Long, Long>? = null,
        parentSub: String,
        assignClock: (Long) -> Unit,
    ): ItemSpec {
        return when (value) {
            is String -> {
                assignClock(value.length.toLong())
                ItemSpec(CONTENT_STRING, { it.writeVarString(value) }, value.length.toLong(), parentYKey, parentId, parentSub)
            }
            else -> {
                assignClock(1)
                ItemSpec(CONTENT_ANY, { enc ->
                    enc.writeVarUint(1)
                    enc.writeAny(value)
                }, 1, parentYKey, parentId, parentSub)
            }
        }
    }

    private fun writeItem(encoder: YjsEncoder, item: ItemSpec) {
        val hasParentSub = item.parentSub != null
        val hasOrigin = false
        val hasRight = false

        var info = item.contentRef
        if (hasOrigin) info = info or BIT_ORIGIN
        if (hasRight) info = info or BIT_RIGHT
        if (hasParentSub) info = info or BIT_PARENT_SUB

        encoder.writeUint8(info)

        // No origin, no right to write

        // Parent info is written when !hasOrigin && !hasRight
        if (!hasOrigin && !hasRight) {
            if (item.parentYKey != null) {
                encoder.writeVarUint(1) // parentInfo = 1 (string, root type)
                encoder.writeVarString(item.parentYKey)
            } else if (item.parentId != null) {
                encoder.writeVarUint(0) // parentInfo = 0 (ID, nested type)
                encoder.writeVarUint(item.parentId.first)
                encoder.writeVarUint(item.parentId.second)
            }
        }

        // parentSub string
        if (hasParentSub) {
            encoder.writeVarString(item.parentSub)
        }

        // Content
        item.contentWriter(encoder)
    }

    private fun layerToMap(layer: EditorLayer): Map<String, Any?> {
        return when (layer) {
            is ArrowLayer -> mapOf(
                "id" to layer.id, "type" to layer.type, "visible" to layer.visible, "name" to layer.name,
                "x1" to layer.x1, "y1" to layer.y1, "x2" to layer.x2, "y2" to layer.y2,
                "color" to layer.color, "lineWidth" to layer.lineWidth, "rotation" to layer.rotation,
            )
            is RectLayer -> mapOf(
                "id" to layer.id, "type" to layer.type, "visible" to layer.visible, "name" to layer.name,
                "x" to layer.x, "y" to layer.y, "w" to layer.w, "h" to layer.h,
                "color" to layer.color, "lineWidth" to layer.lineWidth,
            )
            is EllipseLayer -> mapOf(
                "id" to layer.id, "type" to layer.type, "visible" to layer.visible, "name" to layer.name,
                "cx" to layer.cx, "cy" to layer.cy, "rx" to layer.rx, "ry" to layer.ry,
                "color" to layer.color, "lineWidth" to layer.lineWidth,
            )
            is HighlightLayer -> mapOf(
                "id" to layer.id, "type" to layer.type, "visible" to layer.visible, "name" to layer.name,
                "x" to layer.x, "y" to layer.y, "w" to layer.w, "h" to layer.h,
                "color" to layer.color,
            )
            is MosaicLayer -> mapOf(
                "id" to layer.id, "type" to layer.type, "visible" to layer.visible, "name" to layer.name,
                "x" to layer.x, "y" to layer.y, "w" to layer.w, "h" to layer.h,
                "blockSize" to layer.blockSize,
            )
            is TextLayer -> mapOf(
                "id" to layer.id, "type" to layer.type, "visible" to layer.visible, "name" to layer.name,
                "x" to layer.x, "y" to layer.y, "text" to layer.text,
                "fontSize" to layer.fontSize, "color" to layer.color,
                "fontFamily" to layer.fontFamily, "fontWeight" to layer.fontWeight,
                "fontStyle" to layer.fontStyle, "textAlign" to layer.textAlign,
                "letterSpacing" to layer.letterSpacing, "textDecoration" to layer.textDecoration,
                "textStroke" to mapOf("width" to layer.textStroke.width, "color" to layer.textStroke.color),
                "textShadow" to mapOf(
                    "offsetX" to layer.textShadow.offsetX, "offsetY" to layer.textShadow.offsetY,
                    "blur" to layer.textShadow.blur, "color" to layer.textShadow.color,
                ),
                "glow" to layer.glow, "rotation" to layer.rotation, "maxWidth" to layer.maxWidth,
            )
            is ImageLayer -> mapOf(
                "id" to layer.id, "type" to layer.type, "visible" to layer.visible, "name" to layer.name,
                "x" to layer.x, "y" to layer.y, "w" to layer.w, "h" to layer.h,
                "opacity" to layer.opacity, "rotation" to layer.rotation,
            )
            is FreehandLayer -> mapOf(
                "id" to layer.id, "type" to layer.type, "visible" to layer.visible, "name" to layer.name,
                "points" to "array",
                "color" to layer.color, "lineWidth" to layer.lineWidth,
            )
            is StickerLayer -> mapOf(
                "id" to layer.id, "type" to layer.type, "visible" to layer.visible, "name" to layer.name,
                "x" to layer.x, "y" to layer.y, "w" to layer.w, "h" to layer.h,
                "color" to layer.color, "text" to layer.text,
                "fontSize" to layer.fontSize, "fontWeight" to layer.fontWeight,
                "fontStyle" to layer.fontStyle, "rotation" to layer.rotation,
            )
        }
    }
}
