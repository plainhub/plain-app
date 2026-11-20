package com.ismartcoding.plain.platform.files

import com.ismartcoding.plain.features.files.DriveType
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class StorageVolumeFactsTest {
    @get:Rule val temp = TemporaryFolder()

    @Test
    fun queriesOneSnapshotAndReadsEachCapacityByItsPath() {
        val primary = StorageRoot("/primary", DriveType.INTERNAL_STORAGE)
        val usb = StorageRoot("/usb", DriveType.USB_STORAGE)
        val sd = StorageRoot("/sd", DriveType.SDCARD)
        val removed = StorageRoot("/removed", DriveType.USB_STORAGE)
        val paths = mutableListOf<String>()
        var queries = 0
        val facts = storageVolumeFacts(
            roots = {
                queries++
                orderedStorageRoots(sequenceOf(removed, primary, sd, null, usb, sd, usb).constrainOnce())
            },
            statsForPath = { path ->
                paths.add(path)
                when (path) {
                    primary.path -> StorageStats(100, 50)
                    sd.path -> StorageStats(80, 20)
                    usb.path -> StorageStats(60, 10)
                    removed.path -> null
                    else -> error(path)
                }
            },
            grants = listOf("/shared/bookmarks"),
            appPath = "/app",
            name = { it.name },
        )
        assertEquals(1, queries)
        assertEquals(listOf("/primary", "/sd", "/removed", "/usb"), paths)
        assertEquals(listOf("/primary", "/sd", "/usb", "/shared/bookmarks", "/app"), facts.map { it.path })
        assertEquals(listOf(100L, 80L, 60L, 0L, 0L), facts.map { it.totalBytes })
        assertEquals(listOf(50L, 20L, 10L, 0L, 0L), facts.map { it.freeBytes })
        assertEquals("USB_STORAGE 1", facts[2].name)
    }

    @Test
    fun classifiesOnlyMountedPrimaryAndRemovableVolumes() {
        fun type(path: String, label: String) =
            androidStorageRoot("mounted_ro", false, true, { File(path) }, { label })?.type
        assertEquals(DriveType.USB_STORAGE, type("/storage/ABCD-1234", "USB drive"))
        assertEquals(DriveType.USB_STORAGE, type("/storage/usbdisk0", "Archive"))
        assertEquals(DriveType.USB_STORAGE, type("/storage/otg", "Archive"))
        assertEquals(DriveType.SDCARD, type("/storage/ABCD-1234", "Archive"))
        assertEquals(DriveType.SDCARD, type("/storage/ABCD-1234", "Houseboat"))
        val primary = androidStorageRoot("mounted", true, true, { File("/primary") }, { error("primary label") })
        assertEquals(DriveType.INTERNAL_STORAGE, primary?.type)
        assertNull(androidStorageRoot("unmounted", false, true, { error("unmounted path") }, { error("label") }))
        assertNull(androidStorageRoot("mounted", false, false, { error("fixed secondary path") }, { error("label") }))
        assertNull(androidStorageRoot("mounted", false, true, { null }, { error("missing directory") }))
    }

    @Test
    fun resolvesLegacyVolumesByUuidWithoutDependingOnArrayOrder() {
        val usb = temp.newFolder("DCBA-4321")
        assertEquals(usb, legacyStorageRoot(usb.name, temp.root) { error("unneeded lookup") })
        val oemRoot = File("/external/usbdisk0")
        assertEquals(oemRoot, legacyStorageRoot("ABCD-1234", temp.root) { oemRoot })
        assertEquals(oemRoot, legacyStorageRoot(null, temp.root) { oemRoot })
        assertNull(legacyStorageRoot(null, temp.root) { null })
    }
}
