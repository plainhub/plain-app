package com.ismartcoding.plain.helpers

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Base64
import com.ismartcoding.lib.extensions.getFileName
import com.ismartcoding.lib.extensions.getFilenameFromPath
import com.ismartcoding.lib.extensions.hasPermission
import com.ismartcoding.lib.extensions.scanFileByConnection
import com.ismartcoding.lib.helpers.CryptoHelper
import com.ismartcoding.lib.isRPlus
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.extensions.newFile
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

object FileHelper {
    fun fileFromAsset(
        context: Context,
        name: String,
    ): File {
        return File("${context.cacheDir.absolutePath}/$name").apply {
            writeBytes(context.assets.open(name).readBytes())
        }
    }

    fun writeFile(
        context: Context,
        filename: String,
        content: String,
    ) {
        FileWriter(File(context.filesDir, filename)).use {
            it.write(content)
        }
    }

    fun hasStoragePermission(context: Context): Boolean {
        return if (isRPlus()) {
            Environment.isExternalStorageManager()
        } else {
            context.hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    fun getFileId(path: String): String {
        if (path.isEmpty()) {
            return ""
        }
        if (path.startsWith("https://", true) || path.startsWith("http://", true)) {
            return path
        }
        return Base64.encodeToString(CryptoHelper.chaCha20Encrypt(TempData.urlToken, path), Base64.NO_WRAP)
    }

    fun rename(
        filePath: String,
        newName: String,
    ): File? {
        return rename(File(filePath), newName)
    }

    fun rename(
        file: File,
        newName: String,
    ): File? {
        if (!file.exists()) return null
        if (newName.isBlank()) return null
        if (newName == file.name) return file
        val newFile = File((file.parent?.plus(File.separator) ?: "") + newName)
        if (!newFile.exists()) {
            file.renameTo(newFile)
            return newFile
        }
        return null
    }

    fun copyFile(
        context: Context,
        pathFrom: Uri,
        pathTo: String,
    ) {
        context.contentResolver.openInputStream(pathFrom).use { input ->
            if (input != null) {
                val targetPath = Paths.get(pathTo)
                Files.copy(input, targetPath, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

    fun copyFileToDownloads(path: String): String {
        return copyFileToPublicDir(path, Environment.DIRECTORY_DOWNLOADS)
    }

    fun copyFileToPublicDir(path: String, dirName: String, newName: String = ""): String {
        try {
            val fileName = newName.ifEmpty { path.getFilenameFromPath() }
            val file = createPublicFile(fileName, dirName)
            File(path).copyTo(file)
            MainApp.instance.scanFileByConnection(file, null)
            return file.absolutePath
        } catch (ex: Exception) {
            ex.printStackTrace()
            LogCat.e(ex.toString())
        }
        return ""
    }

    fun copyFileToDownloads(context: Context, uri: Uri): String {
        try {
            val fileName = uri.getFileName(context)
            val file = createPublicFile(fileName, Environment.DIRECTORY_DOWNLOADS)
            val outputStream = FileOutputStream(file)
            if (uri.scheme == "content") {
                val inputStream = context.contentResolver.openInputStream(uri)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
            } else {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url(uri.toString())
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val inputStream = response.body?.byteStream()
                    inputStream?.copyTo(outputStream)
                    inputStream?.close()
                    outputStream.close()
                }
            }
            MainApp.instance.scanFileByConnection(file, null)
            return file.absolutePath
        } catch (ex: Exception) {
            ex.printStackTrace()
            LogCat.e(ex.toString())
        }
        return ""
    }



    private fun createPublicFile(fileName: String, dirName: String): File {
        val dir = PathHelper.getPlainPublicDir(dirName)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        var file = File(dir, fileName)
        if (file.exists()) {
            file = file.newFile()
        }
        return file
    }


}
