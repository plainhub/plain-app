package com.ismartcoding.plain.platform.permissions

import com.ismartcoding.plain.lib.JsonHelper
import kotlin.test.Test
import kotlin.test.assertEquals

class PermissionSerializationTest {
    @Test
    fun permissionSnapshotUsesTheExistingEnumNames() {
        val permissions = listOf(Permission.READ_CONTACTS, Permission.RECORD_AUDIO)
        val encoded = JsonHelper.jsonEncode(permissions)
        assertEquals("[\"READ_CONTACTS\",\"RECORD_AUDIO\"]", encoded)
        assertEquals(permissions, JsonHelper.jsonDecode<List<Permission>>(encoded))
    }

    @Test
    fun deniedPermissionSnapshotCanBeEmpty() {
        assertEquals("[]", JsonHelper.jsonEncode(emptyList<Permission>()))
    }
}
