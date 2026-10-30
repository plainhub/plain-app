package com.ismartcoding.plain.tests

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.ismartcoding.plain.MainActivity
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.features.audio.*
import com.ismartcoding.plain.features.files.FileSortBy
import com.ismartcoding.plain.features.media.MediaCatalogClient
import com.ismartcoding.plain.models.AudioCommand
import com.ismartcoding.plain.preferences.PrefKeys
import com.ismartcoding.plain.ui.audio.AudioArtistPage
import com.ismartcoding.plain.ui.audio.AudioQueueViewModel
import com.ismartcoding.plain.ui.audio.AudioViewModel
import com.ismartcoding.plain.ui.casting.CastViewModel
import com.ismartcoding.plain.ui.components.DialogHelper
import com.ismartcoding.plain.ui.files.MediaFoldersViewModel
import com.ismartcoding.plain.ui.navigation.MainDialogs
import com.ismartcoding.plain.ui.tags.TagsViewModel
import com.ismartcoding.plain.ui.theme.AppTheme
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.play_all
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.jetbrains.compose.resources.getString
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AudioPlaybackDeviceTest {
    @get:Rule val activity = ActivityTestRule(MainActivity::class.java)
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val store = ViewModelStore()
    private val models = ViewModelProvider(store, ViewModelProvider.NewInstanceFactory())
    private val uris = mutableListOf<Uri>()

    @Test
    fun artistOrderAndPermissionDecisionsUseTheRealPlayer() = runBlocking {
        instrumentation.uiAutomation.serviceInfo = instrumentation.uiAutomation.serviceInfo.apply {
            flags = flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }
        assertTrue("Use an isolated test package", appContext.packageName.endsWith(".mediatest"))
        assertNotEquals(PackageManager.PERMISSION_GRANTED, appContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS))
        assertTrue(AudioQueueClient.queuedMediaIds().isEmpty())
        val artist = "Plain fixture ${UUID.randomUUID().toString().take(8)}"
        try {
            createAudio(artist, "Alpha")
            createAudio(artist, "Zulu")
            android.util.Log.i("PlainMediaDeviceTest", "Waiting for artist metadata")
            val tracks = withTimeout(15_000) {
                var found = MediaCatalogClient.artistTracks(artist, FileSortBy.NAME_DESC)
                while (found.size != 2) {
                    delay(200)
                    found = MediaCatalogClient.artistTracks(artist, FileSortBy.NAME_DESC)
                }
                found
            }
            assertEquals(listOf("Zulu", "Alpha"), tracks.map { it.title })
            android.util.Log.i("PlainMediaDeviceTest", "Artist metadata ready; starting old track")
            Prefs.set(PrefKeys.AUDIO_SORT_BY, FileSortBy.NAME_ASC.name)
            val old = tracks.last().toPlaylistAudio()
            AudioQueueClient.enqueue(listOf(old))
            AudioCommands.play(old)
            waitFor { AudioQueueClient.playback().isPlaying && AudioQueueClient.playback().positionMs > 100 }
            val original = AudioQueueClient.playback()
            android.util.Log.i("PlainMediaDeviceTest", "Old track is playing; rendering artist page")
            lateinit var queue: AudioQueueViewModel
            onMain {
                queue = models[AudioQueueViewModel::class.java]
                val audio = models[AudioViewModel::class.java]
                audio.sortBy.value = FileSortBy.NAME_DESC
                activity.activity.setContent {
                    AppTheme(useDarkTheme = false) {
                        AudioArtistPage(rememberNavController(), artist, queue, audio,
                            models[TagsViewModel::class.java], models[MediaFoldersViewModel::class.java], models[CastViewModel::class.java])
                        MainDialogs()
                    }
                }
            }
            val playAll = getString(Res.string.play_all)
            waitFor { onMainValue { text(playAll) != null } }
            android.util.Log.i("PlainMediaDeviceTest", "Artist page ready; cancelling permission explanation")
            screenshot("artist")
            clickText(playAll)
            waitFor { onMainValue { DialogHelper.confirmation.value != null } }
            screenshot("permission-explanation")
            instrumentation.uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
            waitFor { onMainValue { DialogHelper.confirmation.value == null } }
            assertPreserved(original.mediaId, original.positionMs)

            android.util.Log.i("PlainMediaDeviceTest", "Cancellation preserved playback; denying system permission")

            clickText(playAll)
            waitFor { onMainValue { DialogHelper.confirmation.value != null } }
            clickText(onMainValue { DialogHelper.confirmation.value!!.confirmButton.first })
            clickSystemPermission("permission_deny_button")
            delay(500)
            assertPreserved(original.mediaId, original.positionMs)

            android.util.Log.i("PlainMediaDeviceTest", "Denial preserved playback; allowing system permission")

            clickText(playAll)
            waitFor { onMainValue { DialogHelper.confirmation.value != null } }
            clickText(onMainValue { DialogHelper.confirmation.value!!.confirmButton.first })
            allowNotifications()
            android.util.Log.i("PlainMediaDeviceTest", "Notification permission granted; waiting for the new track")
            waitFor { AudioQueueClient.playback().mediaId == tracks.first().id && AudioQueueClient.playback().isPlaying && AudioQueueClient.playback().positionMs > 100 }
            assertEquals(tracks.map { it.id }, AudioQueueClient.queuePage(0, 10).map { it.mediaId })
            assertEquals(tracks.first().id, onMainValue { queue.selectedMediaId.value })
            screenshot("playing-in-page-order")

            val playlist = AudioPlaylistsClient.createPlaylist("Plain order fixture")
            try {
                AudioPlaylistsClient.addPlaylistItems(playlist.id, tracks.reversed().map { it.toPlaylistAudio() })
                val selected = AudioQueueClient.replace("playlist_id:${playlist.id}", FileSortBy.NAME_DESC)
                assertEquals(tracks.last().id, selected!!.mediaId)
                assertEquals(tracks.reversed().map { it.id }, AudioQueueClient.queuePage(0, 10).map { it.mediaId })
            } finally {
                AudioPlaylistsClient.deletePlaylist(playlist.id)
            }
        } finally {
            AudioCommands.command(AudioCommand.CLEAR)
            AudioQueueClient.clearQueue()
            onMain { store.clear(); DialogHelper.confirmation.value = null }
            uris.forEach { appContext.contentResolver.delete(it, null, null) }
        }
    }

    private suspend fun assertPreserved(id: String?, position: Long) {
        val playback = AudioQueueClient.playback()
        assertEquals(id, playback.mediaId)
        assertTrue("Old track must remain playing: $playback", playback.isPlaying)
        assertTrue(playback.positionMs >= position)
        assertEquals(listOf(id), AudioQueueClient.queuePage(0, 10).map { it.mediaId })
        waitFor { AudioQueueClient.playback().positionMs > playback.positionMs }
    }

    private fun createAudio(artist: String, title: String) {
        fun tag(name: String, value: String): ByteArray {
            val text = byteArrayOf(0) + value.toByteArray()
            return name.toByteArray() + ByteBuffer.allocate(4).putInt(text.size).array() + byteArrayOf(0, 0) + text
        }
        val tags = tag("TPE1", artist) + tag("TIT2", title)
        val tagSize = byteArrayOf((tags.size shr 21 and 127).toByte(), (tags.size shr 14 and 127).toByte(),
            (tags.size shr 7 and 127).toByte(), (tags.size and 127).toByte())
        val header = "ID3".toByteArray() + byteArrayOf(3, 0, 0) + tagSize + tags
        val fixture = File(InstrumentationRegistry.getArguments().getString("mp3Fixture")!!).readBytes()
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$artist-$title.mp3")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Music/PlainMediaDeviceTests")
            put(MediaStore.MediaColumns.MIME_TYPE, "audio/mpeg")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val resolver = appContext.contentResolver
        val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)!!
        uris.add(uri)
        resolver.openOutputStream(uri)!!.use { output ->
            output.write(header)
            repeat(120) { output.write(fixture) }
        }
        values.clear()
        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        values.put(MediaStore.Audio.Media.ARTIST, artist)
        values.put(MediaStore.Audio.Media.TITLE, title)
        assertEquals(1, resolver.update(uri, values, null, null))
        resolver.query(uri, arrayOf(MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.TITLE), null, null, null)!!.use {
            check(it.moveToFirst())
            android.util.Log.i("PlainMediaDeviceTest", "Fixture metadata: ${it.getString(0)} / ${it.getString(1)}")
        }
    }

    private suspend fun waitFor(condition: suspend () -> Boolean) = withTimeout(15_000) {
        while (!condition()) delay(100)
    }

    private suspend fun clickText(value: String) {
        waitFor { onMainValue { text(value) != null } }
        onMain {
            var node = text(value) ?: error("Missing text: $value")
            while (node.config.getOrNull(SemanticsActions.OnClick) == null) node = node.parent ?: error("Missing click action: $value")
            check(node.config.getOrNull(SemanticsActions.OnClick)!!.action!!.invoke())
        }
    }

    private suspend fun clickSystemPermission(id: String) {
        waitFor {
            fun find(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
                if (node == null) return null
                if (node.viewIdResourceName?.endsWith(":id/$id") == true) return node
                return (0 until node.childCount).firstNotNullOfOrNull { find(node.getChild(it)) }
            }
            find(instrumentation.uiAutomation.rootInActiveWindow)?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
        }
    }

    private suspend fun allowNotifications() {
        waitFor {
            val root = instrumentation.uiAutomation.rootInActiveWindow ?: return@waitFor false
            val allow = root.findAccessibilityNodeInfosByViewId("com.android.permissioncontroller:id/permission_allow_button").firstOrNull()
            if (allow != null) return@waitFor allow.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (root.packageName?.toString() != "com.android.settings") return@waitFor false
            val toggle = root.findAccessibilityNodeInfosByViewId("com.android.settings:id/switch_widget").firstOrNull() ?: return@waitFor false
            delay(1_000)
            if (!toggle.isChecked) {
                var target = toggle
                while (!target.isClickable) target = target.parent ?: return@waitFor false
                if (!target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return@waitFor false
            }
            waitFor { appContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED }
            instrumentation.uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
        }
    }

    private fun text(value: String): SemanticsNode? {
        fun roots(view: View): List<ViewRootForTest> = when (view) {
            is ViewRootForTest -> listOf(view)
            is ViewGroup -> (0 until view.childCount).flatMap { roots(view.getChildAt(it)) }
            else -> emptyList()
        }
        fun descendants(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::descendants)
        return android.view.inspector.WindowInspector.getGlobalWindowViews().asReversed()
            .filter { it.isAttachedToWindow && it.isShown }.flatMap(::roots)
            .flatMap { descendants(it.semanticsOwner.unmergedRootSemanticsNode) }
            .firstOrNull { it.config.getOrNull(SemanticsProperties.Text)?.any { part -> part.text == value } == true }
    }

    private fun screenshot(name: String) {
        instrumentation.waitForIdleSync()
        val drawn = java.util.concurrent.CountDownLatch(1)
        onMain {
            val view = activity.activity.window.decorView
            view.postOnAnimation { view.postOnAnimation { drawn.countDown() } }
        }
        check(drawn.await(5, java.util.concurrent.TimeUnit.SECONDS))
        Thread.sleep(300)
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(appContext.cacheDir, "media-$name.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    private fun onMain(block: () -> Unit) = instrumentation.runOnMainSync(block)
    private fun <T> onMainValue(block: () -> T): T {
        var value: T? = null
        onMain { value = block() }
        return value!!
    }
}
