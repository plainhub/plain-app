package com.ismartcoding.plain.tests

import android.view.View
import android.view.ViewGroup
import com.ismartcoding.plain.MainActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.models.PairingRequestNotice
import com.ismartcoding.plain.models.NearbyDevice
import com.ismartcoding.plain.features.nearby.DiscoveryMethod
import com.ismartcoding.plain.features.nearby.PeerStatus
import com.ismartcoding.plain.features.nearby.RustPairingRuntime
import com.ismartcoding.plain.ui.nearby.PairPeerDialog
import com.ismartcoding.plain.ui.nearby.NearbyViewModel
import com.ismartcoding.plain.platform.system.DeviceType
import com.ismartcoding.plain.ui.nearby.PairingRequestPage
import com.ismartcoding.plain.ui.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class PairingRequestLayoutTest {
    @get:Rule
    val activityRule = ActivityTestRule(MainActivity::class.java)
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private var allow = ""
    private var deny = ""
    private var close = ""
    private var expired = ""
    private var density = 1f
    private val closed = AtomicInteger()
    private val code = "2048 4096"
    private var renderedScenario = ""

    @Before
    fun keepScreenOn() = onMain {
        activityRule.activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    @Test
    fun initiatorShowsWaitingImmediatelyAndCanCancelBeforeConfirmation() {
        val showing = androidx.compose.runtime.mutableStateOf(true)
        val peerId = "pairing-ui-${java.util.UUID.randomUUID()}"
        var pair = ""
        var waiting = ""
        var cancel = ""
        java.net.ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress()).use { server ->
            val device = NearbyDevice(
                id = peerId, name = "Pairing UI fixture", ips = listOf("127.0.0.1"), port = server.localPort,
                deviceType = DeviceType.TABLET, version = "fixture", platform = "", lastSeen = "2026-10-30T00:00:00Z",
                discoveryMethods = listOf(DiscoveryMethod.QR), bestIp = "127.0.0.1", status = PeerStatus.UNPAIRED,
            )
            try {
                onMain {
                    activityRule.activity.setContent {
                        pair = stringResource(Res.string.pair)
                        waiting = stringResource(Res.string.waiting_for_confirmation)
                        cancel = stringResource(Res.string.cancel)
                        AppTheme(useDarkTheme = false) {
                            if (showing.value) PairPeerDialog(device) { showing.value = false }
                        }
                    }
                }
                waitUntil(5_000) { pair.isNotEmpty() && text(pair) != null }
                clickText(pair)
                waitUntil(5_000) {
                    val cancelNode = text(cancel)
                    if (text(waiting) == null || cancelNode == null) false else {
                        assertNull(text(pair))
                        assertTrue(clickTarget(cancelNode).config.getOrNull(SemanticsActions.OnClick)!!.action!!.invoke())
                        true
                    }
                }
                waitUntil(5_000) { !showing.value }
            } finally {
                onMain {
                    NearbyViewModel.itemStatus.remove(peerId)
                }
            }
        }
    }

    private fun clickText(value: String) = onMain {
        val node = text(value)
        assertNotNull("Missing text '$value': ${nodes().map { it.config }}", node)
        val action = clickTarget(node!!).config.getOrNull(SemanticsActions.OnClick)?.action
        assertNotNull("Missing click action for '$value'", action)
        assertTrue(action!!.invoke())
    }

    @Test
    fun requestKeepsEqualBottomActionsAndReadableCodeAcrossThemesAndLargeText() {
        for ((name, dark, amoled) in listOf(Triple("light", false, false), Triple("dark", true, false), Triple("amoled", true, true))) {
            show(dark, amoled)
            screenshot(name)
            assertActions()
        }
        show(false, false, fontScale = 1.5f, deviceName = "Living room tablet with a long device name 客厅平板设备")
        assertActions()
        screenshot("large-text")
        show(false, false)
        waitUntil(95_000) { text(expired) != null }
        onMain {
            assertNull(text(allow))
            assertNull(text(deny))
            assertNotNull(text(close))
            clickTarget(text(close)!!).config.getOrNull(SemanticsActions.OnClick)!!.action!!.invoke()
        }
        assertEquals(1, closed.get())
        screenshot("expired")
    }

    private fun show(dark: Boolean, amoled: Boolean, fontScale: Float = 1f, deviceName: String = "Living room tablet") {
        val notice = PairingRequestNotice(
            fromId = "fixture-peer", fromName = deviceName, port = 2443, deviceType = DeviceType.TABLET,
            ecdhPublicKey = "", signaturePublicKey = "", timestamp = 0, ips = listOf("192.0.2.20"),
            signature = "fixture-request", fromIp = "192.0.2.20", awareSupported = false, verificationCode = code,
        )
        val scenario = java.util.UUID.randomUUID().toString()
        onMain {
            activityRule.activity.setContent {
                val base = LocalDensity.current
                density = base.density
                allow = stringResource(Res.string.allow)
                deny = stringResource(Res.string.deny)
                close = stringResource(Res.string.close)
                expired = stringResource(Res.string.pairing_request_expired)
                CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale)) {
                    AppTheme(useDarkTheme = dark, amoledDarkTheme = amoled) {
                        androidx.compose.runtime.key(notice, dark, amoled, fontScale) {
                            PairingRequestPage(notice) { closed.incrementAndGet() }
                            androidx.compose.runtime.SideEffect { renderedScenario = scenario }
                        }
                    }
                }
            }
        }
        waitUntil(5_000) { renderedScenario == scenario && text(allow) != null && text(code) != null }
    }

    private fun assertActions() = onMain {
        val allowBounds = clickTarget(text(allow)!!).boundsInRoot
        val denyBounds = clickTarget(text(deny)!!).boundsInRoot
        assertEquals(allowBounds.top, denyBounds.top, 1f)
        assertEquals(allowBounds.width, denyBounds.width, 1f)
        assertTrue(allowBounds.height >= 48 * density - 1)
        assertTrue(denyBounds.right <= allowBounds.left)
        assertTrue(text(code)!!.boundsInRoot.bottom < allowBounds.top)
        assertTrue(nodes().any { it.config.getOrNull(SemanticsProperties.Text)?.any { value -> Regex("\\d{2}:\\d{2}").matches(value.text) } == true })
    }

    private fun clickTarget(node: SemanticsNode): SemanticsNode =
        generateSequence(node) { it.parent }.first { it.config.getOrNull(SemanticsActions.OnClick) != null }

    private fun text(value: String): SemanticsNode? = nodes().firstOrNull {
        it.config.getOrNull(SemanticsProperties.Text)?.any { text -> text.text == value } == true
    }

    private fun nodes(): List<SemanticsNode> {
        return android.view.inspector.WindowInspector.getGlobalWindowViews().asReversed()
            .filter { it.isAttachedToWindow && it.isShown }.sortedByDescending { it.hasWindowFocus() }
            .flatMap { roots(it) }
            .flatMap { descendants(it.semanticsOwner.unmergedRootSemanticsNode) }
    }

    private fun descendants(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::descendants)
    private fun roots(view: View): List<ViewRootForTest> = when (view) {
        is ViewRootForTest -> listOf(view)
        is ViewGroup -> (0 until view.childCount).flatMap { roots(view.getChildAt(it)) }
        else -> emptyList()
    }
    private fun waitUntil(timeoutMs: Long, condition: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            var ready = false
            onMain { ready = condition() }
            if (ready) return
            Thread.sleep(100)
        }
        var visible = ""
        onMain { visible = nodes().mapNotNull { it.config.getOrNull(SemanticsProperties.Text)?.joinToString { value -> value.text } }.joinToString("\n") }
        val pairing = kotlinx.coroutines.runBlocking { RustPairingRuntime.states() }
        File(instrumentation.targetContext.cacheDir, "pairing-ui-failure.txt").writeText(visible + "\n" + pairing.joinToString("\n"))
        screenshot("failure")
        fail("Pairing request did not reach the expected visible state")
    }
    private fun screenshot(name: String) {
        instrumentation.waitForIdleSync()
        val drawn = java.util.concurrent.CountDownLatch(1)
        onMain {
            val view = activityRule.activity.window.decorView
            view.postOnAnimation { view.postOnAnimation { drawn.countDown() } }
        }
        assertTrue("Frame did not draw", drawn.await(5, java.util.concurrent.TimeUnit.SECONDS))
        Thread.sleep(500)
        instrumentation.uiAutomation.waitForIdle(200, 2_000)
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        val output = File(instrumentation.targetContext.cacheDir, "pairing-request-$name.png")
        output.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
    private fun onMain(block: () -> Unit) = instrumentation.runOnMainSync(block)
}
