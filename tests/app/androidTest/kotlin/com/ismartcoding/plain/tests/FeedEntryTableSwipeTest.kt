package com.ismartcoding.plain.tests

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inspector.WindowInspector
import androidx.activity.compose.setContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.getOrNull
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ismartcoding.plain.MainActivity
import com.ismartcoding.plain.features.chat.peer.ManagedDeviceClient
import com.ismartcoding.plain.features.nearby.PeerStatus
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.models.FeedEntry
import com.ismartcoding.plain.models.ManagedDeviceInfo
import com.ismartcoding.plain.models.Peer
import com.ismartcoding.plain.platform.system.DeviceType
import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.rust.graphql.GraphqlResponse
import com.ismartcoding.plain.rust.graphql.PeerHttpClient
import com.ismartcoding.plain.ui.feeds.FeedEntryPage
import com.ismartcoding.plain.ui.feeds.FeedEntryPagerViewModel
import com.ismartcoding.plain.ui.feeds.FeedEntryViewModel
import com.ismartcoding.plain.ui.tags.TagsViewModel
import com.ismartcoding.plain.ui.theme.AppTheme
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Instant

@RunWith(AndroidJUnit4::class)
class FeedEntryTableSwipeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun wideTableKeepsHorizontalGesturesWhileTheRestOfTheArticlePages() {
        val current = FeedEntry(
            id = "current", title = "Article with a table", feedId = "fixture-feed", read = true,
            content = """
                Article text outside the table.

                | One | Two | Three | Four | Five | Six |
                | --- | --- | --- | --- | --- | --- |
                | A   | B   | C     | D    | E    | F   |
            """.trimIndent(),
        )
        val previous = FeedEntry(id = "previous", title = "Previous article", feedId = current.feedId, read = true)
        val next = FeedEntry(id = "next", title = "Next article", feedId = current.feedId, read = true)
        val entries = listOf(previous, current, next)
        val source = runBlocking { fixtureSource(entries) }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var article: FeedEntryViewModel
            scenario.onActivity { activity ->
                article = FeedEntryViewModel(source)
                val pager = FeedEntryPagerViewModel(source).apply { setup(entries.map { it.id }) }
                val tags = TagsViewModel()
                activity.setContent {
                    AppTheme(useDarkTheme = false) {
                        FeedEntryPage(rememberNavController(), current.id, tags, pager, source, article)
                    }
                }
            }
            val bounds = await("scrollable markdown table") {
                tableScrollNode()?.let { node ->
                    val range = node.config[SemanticsProperties.HorizontalScrollAxisRange]
                    node.boundsInWindow.takeIf { !it.isEmpty && range.maxValue() > 0 }
                }
            }
            scrollTableBy(bounds.width * 4)
            await("table at its right edge") {
                tableScrollRange()?.let { range -> true.takeIf { range.value() == range.maxValue() } }
            }

            swipe(bounds, bounds.center.y, left = true)
            instrumentation.runOnMainSync {
                assertEquals("Swiping at the right table edge switched articles", current.id, article.item.value?.id)
            }

            swipe(bounds, bounds.center.y, left = false)
            await("table scrolled back") {
                tableScrollRange()?.let { range -> true.takeIf { range.value() < range.maxValue() } }
            }
            scrollTableBy(-bounds.width * 4)
            await("table at its left edge") {
                tableScrollRange()?.let { range -> true.takeIf { range.value() == 0f } }
            }
            swipe(bounds, bounds.center.y, left = false)
            instrumentation.runOnMainSync {
                assertEquals("Swiping at the left table edge switched articles", current.id, article.item.value?.id)
            }

