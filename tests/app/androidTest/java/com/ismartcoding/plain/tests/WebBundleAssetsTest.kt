package com.ismartcoding.plain.tests

import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.rust.RustRuntime

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ismartcoding.plain.Prefs
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipFile

@RunWith(AndroidJUnit4::class)
class WebBundleAssetsTest {
    @Test
    fun webResourcesAreServedByRustWithoutAnApkCopy() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val info = context.applicationInfo
        for (archive in listOf(info.sourceDir) + info.splitSourceDirs.orEmpty()) {
            ZipFile(archive).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val name = entries.nextElement().name
                    assertFalse("Duplicate Web resource: $name", name.startsWith("web/") || name.startsWith("assets/web/"))
                }
            }
        }
        val oldAccess = Prefs.remoteAccess.value
        try {
            Prefs.remoteAccess.value = true
            withIO { RustRuntime.start() }
            val base = "http://127.0.0.1:${Prefs.httpPort.value}"
            val shell = get("$base/", "text/html")
            assertTrue(shell.contains("<html"))
            assertTrue(shell.contains("window.__SERVER_TIME__="))
            val script = Regex("src=\"(/assets/[^\"]+\\.js)\"").find(shell)?.groupValues?.get(1)
            assertNotNull("SPA script missing", script)
            assertTrue(get("$base$script", "application/javascript").isNotEmpty())
            assertTrue(get("$base/settings", "text/html").contains("window.__SERVER_TIME__="))
        } finally {
            Prefs.remoteAccess.value = oldAccess
        }
    }

    private fun get(url: String, contentType: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        try {
            assertEquals(200, connection.responseCode)
            assertTrue(connection.contentType.startsWith(contentType))
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
