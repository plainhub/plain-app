package com.ismartcoding.lib.helpers

import com.ismartcoding.lib.extensions.*
import com.ismartcoding.lib.logcat.LogCat
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ZipHelper {
    fun zip(
        sourcePaths: List<String>,
        targetPath: String,
    ): Boolean {
        val queue = LinkedList<String>()
        val fos = FileOutputStream(File(targetPath))
        val zout = ZipOutputStream(fos)
        var res: Closeable = fos

        try {
            sourcePaths.forEach { currentPath ->
                var name: String
                var mainFilePath = currentPath
                val base = "${mainFilePath.getParentPath()}/"
                res = zout
                queue.push(mainFilePath)
                if (File(mainFilePath).isDirectory) {
                    name = "${mainFilePath.getFilenameFromPath()}/"
                    zout.putNextEntry(ZipEntry(name))
                }

                while (!queue.isEmpty()) {
                    mainFilePath = queue.pop()
                    val mainFile = File(mainFilePath)
                    if (mainFile.isDirectory) {
                        mainFile.listFiles()?.forEach { file ->
                            name = file.path.relativizeWith(base)
                            if (file.isDirectory) {
                                queue.push(file.absolutePath)
                                name = "${name.trimEnd('/')}/"
                                zout.putNextEntry(ZipEntry(name))
                            } else {
                                zout.putNextEntry(ZipEntry(name))
                                FileInputStream(file).copyTo(zout)
                                zout.closeEntry()
                            }
                        }
                    } else {
                        name = if (base == currentPath) currentPath.getFilenameFromPath() else mainFilePath.relativizeWith(base)
                        zout.putNextEntry(ZipEntry(name))
                        FileInputStream(mainFile).copyTo(zout)
                        zout.closeEntry()
                    }
                }
            }
        } catch (exception: Exception) {
            LogCat.e(exception.toString())
            return false
        } finally {
            res.close()
        }
        return true
    }

    suspend fun zipFolderToStreamAsync(
        folder: File,
        zip: ZipOutputStream,
        path: String = "",
    ) {
        folder.listFiles()?.forEach { file ->
            val filePath = if (path.isNotEmpty()) "$path/${file.name}" else file.name
            if (file.isDirectory) {
                zip.putNextEntry(ZipEntry("$filePath/"))
                zipFolderToStreamAsync(file, zip, filePath)
            } else {
                zip.putNextEntry(ZipEntry(filePath))
                file.inputStream().copyTo(zip)
            }
            zip.closeEntry()
        }
    }
}
