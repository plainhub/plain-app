package com.ismartcoding.plain.db

import com.ismartcoding.plain.extensions.getFinalPath

fun DMessageFile.getPreviewPath(peer: DPeer?): String {
    return if (isRemoteFile()) {
        peer?.getFileUrl(parseFileId()) + "&w=200&h=200"
    } else {
        uri.getFinalPath()
    }
}
