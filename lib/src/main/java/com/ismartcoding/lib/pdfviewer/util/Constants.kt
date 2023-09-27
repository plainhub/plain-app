package com.ismartcoding.lib.pdfviewer.util

object Constants {
    const val DEBUG_MODE = false

    /** Between 0 and 1, the thumbnails quality (default 0.3). Increasing this value may cause performance decrease  */
    const val THUMBNAIL_RATIO = 0.3f

    /**
     * The size of the rendered parts (default 256)
     * Tinier : a little bit slower to have the whole page rendered but more reactive.
     * Bigger : user will have to wait longer to have the first visual results
     */
    const val PART_SIZE = 256f

    /** Part of document above and below screen that should be preloaded, in dp  */
    const val PRELOAD_OFFSET = 20

    /** The size of the cache (number of bitmaps kept)  */
    var CACHE_SIZE = 120
    var THUMBNAILS_CACHE_SIZE = 8

    var PINCH_MAXIMUM_ZOOM = 10f
    var PINCH_MINIMUM_ZOOM = 1f
}
