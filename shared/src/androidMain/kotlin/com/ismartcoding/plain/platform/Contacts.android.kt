package com.ismartcoding.plain.platform

import com.ismartcoding.plain.data.DContactSource
import com.ismartcoding.plain.data.DGroup
import com.ismartcoding.plain.features.contact.GroupHelper
import com.ismartcoding.plain.features.contact.SourceHelper

actual fun getContactGroups(): List<DGroup> = GroupHelper.getAll()

actual fun createContactGroup(name: String, accountName: String, accountType: String): DGroup =
    GroupHelper.create(name, accountName, accountType)

actual fun updateContactGroup(id: String, name: String): DGroup? {
    GroupHelper.update(id, name)
    return DGroup(id.toLong(), name)
}

actual fun deleteContactGroup(id: String) = GroupHelper.delete(id)

actual fun getContactSources(): List<DContactSource> = SourceHelper.getAll()
