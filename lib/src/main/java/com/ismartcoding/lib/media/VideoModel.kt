package com.ismartcoding.lib.media

import android.net.Uri

class VideoModel
    @JvmOverloads
    constructor(val uri: Uri, var headers: Map<String, String>? = null) {
        /** 是否循环播放 */
        var looping = false

        /** 预处理完跳转位置 */
        var seekPosition: Long = 0

        /** 是否静音 */
        var isMute = false
    }
