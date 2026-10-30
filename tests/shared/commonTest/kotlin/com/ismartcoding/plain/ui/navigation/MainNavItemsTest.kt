package com.ismartcoding.plain.ui.navigation

import com.ismartcoding.plain.platform.system.Capability
import kotlin.test.Test
import kotlin.test.assertEquals

class MainNavItemsTest {
    @Test
    fun services_is_last_and_only_available_with_local_authorization() {
        assertEquals(listOf(1, 2, 3, 0), mainNavItems(listOf(Capability.NOTES), localOperationsAllowed = true).map { it.index })
        assertEquals(listOf(1, 2, 3), mainNavItems(listOf(Capability.NOTES), localOperationsAllowed = false).map { it.index })
    }

    @Test
    fun tools_uses_the_selected_devices_capabilities() {
        assertEquals(listOf(1, 3, 0), mainNavItems(emptyList(), localOperationsAllowed = true).map { it.index })
        assertEquals(listOf(1, 3), mainNavItems(emptyList(), localOperationsAllowed = false).map { it.index })
    }
}
