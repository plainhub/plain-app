package com.ismartcoding.plain.platform

import com.ismartcoding.plain.data.DContactSource
import com.ismartcoding.plain.data.DGroup

actual fun getContactGroups(): List<DGroup> = emptyList()

actual fun createContactGroup(name: String, accountName: String, accountType: String): DGroup =
    DGroup(0L, name)

actual fun updateContactGroup(id: String, name: String): DGroup? = null

actual fun deleteContactGroup(id: String) {}

actual fun getContactSources(): List<DContactSource> = emptyList()
