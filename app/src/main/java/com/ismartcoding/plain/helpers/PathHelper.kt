package com.ismartcoding.plain.helpers

import android.os.Environment
import java.io.File

object PathHelper {
    fun getPlainPublicDir(dirName: String): File {
        val dir = Environment.getExternalStoragePublicDirectory(dirName)
        return File(dir, "PlainApp")
    }
}