            val outsideY = await("article paragraph above the table") {
                nodes().firstOrNull {
                    it.config.getOrNull(SemanticsProperties.Text)?.any { text ->
                        text.text == "Article text outside the table."
                    } == true
                }?.boundsInWindow?.center?.y
            }
            swipe(bounds, outsideY, left = true)
            await("next article after swiping outside the table") {
                true.takeIf { article.item.value?.id == next.id }
            }
        }
    }

    private suspend fun fixtureSource(entries: List<FeedEntry>): ContentSource.Remote {
        val now = Instant.fromEpochMilliseconds(0)
        val peer = Peer(
            id = "feed-fixture", name = "Feed fixture", ip = "device.test", port = 8443,
            status = PeerStatus.PAIRED, deviceType = DeviceType.PHONE,
            createdAt = now, updatedAt = now, publicKey = "fixture-key", token = "fixture-token",
        )
        val info = ManagedDeviceInfo(peer.name, emptyList(), emptyList(), "/fixture", "/downloads", "fixture-token", null)
        val http = PeerHttpClient(
            peer, peers = { listOf(peer) }, clientId = { "fixture-client" },
            executeGraphql = { _, operation ->
                val (field, value) = when {
                    "app {" in operation.query -> "app" to JsonHelper.jsonEncodeToElement(info)
                    "feed(" in operation.query -> "feed" to JsonNull
                    "feedEntry(" in operation.query -> {
                        val id = operation.variables.getValue("id").jsonPrimitive.content
                        "feedEntry" to JsonHelper.jsonEncodeToElement(entries.single { it.id == id })
                    }
                    else -> error("Unexpected feed fixture query: ${operation.query}")
                }
                GraphqlResponse(JsonObject(mapOf(field to value)))
            },
        )
        return ContentSource.Remote(ManagedDeviceClient.load(peer, http, updates = { emptyFlow() }))
    }

    private fun tableScrollNode(): SemanticsNode? {
        val table = nodes().firstOrNull { it.config.getOrNull(SemanticsProperties.CollectionInfo)?.columnCount == 6 }
        return table?.children?.firstOrNull { it.config.getOrNull(SemanticsProperties.HorizontalScrollAxisRange) != null }
    }

    private fun tableScrollRange(): ScrollAxisRange? =
        tableScrollNode()?.config?.getOrNull(SemanticsProperties.HorizontalScrollAxisRange)

    private fun scrollTableBy(distance: Float) = instrumentation.runOnMainSync {
        val scroll = checkNotNull(tableScrollNode()).config[SemanticsActions.ScrollBy]
        assertTrue(scroll.action?.invoke(distance, 0f) == true)
    }

    private fun nodes(): List<SemanticsNode> {
        fun roots(view: View): List<ViewRootForTest> = when (view) {
            is ViewRootForTest -> listOf(view)
            is ViewGroup -> (0 until view.childCount).flatMap { roots(view.getChildAt(it)) }
            else -> emptyList()
        }
        fun flatten(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::flatten)
        return WindowInspector.getGlobalWindowViews().filter { it.isShown }.flatMap(::roots)
            .flatMap { flatten(it.semanticsOwner.unmergedRootSemanticsNode) }
    }

    private fun swipe(bounds: Rect, y: Float, left: Boolean) {
        val start = if (left) 0.9f else 0.1f
        val end = 1f - start
        val down = SystemClock.uptimeMillis()
        for (step in 0..20) {
            val action = when (step) {
                0 -> MotionEvent.ACTION_DOWN
                20 -> MotionEvent.ACTION_UP
                else -> MotionEvent.ACTION_MOVE
            }
            val x = bounds.left + bounds.width * (start + (end - start) * step / 20)
            val event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, x, y, 0)
            event.source = InputDevice.SOURCE_TOUCHSCREEN
            try {
                instrumentation.sendPointerSync(event)
            } finally {
                event.recycle()
            }
            SystemClock.sleep(16)
        }
        instrumentation.waitForIdleSync()
    }

    private fun <T : Any> await(description: String, find: () -> T?): T {
        val deadline = SystemClock.uptimeMillis() + 5_000
        while (SystemClock.uptimeMillis() < deadline) {
            var result: T? = null
            instrumentation.runOnMainSync { result = find() }
            result?.let { return it }
            SystemClock.sleep(25)
        }
        error("Expected $description")
    }
}
