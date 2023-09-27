package com.ismartcoding.lib.mustache

import com.ismartcoding.lib.mustache.Template.Companion.isThisName
import java.io.IOException
import java.io.Reader
import java.io.Writer
import java.util.*

/**
 * Provides [Mustache](http://mustache.github.com/) templating services.
 *
 *
 *  Basic usage:
 * <pre>`String source = "Hello {{arg}}!";
 * Template tmpl = Mustache.compiler().compile(source);
 * Map<String, Object> context = new HashMap<String, Object>();
 * context.put("arg", "world");
 * tmpl.execute(context); // returns "Hello world!"
`</pre> *
 */
object Mustache {
    /**
     * Returns a compiler that escapes HTML by default and does not use standards mode.
     */
    fun compiler(): Compiler {
        return Compiler(
            standardsMode = false, strictSections = false, nullValue = null, missingIsNull = false, emptyStringIsFalse = false,
            zeroIsFalse = false, DEFAULT_FORMATTER, FAILING_LOADER, DefaultCollector(), Delims(),
        )
    }

    /**
     * Compiles the supplied template into a repeatedly executable intermediate form.
     */
    internal fun compile(
        source: Reader,
        compiler: Compiler,
    ): Template {
        val accum = Parser(compiler).parse(source)
        return Template(trim(accum.finish(), true), compiler)
    }

    internal fun trim(
        segs: Array<Segment>,
        top: Boolean,
    ): Array<Segment> {
        // now that we have all of our segments, we make a pass through them to trim whitespace
        // from section tags which stand alone on their lines
        var ii = 0
        val ll = segs.size
        while (ii < ll) {
            val seg = segs[ii]
            val pseg = if (ii > 0) segs[ii - 1] else null
            val nseg = if (ii < ll - 1) segs[ii + 1] else null
            val prev = if (pseg is StringSegment) pseg else null
            val next = if (nseg is StringSegment) nseg else null
            // if we're at the top-level there are virtual "blank lines" before & after segs
            val prevBlank = pseg == null && top || prev != null && prev.trailsBlank()
            val nextBlank = nseg == null && top || next != null && next.leadsBlank()
            // potentially trim around the open and close tags of a block segment
            if (seg is BlockSegment) {
                val block = seg
                if (prevBlank && block.firstLeadsBlank()) {
                    if (pseg != null) segs[ii - 1] = prev!!.trimTrailBlank()
                    block.trimFirstBlank()
                }
                if (nextBlank && block.lastTrailsBlank()) {
                    block.trimLastBlank()
                    if (nseg != null) segs[ii + 1] = next!!.trimLeadBlank()
                }
            } else if (seg is FauxSegment) {
                if (prevBlank && nextBlank) {
                    if (pseg != null) segs[ii - 1] = prev!!.trimTrailBlank()
                    if (nseg != null) segs[ii + 1] = next!!.trimLeadBlank()
                }
            }
            ii++
        }
        return segs
    }

    internal fun restoreStartTag(
        text: StringBuilder,
        starts: Delims,
    ) {
        text.insert(0, starts.start1)
        if (starts.start2 != NO_CHAR) {
            text.insert(1, starts.start2)
        }
    }

    internal const val TEXT = 0
    internal const val MATCHING_START = 1
    internal const val MATCHING_END = 2
    internal const val TAG = 3

    /**
     * Used when we have only a single character delimiter.
     */
    const val NO_CHAR = Character.MIN_VALUE
    private val FAILING_LOADER =
        object : TemplateLoader {
            override fun getTemplate(name: String): Reader? {
                throw UnsupportedOperationException("Template loading not configured")
            }
        }
    private val DEFAULT_FORMATTER =
        object : Formatter {
            override fun format(value: Any): String {
                return value.toString()
            }
        }

    /**
     * Handles converting objects to strings when rendering templates.
     */
    interface Formatter {
        /**
         * Converts `value` to a string for inclusion in a template.
         */
        fun format(value: Any): String
    }

