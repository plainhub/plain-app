package com.ismartcoding.plain.tests

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.PixelCopy
import android.view.View
import android.view.ViewGroup
import android.view.inspector.WindowInspector
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ismartcoding.plain.MainActivity
import com.ismartcoding.plain.ui.base.AlertType
import com.ismartcoding.plain.ui.base.PAlert
import com.ismartcoding.plain.ui.base.PAlertTextButton
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PTextButton
import com.ismartcoding.plain.ui.base.PBottomSheetTopAppBar
import com.ismartcoding.plain.ui.base.PSheetPrimaryActionsCard
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.fastscroll.LazyColumnScrollbar
import com.ismartcoding.plain.ui.base.fastscroll.LazyVerticalGridScrollbar
import com.ismartcoding.plain.ui.base.fastscroll.ScrollbarSettings
import com.ismartcoding.plain.ui.components.ButtonSize
import com.ismartcoding.plain.ui.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.features.chat.ChatStatus
import com.ismartcoding.plain.features.chat.data.ChatTarget
import com.ismartcoding.plain.features.chat.models.ChatAttachmentType
import com.ismartcoding.plain.features.chat.models.ChatMessage
import com.ismartcoding.plain.features.chat.models.ChatMessageAttachment
import com.ismartcoding.plain.features.chat.models.MessageFile
import com.ismartcoding.plain.features.chat.models.MessageType
import com.ismartcoding.plain.ui.audio.AudioQueueViewModel
import com.ismartcoding.plain.ui.chat.ChatViewModel
import com.ismartcoding.plain.ui.chat.VChat
import com.ismartcoding.plain.ui.chat.components.ChatListItem
import com.ismartcoding.plain.ui.media.viewer.previewer.rememberPreviewerState
import kotlin.time.Instant
import com.ismartcoding.plain.i18n.nearby_current_device
import com.ismartcoding.plain.i18n.nearby_switch_device
import com.ismartcoding.plain.i18n.offline
import com.ismartcoding.plain.i18n.event_reason_remote_access_disabled
import com.ismartcoding.plain.i18n.nearby_enable_remote_access
import com.ismartcoding.plain.features.nearby.PairingMode
import com.ismartcoding.plain.features.nearby.PeerStatus
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.cancel
import com.ismartcoding.plain.i18n.dark_theme
import com.ismartcoding.plain.i18n.donation_title
import com.ismartcoding.plain.i18n.images
import com.ismartcoding.plain.i18n.pair
import com.ismartcoding.plain.i18n.pair_another_device
import com.ismartcoding.plain.i18n.pairing_expired
import com.ismartcoding.plain.i18n.pairing_failed
import com.ismartcoding.plain.i18n.pairing_rejected
import com.ismartcoding.plain.i18n.stay_online_foreground_desc
import com.ismartcoding.plain.models.NearbyDevice
import com.ismartcoding.plain.models.PairingErrorCode
import com.ismartcoding.plain.models.PairingProgressNotice
import com.ismartcoding.plain.platform.system.AppFeatureType
import com.ismartcoding.plain.platform.system.DeviceType
import com.ismartcoding.plain.platform.system.has
import com.ismartcoding.plain.ui.chat.components.NearbyDeviceItem
import com.ismartcoding.plain.ui.home.PlainAppServiceSection
import com.ismartcoding.plain.ui.i18n.LocaleHelper
import com.ismartcoding.plain.ui.nearby.NearbyViewModel
import com.ismartcoding.plain.ui.nearby.PairPeerDialog
import com.ismartcoding.plain.ui.nearby.PairingProjection
import com.ismartcoding.plain.ui.resources.image
import com.ismartcoding.plain.ui.resources.timer
import com.ismartcoding.plain.ui.settings.SettingsPage
import com.ismartcoding.plain.ui.tools.FeatureItem
import com.ismartcoding.plain.ui.tools.FeatureItemsGrid
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import org.junit.Assume.assumeTrue
import com.ismartcoding.plain.ui.resources.Res as UiRes

