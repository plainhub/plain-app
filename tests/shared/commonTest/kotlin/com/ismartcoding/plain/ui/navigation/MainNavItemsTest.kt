package com.ismartcoding.plain.ui.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class MainNavItemsTest {
    @Test
    fun tools_are_always_available_and_settings_require_local_authorization() {
        assertEquals(listOf(1, 2, 3, 0), mainNavItems(localOperationsAllowed = true).map { it.index })
        assertEquals(listOf(1, 2, 3), mainNavItems(localOperationsAllowed = false).map { it.index })
    }
}
