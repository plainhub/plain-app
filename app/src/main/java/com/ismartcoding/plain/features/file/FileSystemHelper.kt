package com.ismartcoding.plain.features.file

import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.storage.StorageManager
import androidx.appcompat.app.AppCompatActivity
import com.ismartcoding.lib.extensions.getDirectChildrenCount
import com.ismartcoding.lib.isRPlus
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import com.ismartcoding.plain.storageManager
import kotlinx.datetime.Instant
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.attribute.PosixFileAttributes

object FileSystemHelper {
    fun getMainStorageStats(context: Context): DStorageStats {
        val externalDirs = context.getExternalFilesDirs(null)
        val storageManager = context.getSystemService(AppCompatActivity.STORAGE_SERVICE) as StorageManager

        val stats = DStorageStats()
        externalDirs.forEach { file ->
            val storageVolume = storageManager.getStorageVolume(file) ?: return@forEach
            if (storageVolume.isPrimary) {
                // internal storage
                val storageStatsManager = context.getSystemService(AppCompatActivity.STORAGE_STATS_SERVICE) as StorageStatsManager
                val uuid = StorageManager.UUID_DEFAULT
                stats.totalBytes = storageStatsManager.getTotalBytes(uuid)
                stats.freeBytes = storageStatsManager.getFreeBytes(uuid)
            }
        }

        return stats
    }

    fun getInternalStoragePath(context: Context): String {
        return (if (isRPlus()) {
            storageManager.primaryStorageVolume.directory?.path
        } else null) ?: context.getExternalFilesDir(null)?.absolutePath?.trimEnd('/') ?: ""
    }

    fun getInternalStorageName(context: Context): String {
        return storageManager.primaryStorageVolume.getDescription(context) ?: getString(R.string.internal_storage)
    }

    private fun convertFile(file: File, showHidden: Boolean): DFile {
        var size: Long = 0
        val isDir = file.isDirectory
        if (!isDir) {
            size = file.length()
        }
        return DFile(
            file.name,
            file.path,
            "",
            Instant.fromEpochMilliseconds(file.lastModified()),
            size,
            isDir,
            if (isDir) file.getDirectChildrenCount(showHidden) else 0
        )
    }

    fun getFilesList(
        dir: String,
        showHidden: Boolean,
        sortBy: FileSortBy,
    ): List<DFile> {
        val pathFile = File(dir)
        val files = ArrayList<DFile>()
        if (pathFile.exists() && pathFile.isDirectory) {
            val listFiles = pathFile.listFiles()
            listFiles?.forEach { file ->
                if (!showHidden && file.isHidden) {
                    return@forEach
                }
                files.add(convertFile(file, showHidden))
            }
        }

        return files.sort(sortBy)
    }

    fun search(q: String, dir: String, showHidden: Boolean): ArrayList<DFile> {
        val files = ArrayList<DFile>()
        File(dir).listFiles()?.sortedBy { it.isDirectory }?.forEach {
            if (!showHidden && it.isHidden) {
                return@forEach
            }

            if (it.name.contains(q, true)) {
                files.add(convertFile(it, showHidden))
            }

            if (it.isDirectory) {
                files.addAll(search(q, it.absolutePath, showHidden))
            }
        }

        return files
    }

    fun createDirectory(path: String): DFile {
        val file = File(path)
        file.mkdirs()
        return convertFile(file, false)
    }

    fun createFile(path: String): DFile {
        val file = File(path)
        file.createNewFile()
        return convertFile(file, false)
    }

    private fun parseFilePermission(f: File): String {
        val attributes = Files.readAttributes(f.toPath(), PosixFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS)
        val p = attributes.permissions()
        return ""
    }
}

fun List<DFile>.sort(sortBy: FileSortBy): List<DFile> {
    val comparator = compareBy<DFile> { if (it.isDir) 0 else 1 }
    return when (sortBy) {
        FileSortBy.NAME_ASC -> {
            this.sortedWith(comparator.thenBy { it.name.lowercase() })
        }
        FileSortBy.NAME_DESC -> {
            this.sortedWith(comparator.thenByDescending { it.name.lowercase() })
        }
        FileSortBy.SIZE_ASC -> {
            this.sortedWith(comparator.thenBy { it.size })
        }
        FileSortBy.SIZE_DESC -> {
            this.sortedWith(comparator.thenByDescending { it.size })
        }
        FileSortBy.DATE_ASC -> {
            this.sortedWith(comparator.thenBy { it.updatedAt })
        }
        FileSortBy.DATE_DESC -> {
            this.sortedWith(comparator.thenByDescending { it.updatedAt })
        }
    }
}