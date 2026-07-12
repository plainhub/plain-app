package com.ismartcoding.plain.lib.html2md

/**
 * Pure-Kotlin HTML DOM replacing jsoup for html2md conversion.
 * Provides a minimal Node/Element/TextNode tree with HTML parsing.
 */

abstract class HtmlNode {
    var parent: HtmlNode? = null
        internal set
    internal val _childNodes = mutableListOf<HtmlNode>()

    fun parentNode(): HtmlNode? = parent
    fun parent(): HtmlNode? = parent
    fun childNodes(): List<HtmlNode> = _childNodes
    fun childNodeSize(): Int = _childNodes.size
    fun childNode(index: Int): HtmlNode = _childNodes[index]
    fun firstChild(): HtmlNode? = _childNodes.firstOrNull()

    fun previousSibling(): HtmlNode? {
        val p = parent ?: return null
        val idx = p._childNodes.indexOf(this)
        return if (idx > 0) p._childNodes[idx - 1] else null
    }

    fun nextSibling(): HtmlNode? {
        val p = parent ?: return null
        val idx = p._childNodes.indexOf(this)
        return if (idx in 0 until p._childNodes.size - 1) p._childNodes[idx + 1] else null
    }

    fun nextElementSibling(): HtmlElement? {
        var sibling = nextSibling()
        while (sibling != null) {
            if (sibling is HtmlElement) return sibling
            sibling = sibling.nextSibling()
        }
        return null
    }

    fun ownerDocument(): HtmlElement? {
        var node: HtmlNode? = this
        while (node?.parent != null) {
            node = node.parent
        }
        return node as? HtmlElement
    }

    abstract fun nodeName(): String
    abstract fun outerHtml(): String

    open fun attr(name: String): String = ""
    open fun hasAttr(name: String): Boolean = false

    fun replaceWith(newNode: HtmlNode) {
        val p = parent ?: return
        val idx = p._childNodes.indexOf(this)
        if (idx >= 0) {
            p._childNodes[idx] = newNode
            newNode.parent = p
            this.parent = null
        }
    }

    fun remove() {
        val p = parent ?: return
        p._childNodes.remove(this)
        this.parent = null
    }

    internal fun appendChild(child: HtmlNode) {
        child.parent?._childNodes?.remove(child)
        child.parent = this
        _childNodes.add(child)
    }

    internal fun clearChildren() {
        for (c in _childNodes) c.parent = null
        _childNodes.clear()
    }

    fun unwrap() {
        val p = parent ?: return
        val idx = p._childNodes.indexOf(this)
        if (idx >= 0) {
            p._childNodes.removeAt(idx)
            for ((i, child) in _childNodes.withIndex()) {
                child.parent = p
                p._childNodes.add(idx + i, child)
            }
            this.parent = null
            _childNodes.clear()
        }
    }
}

class HtmlTextNode(private var text: String) : HtmlNode() {
    override fun nodeName(): String = "#text"
    override fun outerHtml(): String = text
    fun text(): String = text
    fun text(value: String) {
        text = value
    }
}

class HtmlElement(private var tag: String) : HtmlNode() {
    private val attrs = linkedMapOf<String, String>()

    override fun nodeName(): String = tag

    fun tagName(): String = tag
    fun tagName(name: String) { tag = name.lowercase() }

    override fun outerHtml(): String {
        val sb = StringBuilder()
        sb.append('<').append(tag)
        for ((k, v) in attrs) {
            sb.append(' ').append(k).append("=\"")
                .append(v.replace("&", "&amp;").replace("\"", "&quot;"))
                .append('"')
        }
        if (childNodes().isEmpty() && tag in VOID_ELEMENTS) {
            sb.append(" />")
        } else {
            sb.append('>')
            for (c in childNodes()) sb.append(c.outerHtml())
            sb.append("</").append(tag).append('>')
        }
        return sb.toString()
    }

    override fun toString(): String = outerHtml()

    override fun attr(name: String): String = attrs[name.lowercase()] ?: ""
    override fun hasAttr(name: String): Boolean = attrs.containsKey(name.lowercase())
    fun attr(name: String, value: String) { attrs[name.lowercase()] = value }
    internal fun setAttr(name: String, value: String) { attrs[name.lowercase()] = value }
    fun removeAttr(name: String) { attrs.remove(name.lowercase()) }

    fun attributes(): List<HtmlAttribute> = attrs.entries.map { HtmlAttribute(it.key, it.value) }

    fun `val`(): String = attr("value")
    fun `val`(value: String) { attr("value", value) }

    fun id(): String = attr("id")
    fun id(value: String) { attr("id", value) }