    /**
     * Handles lambdas.
     */
    interface Lambda {
        /**
         * Executes this lambda on the supplied template fragment. The lambda should write its
         * results to `out`.
         *
         * @param frag the fragment of the template that was passed to the lambda.
         * @param out  the writer to which the lambda should write its output.
         */
        @Throws(IOException::class)
        fun execute(
            frag: Template.Fragment?,
            out: Writer?,
        )
    }

    /**
     * Handles lambdas that are also invoked for inverse sections..
     */
    interface InvertibleLambda : Lambda {
        /**
         * Executes this lambda on the supplied template fragment, when the lambda is used in an
         * inverse section. The lambda should write its results to `out`.
         *
         * @param frag the fragment of the template that was passed to the lambda.
         * @param out  the writer to which the lambda should write its output.
         */
        @Throws(IOException::class)
        fun executeInverse(
            frag: Template.Fragment?,
            out: Writer?,
        )
    }

    /**
     * Reads variables from context objects.
     */
    interface VariableFetcher {
        /**
         * Reads the so-named variable from the supplied context object.
         */
        @Throws(Exception::class)
        operator fun get(
            ctx: Any,
            name: String,
        ): Any
    }

    /**
     * Handles loading partial templates.
     */
    interface TemplateLoader {
        /**
         * Returns a reader for the template with the supplied name.
         * Reader will be closed by callee.
         *
         * @throws Exception if the template could not be loaded for any reason.
         */
        @Throws(Exception::class)
        fun getTemplate(name: String): Reader?
    }

    /**
     * Provides a means to implement custom logic for variable lookup. If a context object
     * implements this interface, its `get` method will be used to look up variables instead
     * of the usual methods.
     *
     *
     * This is simpler than having a context implement [Map] which would require that it also
     * support the [Map.entrySet] method for iteration. A `CustomContext` object cannot
     * be used for a list section.
     */
    interface CustomContext {
        /**
         * Fetches the value of a variable named `name`.
         */
        @Throws(Exception::class)
        operator fun get(name: String): Any?
    }

    /**
     * Used to visit the tags in a template without executing it.
     */
    interface Visitor {
        /**
         * Visits a text segment. These are blocks of text that are normally just reproduced as
         * is when executing a template.
         *
         * @param text the block of text. May contain newlines.
         */
        fun visitText(text: String)

        /**
         * Visits a variable tag.
         *
         * @param name the name of the variable.
         */
        fun visitVariable(name: String)

        /**
         * Visits an include (partial) tag.
         *
         * @param name the name of the partial template specified by the tag.
         * @return true if the template should be resolved and visited, false to skip it.
         */
        fun visitInclude(name: String): Boolean

        /**
         * Visits a section tag.
         *
         * @param name the name of the section.
         * @return true if the contents of the section should be visited, false to skip.
         */
        fun visitSection(name: String): Boolean

        /**
         * Visits an inverted section tag.
         *
         * @param name the name of the inverted section.
         * @return true if the contents of the section should be visited, false to skip.
         */
        fun visitInvertedSection(name: String): Boolean
    }

    // a hand-rolled parser; whee!
    class Parser(compiler: Compiler) {
        val delims = compiler.delims.copy()
        val text = StringBuilder()
        var source: Reader? = null
        var accum = Accumulator(compiler, true)
        var state = TEXT
        var line = 1
        var column = 0
        var tagStartColumn = -1

        fun parse(source: Reader): Accumulator {
            this.source = source
            var v: Int
            while (nextChar().also { v = it } != -1) {
                val c = v.toChar()
                ++column // our columns start at one, so increment before parse
                parseChar(c)
                // if we just parsed a newline, reset the column to zero and advance line
                if (c == '\n') {
                    column = 0
                    ++line
                }
            }
            when (state) {
                TAG -> restoreStartTag(text, delims)
                MATCHING_END -> {
                    restoreStartTag(text, delims)
                    text.append(delims.end1)
                }
                MATCHING_START -> text.append(delims.start1)
                TEXT -> {}
            }
            accum.addTextSegment(text)
            return accum
        }

