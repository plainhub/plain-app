package com.ismartcoding.plain.features.file

import android.content.ContentResolver
import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import android.provider.MediaStore
import android.text.TextUtils
import androidx.core.os.bundleOf
import com.ismartcoding.lib.extensions.getDirectChildrenCount
import com.ismartcoding.lib.extensions.getLongValue
import com.ismartcoding.lib.extensions.getStringValue
import com.ismartcoding.lib.isRPlus
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import com.ismartcoding.plain.storageManager
import com.ismartcoding.plain.storageStatsManager
import kotlinx.datetime.Instant
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.attribute.PosixFileAttributes
import java.util.*
import java.util.regex.Pattern


object FileSystemHelper {
    private val physicalPaths = arrayListOf(
        "/storage/sdcard1", // Motorola Xoom
        "/storage/extsdcard", // Samsung SGS3
        "/storage/sdcard0/external_sdcard", // User request
        "/mnt/extsdcard", "/mnt/sdcard/external_sd", // Samsung galaxy family
        "/mnt/external_sd", "/mnt/media_rw/sdcard1", // 4.4.2 on CyanogenMod S3
        "/removable/microsd", // Asus transformer prime
        "/mnt/emmc", "/storage/external_SD", // LG
        "/storage/ext_sd", // HTC One Max
        "/storage/removable/sdcard1", // Sony Xperia Z1
        "/data/sdext", "/data/sdext2", "/data/sdext3", "/data/sdext4", "/sdcard1", // Sony Xperia Z
        "/sdcard2", // HTC One M8s
        "/storage/usbdisk0",
        "/storage/usbdisk1",
        "/storage/usbdisk2"
    )

    fun getInternalStorageStats(): DStorageStatsItem {
        val stats = DStorageStatsItem()
        val uuid = StorageManager.UUID_DEFAULT
        stats.totalBytes = storageStatsManager.getTotalBytes(uuid)
        stats.freeBytes = storageStatsManager.getFreeBytes(uuid)

        return stats
    }

    fun getSDCardStorageStats(context: Context): DStorageStatsItem {
        return getStorageStats(context, getSDCardPath(context))
    }

    fun getUSBStorageStats(context: Context): List<DStorageStatsItem> {
        return getUsbDiskPaths().map { getStorageStats(context, it) }
    }

    private fun getStorageStats(context: Context, path: String): DStorageStatsItem {
        if (path.isNotEmpty()) {
            val stat = StatFs(path)
            val availableBytes = stat.blockSizeLong * stat.availableBlocksLong
            val totalBytes = stat.blockSizeLong * stat.blockCountLong
            return DStorageStatsItem(totalBytes, availableBytes)
        }

        return DStorageStatsItem(0, 0)
    }

    fun getInternalStoragePath(context: Context): String {
        return (if (isRPlus()) {
            storageManager.primaryStorageVolume.directory?.path
        } else null) ?: Environment.getExternalStorageDirectory()?.absolutePath?.trimEnd('/') ?: ""
    }

    fun getInternalStorageName(context: Context): String {
        return storageManager.primaryStorageVolume.getDescription(context) ?: getString(R.string.internal_storage)
    }

    fun getSDCardPath(context: Context): String {
        val internalPath = getInternalStoragePath(context)
        val directories = getStorageDirectories(context).filter {
            it != internalPath && !it.equals(
                "/storage/emulated/0",
                true
            )
        }

        val fullSDPattern = Pattern.compile("^/storage/[A-Za-z0-9]{4}-[A-Za-z0-9]{4}$")
        var sdCardPath = directories.firstOrNull { fullSDPattern.matcher(it).matches() }
            ?: directories.firstOrNull { !physicalPaths.contains(it.lowercase()) } ?: ""

        if (sdCardPath.isEmpty()) {
            val sdPattern = Pattern.compile("^[A-Za-z0-9]{4}-[A-Za-z0-9]{4}$")
            try {
                File("/storage").listFiles()?.forEach {
                    if (sdPattern.matcher(it.name).matches()) {
                        sdCardPath = "/storage/${it.name}"
                    }
                }
            } catch (e: Exception) {
            }
        }

        return sdCardPath.trimEnd('/')
    }

