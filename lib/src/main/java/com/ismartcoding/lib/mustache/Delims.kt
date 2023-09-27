package com.ismartcoding.lib.mustache

class Delims {
    var start1 = '{'
    var end1 = '}'
    var start2 = '{'
    var end2 = '}'
    val isStaches: Boolean
        get() = start1 == '{' && start2 == '{' && end1 == '}' && end2 == '}'

    fun updateDelims(dtext: String): Delims {
        val delims = dtext.split(" ").toTypedArray()
        if (delims.size != 2) throw MustacheException(errmsg(dtext))
        when (delims[0].length) {
            1 -> {
                start1 = delims[0][0]
                start2 = Mustache.NO_CHAR
            }
            2 -> {
                start1 = delims[0][0]
                start2 = delims[0][1]
            }
            else -> throw MustacheException(errmsg(dtext))
        }
        when (delims[1].length) {
            1 -> {
                end1 = delims[1][0]
                end2 = Mustache.NO_CHAR
            }
            2 -> {
                end1 = delims[1][0]
                end2 = delims[1][1]
            }
            else -> throw MustacheException(errmsg(dtext))
        }
        return this
    }

    fun addTag(
        prefix: Char,
        name: String,
        into: StringBuilder,
    ) {
        into.append(start1)
        into.append(start2)
        if (prefix != ' ') into.append(prefix)
        into.append(name)
        into.append(end1)
        into.append(end2)
    }

    fun copy(): Delims {
        val d = Delims()
        d.start1 = start1
        d.start2 = start2
        d.end1 = end1
        d.end2 = end2
        return d
    }

    companion object {
        private fun errmsg(dtext: String): String {
            return "Invalid delimiter configuration '" + dtext + "'. Must be of the " +
                "form {{=1 2=}} or {{=12 34=}} where 1, 2, 3 and 4 are delimiter chars."
        }
    }
}