        private fun parseChar(c: Char) {
            when (state) {
                TEXT ->
                    if (c == delims.start1) {
                        state = MATCHING_START
                        tagStartColumn = column
                        if (delims.start2 == NO_CHAR) {
                            parseChar(NO_CHAR)
                        }
                    } else {
                        text.append(c)
                    }
                MATCHING_START ->
                    if (c == delims.start2) {
                        accum.addTextSegment(text)
                        state = TAG
                    } else {
                        text.append(delims.start1)
                        state = TEXT
                        parseChar(c)
                    }
                TAG ->
                    if (c == delims.end1) {
                        state = MATCHING_END
                        if (delims.end2 == NO_CHAR) {
                            parseChar(NO_CHAR)
                        }
                    } else if (c == delims.start1 && text.length > 0 && text[0] != '!') {
                        // if we've already matched some tag characters and we see a new start tag
                        // character (e.g. "{{foo {" but not "{{{"), treat the already matched text as
                        // plain text and start matching a new tag from this point, unless we're in
                        // a comment tag.
                        restoreStartTag(text, delims)
                        accum.addTextSegment(text)
                        tagStartColumn = column
                        state =
                            if (delims.start2 == NO_CHAR) {
                                accum.addTextSegment(text)
                                TAG
                            } else {
                                MATCHING_START
                            }
                    } else {
                        text.append(c)
                    }
                MATCHING_END ->
                    if (c == delims.end2) {
                        if (text[0] == '=') {
                            delims.updateDelims(text.substring(1, text.length - 1))
                            text.setLength(0)
                            accum.addFauxSegment() // for newline trimming
                        } else {
                            // if the delimiters are {{ and }}, and the tag starts with {{{ then
                            // require that it end with }}} and disable escaping
                            if (delims.isStaches && text[0] == delims.start1) {
                                // we've only parsed }} at this point, so we have to slurp in another
                                // character from the input stream and check it
                                val end3 = nextChar()
                                if (end3 != '}'.code) {
                                    val got = if (end3 == -1) "" else end3.toChar().toString()
                                    throw MustacheParseException(
                                        "Invalid triple-mustache tag: {{$text}}$got",
                                        line,
                                    )
                                }
                                // convert it into (equivalent) {{&text}} which addTagSegment handles
                                text.replace(0, 1, "&")
                            }
                            // process the tag between the mustaches
                            accum = accum.addTagSegment(text, line)
                        }
                        state = TEXT
                    } else {
                        text.append(delims.end1)
                        state = TAG
                        parseChar(c)
                    }
            }
        }

        private fun nextChar(): Int {
            return try {
                source!!.read()
            } catch (ioe: IOException) {
                throw MustacheException(ioe)
            }
        }
    }

    open class Accumulator(protected val _comp: Compiler, val _topLevel: Boolean) {
        fun addTextSegment(text: StringBuilder) {
            if (text.isNotEmpty()) {
                _segs.add(StringSegment(text.toString(), _segs.isEmpty() && _topLevel))
                text.setLength(0)
            }
        }

