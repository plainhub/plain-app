package com.ismartcoding.lib.mustache

/**
 * An exception thrown if we encounter an error while parsing a template.
 */
class MustacheParseException : MustacheException {
    constructor(message: String) : super(message) {}
    constructor(message: String, lineNo: Int) : super("$message @ line $lineNo") {}
}
