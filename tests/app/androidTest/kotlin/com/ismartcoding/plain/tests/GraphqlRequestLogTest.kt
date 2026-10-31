package com.ismartcoding.plain.tests

import android.os.Process
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import com.ismartcoding.plain.rust.graphql.GraphqlRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class GraphqlRequestLogTest {
    @Test
    fun nativeRequestsLogTheQueryOnSuccessAndFailure() = runBlocking {
        val marker = "graphql-log-${UUID.randomUUID()}"
        Log.i("PlainGraphqlTest", marker)
        GraphqlApi.execute(Query.appCacheSize())
        val failure = runCatching {
            GraphqlApi.execute(GraphqlRequest<Unit>("query { missingFieldForLogTest }"))
        }.exceptionOrNull()
        assertTrue(failure is IllegalStateException)
        val reader = ProcessBuilder("logcat", "-d", "--pid=${Process.myPid()}", "-v", "brief").start()
        val output = reader.inputStream.bufferedReader().use { it.readText() }
        assertTrue(reader.waitFor() == 0)
        assertTrue(output.contains(marker))
        val logs = output.substringAfter(marker).lineSequence().filter { it.contains("GraphQL transport=") }.toList()
        assertTrue(logs.any { it.contains("D/PlainApp") && it.contains("transport=native operation=appCacheSize result=success elapsedMs=") })
        assertTrue(logs.any { it.contains("E/PlainApp") && it.contains("transport=native operation=anonymous result=error error=IllegalStateException elapsedMs=") })
        assertTrue(logs.any { it.contains("query { missingFieldForLogTest }") })
        assertTrue(logs.any { it.contains("query { appCacheSize }") })
    }
}
