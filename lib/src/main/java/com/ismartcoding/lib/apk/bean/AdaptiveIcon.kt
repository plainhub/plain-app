package com.ismartcoding.lib.apk.bean

import java.io.Serializable

class AdaptiveIcon(
    val foreground: Icon?,
    val background: Icon?
) : IconFace, Serializable {

    override fun toString(): String {
        return "AdaptiveIcon{" +
                "foreground=" + foreground +
                ", background=" + background +
                '}'
    }

    override val isFile: Boolean
        get() = foreground!!.isFile
    override val data: ByteArray?
        get() = foreground?.data
    override val path: String?
        get() = foreground?.path

    companion object {
        private const val serialVersionUID = 4185750290211529320L
    }
}