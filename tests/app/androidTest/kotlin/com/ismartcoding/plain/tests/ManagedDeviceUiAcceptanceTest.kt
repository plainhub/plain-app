package com.ismartcoding.plain.tests

import com.ismartcoding.plain.ui.resources.smartphone
import com.ismartcoding.plain.ui.resources.srl_header_finish
import com.ismartcoding.plain.ui.resources.srl_header_failed
import com.ismartcoding.plain.ui.resources.srl_header_refreshing
import androidx.activity.compose.setContent
import com.ismartcoding.plain.features.audio.toPlaylistAudio
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.unit.dp
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ismartcoding.plain.MainActivity
import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.platform.system.AppFeatureType
import com.ismartcoding.plain.platform.permissions.Permission
import com.ismartcoding.plain.models.Peer
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import com.ismartcoding.plain.rust.graphql.GraphqlRequest
import com.ismartcoding.plain.ui.i18n.LocaleHelper
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.ismartcoding.plain.features.nearby.PairingMode
import com.ismartcoding.plain.features.nearby.PairingPhase
import com.ismartcoding.plain.features.nearby.RustPairingRuntime
import com.ismartcoding.plain.ui.app.MainViewModel
import android.view.View
import android.view.ViewGroup
import com.ismartcoding.plain.features.files.FileWriteAction
import com.ismartcoding.plain.features.files.FileSortBy
import com.ismartcoding.plain.features.chat.ChatClient
import com.ismartcoding.plain.features.chat.ChatQueries
import com.ismartcoding.plain.features.chat.ChatStatus
import com.ismartcoding.plain.features.chat.data.ChatTarget
import com.ismartcoding.plain.features.chat.data.ChatTargetType
import com.ismartcoding.plain.features.chat.models.MessageText
import android.view.inspector.WindowInspector
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.AnnotatedString
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.Serializable
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@Serializable
private data class ManagedAcceptanceIdentity(val clientId: String, val deviceName: String)

@Serializable
private data class ManagedAcceptancePaths(val appDir: String, val deviceName: String)

@Serializable
private data class ManagedAcceptanceCastItem(val path: String)

@Serializable
private data class ManagedAcceptanceCastState(
    val version: Long,
    val items: List<ManagedAcceptanceCastItem>,
    val currentUri: String,
    val playing: Boolean,
)

@Serializable
private data class ManagedAcceptanceSession(
    val clientId: String,
    val osName: String,
    val osVersion: String,
    val browserName: String,
    val browserVersion: String,
)

@Serializable
private data class ManagedAcceptancePeer(val id: String, val manageable: Boolean)

@Serializable
private data class ManagedAcceptanceDescriptor(val id: String, val peers: List<ManagedAcceptancePeer>)

@Serializable
private data class ManagedFixtureApp(val app: com.ismartcoding.plain.models.ManagedDeviceInfo)

@Serializable
private data class ManagedFixturePrefs(val prefs: Map<String, String>)

