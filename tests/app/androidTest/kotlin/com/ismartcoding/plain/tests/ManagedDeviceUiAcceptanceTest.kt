package com.ismartcoding.plain.tests

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
import com.ismartcoding.plain.models.Peer
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import com.ismartcoding.plain.rust.graphql.GraphqlRequest
import com.ismartcoding.plain.ui.i18n.LocaleHelper
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@Serializable
private data class ManagedAcceptanceIdentity(val clientId: String, val deviceName: String)

@Serializable
private data class ManagedAcceptancePeer(val id: String, val manageable: Boolean)

@Serializable
private data class ManagedAcceptanceDescriptor(val id: String, val peers: List<ManagedAcceptancePeer>)

@RunWith(AndroidJUnit4::class)
class ManagedDeviceUiAcceptanceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val identityRequest = GraphqlRequest<ManagedAcceptanceIdentity>("query { app { clientId deviceName } }", field = "app")

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
    fun managesRemoteNoteThroughTheChatEntryWithoutWritingLocalContent() = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use {
            val targetId = checkNotNull(InstrumentationRegistry.getArguments().getString("managedPeerId"))
            val peer = checkNotNull(GraphqlApi.execute(Query.peers()).find { it.id == targetId })
            check(peer.isPaired() && peer.token.isNotEmpty())
            val remote = GraphqlApi.forPeer(peer)
            val target = remote.execute(identityRequest)
            assertEquals(targetId, target.clientId)
            val marker = "managed-ui-${UUID.randomUUID()}"
            val note = remote.execute(Mutation.saveNote("", marker, "Initial $marker"))
            try {
                assertNull(GraphqlApi.execute(Query.note(note.id)))
                click(text(Res.string.chat), "chat navigation", bottom = true)
                click(peer.name, "selected peer")
                click(text(Res.string.remote_management), "remote management entry")
                awaitNode("target tools page") { it.text?.toString() == target.deviceName }
                val tools = allText()
                for (excluded in listOf(Res.string.sound_meter, Res.string.pomodoro_timer, Res.string.image_editor)) {
                    assertFalse("Local tool exposed for remote management", tools.contains(text(excluded)))
                }
                click(text(Res.string.notes), "remote notes")
                awaitNode("note fixture") { it.text?.toString()?.contains(marker) == true }
                click(marker, "remote note fixture")
                val field = awaitNode("remote note editor") { it.isEditable && it.text?.toString()?.contains("Initial $marker") == true }
                val edited = "Edited $marker"
                val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, edited) }
                assertTrue(field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args))
                click(text(Res.string.save), "save remote note")
                awaitNode("updated remote preview") { it.text?.toString()?.contains(edited) == true }
                assertEquals(edited, remote.execute(Query.note(note.id))?.content)
                assertNull(GraphqlApi.execute(Query.note(note.id)))
                println("MANAGED_NOTE_RESULT=remote_update_verified_local_absent")
            } finally {
                val remoteNote = remote.execute(Query.note(note.id))
                if (remoteNote?.title == marker) remote.execute(Mutation.deleteNotes("ids:${note.id}"))
                val localNote = GraphqlApi.execute(Query.note(note.id))
                if (localNote?.title == marker) GraphqlApi.execute(Mutation.deleteNotes("ids:${note.id}"))
                assertNull(remote.execute(Query.note(note.id)))
            }
        }
    }

    private fun text(resource: org.jetbrains.compose.resources.StringResource) = LocaleHelper.getString(resource)

    private fun nodes(root: AccessibilityNodeInfo): List<AccessibilityNodeInfo> = buildList {
        add(root)
        for (i in 0 until root.childCount) root.getChild(i)?.let { addAll(nodes(it)) }
    }

    private fun awaitNode(role: String, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo {
        val deadline = System.nanoTime() + 30_000_000_000L
        while (System.nanoTime() < deadline) {
            val root = instrumentation.uiAutomation.rootInActiveWindow
            if (root?.packageName?.toString() == instrumentation.targetContext.packageName) {
                nodes(root).firstOrNull(predicate)?.let { return it }
            }
            Thread.sleep(100)
        }
        throw AssertionError("Timed out waiting for $role")
    }

    private fun allText(): List<String> = nodes(checkNotNull(instrumentation.uiAutomation.rootInActiveWindow)).mapNotNull { it.text?.toString() }

    private fun click(value: String, role: String, bottom: Boolean = false) {
        awaitNode(role) { it.text?.toString()?.contains(value) == true || it.contentDescription?.toString() == value }
        val candidates = nodes(checkNotNull(instrumentation.uiAutomation.rootInActiveWindow)).filter {
            it.text?.toString()?.contains(value) == true || it.contentDescription?.toString() == value
        }
        var node = if (bottom) candidates.last() else candidates.first()
        while (!node.isClickable && node.parent != null) node = checkNotNull(node.parent)
        assertTrue("Could not click $role", node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
    }
}
