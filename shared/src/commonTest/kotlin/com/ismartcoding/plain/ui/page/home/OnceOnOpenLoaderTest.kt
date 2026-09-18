package com.ismartcoding.plain.ui.page.home

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnceOnOpenLoaderTest {

    @Test
    fun closedDrawerNeverFires() {
        val loader = OnceOnOpenLoader()
        assertFalse(loader.shouldLoad(isOpen = false))
        assertFalse(loader.shouldLoad(isOpen = false))
    }

    @Test
    fun firstOpenFiresExactlyOnce() {
        val loader = OnceOnOpenLoader()
        assertTrue(loader.shouldLoad(isOpen = true))
        assertFalse(loader.shouldLoad(isOpen = true))
    }

    @Test
    fun reopenAfterCloseDoesNotRefire() {
        val loader = OnceOnOpenLoader()
        assertFalse(loader.shouldLoad(isOpen = false))
        assertTrue(loader.shouldLoad(isOpen = true))
        assertFalse(loader.shouldLoad(isOpen = false))
        assertFalse(loader.shouldLoad(isOpen = true))
    }

    @Test
    fun closedObservationsBeforeFirstOpenDoNotConsumeTheSingleFire() {
        val loader = OnceOnOpenLoader()
        loader.shouldLoad(isOpen = false)
        loader.shouldLoad(isOpen = false)
        assertTrue(loader.shouldLoad(isOpen = true))
    }
}
