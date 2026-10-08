package com.ismartcoding.plain.httpserver

import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.lib.JsonHelper.jsonEncode
import com.ismartcoding.plain.platform.DPackageInfo
import com.ismartcoding.plain.enums.PackageType
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * `SystemProviderHost` hands these to the Rust side as JSON —
 * `systemCreateDir` and `systemWriteTextFile` all end in `jsonEncode(...)` on
 * the DTO. kotlinx serialization resolves a serializer from the static type, so
 * a DTO without `@Serializable` throws at the call instead of at compile time;
 * the only symptom is a GraphQL error carrying `Serializer for class '...' is
 * not found`, long after the change that dropped the annotation.
 *
 * Same failure as [DeviceFactsSerializationTest]; a separate class because
 * these are the database and file facts rather than the device's.
 */
class HostBridgeDtoSerializationTest {
    private val epoch = Instant.fromEpochMilliseconds(0)

    @Test
    fun fileFactsEncode() {
        val json = jsonEncode(
            DFile(
                name = "notes.txt",
                path = "/sdcard/Download/notes.txt",
                permission = "rwx",
                createdAt = epoch,
                updatedAt = epoch,
                size = 12,
                isDir = false,
                childCount = 0,
            )
        )
        assertTrue(json.contains("notes.txt"), json)
        assertTrue(json.contains("/sdcard/Download/notes.txt"), json)
    }

    /** Already annotated; kept so a future edit cannot quietly drop it — the
     *  package facts go over the same bridge. */
    @Test
    fun packageFactsEncode() {
        val json = jsonEncode(
            listOf(
                DPackageInfo(
                    id = "com.example",
                    name = "Example",
                    type = PackageType.USER,
                    version = "1.0",
                    path = "/data/app/example",
                    size = 1024,
                    installedAt = epoch,
                    updatedAt = epoch,
                )
            )
        )
        assertTrue(json.contains("com.example"), json)
        assertTrue(json.contains("USER"), json)
    }
}
