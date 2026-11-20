package com.ismartcoding.plain.tests

import android.accessibilityservice.AccessibilityService
import android.os.Environment
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.core.app.ActivityScenario
import com.ismartcoding.plain.MainActivity
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.features.web.AccessFeature
import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.grant_access
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.jetbrains.compose.resources.getString
import org.junit.Assert.assertTrue
import org.junit.Test

class WebFileAccessRequestTest {
    @Test
    fun approvingImagesEnablesTheFileAccessSwitchAfterSystemApproval() = runBlocking {
        val files = setOf(AccessFeature.IMAGES, AccessFeature.AUDIO, AccessFeature.VIDEOS, AccessFeature.DOCS, AccessFeature.FILES)
        org.junit.Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("fileAccessFixture") == "true")
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        check(!Environment.isExternalStorageManager())
        Prefs.completeOnboarding()
        ActivityScenario.launch(MainActivity::class.java).use {
            val previous = Prefs.apiAccessFeatures.value
            try {
                Prefs.setApiAccess(files, false)
                assertTrue(GraphqlApi.execute(Mutation.requestAccess(AccessFeature.IMAGES)))
                val label = getString(Res.string.grant_access)
                suspend fun tapWhenVisible(matches: (AccessibilityNodeInfo) -> Boolean) {
                    withTimeout(15000) {
                        while (true) {
                            val node = automation.rootInActiveWindow?.let { findNode(it, matches) }
                            if (node != null) {
                                val bounds = android.graphics.Rect()
                                node.getBoundsInScreen(bounds)
                                automation.executeShellCommand("input tap ${bounds.centerX()} ${bounds.centerY()}").close()
                                break
                            }
                            delay(100)
                        }
                    }
                }
                tapWhenVisible { it.text?.toString() == label }
                tapWhenVisible { it.packageName == "com.android.settings" && it.isCheckable && !it.isChecked }
                withTimeout(5000) {
                    while (!Environment.isExternalStorageManager()) delay(100)
                }
                assertTrue(automation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK))
                withTimeout(5000) {
                    while (!Prefs.apiAccessFeatures.value.containsAll(files)) delay(100)
                }
            } finally {
                Prefs.setApiAccessFeatures(previous)
            }
        }
    }

    private fun findNode(node: AccessibilityNodeInfo, matches: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        if (matches(node)) return node
        for (index in 0 until node.childCount) {
            node.getChild(index)?.let { findNode(it, matches) }?.let { return it }
        }
        return null
    }
}
