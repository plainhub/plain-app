package com.ismartcoding.plain.tests

import java.util.UUID
import com.ismartcoding.plain.rust.graphql.GraphqlRequest
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.rust.RustRuntime

import com.ismartcoding.plain.rust.graphql.GraphqlApi

import com.ismartcoding.plain.features.sessions.RustSessionStore
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import android.os.Environment
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.platform.permissions.Permission
import com.ismartcoding.plain.Prefs
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.*
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ApiFixtureRustHttpTest {
    @Test
    fun serveDisposableApiFixtures() = runBlocking {
        check(appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0)
        check(Build.HARDWARE == "ranchu" || Build.HARDWARE == "goldfish")
        check(InstrumentationRegistry.getArguments().getString("plainApiFixture") == "isolated")
        val ready = File(appContext.filesDir, "api-test-session.json")
        val stop = File(appContext.filesDir, "api-test-stop")
        ready.delete()
        stop.delete()
        val root = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "PlainApiFixture/${UUID.randomUUID()}")
        val peerFile = File(appContext.filesDir, "api-test-peer.json")
        val peerFixture = JsonHelper.jsonDecode<ApiPeerFixture>(peerFile.readText())
        peerFile.delete()
        peerFixture.validate()
        val peerId = peerFixture.id
        var channelId = ""
        var clientId = ""
        var peerCreated = false
        var rootCreated = false
        val oldAccess = Prefs.desktopAccess.value
        Prefs.desktopAccess.value = true
        withIO { RustRuntime.start() }
        val oldPermissions = Prefs.apiAccessFeatures.value
        try {
            Prefs.setApiAccessFeatures(setOf(com.ismartcoding.plain.features.web.AccessFeature.IMAGES, com.ismartcoding.plain.features.web.AccessFeature.VIDEOS, com.ismartcoding.plain.features.web.AccessFeature.AUDIO, com.ismartcoding.plain.features.web.AccessFeature.DOCS, com.ismartcoding.plain.features.web.AccessFeature.FILES, com.ismartcoding.plain.features.web.AccessFeature.CONTACTS, com.ismartcoding.plain.features.web.AccessFeature.CALL_LOGS, com.ismartcoding.plain.features.web.AccessFeature.SMS, com.ismartcoding.plain.features.web.AccessFeature.APPS, com.ismartcoding.plain.features.web.AccessFeature.NOTIFICATIONS))
            assertTrue(root.mkdirs())
            rootCreated = true
            val session = RustSessionStore.create("API emulator fixture")
            clientId = session.clientId
            val database = File(appContext.filesDir, "plain.db")
            check(database.isFile && database.canonicalFile.parentFile == appContext.filesDir.canonicalFile)
            val registrationLog = File.createTempFile("api-peer-registration-", ".log", appContext.cacheDir)
            try {
                val classpath = listOf(appContext.applicationInfo.sourceDir, InstrumentationRegistry.getInstrumentation().context.applicationInfo.sourceDir).joinToString(File.pathSeparator)
                val command = ProcessBuilder("/system/bin/app_process", "/system/bin", ApiPeerFixtureDatabase::class.java.name, database.path)
                    .redirectErrorStream(true).redirectOutput(registrationLog)
                command.environment()["CLASSPATH"] = classpath
                val process = command.start()
                process.outputStream.bufferedWriter().use { it.write(JsonHelper.jsonEncode(peerFixture)) }
                if (!process.waitFor(10, TimeUnit.SECONDS)) {
                    process.destroyForcibly()
                    error("API fixture peer registration timed out")
                }
                check(process.exitValue() == 0) { "API fixture peer registration failed: ${registrationLog.readText().takeLast(4000)}" }
                peerCreated = true
            } finally {
                registrationLog.delete()
            }
            check(GraphqlApi.execute(GraphqlRequest<JsonObject>("query { peers { id } }")).getValue("peers").jsonArray.count { it.jsonObject.getValue("id").jsonPrimitive.content == peerId } == 1)
            channelId = GraphqlApi.execute(GraphqlRequest<JsonObject>("mutation { createChatChannel(name: \"API fixture channel\") { id } }")).getValue("createChatChannel").jsonObject.getValue("id").jsonPrimitive.content
            val channelKey = GraphqlApi.execute(GraphqlRequest<JsonObject>("query { chatChannels { id key } }")).getValue("chatChannels").jsonArray.single { it.jsonObject.getValue("id").jsonPrimitive.content == channelId }.jsonObject.getValue("key")
            val app = GraphqlApi.execute(GraphqlRequest<JsonObject>("query { app { httpPort capabilities } }")).getValue("app").jsonObject
            val credentials = JsonHelper.jsonEncode(mapOf(
                "clientId" to JsonPrimitive(clientId),
                "token" to JsonPrimitive(session.token),
                "port" to app.getValue("httpPort"),
                "capabilities" to app.getValue("capabilities"),
                "testRoot" to JsonPrimitive(root.path),
                "dataDir" to JsonPrimitive(appContext.filesDir.path),
                "cacheDir" to JsonPrimitive(appContext.cacheDir.path),
                "peerId" to JsonPrimitive(peerId),
                "peerChannelId" to JsonPrimitive(channelId),
                "peerChannelKey" to channelKey,
            ))
            appContext.openFileOutput(ready.name, Context.MODE_PRIVATE).bufferedWriter().use { it.write(credentials) }
            withTimeout(10 * 60 * 1000L) { while (!stop.exists()) delay(250) }
        } finally {
            ready.delete()
            stop.delete()
            if (clientId.isNotEmpty()) GraphqlApi.execute(GraphqlRequest<JsonObject>("mutation { revokeSession(clientId: ${JsonPrimitive(clientId)}) }"))
            if (channelId.isNotEmpty()) GraphqlApi.execute(GraphqlRequest<JsonObject>("mutation { deleteChatChannel(id: ${JsonPrimitive(channelId)}) }"))
            if (peerCreated) GraphqlApi.execute(GraphqlRequest<JsonObject>("mutation { deletePeer(id: ${JsonPrimitive(peerId)}) }"))
            if (rootCreated) root.deleteRecursively()
            Prefs.setApiAccessFeatures(oldPermissions)
            Prefs.desktopAccess.value = oldAccess
        }
    }
}
