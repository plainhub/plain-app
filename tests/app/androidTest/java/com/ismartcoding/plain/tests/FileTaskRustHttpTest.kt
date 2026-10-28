package com.ismartcoding.plain.tests

import com.ismartcoding.plain.rust.graphql.GraphqlRequest
import com.ismartcoding.plain.rust.graphql.GraphqlApi

import android.os.Environment
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ismartcoding.plain.features.files.*
import com.ismartcoding.plain.Prefs
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import com.ismartcoding.plain.models.FileTask
import com.ismartcoding.plain.platform.files.scanFileTaskPaths

@RunWith(AndroidJUnit4::class)
class FileTaskRustHttpTest {
    private suspend fun awaitTask(id: String): FileTask {
        while (true) {
            val task = checkNotNull(TestFileTaskClient.list(0, Int.MAX_VALUE, "").find { it.id == id })
            if (task.status == FileTaskStatus.DONE || task.status == FileTaskStatus.ERROR) return task
            delay(100)
        }
    }

    @Test
    fun fileTasksCopyMoveTrackActualTargetsAndReportRealFailures() = runBlocking {
        val root = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,"file-task-${UUID.randomUUID()}").apply { mkdirs() }
        val external = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"file-task-${UUID.randomUUID()}")
        val originalPermissions = Prefs.apiPermissions.value
        try {
            val source = File(root,"source.txt").apply { writeText("synthetic") }
            val target = File(root,"target.txt").apply { writeText("existing") }
            val second = File(root,"second.txt").apply { writeText("two") }
            val copied = TestFileTaskClient.create(FileTaskType.COPY,listOf(FileTaskOp(source.path,target.path),FileTaskOp(second.path,File(root,"second-copy.txt").path)))
            val done = withTimeout(20_000) { awaitTask(copied.id) }
            assertEquals(FileTaskStatus.DONE,done.status)
            assertEquals(12L,done.doneBytes)
            assertEquals(2L,done.doneItems)
            assertEquals("existing",target.readText())
            assertEquals("synthetic",File(root,"target (1).txt").readText())
            assertEquals(File(root,"target (1).txt").path,done.completedOps[0].dst)
            assertEquals(done.completedOps,TestFileTaskClient.list(0, Int.MAX_VALUE, "").find { it.id == done.id }!!.completedOps)
            assertFalse(File(root,"target (2).txt").exists())
            assertTrue(TestFileTaskClient.list(0,0,"").isEmpty())
            val movedRoot = File(root,"moved")
            val moved = TestFileTaskClient.create(FileTaskType.MOVE,listOf(FileTaskOp(File(root,"target (1).txt").path,movedRoot.path)))
            assertEquals(FileTaskStatus.DONE,withTimeout(20_000) { awaitTask(moved.id) }.status)
            assertFalse(File(root,"target (1).txt").exists())
            assertEquals("synthetic",movedRoot.readText())
            val failed = TestFileTaskClient.create(FileTaskType.COPY,listOf(FileTaskOp(File(root,"missing").path,File(root,"never").path)))
            val error = withTimeout(20_000) { awaitTask(failed.id) }
            assertEquals(FileTaskStatus.ERROR,error.status)
            assertTrue(error.error.isNotEmpty())
            assertTrue(error.completedOps.isEmpty())
            assertFalse(File(root,"never").exists())
            check(external.mkdirs())
            val externalCopy = TestFileTaskClient.create(FileTaskType.COPY,listOf(FileTaskOp(source.path,external.path)))
            assertEquals(FileTaskStatus.DONE,withTimeout(25_000) { awaitTask(externalCopy.id) }.status)
            val externalSource = File(external,source.name)
            assertEquals("synthetic",externalSource.readText())
            val externalTarget = File(external,"moved.txt")
            val externalMove = TestFileTaskClient.create(FileTaskType.MOVE,listOf(FileTaskOp(externalSource.path,externalTarget.path)))
            assertEquals(FileTaskStatus.DONE,withTimeout(25_000) { awaitTask(externalMove.id) }.status)
            assertFalse(externalSource.exists())
            assertEquals("synthetic",externalTarget.readText())
            var rejected = false
            try { TestFileTaskClient.create(FileTaskType.COPY,listOf(FileTaskOp("/system/test",File(root,"forbidden").path))) }
            catch (_: Exception) { rejected = true }
            assertTrue(rejected)
            val spoofedCaller = runCatching { GraphqlApi.execute(GraphqlRequest<JsonObject>("query { " + "fileTasks(clientId: ${JsonPrimitive("foreign-${UUID.randomUUID()}")}, query: \"\", offset: 0, limit: 1) { id }" + " }")) }
            assertTrue(spoofedCaller.isFailure)
            Prefs.setApiPermissions(originalPermissions + "WRITE_EXTERNAL_STORAGE")
            assertEquals(true, GraphqlApi.execute(GraphqlRequest<JsonObject>("mutation { " + """copyFile(src: "${source.path}", dst: "${File(root,"public-copy.txt").path}", overwrite: false)""" + " }")).getValue("copyFile").jsonPrimitive.boolean)
            assertEquals("synthetic",File(root,"public-copy.txt").readText())
            var publicFailed = false
            try { GraphqlApi.execute(GraphqlRequest<JsonObject>("mutation { " + """copyFile(src: "/system/test", dst: "${File(root,"public-never").path}", overwrite: false)""" + " }")) }
            catch (_: Exception) { publicFailed = true }
            assertTrue(publicFailed)
        } finally {
            Prefs.setApiPermissions(originalPermissions)
            root.deleteRecursively()
            external.deleteRecursively()
            scanFileTaskPaths(listOf(File(external,"source.txt").path,File(external,"moved.txt").path))
        }
    }
}