        fun addTagSegment(
            accum: StringBuilder,
            tagLine: Int,
        ): Accumulator {
            val outer = this
            val tag = accum.toString().trim { it <= ' ' }
            val tag1 = tag.substring(1).trim { it <= ' ' }
            accum.setLength(0)
            return when (tag[0]) {
                '#' -> {
                    requireNoNewlines(tag, tagLine)
                    object : Accumulator(_comp, false) {
                        override fun finish(): Array<Segment> {
                            throw MustacheParseException(
                                "Section missing close tag '$tag1'",
                                tagLine,
                            )
                        }

                        override fun addCloseSectionSegment(
                            itag: String,
                            line: Int,
                        ): Accumulator {
                            requireSameName(tag1, itag, line)
                            outer._segs.add(SectionSegment(_comp, itag, super.finish(), tagLine))
                            return outer
                        }
                    }
                }
                '>' -> {
                    _segs.add(IncludedTemplateSegment(_comp, tag1))
                    this
                }
                '^' -> {
                    requireNoNewlines(tag, tagLine)
                    object : Accumulator(_comp, false) {
                        override fun finish(): Array<Segment> {
                            throw MustacheParseException(
                                "Inverted section missing close tag '$tag1'",
                                tagLine,
                            )
                        }

                        override fun addCloseSectionSegment(
                            itag: String,
                            line: Int,
                        ): Accumulator {
                            requireSameName(tag1, itag, line)
                            outer._segs.add(InvertedSegment(_comp, itag, super.finish(), tagLine))
                            return outer
                        }
                    }
                }
                '/' -> {
                    requireNoNewlines(tag, tagLine)
                    addCloseSectionSegment(tag1, tagLine)
                }
                '!' -> {
                    // comment!, ignore
                    _segs.add(FauxSegment()) // for whitespace trimming
                    this
                }
                '&' -> {
                    requireNoNewlines(tag, tagLine)
                    _segs.add(VariableSegment(tag1, tagLine, _comp.formatter))
                    this
                }
                else -> {
                    requireNoNewlines(tag, tagLine)
                    _segs.add(VariableSegment(tag, tagLine, _comp.formatter))
                    this
                }
            }
        }

        fun addFauxSegment() {
            _segs.add(FauxSegment())
        }

        open fun finish(): Array<Segment> {
            return _segs.toTypedArray()
        }

        protected open fun addCloseSectionSegment(
            tag: String,
            line: Int,
        ): Accumulator {
            throw MustacheParseException(
                "Section close tag with no open tag '$tag'",
                line,
            )
        }

        protected val _segs: MutableList<Segment> = ArrayList()

        companion object {
            protected fun requireNoNewlines(
                tag: String,
                line: Int,
            ) {
                if (tag.indexOf('\n') != -1 || tag.indexOf('\r') != -1) {
                    throw MustacheParseException(
                        "Invalid tag name: contains newline '$tag'",
                        line,
                    )
                }
            }

            protected fun requireSameName(
                name1: String,
                name2: String,
                line: Int,
            ) {
                if (name1 != name2) {
                    throw MustacheParseException(
                        "Section close tag with mismatched open tag '" +
                            name2 + "' != '" + name1 + "'",
                        line,
                    )
                }
            }
        }
    }

    /**
     * A simple segment that reproduces a string.
     */
    class StringSegment(val text: String, leadBlank: Int, trailBlank: Int) : Segment() {
        constructor(text: String, first: Boolean) : this(text, blankPos(text, true, first), blankPos(text, false, first)) {}

        fun leadsBlank(): Boolean {
            return _leadBlank != -1
        }

        fun trailsBlank(): Boolean {
            return _trailBlank != -1
        }

        fun trimLeadBlank(): StringSegment {
            if (_leadBlank == -1) return this
            val lpos = _leadBlank + 1
            val newTrail = if (_trailBlank == -1) -1 else _trailBlank - lpos
            return StringSegment(text.substring(lpos), -1, newTrail)
        }

        fun trimTrailBlank(): StringSegment {
            return if (_trailBlank == -1) {
                this
            } else {
                StringSegment(
                    text.substring(0, _trailBlank),
                    _leadBlank,
                    -1,
                )
            }
        }

        override fun execute(
            tmpl: Template,
            ctx: Context,
            out: Writer,
        ) {
            write(out, text)
        }

        override fun decompile(
            delims: Delims,
            into: StringBuilder,
        ) {
            into.append(text)
        }

        override fun visit(visitor: Visitor) {
            visitor.visitText(text)
        }

        override fun toString(): String {
            return "Text(" + text.replace("\r", "\\r").replace("\n", "\\n") + ")" +
                _leadBlank + "/" + _trailBlank
        }

        protected val _leadBlank: Int
        protected val _trailBlank: Int

