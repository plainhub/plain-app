package com.ismartcoding.plain.tests

import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import androidx.test.platform.app.InstrumentationRegistry
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import com.ismartcoding.plain.rust.graphql.GraphqlRequest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageMountsTest {
    @Test
    fun mountsReturnCapacityForEachMountedPath() = runBlocking {
        val mounts = GraphqlApi.execute(GraphqlRequest<List<JsonObject>>(
            "query { mounts { path totalBytes freeBytes driveType } }", field = "mounts",
        )).filter { it.getValue("driveType").jsonPrimitive.content != "APP" }
        val paths = mounts.map { it.getValue("path").jsonPrimitive.content }
        assertEquals(paths.distinct(), paths)
        assertTrue(Environment.getExternalStorageDirectory().absolutePath in paths)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val manager = context.getSystemService(StorageManager::class.java)
            val expected = manager.storageVolumes.filter { it.isPrimary || it.isRemovable }
                .mapNotNull { it.directory?.absolutePath }.toSet()
            assertEquals(expected, paths.toSet())
        }
        for (mount in mounts) {
            val path = mount.getValue("path").jsonPrimitive.content
            val total = mount.getValue("totalBytes").jsonPrimitive.long
            assertEquals(StatFs(path).totalBytes, total)
            assertTrue(mount.getValue("freeBytes").jsonPrimitive.long in 0..total)
        }
    }
}