@RunWith(AndroidJUnit4::class)
class ManagedDeviceUiAcceptanceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    @org.junit.Before
    fun resetSelectedDevice() {
        instrumentation.runOnMainSync { MainViewModel.selectDevice(null) }
    }

    private val identityRequest = GraphqlRequest<ManagedAcceptanceIdentity>("query { app { clientId deviceName } }", field = "app")

    @Test
    fun shareItemsPickerReadsOnlyItsOwningDevice() = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val requests = java.util.Collections.synchronizedList(mutableListOf<com.ismartcoding.plain.rust.graphql.GraphqlOperation>())
            val source = shareFixtureSource(requests)
            val picked = java.util.concurrent.atomic.AtomicReference<Map<String, Boolean>>()
            scenario.onActivity { activity ->
                activity.setContent {
                    com.ismartcoding.plain.ui.theme.AppTheme(useDarkTheme = false) {
                        androidx.compose.runtime.CompositionLocalProvider(com.ismartcoding.plain.ui.navigation.LocalContentSource provides source) {
                            com.ismartcoding.plain.ui.shares.ShareItemsPickerSheet(
                                initialSelected = emptyMap(), onDismiss = {}, onConfirm = { picked.set(it) },
                            )
                        }
                    }
                }
            }
            awaitNode("target share picker file") { it.text == "target-only-share-item.txt" }
            awaitNode("target share picker root") { it.text == "/target-share-root" }
            click("target-only-share-item.txt", "select target share item")
            click(text(Res.string.done), "confirm target share item")
            awaitValue("confirmed target share item") { picked.get()?.takeIf { it.containsKey("/target-share-root/target-only-share-item.txt") } }
            val files = requests.filter { it.query.contains("files(") }
            assertEquals(1, files.size)
            assertEquals(kotlinx.serialization.json.JsonPrimitive("/target-share-root"), files.single().variables["root"])
        }
    }

    @Test
    fun remoteShareAndDestinationPickersReachTargetMounts() = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val requests = java.util.Collections.synchronizedList(mutableListOf<com.ismartcoding.plain.rust.graphql.GraphqlOperation>())
            val source = shareFixtureSource(requests)
            val picked = java.util.concurrent.atomic.AtomicReference<Map<String, Boolean>>()
            scenario.onActivity { activity ->
                activity.setContent {
                    com.ismartcoding.plain.ui.theme.AppTheme(useDarkTheme = false) {
                        com.ismartcoding.plain.ui.shares.ShareItemsPickerSheet(emptyMap(), {}, { picked.set(it) }, source)
                    }
                }
            }
            awaitNode("initial target app directory") { it.text == "/target-share-root" }
            click("..", "target storage roots")
            click("Target storage", "target primary storage")
            click("target-public-file.txt", "select primary target file")
            click("..", "target storage roots again")
            click("Target secondary", "target secondary storage")
            click("target-secondary-file.txt", "select secondary target file")
            click(text(Res.string.done), "confirm files from target volumes")
            awaitValue("files from both target volumes") { picked.get()?.takeIf { it.size == 2 } }
            assertEquals(setOf("/target-public/target-public-file.txt", "/target-secondary/target-secondary-file.txt"), picked.get().keys)
            val destination = java.util.concurrent.atomic.AtomicReference<String>()
            scenario.onActivity { activity ->
                activity.setContent {
                    com.ismartcoding.plain.ui.theme.AppTheme(useDarkTheme = false) {
                        com.ismartcoding.plain.ui.components.FolderPickSheet({}, { destination.set(it) }, source)
                    }
                }
            }
            awaitNode("destination target app directory") { it.text == "/target-share-root" }
            click("..", "destination target roots")
            click("Target storage", "destination target storage")
            click("target-folder", "destination target folder")
            click(text(Res.string.choose_this_folder), "confirm target directory")
            awaitValue("target directory destination") { destination.get()?.takeIf { it == "/target-public/target-folder" } }
            assertTrue(synchronized(requests) { requests.any { it.query.contains("mounts {") } })
            assertTrue(synchronized(requests) { requests.filter { it.query.contains("files(") }.all { it.variables["root"].toString().contains("/target-") } })
        }
    }

    @Test
    fun remoteSharePickerKeepsItsSheetWhenTheTargetReadFails() = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val requests = java.util.Collections.synchronizedList(mutableListOf<com.ismartcoding.plain.rust.graphql.GraphqlOperation>())
            val source = shareFixtureSource(requests, failRead = { it.query.contains("files(") })
            instrumentation.runOnMainSync { com.ismartcoding.plain.ui.components.ToastManager.current.value = null }
            scenario.onActivity { activity ->
                activity.setContent {
                    com.ismartcoding.plain.ui.theme.AppTheme(useDarkTheme = false) {
                        com.ismartcoding.plain.ui.shares.ShareItemsPickerSheet(emptyMap(), {}, {}, source)
                    }
                }
            }
            awaitValue("friendly target directory failure") {
                var error: String? = null
                instrumentation.runOnMainSync { error = com.ismartcoding.plain.ui.components.ToastManager.current.value?.message }
                error?.takeIf { it == text(Res.string.load_failed) }
            }
            awaitNode("share picker retained after target failure") { it.text == text(Res.string.add_items) }
            awaitNode("target directory retained after failure") { it.text == "/target-share-root" }
            assertEquals(1, synchronized(requests) { requests.count { it.query.contains("files(") } })
        }
    }

    @Test
    fun remoteShareEditorReportsTargetLoadAndSaveFailures() = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val stage = InstrumentationRegistry.getArguments().getString("managedShareFailure") ?: "SAVE"
            require(stage == "LOAD" || stage == "SAVE")
            val operationName = if (stage == "LOAD") "shareRecord(" else "updateShare("
            val requests = java.util.Collections.synchronizedList(mutableListOf<com.ismartcoding.plain.rust.graphql.GraphqlOperation>())
            val source = shareFixtureSource(requests, failRead = { it.query.contains(operationName) })
            instrumentation.runOnMainSync { com.ismartcoding.plain.ui.components.ToastManager.current.value = null }
            scenario.onActivity { activity ->
                activity.setContent {
                    com.ismartcoding.plain.ui.theme.AppTheme(useDarkTheme = false) {
                        val navigation = androidx.navigation.compose.rememberNavController()
                        androidx.compose.runtime.CompositionLocalProvider(com.ismartcoding.plain.ui.navigation.LocalContentSource provides source) {
                            com.ismartcoding.plain.ui.shares.EditSharePage(navigation, "share-ui-fixture")
                        }
                    }
                }
            }
            if (stage == "SAVE") {
                awaitNode("target share editor") { it.text?.contains("target-share-root/existing.txt") == true }
                click(text(Res.string.save), "save target share")
            }
            awaitValue("reported target share failure") {
                var message: String? = null
                instrumentation.runOnMainSync { message = com.ismartcoding.plain.ui.components.ToastManager.current.value?.message }
                message?.takeIf { it == if (stage == "LOAD") text(Res.string.load_failed) else "connection_timeout" }
            }
            awaitNode("share editor retained after failure") { it.text == text(Res.string.edit_share_link) }
            if (stage == "SAVE") {
                awaitNode("save enabled after failure") { it.text == text(Res.string.save) && generateSequence(it) { node -> node.parent }.any { node -> node.isClickable && node.isEnabled } }
                awaitNode("target share paths retained after failure") { it.text?.contains("target-share-root/existing.txt") == true }
            }
            assertEquals(1, synchronized(requests) { requests.count { it.query.contains(operationName) } })
        }
    }

    @Test
    fun remoteShareForwardingWritesOnlyTheOwningDeviceChat() = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val before = GraphqlApi.execute(Query.chatConversations())
            val requests = java.util.Collections.synchronizedList(mutableListOf<com.ismartcoding.plain.rust.graphql.GraphqlOperation>())
            val source = shareFixtureSource(requests)
            scenario.onActivity { activity ->
                activity.setContent {
                    com.ismartcoding.plain.ui.theme.AppTheme(useDarkTheme = false) {
                        val navigation = androidx.navigation.compose.rememberNavController()
                        androidx.compose.runtime.CompositionLocalProvider(com.ismartcoding.plain.ui.navigation.LocalContentSource provides source) {
                            com.ismartcoding.plain.ui.shares.EditSharePage(navigation, "share-ui-fixture")
                        }
                    }
                }
            }
            awaitNode("target share editor") { it.text?.contains("target-share-root/existing.txt") == true }
            click(text(Res.string.forward), "target share forward dialog")
            click(text(Res.string.local_chat), "target device local conversation")
            var container = checkNotNull(awaitNode("one forward recipient") { it.text == "1" }.parent)
            while (nodes(container).none { it.isClickable } && container.parent != null) container = checkNotNull(container.parent)
            performClick(nodes(container).last { it.isClickable }, "send target share")
            val sent = awaitValue("target share send request") {
                synchronized(requests) { requests.firstOrNull { it.query.contains("sendShare(") } }
            }
            assertEquals(kotlinx.serialization.json.JsonPrimitive("share-ui-fixture"), sent.variables["shareId"])
            assertEquals(kotlinx.serialization.json.JsonArray(listOf(kotlinx.serialization.json.JsonPrimitive("peer:local"))), sent.variables["targets"])
            assertEquals(before, GraphqlApi.execute(Query.chatConversations()))
        }
    }

    @Test
    fun realRemoteSharedBrowserCachesAndDownloadsOnlyOnItsTarget() = runBlocking {
        check(instrumentation.targetContext.packageName == "com.ismartcoding.plain.managedtest")
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val targetId = checkNotNull(InstrumentationRegistry.getArguments().getString("managedPeerId"))
            val peer = checkNotNull(GraphqlApi.execute(Query.peers()).find { it.id == targetId })
            val client = com.ismartcoding.plain.features.chat.peer.ManagedDeviceClient.load(peer)
            val source = com.ismartcoding.plain.rust.graphql.ContentSource.Remote(client)
            val remote = client.api
            val localDownloads = GraphqlApi.execute(Query.sharedDownloadQueue()).tasks
            val marker = "managed-real-share-${UUID.randomUUID()}"
            val directory = source.info.appDir.trimEnd('/') + "/" + marker
            val fileName = "$marker.txt"
            val originalPath = "$directory/$fileName"
            val downloadedPath = source.info.downloadsDir.trimEnd('/') + "/PlainApp/" + fileName
            val contents = "Synthetic target-owned shared file $marker"
            var shareId: String? = null
            var messageId: String? = null
            var cachedPath: String? = null
            remote.execute(Mutation.fileWrite(FileWriteAction.CREATE_DIRECTORY, directory, "", false))
            try {
                remote.execute(Mutation.fileWrite(FileWriteAction.WRITE_TEXT, originalPath, contents, false))
                val share = remote.execute(Mutation.createShare(marker, listOf(originalPath), true, null))
                shareId = share.id
                val message = remote.execute(Mutation.sendShare(listOf("peer:local"), share.id)).single()
                messageId = message.id
                assertNull(GraphqlApi.execute(Query.chatItem(message.id)))
                val card = checkNotNull(message.messageContent?.value as? com.ismartcoding.plain.features.chat.models.MessageShare)
                val browsed = checkNotNull(source.sharedLinks.browse(message.id, card, null))
                val entry = browsed.info.entries.single { it.name == fileName }
                val path = source.sharedLinks.cacheFile(browsed.link, browsed.info.urlToken, entry)
                cachedPath = path
                assertFalse("Remote shared cache must not be created on the controller", java.io.File(path).exists())
                val cached = remote.execute(Query.files(path.substringBeforeLast('/'), "", FileSortBy.NAME_ASC, 0, 1000)).single { it.path == path }
                assertEquals(contents.encodeToByteArray().size.toLong(), cached.size)
                scenario.onActivity { activity ->
                    activity.setContent {
                        com.ismartcoding.plain.ui.theme.AppTheme(useDarkTheme = false) {
                            val navigation = androidx.navigation.compose.rememberNavController()
                            androidx.compose.runtime.CompositionLocalProvider(com.ismartcoding.plain.ui.navigation.LocalContentSource provides source) {
                                com.ismartcoding.plain.ui.shares.browser.SharedFolderPage(navigation, message.id)
                            }
                        }
                    }
                }
                awaitNode("real target shared entry") { it.text == fileName }
                click(text(Res.string.download), "download target shared entry")
                click(text(Res.string.download_to_downloads), "save into target Downloads")
                val completed = awaitValue("target shared transfer completes") {
                    remote.execute(Query.sharedDownloadQueue()).tasks.find { it.messageId == message.id }?.let { task ->
                        assertFalse("Target shared download failed: ${task.error}; ${task.failures}", task.status in setOf(
                            com.ismartcoding.plain.features.downloads.DownloadStatus.FAILED,
                            com.ismartcoding.plain.features.downloads.DownloadStatus.PARTIAL,
                            com.ismartcoding.plain.features.downloads.DownloadStatus.CANCELED,
                        ))
                        task.takeIf { it.status == com.ismartcoding.plain.features.downloads.DownloadStatus.COMPLETED }
                    }
                }
                assertEquals(contents.encodeToByteArray().size.toLong(), completed.downloadedSize)
                assertEquals(1L, completed.doneFiles)
                val downloaded = remote.execute(Query.files(downloadedPath.substringBeforeLast('/'), "", FileSortBy.NAME_ASC, 0, 1000)).single { it.path == downloadedPath }
                assertEquals(contents.encodeToByteArray().size.toLong(), downloaded.size)
                assertFalse("Target Downloads must not be written on the controller", java.io.File(downloadedPath).exists())
                assertEquals(localDownloads, GraphqlApi.execute(Query.sharedDownloadQueue()).tasks)
                report("remote_shared_browser", "real_http_browse_target_cache_download_progress_and_controller_isolation_verified")
            } finally {
                messageId?.let { id ->
                    remote.execute(Query.sharedDownloadQueue()).tasks.filter { it.messageId == id }.forEach { task ->
                        remote.execute(Mutation.sharedDownloadControl(com.ismartcoding.plain.features.shares.SharedDownloadCommand.REMOVE, task.id))
                    }
                    remote.execute(Mutation.deleteChatItem(id))
                }
                shareId?.let { remote.execute(Mutation.deleteShare(it)) }
                val cleanupPaths = listOfNotNull(directory, cachedPath, downloadedPath)
                remote.execute(Mutation.deleteFiles(cleanupPaths))
                assertEquals(localDownloads, GraphqlApi.execute(Query.sharedDownloadQueue()).tasks)
            }
        }
    }

    @Test
    fun targetConversationShareCardLoadsFromTheOwningDevice() = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val id = "share-card-ui-fixture"
            val nativeDownloads = GraphqlApi.execute(Query.sharedDownloadQueue()).tasks
            assertNull(GraphqlApi.execute(Query.chatItem(id)))
            val requests = java.util.Collections.synchronizedList(mutableListOf<com.ismartcoding.plain.rust.graphql.GraphqlOperation>())
            val source = shareFixtureSource(requests)
            scenario.onActivity { activity ->
                activity.setContent {
                    com.ismartcoding.plain.ui.theme.AppTheme(useDarkTheme = false) {
                        val navigation = androidx.navigation.compose.rememberNavController()
                        androidx.compose.runtime.CompositionLocalProvider(com.ismartcoding.plain.ui.navigation.LocalContentSource provides source) {
                            com.ismartcoding.plain.ui.shares.browser.SharedFolderPage(navigation, id)
                        }
                    }
                }
            }
            awaitNode("target conversation shared entry") { it.text == "target-card-file.txt" }
            assertTrue(synchronized(requests) { requests.any { it.query.contains("chatItem(") && it.variables["id"] == kotlinx.serialization.json.JsonPrimitive(id) } })
            assertTrue(synchronized(requests) { requests.any { it.query.contains("browseSharedLink(") } })
            click(text(Res.string.download), "save shared target entry")
            click(text(Res.string.download_to_downloads), "target Downloads destination")
            val enqueued = awaitValue("target shared download enqueue") {
                synchronized(requests) { requests.firstOrNull { it.query.contains("enqueueSharedDownload(") } }
            }
            val input = JsonHelper.jsonDecodeFromElement<com.ismartcoding.plain.features.shares.SharedDownloadInput>(enqueued.variables.getValue("input"))
            assertEquals(id, input.messageId)
            assertEquals(listOf("/root/target-card-file.txt"), input.entries.map { it.relativePath })
            click("Target shared download", "target download summary")
            click(text(Res.string.cancel), "cancel target download")
            awaitValue("target shared download cancel") {
                synchronized(requests) { requests.firstOrNull { it.query.contains("cancelSharedDownload(") } }
            }
            click(text(Res.string.completed), "finished target downloads")
            click(text(Res.string.cancel), "remove finished target download")
            awaitValue("target shared download remove") {
                synchronized(requests) { requests.firstOrNull { it.query.contains("removeSharedDownload(") } }
            }
            assertEquals(nativeDownloads, GraphqlApi.execute(Query.sharedDownloadQueue()).tasks)
            assertNull(GraphqlApi.execute(Query.chatItem(id)))
        }
    }

    @Test
    fun remoteForwardRecipientsFollowOnlyTheirTargetUpdates() = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val requests = java.util.Collections.synchronizedList(mutableListOf<com.ismartcoding.plain.rust.graphql.GraphqlOperation>())
            val targetPeers = java.util.concurrent.atomic.AtomicReference<List<Peer>>(emptyList())
            val updates = kotlinx.coroutines.flow.MutableSharedFlow<com.ismartcoding.plain.rust.graphql.SubscriptionUpdate<com.ismartcoding.plain.rust.graphql.GraphqlResponse>>(replay = 1)
            val opens = java.util.concurrent.atomic.AtomicInteger()
            val closed = java.util.concurrent.atomic.AtomicInteger()
            val visible = androidx.compose.runtime.mutableStateOf(true)
            val source = shareFixtureSource(requests, kotlinx.coroutines.flow.flow {
                opens.incrementAndGet()
                try { updates.collect { emit(it) } }
                finally { closed.incrementAndGet() }
            }, readPeers = { targetPeers.get() })
            scenario.onActivity { activity ->
                activity.setContent {
                    com.ismartcoding.plain.ui.theme.AppTheme(useDarkTheme = false) {
                        androidx.compose.runtime.CompositionLocalProvider(com.ismartcoding.plain.ui.navigation.LocalContentSource provides source) {
                            androidx.compose.foundation.layout.Column {
                                if (visible.value) {
                                    com.ismartcoding.plain.ui.chat.components.chatTargetOptions().forEach { androidx.compose.material3.Text(it.title) }
                                } else androidx.compose.material3.Text("Recipient picker closed")
                            }
                        }
                    }
                }
            }
            awaitNode("local destination") { it.text == text(Res.string.local_chat) }
            awaitValue("initial target recipient read") { synchronized(requests) { requests.firstOrNull { it.query.contains("peers {") } } }
            targetPeers.set(listOf(Peer(
                id = "fixture-recipient", name = "Target recipient", ip = "", port = 8443,
                status = com.ismartcoding.plain.features.nearby.PeerStatus.PAIRED,
                deviceType = com.ismartcoding.plain.platform.system.DeviceType.PHONE,
                createdAt = kotlin.time.Instant.fromEpochMilliseconds(0), updatedAt = kotlin.time.Instant.fromEpochMilliseconds(0),
                publicKey = "recipient-key",
            )))
            updates.emit(com.ismartcoding.plain.rust.graphql.SubscriptionUpdate.Data(com.ismartcoding.plain.rust.graphql.GraphqlResponse(data = JsonHelper.jsonDecode("""{"peersChanged":true}"""))))
            awaitNode("new target recipient after target event") { it.text == "Target recipient" }
            val recipient = targetPeers.get().single()
            targetPeers.set(emptyList())
            updates.emit(com.ismartcoding.plain.rust.graphql.SubscriptionUpdate.Data(com.ismartcoding.plain.rust.graphql.GraphqlResponse(data = JsonHelper.jsonDecode("""{"peersChanged":true}"""))))
            awaitValue("target recipient removed after target event") {
                var visible = true
                instrumentation.runOnMainSync { visible = currentNodes().any { it.text == "Target recipient" } }
                if (!visible) true else null
            }
            assertEquals("Recipient ViewModels share one target subscription", 1, opens.get())
            instrumentation.runOnMainSync { visible.value = false }
            awaitNode("closed recipient picker") { it.text == "Recipient picker closed" }
            awaitValue("recipient target subscription released") { closed.get().takeIf { it == 1 } }
            targetPeers.set(listOf(recipient.copy(name = "Updated target recipient")))
            instrumentation.runOnMainSync { visible.value = true }
            awaitNode("recipient after picker reopens") { it.text == "Updated target recipient" }
            assertEquals(2, opens.get())
        }
    }

    @Test
    fun nativeSharedPreviewUsesItsExistingLoopbackResourceAndLocalCache() = runBlocking {
        check(instrumentation.targetContext.packageName == "com.ismartcoding.plain.managedtest")
        ActivityScenario.launch(MainActivity::class.java).use {
            val root = java.io.File(instrumentation.targetContext.cacheDir, "owned-shared-preview-${UUID.randomUUID()}").apply { mkdirs() }
            val file = java.io.File(root, "preview.txt").apply { writeText("Synthetic shared preview") }
            val image = java.io.File(root, "wide.jpg")
            android.graphics.Bitmap.createBitmap(640, 320, android.graphics.Bitmap.Config.ARGB_8888).let { bitmap ->
                try {
                    bitmap.eraseColor(android.graphics.Color.GREEN)
                    image.outputStream().use { check(bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, it)) }
                } finally { bitmap.recycle() }
            }
            var shareId: String? = null
            var cached: java.io.File? = null
            var thumbnail: java.io.File? = null
            try {
                val share = com.ismartcoding.plain.features.shares.SharesClient.createShare("Owned preview", listOf(file.absolutePath, image.absolutePath), true, null)
                shareId = share.id
                val link = com.ismartcoding.plain.features.shares.SharedLinkClient.ownLink(share.id, "127.0.0.1")
                val path = com.ismartcoding.plain.features.shares.SharedLinkClient.cacheFile(
                    link, share.urlToken,
                    com.ismartcoding.plain.features.shares.SharedFileDto(file.name, share.roots.first { it.realPath == file.canonicalPath }.relativePath, size = file.length(), mimeType = "text/plain"),
                )
                val preview = java.io.File(path).canonicalFile
                assertTrue(preview.path.startsWith(instrumentation.targetContext.cacheDir.canonicalPath + "/"))
                cached = preview
                assertEquals(file.readText(), preview.readText())
                val thumbnailPath = com.ismartcoding.plain.features.shares.SharedLinkClient.cacheFile(
                    link, share.urlToken,
                    com.ismartcoding.plain.features.shares.SharedFileDto(image.name, share.roots.first { it.realPath == image.canonicalPath }.relativePath, size = image.length(), mimeType = "image/jpeg", hasThumb = true),
                    thumbnail = true,
                )
                val thumbnailFile = java.io.File(thumbnailPath).canonicalFile
                assertTrue(thumbnailFile.path.startsWith(instrumentation.targetContext.cacheDir.canonicalPath + "/"))
                thumbnail = thumbnailFile
                val bitmap = checkNotNull(android.graphics.BitmapFactory.decodeFile(thumbnailPath))
                try {
                    assertEquals("Shared thumbnail must preserve the original 2:1 aspect ratio", bitmap.height * 2, bitmap.width)
                    assertTrue(bitmap.width <= 200 && bitmap.height <= 200)
                    report("native_shared_thumbnail", "${bitmap.width}x${bitmap.height}")
                } finally { bitmap.recycle() }
            } finally {
                shareId?.let { com.ismartcoding.plain.features.shares.SharesClient.deleteShare(it) }
                cached?.let { check(it.delete()) }
                thumbnail?.let { check(it.delete()) }
                check(root.deleteRecursively())
            }
        }
    }

    private suspend fun shareFixtureSource(
        requests: MutableList<com.ismartcoding.plain.rust.graphql.GraphqlOperation>,
        updates: kotlinx.coroutines.flow.Flow<com.ismartcoding.plain.rust.graphql.SubscriptionUpdate<com.ismartcoding.plain.rust.graphql.GraphqlResponse>> = kotlinx.coroutines.flow.emptyFlow(),
        readPeers: () -> List<Peer> = { emptyList() },
        failRead: (com.ismartcoding.plain.rust.graphql.GraphqlOperation) -> Boolean = { false },
        capabilities: List<com.ismartcoding.plain.platform.system.Capability> = emptyList(),
        readFeatures: () -> String = { "NOTES|FILES" },
    ): com.ismartcoding.plain.rust.graphql.ContentSource.Remote {
        val peer = com.ismartcoding.plain.models.Peer(
            id = "share-ui-fixture-peer", name = "Target fixture", ip = "", port = 8443,
            status = com.ismartcoding.plain.features.nearby.PeerStatus.PAIRED,
            deviceType = com.ismartcoding.plain.platform.system.DeviceType.PHONE,
            createdAt = kotlin.time.Instant.fromEpochMilliseconds(0), updatedAt = kotlin.time.Instant.fromEpochMilliseconds(0),
            publicKey = "fixture-key", token = "fixture-token",
            address = com.ismartcoding.plain.models.PeerAddress("device.test", "Target fixture", "https://device.test:8443", "", ""),
        )
        val card = com.ismartcoding.plain.features.chat.models.MessageShare(
            "fixture-shared-link", "fixture-link-key", com.ismartcoding.plain.features.chat.models.SharePeerInfo("link-peer", "device.test", 8443), "Target folder",
        )
        val chat = com.ismartcoding.plain.models.ChatItem(
            id = "share-card-ui-fixture", content = JsonHelper.jsonEncode(com.ismartcoding.plain.features.chat.models.MessageContent(com.ismartcoding.plain.features.chat.models.MessageType.SHARE, card)),
        )
        val browsed = com.ismartcoding.plain.features.shares.SharedBrowseResult(
            com.ismartcoding.plain.features.shares.SharedInfoDto(name = "Target folder", urlToken = "fixture-link-key", entries = listOf(com.ismartcoding.plain.features.shares.SharedFileDto(name = "target-card-file.txt", relativePath = "/root/target-card-file.txt", size = 8, mimeType = "text/plain"))),
            com.ismartcoding.plain.features.shares.SharedLink("device.test", 8443, "fixture-shared-link", "fixture-token", "https://device.test:8443/share/fixture"), card,
        )
        val downloads = java.util.concurrent.atomic.AtomicReference(com.ismartcoding.plain.models.SharedDownloadSnapshot("fixture-runtime", 0, emptyList()))
        val http = com.ismartcoding.plain.rust.graphql.PeerHttpClient(
            peer, peers = { listOf(peer) }, clientId = { "fixture-client" },
            encrypt = { _, payload -> kotlin.io.encoding.Base64.encode(payload.encodeToByteArray()) },
            decrypt = { _, _, response -> response },
            transport = object : com.ismartcoding.plain.rust.graphql.GraphqlHttpTransport {
                override suspend fun post(url: String, clientId: String, body: ByteArray, contentType: String, tlsCertificateSha256: String?): com.ismartcoding.plain.rust.graphql.GraphqlHttpResponse {
                    val operation = JsonHelper.jsonDecode<com.ismartcoding.plain.rust.graphql.GraphqlOperation>(body.decodeToString())
                    requests.add(operation)
                    if (failRead(operation)) error("connection_timeout")
                    val response = when {
                        operation.query.contains("app {") -> JsonHelper.jsonEncode(com.ismartcoding.plain.rust.graphql.GraphqlResponse(
                            data = JsonHelper.jsonEncodeToElement(ManagedFixtureApp(com.ismartcoding.plain.models.ManagedDeviceInfo(
                                "Target fixture", capabilities, listOf(Permission.WRITE_EXTERNAL_STORAGE), "/target-share-root", "/target-downloads", "fixture-resource-token", "fixture-certificate",
                            ))).jsonObject,
                        ))
                        operation.query.contains("{ prefs }") -> JsonHelper.jsonEncode(com.ismartcoding.plain.rust.graphql.GraphqlResponse(
                            data = JsonHelper.jsonEncodeToElement(ManagedFixturePrefs(mapOf("app_home_features" to readFeatures()))).jsonObject,
                        ))
                        operation.query.contains("setSavedValue(") -> """{"data":{"setSavedValue":true}}"""
                        operation.query.contains("shareRecord(") -> """{"data":{"shareRecord":{"id":"share-ui-fixture","name":"Target fixture share","password":"","urlToken":"fixture-resource-token","readOnly":true,"expiresAt":null,"createdAt":"2026-01-01T00:00:00Z","updatedAt":"2026-01-01T00:00:00Z","roots":[{"relativePath":"root","realPath":"/target-share-root/existing.txt","isDir":false}]}}}"""
                        operation.query.contains("shareLink(") -> """{"data":{"shareLink":{"host":"device.test","port":8443,"sharedId":"share-ui-fixture","token":"fixture-share-token","pageUrl":"https://device.test:8443/share/fixture"}}}"""
                        operation.query.contains("sendShare(") -> """{"data":{"sendShare":[]}}"""
                        operation.query.contains("peers {") -> JsonHelper.jsonEncode(mapOf("data" to mapOf("peers" to JsonHelper.jsonEncodeToElement(readPeers()))))
                        operation.query.contains("chatChannels") || operation.query.contains("chatConversations") -> """{"data":{"chatChannels":[],"chatConversations":[],"peers":[]}}"""
                        operation.query.contains("chatItem(") -> JsonHelper.jsonEncode(mapOf("data" to mapOf("chatItem" to JsonHelper.jsonEncodeToElement(chat))))
                        operation.query.contains("browseSharedLink(") -> JsonHelper.jsonEncode(mapOf("data" to mapOf("browseSharedLink" to JsonHelper.jsonEncodeToElement(browsed))))
                        operation.query.contains("sharedDownloadQueue") -> JsonHelper.jsonEncode(mapOf("data" to mapOf("sharedDownloadQueue" to JsonHelper.jsonEncodeToElement(downloads.get()))))
                        operation.query.contains("savedValue(") -> """{"data":{"savedValue":null}}"""
                        operation.query.contains("enqueueSharedDownload(") -> {
                            val input = JsonHelper.jsonDecodeFromElement<com.ismartcoding.plain.features.shares.SharedDownloadInput>(operation.variables.getValue("input"))
                            downloads.updateAndGet { current -> current.copy(revision = current.revision + 1, tasks = listOf(com.ismartcoding.plain.models.SharedDownloadRow(
                                "fixture-download", input.messageId, input.kind, "Target shared download", input.targetDir,
                                input.link, input.urlToken, input.entries, input.zipName,
                                com.ismartcoding.plain.features.downloads.DownloadStatus.DOWNLOADING, "", 4, 8, 4, 1, 0, 0, "", false, emptyList(),
                            ))) }
                            """{"data":{}}"""
                        }
                        operation.query.contains("cancelSharedDownload(") -> {
                            downloads.updateAndGet { current -> current.copy(revision = current.revision + 1, tasks = current.tasks.map { it.copy(status = com.ismartcoding.plain.features.downloads.DownloadStatus.CANCELED) }) }
                            """{"data":{}}"""
                        }
                        operation.query.contains("removeSharedDownload(") -> {
                            downloads.updateAndGet { current -> current.copy(revision = current.revision + 1, tasks = emptyList()) }
                            """{"data":{}}"""
                        }
                        operation.query.contains("mounts {") -> """{"data":{"mounts":[{"name":"Target storage","path":"/target-public"},{"name":"Target secondary","path":"/target-secondary"}]}}"""
                        operation.query.contains("files(") -> when (operation.variables["root"]) {
                            kotlinx.serialization.json.JsonPrimitive("/target-share-root") -> """{"data":{"files":[{"name":"target-only-share-item.txt","path":"/target-share-root/target-only-share-item.txt","createdAt":null,"updatedAt":"2026-01-01T00:00:00Z","size":8,"isDir":false,"childCount":0}]}}"""
                            kotlinx.serialization.json.JsonPrimitive("/target-public") -> """{"data":{"files":[{"name":"target-public-file.txt","path":"/target-public/target-public-file.txt","createdAt":null,"updatedAt":"2026-01-01T00:00:00Z","size":8,"isDir":false,"childCount":0},{"name":"target-folder","path":"/target-public/target-folder","createdAt":null,"updatedAt":"2026-01-01T00:00:00Z","size":0,"isDir":true,"childCount":0}]}}"""
                            kotlinx.serialization.json.JsonPrimitive("/target-secondary") -> """{"data":{"files":[{"name":"target-secondary-file.txt","path":"/target-secondary/target-secondary-file.txt","createdAt":null,"updatedAt":"2026-01-01T00:00:00Z","size":8,"isDir":false,"childCount":0}]}}"""
                            kotlinx.serialization.json.JsonPrimitive("/target-public/target-folder") -> """{"data":{"files":[]}}"""
                            else -> error("Unexpected target fixture directory")
                        }
                        else -> error("Unexpected share fixture operation")
                    }
                    return com.ismartcoding.plain.rust.graphql.GraphqlHttpResponse(200, response.encodeToByteArray())
                }
            },
        )
        return com.ismartcoding.plain.rust.graphql.ContentSource.Remote(com.ismartcoding.plain.features.chat.peer.ManagedDeviceClient.load(peer, http, updates = { updates }))
    }

    @Test
    fun localRootStartsWithChatAndKeepsServicesLastAndSettingsAccessible() = runBlocking {
        instrumentation.runOnMainSync {
            Prefs.onboardingCompleted.value = true
            MainViewModel.selectDevice(null)
            MainViewModel.currentRootTab.intValue = 1
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            awaitNode("Chat settings action") { it.contentDescription == text(Res.string.settings) }
            click(text(Res.string.select_a_device), "device sheet without pairs")
            val rename = awaitNode("rename-only sheet") { it.overlay && it.contentDescription == text(Res.string.rename) }
            val renameItem = generateSequence(rename) { it.parent }.first { it.isClickable }
            val sheetTitle = awaitNode("device sheet title") { it.overlay && it.text == text(Res.string.select_a_device) }
            assertTrue(renameItem.boundsInWindow.left > sheetTitle.boundsInWindow.right)
            assertTrue(kotlin.math.abs(renameItem.boundsInWindow.center.y - sheetTitle.boundsInWindow.center.y) < renameItem.boundsInWindow.height)
            assertFalse(uiNodes().any { it.overlay && it.contentDescription == text(Res.string.close) })
            val display = instrumentation.targetContext.resources.displayMetrics
            assertTrue(renameItem.boundsInWindow.left > 0)
            assertTrue(renameItem.boundsInWindow.right < display.widthPixels)
            assertTrue(renameItem.boundsInWindow.top > display.heightPixels / 2)
            captureNavigation("rename-only-sheet")
            assertFalse(allText().contains(text(Res.string.pair_device)))
            assertFalse(allText().contains(text(Res.string.this_device)))
            click(text(Res.string.rename), "rename selected device")
            awaitNode("rename input") { it.isEditable }
            click(text(Res.string.cancel), "cancel rename")
            awaitMainWindowFocus(scenario)
            assertEquals(1, MainViewModel.currentRootTab.intValue)
            captureNavigation("local-chat")
            click(text(Res.string.tools), "local tools tab")
            click(text(Res.string.add), "local tools actions")
            click(text(Res.string.customize_home_features), "local customization remains available")
            awaitNode("local customization page") { it.text == text(Res.string.customize_home_features) }
            awaitMainWindowFocus(scenario)
            click(text(Res.string.back), "return from local customization")
            click(text(Res.string.services), "services tab")
            awaitNode("Remote Access service") { it.text == text(Res.string.home_remote_access) }
            assertEquals(0, MainViewModel.currentRootTab.intValue)
            captureNavigation("local-services")
            click(text(Res.string.chat), "chat tab")
            click(text(Res.string.settings), "app settings")
            awaitNode("application settings") { it.text == text(Res.string.dark_theme) }
            click(text(Res.string.back), "return from app settings")
            awaitNode("Chat settings action after return") { it.contentDescription == text(Res.string.settings) }
            assertEquals(1, MainViewModel.currentRootTab.intValue)
        }
    }

    @Test
    fun unavailableSelectedDeviceKeepsSettingsAndCanSwitchBackToLocal() = runBlocking<Unit> {
        instrumentation.runOnMainSync {
            Prefs.onboardingCompleted.value = true
            MainViewModel.selectDevice(null)
            MainViewModel.currentRootTab.intValue = 1
        }
        val now = kotlin.time.Instant.fromEpochMilliseconds(0)
        val peer = Peer(
            id = "unavailable-fixture", name = "Remote device for testing", ip = "",
            status = com.ismartcoding.plain.features.nearby.PeerStatus.PAIRED, port = 8443,
            deviceType = com.ismartcoding.plain.platform.system.DeviceType.PHONE,
            createdAt = now, updatedAt = now, publicKey = "fixture-public-key", token = "fixture-token",
        )
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    com.ismartcoding.plain.ui.theme.AppTheme(useDarkTheme = false) {
                        val nav = androidx.compose.runtime.key(MainViewModel.selectedDevice.value) { androidx.navigation.compose.rememberNavController() }
                        val peers = androidx.lifecycle.viewmodel.compose.viewModel(key = "navigation-fixture-peers") {
                            com.ismartcoding.plain.ui.chat.PeerViewModel(readPeers = { listOf(peer) }, readLatestChats = { emptyMap() })
                        }
                        com.ismartcoding.plain.ui.navigation.MainNavGraph(
                            navController = nav,
                            peerVM = peers,
                            channelVM = androidx.lifecycle.viewmodel.compose.viewModel { com.ismartcoding.plain.ui.chat.ChannelViewModel() },
                            audioQueueVM = androidx.lifecycle.viewmodel.compose.viewModel { com.ismartcoding.plain.ui.audio.AudioQueueViewModel() },
                            pomodoroVM = androidx.lifecycle.viewmodel.compose.viewModel { com.ismartcoding.plain.ui.pomodoro.PomodoroViewModel() },
                            noteTagsVM = androidx.lifecycle.viewmodel.compose.viewModel(key = "fixture-note-tags") { com.ismartcoding.plain.ui.tags.TagsViewModel() },
                            platformPages = null, batterySettings = null,
                        )
                    }
                }
            }
            click(text(Res.string.select_a_device), "device selector")
            val sheetName = awaitNode("device selection sheet") { it.overlay && it.text == peer.name }
            instrumentation.runOnMainSync {
                val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                assertTrue(sheetName.semantic.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts) == true)
                assertEquals(1, layouts.single().lineCount)
                assertFalse(layouts.single().isLineEllipsized(0))
            }
            assertFalse(allText().contains(text(Res.string.pair_device)))
            assertFalse(allText().contains(text(Res.string.online)))
            assertFalse(allText().contains(text(Res.string.offline)))
            val deviceRow = generateSequence(sheetName) { it.parent }.first { it.isClickable }
            assertTrue(deviceRow.boundsInWindow.left > 0)
            assertTrue(deviceRow.boundsInWindow.right < instrumentation.targetContext.resources.displayMetrics.widthPixels)
            captureNavigation("device-sheet")
            click(peer.name, "unavailable device")
            awaitNode("remote authorization error") { it.text == text(Res.string.managed_authorization_changed) }
            awaitValue("remote navigation without local services") {
                if (uiNodes().none { it.contentDescription == text(Res.string.services) }) true else null
            }
            captureNavigation("remote-error")
            click(text(Res.string.settings), "app settings from remote")
            awaitNode("app settings contents") { it.text == text(Res.string.dark_theme) }
            for (step in 0 until 8) {
                if (uiNodes().any { it.isVisible && it.text == text(Res.string.logs) }) break
                instrumentation.runOnMainSync {
                    val page = currentNodes().first { it.semantic.config.getOrNull(SemanticsActions.ScrollBy) != null }
                    page.semantic.config.getOrNull(SemanticsActions.ScrollBy)?.action?.invoke(0f, page.boundsInWindow.height * 0.6f)
                }
            }
            val logs = awaitNode("application-local logs") { it.text == text(Res.string.logs) }
            val logsAction = generateSequence(logs) { it.parent }.first { it.isClickable }
            assertTrue(logsAction.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            awaitNode("loaded local log viewer") { it.contentDescription == text(Res.string.wrap_content) }
            click(text(Res.string.back), "return from local logs")
            click(text(Res.string.back), "return to selected device")
            awaitNode("selected device retained") { it.text == peer.name }
            click(text(Res.string.select_a_device), "device selector from remote")
            click(text(Res.string.this_device), "switch back to local")
            awaitNode("local services tab restored") { it.contentDescription == text(Res.string.services) }
        }
    }

    @Test
    fun sharedToolsPageRefreshesPreferencesAndWritesDraftOnlyToTheSelectedDevice() = runBlocking<Unit> {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val localBefore = GraphqlApi.execute(Query.savedValue("quick_note_draft", ""))
            val requests = java.util.Collections.synchronizedList(mutableListOf<com.ismartcoding.plain.rust.graphql.GraphqlOperation>())
            val features = java.util.concurrent.atomic.AtomicReference("NOTES|FILES")
            val failRefresh = java.util.concurrent.atomic.AtomicBoolean(false)
            val source = shareFixtureSource(requests, capabilities = listOf(
                com.ismartcoding.plain.platform.system.Capability.NOTES,
                com.ismartcoding.plain.platform.system.Capability.FILE_BROWSER,
            ), readFeatures = { features.get() }, failRead = { failRefresh.get() && "{ prefs }" in it.query })
            scenario.onActivity { activity ->
                activity.setContent {
                    com.ismartcoding.plain.ui.theme.AppTheme(useDarkTheme = false) {
                        val nav = androidx.navigation.compose.rememberNavController()
                        androidx.compose.runtime.CompositionLocalProvider(com.ismartcoding.plain.ui.navigation.LocalContentSource provides source) {
                            com.ismartcoding.plain.ui.tools.ToolsPage(nav, onTabSelected = {})
                        }
                    }
                }
            }
            awaitNode("selected device notes tool") { it.text == text(Res.string.notes) }
            awaitNode("selected device files tool") { it.text == text(Res.string.files) }
            assertFalse(allText().contains(text(Res.string.sound_meter)))
            assertTrue(uiNodes().none { it.contentDescription == text(Res.string.add) })
            val draft = "Selected device quick draft"
            val field = awaitNode("selected device quick note") { it.isEditable }
            assertTrue(field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, draft)
            }))
            awaitValue("selected device draft save") {
                synchronized(requests) {
                    requests.firstOrNull { "setSavedValue(" in it.query && it.variables["value"]?.jsonPrimitive?.content == draft }
                }
            }
            assertEquals(localBefore, GraphqlApi.execute(Query.savedValue("quick_note_draft", "")))
            features.set("FILES")
            pullToolsDown()
            awaitValue("refreshed selected-device tools") {
                if (uiNodes().none { it.isVisible && it.text == text(Res.string.notes) }) true else null
            }
            awaitNode("files after refresh") { it.text == text(Res.string.files) }
            awaitToolsRefreshFinished()
            features.set("NOTES|FILES")
            failRefresh.set(true)
            pullToolsDown()
            awaitValue("failed refresh reached the selected device") {
                if (synchronized(requests) { requests.count { "{ prefs }" in it.query } } == 3) true else null
            }
            awaitNode("failed tools refresh indicator") { it.text == text(com.ismartcoding.plain.ui.resources.Res.string.srl_header_failed) }
            awaitToolsRefreshFinished()
            assertTrue(uiNodes().none { it.isVisible && it.text == text(Res.string.notes) })
            awaitNode("tools preserved after failed refresh") { it.text == text(Res.string.files) }
            failRefresh.set(false)
            pullToolsDown()
            awaitNode("notes restored after refresh recovery") { it.text == text(Res.string.notes) }
            assertEquals(draft, awaitNode("draft preserved across refresh") { it.isEditable }.text)
            assertEquals(4, synchronized(requests) { requests.count { "{ prefs }" in it.query } })
            assertTrue(synchronized(requests) { requests.none { "setPref(" in it.query } })
            assertEquals(localBefore, GraphqlApi.execute(Query.savedValue("quick_note_draft", "")))
            captureNavigation("selected-tools")
        }
    }

    private suspend fun awaitToolsRefreshFinished() {
        val finished = text(com.ismartcoding.plain.ui.resources.Res.string.srl_header_finish)
        val refreshing = text(com.ismartcoding.plain.ui.resources.Res.string.srl_header_refreshing)
        val failed = text(com.ismartcoding.plain.ui.resources.Res.string.srl_header_failed)
        awaitValue("tools refresh indicator retracted") {
            if (uiNodes().none { it.isVisible && (it.text == finished || it.text == refreshing || it.text == failed) }) true else null
        }
    }

    private fun pullToolsDown() {
        val display = instrumentation.targetContext.resources.displayMetrics
        val x = display.widthPixels * 0.5f
        val start = display.heightPixels * 0.45f
        val distance = display.heightPixels * 0.3f
        val down = android.os.SystemClock.uptimeMillis()
        for (step in 0..24) {
            val action = when (step) {
                0 -> android.view.MotionEvent.ACTION_DOWN
                24 -> android.view.MotionEvent.ACTION_UP
                else -> android.view.MotionEvent.ACTION_MOVE
            }
            val event = android.view.MotionEvent.obtain(down, android.os.SystemClock.uptimeMillis(), action, x, start + distance * step / 24, 0)
            assertTrue(instrumentation.uiAutomation.injectInputEvent(event, true))
            event.recycle()
            android.os.SystemClock.sleep(16)
        }
        instrumentation.waitForIdleSync()
    }

    private fun captureNavigation(name: String) {
        val image = instrumentation.uiAutomation.takeScreenshot()
        java.io.File(instrumentation.targetContext.cacheDir, "navigation-$name.png").outputStream().use {
            image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        image.recycle()
    }

    @Test
    fun longChatPreviewUsesOneEllipsizedLine() {
        val message = "managed-chat-2bf41508-8317-426e-9d7c-b3c4fe7c8f0f ".repeat(4)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    com.ismartcoding.plain.ui.theme.AppTheme(useDarkTheme = false) {
                        Box(Modifier.width(320.dp)) {
                            com.ismartcoding.plain.ui.chat.components.PeerListItem(
                                title = "Device", desc = message,
                                icon = com.ismartcoding.plain.ui.resources.Res.drawable.smartphone,
                            )
                        }
                    }
                }
            }
            val subtitle = awaitNode("long device chat preview") { it.text == message }
            instrumentation.runOnMainSync {
                val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                assertTrue(subtitle.semantic.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts) == true)
                assertEquals(1, layouts.single().lineCount)
                assertTrue(layouts.single().isLineEllipsized(0))
            }
            report("chat_preview_result", "one_line_ellipsis_verified")
        }
    }

    @Test
    fun globalSearchKeepsLocalAndRemoteNotesSeparateAndUsesTheExistingTargetPage() = runBlocking {
        val marker = "managed-search-${UUID.randomUUID()}"
        val peerId = checkNotNull(InstrumentationRegistry.getArguments().getString("managedPeerId"))
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            awaitNode("main navigation") { it.contentDescription == text(Res.string.chat) }
            val peer = checkNotNull(GraphqlApi.execute(Query.peers()).find { it.id == peerId })
            val client = com.ismartcoding.plain.features.chat.peer.ManagedDeviceClient.load(peer)
            val remote = com.ismartcoding.plain.rust.graphql.ContentSource.Remote(client)
            val localNote = GraphqlApi.execute(Mutation.saveNote("", "$marker-local", "$marker local body"))
            val targetNote = client.api.execute(Mutation.saveNote("", "$marker-target", "$marker target body"))
            try {
                assertNull(GraphqlApi.execute(Query.note(targetNote.id)))
                val vm = installGlobalSearchFixture(scenario, remote, com.ismartcoding.plain.ui.search.GlobalSearchDomain.NOTES, marker)
                click(text(Res.string.search), "global search navigation")
                awaitNode("local search note") { it.text == localNote.title }
                awaitNode("target search note") { it.text == targetNote.title }
                val state = vm.domainStates.getValue(com.ismartcoding.plain.ui.search.GlobalSearchDomain.NOTES)
                assertEquals(2, state.total.value)
                assertEquals(2, state.hits.value.map { it.key }.toSet().size)
                assertEquals(1, state.hits.value.count { it.device is com.ismartcoding.plain.rust.graphql.ContentSource.Local })
                assertEquals(1, state.hits.value.count { it.device === remote })
                click(targetNote.title, "remote search result")
                awaitNode("existing target note detail") { it.text?.contains("$marker target body") == true }
                assertNull(GraphqlApi.execute(Query.note(targetNote.id)))
                report("global_search_notes", "both_sources_and_target_existing_detail_verified_no_native_import")
            } finally {
                GraphqlApi.execute(Mutation.trashNotes("ids:=${localNote.id}"))
                GraphqlApi.execute(Mutation.deleteNotes("ids:=${localNote.id}"))
                client.api.execute(Mutation.trashNotes("ids:=${targetNote.id}"))
                client.api.execute(Mutation.deleteNotes("ids:=${targetNote.id}"))
                assertNull(GraphqlApi.execute(Query.note(localNote.id)))
                assertNull(client.api.execute(Query.note(targetNote.id)))
            }
        }
    }

    @Test
    fun globalSearchChatGlyphAndFullTargetConversationKeepWritesOnTheTarget() = runBlocking {
        val marker = "managed-search-${UUID.randomUUID()}"
        val context = "managed-context-${UUID.randomUUID()}"
        val reply = "$marker-reply"
        val peerId = checkNotNull(InstrumentationRegistry.getArguments().getString("managedPeerId"))
        var remote: com.ismartcoding.plain.rust.graphql.ContentSource.Remote? = null
        val owned = mutableSetOf<String>()
        var originalDraft: String? = null
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                awaitNode("main navigation") { it.contentDescription == text(Res.string.chat) }
                val peer = checkNotNull(GraphqlApi.execute(Query.peers()).find { it.id == peerId })
                val source = com.ismartcoding.plain.rust.graphql.ContentSource.Remote(com.ismartcoding.plain.features.chat.peer.ManagedDeviceClient.load(peer))
                remote = source
                originalDraft = source.chats.readDraft("peer:local")
                owned.add(source.chats.sendText(ChatTarget("local", ChatTargetType.PEER), marker).id)
                owned.add(source.chats.sendText(ChatTarget("local", ChatTargetType.PEER), context).id)
                assertTrue(ChatClient.records.search(marker, 20, 0).isEmpty())
                installGlobalSearchFixture(scenario, source, com.ismartcoding.plain.ui.search.GlobalSearchDomain.CHAT, marker)
                click(text(Res.string.search), "global search navigation")
                awaitNode("target chat search result") { it.text == marker }
                assertGlobalSearchChatGlyph(scenario, marker)
                var result = searchResultNode(marker)
                while (!result.isClickable) result = checkNotNull(result.parent)
                assertTrue(result.performAction(AccessibilityNodeInfo.ACTION_CLICK))
                awaitNode("full conversation matching message") { it.text == marker }
                awaitNode("full conversation nonmatching context") { it.text == context }
                awaitNode("target device subtitle") { it.text == source.info.deviceName }
                val input = awaitNode("target conversation input") { it.isEditable && it.text != marker }
                assertTrue(input.performAction(AccessibilityNodeInfo.ACTION_CLICK))
                assertTrue(input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, reply) }))
                awaitValue("target draft persisted") { if (source.chats.readDraft("peer:local") == reply) true else null }
                click(text(Res.string.send_message), "send target conversation text")
                val saved = awaitValue("target text persisted") { source.chats.records.search(reply, 10, 0).firstOrNull { (it.messageContent.value as? MessageText)?.text == reply } }
                owned.add(saved.id)
                awaitValue("target draft cleared") { if (source.chats.readDraft("peer:local").isEmpty()) true else null }
                assertTrue(ChatClient.records.search(marker, 20, 0).isEmpty())
                report("global_search_chat", "glyph_full_conversation_and_target_only_send_verified")
            }
        } finally {
            remote?.let { source ->
                source.chats.records.search(reply, 10, 0).filter { (it.messageContent.value as? MessageText)?.text == reply }.forEach { owned.add(it.id) }
                source.chats.deleteByIds(owned)
                originalDraft?.let { source.chats.writeDraft("peer:local", it) }
                assertTrue(source.chats.records.search(marker, 20, 0).isEmpty())
                assertTrue(ChatClient.records.search(marker, 20, 0).isEmpty())
            }
        }
    }

    @Test
    fun globalSearchRemoteAudioQueueIconsFollowInitialAndExternalTargetChanges() = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            awaitNode("main navigation") { it.contentDescription == text(Res.string.chat) }
            val args = InstrumentationRegistry.getArguments()
            val peerId = checkNotNull(args.getString("managedPeerId"))
            val prefix = checkNotNull(args.getString("managedAudioPrefix"))
            require(prefix.startsWith("managed-acceptance-"))
            val peer = checkNotNull(GraphqlApi.execute(Query.peers()).find { it.id == peerId })
            val source = com.ismartcoding.plain.rust.graphql.ContentSource.Remote(com.ismartcoding.plain.features.chat.peer.ManagedDeviceClient.load(peer))
            val tracks = source.catalog.rows(com.ismartcoding.plain.platform.system.DataType.AUDIO, "text:$prefix trash:false", 10, 0, FileSortBy.NAME_ASC).filterIsInstance<com.ismartcoding.plain.models.Audio>()
            assertEquals(2, tracks.size)
            val before = source.queue.queuePage(0, 10000)
            val localQueue = com.ismartcoding.plain.features.audio.AudioQueueClient.queuePage(0, 10000)
            val localPlayback = com.ismartcoding.plain.features.audio.AudioQueueClient.playback()
            try {
                source.queue.enqueue(listOf(tracks.first().toPlaylistAudio()))
                installGlobalSearchFixture(scenario, source, com.ismartcoding.plain.ui.search.GlobalSearchDomain.AUDIO, prefix)
                click(text(Res.string.search), "global search navigation")
                awaitSearchAudioAction(tracks.first().title, text(Res.string.remove_from_queue))
                awaitSearchAudioAction(tracks.last().title, text(Res.string.add_to_queue))
                assertTrue(awaitSearchAudioAction(tracks.first().title, text(Res.string.remove_from_queue)).performAction(AccessibilityNodeInfo.ACTION_CLICK))
                awaitValue("owned target track removed") { if (tracks.first().id !in source.queue.queuedMediaIds()) true else null }
                awaitSearchAudioAction(tracks.first().title, text(Res.string.add_to_queue))
                assertTrue(awaitSearchAudioAction(tracks.last().title, text(Res.string.add_to_queue)).performAction(AccessibilityNodeInfo.ACTION_CLICK))
                awaitValue("owned target track added") { if (tracks.last().id in source.queue.queuedMediaIds()) true else null }
                awaitSearchAudioAction(tracks.last().title, text(Res.string.remove_from_queue))
                source.queue.removeQueued(tracks.last().id)
                awaitSearchAudioAction(tracks.last().title, text(Res.string.add_to_queue))
                assertEquals(localQueue, com.ismartcoding.plain.features.audio.AudioQueueClient.queuePage(0, 10000))
                assertEquals(localPlayback, com.ismartcoding.plain.features.audio.AudioQueueClient.playback())
                assertEquals(before, source.queue.queuePage(0, 10000))
                report("global_search_remote_audio", "initial_membership_ui_toggle_and_external_event_verified_native_unchanged")
            } finally {
                tracks.forEach { source.queue.removeQueued(it.id) }
                assertEquals(before, source.queue.queuePage(0, 10000))
            }
        }
    }

    @Test
    fun globalSearchLocalAudioQueueIconUsesMediaIdAndReactsToTheExistingProjection() = runBlocking {
        val file = createOwnedWave(1)
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                awaitNode("main navigation") { it.contentDescription == text(Res.string.chat) }
                val before = com.ismartcoding.plain.features.audio.AudioQueueClient.queuePage(0, 10000)
                val now = kotlin.time.Instant.parse("2026-01-01T00:00:00Z")
                val audio = com.ismartcoding.plain.models.Audio("owned-media-id", "Owned queue icon", "Fixture", file.absolutePath, 1000, file.length(), "", createdAt = now, updatedAt = now)
                lateinit var queue: com.ismartcoding.plain.ui.audio.AudioQueueViewModel
                scenario.onActivity { activity ->
                    activity.setContent {
                        com.ismartcoding.plain.ui.theme.AppTheme(useDarkTheme = false) {
                            val nav = androidx.navigation.compose.rememberNavController()
                            val model = androidx.lifecycle.viewmodel.compose.viewModel(key = "owned-search-audio") { com.ismartcoding.plain.ui.audio.AudioQueueViewModel() }
                            queue = model
                            val context = com.ismartcoding.plain.ui.search.rememberGlobalSearchRowContext(nav, model)
                            val hit = com.ismartcoding.plain.ui.search.GlobalSearchHit("owned-audio", audio.title, source = com.ismartcoding.plain.ui.search.GlobalSearchSource.Audio(audio), action = com.ismartcoding.plain.ui.search.GlobalSearchAction.PlayAudio(audio))
                            com.ismartcoding.plain.ui.search.GlobalSearchRow(hit, "Owned", context, onOpen = {})
                        }
                    }
                }
                awaitNode("owned audio row") { it.text == audio.title }
                queue.loadAsync()
                scenario.onActivity { queue.queuedMediaIds.value = setOf(audio.id) }
                awaitSearchAudioAction(audio.title, text(Res.string.remove_from_queue))
                scenario.onActivity { queue.queuedMediaIds.value = emptySet() }
                awaitSearchAudioAction(audio.title, text(Res.string.add_to_queue))
                assertEquals(before, com.ismartcoding.plain.features.audio.AudioQueueClient.queuePage(0, 10000))
                report("global_search_local_audio", "media_id_projection_binding_verified_without_queue_writes")
            }
        } finally { check(file.delete()) }
    }

    @Test
    fun freshNativeAudioUsesTheLocalPathAndReportsPlaybackThroughRust() = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use {
            awaitNode("main navigation") { it.contentDescription == text(Res.string.chat) }
            val queue = com.ismartcoding.plain.features.audio.AudioQueueClient
            assertTrue("Fresh-data fixture requires an empty native queue", queue.queuePage(0, 10).isEmpty())
            assertTrue("Fresh-data fixture requires no native selection", queue.playback().path.isNullOrEmpty())
            val file = createOwnedWave(20)
            try {
                com.ismartcoding.plain.features.audio.AudioCommands.play(com.ismartcoding.plain.models.AudioItem(title = "Owned native audio", artist = "", path = file.absolutePath, durationMs = 20000), enqueue = true)
                awaitValue("native SDK actually playing") {
                    var ready = false
                    instrumentation.runOnMainSync { ready = com.ismartcoding.plain.platform.media.AndroidAudioPlayer.isPlaying() }
                    if (ready) true else null
                }
                var actualPath = ""
                instrumentation.runOnMainSync { actualPath = com.ismartcoding.plain.platform.media.createAudioPlayer().currentPath }
                assertEquals(file.canonicalFile, java.io.File(actualPath.removePrefix("file://")).canonicalFile)
                assertTrue(com.ismartcoding.plain.features.audio.AudioCommands.output.value.source is com.ismartcoding.plain.rust.graphql.ContentSource.Local)
                assertTrue(queue.queuePage(0, 10).isEmpty())
                com.ismartcoding.plain.features.audio.AudioCommands.command(com.ismartcoding.plain.models.AudioCommand.SEEK, 5000)
                awaitValue("native seek persisted by Rust") { queue.playback().takeIf { it.positionMs >= 5000 } }
                com.ismartcoding.plain.features.audio.AudioCommands.command(com.ismartcoding.plain.models.AudioCommand.PAUSE)
                awaitValue("native pause applied") {
                    var playing = true
                    instrumentation.runOnMainSync { playing = com.ismartcoding.plain.platform.media.AndroidAudioPlayer.isPlaying() }
                    if (!playing) true else null
                }
                report("native_audio", "direct_path_actual_sdk_seek_pause_and_native_rust_state_verified")
            } finally {
                com.ismartcoding.plain.features.audio.AudioCommands.command(com.ismartcoding.plain.models.AudioCommand.CLEAR)
                awaitValue("owned native selection cleared") { if (queue.playback().path.isNullOrEmpty()) true else null }
                assertTrue(queue.queuePage(0, 10).isEmpty())
                check(file.delete())
            }
        }
    }

    private fun createOwnedWave(seconds: Int): java.io.File {
        val file = java.io.File(instrumentation.targetContext.cacheDir, "managed-audio-${UUID.randomUUID()}.wav")
        val samples = 8000 * seconds * 2
        val header = java.nio.ByteBuffer.allocate(44).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            .put("RIFF".toByteArray()).putInt(samples + 36).put("WAVEfmt ".toByteArray()).putInt(16)
            .putShort(1).putShort(1).putInt(8000).putInt(16000).putShort(2).putShort(16)
            .put("data".toByteArray()).putInt(samples).array()
        file.outputStream().use { it.write(header); it.write(ByteArray(samples)) }
        return file
    }

    @Test
    fun opensLocalMediaPageWithoutEnteringManagedDeviceScope() {
        val feature = checkNotNull(InstrumentationRegistry.getArguments().getString("localMediaFeature"))
        val titleResource = when (feature) {
            "IMAGES" -> Res.string.images
            "VIDEOS" -> Res.string.videos
            "AUDIO" -> Res.string.audios
            else -> error("Unsupported media fixture")
        }
        val intent = android.content.Intent(instrumentation.targetContext, MainActivity::class.java)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        ActivityScenario.launch<MainActivity>(intent).use {
            awaitNode("localized main navigation") { it.contentDescription == text(Res.string.tools) }
            click(text(Res.string.tools), "local tools navigation")
            val title = text(titleResource)
            click(title, "local media entry")
            awaitNode("local media content or permission action") {
                it.contentDescription == text(Res.string.search) || it.text == text(Res.string.grant_access)
            }
            assertTrue(allText().any { it == title })
            awaitNode("local media title after navigation") { it.text == title }
            val transitionDeadline = System.nanoTime() + 30_000_000_000L
            while (uiNodes().any { it.isVisible && it.text == text(Res.string.tools) } && System.nanoTime() < transitionDeadline) Thread.sleep(100)
            assertFalse(uiNodes().any { it.isVisible && it.text == text(Res.string.tools) })
            if (allText().any { it == text(Res.string.grant_access) }) {
                assertFalse(uiNodes().any { it.isVisible && it.contentDescription == text(Res.string.folders) })
                assertFalse(uiNodes().any { it.isVisible && it.contentDescription == text(Res.string.search) })
            }
            report("local_media_result", feature + "_opened_without_managed_scope")
        }
    }

    @Test
    fun pairingModeDialogUsesPlainAccessChoices() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        val dark = args.getString("managedDarkTheme")?.toBooleanStrict() ?: false
        val dismissed = java.util.concurrent.atomic.AtomicBoolean()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    com.ismartcoding.plain.ui.theme.AppTheme(useDarkTheme = dark) {
                        val show = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }
                        Box(Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background)) {
                            if (show.value) com.ismartcoding.plain.ui.nearby.PairPeerDialog(
                                com.ismartcoding.plain.models.NearbyDevice(
                                    id = "pair-dialog-ui-fixture", name = "Nearby device", port = 8443,
                                    deviceType = com.ismartcoding.plain.platform.system.DeviceType.PHONE,
                                    version = "fixture", platform = "fixture", lastSeen = "",
                                    status = com.ismartcoding.plain.features.nearby.PeerStatus.UNPAIRED,
                                ),
                            ) { show.value = false; dismissed.set(true) }
                        }
                    }
                }
            }
            val manage = text(Res.string.pair_manage_device)
            val chat = text(Res.string.pair_chat_only)
            awaitNode("default manage option") { it.isCheckable && it.isChecked && nodes(it).any { child -> child.text?.contains(manage) == true } }
            click(chat, "select chat access")
            awaitNode("selected chat option") { it.isCheckable && it.isChecked && nodes(it).any { child -> child.text?.contains(chat) == true } }
            click(manage, "select management access")
            awaitNode("selected manage option") { it.isCheckable && it.isChecked && nodes(it).any { child -> child.text?.contains(manage) == true } }
            report("pairing_dialog_ready", if (dark) "dark" else "light")
            delay(args.getString("managedHoldMillis")?.toLong() ?: 0)
            click(text(Res.string.cancel), "dismiss pairing mode dialog")
            awaitValue("pairing dialog dismissed") { dismissed.get().takeIf { it } }
            Unit
        }
    }

    @Test
    fun configuresOwnedPairingAcceptanceSettings() = runBlocking {
        check(instrumentation.targetContext.packageName == "com.ismartcoding.plain.managedtest")
        ActivityScenario.launch(MainActivity::class.java).use {
            val args = InstrumentationRegistry.getArguments()
            val identity = GraphqlApi.execute(identityRequest)
            check(identity.clientId == checkNotNull(args.getString("managedInstanceId")))
            report("device_name_before", JsonHelper.jsonEncode(identity.deviceName))
            report("approval_before", Prefs.loginRequireApproval.value.toString())
            val name = checkNotNull(args.getString("managedDeviceName"))
            Prefs.set(com.ismartcoding.plain.preferences.PrefKeys.DEVICE_NAME, name)
            args.getString("managedApprovalEnabled")?.let { Prefs.setLoginRequireApproval(it.toBooleanStrict()) }
            awaitValue("owned device name updated") { GraphqlApi.execute(identityRequest).takeIf { it.deviceName == name } }
            Unit
        }
    }

    @Test
    fun describeExistingDeviceAccess() = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use {
            val local = GraphqlApi.execute(identityRequest)
            val peers = GraphqlApi.execute(Query.peers())
            instrumentation.sendStatus(2, Bundle().apply {
                putString("managed_descriptor", JsonHelper.jsonEncode(ManagedAcceptanceDescriptor(local.clientId, peers.map { ManagedAcceptancePeer(it.id, it.isPaired() && it.token.isNotEmpty()) })))
            })
        }
    }

    @Test
    fun configuresAnAuthorizedTestApiPermission() = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use {
            val args = InstrumentationRegistry.getArguments()
            val permission = Permission.valueOf(checkNotNull(args.getString("managedPermission")))
            val enabled = checkNotNull(args.getString("managedPermissionEnabled")).toBooleanStrict()
            report("permission_before", (permission.name in Prefs.apiPermissions.value).toString())
            Prefs.setApiPermission(permission, enabled)
            awaitValue("updated API permission state") { if ((permission.name in Prefs.apiPermissions.value) == enabled) true else null }
            assertEquals(enabled, permission.name in Prefs.apiPermissions.value)
        }
    }

    @Test
    fun answersRemotePermissionRequestWithoutOpeningAccessSettings() = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use {
            val args = InstrumentationRegistry.getArguments()
            val permission = Permission.valueOf(checkNotNull(args.getString("managedPermission")))
            val feature = com.ismartcoding.plain.features.web.WebSettingsFeature.valueOf(checkNotNull(args.getString("managedPermissionFeature")))
            val decision = checkNotNull(args.getString("managedAccessDecision"))
            val previous = permission.name in Prefs.apiPermissions.value
            try {
                Prefs.setApiPermission(permission, false)
                awaitValue("disabled test permission") { if (permission.name !in Prefs.apiPermissions.value) true else null }
                report("permission_request_ready", feature.name)
                awaitNode("direct access confirmation") { it.text == text(Res.string.web_access_request) }
                assertEquals(feature, com.ismartcoding.plain.ui.app.MainViewModel.pendingWebAccessRequest.value?.feature)
                assertFalse(permission.name in Prefs.apiPermissions.value)
                when (decision) {
                    "APPROVE" -> click(text(Res.string.grant_access), "approve remote permission")
                    "REJECT" -> click(text(Res.string.reject), "reject remote permission")
                    "DISMISS" -> instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
                    else -> error("Unsupported permission fixture decision")
                }
                awaitValue("closed access request") {
                    if (com.ismartcoding.plain.ui.app.MainViewModel.pendingWebAccessRequest.value == null) true else null
                }
                if (decision == "APPROVE") {
                    awaitValue("confirmed API permission") { if (permission.name in Prefs.apiPermissions.value) true else null }
                } else assertFalse(permission.name in Prefs.apiPermissions.value)
                report("permission_request_result", decision + "_verified")
                delay(args.getString("managedHoldMillis")?.toLong() ?: 3000L)
            } finally {
                Prefs.setApiPermission(permission, previous)
                awaitValue("restored original API permission") { if ((permission.name in Prefs.apiPermissions.value) == previous) true else null }
                report("permission_request_restored", previous.toString())
            }
        }
    }

    @Test
    fun sendsTheExistingRemotePermissionRequest() = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use {
            val args = InstrumentationRegistry.getArguments()
            val peerId = checkNotNull(args.getString("managedPeerId"))
            val permission = Permission.valueOf(checkNotNull(args.getString("managedPermission")))
            val feature = com.ismartcoding.plain.features.web.WebSettingsFeature.valueOf(checkNotNull(args.getString("managedPermissionFeature")))
            val decision = checkNotNull(args.getString("managedAccessDecision"))
            val peer = checkNotNull(GraphqlApi.execute(Query.peers()).find { it.id == peerId })
            val remote = GraphqlApi.forPeer(peer)
            awaitValue("target permission fixture ready") {
                if (permission !in remote.execute(Query.appPermissions())) true else null
            }
            assertTrue(remote.execute(Mutation.openWebSettings(feature)))
            if (decision == "APPROVE") {
                awaitValue("effective remote permission") { if (permission in remote.execute(Query.appPermissions())) true else null }
            } else assertFalse(permission in remote.execute(Query.appPermissions()))
            report("permission_request_sent", feature.name + "_" + decision)
        }
    }

    @Test
    fun opensLocalAppDetailsAndManifestThroughTheSharedRoutes() = runBlocking {
        val intent = android.content.Intent(instrumentation.targetContext, MainActivity::class.java)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        ActivityScenario.launch<MainActivity>(intent).use {
            awaitNode("local tools navigation") { it.contentDescription == text(Res.string.tools) }
            val previous = Prefs.appHomeFeatures.value
            val configured = Prefs.formatFeatures(listOf(AppFeatureType.APPS.name) + Prefs.parseFeatures(previous).filterNot { it == AppFeatureType.APPS.name })
            try {
                Prefs.set(com.ismartcoding.plain.preferences.PrefKeys.APP_HOME_FEATURES, configured)
                awaitValue("configured local Apps entry") { if (Prefs.appHomeFeatures.value == configured) true else null }
                click(text(Res.string.tools), "local tools")
                click(text(Res.string.apps), "local apps")
                val id = InstrumentationRegistry.getArguments().getString("managedLocalPackageId") ?: instrumentation.targetContext.packageName
                val item = checkNotNull(com.ismartcoding.plain.features.packages.PackagesClient.find(id))
                val manifest = com.ismartcoding.plain.features.packages.PackagesClient.manifest(id)
                assertTrue("Native manifest query must return XML", manifest.contains("<manifest") && manifest.contains(id))
                report("local_manifest_query", "xml_bytes_" + manifest.length)
                click(text(Res.string.search), "search local applications")
                val input = awaitNode("local application search input") { it.isEditable }
                assertTrue(input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, id)
                }))
                val search = awaitNode("local application query") { it.isEditable && it.text == id }
                instrumentation.runOnMainSync {
                    assertTrue(search.semantic.config.getOrNull(SemanticsActions.OnImeAction)?.action?.invoke() == true)
                }
                val results = com.ismartcoding.plain.features.packages.PackagesClient.search(
                    com.ismartcoding.plain.helpers.SearchHelper.buildTextFilter(id), 50, 0, FileSortBy.NAME_ASC,
                )
                val index = results.indexOfFirst { it.id == id }
                assertTrue("Selected local application must be in search results", index >= 0)
                val list = awaitNode("local application list") { it.semantic.config.getOrNull(SemanticsActions.ScrollToIndex) != null }
                instrumentation.runOnMainSync {
                    assertTrue(list.semantic.config.getOrNull(SemanticsActions.ScrollToIndex)?.action?.invoke(index + 1) == true)
                }
                val row = awaitNode("local application result") { it.text == id && !it.isEditable && it.isVisible }
                performClick(row, "local application details")
                awaitNode("local source directory") { it.text == text(Res.string.source_directory) }
                awaitNode("native local data directory") { it.text == text(Res.string.data_directory) }
                assertTrue(allText().contains(item.path))
                assertTrue(allText().contains(id))
                click("Manifest", "local package manifest")
                awaitNode("local manifest content") { it.text?.contains("<manifest") == true && it.text.contains(id) }
                report("local_package_details", "shared_routes_preserve_native_details_and_manifest")
            } finally {
                Prefs.set(com.ismartcoding.plain.preferences.PrefKeys.APP_HOME_FEATURES, previous)
                awaitValue("restored local Tools customization") { if (Prefs.appHomeFeatures.value == previous) true else null }
                assertEquals(previous, Prefs.appHomeFeatures.value)
            }
        }
    }

    @Test
    fun opensRemoteOnlyAppDetailsAndManifest() = runBlocking {
        val intent = android.content.Intent(instrumentation.targetContext, MainActivity::class.java)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        ActivityScenario.launch<MainActivity>(intent).use {
            awaitNode("localized chat navigation") { it.contentDescription == text(Res.string.chat) }
            val peerId = checkNotNull(InstrumentationRegistry.getArguments().getString("managedPeerId"))
            val peer = checkNotNull(GraphqlApi.execute(Query.peers()).find { it.id == peerId })
            val remote = GraphqlApi.forPeer(peer)
            val localIds = GraphqlApi.execute(Query.packages("", 0, 1000, FileSortBy.NAME_ASC)).map { it.id }.toSet()
            val packages = remote.execute(Query.packages("", 0, 50, FileSortBy.NAME_ASC))
            val target = checkNotNull(packages.take(5).find { it.id !in localIds }) { "A visible target-only package is required" }
            openRemoteTools(peer.name)
            click(text(Res.string.apps), "remote apps")
            click(target.id, "target-only package row")
            awaitNode("existing application details") { it.text == text(Res.string.source_directory) }
            assertTrue(allText().contains(target.path))
            assertTrue(allText().contains(target.id))
            assertFalse(allText().contains(text(Res.string.launch)))
            assertFalse(allText().contains(text(Res.string.data_directory)))
            assertFalse(allText().contains(text(Res.string.settings)))
            click("Manifest", "target package manifest")
            awaitNode("target manifest content") { it.text?.contains("<manifest") == true && it.text.contains(target.id) }
            assertTrue(com.ismartcoding.plain.features.packages.PackagesClient.find(target.id) == null)
            report("remote_package_details", "target_only_package_and_manifest_verified")
        }
    }

    @Test
    fun requestsPermissionFromTheRemoteContentPage() = runBlocking {
        val intent = android.content.Intent(instrumentation.targetContext, MainActivity::class.java)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        ActivityScenario.launch<MainActivity>(intent).use {
            awaitNode("localized chat navigation") { it.contentDescription == text(Res.string.chat) }
            val args = InstrumentationRegistry.getArguments()
            val peerId = checkNotNull(args.getString("managedPeerId"))
            val permission = Permission.valueOf(checkNotNull(args.getString("managedPermission")))
            val decision = checkNotNull(args.getString("managedAccessDecision"))
            val title = text(when (args.getString("managedContentFeature") ?: "FILES") {
                "FILES" -> Res.string.files
                "IMAGES" -> Res.string.images
                "VIDEOS" -> Res.string.videos
                "AUDIO" -> Res.string.audios
                "DOCS" -> Res.string.docs
                "APPS" -> Res.string.apps
                else -> error("Unsupported permission content fixture")
            })
            val peer = checkNotNull(GraphqlApi.execute(Query.peers()).find { it.id == peerId })
            val remote = GraphqlApi.forPeer(peer)
            awaitValue("target permission disabled") { if (permission !in remote.execute(Query.appPermissions())) true else null }
            openRemoteTools(peer.name)
            click(title, "remote content requiring access")
            awaitNode("remote request permission button") { it.text == text(Res.string.request_permission) }
            assertFalse("Search must not query content before permission", uiNodes().any { it.isVisible && it.contentDescription == text(Res.string.search) })
            assertFalse("Folders must not query content before permission", uiNodes().any { it.isVisible && it.contentDescription == text(Res.string.folders) })
            assertFalse(allText().any { it.contains("no_permission") })
            click(text(Res.string.request_permission), "request target permission")
            if (decision == "APPROVE") {
                awaitValue("granted target permission") { if (permission in remote.execute(Query.appPermissions())) true else null }
                awaitValue("permission action replaced by content") {
                    if (uiNodes().none { it.isVisible && it.text == text(Res.string.request_permission) }) true else null
                }
            } else {
                assertFalse(permission in remote.execute(Query.appPermissions()))
                awaitNode("permission request remains available") { it.text == text(Res.string.request_permission) }
            }
            assertFalse(allText().any { it.contains("no_permission") })
            report("remote_permission_page", title + "_" + decision)
        }
    }

    @Test
    fun deniedRemoteFilesNeverDisplayTheLocalFiles() = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use {
            val id = checkNotNull(InstrumentationRegistry.getArguments().getString("managedPeerId"))
            val peer = checkNotNull(GraphqlApi.execute(Query.peers()).find { it.id == id })
            val pathsRequest = GraphqlRequest<ManagedAcceptancePaths>("query { app { appDir deviceName } }", field = "app")
            val remote = GraphqlApi.forPeer(peer)
            val target = remote.execute(pathsRequest)
            val local = GraphqlApi.execute(pathsRequest)
            GraphqlApi.execute(Query.files(local.appDir, "", FileSortBy.NAME_ASC, 0, 1))
            try {
                remote.execute(Query.files(target.appDir, "", FileSortBy.NAME_ASC, 0, 1))
                fail("The target must reject disabled file access")
            } catch (expected: IllegalStateException) { assertEquals("no_permission", expected.message) }
            openRemoteTools(peer.name)
            click(text(Res.string.files), "remote files")
            awaitNode("target permission request action") { it.text == text(Res.string.request_permission) }
            assertFalse(allText().any { it.contains("no_permission") })
            report("remote_file_denial", "verified_local_native_still_available")
        }
    }

    @Test
    fun sendsAndCleansAnOwnedChatMessage() = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use {
            val args = InstrumentationRegistry.getArguments()
            val target = checkNotNull(args.getString("managedPeerId"))
            val marker = checkNotNull(args.getString("managedChatMarker"))
            check(marker.startsWith("managed-chat-"))
            val peer = checkNotNull(GraphqlApi.execute(Query.peers()).find { it.id == target })
            assertTrue(peer.isPaired())
            val item = ChatClient.sendText(ChatTarget(target, ChatTargetType.PEER), marker)
            try {
                assertEquals(ChatStatus.SENT, ChatClient.deliver(item.id).status)
                report("chat_delivery", "SENT")
            } finally {
                val stored = ChatQueries.getChatItem(item.id)
                if ((stored?.messageContent?.value as? MessageText)?.text == marker) ChatClient.deleteOne(item.id)
                assertNull(ChatQueries.getChatItem(item.id))
            }
        }
    }

    @Test
    fun receivesAndCleansAnOwnedChatMessage() = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use {
            val args = InstrumentationRegistry.getArguments()
            val sender = checkNotNull(args.getString("managedPeerId"))
            val marker = checkNotNull(args.getString("managedChatMarker"))
            check(marker.startsWith("managed-chat-"))
            val item = awaitValue("delivered chat fixture") {
                ChatQueries.searchAsync(marker, 100, 0).find {
                    it.fromId == sender && (it.messageContent.value as? MessageText)?.text == marker
                }
            }
            ChatClient.deleteOne(item.id)
            assertNull(ChatQueries.getChatItem(item.id))
            report("chat_received", "verified_and_cleaned")
            delay(5_000)
        }
    }

    @Test
    fun cleansAnOwnedNoteFixture() = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use {
            val args = InstrumentationRegistry.getArguments()
            val id = checkNotNull(args.getString("managedFixtureId"))
            val title = checkNotNull(args.getString("managedFixtureTitle"))
            check(title.startsWith("managed-ui-"))
            val note = GraphqlApi.execute(Query.note(id))
            if (note != null) {
                assertEquals(title, note.title)
                GraphqlApi.execute(Mutation.trashNotes("ids:=$id"))
                GraphqlApi.execute(Mutation.deleteNotes("ids:=$id"))
            }
            assertNull(GraphqlApi.execute(Query.note(id)))
        }
    }

    @Test
    fun remoteCastCommandsAndStateReadStayOnTheTarget() = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use {
            val targetId = checkNotNull(InstrumentationRegistry.getArguments().getString("managedPeerId"))
            val peer = checkNotNull(GraphqlApi.execute(Query.peers()).find { it.id == targetId })
            val remote = GraphqlApi.forPeer(peer)
            val selection = "version items {path} currentUri playing"
            val state = GraphqlRequest<ManagedAcceptanceCastState>("query { dlnaSenderState { $selection } }", field = "dlnaSenderState")
            val localBefore = GraphqlApi.execute(state)
            val targetBefore = remote.execute(state)
            val marker = "/managed-cast-${UUID.randomUUID()}.mp3"
            val add = GraphqlRequest<ManagedAcceptanceCastState>(
                "mutation(\$path:String!){addDlnaItem(item:{path:\$path,title:\$path}){$selection}}",
                mapOf("path" to JsonHelper.jsonEncodeToElement(marker)), field = "addDlnaItem",
            )
            val remove = GraphqlRequest<ManagedAcceptanceCastState>(
                "mutation(\$path:String!){removeDlnaItem(path:\$path){$selection}}",
                mapOf("path" to JsonHelper.jsonEncodeToElement(marker)), field = "removeDlnaItem",
            )
            val updates = kotlinx.coroutines.channels.Channel<ManagedAcceptanceCastState>(8)
            val subscription = launch(kotlinx.coroutines.Dispatchers.IO) {
                com.ismartcoding.plain.rust.graphql.RustGraphqlSubscriptions.subscribePeerUpdates(
                    peer, listOf(com.ismartcoding.plain.rust.graphql.GraphqlOperation("subscription { dlnaSenderChanged { $selection } }")),
                ).collect { update ->
                    when (update) {
                        com.ismartcoding.plain.rust.graphql.SubscriptionUpdate.Attached -> Unit
                        is com.ismartcoding.plain.rust.graphql.SubscriptionUpdate.Failed -> throw update.error
                        is com.ismartcoding.plain.rust.graphql.SubscriptionUpdate.Data -> {
                            val response = update.value
                            check(response.errors.isEmpty()) { response.errors.joinToString { it.message } }
                            updates.send(JsonHelper.jsonDecodeFromElement(checkNotNull(checkNotNull(response.data)["dlnaSenderChanged"])))
                        }
                    }
                }
            }
            suspend fun nextState(): ManagedAcceptanceCastState = kotlinx.coroutines.withTimeout(15_000) { updates.receive() }
            try {
                assertEquals(targetBefore, nextState())
                val added = remote.execute(add)
                assertEquals(added, nextState())
                assertTrue(added.version > targetBefore.version)
                assertEquals(targetBefore.items + ManagedAcceptanceCastItem(marker), added.items)
                assertEquals(added, remote.execute(state))
                assertEquals(localBefore, GraphqlApi.execute(state))
            } finally {
                if (remote.execute(state).items.any { item -> item.path == marker }) remote.execute(remove)
                assertEquals(targetBefore.items, remote.execute(state).items)
                subscription.cancel()
                subscription.join()
                updates.close()
            }
            assertEquals(localBefore, GraphqlApi.execute(state))
            report("remote_cast_state", "encrypted_subscription_http_commands_and_cancellation_verified_controller_unchanged")
        }
    }

    @Test
    fun remoteCastDialogAndViewModelKeepCommandsAndLiveStateOnTheTarget() = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            awaitNode("chat navigation") { it.contentDescription == text(Res.string.chat) }
            val targetId = checkNotNull(InstrumentationRegistry.getArguments().getString("managedPeerId"))
            val peer = checkNotNull(GraphqlApi.execute(Query.peers()).find { it.id == targetId })
            val client = com.ismartcoding.plain.features.chat.peer.ManagedDeviceClient.load(peer)
            val source = com.ismartcoding.plain.rust.graphql.ContentSource.Remote(client)
            val selection = "version items {path} currentUri playing"
            val state = GraphqlRequest<ManagedAcceptanceCastState>("query { dlnaSenderState { $selection } }", field = "dlnaSenderState")
            val localBefore = GraphqlApi.execute(state)
            val targetBefore = client.api.execute(state)
            val marker = "/managed-cast-vm-${UUID.randomUUID()}.mp3"
            var model: com.ismartcoding.plain.ui.casting.CastViewModel? = null
            val remove = GraphqlRequest<ManagedAcceptanceCastState>(
                "mutation(\$path:String!){removeDlnaItem(path:\$path){$selection}}",
                mapOf("path" to JsonHelper.jsonEncodeToElement(marker)), field = "removeDlnaItem",
            )
            try {
                scenario.onActivity { activity ->
                    activity.setContent {
                        val vm = androidx.lifecycle.viewmodel.compose.viewModel<com.ismartcoding.plain.ui.casting.CastViewModel>(key = "owned-remote-cast") {
                            com.ismartcoding.plain.ui.casting.CastViewModel(source)
                        }
                        model = vm
                        androidx.compose.runtime.LaunchedEffect(Unit) { vm.showCastDialog.value = true }
                        com.ismartcoding.plain.ui.casting.CastDialog(vm)
                    }
                }
                awaitNode("production target casting dialog") { it.text == text(Res.string.cast_select_screen) }
                awaitValue("target discovery command") { client.api.execute(state).takeIf { it.version > targetBefore.version } }
                client.api.execute(GraphqlRequest<ManagedAcceptanceCastState>(
                    "mutation(\$path:String!){addDlnaItem(item:{path:\$path,title:\$path}){$selection}}",
                    mapOf("path" to JsonHelper.jsonEncodeToElement(marker)), field = "addDlnaItem",
                ))
                awaitValue("production ViewModel receives target subscription") { model?.items?.value?.find { it.path == marker } }
                scenario.onActivity { checkNotNull(model).removeCastItem(com.ismartcoding.plain.features.dlna.sender.CastItem(marker, marker)) }
                val removed = awaitValue("ViewModel command removes only target item") { client.api.execute(state).takeIf { value -> value.items.none { it.path == marker } } }
                scenario.onActivity { checkNotNull(model).showCastDialog.value = false }
                awaitValue("target discovery stops when dialog closes") { client.api.execute(state).takeIf { it.version > removed.version } }
                assertEquals(localBefore, GraphqlApi.execute(state))
                assertEquals(targetBefore.items, client.api.execute(state).items)
                report("remote_cast_viewmodel", "production_dialog_target_scan_subscription_queue_command_and_cleanup_local_unchanged")
            } finally {
                scenario.onActivity { model?.showCastDialog?.value = false }
                if (client.api.execute(state).items.any { it.path == marker }) client.api.execute(remove)
            }
        }
    }

    @Test
    fun freshManagedLoginStoresTheControllerOsMetadata() = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use {
            val controller = checkNotNull(InstrumentationRegistry.getArguments().getString("managedPeerId"))
            val request = GraphqlRequest<List<ManagedAcceptanceSession>>(
                "query { sessions { clientId osName osVersion browserName browserVersion } }", field = "sessions",
            )
            val session = awaitValue("management login session") {
                GraphqlApi.execute(request).find { it.clientId == controller }
            }
            assertEquals("Android", session.osName)
            assertTrue(session.osVersion.isNotBlank())
            assertEquals("PlainApp", session.browserName)
            assertTrue(session.browserVersion.isNotBlank())
            report("management_session_os", "controller_os_and_app_metadata_present")
        }
    }

    @Test
    fun unpairsTheExpectedPeerWithoutDeletingMessages() = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use {
            val id = checkNotNull(InstrumentationRegistry.getArguments().getString("managedPeerId"))
            val before = com.ismartcoding.plain.features.chat.RustChatStore.getByPeerId(id).map { it.id to it.content }
            GraphqlApi.execute(Mutation.unpairPeer(id))
            val peer = checkNotNull(GraphqlApi.execute(Query.peers()).find { it.id == id })
            assertFalse(peer.isPaired())
            assertTrue(peer.token.isEmpty())
            assertEquals(before, com.ismartcoding.plain.features.chat.RustChatStore.getByPeerId(id).map { it.id to it.content })
        }
    }

    @Test
    fun pairsFromNearbyWithTheSelectedAccess() = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use {
            val args = InstrumentationRegistry.getArguments()
            val targetId = checkNotNull(args.getString("managedPeerId"))
            val mode = PairingMode.valueOf(checkNotNull(args.getString("managedPairMode")))
            val expectedName = args.getString("managedExpectedDeviceName")
            val previous = RustPairingRuntime.states().map { it.generation }.toSet()
            click(text(Res.string.chat), "chat navigation")
            click(text(Res.string.add), "add menu")
            click(text(Res.string.pair_device), "nearby navigation")
            val nearby = awaitValue("discovered target") { GraphqlApi.execute(Query.nearbyDevices()).find { it.id == targetId && (expectedName == null || it.name == expectedName) } }
            val pair = awaitValue("target pair action") {
                var candidate: UiNode? = null
                instrumentation.runOnMainSync {
                    val current = currentNodes()
                    val row = current.firstOrNull {
                        it.isVisible && it.semantic.config.getOrNull(SemanticsProperties.TestTag) == "nearby-device:$targetId"
                    }
                    candidate = row?.let { nodes(it).firstOrNull { child -> child.isVisible && child.text == text(Res.string.pair) } }
                }
                candidate
            }
            var pairAction = pair
            while (pairAction.semantic.config.getOrNull(SemanticsProperties.Role) != androidx.compose.ui.semantics.Role.Button && pairAction.parent != null) pairAction = checkNotNull(pairAction.parent)
            assertEquals(androidx.compose.ui.semantics.Role.Button, pairAction.semantic.config.getOrNull(SemanticsProperties.Role))
            assertTrue(pairAction.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            awaitNode("pairing access title") { it.text == text(Res.string.pair_device) }
            awaitNode("pair dialog belongs to the expected device") { it.text == nearby.name }
            val label = text(if (mode == PairingMode.CHAT) Res.string.pair_chat_only else Res.string.pair_manage_device)
            var choices = awaitNode("access choice") { it.text?.toString()?.contains(label) == true }
            while (!choices.isCheckable && choices.parent != null) choices = checkNotNull(choices.parent)
            assertTrue(choices.isCheckable)
            val radioId = choices.semantic.id
            performClick(choices, "access radio")
            awaitNode("selected access radio") { it.semantic.id == radioId && it.isChecked }
            click(text(Res.string.pair), "confirm pairing")
            awaitNode("waiting for the other device to confirm pairing") {
                it.overlay && it.text == text(Res.string.waiting_for_confirmation)
            }
            assertFalse(uiNodes().any { it.overlay && it.text == text(Res.string.pair_device) })
            var lastPhase: PairingPhase? = null
            val terminal = awaitValue("pairing completion") {
                val state = RustPairingRuntime.states().lastOrNull { it.deviceId == targetId && it.generation !in previous }
                if (state?.phase != lastPhase) {
                    lastPhase = state?.phase
                    report("pairing_phase", state?.phase?.name.orEmpty())
                    state?.verificationCode?.let { report("pairing_code", it) }
                }
                state?.takeIf { it.phase in setOf(PairingPhase.SUCCEEDED, PairingPhase.FAILED, PairingPhase.CANCELLED, PairingPhase.PASSWORD_REQUIRED) }
            }
            assertEquals("Pairing error: ${terminal.error}", PairingPhase.SUCCEEDED, terminal.phase)
            val peer = awaitValue("paired record") { GraphqlApi.execute(Query.peers()).find { it.id == targetId && it.isPaired() } }
            assertEquals(mode == PairingMode.MANAGE, peer.token.isNotEmpty())
            if (mode == PairingMode.MANAGE) assertEquals(targetId, GraphqlApi.forPeer(peer).execute(identityRequest).clientId)
            click(text(Res.string.back), "back to chat list")
            click(text(Res.string.chat), "chat navigation")
            click(peer.name, "paired chat")
            assertFalse(allText().any { it == text(Res.string.remote_management) })
            report("pairing_result", mode.name + "_verified")
        }
    }

    @Test
    fun checksExistingPairingAccessFromChat() = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use {
            val args = InstrumentationRegistry.getArguments()
            val targetId = checkNotNull(args.getString("managedPeerId"))
            val mode = PairingMode.valueOf(checkNotNull(args.getString("managedPairMode")))
            val peer = checkNotNull(GraphqlApi.execute(Query.peers()).find { it.id == targetId })
            assertTrue(peer.isPaired())
            assertEquals(mode == PairingMode.MANAGE, peer.token.isNotEmpty())
            click(text(Res.string.chat), "chat navigation")
            awaitNode("paired device") { it.text == peer.name }
            assertFalse(allText().any { it == text(Res.string.pair_chat_only) })
            click(peer.name, "paired chat")
            if (mode == PairingMode.MANAGE) {
                assertFalse(allText().any { it == text(Res.string.remote_management) })
                assertEquals(targetId, GraphqlApi.forPeer(peer).execute(identityRequest).clientId)
            } else {
                assertFalse(allText().any { it == text(Res.string.remote_management) })
                try {
                    GraphqlApi.forPeer(peer).execute(identityRequest)
                    fail("Chat-only peer must not execute management requests")
                } catch (expected: IllegalStateException) {
                    assertEquals("no_permission", expected.message)
                }
            }
            report("existing_access_result", mode.name + "_verified")
        }
    }

    @Test
    fun approvesExpectedIncomingPairingOnTheRenderedPage() = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use {
            val args = InstrumentationRegistry.getArguments()
            val senderId = checkNotNull(args.getString("managedPeerId"))
            val mode = PairingMode.valueOf(checkNotNull(args.getString("managedPairMode")))
            awaitValue("expected incoming request") {
                if (mode == PairingMode.CHAT) MainViewModel.pendingPairingRequest.value?.takeIf { it.fromId == senderId }
                else MainViewModel.pendingLoginRequest.value?.takeIf { it.clientId == senderId }
            }
            if (mode == PairingMode.MANAGE) report("pairing_code", checkNotNull(MainViewModel.pendingLoginRequest.value).request.verificationCode)
            if (mode == PairingMode.CHAT) {
                awaitNode("incoming chat pairing page") { it.text == text(Res.string.pairing_request) }
                instrumentation.runOnMainSync {
                    currentNodes().firstOrNull { it.semantic.config.getOrNull(SemanticsActions.ScrollBy) != null }?.let { page ->
                        page.semantic.config.getOrNull(SemanticsActions.ScrollBy)?.action?.invoke(0f, page.boundsInWindow.height)
                    }
                }
            }
            click(text(Res.string.allow), "approve expected request")
            report("pairing_approval", mode.name)
            delay(args.getString("managedHoldMillis")?.toLong() ?: 90_000L)
        }
    }

    private fun report(key: String, value: String) = instrumentation.sendStatus(2, Bundle().apply { putString(key, value) })

    private suspend fun <T> awaitValue(role: String, value: suspend () -> T?): T {
        val deadline = System.nanoTime() + 90_000_000_000L
        while (System.nanoTime() < deadline) {
            value()?.let { return it }
            delay(200)
        }
        throw AssertionError("Timed out waiting for $role; visible text: ${allText()}")
    }

    @Test
    fun managesRemoteNoteThroughTheDeviceSelectorWithoutWritingLocalContent() = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use {
            val targetId = checkNotNull(InstrumentationRegistry.getArguments().getString("managedPeerId"))
            val peer = checkNotNull(GraphqlApi.execute(Query.peers()).find { it.id == targetId })
            check(peer.isPaired() && peer.token.isNotEmpty())
            val remote = GraphqlApi.forPeer(peer)
            val target = remote.execute(identityRequest)
            assertEquals(targetId, target.clientId)
            val marker = "managed-ui-${UUID.randomUUID()}"
            val note = remote.execute(Mutation.saveNote("", marker, "# $marker\n\nInitial $marker"))
            try {
                assertNull(GraphqlApi.execute(Query.note(note.id)))
                openRemoteTools(peer.name)
                awaitNode("target tools page") { it.text?.toString() == target.deviceName }
                val tools = allText()
                for (excluded in listOf(Res.string.sound_meter, Res.string.pomodoro_timer, Res.string.image_editor)) {
                    assertFalse("Local tool exposed for remote management", tools.contains(text(excluded)))
                }
                click(text(Res.string.notes), "remote notes")
                awaitNode("note fixture") { it.text?.toString()?.contains(marker) == true }
                click(marker, "remote note fixture")
                val paragraph = awaitNode("existing paragraph preview") { it.text?.contains("Initial $marker") == true }
                val point = paragraph.screenCenter
                val down = android.os.SystemClock.uptimeMillis()
                for (action in listOf(android.view.MotionEvent.ACTION_DOWN, android.view.MotionEvent.ACTION_UP)) {
                    val event = android.view.MotionEvent.obtain(down, android.os.SystemClock.uptimeMillis(), action, point.x, point.y, 0)
                    instrumentation.sendPointerSync(event)
                    event.recycle()
                }
                val field = awaitNode("remote note editor") { it.isEditable && it.text?.toString()?.contains("Initial $marker") == true }
                val edited = "Edited $marker"
                val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, edited) }
                assertTrue(field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args))
                awaitValue("persisted remote edit") { remote.execute(Query.note(note.id))?.takeIf { it.content.contains(edited) } }
                assertTrue(checkNotNull(remote.execute(Query.note(note.id))).content.contains(edited))
                click(text(Res.string.back), "return to remote notes")
                awaitValue("closed remote note editor") { if (uiNodes().none { it.isEditable }) true else null }
                assertNull(GraphqlApi.execute(Query.note(note.id)))
                openActions(marker)
                click(text(Res.string.trash), "trash remote note")
                awaitValue("trashed remote note") { remote.execute(Query.note(note.id))?.takeIf { it.deletedAt != null } }
                openNotesSidebar()
                selectDrawerItem(text(Res.string.trash), "remote trash list")
                awaitNode("trashed note row") { it.text == marker }
                openActions(marker)
                click(text(Res.string.restore), "restore remote note")
                awaitValue("restored remote note") { remote.execute(Query.note(note.id))?.takeIf { it.deletedAt == null } }
                openNotesSidebar()
                selectDrawerItem(text(Res.string.all), "active remote notes list")
                awaitNode("restored note row") { it.text == marker }
                openActions(marker)
                click(text(Res.string.trash), "trash remote note again")
                awaitValue("second remote trash") { remote.execute(Query.note(note.id))?.takeIf { it.deletedAt != null } }
                openNotesSidebar()
                selectDrawerItem(text(Res.string.trash), "remote trash list")
                awaitNode("trashed note for permanent deletion") { it.text == marker }
                openActions(marker)
                click(text(Res.string.delete), "delete remote note permanently")
                awaitValue("removed remote note") { if (remote.execute(Query.note(note.id)) == null) true else null }
                assertNull(GraphqlApi.execute(Query.note(note.id)))
                report("remote_note_result", "edit_trash_restore_delete_verified_local_absent")
            } finally {
                val remoteNote = remote.execute(Query.note(note.id))
                if (remoteNote?.title == marker) {
                    remote.execute(Mutation.trashNotes("ids:=${note.id}"))
                    remote.execute(Mutation.deleteNotes("ids:=${note.id}"))
                }
                val localNote = GraphqlApi.execute(Query.note(note.id))
                if (localNote?.title == marker) {
                    GraphqlApi.execute(Mutation.trashNotes("ids:=${note.id}"))
                    GraphqlApi.execute(Mutation.deleteNotes("ids:=${note.id}"))
                }
                assertNull(remote.execute(Query.note(note.id)))
            }
        }
    }

    private suspend fun assertNoteImageRendered(scenario: ActivityScenario<MainActivity>, colorTolerance: Int = 0) {
        awaitValue("note image rendered in the current window") {
            val capture = kotlinx.coroutines.CompletableDeferred<android.graphics.Bitmap>()
            scenario.onActivity { activity ->
                val decor = activity.window.decorView
                val bitmap = android.graphics.Bitmap.createBitmap(decor.width, decor.height, android.graphics.Bitmap.Config.ARGB_8888)
                android.view.PixelCopy.request(activity.window, bitmap, { result ->
                    if (result == android.view.PixelCopy.SUCCESS) capture.complete(bitmap)
                    else {
                        bitmap.recycle()
                        capture.completeExceptionally(AssertionError("Window pixel copy failed: $result"))
                    }
                }, android.os.Handler(android.os.Looper.getMainLooper()))
            }
            val screen = kotlinx.coroutines.withTimeout(5000) { capture.await() }
            var matching = 0
            for (y in 0 until screen.height step 4) for (x in 0 until screen.width step 4) {
                val pixel = screen.getPixel(x, y)
                if (android.graphics.Color.red(pixel) >= 255 - colorTolerance && android.graphics.Color.green(pixel) <= colorTolerance && android.graphics.Color.blue(pixel) >= 255 - colorTolerance) matching++
            }
            screen.recycle()
            if (matching >= 64) true else null
        }
    }

    private fun openRemoteTools(peerName: String) {
        click(text(Res.string.chat), "chat navigation")
        click(text(Res.string.select_a_device), "device selector")
        click(peerName, "selected device")
        click(text(Res.string.tools), "selected device tools")
    }

    private suspend fun openRemoteNoteImageEditor(peerName: String, noteTitle: String) {
        openRemoteTools(peerName)
        click(text(Res.string.notes), "remote notes")
        click(noteTitle, "owned remote note")
        val paragraph = awaitNode("editable note preview") { it.text?.contains("Initial $noteTitle") == true }
        val point = paragraph.screenCenter
        touch(point.x, point.y)
        awaitNode("note editor") { it.isEditable }
        click(text(Res.string.md_insert), "insert toolbar")
        click(text(Res.string.image), "insert image")
        awaitNode("image insert dialog") { it.text == text(Res.string.insert_image) }
    }

    @Test
    fun resourceDownloadsShowProgressAndCancelWithoutPublishingPartialFiles() = runBlocking {
        val context = instrumentation.targetContext
        val intent = android.content.Intent(context, MainActivity::class.java)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        ActivityScenario.launch<MainActivity>(intent).use {
            awaitNode("chat navigation") { it.contentDescription == text(Res.string.chat) }
            val bytes = ByteArray(128 * 1024) { (it % 251).toByte() }
            val downloads = java.io.File(context.cacheDir, "resource_downloads")
            for (cancel in listOf(true, false)) {
                val marker = "managed-download-${UUID.randomUUID()}"
                val directory = java.io.File(context.cacheDir, marker)
                val before = downloads.list()?.toSet().orEmpty()
                val firstChunk = kotlinx.coroutines.CompletableDeferred<Unit>()
                val complete = kotlinx.coroutines.CompletableDeferred<Unit>()
                val server = java.net.ServerSocket(0, 1, java.net.InetAddress.getByName("127.0.0.1"))
                val serving = async(kotlinx.coroutines.Dispatchers.IO) {
                    try { server.accept().use { connection ->
                        val input = connection.getInputStream().bufferedReader()
                        while (input.readLine()?.isNotEmpty() == true) { }
                        val output = connection.getOutputStream()
                        output.write("HTTP/1.1 200 OK\r\nContent-Length: ${bytes.size}\r\nContent-Type: application/octet-stream\r\nConnection: close\r\n\r\n".toByteArray())
                        output.write(bytes, 0, bytes.size / 2)
                        output.flush()
                        firstChunk.complete(Unit)
                        complete.await()
                        if (!cancel) { output.write(bytes, bytes.size / 2, bytes.size / 2); output.flush() }
                    } } catch (error: java.net.SocketException) {
                        if (!server.isClosed) throw error
                    }
                }
                val operation = async {
                    com.ismartcoding.plain.platform.media.savePreviewMedia(
                        com.ismartcoding.plain.ui.media.viewer.PreviewItem(marker, "http://127.0.0.1:${server.localPort}/$marker.pdf"), directory.path,
                    )
                }
                try {
                    kotlinx.coroutines.withTimeout(10000) { firstChunk.await() }
                    awaitNode("visible download file name") { it.text == "$marker.pdf" }
                    awaitNode("visible received and total bytes") { it.text?.contains(" / ") == true }
                    assertFalse(directory.exists())
                    if (cancel) {
                        click(text(Res.string.cancel), "cancel resource download")
                        kotlinx.coroutines.withTimeout(10000) { operation.join() }
                        assertTrue("Cancelled download must not return", operation.isCancelled)
                        assertFalse(directory.exists())
                    } else {
                        complete.complete(Unit)
                        kotlinx.coroutines.withTimeout(10000) { operation.await() }
                        assertArrayEquals(bytes, java.io.File(directory, "$marker.pdf").readBytes())
                    }
                    awaitValue("download files released") {
                        if (before == downloads.list()?.toSet().orEmpty()) true else null
                    }
                } finally {
                    complete.complete(Unit)
                    operation.cancel()
                    server.close()
                    serving.cancel()
                    if (directory.exists()) check(directory.deleteRecursively())
                }
            }
            report("resource_download", "progress_cancel_partial_cleanup_and_complete_bytes_verified")
        }
    }

    @Test
    fun localFileReplacementStillUsesNativeChunks() = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use {
            val paths = GraphqlApi.execute(GraphqlRequest<ManagedAcceptancePaths>("query { app { appDir deviceName } }", field = "app"))
            val directory = paths.appDir.trimEnd('/') + "/managed-local-file-" + UUID.randomUUID()
            val path = "$directory/owned.txt"
            GraphqlApi.execute(Mutation.fileWrite(FileWriteAction.CREATE_DIRECTORY, directory, "", false))
            try {
                GraphqlApi.execute(Mutation.fileWrite(FileWriteAction.WRITE_TEXT, path, "before", false))
                val bytes = ByteArray(2 * 1024 * 1024 + 3) { (it % 251).toByte() }
                assertTrue(com.ismartcoding.plain.features.files.FilesClient.writeChunks(
                    path, bytes.asSequence().chunked(65537).map { it.toByteArray() }.iterator(),
                ))
                assertArrayEquals(bytes, java.io.File(path).readBytes())
                assertEquals(bytes.size.toLong(), GraphqlApi.execute(Query.fileInfo(path)).size)
                report("local_file_chunks", "native_replace_exact_bytes_verified")
            } finally {
                GraphqlApi.execute(Mutation.deleteFiles(listOf(directory)))
                assertFalse(java.io.File(directory).exists())
            }
        }
    }

    @Test
    fun localNoteImageImportStillUsesTheNativeStore() = runBlocking {
        val intent = android.content.Intent(instrumentation.targetContext, MainActivity::class.java)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            awaitNode("local tools navigation") { it.contentDescription == text(Res.string.tools) }
            val marker = "managed-local-image-${UUID.randomUUID()}"
            val paths = GraphqlApi.execute(GraphqlRequest<ManagedAcceptancePaths>("query { app { appDir deviceName } }", field = "app"))
            val permissionSnapshot = Prefs.apiPermissions.value
            val picked = java.io.File(instrumentation.targetContext.cacheDir, "$marker.png")
            val bitmap = android.graphics.Bitmap.createBitmap(64, 64, android.graphics.Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.MAGENTA)
            picked.outputStream().use { check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)) }
            bitmap.recycle()
            val note = GraphqlApi.execute(Mutation.saveNote("", marker, "Initial $marker"))
            var reference: String? = null
            val exported = java.io.File(instrumentation.targetContext.cacheDir, "$marker-export")
            assertFalse(exported.exists())
            try {
                click(text(Res.string.tools), "local tools")
                click(text(Res.string.notes), "local notes")
                click(marker, "owned local note")
                val paragraph = awaitNode("local note preview") { it.text?.contains("Initial $marker") == true }
                touch(paragraph.screenCenter.x, paragraph.screenCenter.y)
                awaitNode("local note editor") { it.isEditable }
                click(text(Res.string.md_insert), "local insert toolbar")
                click(text(Res.string.image), "local image dialog")
                awaitNode("local browse needs no remote authorization") { it.text == text(Res.string.browse) }
                assertFalse(allText().contains(text(Res.string.request_permission)))
                com.ismartcoding.plain.lib.sendEvent(com.ismartcoding.plain.events.PickFileResultEvent(
                    com.ismartcoding.plain.features.files.PickFileTag.EDITOR,
                    com.ismartcoding.plain.features.files.PickFileType.IMAGE,
                    setOf(androidx.core.content.FileProvider.getUriForFile(instrumentation.targetContext, instrumentation.targetContext.packageName + ".provider", picked).toString()),
                ))
                reference = awaitNode("native image reference") { it.isEditable && it.text?.startsWith("app://note-images/$marker") == true }.text
                val imageReference = checkNotNull(reference)
                click(text(Res.string.confirm), "insert native image")
                awaitValue("native note image saved") { GraphqlApi.execute(Query.note(note.id))?.takeIf { it.content.contains(imageReference) } }
                val localPath = paths.appDir.trimEnd('/') + "/" + imageReference.removePrefix("app://")
                assertEquals(picked.length(), java.io.File(localPath).length())
                assertEquals(permissionSnapshot, Prefs.apiPermissions.value)
                click(text(Res.string.view), "view local note image")
                assertNoteImageRendered(scenario)
                com.ismartcoding.plain.platform.media.savePreviewMedia(
                    com.ismartcoding.plain.ui.media.viewer.PreviewItem(marker, localPath, picked.length()), exported.path,
                )
                val saved = java.io.File(exported, java.io.File(localPath).name)
                assertArrayEquals(picked.readBytes(), saved.readBytes())
                assertArrayEquals(picked.readBytes(), java.io.File(localPath).readBytes())
                report("local_note_image", "native_store_import_render_and_export_without_remote_permission")
            } finally {
                if (exported.exists()) check(exported.deleteRecursively())
                GraphqlApi.execute(Mutation.trashNotes("ids:=${note.id}"))
                GraphqlApi.execute(Mutation.deleteNotes("ids:=${note.id}"))
                reference?.let { check(java.io.File(paths.appDir, it.removePrefix("app://")).delete()) }
                check(picked.delete())
            }
        }
    }

    @Test
    fun requestsTargetStorageAccessFromTheNoteImageEditor() = runBlocking {
        val intent = android.content.Intent(instrumentation.targetContext, MainActivity::class.java)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        ActivityScenario.launch<MainActivity>(intent).use {
            awaitNode("chat navigation") { it.contentDescription == text(Res.string.chat) }
            val args = InstrumentationRegistry.getArguments()
            val peer = checkNotNull(GraphqlApi.execute(Query.peers()).find { it.id == args.getString("managedPeerId") })
            val remote = GraphqlApi.forPeer(peer)
            val permission = Permission.WRITE_EXTERNAL_STORAGE
            awaitValue("target storage access disabled") { if (permission !in remote.execute(Query.appPermissions())) true else null }
            val marker = "managed-image-permission-${UUID.randomUUID()}"
            val note = remote.execute(Mutation.saveNote("", marker, "Initial $marker"))
            try {
                openRemoteNoteImageEditor(peer.name, marker)
                awaitNode("image upload permission action") { it.text == text(Res.string.request_permission) }
                assertFalse(allText().any { it.contains("no_permission") })
                click(text(Res.string.request_permission), "request image upload access")
                if (args.getString("managedAccessDecision") == "APPROVE") {
                    awaitNode("image browse restored after consent") { it.text == text(Res.string.browse) }
                    assertTrue(permission in remote.execute(Query.appPermissions()))
                } else {
                    awaitNode("permission remains available after rejection") { it.text == text(Res.string.request_permission) }
                    assertFalse(permission in remote.execute(Query.appPermissions()))
                }
                assertTrue(allText().contains(text(Res.string.insert_image)))
                assertFalse(allText().any { it.contains("no_permission") })
                report("remote_note_image_permission", "editor_preserved_with_target_consent")
            } finally {
                remote.execute(Mutation.trashNotes("ids:=${note.id}"))
                remote.execute(Mutation.deleteNotes("ids:=${note.id}"))
            }
        }
    }

    @Test
    fun rectangularThumbnailsKeepTheirRatioInsideSquareCells() = runBlocking {
        val context = instrumentation.targetContext
        val intent = android.content.Intent(context, MainActivity::class.java)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            awaitNode("chat navigation") { it.contentDescription == text(Res.string.chat) }
            for ((width, height, extension) in listOf(Triple(900, 300, "png"), Triple(300, 900, "png"), Triple(900, 300, "svg"), Triple(300, 900, "svg"))) {
                val file = java.io.File(context.cacheDir, "thumbnail-ratio-${UUID.randomUUID()}.$extension")
                val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.MAGENTA)
                try {
                    if (extension == "svg") {
                        file.writeText("""<svg xmlns="http://www.w3.org/2000/svg" width="$width" height="$height" viewBox="0 0 $width $height"><rect width="$width" height="$height" fill="#ff00ff"/></svg>""")
                    } else {
                        file.outputStream().use { output -> check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output)) }
                    }
                    val thumbnail = checkNotNull(com.ismartcoding.plain.ui.media.thumbnail.ThumbnailGenerator.getBitmapAsync(
                        context, file, 256, 256, centerCrop = false,
                    ))
                    try {
                        assertEquals(width.toFloat() / height, thumbnail.width.toFloat() / thumbnail.height, 0.03f)
                    } finally { thumbnail.recycle() }
                    var item: com.ismartcoding.plain.ui.media.viewer.previewer.TransformItemState? = null
                    scenario.onActivity { activity ->
                        activity.setContent {
                            androidx.compose.runtime.key(file.path) {
                                val state = com.ismartcoding.plain.ui.media.viewer.previewer.rememberTransformItemState()
                                item = state
                                com.ismartcoding.plain.ui.media.viewer.previewer.TransformImageViewWithUri(
                                    modifier = Modifier.width(160.dp).height(160.dp),
                                    path = file.path, fileName = file.name, key = file.path,
                                    itemState = state,
                                    previewerState = com.ismartcoding.plain.ui.media.viewer.previewer.rememberPreviewerState(pageCount = { 1 }),
                                    widthPx = 256,
                                )
                            }
                        }
                    }
                    awaitValue("rectangular image decoded inside square cell") {
                        var ready = false
                        scenario.onActivity {
                            val state = item
                            val size = state?.intrinsicSize
                            if (state != null && size != null && state.blockSize.width > 0 && state.blockSize.height > 0) {
                                assertEquals(state.blockSize.width, state.blockSize.height)
                                assertEquals(width.toFloat() / height, size.width / size.height, 0.03f)
                                ready = true
                            }
                        }
                        if (ready) true else null
                    }
                    assertNoteImageRendered(scenario)
                } finally { bitmap.recycle(); check(file.delete()) }
            }
            report("thumbnail_ratio", "png_and_svg_landscape_and_portrait_preserved_inside_square_cells")
        }
    }

    @Test
    fun remoteNoteImagesAreUploadedToTheTargetStore() = runBlocking {
        val intent = android.content.Intent(instrumentation.targetContext, MainActivity::class.java)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            awaitNode("chat navigation") { it.contentDescription == text(Res.string.chat) }
            val peerId = checkNotNull(InstrumentationRegistry.getArguments().getString("managedPeerId"))
            val peer = checkNotNull(GraphqlApi.execute(Query.peers()).find { it.id == peerId })
            val remote = GraphqlApi.forPeer(peer)
            val marker = "managed-image-${UUID.randomUUID()}"
            val targetPaths = remote.execute(GraphqlRequest<ManagedAcceptancePaths>("query { app { appDir deviceName } }", field = "app"))
            val localPaths = GraphqlApi.execute(GraphqlRequest<ManagedAcceptancePaths>("query { app { appDir deviceName } }", field = "app"))
            val localImages = java.io.File(localPaths.appDir, "note-images")
            fun imageNames() = if (localImages.exists()) checkNotNull(localImages.list()).toSet() else emptySet()
            val beforeImages = imageNames()
            val picked = java.io.File(instrumentation.targetContext.cacheDir, "$marker.png")
            val bitmap = android.graphics.Bitmap.createBitmap(512, 512, android.graphics.Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.MAGENTA)
            val qrValue = "managed-image-${UUID.randomUUID()}"
            val qrBitmap = com.ismartcoding.plain.ui.qrcode.generateQrCode(qrValue, 384, 384).asAndroidBitmap()
            android.graphics.Canvas(bitmap).drawBitmap(qrBitmap, 64f, 64f, null)
            qrBitmap.recycle()
            picked.outputStream().use { output -> check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output)) }
            bitmap.recycle()
            val note = remote.execute(Mutation.saveNote("", marker, "Initial $marker"))
            var reference: String? = null
            var attachment: com.ismartcoding.plain.models.AppFile? = null
            var messageId: String? = null
            var savedImage: String? = null
            val downloaded = java.io.File(instrumentation.targetContext.cacheDir, "resource_downloads")
            val beforeDownloads = downloaded.list()?.toSet().orEmpty()
            var failure: Throwable? = null
            try {
                openRemoteNoteImageEditor(peer.name, marker)
                com.ismartcoding.plain.lib.sendEvent(com.ismartcoding.plain.events.PickFileResultEvent(
                    com.ismartcoding.plain.features.files.PickFileTag.EDITOR,
                    com.ismartcoding.plain.features.files.PickFileType.IMAGE,
                    setOf(androidx.core.content.FileProvider.getUriForFile(instrumentation.targetContext, instrumentation.targetContext.packageName + ".provider", picked).toString()),
                ))
                reference = awaitNode("target stored image reference") { it.isEditable && it.text?.startsWith("app://note-images/") == true }.text
                val imageReference = checkNotNull(reference)
                report("owned_note_image_reference", imageReference)
                click(text(Res.string.confirm), "insert target image")
                awaitValue("remote note image saved") { remote.execute(Query.note(note.id))?.takeIf { it.content.contains(imageReference) } }
                assertEquals(picked.length(), remote.execute(Query.fileInfo(imageReference)).size)
                val fileQuery = "text:" + imageReference.substringAfterLast('/')
                assertEquals(0, remote.execute(Query.appFileCount(fileQuery)))
                assertEquals(0, GraphqlApi.execute(Query.appFileCount(fileQuery)))
                val copied = remote.execute(Mutation.importAppFile(imageReference, "$marker.png", "image/png", false))
                attachment = copied
                val content = com.ismartcoding.plain.features.chat.models.MessageContent(
                    com.ismartcoding.plain.features.chat.models.MessageType.IMAGES,
                    com.ismartcoding.plain.features.chat.models.MessageImages(listOf(com.ismartcoding.plain.features.chat.models.MessageFile(
                        uri = "fid:" + copied.realPath.substringAfterLast('/'), size = copied.size, fileName = "$marker.png",
                    ))),
                )
                val sent = remote.execute(Mutation.sendChatItem("peer:local", JsonHelper.jsonEncode(content))).single()
                messageId = sent.id
                remote.execute(Mutation.deleteChatItem(sent.id))
                messageId = null
                attachment = null
                assertEquals(picked.length(), remote.execute(Query.fileInfo(imageReference)).size)
                assertTrue(checkNotNull(remote.execute(Query.note(note.id))).content.contains(imageReference))
                assertEquals(beforeImages, imageNames())
                assertNull(GraphqlApi.execute(Query.note(note.id)))
                click(text(Res.string.view), "read saved note")
                assertNoteImageRendered(scenario)
                click(imageReference, "preview the remote note image")
                val preview = awaitValue("target-scoped preview item") {
                    com.ismartcoding.plain.ui.media.MediaPreviewData.items.firstOrNull { it.path == imageReference }
                }
                assertTrue(preview.source is com.ismartcoding.plain.rust.graphql.ContentSource.Remote)
                val targetSource = preview.source as com.ismartcoding.plain.rust.graphql.ContentSource.Remote
                assertEquals(qrValue, com.ismartcoding.plain.ui.platform.media.tryDecodeQrCode(imageReference, preview.source))
                val output = targetSource.info.downloadsDir.trimEnd('/') + "/" + imageReference.substringAfterLast('/')
                assertEquals(0L, remote.execute(Query.fileInfo(output)).size)
                savedImage = output
                click(text(Res.string.save), "save remote image")
                click(text(Res.string.download_to_downloads), "save to target Downloads")
                awaitValue("image saved on the target") {
                    remote.execute(Query.fileInfo(output)).size.takeIf { it == picked.length() }
                }
                assertEquals(beforeDownloads, downloaded.list()?.toSet().orEmpty())
                assertEquals(beforeImages, imageNames())
                assertEquals(picked.length(), remote.execute(Query.fileInfo(imageReference)).size)
                report("remote_note_image", "target_store_preview_and_target_save_verified_local_absent")
            } catch (error: Throwable) {
                failure = error
                throw error
            } finally {
                try {
                    remote.execute(Mutation.trashNotes("ids:=${note.id}"))
                    remote.execute(Mutation.deleteNotes("ids:=${note.id}"))
                    messageId?.let { remote.execute(Mutation.deleteChatItem(it)) }
                    if (messageId == null) attachment?.let { remote.execute(Mutation.releaseAppFile(it.id)) }
                    savedImage?.let { remote.execute(Mutation.deleteFiles(listOf(it))) }
                    reference?.let { remote.execute(Mutation.deleteFiles(listOf(targetPaths.appDir.trimEnd('/') + "/" + it.removePrefix("app://")))) }
                    check(picked.delete())
                } catch (cleanup: Throwable) {
                    if (failure == null) throw cleanup
                    failure.addSuppressed(cleanup)
                }
            }
        }
    }

    @Test
    fun remoteAudioUsesTheTargetQueueAndFsWithLocalRendering() = runBlocking {
        val intent = android.content.Intent(instrumentation.targetContext, MainActivity::class.java)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        ActivityScenario.launch<MainActivity>(intent).use {
            awaitNode("chat navigation") { it.contentDescription == text(Res.string.chat) }
            val args = InstrumentationRegistry.getArguments()
            val peerId = checkNotNull(args.getString("managedPeerId"))
            val prefix = checkNotNull(args.getString("managedAudioPrefix"))
            require(prefix.startsWith("managed-acceptance-"))
            val peer = checkNotNull(GraphqlApi.execute(Query.peers()).find { it.id == peerId })
            val remote = GraphqlApi.forPeer(peer)
            val type = com.ismartcoding.plain.platform.system.DataType.AUDIO
            val tracks = remote.execute(Query.mediaRows(type, "text:$prefix trash:false", 10, 0, FileSortBy.NAME_ASC))
                .filterIsInstance<com.ismartcoding.plain.models.Audio>().sortedBy { it.title }
            assertEquals("Two owned MP3 fixtures must be scanned on the target", 2, tracks.size)
            require(tracks.all { it.path.substringAfterLast('/').startsWith(prefix) && it.path.endsWith(".mp3") })
            val nativeQueue = GraphqlApi.execute(Query.audioQueueItems(0, 10000, ""))
            val nativePlayback = GraphqlApi.execute(Query.audioPlayback())
            val targetQueue = remote.execute(Query.audioQueueItems(0, 10000, ""))
            val targetPlayback = remote.execute(Query.audioPlayback())
            assertFalse("Target player must be idle before acceptance", targetPlayback.isPlaying)
            val historyQuery = "type:AUDIO ids:" + tracks.joinToString(",") { it.id }
            val nativeHistory = GraphqlApi.execute(Query.mediaPlayHistory(historyQuery, 0, 100))
            val previousSelection = targetPlayback.mediaId?.takeIf { id ->
                remote.execute(Query.mediaRows(type, "ids:=$id", 1, 0, FileSortBy.NAME_ASC)).any { it.id == id }
            }
            var failure: Throwable? = null
            val nativeMode = com.ismartcoding.plain.Prefs.audioPlayMode.value
            val nativeSpeed = com.ismartcoding.plain.Prefs.audioPlaybackSpeed.value
            try {
                remote.execute(Mutation.updateAudioPlayMode(com.ismartcoding.plain.features.audio.MediaPlayMode.REPEAT))
                openRemoteTools(peer.name)
                click(text(Res.string.audios), "remote audio library")
                assertFalse("Granted remote storage must not flash the request-permission page", allText().contains(text(Res.string.remote_permission_required)))
                click(text(Res.string.view_all), "all remote audio", firstMatch = true)
                var playbackStartedAt = 0L
                click(tracks.first().title, "target MP3 fixture", onClick = { playbackStartedAt = android.os.SystemClock.elapsedRealtime() })
                awaitValue("current phone is rendering remote audio") {
                    var playing = false
                    instrumentation.runOnMainSync { playing = com.ismartcoding.plain.platform.media.AndroidAudioPlayer.isPlaying() }
                    if (playing) true else null
                }
                val playbackStartMs = android.os.SystemClock.elapsedRealtime() - playbackStartedAt
                report("remote_audio_start_ms", playbackStartMs.toString())
                args.getString("managedAudioStartLimitMs")?.toLong()?.let { limit ->
                    assertTrue("Remote audio took ${playbackStartMs}ms; limit ${limit}ms", playbackStartMs <= limit)
                }
                var sdkPath = ""
                instrumentation.runOnMainSync { sdkPath = com.ismartcoding.plain.platform.media.createAudioPlayer().currentPath }
                assertTrue("Renderer must read target /fs", sdkPath.startsWith(checkNotNull(peer.address).baseUrl.trimEnd('/') + "/fs?"))
                awaitValue("remote playing history") { remote.execute(Query.mediaPlayHistory(historyQuery, 0, 100)).firstOrNull { it.mediaId == tracks.first().id } }
                assertFalse("Target device must not play audio", remote.execute(Query.audioPlayback()).isPlaying)
                remote.execute(Mutation.addAudioToQueue("ids:=" + tracks.last().id))
                com.ismartcoding.plain.features.audio.AudioCommands.command(com.ismartcoding.plain.models.AudioCommand.NEXT)
                awaitValue("target next-track selection") { remote.execute(Query.audioPlayback()).takeIf { it.mediaId == tracks.last().id } }
                awaitValue("next track actually started") { remote.execute(Query.mediaPlayHistory(historyQuery, 0, 100)).firstOrNull { it.mediaId == tracks.last().id } }
                com.ismartcoding.plain.features.audio.AudioCommands.command(com.ismartcoding.plain.models.AudioCommand.SEEK, 10_000)
                awaitValue("current phone seek") {
                    var position = 0L
                    instrumentation.runOnMainSync { position = com.ismartcoding.plain.platform.media.audioPlayerProgress() }
                    if (position >= 10_000) true else null
                }
                com.ismartcoding.plain.features.audio.AudioCommands.command(com.ismartcoding.plain.models.AudioCommand.PAUSE)
                awaitValue("target progress record") { remote.execute(Query.mediaPlayHistory(historyQuery, 0, 100)).firstOrNull { it.mediaId == tracks.last().id && it.positionMs >= 10_000 } }
                assertEquals(nativeQueue, GraphqlApi.execute(Query.audioQueueItems(0, 10000, "")))
                assertEquals(nativePlayback, GraphqlApi.execute(Query.audioPlayback()))
                assertEquals(nativeHistory, GraphqlApi.execute(Query.mediaPlayHistory(historyQuery, 0, 100)))
                assertEquals(nativeMode, com.ismartcoding.plain.Prefs.audioPlayMode.value)
                assertEquals(nativeSpeed, com.ismartcoding.plain.Prefs.audioPlaybackSpeed.value)
                report("remote_audio", "target_queue_history_fs_and_controller_playback_verified_local_unchanged")
            } catch (error: Throwable) {
                failure = error
                throw error
            } finally {
                try {
                    if (com.ismartcoding.plain.features.audio.AudioCommands.output.value.source is com.ismartcoding.plain.rust.graphql.ContentSource.Remote) {
                        com.ismartcoding.plain.features.audio.AudioCommands.command(com.ismartcoding.plain.models.AudioCommand.PAUSE)
                    }
                    tracks.forEach { remote.execute(Mutation.removeAudioFromQueue(it.id)) }
                    remote.execute(Mutation.deleteFiles(tracks.map { it.path }))
                    remote.execute(Mutation.updateAudioPlayMode(targetPlayback.mode))
                    previousSelection?.let { remote.execute(Mutation.playAudio(it)) }
                    assertEquals(targetQueue.map { it.mediaId }, remote.execute(Query.audioQueueItems(0, 10000, "")).map { it.mediaId })
                } catch (cleanup: Throwable) {
                    if (failure == null) throw cleanup
                    failure.addSuppressed(cleanup)
                }
            }
        }
    }

    @Test
    fun remoteFeedManagementKeepsItsCatalogAndEditsOnTheTarget() = runBlocking {
        val peerId = checkNotNull(InstrumentationRegistry.getArguments().getString("managedPeerId"))
        val marker = "managed-feed-${UUID.randomUUID()}"
        ActivityScenario.launch(MainActivity::class.java).use {
            awaitNode("main navigation") { it.contentDescription == text(Res.string.chat) }
            val peer = checkNotNull(GraphqlApi.execute(Query.peers()).find { it.id == peerId })
            val source = com.ismartcoding.plain.rust.graphql.ContentSource.Remote(com.ismartcoding.plain.features.chat.peer.ManagedDeviceClient.load(peer))
            source.feeds.importAsync("""<opml version="2.0"><head><title>Owned acceptance</title></head><body><outline type="rss" text="$marker" title="$marker" xmlUrl="https://example.test/$marker.xml"/></body></opml>""")
            val feed = source.feeds.getAll().single { it.name == marker }
            try {
                assertNull(com.ismartcoding.plain.features.feeds.FeedsClient.getById(feed.id))
                openRemoteTools(peer.name)
                click(text(Res.string.feeds), "existing target feed page")
                var drawer = awaitNode("target feeds drawer") { it.contentDescription == text(Res.string.feeds) }
                while (!drawer.isClickable) drawer = checkNotNull(drawer.parent)
                assertTrue(drawer.performAction(AccessibilityNodeInfo.ACTION_CLICK))
                openActions(marker)
                click(text(Res.string.edit), "edit target feed")
                val field = awaitNode("target feed name input") { it.isEditable && it.text == marker }
                val edited = "$marker-edited"
                assertTrue(field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, edited)
                }))
                click(text(Res.string.save), "persist target feed name")
                awaitValue("target feed rename") { source.feeds.getById(feed.id)?.takeIf { it.name == edited } }
                assertNull(com.ismartcoding.plain.features.feeds.FeedsClient.getById(feed.id))
                openActions(edited)
                click(text(Res.string.delete), "delete owned target feed")
                awaitValue("target feed deleted") { if (source.feeds.getById(feed.id) == null) true else null }
                assertNull(com.ismartcoding.plain.features.feeds.FeedsClient.getById(feed.id))
                report("remote_feeds", "target_catalog_rename_delete_and_native_isolation_verified")
            } finally {
                if (source.feeds.getById(feed.id) != null) source.feeds.deleteAsync(setOf(feed.id))
                assertNull(source.feeds.getById(feed.id))
            }
        }
    }

    @Test
    fun globalSearchDiscoversThePairedDeviceAndOpensItsMedia() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        val peerId = checkNotNull(args.getString("managedPeerId"))
        val prefix = checkNotNull(args.getString("managedMediaPrefix"))
        require(prefix.startsWith("managed-acceptance-"))
        val domain = com.ismartcoding.plain.ui.search.GlobalSearchDomain.valueOf(checkNotNull(args.getString("managedMediaFeature")))
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            awaitNode("main navigation") { it.contentDescription == text(Res.string.chat) }
            val peer = checkNotNull(GraphqlApi.execute(Query.peers()).find { it.id == peerId })
            val expected = com.ismartcoding.plain.rust.graphql.ContentSource.Remote(com.ismartcoding.plain.features.chat.peer.ManagedDeviceClient.load(peer))
            lateinit var model: com.ismartcoding.plain.ui.search.GlobalSearchViewModel
            scenario.onActivity { activity ->
                model = com.ismartcoding.plain.ui.search.GlobalSearchViewModel()
                model.queryText.value = prefix
                model.domain.value = domain
                model.submitted.value = true
                activity.viewModelStore.put("globalSearchVM", model)
            }
            scenario.recreate()
            click(text(Res.string.search), "production global search")
            scenario.onActivity { model.search() }
            val hit = awaitValue("production-enumerated target search hit") {
                model.domainStates[domain]?.hits?.value?.singleOrNull { it.device.key == expected.key }
            }
            assertFalse(model.domainStates.getValue(domain).hits.value.any { it.device is com.ismartcoding.plain.rust.graphql.ContentSource.Local })
            assertTrue(model.deviceErrors.value.none { it.name == peer.name })
            if (domain == com.ismartcoding.plain.ui.search.GlobalSearchDomain.DOCS) {
                click(hit.title, "production target document result")
                awaitNode("downloaded target search document") { it.text?.contains("Target document $prefix") == true }
            } else {
                val path = when (val value = hit.source) {
                    is com.ismartcoding.plain.ui.search.GlobalSearchSource.Image -> value.image.path
                    is com.ismartcoding.plain.ui.search.GlobalSearchSource.Video -> value.video.path
                    else -> error("Unexpected media search source")
                }
                var row = awaitNode("production target media result") { it.contentDescription == path }
                while (!row.isClickable) row = checkNotNull(row.parent)
                assertTrue(row.performAction(AccessibilityNodeInfo.ACTION_CLICK))
                awaitValue("search preview uses discovered target") {
                    com.ismartcoding.plain.ui.media.MediaPreviewData.items.firstOrNull { it.path == path && it.source.key == expected.key }
                }
                assertNoteImageRendered(scenario, colorTolerance = 4)
            }
            report("production_search_media", domain.name + "_device_enumeration_target_result_and_preview_verified")
        }
    }

    @Test
    fun remoteMediaPagesReadAndPreviewOnlyTheSelectedDevice() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        val peerId = checkNotNull(args.getString("managedPeerId"))
        val prefix = checkNotNull(args.getString("managedMediaPrefix"))
        require(prefix.startsWith("managed-acceptance-"))
        val feature = checkNotNull(args.getString("managedMediaFeature"))
        val (type, title) = when (feature) {
            "IMAGES" -> com.ismartcoding.plain.platform.system.DataType.IMAGE to Res.string.images
            "VIDEOS" -> com.ismartcoding.plain.platform.system.DataType.VIDEO to Res.string.videos
            "DOCS" -> com.ismartcoding.plain.platform.system.DataType.DOC to Res.string.docs
            else -> error("Unsupported media fixture")
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            awaitNode("main navigation") { it.contentDescription == text(Res.string.chat) }
            val peer = checkNotNull(GraphqlApi.execute(Query.peers()).find { it.id == peerId })
            val source = com.ismartcoding.plain.rust.graphql.ContentSource.Remote(com.ismartcoding.plain.features.chat.peer.ManagedDeviceClient.load(peer))
            val query = "text:$prefix trash:false"
            report("remote_media_stage", feature + "_catalog")
            val rows = awaitValue("owned target media scan") {
                source.catalog.rows(type, query, 10, 0, FileSortBy.NAME_ASC).takeIf { it.size == 1 }
            }
            val selectedId = rows.single().id
            val (path, itemTitle) = when (val row = rows.single()) {
                is com.ismartcoding.plain.models.Image -> row.path to row.title
                is com.ismartcoding.plain.models.Video -> row.path to row.title
                is com.ismartcoding.plain.models.Doc -> row.path to row.title
                else -> error("Unexpected media type")
            }
            assertTrue(path.substringAfterLast('/').startsWith(prefix))
            assertTrue(com.ismartcoding.plain.features.media.MediaCatalogClient.rows(type, query, 10, 0, FileSortBy.NAME_ASC).isEmpty())
            assertFalse(java.io.File(path).exists())
            openRemoteTools(peer.name)
            click(text(title), "selected remote media page")
            click(text(Res.string.search), "remote media search")
            val field = awaitNode("remote media search input") { it.isEditable }
            assertTrue(field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, prefix)
            }))
            val currentField = awaitNode("updated media search input") { it.isEditable && it.text == prefix }
            instrumentation.runOnMainSync {
                check(currentField.semantic.config.getOrNull(SemanticsActions.OnImeAction)?.action?.invoke() == true)
            }
            report("remote_media_stage", feature + "_open")
            if (feature == "DOCS") {
                click(itemTitle, "owned remote document")
                awaitNode("downloaded target document text") { it.text?.contains("Target document $prefix") == true }
            } else {
                awaitNode("owned target thumbnail") { it.contentDescription == path }
                if (feature == "IMAGES") assertNoteImageRendered(scenario, colorTolerance = 4)
                report("remote_media_stage", feature + "_thumbnail_rendered")
                var targetAction = awaitNode("settled target thumbnail") { it.contentDescription == path }
                while (!targetAction.isClickable) targetAction = checkNotNull(targetAction.parent)
                assertTrue(targetAction.performAction(AccessibilityNodeInfo.ACTION_CLICK))
                awaitValue("preview retains target provenance") {
                    com.ismartcoding.plain.ui.media.MediaPreviewData.items.find { it.id == selectedId }?.takeIf {
                        it.source is com.ismartcoding.plain.rust.graphql.ContentSource.Remote && it.source.key == source.key && it.path == path
                    }
                }
                assertNoteImageRendered(scenario, colorTolerance = 4)
                if (feature == "VIDEOS") {
                    val historyQuery = "type:VIDEO ids:=$selectedId"
                    awaitValue("target video actually playing") { source.api.execute(Query.mediaPlayHistory(historyQuery, 0, 10)).firstOrNull { it.mediaId == selectedId } }
                    assertTrue(GraphqlApi.execute(Query.mediaPlayHistory(historyQuery, 0, 10)).isEmpty())
                }
            }
            assertTrue(com.ismartcoding.plain.features.media.MediaCatalogClient.rows(type, query, 10, 0, FileSortBy.NAME_ASC).isEmpty())
            report("remote_media_page", feature + "_target_catalog_existing_page_and_preview_verified")
        }
    }

    @Test
    fun managesOwnedRemoteFilesWithoutWritingLocalContent() = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use {
            val targetId = checkNotNull(InstrumentationRegistry.getArguments().getString("managedPeerId"))
            val peer = checkNotNull(GraphqlApi.execute(Query.peers()).find { it.id == targetId })
            val remote = GraphqlApi.forPeer(peer)
            val pathsRequest = GraphqlRequest<ManagedAcceptancePaths>("query { app { appDir deviceName } }", field = "app")
            val target = remote.execute(pathsRequest)
            val local = GraphqlApi.execute(pathsRequest)
            val name = "000-managed-ui-${UUID.randomUUID()}"
            val directory = target.appDir.trimEnd('/') + "/" + name
            val fileName = "original.txt"
            val path = "$directory/$fileName"
            val initial = "Initial $name"
            val edited = "Edited $name"
            suspend fun entries(root: String, query: String = "") = remote.execute(Query.files(root, query, FileSortBy.NAME_ASC, 0, 1000))
            report("file_stage", "create_directory")
            remote.execute(Mutation.fileWrite(FileWriteAction.CREATE_DIRECTORY, directory, "", false))
            try {
                report("file_stage", "seed_text")
                remote.execute(Mutation.fileWrite(FileWriteAction.WRITE_TEXT, path, initial, false))
                assertTrue(GraphqlApi.execute(Query.files(local.appDir, "", FileSortBy.NAME_ASC, 0, 1000)).none { it.name == name })
                openRemoteTools(peer.name)
                report("file_stage", "open_remote_root")
                click(text(Res.string.files), "remote files")
                click(text(Res.string.folders), "remote storage drawer")
                selectDrawerItem(text(Res.string.app_data), "target app storage")
                click(name, "owned remote directory")
                click(fileName, "remote text file")
                click(text(Res.string.edit), "edit downloaded target text")
                val editor = awaitNode("downloaded remote text") { it.isEditable && it.text == initial }
                assertTrue(editor.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, edited)
                }))
                click(text(Res.string.save), "save remote text")
                awaitValue("target file updated size") { entries(directory).find { it.name == fileName && it.size == edited.encodeToByteArray().size.toLong() } }
                awaitValue("remote editor save finished") { if (!com.ismartcoding.plain.ui.components.DialogHelper.loading.value) true else null }
                click(text(Res.string.close), "leave saved edit mode")
                leaveTextFileViewer("return to target directory")
                click(fileName, "reopen remote text")
                click(text(Res.string.edit), "verify fresh target text in existing editor")
                awaitNode("fresh remote content after save") { it.isEditable && it.text == edited }
                click(text(Res.string.close), "leave unchanged edit mode")
                leaveTextFileViewer("return to remote files")
                openActions(fileName)
                report("file_stage", "rename_dialog")
                click(text(Res.string.rename), "remote file rename")
                val rename = awaitNode("remote rename input") { it.isEditable && it.text == fileName }
                assertTrue(rename.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "renamed.txt")
                }))
                click(text(Res.string.save), "confirm remote rename")
                awaitValue("renamed target file") { entries(directory).find { it.name == "renamed.txt" } }
                awaitNode("renamed file row") { it.text == "renamed.txt" }
                openActions("renamed.txt")
                click(text(Res.string.delete), "delete remote file")
                click(text(Res.string.confirm), "confirm target file deletion")
                awaitValue("deleted target file") { if (entries(directory).isEmpty()) true else null }
                click(text(Res.string.more), "target file actions")
                click(text(Res.string.create_folder), "create remote folder")
                val folder = awaitNode("remote folder input") { it.isEditable }
                assertTrue(folder.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "child")
                }))
                click(text(Res.string.confirm), "confirm remote folder")
                awaitValue("created target child directory") { entries(directory).find { it.name == "child" && it.isDir } }
                click(text(Res.string.app_data), "return to remote root breadcrumb")
                awaitNode("owned directory row") { it.text == name }
                openActions(name)
                click(text(Res.string.delete), "delete owned remote directory")
                click(text(Res.string.confirm), "confirm owned directory deletion")
                awaitValue("removed target directory") { if (entries(target.appDir).none { it.path == directory }) true else null }
                assertTrue(GraphqlApi.execute(Query.files(local.appDir, "", FileSortBy.NAME_ASC, 0, 1000)).none { it.name == name })
                report("remote_files_result", "read_edit_reopen_rename_create_delete_verified_local_absent")
            } catch (error: Throwable) {
                report("file_ui_failure", uiNodes().mapNotNull { it.text ?: it.contentDescription }.joinToString(" | "))
                throw error
            } finally {
                if (entries(target.appDir).any { it.path == directory }) remote.execute(Mutation.deleteFiles(listOf(directory)))
                assertTrue(entries(target.appDir).none { it.path == directory })
            }
        }
    }

    private fun text(resource: org.jetbrains.compose.resources.StringResource) = LocaleHelper.getString(resource)

    private fun installGlobalSearchFixture(
        scenario: ActivityScenario<MainActivity>,
        remote: com.ismartcoding.plain.rust.graphql.ContentSource.Remote,
        domain: com.ismartcoding.plain.ui.search.GlobalSearchDomain,
        query: String,
    ): com.ismartcoding.plain.ui.search.GlobalSearchViewModel {
        lateinit var model: com.ismartcoding.plain.ui.search.GlobalSearchViewModel
        scenario.onActivity { activity ->
            model = com.ismartcoding.plain.ui.search.GlobalSearchViewModel(readDevices = {
                com.ismartcoding.plain.ui.search.GlobalSearchDevices(listOf(com.ismartcoding.plain.rust.graphql.ContentSource.Local, remote))
            })
            model.domain.value = domain
            model.queryText.value = query
            model.submitted.value = true
            activity.viewModelStore.put("globalSearchVM", model)
        }
        scenario.recreate()
        scenario.onActivity { model.search() }
        return model
    }

    private fun awaitSearchAudioAction(title: String, description: String): UiNode {
        var action = awaitNode("audio row action $description") { candidate ->
            if (candidate.contentDescription != description) false else {
                var parent = candidate.parent
                var matches = false
                while (parent != null) {
                    val texts = nodes(parent).mapNotNull { it.text }
                    if (texts.isNotEmpty()) { matches = title in texts; break }
                    parent = parent.parent
                }
                matches
            }
        }
        while (!action.isClickable) action = checkNotNull(action.parent)
        return action
    }

    private fun searchResultNode(marker: String): UiNode = awaitNode("chat search snippet") { candidate ->
        var current: UiNode? = candidate
        var editable = false
        while (current != null) {
            if (current.isEditable) editable = true
            current = current.parent
        }
        candidate.text == marker && !editable
    }

    private suspend fun assertGlobalSearchChatGlyph(scenario: ActivityScenario<MainActivity>, marker: String) {
        awaitValue("chat search glyph pixels") {
            var row = searchResultNode(marker)
            while (!row.isClickable) row = checkNotNull(row.parent)
            val density = instrumentation.targetContext.resources.displayMetrics.density
            val bounds = row.boundsInWindow.translate(row.windowOffset)
            val left = (bounds.left + 24 * density).toInt()
            val top = (bounds.top + 16 * density).toInt()
            val size = (24 * density).toInt()
            val capture = kotlinx.coroutines.CompletableDeferred<android.graphics.Bitmap>()
            scenario.onActivity { activity ->
                val decor = activity.window.decorView
                val bitmap = android.graphics.Bitmap.createBitmap(decor.width, decor.height, android.graphics.Bitmap.Config.ARGB_8888)
                android.view.PixelCopy.request(activity.window, bitmap, { result ->
                    if (result == android.view.PixelCopy.SUCCESS) capture.complete(bitmap)
                    else { bitmap.recycle(); capture.completeExceptionally(AssertionError("Window pixel copy failed: $result")) }
                }, android.os.Handler(android.os.Looper.getMainLooper()))
            }
            val bitmap = kotlinx.coroutines.withTimeout(5000) { capture.await() }
            val background = bitmap.getPixel(left + 1, top + 1)
            var ink = 0
            for (y in top until top + size) for (x in left until left + size) {
                val pixel = bitmap.getPixel(x, y)
                if (listOf(0, 8, 16).any { shift -> kotlin.math.abs((pixel shr shift and 255) - (background shr shift and 255)) > 32 }) ink++
            }
            bitmap.recycle()
            if (ink >= 24) true else null
        }
    }

    private fun nodes(root: UiNode): List<UiNode> = listOf(root) + root.children.flatMap { nodes(it) }

    private inner class UiNode(val semantic: SemanticsNode, val parent: UiNode?, val windowOffset: androidx.compose.ui.geometry.Offset, val overlay: Boolean) {
        val boundsInWindow = semantic.boundsInWindow
        val screenCenter get() = boundsInWindow.center + windowOffset
        val isVisible: Boolean get() {
            val bounds = boundsInWindow.translate(windowOffset)
            val display = instrumentation.targetContext.resources.displayMetrics
            return !bounds.isEmpty && bounds.right > 0 && bounds.bottom > 0 &&
                bounds.left < display.widthPixels && bounds.top < display.heightPixels
        }
        val text = semantic.config.getOrNull(SemanticsProperties.EditableText)?.text
            ?: semantic.config.getOrNull(SemanticsProperties.Text)?.joinToString("\n") { it.text }
        val contentDescription = semantic.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString("\n")
        val isEnabled = semantic.config.getOrNull(SemanticsProperties.Disabled) == null
        val isEditable = semantic.config.getOrNull(SemanticsActions.SetText) != null
        val isLongClickable = semantic.config.getOrNull(SemanticsActions.OnLongClick) != null
        val isClickable = semantic.config.getOrNull(SemanticsActions.OnClick) != null
        val isCheckable = semantic.config.getOrNull(SemanticsProperties.Selected) != null || semantic.config.getOrNull(SemanticsProperties.ToggleableState) != null
        val isChecked = semantic.config.getOrNull(SemanticsProperties.Selected) == true || semantic.config.getOrNull(SemanticsProperties.ToggleableState) == ToggleableState.On
        val children: List<UiNode> = semantic.children.map { UiNode(it, this, windowOffset, overlay) }
        fun performAction(action: Int, args: Bundle? = null): Boolean {
            var result = false
            instrumentation.runOnMainSync {
                result = when (action) {
                    AccessibilityNodeInfo.ACTION_LONG_CLICK -> semantic.config.getOrNull(SemanticsActions.OnLongClick)?.action?.invoke() == true
                    AccessibilityNodeInfo.ACTION_CLICK -> semantic.config.getOrNull(SemanticsActions.OnClick)?.action?.invoke() == true
                    AccessibilityNodeInfo.ACTION_SET_TEXT -> semantic.config.getOrNull(SemanticsActions.SetText)?.action?.invoke(AnnotatedString(args?.getCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE).toString())) == true
                    else -> false
                }
            }
            return result
        }
    }

    private fun currentNodes(): List<UiNode> {
        fun roots(view: View): List<ViewRootForTest> = when (view) {
            is ViewRootForTest -> listOf(view)
            is ViewGroup -> (0 until view.childCount).flatMap { roots(view.getChildAt(it)) }
            else -> emptyList()
        }
        val window = WindowInspector.getGlobalWindowViews().asReversed().filter { it.isShown }.sortedByDescending { it.hasWindowFocus() }
            .firstOrNull { roots(it).isNotEmpty() } ?: return emptyList()
        val screen = IntArray(2)
        val inside = IntArray(2)
        window.getLocationOnScreen(screen)
        window.getLocationInWindow(inside)
        val offset = androidx.compose.ui.geometry.Offset((screen[0] - inside[0]).toFloat(), (screen[1] - inside[1]).toFloat())
        val overlay = (window.layoutParams as android.view.WindowManager.LayoutParams).type != android.view.WindowManager.LayoutParams.TYPE_BASE_APPLICATION
        return roots(window).flatMap { nodes(UiNode(it.semanticsOwner.unmergedRootSemanticsNode, null, offset, overlay)) }
    }

    private fun uiNodes(): List<UiNode> {
        var result = emptyList<UiNode>()
        instrumentation.runOnMainSync { result = currentNodes() }
        return result
    }

    private fun awaitNode(role: String, predicate: (UiNode) -> Boolean): UiNode {
        val deadline = System.nanoTime() + 30_000_000_000L
        while (System.nanoTime() < deadline) {
            uiNodes().firstOrNull { it.isVisible && predicate(it) }?.let { return it }
            Thread.sleep(100)
        }
        throw AssertionError("Timed out waiting for $role; visible text: ${allText()}")
    }

    private suspend fun awaitMainWindowFocus(scenario: ActivityScenario<MainActivity>) {
        awaitValue("main window focus after dialog dismissal") {
            var focused = false
            scenario.onActivity { focused = it.window.decorView.hasWindowFocus() }
            if (focused) true else null
        }
    }

    private fun allText(): List<String> = uiNodes().filter { it.isVisible }.mapNotNull { it.text }

    private fun openNotesSidebar() {
        val title = awaitNode("notes toolbar") {
            (it.text == text(Res.string.notes) || it.text == text(Res.string.notes) + " - " + text(Res.string.trash)) &&
                it.isVisible
        }
        val bounds = title.boundsInWindow
        val button = uiNodes().filter {
            it.isClickable && it.isEnabled && it.boundsInWindow.center.y in bounds.top..bounds.bottom &&
                nodes(it).drop(1).none { child -> child.isClickable }
        }.minByOrNull { it.boundsInWindow.left } ?: error("Notes sidebar button unavailable")
        performClick(button, "notes sidebar")
    }

    private suspend fun selectDrawerItem(value: String, role: String) {
        var action = awaitNode(role) { node ->
            node.text == value && generateSequence(node) { it.parent }.any {
                it.semantic.config.getOrNull(SemanticsProperties.PaneTitle) != null
            }
        }
        while (!action.isClickable) action = checkNotNull(action.parent)
        assertTrue(action.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        awaitValue("closed navigation drawer") {
            if (uiNodes().none { pane ->
                pane.isVisible && pane.semantic.config.getOrNull(SemanticsProperties.PaneTitle) != null &&
                    nodes(pane).any { it.text == value }
            }) true else null
        }
    }

    private suspend fun leaveTextFileViewer(role: String) {
        val action = awaitNode("text file viewer edit action") { it.contentDescription == text(Res.string.edit) }
        click(text(Res.string.back), role)
        awaitValue("removed text file viewer") {
            if (uiNodes().none { it.semantic.id == action.semantic.id }) true else null
        }
    }

    private fun openActions(title: String) {
        fun longClickable(node: UiNode): Boolean = node.isLongClickable || node.parent?.let { longClickable(it) } == true
        var row = awaitNode("owned content row") { it.text == title && longClickable(it) }
        while (!row.isLongClickable) row = checkNotNull(row.parent)
        assertTrue(row.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK))
    }

    private fun touch(x: Float, y: Float, holdMillis: Long = 0) {
        val down = android.os.SystemClock.uptimeMillis()
        val press = android.view.MotionEvent.obtain(down, down, android.view.MotionEvent.ACTION_DOWN, x, y, 0)
        instrumentation.sendPointerSync(press)
        press.recycle()
        if (holdMillis > 0) Thread.sleep(holdMillis)
        val release = android.view.MotionEvent.obtain(down, android.os.SystemClock.uptimeMillis(), android.view.MotionEvent.ACTION_UP, x, y, 0)
        instrumentation.sendPointerSync(release)
        release.recycle()
        instrumentation.waitForIdleSync()
    }

    private fun performClick(start: UiNode, role: String) {
        var node = start
        while (!node.isClickable && node.parent != null) node = checkNotNull(node.parent)
        assertTrue("Disabled action $role", node.isEnabled)
        if (node.overlay) {
            assertTrue(node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        } else {
            val point = node.screenCenter
            touch(point.x, point.y)
        }
    }

    private fun click(value: String, role: String, firstMatch: Boolean = false, onClick: (() -> Unit)? = null) {
        fun clickable(node: UiNode): Boolean = if (node.isClickable) node.isEnabled else node.parent?.let { clickable(it) } == true
        val deadline = System.nanoTime() + 30_000_000_000L
        var previousPoint: androidx.compose.ui.geometry.Offset? = null
        var previousNodeId: Int? = null
        while (System.nanoTime() < deadline) {
            var nodeId: Int? = null
            var point: androidx.compose.ui.geometry.Offset? = null
            var activated = false
            instrumentation.runOnMainSync {
                val candidates = currentNodes().filter { (it.text?.contains(value) == true || it.contentDescription == value) && clickable(it) && it.isVisible }
                val exact = candidates.filter { it.text == value || it.contentDescription == value }
                val matches = exact.ifEmpty { candidates }
                if (matches.isNotEmpty()) {
                    var target = if (firstMatch) matches.first() else matches.last()
                    if (target.overlay) {
                        while (!target.isClickable) target = checkNotNull(target.parent)
                        onClick?.invoke()
                        activated = target.semantic.config.getOrNull(SemanticsActions.OnClick)?.action?.invoke() == true
                    } else {
                        point = target.screenCenter
                        nodeId = target.semantic.id
                    }
                }
            }
            if (activated) return
            if (point != null && point == previousPoint && nodeId == previousNodeId) {
                onClick?.invoke()
                touch(point!!.x, point!!.y)
                return
            }
            previousPoint = point
            previousNodeId = nodeId
            Thread.sleep(100)
        }
        throw AssertionError("Timed out waiting for $role; visible text: ${allText()}")
    }
}
