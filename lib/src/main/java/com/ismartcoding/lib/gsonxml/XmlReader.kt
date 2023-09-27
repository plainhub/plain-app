package com.ismartcoding.lib.gsonxml

import com.google.gson.JsonSyntaxException
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.IOException
import java.io.Reader

class XmlReader(`in`: Reader, val options: Options) : JsonReader(`in`) {
    /**
     * Scope.
     */
    internal enum class Scope(
        /**
         * Inside array flag.
         */
        val insideArray: Boolean,
    ) {
        /**
         * We are inside an object. Next token should be [JsonToken.NAME] or [JsonToken.END_OBJECT].
         */
        INSIDE_OBJECT(false),

        /**
         * We are inside an array. Next token should be [JsonToken.BEGIN_OBJECT] or [JsonToken.END_ARRAY].
         */
        INSIDE_ARRAY(true),

        /**
         * We are inside automatically added array. Next token should be [JsonToken.BEGIN_OBJECT] or [JsonToken.END_ARRAY].
         */
        INSIDE_EMBEDDED_ARRAY(true),

        /**
         * We are inside primitive embedded array. Child scope can be #PRIMITIVE_VALUE only.
         */
        INSIDE_PRIMITIVE_EMBEDDED_ARRAY(true),

        /**
         * We are inside primitive array. Child scope can be #PRIMITIVE_VALUE only.
         */
        INSIDE_PRIMITIVE_ARRAY(true),

        /**
         * We are inside primitive value. Next token should be [JsonToken.STRING] or [JsonToken.END_ARRAY].
         */
        PRIMITIVE_VALUE(false),

        /**
         * New start tag met, we returned [JsonToken.NAME]. Object, array, or value can go next.
         */
        NAME(false),
    }

    /**
     * XML parser.
     */
    private val xmlParser = XmlPullParserFactory.newInstance().newPullParser()

    /**
     * Tokens pool.
     */
    private val tokensPool =
        RefsPool(
            object : Creator<TokenRef> {
                override fun create(): TokenRef {
                    return TokenRef()
                }
            },
        )

    /**
     * Values pool.
     */
    private val valuesPool =
        RefsPool(
            object : Creator<ValueRef> {
                override fun create(): ValueRef {
                    return ValueRef()
                }
            },
        )

    /**
     * Tokens queue.
     */
    private var tokensQueue: TokenRef? = null
    private var tokensQueueStart: TokenRef? = null

    /**
     * Values queue.
     */
    private var valuesQueue: ValueRef? = null
    private var valuesQueueStart: ValueRef? = null
    private var expectedToken: JsonToken? = null

    /**
     * State.
     */
    private var endReached = false
    private var firstStart = true
    private var lastTextWhiteSpace = false

    /**
     * Stack of scopes.
     */
    private val scopeStack = Stack<Scope>()

    /**
     * Stack of last closed tags.
     */
    private val closeStack = Stack<ClosedTag>()

    /**
     * Current token.
     */
    private var token: JsonToken? = null

    /**
     * Counter for "$".
     */
    private var textNameCounter = 0

    /**
     * Skipping state flag.
     */
    private var skipping = false

    /**
     * Last XML token info.
     */
    private val xmlToken = XmlTokenInfo()

    /**
     * Attributes.
     */
    private val attributes = AttributesData(10)

