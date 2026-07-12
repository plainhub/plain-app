package com.ismartcoding.plain.db

import com.ismartcoding.plain.lib.extensions.getFinalPath
import com.ismartcoding.plain.platform.getFileId

fun DMessageFile.getPreviewPath(peer: DPeer?): String {
    return if (isRemoteFile()) {
        peer?.getFileUrl(parseFileId()) + "&w=200&h=200"
    } else {
        uri.getFinalPath()
    }
}

fun DMessageContent.toPeerMessageContent(): DMessageContent {
    return when (type) {
        DMessageType.FILES.value -> {
            val files = value as DMessageFiles
            val modified = files.items.map { file ->
                val fileId = getFileId(file.uri)
                file.copy(uri = "fsid:$fileId")
            }
            DMessageContent(type, DMessageFiles(modified))
        }

        DMessageType.IMAGES.value -> {
            val images = value as DMessageImages
            val modified = images.items.map { image ->
                val fileId = getFileId(image.uri)
                image.copy(uri = "fsid:$fileId")
            }
            DMessageContent(type, DMessageImages(modified))
        }

        else -> this
    }
}