    fun className(): String = attr("class")
    fun classNames(): Set<String> {
        val cls = attr("class")
        return if (cls.isBlank()) emptySet() else cls.split(Regex("\\s+")).toSet()
    }
    fun classNames(classes: Set<String>) {
        if (classes.isEmpty()) removeAttr("class") else attr("class", classes.joinToString(" "))
    }
    fun addClass(cls: String) {
        val classes = classNames().toMutableSet()
        classes.add(cls)
        classNames(classes)
    }
    fun removeClass(cls: String) {
        val classes = classNames().toMutableSet()
        classes.remove(cls)
        classNames(classes)
    }

    fun children(): List<HtmlElement> = childNodes().filterIsInstance<HtmlElement>()
    fun childrenSize(): Int = children().size
    fun child(index: Int): HtmlElement = children()[index]

    fun wholeText(): String {
        val sb = StringBuilder()
        fun collect(n: HtmlNode) {
            if (n is HtmlTextNode) sb.append(n.text())
            else for (c in n.childNodes()) collect(c)
        }
        for (c in childNodes()) collect(c)
        return sb.toString()
    }

    fun text(): String = wholeText().replace(Regex("\\s+"), " ").trim()

    fun text(value: String) {
        clearChildren()
        appendChild(HtmlTextNode(value))
    }

    fun html(): String = childNodes().joinToString("") { it.outerHtml() }

    fun html(value: String) {
        clearChildren()
        val parsed = HtmlParser.parse(value)
        for (child in parsed.childNodes().toList()) {
            child.parent = this
            _childNodes.add(child)
        }
    }

    fun createElement(tagName: String): HtmlElement = HtmlElement(tagName.lowercase())

    fun body(): HtmlElement? = getElementsByTag("body").firstOrNull() ?: this
    fun head(): HtmlElement? = getElementsByTag("head").firstOrNull()
    fun title(): String = getElementsByTag("title").firstOrNull()?.wholeText()?.trim() ?: ""
    fun charset(): String {
        for (meta in getElementsByTag("meta")) {
            val cs = meta.attr("charset")
            if (cs.isNotEmpty()) return cs
        }
        for (meta in getElementsByTag("meta")) {
            if (meta.attr("http-equiv").equals("Content-Type", ignoreCase = true)) {
                val content = meta.attr("content")
                val idx = content.indexOf("charset=", ignoreCase = true)
                if (idx >= 0) return content.substring(idx + 8).trim()
            }
        }
        return "UTF-8"
    }

    fun getElementById(id: String): HtmlElement? {
        for (child in childNodes()) {
            if (child is HtmlElement) {
                if (child.attr("id") == id) return child
                child.getElementById(id)?.let { return it }
            }
        }
        return null
    }

    fun getElementsByTag(tagName: String): List<HtmlElement> {
        val result = mutableListOf<HtmlElement>()
        val target = tagName.lowercase()
        fun search(node: HtmlNode) {
            for (child in node.childNodes()) {
                if (child is HtmlElement) {
                    if (child.tag == target) result.add(child)
                    search(child)
                }
            }
        }
        search(this)
        return result
    }

    fun select(selector: String): List<HtmlElement> {
        val result = linkedSetOf<HtmlElement>()
        for (part in selector.split(",").map { it.trim() }) {
            when {
                part.startsWith("#") -> {
                    getElementById(part.substring(1))?.let { result.add(it) }
                }
                part.contains("[") -> {
                    val match = Regex("""^(\*|[\w-]+)\[([\w-]+)(?:([~|^$*]?=)(.+?))?\]$""").find(part)
                    if (match != null) {
                        val (selTag, attrName, op, attrValue) = match.destructured
                        val elements = if (selTag == "*" || selTag.isEmpty()) getAllDescendants() else getElementsByTag(selTag)
                        val cleanValue = attrValue.trim('"', '\'')
                        for (elem in elements) {
                            if (op.isEmpty()) {
                                if (elem.hasAttr(attrName)) result.add(elem)
                            } else if (op == "=") {
                                if (elem.attr(attrName) == cleanValue) result.add(elem)
                            } else {
                                val v = elem.attr(attrName)
                                val matches = when (op) {
                                    "~=" -> v.split(Regex("\\s+")).contains(cleanValue)
                                    "|=" -> v == cleanValue || v.startsWith("$cleanValue-")
                                    "^=" -> v.startsWith(cleanValue)
                                    "$=" -> v.endsWith(cleanValue)
                                    "*=" -> v.contains(cleanValue)
                                    else -> false
                                }
                                if (matches) result.add(elem)
                            }
                        }
                    }
                }
                part == "*" -> result.addAll(getAllDescendants())
                else -> result.addAll(getElementsByTag(part))
            }
        }
        return result.toList()
    }

