package com.ismartcoding.lib.mustache

import java.lang.RuntimeException

/**
 * An exception thrown when an error occurs parsing or executing a Mustache template.
 */
open class MustacheException : RuntimeException {
    /** An exception thrown if we encounter a context error (e.g. a missing variable) while
     * compiling or executing a template.  */
    class Context : MustacheException {
        /** The key that caused the problem.  */
        val key: String

        /** The line number of the template on which the problem occurred.  */
        val lineNo: Int

        constructor(message: String, key: String, lineNo: Int) : super(message) {
            this.key = key
            this.lineNo = lineNo
        }

        constructor(message: String, key: String, lineNo: Int, cause: Throwable?) : super(message, cause) {
            this.key = key
            this.lineNo = lineNo
        }
    }

    constructor(message: String) : super(message) {}
    constructor(cause: Throwable) : super(cause) {}
    constructor(message: String, cause: Throwable?) : super(message, cause) {}
}
