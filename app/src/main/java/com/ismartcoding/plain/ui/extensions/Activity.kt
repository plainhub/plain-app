package com.ismartcoding.plain.ui.extensions

import android.app.Activity
import android.content.Intent
import androidx.core.content.FileProvider
import com.ismartcoding.lib.extensions.getUriMimeType
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.helpers.DialogHelper
import java.io.File

fun Activity.openPathIntent(
    path: String,
    extras: HashMap<String, Boolean> = HashMap(),
) {
    val file = File(path)
    val newUri = FileProvider.getUriForFile(this, Constants.AUTHORITY, file)
    val mimeType = getUriMimeType(path, newUri)
    Intent().apply {
        action = Intent.ACTION_VIEW
        setDataAndType(newUri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        for ((key, value) in extras) {
            putExtra(key, value)
        }

        try {
            startActivity(Intent.createChooser(this, getString(R.string.open_with)))
        } catch (e: Exception) {
            DialogHelper.showMessage(e.toString())
        }
    }
}