    fun selectFirst(selector: String): HtmlElement? = select(selector).firstOrNull()

    private fun getAllDescendants(): List<HtmlElement> {
        val result = mutableListOf<HtmlElement>()
        fun search(node: HtmlNode) {
            for (child in node.childNodes()) {
                if (child is HtmlElement) {
                    result.add(child)
                    search(child)
                }
            }
        }
        search(this)
        return result
    }

    companion object {
        private val VOID_ELEMENTS = setOf(
            "area", "base", "br", "col", "embed", "hr", "img", "input",
            "link", "meta", "param", "source", "track", "wbr",
        )
    }
}

data class HtmlAttribute(val key: String, val value: String)

internal object HtmlParser {
    private val VOID_ELEMENTS = setOf(
        "area", "base", "br", "col", "embed", "hr", "img", "input",
        "link", "meta", "param", "source", "track", "wbr",
    )

    fun parse(html: String): HtmlElement {
        val root = HtmlElement("html-root")
        val stack = ArrayDeque<HtmlElement>()
        stack.addLast(root)
        var pos = 0
        val sb = StringBuilder()

        while (pos < html.length) {
            when {
                html.startsWith("<!--", pos) -> {
                    flushText(stack.last(), sb)
                    val end = html.indexOf("-->", pos + 4)
                    pos = if (end < 0) html.length else end + 3
                }
                html.startsWith("<![CDATA[", pos) -> {
                    val end = html.indexOf("]]>", pos + 9)
                    val text = if (end < 0) html.substring(pos + 9) else html.substring(pos + 9, end)
                    sb.append(text)
                    pos = if (end < 0) html.length else end + 3
                }
                html.startsWith("<!", pos) || html.startsWith("<?", pos) -> {
                    flushText(stack.last(), sb)
                    val end = html.indexOf('>', pos)
                    pos = if (end < 0) html.length else end + 1
                }
                html.startsWith("</", pos) -> {
                    flushText(stack.last(), sb)
                    val end = html.indexOf('>', pos + 2)
                    if (end < 0) {
                        pos = html.length
                    } else {
                        val tagName = html.substring(pos + 2, end).trim().lowercase()
                        val matchIndex = (stack.size - 1 downTo 1).firstOrNull { stack[it].nodeName() == tagName }
                        if (matchIndex != null) {
                            while (stack.size > matchIndex) {
                                stack.removeLast()
                            }
                        }
                        pos = end + 1
                    }
                }
                html[pos] == '<' && pos + 1 < html.length && html[pos + 1].isLetter() -> {
                    flushText(stack.last(), sb)
                    val tagResult = parseStartTag(html, pos)
                    if (tagResult != null) {
                        val (elem, newPos, selfClosed) = tagResult
                        pos = newPos
                        stack.last().appendChild(elem)
                        if (!selfClosed && elem.nodeName() !in VOID_ELEMENTS) {
                            stack.addLast(elem)
                        }
                    } else {
                        sb.append('<')
                        pos++
                    }
                }
                html[pos] == '<' -> {
                    sb.append('<')
                    pos++
                }
                else -> {
                    val nextLt = html.indexOf('<', pos)
                    if (nextLt < 0) {
                        sb.append(decodeEntities(html.substring(pos)))
                        pos = html.length
                    } else {
                        sb.append(decodeEntities(html.substring(pos, nextLt)))
                        pos = nextLt
                    }
                }
            }
        }
        flushText(stack.last(), sb)
        return root
    }

    private fun flushText(parent: HtmlElement, sb: StringBuilder) {
        if (sb.isNotEmpty()) {
            parent.appendChild(HtmlTextNode(sb.toString()))
            sb.clear()
        }
    }

