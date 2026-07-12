package com.ismartcoding.plain.lib.androidsvg

import com.ismartcoding.plain.lib.opml.SimpleXmlReader

object SvgParser {
    fun parse(svg: String): SvgDocument {
        val reader = SimpleXmlReader(svg)
        val root = parseRoot(reader) ?: throw SvgParseException("No <svg> root element")
        val (w, h) = root.dimensions
        return SvgDocument(w, h, root.viewBox, root)
    }

    private fun parseRoot(reader: SimpleXmlReader): SvgGroup? {
        var event = reader.eventType
        while (event != SimpleXmlReader.END_DOCUMENT) {
            if (event == SimpleXmlReader.START_TAG) {
                if (reader.name == "svg") return parseGroup(reader, isRoot = true)
            }
            event = reader.next()
        }
        return null
    }

    private fun parseGroup(reader: SimpleXmlReader, isRoot: Boolean = false): SvgGroup {
        val children = mutableListOf<SvgNode>()
        var viewBox: SvgRect? = null
        var width = -1f
        var height = -1f
        val transform = parseTransform(reader.getAttributeValue(null, "transform"))

        if (isRoot) {
            width = parseLength(reader.getAttributeValue(null, "width"))
            height = parseLength(reader.getAttributeValue(null, "height"))
            viewBox = parseViewBox(reader.getAttributeValue(null, "viewBox"))
        }

        val fill = reader.getAttributeValue(null, "fill")
        val stroke = reader.getAttributeValue(null, "stroke")
        val strokeWidth = reader.getAttributeValue(null, "stroke-width")
        val style = SvgStyle.inherit(fill, stroke, strokeWidth)

        val tagName = reader.name
        var event = reader.next()
        while (event != SimpleXmlReader.END_DOCUMENT) {
            if (event == SimpleXmlReader.START_TAG) {
                when (reader.name) {
                    "g" -> children.add(parseGroup(reader))
                    "path" -> parsePathElement(reader, style)?.let { children.add(it) }
                    "rect" -> parseRectElement(reader, style)?.let { children.add(it) }
                    "circle" -> parseCircleElement(reader, style)?.let { children.add(it) }
                    "ellipse" -> parseEllipseElement(reader, style)?.let { children.add(it) }
                    "line" -> parseLineElement(reader, style)?.let { children.add(it) }
                    "polyline" -> parsePolylineElement(reader, style, closed = false)?.let { children.add(it) }
                    "polygon" -> parsePolylineElement(reader, style, closed = true)?.let { children.add(it) }
                }
            } else if (event == SimpleXmlReader.END_TAG) {
                if (reader.name == tagName) break
            }
            event = reader.next()
        }

        return SvgGroup(isRoot, width, height, viewBox, transform, children)
    }

    private fun parsePathElement(reader: SimpleXmlReader, parentStyle: SvgStyle): SvgLeaf? {
        val d = reader.getAttributeValue(null, "d") ?: return null
        val shape = SvgShape.Path(PathParser.parse(d))
        val style = SvgStyle.fromAttrs(
            reader.getAttributeValue(null, "fill"),
            reader.getAttributeValue(null, "stroke"),
            reader.getAttributeValue(null, "stroke-width"),
            parentStyle,
        )
        return SvgLeaf(shape, style)
    }

    private fun parseRectElement(reader: SimpleXmlReader, parentStyle: SvgStyle): SvgLeaf? {
        val x = reader.getAttributeValue(null, "x")?.toFloatOrNull() ?: 0f
        val y = reader.getAttributeValue(null, "y")?.toFloatOrNull() ?: 0f
        val w = reader.getAttributeValue(null, "width")?.toFloatOrNull() ?: 0f
        val h = reader.getAttributeValue(null, "height")?.toFloatOrNull() ?: 0f
        val shape = SvgShape.Rect(x, y, w, h)
        return SvgLeaf(shape, SvgStyle.fromAttrs(
            reader.getAttributeValue(null, "fill"),
            reader.getAttributeValue(null, "stroke"),
            reader.getAttributeValue(null, "stroke-width"),
            parentStyle,
        ))
    }

    private fun parseCircleElement(reader: SimpleXmlReader, parentStyle: SvgStyle): SvgLeaf? {
        val cx = reader.getAttributeValue(null, "cx")?.toFloatOrNull() ?: 0f
        val cy = reader.getAttributeValue(null, "cy")?.toFloatOrNull() ?: 0f
        val r = reader.getAttributeValue(null, "r")?.toFloatOrNull() ?: 0f
        if (r <= 0f) return null
        val shape = SvgShape.Circle(cx, cy, r)
        return SvgLeaf(shape, SvgStyle.fromAttrs(
            reader.getAttributeValue(null, "fill"),
            reader.getAttributeValue(null, "stroke"),
            reader.getAttributeValue(null, "stroke-width"),
            parentStyle,
        ))
    }

