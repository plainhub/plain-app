package com.ismartcoding.lib.mustache

import java.io.IOException
import java.io.Writer

/** A template is broken into segments.  */
abstract class Segment {
    abstract fun execute(
        tmpl: Template,
        ctx: Context,
        out: Writer,
    )

    abstract fun decompile(
        delims: Delims,
        into: StringBuilder,
    )

    abstract fun visit(visitor: Mustache.Visitor)

    protected fun write(
        out: Writer,
        data: String,
    ) {
        try {
            out.write(data)
        } catch (ioe: IOException) {
            throw MustacheException(ioe)
        }
    }
}
