package com.ismartcoding.lib.mustache

import com.ismartcoding.lib.mustache.Mustache.VariableFetcher
import java.io.StringWriter
import java.io.Writer

/**
 * Represents a compiled template. Templates are executed with a *context* to generate
 * output. The context can be any tree of objects. Variables are resolved against the context.
 * Given a name `foo`, the following mechanisms are supported for resolving its value
 * (and are sought in this order):
 *
 *
 *  * If the variable has the special name `this` the context object itself will be
 * returned. This is useful when iterating over lists.
 *  * If the object is a [Map], [Map.get] will be called with the string `foo`
 * as the key.
 *  * A method named `foo` in the supplied object (with non-void return value).
 *  * A method named `getFoo` in the supplied object (with non-void return value).
 *  * A field named `foo` in the supplied object.
 *
 *
 *
 *  The field type, method return type, or map value type should correspond to the desired
 * behavior if the resolved name corresponds to a section. [Boolean] is used for showing or
 * hiding sections without binding a sub-context. Arrays, [Iterator] and [Iterable]
 * implementations are used for sections that repeat, with the context bound to the elements of the
 * array, iterator or iterable. Lambdas are current unsupported, though they would be easy enough
 * to add if desire exists. See the [Mustache
 * documentation](http://mustache.github.com/mustache.5.html) for more details on section behavior.
 */
class Template(val _segs: Array<Segment>, val _compiler: Compiler) {
    /**
     * Encapsulates a fragment of a template that is passed to a lambda. The fragment is bound to
     * the variable context that was in effect at the time the lambda was called.
     */
    abstract inner class Fragment {
        /** Executes this fragment; writes its result to `out`.  */
        abstract fun execute(out: Writer)

        /** Executes this fragment with the provided context; writes its result to `out`. The
         * provided context will be nested in the fragment's bound context.  */
        abstract fun execute(
            context: Any?,
            out: Writer,
        )

        /** Executes `tmpl` using this fragment's bound context. This allows a lambda to
         * resolve its fragment to a dynamically loaded template and then run that template with
         * the same context as the lamda, allowing a lambda to act as a 'late bound' included
         * template, i.e. you can decide which template to include based on information in the
         * context.  */
        abstract fun executeTemplate(
            tmpl: Template,
            out: Writer,
        )

        /** Executes this fragment and returns its result as a string.  */
        fun execute(): String {
            val out = StringWriter()
            execute(out)
            return out.toString()
        }

        /** Executes this fragment with the provided context; returns its result as a string. The
         * provided context will be nested in the fragment's bound context.  */
        fun execute(context: Any?): String {
            val out = StringWriter()
            execute(context, out)
            return out.toString()
        }

        /** Returns the context object in effect for this fragment. The actual type of the object
         * depends on the structure of the data passed to the top-level template. You know where
         * your lambdas are executed, so you know what type to which to cast the context in order
         * to inspect it (be that a `Map` or a POJO or something else).  */
        abstract fun context(): Any?

        /** Like [.context] btu returns the `n`th parent context object. `0`
         * returns the same value as [.context], `1` returns the parent context,
         * `2` returns the grandparent and so forth. Note that if you request a parent that
         * does not exist an exception will be thrown. You should only use this method when you
         * know your lambda is run consistently in a context with a particular lineage.  */
        abstract fun context(n: Int): Any

        /** Decompiles the template inside this lamdba and returns *an approximation* of
         * the original template from which it was parsed. This is not the exact character for
         * character representation because the original text is not preserved because that would
         * incur a huge memory penalty for all users of the library when the vast majority of
         * them do not call decompile.
         *
         *
         * Limitations:
         *  *  Whitespace inside tags is not preserved: i.e. `{{ foo.bar }}` becomes
         * `{{foo.bar}}`.
         *  *  If the delimiters are changed by the template, those are not preserved.
         * The delimiters configured on the [Compiler] are used for all decompilation.
         *
         *
         *
         * This feature is meant to enable use of lambdas for i18n such that you can recover
         * the contents of a lambda (so long as they're simple) to use as the lookup key for a
         * translation string. For example: `{{#i18n}}Hello {{user.name}}!{{/i18n}}` can be
         * sent to an `i18n` lambda which can use `decompile` to recover the text
         * `Hello {{user.name}}!` to be looked up in a translation dictionary. The
         * translated fragment could then be compiled and cached and then executed in lieu of the
         * original fragment using [Fragment.context].
         */
        fun decompile(): String {
            return decompile(StringBuilder()).toString()
        }

        /** Decompiles this fragment into `into`. See [.decompile].
         * @return `into` for call chaining.
         */
        abstract fun decompile(into: StringBuilder): StringBuilder
    }

