package com.ismartcoding.plain.ui.extensions

import android.app.Activity
import android.content.Intent
import androidx.core.content.FileProvider
import com.ismartcoding.lib.extensions.getMediaContentUri
import com.ismartcoding.lib.extensions.getUriMimeType
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.helpers.DialogHelper
import java.io.File

fun Activity.openPathIntent(
    path: String,
    extras: HashMap<String, Boolean> = HashMap(),
) {
    var newUri = this.getMediaContentUri(path)
    if (newUri == null) {
        newUri = FileProvider.getUriForFile(this, Constants.AUTHORITY, File(path))
    }
    val mimeType = getUriMimeType(path, newUri!!)
    val intent = Intent()
    intent.action = Intent.ACTION_VIEW
    intent.setDataAndType(newUri, mimeType)
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    for ((key, value) in extras) {
        intent.putExtra(key, value)
    }
    try {
        startActivity(Intent.createChooser(intent, getString(R.string.open_with)))
    } catch (e: Exception) {
        DialogHelper.showMessage(e.toString())
    }
}


