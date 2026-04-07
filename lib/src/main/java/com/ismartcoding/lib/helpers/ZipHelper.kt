package com.ismartcoding.lib.helpers

import com.ismartcoding.lib.extensions.*
import com.ismartcoding.lib.logcat.LogCat
import java.io.BufferedInputStream
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipHelper {
    fun zip(
        sourcePaths: List<String>,
        targetPath: String,
    ): Boolean {
        val queue = LinkedList<String>()
        val fos: FileOutputStream
        try {
            fos = FileOutputStream(File(targetPath))
        } catch (exception: Exception) {
            LogCat.e(exception.toString())
            return false
        }
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

    fun unzip(zipFile: File, targetDir: File): Boolean {
        return try {
            zipFile.inputStream().buffered().use { stream ->
                unzip(stream, targetDir)
            }
        } catch (exception: Exception) {
            LogCat.e(exception.toString())
            false
        }
    }

    fun unzip(stream: InputStream, targetDir: File): Boolean {
        return try {
            if (!targetDir.exists() && !targetDir.mkdirs()) {
                throw IllegalStateException("Unable to create target directory: ${targetDir.path}")
            }

            val targetPath = targetDir.canonicalPath + File.separator
            ZipInputStream(BufferedInputStream(stream)).use { zipInput ->
                var entry = zipInput.nextEntry
                while (entry != null) {
                    val output = File(targetDir, entry.name)
                    val outputPath = output.canonicalPath
                    if (!outputPath.startsWith(targetPath)) {
                        throw SecurityException("Zip entry escapes target directory: ${entry.name}")
                    }

                    if (entry.isDirectory || entry.name.endsWith("/")) {
                        if (!output.exists() && !output.mkdirs()) {
                            throw IllegalStateException("Unable to create directory: ${output.path}")
                        }
                    } else {
                        output.parentFile?.let { parent ->
                            if (!parent.exists() && !parent.mkdirs()) {
                                throw IllegalStateException("Unable to create directory: ${parent.path}")
                            }
                        }
                        FileOutputStream(output).use { outputStream ->
                            zipInput.copyTo(outputStream)
                        }
                        if (entry.time > 0) {
                            output.setLastModified(entry.time)
                        }
                    }
                    zipInput.closeEntry()
                    entry = zipInput.nextEntry
                }
            }
            true
        } catch (exception: Exception) {
            LogCat.e(exception.toString())
            false
        }
    }
}
