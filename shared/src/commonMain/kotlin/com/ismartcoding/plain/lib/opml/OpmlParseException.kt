package com.ismartcoding.plain.lib.opml

class OpmlParseException : Exception {
    constructor(cause: Throwable) : super(cause)
    constructor(message: String) : super(message)
}