    /** Used to cache variable fetchers for a given context class, name combination.  */
    inner class Key(val cclass: Class<*>, val name: String) {
        override fun hashCode(): Int {
            return cclass.hashCode() * 31 + name.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            val okey = other as Key?
            return okey!!.cclass == cclass && okey.name == name
        }

        override fun toString(): String {
            return cclass.name + ":" + name
        }
    }

    /**
     * Executes this template with the given context, returning the results as a string.
     * @throws MustacheException if an error occurs while executing or writing the template.
     */
    @Throws(MustacheException::class)
    fun execute(context: Map<String, Any>): String {
        val out = StringWriter()
        execute(context, out)
        return out.toString()
    }

    /**
     * Executes this template with the given context, writing the results to the supplied writer.
     * @throws MustacheException if an error occurs while executing or writing the template.
     */
    @Throws(MustacheException::class)
    fun execute(
        context: Map<String, Any>,
        out: Writer,
    ) {
        executeSegs(Context(context, null, 0, false, false), out)
    }

    /**
     * Executes this template with the supplied context and parent context, writing the results to
     * the supplied writer. The parent context will be searched for variables that cannot be found
     * in the main context, in the same way the main context becomes a parent context when entering
     * a block.
     * @throws MustacheException if an error occurs while executing or writing the template.
     */
    @Throws(MustacheException::class)
    fun execute(
        context: Map<String, Any>,
        parentContext: Any,
        out: Writer,
    ) {
        val pctx = Context(parentContext, null, 0, false, false)
        executeSegs(Context(context, pctx, 0, false, false), out)
    }

    /**
     * Visits the tags in this template (via `visitor`) without executing it.
     * @param visitor the visitor to be called back on each tag in the template.
     */
    fun visit(visitor: Mustache.Visitor) {
        for (seg in _segs) {
            seg.visit(visitor)
        }
    }

    @Throws(MustacheException::class)
    fun executeSegs(
        ctx: Context,
        out: Writer,
    ) {
        for (seg in _segs) {
            seg.execute(this, ctx, out)
        }
    }

    fun createFragment(
        segs: Array<Segment>,
        currentCtx: Context,
    ): Fragment {
        return object : Fragment() {
            override fun execute(out: Writer) {
                execute(currentCtx, out)
            }

            override fun execute(
                context: Any?,
                out: Writer,
            ) {
                execute(currentCtx.nest(context!!), out)
            }

            override fun executeTemplate(
                tmpl: Template,
                out: Writer,
            ) {
                tmpl.executeSegs(currentCtx, out)
            }

            override fun context(): Any? {
                return currentCtx.data
            }

            override fun context(n: Int): Any {
                return context(currentCtx, n)
            }

            override fun decompile(into: StringBuilder): StringBuilder {
                for (seg in segs) seg.decompile(_compiler.delims, into)
                return into
            }

            private fun context(
                ctx: Context,
                n: Int,
            ): Any {
                return if (n == 0) ctx.data else context(ctx.parent!!, n - 1)
            }

            private fun execute(
                ctx: Context,
                out: Writer,
            ) {
                for (seg in segs) {
                    seg.execute(this@Template, ctx, out)
                }
            }
        }
    }

    /**
     * Called by executing segments to obtain the value of the specified variable in the supplied
     * context.
     *
     * @param ctx the context in which to look up the variable.
     * @param name the name of the variable to be resolved.
     * @param missingIsNull whether to fail if a variable cannot be resolved, or to return null in
     * that case.
     *
     * @return the value associated with the supplied name or null if no value could be resolved.
     */
    protected fun getValue(
        ctx: Context,
        name: String,
        line: Int,
        missingIsNull: Boolean,
    ): Any? {
        // handle our special variables
        if (name == FIRST_NAME) {
            return ctx.onFirst
        } else if (name == LAST_NAME) {
            return ctx.onLast
        } else if (name == INDEX_NAME) {
            return ctx.index
        }

        // if we're in standards mode, restrict ourselves to simple direct resolution (no compound
        // keys, no resolving values in parent contexts)
        if (_compiler.standardsMode) {
            val value = getValueIn(ctx.data, name, line)
            return checkForMissing(name, line, missingIsNull, value)
        }

        // first search our parent contexts for the key (even if the key is a compound key, we will
        // first try to find it "whole" and only if that fails do we resolve it in parts)
        val value = getValueIn(ctx.data, name, line)
        if (value !== NO_FETCHER_FOUND) {
            return value
        }
        // if we reach here, we found nothing in this or our parent contexts...

        // if we have a compound key, decompose the value and resolve it step by step
        return if (name != DOT_NAME && name.indexOf(DOT_NAME) != -1) {
            getCompoundValue(ctx, name, line, missingIsNull)
        } else {
            // otherwise let checkForMissing() decide what to do
            checkForMissing(name, line, missingIsNull, NO_FETCHER_FOUND)
        }
    }