    private fun parseStartTag(html: String, start: Int): Triple<HtmlElement, Int, Boolean>? {
        var pos = start + 1
        val nameEnd = findNameEnd(html, pos)
        if (nameEnd == pos) return null
        val tagName = html.substring(pos, nameEnd).lowercase()
        if (tagName.isEmpty() || !tagName[0].isLetter()) return null
        pos = nameEnd

        val elem = HtmlElement(tagName)
        var selfClosed = false

        while (pos < html.length) {
            while (pos < html.length && html[pos].isWhitespace()) pos++
            if (pos >= html.length || html[pos] == '>' || html[pos] == '/') break

            val attrNameStart = pos
            while (pos < html.length && html[pos] != '=' && html[pos] != '>' && html[pos] != '/' && !html[pos].isWhitespace()) pos++
            if (pos == attrNameStart) {
                pos++
                continue
            }
            val attrName = html.substring(attrNameStart, pos).lowercase()

            while (pos < html.length && html[pos].isWhitespace()) pos++

            if (pos < html.length && html[pos] == '=') {
                pos++
                while (pos < html.length && html[pos].isWhitespace()) pos++
                if (pos < html.length && (html[pos] == '"' || html[pos] == '\'')) {
                    val quote = html[pos++]
                    val valueEnd = html.indexOf(quote, pos)
                    if (valueEnd < 0) {
                        elem.setAttr(attrName, decodeEntities(html.substring(pos)))
                        pos = html.length
                    } else {
                        elem.setAttr(attrName, decodeEntities(html.substring(pos, valueEnd)))
                        pos = valueEnd + 1
                    }
                } else {
                    val valueStart = pos
                    while (pos < html.length && html[pos] != '>' && !html[pos].isWhitespace()) pos++
                    elem.setAttr(attrName, decodeEntities(html.substring(valueStart, pos)))
                }
            } else {
                elem.setAttr(attrName, "")
            }
        }

        if (pos < html.length && html[pos] == '/') {
            selfClosed = true
            pos++
        }
        if (pos < html.length && html[pos] == '>') pos++

        return Triple(elem, pos, selfClosed)
    }

    private fun findNameEnd(html: String, start: Int): Int {
        var i = start
        while (i < html.length) {
            val c = html[i]
            if (c.isWhitespace() || c == '>' || c == '/' || c == '=') break
            i++
        }
        return i
    }

    private fun decodeEntities(s: String): String {
        if ('&' !in s) return s
        return s.replace(Regex("&(#x[0-9a-fA-F]+|#\\d+|[a-zA-Z][a-zA-Z0-9]*);")) { mr ->
            val ent = mr.value
            try {
                when {
                    ent.startsWith("&#x") || ent.startsWith("&#X") ->
                        ent.substring(3, ent.length - 1).toInt(16).toChar().toString()
                    ent.startsWith("&#") ->
                        ent.substring(2, ent.length - 1).toInt().toChar().toString()
                    else -> {
                        val name = ent.substring(1, ent.length - 1)
                        when (name) {
                            "amp" -> "&"
                            "lt" -> "<"
                            "gt" -> ">"
                            "quot" -> "\""
                            "apos" -> "'"
                            "nbsp" -> "\u00A0"
                            "copy" -> "\u00A9"
                            "reg" -> "\u00AE"
                            "trade" -> "\u2122"
                            "mdash" -> "\u2014"
                            "ndash" -> "\u2013"
                            "hellip" -> "\u2026"
                            "lsquo" -> "\u2018"
                            "rsquo" -> "\u2019"
                            "ldquo" -> "\u201C"
                            "rdquo" -> "\u201D"
                            "laquo" -> "\u00AB"
                            "raquo" -> "\u00BB"
                            "deg" -> "\u00B0"
                            "plusmn" -> "\u00B1"
                            "times" -> "\u00D7"
                            "divide" -> "\u00F7"
                            "frac12" -> "\u00BD"
                            "frac14" -> "\u00BC"
                            "frac34" -> "\u00BE"
                            "sup2" -> "\u00B2"
                            "sup3" -> "\u00B3"
                            "micro" -> "\u00B5"
                            "para" -> "\u00B6"
                            "middot" -> "\u00B7"
                            "cent" -> "\u00A2"
                            "pound" -> "\u00A3"
                            "yen" -> "\u00A5"
                            "euro" -> "\u20AC"
                            "sect" -> "\u00A7"
                            "bull" -> "\u2022"
                            "dagger" -> "\u2020"
                            "Dagger" -> "\u2021"
                            "permil" -> "\u2030"
                            "prime" -> "\u2032"
                            "Prime" -> "\u2033"
                            "infin" -> "\u221E"
                            "ne" -> "\u2260"
                            "le" -> "\u2264"
                            "ge" -> "\u2265"
                            "larr" -> "\u2190"
                            "uarr" -> "\u2191"
                            "rarr" -> "\u2192"
                            "darr" -> "\u2193"
                            "harr" -> "\u2194"
                            "spades" -> "\u2660"
                            "clubs" -> "\u2663"
                            "hearts" -> "\u2665"
                            "diams" -> "\u2666"
                            "alpha" -> "\u03B1"
                            "beta" -> "\u03B2"
                            "gamma" -> "\u03B3"
                            "delta" -> "\u03B4"
                            "epsilon" -> "\u03B5"
                            "pi" -> "\u03C0"
                            "omega" -> "\u03C9"
                            "Alpha" -> "\u0391"
                            "Beta" -> "\u0392"
                            "Gamma" -> "\u0393"
                            "Delta" -> "\u0394"
                            "Omega" -> "\u03A9"
                            else -> ent
                        }
                    }
                }
            } catch (e: Exception) {
                ent
            }
        }
    }
}
