package com.ismartcoding.plain.tests

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
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ismartcoding.plain.MainActivity
import com.ismartcoding.plain.ui.base.PBottomSheetTopAppBar
import com.ismartcoding.plain.ui.base.PSheetPrimaryActionsCard
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.fastscroll.LazyColumnScrollbar
import com.ismartcoding.plain.ui.base.fastscroll.LazyVerticalGridScrollbar
import com.ismartcoding.plain.ui.base.fastscroll.ScrollbarSettings
import com.ismartcoding.plain.ui.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedComponentSpacingTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

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

    private fun render(content: @Composable () -> Unit, verify: () -> Unit) {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    CompositionLocalProvider(LocalDensity provides Density(1f)) {
                        AppTheme(useDarkTheme = false) {
                            Column(Modifier.width(320.dp).testTag("frame")) { content() }
                        }
                    }
                }
            }
            verify()
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