    fun getUsbDiskPaths(): List<String> {
        val storageVolumes = storageManager.storageVolumes
        val paths = mutableListOf<String>()
        var rootDirs: Array<File>? = null
        for (storageVolume in storageVolumes) {
            if (storageVolume.isRemovable) {
                var path = ""
                path = if (isRPlus()) {
                    storageVolume.directory.toString()
                } else {
                    val uuid = storageVolume.uuid ?: continue
                    if (rootDirs == null) {
                        rootDirs = File("/storage").listFiles()
                    }
                    rootDirs?.find { it.name.contains(uuid) }?.absolutePath ?: ""
                }
                if (path.isNotEmpty() && !path.contains("storage")) {
                    paths.add(path)
                }
            }
        }

        return paths
    }

    private fun getStorageDirectories(context: Context): Array<String> {
        val paths = HashSet<String>()
        val rawExternalStorage = System.getenv("EXTERNAL_STORAGE")
        val rawSecondaryStoragesStr = System.getenv("SECONDARY_STORAGE")
        val rawEmulatedStorageTarget = System.getenv("EMULATED_STORAGE_TARGET")
        if (TextUtils.isEmpty(rawEmulatedStorageTarget)) {
            context.getExternalFilesDirs(null).filterNotNull().map { it.absolutePath }
                .mapTo(paths) { it.substring(0, it.indexOf("Android/data")) }
        } else {
            val path = Environment.getExternalStorageDirectory().absolutePath
            val folders = Pattern.compile("/").split(path)
            val lastFolder = folders[folders.size - 1]
            var isDigit = false
            try {
                Integer.valueOf(lastFolder)
                isDigit = true
            } catch (ignored: NumberFormatException) {
            }

            val rawUserId = if (isDigit) lastFolder else ""
            if (TextUtils.isEmpty(rawUserId)) {
                paths.add(rawEmulatedStorageTarget!!)
            } else {
                paths.add(rawEmulatedStorageTarget + File.separator + rawUserId)
            }
        }

        if (!TextUtils.isEmpty(rawSecondaryStoragesStr)) {
            val rawSecondaryStorages = rawSecondaryStoragesStr!!.split(File.pathSeparator.toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
            Collections.addAll(paths, *rawSecondaryStorages)
        }
        return paths.map { it.trimEnd('/') }.toTypedArray()
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

    fun getRecents(context: Context): List<DFile> {
        val items = arrayListOf<DFile>()
        val limit = 100
        val uri = MediaStore.Files.getContentUri("external").buildUpon()
            .appendQueryParameter("limit", limit.toString())
            .appendQueryParameter("offset", "0")
            .build()
        val projection = arrayOf(
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.SIZE
        )

        val queryArgs = bundleOf(
            ContentResolver.QUERY_ARG_LIMIT to limit,
            ContentResolver.QUERY_ARG_SORT_COLUMNS to arrayOf(MediaStore.Files.FileColumns.DATE_MODIFIED),
            ContentResolver.QUERY_ARG_SORT_DIRECTION to ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
        )
        context.contentResolver?.query(uri, projection, queryArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                do {
                    val path = cursor.getStringValue(MediaStore.Files.FileColumns.DATA)
                    if (File(path).isDirectory) {
                        continue
                    }

                    val name = cursor.getStringValue(MediaStore.Files.FileColumns.DISPLAY_NAME)
                    val size = cursor.getLongValue(MediaStore.Files.FileColumns.SIZE)
                    val updatedAt = Instant.fromEpochMilliseconds(cursor.getLongValue(MediaStore.Files.FileColumns.DATE_MODIFIED) * 1000L)
                    items.add(DFile(name, path, "", updatedAt, size, false, 0))
                } while (cursor.moveToNext())
            }
        }

        return items.take(50)
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