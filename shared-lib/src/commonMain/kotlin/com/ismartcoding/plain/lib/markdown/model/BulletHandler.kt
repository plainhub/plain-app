package com.ismartcoding.plain.lib.markdown.model

import org.intellij.markdown.IElementType

/** An interface of providing use case specific un/ordered list handling.*/
fun interface BulletHandler {
    fun transform(type: IElementType, bullet: CharSequence?, index: Int, listNumber: Int, depth: Int): String
}