        init {
            assert(leadBlank >= -1)
            assert(trailBlank >= -1)
            _leadBlank = leadBlank
            _trailBlank = trailBlank
        }

        companion object {
            private fun blankPos(
                text: String,
                leading: Boolean,
                first: Boolean,
            ): Int {
                val len = text.length
                var ii = if (leading) 0 else len - 1
                val ll = if (leading) len else -1
                val dd = if (leading) 1 else -1
                while (ii != ll) {
                    val c = text[ii]
                    if (c == '\n') return if (leading) ii else ii + 1
                    if (!Character.isWhitespace(c)) return -1
                    ii += dd
                }
                // if this is the first string segment and we're looking for trailing whitespace, a
                // totally blank segment (but which lacks a newline) is all trailing whitespace
                return if (leading || !first) -1 else 0
            }
        }
    }

    /**
     * A segment that loads and executes a sub-template.
     */
    class IncludedTemplateSegment(protected val _comp: Compiler, protected val _name: String) : Segment() {
        override fun execute(
            tmpl: Template,
            ctx: Context,
            out: Writer,
        ) {
            // we must take care to preserve our context rather than creating a new one, which
            // would happen if we just called execute() with ctx.data
            template.executeSegs(ctx, out)
        }

        override fun decompile(
            delims: Delims,
            into: StringBuilder,
        ) {
            delims.addTag('>', _name, into)
        }

        override fun visit(visitor: Visitor) {
            if (visitor.visitInclude(_name)) {
                template.visit(visitor)
            }
        }

        // we compile our template lazily to avoid infinie recursion if a template includes
        // itself (see issue #13)
        protected val template: Template
            get() {
                // we compile our template lazily to avoid infinie recursion if a template includes
                // itself (see issue #13)
                if (_template == null) {
                    _template = _comp.loadTemplate(_name)
                }
                return _template!!
            }
        private var _template: Template? = null
    }

    /**
     * A helper class for named segments.
     */
    abstract class NamedSegment protected constructor(protected val _name: String, protected val _line: Int) : Segment()

    /**
     * A segment that substitutes the contents of a variable.
     */
    class VariableSegment(name: String, line: Int, private val _formatter: Formatter) : NamedSegment(name, line) {
        override fun execute(
            tmpl: Template,
            ctx: Context,
            out: Writer,
        ) {
            val value = tmpl.getValueOrDefault(ctx, _name, _line)
            if (value == null) {
                val msg =
                    if (isThisName(
                            _name,
                        )
                    ) {
                        "Resolved '.' to null (which is disallowed), on line $_line"
                    } else {
                        "No key, method or field with name '$_name' on line $_line"
                    }
                throw MustacheException.Context(msg, _name, _line)
            }
            write(out, _formatter.format(value))
        }

        override fun decompile(
            delims: Delims,
            into: StringBuilder,
        ) {
            delims.addTag(' ', _name, into)
        }

        override fun visit(visitor: Visitor) {
            visitor.visitVariable(_name)
        }

        override fun toString(): String {
            return "Var($_name:$_line)"
        }
    }

    /**
     * A helper class for block segments.
     */
    abstract class BlockSegment protected constructor(name: String, segs: Array<Segment>, line: Int) : NamedSegment(name, line) {
        fun firstLeadsBlank(): Boolean {
            return if (_segs.isEmpty() || _segs[0] !is StringSegment) false else (_segs[0] as StringSegment).leadsBlank()
        }

        fun trimFirstBlank() {
            _segs[0] = (_segs[0] as StringSegment).trimLeadBlank()
        }

        fun lastTrailsBlank(): Boolean {
            val lastIdx = _segs.size - 1
            return if (_segs.isEmpty() || _segs[lastIdx] !is StringSegment) false else (_segs[lastIdx] as StringSegment).trailsBlank()
        }

        fun trimLastBlank() {
            val idx = _segs.size - 1
            _segs[idx] = (_segs[idx] as StringSegment).trimTrailBlank()
        }