    /**
     * Decomposes the compound key `name` into components and resolves the value they
     * reference.
     */
    private fun getCompoundValue(
        ctx: Context,
        name: String,
        line: Int,
        missingIsNull: Boolean,
    ): Any? {
        val comps = name.split("\\.").toTypedArray()
        // we want to allow the first component of a compound key to be located in a parent
        // context, but once we're selecting sub-components, they must only be resolved in the
        // object that represents that component
        var data = getValue(ctx, comps[0], line, missingIsNull)
        for (ii in 1 until comps.size) {
            if (data === NO_FETCHER_FOUND) {
                if (!missingIsNull) {
                    throw MustacheException.Context(
                        "Missing context for compound variable '" + name + "' on line " + line +
                            ". '" + comps[ii - 1] + "' was not found.",
                        name,
                        line,
                    )
                }
                return null
            } else if (data == null) {
                return null
            }
            // once we step into a composite key, we drop the ability to query our parent contexts;
            // that would be weird and confusing
            data = getValueIn(data, comps[ii], line)
        }
        return checkForMissing(name, line, missingIsNull, data)
    }

    /**
     * Returns the value of the specified variable, noting that it is intended to be used as the
     * contents for a section.
     */
    fun getSectionValue(
        ctx: Context,
        name: String,
        line: Int,
    ): Any {
        val value = getValue(ctx, name, line, !_compiler.strictSections)
        // TODO: configurable behavior on null values?
        return value ?: emptyList<Any>()
    }

    /**
     * Returns the value for the specified variable, or the configured default value if the
     * variable resolves to null. See [.getValue].
     */
    fun getValueOrDefault(
        ctx: Context,
        name: String,
        line: Int,
    ): Any? {
        val value = getValue(ctx, name, line, _compiler.missingIsNull)
        // getValue will raise MustacheException if a variable cannot be resolved and missingIsNull
        // is not configured; so we're safe to assume that any null that makes it up to this point
        // can be converted to nullValue
        return value ?: _compiler.computeNullValue(name)
    }

    private fun getValueIn(
        data: Any,
        name: String,
        line: Int,
    ): Any {
        // if we're getting `.` or `this` then just return the whole context; we do this before the
        // null check because it may be valid for the context to be null (if we're iterating over a
        // list which contains nulls, for example)
        if (isThisName(name)) return data
        val key = Key(data.javaClass, name)
        var fetcher = _fcache[key]
        fetcher =
            if (fetcher != null) {
                try {
                    return fetcher[data, name]
                } catch (e: Exception) {
                    // zoiks! non-monomorphic call site, update the cache and try again
                    _compiler.collector.createFetcher(data, key.name)
                }
            } else {
                _compiler.collector.createFetcher(data, key.name)
            }

        // if we were unable to create a fetcher, use the NOT_FOUND_FETCHER which will return
        // NO_FETCHER_FOUND to let the caller know that they can try the parent context or do le
        // freak out; we still cache this fetcher to avoid repeatedly looking up and failing to
        // find a fetcher in the same context (which can be expensive)
        if (fetcher == null) {
            fetcher = NOT_FOUND_FETCHER
        }
        return try {
            val value = fetcher[data, name]
            _fcache.put(key, fetcher)
            value
        } catch (e: Exception) {
            throw MustacheException.Context(
                "Failure fetching variable '$name' on line $line",
                name,
                line,
                e,
            )
        }
    }

    private fun checkForMissing(
        name: String,
        line: Int,
        missingIsNull: Boolean,
        value: Any?,
    ): Any? {
        return if (value === NO_FETCHER_FOUND) {
            if (missingIsNull) return null
            throw MustacheException.Context(
                "No method or field with name '$name' on line $line",
                name,
                line,
            )
        } else {
            value
        }
    }

    private val _fcache: MutableMap<Key, VariableFetcher> = _compiler.collector.createFetcherCache()

    companion object {
        /** A sentinel object that can be returned by a [Mustache.Collector] to indicate that a
         * variable does not exist in a particular context.  */
        val NO_FETCHER_FOUND = "<no fetcher found>"

        fun isThisName(name: String): Boolean {
            return DOT_NAME == name || THIS_NAME == name
        }

        const val DOT_NAME = "."
        const val THIS_NAME = "this"
        const val FIRST_NAME = "-first"
        const val LAST_NAME = "-last"
        const val INDEX_NAME = "-index"

        /** A fetcher cached for lookups that failed to find a fetcher.  */
        val NOT_FOUND_FETCHER =
            object : VariableFetcher {
                override fun get(
                    ctx: Any,
                    name: String,
                ): Any {
                    return NO_FETCHER_FOUND
                }
            }
    }
}
