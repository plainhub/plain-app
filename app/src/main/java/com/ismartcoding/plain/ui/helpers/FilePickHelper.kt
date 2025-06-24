package com.ismartcoding.plain.ui.helpers

import android.content.Intent
import android.net.Uri

// https://developer.android.com/training/data-storage/shared/photopicker
object FilePickHelper {
    fun getPickFileIntent(multiple: Boolean): Intent {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, multiple)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        intent.type = "*/*"

        return intent
    }

    fun getFallbackPickFileIntent(multiple: Boolean): Intent {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, multiple)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.type = "*/*"

        return intent
    }

    fun getUris(intent: Intent): Set<Uri> {
        val uris = mutableSetOf<Uri>()
        if (intent.clipData != null) {
            val count = intent.clipData?.itemCount ?: 0
            for (i in 0 until count) {
                val uri = intent.clipData?.getItemAt(i)?.uri
                if (uri != null) {
                    uris.add(uri)
                }
            }
        } else if (intent.data != null) {
            uris.add(intent.data!!)
        }
        return uris
    }
}