        protected fun executeSegs(
            tmpl: Template,
            ctx: Context,
            out: Writer,
        ) {
            for (seg in _segs) {
                seg.execute(tmpl, ctx, out)
            }
        }

        protected val _segs: Array<Segment>

        init {
            _segs = trim(segs, false)
        }
    }

    /**
     * A segment that represents an inverted section.
     */
    class InvertedSegment(protected val _comp: Compiler, name: String, segs: Array<Segment>, line: Int) : BlockSegment(name, segs, line) {
        override fun execute(
            tmpl: Template,
            ctx: Context,
            out: Writer,
        ) {
            val value = tmpl.getSectionValue(ctx, _name, _line) // won't return null
            val iter = _comp.collector.toIterator(value)
            if (iter != null) {
                if (!iter.hasNext()) {
                    executeSegs(tmpl, ctx, out)
                }
            } else if (value is Boolean) {
                if (!value) {
                    executeSegs(tmpl, ctx, out)
                }
            } else if (value is InvertibleLambda) {
                try {
                    value.executeInverse(tmpl.createFragment(_segs, ctx), out)
                } catch (ioe: IOException) {
                    throw MustacheException(ioe)
                }
            } else if (_comp.isFalsey(value)) {
                executeSegs(tmpl, ctx, out)
            } // TODO: fail?
        }

        override fun decompile(
            delims: Delims,
            into: StringBuilder,
        ) {
            delims.addTag('^', _name, into!!)
            for (seg in _segs) seg.decompile(delims, into)
            delims.addTag('/', _name, into)
        }

        override fun visit(visitor: Visitor) {
            if (visitor.visitInvertedSection(_name)) {
                for (seg in _segs) {
                    seg.visit(visitor)
                }
            }
        }

        override fun toString(): String {
            return "Inverted(" + _name + ":" + _line + "): " + Arrays.toString(_segs)
        }
    }

    /**
     * A segment that represents a section.
     */
    class SectionSegment(protected val _comp: Compiler, name: String, segs: Array<Segment>, line: Int) : BlockSegment(name, segs, line) {
        override fun execute(
            tmpl: Template,
            ctx: Context,
            out: Writer,
        ) {
            val value = tmpl.getSectionValue(ctx, _name, _line) // won't return null
            val iter = _comp.collector.toIterator(value)
            if (iter != null) {
                var index = 0
                while (iter.hasNext()) {
                    val elem = iter.next()!!
                    val onFirst = index == 0
                    val onLast = !iter.hasNext()
                    executeSegs(tmpl, ctx.nest(elem, ++index, onFirst, onLast), out)
                }
            } else if (value is Boolean) {
                if (value) {
                    executeSegs(tmpl, ctx, out)
                }
            } else if (value is Lambda) {
                try {
                    value.execute(tmpl.createFragment(_segs, ctx), out)
                } catch (ioe: IOException) {
                    throw MustacheException(ioe)
                }
            } else if (_comp.isFalsey(value)) {
                // omit the section
            } else {
                executeSegs(tmpl, ctx.nest(value), out)
            }
        }

        override fun decompile(
            delims: Delims,
            into: StringBuilder,
        ) {
            delims.addTag('#', _name, into)
            for (seg in _segs) seg.decompile(delims, into)
            delims.addTag('/', _name, into)
        }

        override fun visit(visitor: Visitor) {
            if (visitor.visitSection(_name)) {
                for (seg in _segs) {
                    seg.visit(visitor)
                }
            }
        }

        override fun toString(): String {
            return "Section(" + _name + ":" + _line + "): " + Arrays.toString(_segs)
        }
    }

    class FauxSegment : Segment() {
        override fun execute(
            tmpl: Template,
            ctx: Context,
            out: Writer,
        ) {} // nada

        override fun decompile(
            delims: Delims,
            into: StringBuilder,
        ) {} // nada

        override fun visit(visit: Visitor) {}

        override fun toString(): String {
            return "Faux"
        }
    }
}
