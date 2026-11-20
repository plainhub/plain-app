package com.ismartcoding.plain.platform.files

import android.os.Environment
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.features.files.DriveType
import com.ismartcoding.plain.platform.isRPlus
import com.ismartcoding.plain.storageManager
import java.io.File

private val usbMarker = Regex("""(?<![a-z])(?:usb|otg)(?:disk|drive|storage|stick)?(?![a-z])""", RegexOption.IGNORE_CASE)

internal fun androidStorageRoot(
    state: String,
    primary: Boolean,
    removable: Boolean,
    directory: () -> File?,
    description: () -> String,
): StorageRoot? {
    if (state != Environment.MEDIA_MOUNTED && state != Environment.MEDIA_MOUNTED_READ_ONLY) return null
    if (!primary && !removable) return null
    val path = directory()?.absolutePath ?: return null
    val type = when {
        primary -> DriveType.INTERNAL_STORAGE
        usbMarker.containsMatchIn(path) || usbMarker.containsMatchIn(description()) -> DriveType.USB_STORAGE
        else -> DriveType.SDCARD
    }
    return StorageRoot(path, type)
}

internal fun legacyStorageRoot(uuid: String?, mountDir: File, externalDirectory: () -> File?): File? {
    val standard = uuid?.let { File(mountDir, it) }
    return standard?.takeIf { it.isDirectory } ?: externalDirectory()
}

internal actual fun storageRoots(): List<StorageRoot> {
    val legacyDirs by lazy {
        val suffix = "/Android/data/${appContext.packageName}/files"
        appContext.getExternalFilesDirs(null).drop(1).mapNotNull { dir ->
            if (dir == null || !dir.path.endsWith(suffix)) return@mapNotNull null
            val volume = storageManager.getStorageVolume(dir) ?: return@mapNotNull null
            volume to File(dir.path.removeSuffix(suffix))
        }.toMap()
    }
    return orderedStorageRoots(storageManager.storageVolumes.asSequence().map { volume ->
        androidStorageRoot(
            volume.state, volume.isPrimary, volume.isRemovable,
            directory = {
                when {
                    isRPlus() -> volume.directory
                    volume.isPrimary -> Environment.getExternalStorageDirectory()
                    else -> legacyStorageRoot(volume.uuid, File("/storage")) { legacyDirs[volume] }
                }
            },
            description = { volume.getDescription(appContext) },
        )
    })
}

internal fun orderedStorageRoots(roots: Sequence<StorageRoot?>): List<StorageRoot> =
    roots.filterNotNull().distinctBy { it.path }.sortedBy { it.type.ordinal }.toList()
