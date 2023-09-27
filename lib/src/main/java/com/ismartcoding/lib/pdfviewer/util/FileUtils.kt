package com.ismartcoding.lib.pdfviewer.util

import android.content.Context
import java.io.*

object FileUtils {
    @Throws(IOException::class)
    fun fileFromAsset(
        context: Context,
        assetName: String,
    ): File {
        val outFile = File(context.cacheDir, "$assetName-pdfview.pdf")
        if (assetName.contains("/")) {
            outFile.parentFile?.mkdirs()
        }
        copy(context.assets.open(assetName), outFile)
        return outFile
    }

    @Throws(IOException::class)
    fun copy(
        inputStream: InputStream,
        output: File,
    ) {
        var outputStream: OutputStream? = null
        try {
            outputStream = FileOutputStream(output)
            var read = 0
            val bytes = ByteArray(1024)
            while (inputStream.read(bytes).also { read = it } != -1) {
                outputStream.write(bytes, 0, read)
            }
        } finally {
            try {
                inputStream.close()
            } finally {
                outputStream?.close()
            }
        }
    }
}
