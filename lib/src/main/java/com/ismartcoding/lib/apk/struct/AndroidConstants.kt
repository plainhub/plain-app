package com.ismartcoding.lib.apk.struct

object AndroidConstants {
    const val RESOURCE_FILE = "resources.arsc"
    const val MANIFEST_FILE = "AndroidManifest.xml"
    const val DEX_FILE = "classes.dex"
    const val DEX_ADDITIONAL = "classes%d.dex"
    const val RES_PREFIX = "res/"
    const val ASSETS_PREFIX = "assets/"
    const val LIB_PREFIX = "lib/"
    const val META_PREFIX = "META-INF/"
    const val ARCH_ARMEABI = ""

    /**
     * the binary xml file used system attr id.
     */
    const val ATTR_ID_START = 0x01010000

    /**
     * start offset for system android.R.style
     */
    const val SYS_STYLE_ID_START = 0x01030000

    /**
     * end offset for system android.R.style
     */
    const val SYS_STYLE_ID_END = 0x01031000
}