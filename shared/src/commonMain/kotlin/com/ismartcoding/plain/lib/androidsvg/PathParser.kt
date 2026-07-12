package com.ismartcoding.plain.lib.androidsvg

/**
 * Pure-Kotlin SVG path data parser supporting M, L, H, V, C, S, Q, T, A, Z commands
 * (both absolute and relative). Returns a list of [SvgPathSegment] instead of
 * platform-specific Path objects.
 */
internal object PathParser {
    fun parse(d: String): List<SvgPathSegment> {
        val segments = mutableListOf<SvgPathSegment>()
        val tokens = tokenize(d)
        var i = 0
        var cx = 0f
        var cy = 0f
        var startX = 0f
        var startY = 0f
        var lastCmd = ' '
        var prevCtrlX = 0f
        var prevCtrlY = 0f

        while (i < tokens.size) {
            val cmd = if (tokens[i].isCommand()) {
                tokens[i][0]
            } else {
                when (lastCmd) {
                    'M' -> 'L'
                    'm' -> 'l'
                    else -> lastCmd
                }
            }
            if (tokens[i].isCommand()) i++

            val relative = cmd.isLowerCase()

            when (cmd.uppercaseChar()) {
                'M' -> {
                    val (x, y) = readPair(tokens, i, relative, cx, cy)
                    segments.add(SvgPathSegment.MoveTo(x, y))
                    cx = x; cy = y; startX = x; startY = y
                    i += 2
                    while (i < tokens.size && !tokens[i].isCommand()) {
                        val (x2, y2) = readPair(tokens, i, relative, cx, cy)
                        segments.add(SvgPathSegment.LineTo(x2, y2))
                        cx = x2; cy = y2
                        i += 2
                    }
                }
                'L' -> {
                    while (i < tokens.size && !tokens[i].isCommand()) {
                        val (x, y) = readPair(tokens, i, relative, cx, cy)
                        segments.add(SvgPathSegment.LineTo(x, y))
                        cx = x; cy = y
                        i += 2
                    }
                }
                'H' -> {
                    while (i < tokens.size && !tokens[i].isCommand()) {
                        val x = tokens[i].toFloat() + if (relative) cx else 0f
                        segments.add(SvgPathSegment.LineTo(x, cy))
                        cx = x
                        i++
                    }
                }
                'V' -> {
                    while (i < tokens.size && !tokens[i].isCommand()) {
                        val y = tokens[i].toFloat() + if (relative) cy else 0f
                        segments.add(SvgPathSegment.LineTo(cx, y))
                        cy = y
                        i++
                    }
                }
                'C' -> {
                    while (i + 5 < tokens.size && !tokens[i].isCommand()) {
                        val (x1, y1) = readPair(tokens, i, relative, cx, cy)
                        val (x2, y2) = readPair(tokens, i + 2, relative, cx, cy)
                        val (x3, y3) = readPair(tokens, i + 4, relative, cx, cy)
                        segments.add(SvgPathSegment.CubicTo(x1, y1, x2, y2, x3, y3))
                        cx = x3; cy = y3
                        i += 6
                    }
                }
                'S' -> {
                    while (i + 3 < tokens.size && !tokens[i].isCommand()) {
                        val (x2, y2) = readPair(tokens, i, relative, cx, cy)
                        val (x3, y3) = readPair(tokens, i + 2, relative, cx, cy)
                        val x1 = 2 * cx - prevCtrlX
                        val y1 = 2 * cy - prevCtrlY
                        segments.add(SvgPathSegment.CubicTo(x1, y1, x2, y2, x3, y3))
                        prevCtrlX = x2; prevCtrlY = y2
                        cx = x3; cy = y3
                        i += 4
                    }
                }
                'Q' -> {
                    while (i + 3 < tokens.size && !tokens[i].isCommand()) {
                        val (x1, y1) = readPair(tokens, i, relative, cx, cy)
                        val (x2, y2) = readPair(tokens, i + 2, relative, cx, cy)
                        segments.add(SvgPathSegment.QuadTo(x1, y1, x2, y2))
                        prevCtrlX = x1; prevCtrlY = y1
                        cx = x2; cy = y2
                        i += 4
                    }
                }
                'T' -> {
                    while (i + 1 < tokens.size && !tokens[i].isCommand()) {
                        val (x2, y2) = readPair(tokens, i, relative, cx, cy)
                        val x1 = 2 * cx - prevCtrlX
                        val y1 = 2 * cy - prevCtrlY
                        segments.add(SvgPathSegment.QuadTo(x1, y1, x2, y2))
                        prevCtrlX = x1; prevCtrlY = y1
                        cx = x2; cy = y2
                        i += 2
                    }
                }
                'A' -> {
                    while (i + 6 < tokens.size && !tokens[i].isCommand()) {
                        val rx = tokens[i].toFloat()
                        val ry = tokens[i + 1].toFloat()
                        val angle = tokens[i + 2].toFloat()
                        val largeArc = tokens[i + 3].toFloat() != 0f
                        val sweep = tokens[i + 4].toFloat() != 0f
                        val (x, y) = readPair(tokens, i + 5, relative, cx, cy)
                        segments.add(SvgPathSegment.ArcTo(rx, ry, angle, largeArc, sweep, x, y))
                        cx = x; cy = y
                        i += 7
                    }
                }
                'Z' -> {
                    segments.add(SvgPathSegment.Close)
                    cx = startX; cy = startY
                }
            }
            lastCmd = cmd
        }
        return segments
    }

    private fun readPair(
        tokens: List<String>,
        i: Int,
        relative: Boolean,
        cx: Float,
        cy: Float,
    ): Pair<Float, Float> {
        val x = tokens[i].toFloat() + if (relative) cx else 0f
        val y = tokens[i + 1].toFloat() + if (relative) cy else 0f
        return x to y
    }

    private fun String.isCommand(): Boolean = length == 1 && this[0].isLetter()

    private fun tokenize(d: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        for (ch in d) {
            when {
                ch.isLetter() -> {
                    if (sb.isNotEmpty()) {
                        tokens.add(sb.toString()); sb.clear()
                    }
                    tokens.add(ch.toString())
                }
                ch.isDigit() || ch == '.' || ch == '-' -> {
                    if (sb.isNotEmpty() && sb.last() == '-' && ch == '-') {
                        tokens.add(sb.toString()); sb.clear()
                    }
                    if (sb.isNotEmpty() && sb.last() != '-' && sb.last() != '.' && ch == '-') {
                        tokens.add(sb.toString()); sb.clear()
                    }
                    if (sb.isNotEmpty() && sb.last() == '.' && ch == '.') {
                        tokens.add(sb.toString()); sb.clear()
                    }
                    sb.append(ch)
                }
                ch.isWhitespace() || ch == ',' -> {
                    if (sb.isNotEmpty()) {
                        tokens.add(sb.toString()); sb.clear()
                    }
                }
            }
        }
        if (sb.isNotEmpty()) tokens.add(sb.toString())
        return tokens.filter { it.isNotBlank() }
    }
}
