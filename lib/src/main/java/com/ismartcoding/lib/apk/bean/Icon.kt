package com.ismartcoding.lib.apk.bean

import java.io.Serializable

/**
 * The plain file apk icon.
 */
class Icon(
    /**
     * The icon path in apk file
     */
    override val path: String?,
    /**
     * Return the density this icon for. 0 means default icon.
     * see [com.malt.parser.struct.resource.Densities] for more density values.
     */
    val density: Int,
    /**
     * Icon data may be null, due to some apk missing the icon file.
     */
    override val data: ByteArray?
) : IconFace, Serializable {

    override val isFile: Boolean
        get() = true

    override fun toString(): String {
        return "Icon{path='" + path + '\'' + ", density=" + density + ", size=" + (data?.size
            ?: 0) + '}'
    }

    companion object {
        private const val serialVersionUID = 8680309892249769701L
    }
}