package com.ismartcoding.plain.ui.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class MainNavItemsTest {
    @Test
    fun settings_is_the_last_root_tab() {
        assertEquals(listOf(1, 2, 3, 0), mainNavItems.map { it.index })
    }
}
