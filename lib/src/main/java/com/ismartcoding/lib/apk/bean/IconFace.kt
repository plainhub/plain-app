package com.ismartcoding.lib.apk.bean

import java.io.Serializable

/**
 * The icon interface
 */
interface IconFace : Serializable {
    /**
     * If icon is file resource
     */
    val isFile: Boolean

    /**
     * Return the icon file as bytes. This method is valid only when [.isFile] return true.
     * Otherwise, [UnsupportedOperationException] should be thrown.
     */
    val data: ByteArray?

    /**
     * Return the icon file path in apk file. This method is valid only when [.isFile] return true.
     * Otherwise, [UnsupportedOperationException] should be thrown.
     */
    val path: String?
}