package com.ismartcoding.plain.tests

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.MainActivity
import com.ismartcoding.plain.ui.Identifiable
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.base.dragselect.gridDragSelect
import com.ismartcoding.plain.ui.base.dragselect.rememberDragSelectState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class GridDragSelectTest {
    @Test
    fun onlyIndexesItemsInSelectionMode() {
        val idReads = AtomicInteger()
        val items = List(128) { index ->
            object : Identifiable {
                override val id: String
                    get() {
                        idReads.incrementAndGet()
                        return index.toString()
                    }
            }
        }
        val browsing = CountDownLatch(1)
        val selecting = CountDownLatch(1)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var selection: DragSelectState
            scenario.onActivity { activity ->
                activity.setContent {
                    selection = rememberDragSelectState()
                    val selectMode = selection.selectMode
                    Box(Modifier.fillMaxSize().gridDragSelect(items, selection))
                    SideEffect { (if (selectMode) selecting else browsing).countDown() }
                }
            }
            assertTrue(browsing.await(5, TimeUnit.SECONDS))
            assertEquals(0, idReads.get())
            scenario.onActivity { selection.enterSelectMode() }
            assertTrue(selecting.await(5, TimeUnit.SECONDS))
            assertEquals(items.size, idReads.get())
        }
    }
}