@RunWith(AndroidJUnit4::class)
class SharedComponentSpacingTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun foregroundStayOnlineDescriptionHasTopInset() = render({
        PCard(Modifier.testTag("stay-online-card")) {
            PlainAppServiceSection(supportsBackgroundService = false, errorMessage = "", onRetry = null, onStayOnline = {})
        }
    }) {
        val label = LocaleHelper.getString(Res.string.stay_online_foreground_desc)
        await { nodes().any { it.config.getOrNull(SemanticsProperties.Text)?.any { text -> text.text == label } == true } }
        val card = tagged("stay-online-card").boundsInRoot
        val description = text(label).boundsInRoot
        assertEquals(16f, description.top - card.top, 0.5f)
    }

    @Test
    fun homeFeatureTitlesUseOneLineAndEllipsizeWhenNeeded() = render({
        FeatureItemsGrid(
            features = listOf(
                FeatureItem(AppFeatureType.IMAGES, Res.string.images, UiRes.drawable.image) {},
                FeatureItem(AppFeatureType.POMODORO_TIMER, Res.string.stay_online_foreground_desc, UiRes.drawable.timer) {},
            ),
            modifier = Modifier.width(216.dp),
        )
    }) {
        for ((resource, ellipsized) in listOf(Res.string.stay_online_foreground_desc to true, Res.string.images to false)) {
            val label = LocaleHelper.getString(resource)
            await { nodes().any { it.config.getOrNull(SemanticsProperties.Text)?.any { text -> text.text == label } == true } }
            val title = text(label)
            instrumentation.runOnMainSync {
                val layouts = mutableListOf<TextLayoutResult>()
                assertTrue(title.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts) == true)
                assertEquals(1, layouts.single().lineCount)
                assertEquals(ellipsized, layouts.single().isLineEllipsized(0))
            }
        }
    }

    @Test
    fun settingsKeepsOneGapWhenTheUpdateBannerIsHidden() {
        assumeTrue(AppFeatureType.CHECK_UPDATES.has())
        val updateAvailable = Prefs.appUpdateAvailable.value
        Prefs.appUpdateAvailable.value = false
        try {
            render({ SettingsPage(rememberNavController(), viewModel(), viewModel()) }) {
                val donationTitle = LocaleHelper.getString(Res.string.donation_title)
                val darkThemeTitle = LocaleHelper.getString(Res.string.dark_theme)
                await {
                    val labels = nodes().flatMap { it.config.getOrNull(SemanticsProperties.Text).orEmpty() }.map { it.text }
                    donationTitle in labels && darkThemeTitle in labels
                }
                fun boundsOfClickable(label: String) = generateSequence(text(label)) { it.parent }
                    .first { it.config.contains(SemanticsActions.OnClick) }.boundsInRoot

                val donation = boundsOfClickable(donationTitle)
                val darkTheme = boundsOfClickable(darkThemeTitle)
                assertEquals(16f, darkTheme.top - donation.bottom, 0.5f)
            }
        } finally {
            Prefs.appUpdateAvailable.value = updateAvailable
        }
    }

    @Test
    fun pairingAnotherDevicePreservesTheFirstRequestAndKeepsCodesOutOfRows() {
        val first = NearbyDevice("pair-first", "First phone", "", port = 0, deviceType = DeviceType.PHONE,
            version = "", platform = "", lastSeen = Instant.fromEpochMilliseconds(0), status = PeerStatus.PAIRED)
        val second = first.copy(id = "pair-second", name = "Second phone")
        val selected = mutableStateOf<NearbyDevice?>(first)
        val canceled = mutableListOf<String>()
        val code = "01234567"
        fun clickLabel(value: String) {
            val button = generateSequence(text(value)) { it.parent }.first { it.config.contains(androidx.compose.ui.semantics.SemanticsActions.OnClick) }
            instrumentation.runOnMainSync { button.config[androidx.compose.ui.semantics.SemanticsActions.OnClick].action!!.invoke() }
        }
        try {
            render({
                selected.value?.let { device ->
                    PairPeerDialog(device, PairingMode.MANAGE, start = { target, _ ->
                        PairingProjection.started(PairingProgressNotice(target.id, target.name, verificationCode = code))
                        true
                    }, cancel = { canceled.add(it) }, onDismiss = { selected.value = null })
                } ?: NearbyDeviceItem(first, enabled = true) {}
            }) {
                await { nodes().any { it.config.getOrNull(SemanticsProperties.TestTag) == "nearby-pair-code" } }
                text(code)
                clickLabel(LocaleHelper.getString(Res.string.pair_another_device))
                await { nodes().any { it.config.getOrNull(SemanticsProperties.TestTag) == "nearby-device:${first.id}" } }
                assertTrue(nodes().none { node -> node.config.getOrNull(SemanticsProperties.Text)?.any { it.text == code } == true })
                assertTrue(canceled.isEmpty())
                instrumentation.runOnMainSync { selected.value = second }
                await { nodes().any { it.config.getOrNull(SemanticsProperties.TestTag) == "nearby-pair-code" } }
                clickLabel(LocaleHelper.getString(Res.string.cancel))
                await { canceled.isNotEmpty() }
                assertEquals(listOf(second.id), canceled)
                assertTrue(NearbyViewModel.pairingViews.getValue(first.id).active)
            }
        } finally {
            instrumentation.runOnMainSync {
                for (device in listOf(first, second)) {
                    NearbyViewModel.pairingViews.remove(device.id)
                    NearbyViewModel.itemStatus.remove(device.id)
                }
            }
        }
    }

    @Test
    fun nearbyPairingErrorsAppearBelowTheAddressAndKeepThePairButton() {
        val device = NearbyDevice("pair-error", "Test phone", "", port = 8080, remoteAccess = true,
            deviceType = DeviceType.PHONE, version = "", platform = "", lastSeen = Instant.fromEpochMilliseconds(0),
            bestIp = "192.0.2.42", status = PeerStatus.PAIRED)
        val pairLabel = LocaleHelper.getString(Res.string.pair)
        try {
            render({ NearbyDeviceItem(device, enabled = true) {} }) {
                for ((code, resource) in listOf(
                    PairingErrorCode.INTERNAL_ERROR to Res.string.pairing_failed,
                    PairingErrorCode.REJECTED to Res.string.pairing_rejected,
                    PairingErrorCode.TIMEOUT to Res.string.pairing_expired,
                )) {
                    instrumentation.runOnMainSync {
                        PairingProjection.starting(device.id)
                        PairingProjection.failed(PairingProgressNotice(device.id, device.name, code))
                    }
                    val label = LocaleHelper.getString(resource)
                    await { nodes().any { it.config.getOrNull(SemanticsProperties.Text)?.any { text -> text.text == label } == true } }
                    val title = text(device.name).boundsInRoot
                    val address = text(device.bestIp).boundsInRoot
                    val error = text(label).boundsInRoot
                    assertTrue("Pairing error must be below the address", error.top >= address.bottom)
                    assertEquals(title.left, error.left, 0.5f)
                    assertTrue(text(pairLabel).boundsInRoot.left > title.left)
                }
            }
        } finally {
            instrumentation.runOnMainSync {
                NearbyViewModel.pairingViews.remove(device.id)
                NearbyViewModel.itemStatus.remove(device.id)
            }
        }
    }

    @Test
    fun nearbySwitchExplainsUnavailablePeersWithoutSelectingThem() {
        val main = com.ismartcoding.plain.ui.app.MainViewModel
        val toast = com.ismartcoding.plain.ui.components.ToastManager
        val instant = Instant.parse("2026-01-01T10:00:00Z")
        val peer = com.ismartcoding.plain.models.Peer("fixture", "Fixture", "", publicKey = "key", token = "paired",
            status = com.ismartcoding.plain.features.nearby.PeerStatus.PAIRED, port = null,
            deviceType = com.ismartcoding.plain.platform.system.DeviceType.PHONE, createdAt = instant, updatedAt = instant)
        val discovered = com.ismartcoding.plain.models.NearbyDevice(peer.id, peer.name, peer.publicKey, port = 0,
            remoteAccess = false, deviceType = peer.deviceType, version = "", platform = "", lastSeen = instant, status = peer.status)
        val access = mutableStateOf<com.ismartcoding.plain.models.NearbyDevice?>(null)
        val online = mutableStateOf(false)
        val switch = com.ismartcoding.plain.ui.i18n.LocaleHelper.getString(com.ismartcoding.plain.i18n.Res.string.nearby_switch_device)
        try {
            render({
                com.ismartcoding.plain.ui.chat.components.NearbyPairedDeviceItem(peer.copy(online = online.value), access.value) {}
            }) {
                val hint = com.ismartcoding.plain.ui.i18n.LocaleHelper.getString(com.ismartcoding.plain.i18n.Res.string.nearby_enable_remote_access)
                fun accessOffHintVisible() = nodes().any { node ->
                    node.config.getOrNull(SemanticsProperties.Text)?.any { it.text == hint } == true
                }
                fun clickSwitch() {
                    await { nodes().any { switch in it.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty() } }
                    instrumentation.waitForIdleSync()
                    val icon = nodes().single { switch in it.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty() }
                    val button = generateSequence(icon) { it.parent }.first { it.config.contains(androidx.compose.ui.semantics.SemanticsActions.OnClick) }
                    assertTrue("Switch must remain enabled", !button.config.contains(SemanticsProperties.Disabled))
                    instrumentation.runOnMainSync { button.config[androidx.compose.ui.semantics.SemanticsActions.OnClick].action!!.invoke() }
                }
                for ((device, reason) in listOf(null to com.ismartcoding.plain.i18n.Res.string.offline, discovered to com.ismartcoding.plain.i18n.Res.string.event_reason_remote_access_disabled)) {
                    instrumentation.runOnMainSync { access.value = device }
                    await { accessOffHintVisible() == (device != null) }
                    clickSwitch()
                    assertEquals(peer.name + ": " + com.ismartcoding.plain.ui.i18n.LocaleHelper.getString(reason), toast.current.value?.message)
                    assertEquals(null, main.selectedDevice.value)
                }
                instrumentation.runOnMainSync { online.value = true; access.value = null }
                await { !accessOffHintVisible() }
                clickSwitch()
                assertEquals(peer.id, main.selectedDevice.value?.peerId)
            }
        } finally {
            instrumentation.runOnMainSync { main.selectDevice(null); toast.current.value = null }
        }
    }

    @Test
    fun nearbyCanSwitchFromAManagedDeviceBackToThisDevice() {
        val main = com.ismartcoding.plain.ui.app.MainViewModel
        val switch = com.ismartcoding.plain.ui.i18n.LocaleHelper.getString(com.ismartcoding.plain.i18n.Res.string.nearby_switch_device)
        val current = com.ismartcoding.plain.ui.i18n.LocaleHelper.getString(com.ismartcoding.plain.i18n.Res.string.nearby_current_device)
        try {
            render({
                val peers = androidx.compose.runtime.remember {
                    com.ismartcoding.plain.ui.chat.PeerViewModel(readPeers = { emptyList() }, readLatestChats = { emptyMap() })
                }
                com.ismartcoding.plain.ui.nearby.NearbyPage(rememberNavController(), peers)
            }) {
                instrumentation.runOnMainSync { main.selectDevice(com.ismartcoding.plain.ui.app.DeviceIdentity("fixture", "fixture-key")) }
                fun switchNode() = nodes().singleOrNull { switch in it.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty() }
                await { switchNode() != null }
                val button = generateSequence(checkNotNull(switchNode())) { it.parent }
                    .first { it.config.contains(androidx.compose.ui.semantics.SemanticsActions.OnClick) }
                instrumentation.runOnMainSync { button.config[androidx.compose.ui.semantics.SemanticsActions.OnClick].action!!.invoke() }
                await { main.selectedDevice.value == null && switchNode() == null }
                assertTrue(nodes().any { current in it.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty() })
                assertTrue(nodes().none { node -> node.config.getOrNull(SemanticsProperties.Text)?.any { it.text == current } == true })
            }
        } finally {
            instrumentation.runOnMainSync { main.selectDevice(null) }
        }
    }

    @Test
    fun mixedChatMessageAddsOnlyTheGapBetweenAttachmentGroups() = render({
        val image = ChatMessageAttachment(ChatAttachmentType.IMAGE, MessageFile(uri = "fixture.png", fileName = "fixture.png", size = 10))
        val file = ChatMessageAttachment(ChatAttachmentType.FILE, MessageFile(uri = "fixture.pdf", fileName = "fixture.pdf", size = 10))
        val nav = rememberNavController()
        val focus = LocalFocusManager.current
        val previewer = rememberPreviewerState()
        val audio = androidx.compose.runtime.remember { AudioQueueViewModel() }
        for ((id, attachments) in mapOf("text" to emptyList(), "image" to listOf(image), "file" to listOf(file), "mixed" to listOf(image, file))) {
            val chat = VChat(id, "me", Instant.parse("2026-01-01T10:00:00Z"), MessageType.MESSAGE, ChatStatus.SENT,
                value = ChatMessage(text = "Attachment fixture", items = attachments))
            Box(Modifier.testTag("chat-$id")) {
                ChatListItem(nav, ChatViewModel.local, ChatTarget.parseId("peer:local"), audio, listOf(chat), chat, null, 0, 48.dp, 48, focus, previewer)
            }
        }
    }) {
        await { nodes().any { it.config.getOrNull(SemanticsProperties.TestTag) == "chat-mixed" } }
        fun height(id: String): Float = tagged("chat-$id").boundsInRoot.height
        assertTrue("The file attachment must be rendered", nodes().any { node ->
            node.config.getOrNull(SemanticsProperties.Text)?.any { it.text == "fixture.pdf" } == true
        })
        assertEquals(8f, height("mixed") - height("image") - height("file") + height("text"), 0.5f)
    }

    @Test
    fun alertTextActionUsesA32DpTouchTarget() = render({
        PAlert("Network permission is needed to find nearby devices.", AlertType.WARNING, Modifier.testTag("with-button")) {
            PAlertTextButton("Fix now", {}, Modifier.testTag("fix-action"))
        }
        PAlert("No alert action", AlertType.INFO, Modifier.testTag("without-button"))
        PAlert("No alert action", AlertType.INFO, Modifier.testTag("empty-button"), actions = {}, content = {})
        PAlert("More details", AlertType.ERROR, Modifier.testTag("with-detail"), content = {
            Text("Description", Modifier.testTag("detail-content"))
        })
        PTextButton("Outside alert", {}, Modifier.testTag("regular-action"), buttonSize = ButtonSize.SMALL)
    }) {
        await { nodes().any { it.config.getOrNull(SemanticsProperties.TestTag) == "fix-action" } }
        val action = tagged("fix-action").touchBoundsInRoot
        val alert = tagged("with-button").boundsInRoot
        val message = text("Network permission is needed to find nearby devices.").boundsInRoot
        assertEquals(8f, message.top - alert.top, 1f)
        assertEquals(0f, action.top - message.bottom, 0.5f)
        assertEquals(32f, action.height, 0.5f)
        assertEquals(32f, tagged("fix-action").boundsInRoot.height, 0.5f)
        assertEquals(48f, tagged("regular-action").touchBoundsInRoot.height, 0.5f)
        assertEquals(4f, alert.bottom - action.bottom, 0.5f)
        assertEquals(8f, alert.right - action.right, 0.5f)
        assertEquals(8f, alert.right - message.right, 0.5f)
        assertEquals(8f, action.right - text("Fix now").boundsInRoot.right, 0.5f)
        assertEquals(16f, alert.right - text("Fix now").boundsInRoot.right, 0.5f)
        val plain = tagged("without-button").boundsInRoot
        assertEquals(plain.height, tagged("empty-button").boundsInRoot.height, 0.5f)
        assertEquals(8f, tagged("with-detail").boundsInRoot.bottom - tagged("detail-content").boundsInRoot.bottom, 0.5f)
    }

    @Test
    fun alertKeepsActionsAtTheLogicalEndInRtl() = render({
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            PAlert("Network permission is needed to find nearby devices.", AlertType.ERROR, Modifier.testTag("rtl-alert")) {
                PAlertTextButton("OK", {}, Modifier.testTag("rtl-action"))
            }
        }
    }) {
        await { nodes().any { it.config.getOrNull(SemanticsProperties.TestTag) == "rtl-action" } }
        val alert = tagged("rtl-alert").boundsInRoot
        val action = tagged("rtl-action").touchBoundsInRoot
        assertEquals(8f, action.left - alert.left, 0.5f)
        assertEquals(8f, text("OK").boundsInRoot.left - action.left, 0.5f)
        assertEquals(16f, text("OK").boundsInRoot.left - alert.left, 0.5f)
        assertEquals(4f, alert.bottom - action.bottom, 0.5f)
    }

    @Test
    fun alertTextActionClipsOnlyThePressFeedbackToACapsule() = render({
        PAlert("Permission is needed", AlertType.WARNING, Modifier.padding(top = 96.dp)) {
            PAlertTextButton("Fix now", {}, Modifier.testTag("ripple-action"))
        }
    }) { activity ->
        await { nodes().any { it.config.getOrNull(SemanticsProperties.TestTag) == "ripple-action" } }
        val button = tagged("ripple-action")
        val touch = button.positionOnScreen
        val x = button.positionInWindow.x.toInt() + 2
        val y = button.positionInWindow.y.toInt()
        val sample = Rect(x, y + 2, x + 1, y + 17)
        val handler = Handler(Looper.getMainLooper())
        fun feedbackPixels(): Pair<Int, Int> {
            val screenshot = Bitmap.createBitmap(sample.width(), sample.height(), Bitmap.Config.ARGB_8888)
            return try {
                val result = CompletableFuture<Int>()
                PixelCopy.request(activity.window, sample, screenshot, { result.complete(it) }, handler)
                assertEquals(PixelCopy.SUCCESS, result.get(3, TimeUnit.SECONDS).toInt())
                screenshot.getPixel(0, 14) to screenshot.getPixel(0, 0)
            } finally {
                screenshot.recycle()
            }
        }

        val idle = feedbackPixels()
        val down = SystemClock.uptimeMillis()
        fun sendTouch(action: Int) {
            val event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, touch.x + 12, touch.y + 4, 0)
            try {
                event.source = InputDevice.SOURCE_TOUCHSCREEN
                instrumentation.sendPointerSync(event)
            } finally {
                event.recycle()
            }
        }

        try {
            sendTouch(MotionEvent.ACTION_DOWN)
            await { feedbackPixels().first != idle.first }
            assertEquals("Button corner should stay transparent when pressed", idle.second, feedbackPixels().second)
        } finally {
            sendTouch(MotionEvent.ACTION_CANCEL)
        }
    }

    @Test
    fun sheetCardAppliesCallerPaddingAndSemanticsOnce() = render({
        PSheetPrimaryActionsCard(Modifier.testTag("card").padding(horizontal = 8.dp)) {
            Box(Modifier.size(24.dp).testTag("action"))
        }
    }) {
        await { nodes().any { it.config.getOrNull(SemanticsProperties.TestTag) == "action" } }
        assertEquals(1, nodes().count { it.config.getOrNull(SemanticsProperties.TestTag) == "card" })
        val frame = tagged("frame").boundsInRoot
        assertEquals(24f, tagged("action").boundsInRoot.left - frame.left, 0.5f)
    }

    @Test
    fun sheetToolbarReservesActionGapOnlyForVisibleContent() = render({
        PBottomSheetTopAppBar(title = "No actions")
        PBottomSheetTopAppBar(title = "Empty actions", actions = {})
        PBottomSheetTopAppBar(title = "With action", actions = {
            Box(Modifier.size(24.dp).testTag("action"))
        })
    }) {
        await { nodes().any { it.config.getOrNull(SemanticsProperties.TestTag) == "action" } }
        val right = tagged("frame").boundsInRoot.right - 24f
        assertEquals(right, text("No actions").boundsInRoot.right, 0.5f)
        assertEquals(right, text("Empty actions").boundsInRoot.right, 0.5f)
        assertEquals(16f, tagged("action").boundsInRoot.left - text("With action").boundsInRoot.right, 0.5f)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun toolbarMatchesMaterialInsetsWhenActionsAreMissingOrHidden() {
        val control = "Control " + "title ".repeat(60)
        val absent = "Absent " + "title ".repeat(60)
        val conditional = "Conditional " + "title ".repeat(60)
        val visible = mutableStateOf(true)
        render({
            TopAppBar(title = { Text(control, maxLines = 1) })
            PTopAppBar(title = absent)
            PTopAppBar(title = conditional, actions = {
                if (visible.value) Box(Modifier.size(24.dp).testTag("action"))
            })
        }) {
            await { nodes().any { it.config.getOrNull(SemanticsProperties.TestTag) == "action" } }
            val right = text(control).boundsInRoot.right
            assertEquals(right, text(absent).boundsInRoot.right, 0.5f)
            assertTrue(text(conditional).boundsInRoot.right < right)
            instrumentation.runOnMainSync { visible.value = false }
            await { nodes().none { it.config.getOrNull(SemanticsProperties.TestTag) == "action" } }
            assertEquals(right, text(conditional).boundsInRoot.right, 0.5f)
        }
    }

    @Test
    fun scrollbarsApplyCallerModifierOnceWhetherEnabledOrDisabled() = render({
        for (enabled in listOf(true, false)) {
            val list = rememberLazyListState()
            LazyColumnScrollbar(
                state = list,
                modifier = Modifier.testTag("list-$enabled").size(160.dp).padding(8.dp),
                settings = ScrollbarSettings(enabled = enabled, alwaysShowScrollbar = true),
            ) {
                LazyColumn(state = list, modifier = Modifier.fillMaxSize().testTag("list-content-$enabled")) {
                    items(20) { Text("List item $it") }
                }
            }
            val grid = rememberLazyGridState()
            LazyVerticalGridScrollbar(
                state = grid,
                modifier = Modifier.testTag("grid-$enabled").size(160.dp).padding(8.dp),
                settings = ScrollbarSettings(enabled = enabled, alwaysShowScrollbar = true),
            ) {
                LazyVerticalGrid(columns = GridCells.Fixed(2), state = grid, modifier = Modifier.fillMaxSize().testTag("grid-content-$enabled")) {
                    items(20) { Text("Grid item $it") }
                }
            }
        }
    }) {
        await { nodes().any { it.config.getOrNull(SemanticsProperties.TestTag) == "grid-content-false" } }
        for (kind in listOf("list", "grid")) {
            for (enabled in listOf(true, false)) {
                val tag = "$kind-$enabled"
                assertEquals(tag, 1, nodes().count { it.config.getOrNull(SemanticsProperties.TestTag) == tag })
                val outer = tagged(tag).boundsInRoot
                val content = tagged("$kind-content-$enabled").boundsInRoot
                assertEquals(160f, outer.width, 0.5f)
                assertEquals(8f, content.left - outer.left, 0.5f)
                assertEquals(144f, content.width, 0.5f)
            }
        }
    }

    private fun render(content: @Composable () -> Unit, verify: (MainActivity) -> Unit) {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var host: MainActivity
            scenario.onActivity { activity ->
                host = activity
                activity.setContent {
                    CompositionLocalProvider(LocalDensity provides Density(1f)) {
                        AppTheme(useDarkTheme = false) {
                            Column(Modifier.width(320.dp).testTag("frame")) { content() }
                        }
                    }
                }
            }
            verify(host)
        }
    }

    private fun tagged(tag: String): SemanticsNode = nodes().single { it.config.getOrNull(SemanticsProperties.TestTag) == tag }

    private fun text(value: String): SemanticsNode = nodes().single {
        it.config.getOrNull(SemanticsProperties.Text)?.any { label -> label.text == value } == true
    }

    private fun nodes(): List<SemanticsNode> {
        fun roots(view: View): List<ViewRootForTest> = when (view) {
            is ViewRootForTest -> listOf(view)
            is ViewGroup -> (0 until view.childCount).flatMap { roots(view.getChildAt(it)) }
            else -> emptyList()
        }
        fun flatten(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::flatten)
        var result = emptyList<SemanticsNode>()
        instrumentation.runOnMainSync {
            result = WindowInspector.getGlobalWindowViews().filter { it.isShown }.flatMap(::roots)
                .flatMap { flatten(it.semanticsOwner.unmergedRootSemanticsNode) }
        }
        return result
    }

    private fun await(condition: () -> Boolean) {
        val deadline = System.nanoTime() + 10_000_000_000L
        while (System.nanoTime() < deadline) {
            if (condition()) return
            Thread.sleep(25)
        }
        error("Component layout did not become ready")
    }
}