    init {
        xmlToken.type = IGNORE
        try {
            xmlParser.setInput(`in`)
            xmlParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, options.namespaces)
        } catch (e: XmlPullParserException) {
            throw RuntimeException(e)
        }
    }

    private fun dump(): CharSequence {
        return StringBuilder()
            .append("Scopes: ").append(scopeStack).append('\n')
            .append("Closed tags: ").append(closeStack).append('\n')
            .append("Token: ").append(token).append('\n')
            .append("Tokens queue: ").append(tokensQueueStart).append('\n')
            .append("Values queue: ").append(valuesQueueStart).append('\n')
    }

    override fun toString(): String {
        return """
            --- XmlReader ---
            ${dump()}
            """.trimIndent()
    }

    private fun peekNextToken(): JsonToken? {
        return if (tokensQueueStart != null) tokensQueueStart!!.token else null
    }

    private fun nextToken(): JsonToken? {
        val ref = tokensQueueStart ?: return JsonToken.END_DOCUMENT
        tokensQueueStart = ref.next
        if (ref == tokensQueue) {
            tokensQueue = null
        }
        tokensPool.release(ref)
        return ref.token
    }

    private fun nextValue(): ValueRef {
        val ref = valuesQueueStart ?: throw IllegalStateException("No value can be given")
        if (ref == valuesQueue) {
            valuesQueue = null
        }
        valuesPool.release(ref)
        valuesQueueStart = ref.next
        return ref
    }

    @Throws(IOException::class)
    private fun expect(token: JsonToken) {
        val actual = peek()
        this.token = null
        check(actual == token) {
            """$token expected, but met $actual
${dump()}"""
        }
    }

    @Throws(IOException::class)
    override fun beginObject() {
        expectedToken = JsonToken.BEGIN_OBJECT
        expect(expectedToken!!)
    }

    @Throws(IOException::class)
    override fun endObject() {
        expectedToken = JsonToken.END_OBJECT
        expect(expectedToken!!)
    }

    @Throws(IOException::class)
    override fun beginArray() {
        expectedToken = JsonToken.BEGIN_ARRAY
        expect(expectedToken!!)
    }

    @Throws(IOException::class)
    override fun endArray() {
        expectedToken = JsonToken.END_ARRAY
        expect(expectedToken!!)
    }

    @Throws(IOException::class)
    override fun hasNext(): Boolean {
        peek()
        return token != JsonToken.END_OBJECT && token != JsonToken.END_ARRAY
    }

    @Throws(IOException::class)
    override fun skipValue() {
        skipping = true
        try {
            var count = 0
            do {
                val token = peek()
                if (token == JsonToken.BEGIN_ARRAY || token == JsonToken.BEGIN_OBJECT) {
                    count++
                } else if (token == JsonToken.END_ARRAY || token == JsonToken.END_OBJECT) {
                    count--
                } else if (valuesQueue != null) {
                    nextValue() // pull ignored value
                }
                this.token = null // advance
            } while (count != 0)
        } finally {
            skipping = false
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun adaptCurrentToken() {
        if (token == expectedToken) {
            return
        }
        if (expectedToken != JsonToken.BEGIN_ARRAY) {
            return
        }
        when (token) {
            JsonToken.BEGIN_OBJECT -> {
                token = JsonToken.BEGIN_ARRAY
                val lastScope = scopeStack.peek()
                if (peekNextToken() == JsonToken.NAME) {
                    if (options.sameNameList) {
                        // we are replacing current scope with INSIDE_EMBEDDED_ARRAY
                        scopeStack.cleanup(1)

                        // use it as a field
                        pushToQueue(JsonToken.BEGIN_OBJECT)
                        scopeStack.push(Scope.INSIDE_EMBEDDED_ARRAY)
                        scopeStack.push(Scope.INSIDE_OBJECT)
                        if (lastScope == Scope.NAME) {
                            scopeStack.push(Scope.NAME)
                        }
                    } else {
                        // ignore name
                        nextToken()
                        nextValue()
                        var pushPos = scopeStack.size()
                        if (options.primitiveArrays && peekNextToken() == null) {
                            // pull what next: it can be either primitive or object
                            fillQueues(true)
                        }
                        pushPos = scopeStack.cleanup(3, pushPos)
                        if (options.primitiveArrays && peekNextToken() == JsonToken.STRING) {
                            // primitive
                            scopeStack.pushAt(pushPos, Scope.INSIDE_PRIMITIVE_ARRAY)
                        } else {
                            // object (if array it will be adapted again)
                            scopeStack.pushAt(pushPos, Scope.INSIDE_ARRAY)
                            if (scopeStack.size() <= pushPos + 1 || scopeStack[pushPos + 1] != Scope.INSIDE_OBJECT) {
                                scopeStack.pushAt(pushPos + 1, Scope.INSIDE_OBJECT)
                            }
                            if (peekNextToken() != JsonToken.BEGIN_OBJECT) {
                                pushToQueue(JsonToken.BEGIN_OBJECT)
                            }
                        }
                    }
                }
            }
            JsonToken.STRING -> {
                token = JsonToken.BEGIN_ARRAY
                if (options.sameNameList) {
                    if (options.primitiveArrays) {
                        // we have array of primitives
                        pushToQueue(JsonToken.STRING)
                        scopeStack.push(Scope.INSIDE_PRIMITIVE_EMBEDDED_ARRAY)
                    } else {
                        // pass value as a text node inside of an object
                        val value = nextValue().value
                        pushToQueue(JsonToken.END_OBJECT)
                        pushToQueue(JsonToken.STRING)
                        pushToQueue(JsonToken.NAME)
                        pushToQueue(JsonToken.BEGIN_OBJECT)
                        pushToQueue(value)
                        pushToQueue("$")
                        scopeStack.push(Scope.INSIDE_EMBEDDED_ARRAY)
                    }
                } else {
                    // we have an empty list
                    pushToQueue(JsonToken.END_ARRAY)
                    if (valuesQueueStart != null) {
                        nextValue()
                    }
                }
            }
            else -> {}
        }
    }

    @Throws(IOException::class)
    override fun peek(): JsonToken {
        if (expectedToken == null && firstStart) {
            return JsonToken.BEGIN_OBJECT
        }
        if (token != null) {
            try {
                adaptCurrentToken()
            } catch (e: XmlPullParserException) {
                throw JsonSyntaxException("XML parsing exception", e)
            }
            expectedToken = null
            return token!!
        }
        return try {
            fillQueues(false)
            expectedToken = null
            nextToken().also { token = it }!!
        } catch (e: XmlPullParserException) {
            throw JsonSyntaxException("XML parsing exception", e)
        }
    }

    @Throws(IOException::class)
    override fun nextString(): String {
        expect(JsonToken.STRING)
        return nextValue().value!!
    }

    @Throws(IOException::class)
    override fun nextBoolean(): Boolean {
        expect(JsonToken.BOOLEAN)
        val value = nextValue().value
        if ("true".equals(value, ignoreCase = true)) {
            return true
        }
        if ("false".equals(value, ignoreCase = true)) {
            return true
        }
        throw IOException("Cannot parse <$value> to boolean")
    }

    @Throws(IOException::class)
    override fun nextDouble(): Double {
        expect(JsonToken.STRING)
        return nextValue().value!!.toDouble()
    }

    @Throws(IOException::class)
    override fun nextInt(): Int {
        expect(JsonToken.STRING)
        return nextValue().value!!.toInt()
    }

    @Throws(IOException::class)
    override fun nextLong(): Long {
        expect(JsonToken.STRING)
        return nextValue().value!!.toLong()
    }

    @Throws(IOException::class)
    override fun nextName(): String {
        expectedToken = JsonToken.NAME
        expect(JsonToken.NAME)
        return nextValue().value!!
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun nextXmlInfo(): XmlTokenInfo {
        val type = xmlParser!!.next()
        val info = xmlToken
        info.clear()
        when (type) {
            XmlPullParser.START_TAG -> {
                info.type = START_TAG
                info.name = xmlParser.name
                info.ns = xmlParser.namespace
                val aCount = xmlParser.attributeCount
                if (aCount > 0) {
                    attributes.fill(xmlParser)
                    info.attributesData = attributes
                }
            }
            XmlPullParser.END_TAG -> {
                info.type = END_TAG
                info.name = xmlParser.name
                info.ns = xmlParser.namespace
            }
            XmlPullParser.TEXT -> {
                val text = xmlParser.text.trim { it <= ' ' }
                if (text.length == 0) {
                    lastTextWhiteSpace = true
                    info.type = IGNORE
                    return info
                }
                lastTextWhiteSpace = false
                info.type = VALUE
                info.value = text
            }
            XmlPullParser.END_DOCUMENT -> {
                endReached = true
                info.type = IGNORE
            }
            else -> info.type = IGNORE
        }
        return info
    }

    private fun addToQueue(token: JsonToken?) {
        val tokenRef = tokensPool.get()!!
        tokenRef.token = token
        tokenRef.next = null
        if (tokensQueue == null) {
            tokensQueue = tokenRef
            tokensQueueStart = tokenRef
        } else {
            tokensQueue!!.next = tokenRef
            tokensQueue = tokenRef
        }
    }

    private fun pushToQueue(token: JsonToken) {
        val tokenRef = tokensPool.get()!!
        tokenRef.token = token
        tokenRef.next = null
        if (tokensQueueStart == null) {
            tokensQueueStart = tokenRef
            tokensQueue = tokenRef
        } else {
            tokenRef.next = tokensQueueStart
            tokensQueueStart = tokenRef
        }
    }

    private fun addToQueue(value: String?) {
        val valueRef = valuesPool.get()!!
        valueRef.value = value!!.trim { it <= ' ' }
        valueRef.next = null
        if (valuesQueue == null) {
            valuesQueue = valueRef
            valuesQueueStart = valueRef
        } else {
            valuesQueue!!.next = valueRef
            valuesQueue = valueRef
        }
    }

    private fun pushToQueue(value: String?) {
        val valueRef = valuesPool.get()!!
        valueRef.value = value
        valueRef.next = null
        if (valuesQueueStart == null) {
            valuesQueue = valueRef
            valuesQueueStart = valueRef
        } else {
            valueRef.next = valuesQueueStart
            valuesQueueStart = valueRef
        }
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun addToQueue(attrData: AttributesData?) {
        val count = attrData!!.count
        for (i in 0 until count) {
            addToQueue(JsonToken.NAME)
            addToQueue("@" + attrData.getName(i))
            addToQueue(JsonToken.STRING)
            addToQueue(attrData.values[i])
        }
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun fillQueues(force: Boolean) {
        var mustRepeat = force
        while (tokensQueue == null && !endReached || mustRepeat) {
            val xml = nextXmlInfo()
            if (endReached) {
                if (!options.skipRoot) {
                    addToQueue(JsonToken.END_OBJECT)
                }
                break
            }
            if (xml.type == IGNORE) {
                continue
            }
            mustRepeat = false
            when (xml.type) {
                START_TAG ->
                    if (firstStart) {
                        firstStart = false
                        processRoot(xml)
                    } else {
                        processStart(xml)
                    }
                VALUE -> mustRepeat = processText(xml)
                END_TAG -> processEnd(xml)
                else -> {}
            }
            if (!mustRepeat && skipping) {
                break
            }
        }
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun processRoot(xml: XmlTokenInfo) {
        if (!options.skipRoot) {
            addToQueue(expectedToken)
            scopeStack.push(Scope.INSIDE_OBJECT)
            processStart(xml)
        } else if (xml.attributesData != null) {
            addToQueue(JsonToken.BEGIN_OBJECT)
            scopeStack.push(Scope.INSIDE_OBJECT)
            addToQueue(xml.attributesData)
        } else {
            when (expectedToken) {
                JsonToken.BEGIN_OBJECT -> {
                    addToQueue(JsonToken.BEGIN_OBJECT)
                    scopeStack.push(Scope.INSIDE_OBJECT)
                }
                JsonToken.BEGIN_ARRAY -> {
                    addToQueue(JsonToken.BEGIN_ARRAY)
                    scopeStack.push(if (options.rootArrayPrimitive) Scope.INSIDE_PRIMITIVE_ARRAY else Scope.INSIDE_ARRAY)
                }
                else -> throw IllegalStateException("First expectedToken=$expectedToken (not begin_object/begin_array)")
            }
        }
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun processStart(xml: XmlTokenInfo) {
        var processTagName = true
        var lastScope = scopeStack.peek()
        if (options.sameNameList && lastScope!!.insideArray && closeStack.size() > 0) {
            val lastClosedInfo = closeStack.peek()
            if (lastClosedInfo!!.depth == xmlParser!!.depth) {
                val currentName = if (options.namespaces) xml.getName(xmlParser) else xml.name
                if (currentName != lastClosedInfo.name) {
                    // close the previous array
                    addToQueue(JsonToken.END_ARRAY)
                    fixScopeStack()
                    lastScope = scopeStack.peek()
                }
            }
        }
        when (lastScope) {
            Scope.INSIDE_PRIMITIVE_ARRAY, Scope.INSIDE_PRIMITIVE_EMBEDDED_ARRAY -> {
                processTagName = false
                scopeStack.push(Scope.PRIMITIVE_VALUE)
            }
            Scope.INSIDE_EMBEDDED_ARRAY, Scope.INSIDE_ARRAY -> {
                processTagName = false
                addToQueue(JsonToken.BEGIN_OBJECT)
                scopeStack.push(Scope.INSIDE_OBJECT)
            }
            Scope.NAME -> {
                addToQueue(JsonToken.BEGIN_OBJECT)
                scopeStack.push(Scope.INSIDE_OBJECT)
            }
            else -> {}
        }
        if (processTagName) { // ignore tag name inside the array
            scopeStack.push(Scope.NAME)
            addToQueue(JsonToken.NAME)
            addToQueue(xml.getName(xmlParser))
            lastTextWhiteSpace = true // if tag is closed immediately we'll add empty value to the queue
        }
        if (xml.attributesData != null) {
            lastScope = scopeStack.peek()
            check(lastScope != Scope.PRIMITIVE_VALUE) { "Attributes data in primitive scope" }
            if (lastScope == Scope.NAME) {
                addToQueue(JsonToken.BEGIN_OBJECT)
                scopeStack.push(Scope.INSIDE_OBJECT)
            }
            // attributes, as fields
            addToQueue(xml.attributesData)
        }
    }

    private fun processText(xml: XmlTokenInfo): Boolean {
        return when (scopeStack.peek()) {
            Scope.PRIMITIVE_VALUE -> {
                addTextToQueue(xml.value, false)
                false
            }
            Scope.NAME -> {
                addTextToQueue(xml.value, true)
                true
            }
            Scope.INSIDE_OBJECT -> {
                var name = "$"
                if (textNameCounter > 0) {
                    name += textNameCounter
                }
                textNameCounter++
                addToQueue(JsonToken.NAME)
                addToQueue(name)
                addTextToQueue(xml.value, false)
                false
            }
            else -> throw JsonSyntaxException("Cannot process text '" + xml.value + "' inside scope " + scopeStack.peek())
        }
    }

    private fun addTextToQueue(
        value: String?,
        canBeAppended: Boolean,
    ) {
        if (canBeAppended && tokensQueue != null && tokensQueue!!.token == JsonToken.STRING) {
            if (value!!.length > 0) {
                valuesQueue!!.value += " $value"
            }
        } else {
            addToQueue(JsonToken.STRING)
            addToQueue(value)
        }
    }

    private fun fixScopeStack() {
        scopeStack.fix(Scope.NAME)
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun processEnd(xml: XmlTokenInfo) {
        when (scopeStack.peek()) {
            Scope.INSIDE_OBJECT -> {
                addToQueue(JsonToken.END_OBJECT)
                textNameCounter = 0
                fixScopeStack()
            }
            Scope.PRIMITIVE_VALUE -> scopeStack.drop()
            Scope.INSIDE_PRIMITIVE_EMBEDDED_ARRAY, Scope.INSIDE_EMBEDDED_ARRAY -> {
                addToQueue(JsonToken.END_ARRAY)
                addToQueue(JsonToken.END_OBJECT)
                fixScopeStack() // auto-close embedded array
                fixScopeStack() // close current object scope
            }
            Scope.INSIDE_PRIMITIVE_ARRAY, Scope.INSIDE_ARRAY -> {
                addToQueue(JsonToken.END_ARRAY)
                fixScopeStack()
            }
            Scope.NAME -> {
                if (lastTextWhiteSpace) {
                    addTextToQueue("", true)
                }
                fixScopeStack()
            }
            else -> {}
        }
        if (options.sameNameList) {
            val stackSize = xmlParser!!.depth
            val name = if (options.namespaces) xml.getName(xmlParser) else xml.name
            val closeStack = closeStack
            while (closeStack.size() > 0 && closeStack.peek()!!.depth > stackSize) {
                closeStack.drop()
            }
            if (closeStack.size() == 0 || closeStack.peek()!!.depth < stackSize) {
                closeStack.push(ClosedTag(stackSize, name))
            } else {
                closeStack.peek()!!.name = name
            }
        }
    }

    private class TokenRef {
        var token: JsonToken? = null
        var next: TokenRef? = null

        override fun toString(): String {
            return token.toString() + ", " + next
        }
    }

    private class ValueRef {
        var value: String? = null
        var next: ValueRef? = null

        override fun toString(): String {
            return "$value, $next"
        }
    }

    private class XmlTokenInfo {
        var type = 0
        var name: String? = null
        var value: String? = null
        var ns: String? = null
        var attributesData: AttributesData? = null

        fun clear() {
            type = IGNORE
            name = null
            value = null
            ns = null
            attributesData = null
        }

        override fun toString(): String {
            return (
                "xml " +
                    (
                        if (type == START_TAG) {
                            "start"
                        } else if (type == END_TAG) {
                            "end"
                        } else {
                            "value"
                        }
                        ) +
                    " <" + ns + ":" + name + ">=" + value + if (attributesData != null) ", $attributesData" else ""
            )
        }

        @Throws(IOException::class, XmlPullParserException::class)
        fun getName(parser: XmlPullParser?): String? {
            return nameWithNs(name, ns, parser)
        }
    }

    inner class AttributesData(capacity: Int) {
        lateinit var names: Array<String?>
        lateinit var values: Array<String?>
        lateinit var ns: Array<String?>
        var count = 0

        init {
            createArrays(capacity)
        }

        private fun createArrays(capacity: Int) {
            names = arrayOfNulls(capacity)
            values = arrayOfNulls(capacity)
            ns = arrayOfNulls(capacity)
        }

        fun fill(parser: XmlPullParser) {
            val aCount = parser.attributeCount
            if (aCount > names.size) {
                createArrays(aCount)
            }
            count = aCount
            for (i in 0 until aCount) {
                names[i] = parser.getAttributeName(i)
                if (options.namespaces) {
                    ns[i] = parser.getAttributePrefix(i)
                }
                values[i] = parser.getAttributeValue(i)
            }
        }

        @Throws(IOException::class, XmlPullParserException::class)
        fun getName(i: Int): String? {
            return nameWithNs(names[i], ns[i], null)
        }
    }

    /**
     * Xml reader options.
     */
    class Options {
        /**
         * Options.
         */
        var primitiveArrays = false
        var skipRoot = false
        var sameNameList = false
        var namespaces = false
        var rootArrayPrimitive = false
    }

    /**
     * Closed tag data.
     */
    private class ClosedTag(var depth: Int, var name: String?) {
        override fun toString(): String {
            return "'$name'/$depth"
        }
    }

    /**
     * Pool for
     */
    private class RefsPool<T>(
        /**
         * Factory instance.
         */
        private val creator: Creator<T>,
    ) {
        /**
         * Pool.
         */
        private val store = arrayOfNulls<Any>(SIZE)

        /**
         * Store length.
         */
        private var len = 0

        /**
         * Get value from pool or create it.
         */
        fun get(): T? {
            return if (len == 0) {
                creator.create()
            } else {
                store[--len] as T?
            }
        }

        /**
         * Return value to the pool.
         */
        fun release(obj: T) {
            if (len < SIZE) {
                store[len++] = obj
            }
        }

        companion object {
            /**
             * Max count.
             */
            private const val SIZE = 32
        }
    }

    /**
     * Factory.
     */
    private interface Creator<T> {
        fun create(): T
    }

    companion object {
        /**
         * Internal token type.
         */
        private const val START_TAG = 1
        private const val END_TAG = 2
        private const val VALUE = 3
        private const val IGNORE = -1

        @Throws(XmlPullParserException::class)
        fun nameWithNs(
            name: String?,
            namespace: String?,
            parser: XmlPullParser?,
        ): String? {
            var result = name
            var ns = namespace
            if (ns != null && ns.isNotEmpty()) {
                if (parser != null) {
                    val count = parser.getNamespaceCount(parser.depth)
                    for (i in 0 until count) {
                        if (ns == parser.getNamespaceUri(i)) {
                            ns = parser.getNamespacePrefix(i)
                            break
                        }
                    }
                }
                result = "<$ns>$result"
            }
            return result
        }
    }
}
