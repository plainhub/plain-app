package com.ismartcoding.plain.features.chat.models

import com.ismartcoding.plain.models.Peer

import com.ismartcoding.plain.ui.extensions.getFinalPath

suspend fun MessageFile.getPreviewPath(peer: Peer?): String {
    return if (isRemoteFile()) {
        peer?.getFileUrl(parseFileId())?.let { "$it&size=sm" }.orEmpty()
    } else {
        uri.getFinalPath()
    }
}
