package com.ismartcoding.lib.apk.bean

import java.io.Serializable

/**
 * The plain icon, using color drawable resource.
 */
class ColorIcon : IconFace, Serializable {
    override val isFile: Boolean
        get() = false
    override val data: ByteArray?
        get() {
            throw UnsupportedOperationException()
        }
    override val path: String?
        get() {
            throw UnsupportedOperationException()
        }

    companion object {
        private const val serialVersionUID = -7913024425268466186L
    }
}