    private fun parseEllipseElement(reader: SimpleXmlReader, parentStyle: SvgStyle): SvgLeaf? {
        val cx = reader.getAttributeValue(null, "cx")?.toFloatOrNull() ?: 0f
        val cy = reader.getAttributeValue(null, "cy")?.toFloatOrNull() ?: 0f
        val rx = reader.getAttributeValue(null, "rx")?.toFloatOrNull() ?: 0f
        val ry = reader.getAttributeValue(null, "ry")?.toFloatOrNull() ?: 0f
        if (rx <= 0f || ry <= 0f) return null
        val shape = SvgShape.Ellipse(cx, cy, rx, ry)
        return SvgLeaf(shape, SvgStyle.fromAttrs(
            reader.getAttributeValue(null, "fill"),
            reader.getAttributeValue(null, "stroke"),
            reader.getAttributeValue(null, "stroke-width"),
            parentStyle,
        ))
    }

    private fun parseLineElement(reader: SimpleXmlReader, parentStyle: SvgStyle): SvgLeaf? {
        val x1 = reader.getAttributeValue(null, "x1")?.toFloatOrNull() ?: 0f
        val y1 = reader.getAttributeValue(null, "y1")?.toFloatOrNull() ?: 0f
        val x2 = reader.getAttributeValue(null, "x2")?.toFloatOrNull() ?: 0f
        val y2 = reader.getAttributeValue(null, "y2")?.toFloatOrNull() ?: 0f
        val shape = SvgShape.Line(x1, y1, x2, y2)
        return SvgLeaf(shape, SvgStyle.fromAttrs(
            reader.getAttributeValue(null, "fill"),
            reader.getAttributeValue(null, "stroke"),
            reader.getAttributeValue(null, "stroke-width"),
            parentStyle,
        ))
    }

    private fun parsePolylineElement(
        reader: SimpleXmlReader,
        parentStyle: SvgStyle,
        closed: Boolean,
    ): SvgLeaf? {
        val pointsStr = reader.getAttributeValue(null, "points") ?: return null
        val nums = pointsStr.trim().split(Regex("[\\s,]+")).mapNotNull { it.toFloatOrNull() }
        if (nums.size < 4) return null
        val shape = SvgShape.Polyline(nums, closed)
        return SvgLeaf(shape, SvgStyle.fromAttrs(
            reader.getAttributeValue(null, "fill"),
            reader.getAttributeValue(null, "stroke"),
            reader.getAttributeValue(null, "stroke-width"),
            parentStyle,
        ))
    }

    private fun parseViewBox(value: String?): SvgRect? {
        if (value.isNullOrBlank()) return null
        val parts = value.trim().split(Regex("[\\s,]+")).mapNotNull { it.toFloatOrNull() }
        if (parts.size < 4) return null
        return SvgRect(parts[0], parts[1], parts[0] + parts[2], parts[1] + parts[3])
    }

    private fun parseLength(value: String?): Float {
        if (value.isNullOrBlank()) return -1f
        return value.replace(Regex("[^0-9.\\-]"), "").toFloatOrNull() ?: -1f
    }

    private fun parseTransform(value: String?): List<SvgTransformOp>? {
        if (value.isNullOrBlank()) return null
        val ops = mutableListOf<SvgTransformOp>()
        val regex = Regex("(\\w+)\\(([^)]+)\\)")
        for (match in regex.findAll(value)) {
            val cmd = match.groupValues[1]
            val args =
                match.groupValues[2].trim().split(Regex("[\\s,]+")).mapNotNull { it.toFloatOrNull() }
            when (cmd) {
                "translate" -> if (args.size >= 2) ops.add(SvgTransformOp.Translate(args[0], args[1]))
                "scale" ->
                    if (args.size >= 2) ops.add(SvgTransformOp.Scale(args[0], args[1]))
                    else if (args.size == 1) ops.add(SvgTransformOp.Scale(args[0], args[0]))
                "rotate" ->
                    if (args.size == 1) ops.add(SvgTransformOp.Rotate(args[0]))
                    else if (args.size >= 3) ops.add(SvgTransformOp.Rotate(args[0], args[1], args[2]))
                "matrix" ->
                    if (args.size >= 6) ops.add(
                        SvgTransformOp.Matrix(
                            floatArrayOf(args[0], args[2], args[4], args[1], args[3], args[5], 0f, 0f, 1f),
                        ),
                    )
            }
        }
        return if (ops.isEmpty()) null else ops
    }
}
