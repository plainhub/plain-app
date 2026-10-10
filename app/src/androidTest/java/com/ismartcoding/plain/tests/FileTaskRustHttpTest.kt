package com.ismartcoding.plain.tests

import android.os.Environment
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ismartcoding.plain.api.*
import com.ismartcoding.plain.features.file.*
import com.ismartcoding.plain.preferences.RustSystemState
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class FileTaskRustHttpTest {
    @Test
    fun fileTasksCopyMovePersistActualTargetsAndReportRealFailures() = runBlocking {
        val root = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,"file-task-${UUID.randomUUID()}").apply { mkdirs() }
        val external = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"file-task-${UUID.randomUUID()}")
        val ids = mutableListOf<String>()
        val originalPermissions = RustSystemState.state.value.apiPermissions
        try {
            val source = File(root,"source.txt").apply { writeText("synthetic") }
            val target = File(root,"target.txt").apply { writeText("existing") }
            val second = File(root,"second.txt").apply { writeText("two") }
            val copied = FileTaskHelper.create(FileTaskType.COPY,listOf(FileTaskOp(source.path,target.path),FileTaskOp(second.path,File(root,"second-copy.txt").path))).also { ids.add(it.id) }
            val done = withTimeout(20_000) { FileTaskHelper.wait(copied.id) }
            assertEquals(FileTaskStatus.DONE,done.status)
            assertEquals(12L,done.doneBytes)
            assertEquals(2L,done.doneItems)
            assertEquals("existing",target.readText())
            assertEquals("synthetic",File(root,"target_1.txt").readText())
            assertEquals(File(root,"target_1.txt").path,done.completedOps[0].dst)
            assertEquals(done.completedOps,FileTaskHelper.get(done.id)!!.completedOps)
            assertEquals(FileTaskStatus.DONE,FileTaskHelper.recover(done.id).status)
            assertFalse(File(root,"target_2.txt").exists())
            assertTrue(FileTaskHelper.list(0,0,"").isEmpty())
            val movedRoot = File(root,"moved")
            val moved = FileTaskHelper.create(FileTaskType.MOVE,listOf(FileTaskOp(File(root,"target_1.txt").path,movedRoot.path))).also { ids.add(it.id) }
            assertEquals(FileTaskStatus.DONE,withTimeout(20_000) { FileTaskHelper.wait(moved.id) }.status)
            assertFalse(File(root,"target_1.txt").exists())
            assertEquals("synthetic",movedRoot.readText())
            val failed = FileTaskHelper.create(FileTaskType.COPY,listOf(FileTaskOp(File(root,"missing").path,File(root,"never").path))).also { ids.add(it.id) }
            val error = withTimeout(20_000) { FileTaskHelper.wait(failed.id) }
            assertEquals(FileTaskStatus.ERROR,error.status)
            assertTrue(error.error.isNotEmpty())
            assertTrue(error.completedOps.isEmpty())
            assertEquals(FileTaskStatus.ERROR,FileTaskHelper.recover(error.id).status)
            assertFalse(File(root,"never").exists())
            check(external.mkdirs())
            val externalCopy = FileTaskHelper.create(FileTaskType.COPY,listOf(FileTaskOp(source.path,external.path))).also { ids.add(it.id) }
            assertEquals(FileTaskStatus.DONE,withTimeout(25_000) { FileTaskHelper.wait(externalCopy.id) }.status)
            val externalSource = File(external,source.name)
            assertEquals("synthetic",externalSource.readText())
            val externalTarget = File(external,"moved.txt")
            val externalMove = FileTaskHelper.create(FileTaskType.MOVE,listOf(FileTaskOp(externalSource.path,externalTarget.path))).also { ids.add(it.id) }
            assertEquals(FileTaskStatus.DONE,withTimeout(25_000) { FileTaskHelper.wait(externalMove.id) }.status)
            assertFalse(externalSource.exists())
            assertEquals("synthetic",externalTarget.readText())
            var rejected = false
            try { FileTaskHelper.create(FileTaskType.COPY,listOf(FileTaskOp("/system/test",File(root,"forbidden").path))) }
            catch (_: Exception) { rejected = true }
            assertTrue(rejected)
            val foreign = RustContentApi.query("fileHostTaskRecord(clientId: ${JsonPrimitive("foreign-${UUID.randomUUID()}")}, id: ${JsonPrimitive(done.id)}) { id }")
            assertEquals(JsonNull,foreign.getValue("fileHostTaskRecord"))
            RustSystemState.setApiPermissions(originalPermissions + "WRITE_EXTERNAL_STORAGE")
            assertEquals(true, RustContentApi.mutate("""copyFile(src: "${source.path}", dst: "${File(root,"public-copy.txt").path}", overwrite: false)""")["data"]!!.jsonObject["copyFile"]!!.jsonPrimitive.boolean)
            assertEquals("synthetic",File(root,"public-copy.txt").readText())
            var publicFailed = false
            try { RustContentApi.mutate("""copyFile(src: "/system/test", dst: "${File(root,"public-never").path}", overwrite: false)""") }
            catch (_: Exception) { publicFailed = true }
            assertTrue(publicFailed)
        } finally {
            RustSystemState.setApiPermissions(originalPermissions)
            FileTaskHelper.list(0,1000,"").filter { task -> task.completedOps.any { it.src.startsWith(root.path+"/") } || task.id in ids }.forEach { FileTaskHelper.remove(it.id) }
            root.deleteRecursively()
            external.deleteRecursively()
            com.ismartcoding.plain.platform.scanFileTaskPaths(listOf(File(external,"source.txt").path,File(external,"moved.txt").path))
        }
    }
}
