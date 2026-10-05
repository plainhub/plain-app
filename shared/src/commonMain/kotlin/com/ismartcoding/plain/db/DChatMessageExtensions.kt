package com.ismartcoding.plain.db

import com.ismartcoding.plain.extensions.getFinalPath

suspend fun DMessageFile.getPreviewPath(peer: DPeer?): String {
    return if (isRemoteFile()) {
        peer?.getFileUrl(parseFileId())?.let { "$it&w=200&h=200" }.orEmpty()
    } else {
        uri.getFinalPath()
    }
}
