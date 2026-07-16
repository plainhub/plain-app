package com.ismartcoding.plain.platform

import com.ismartcoding.plain.data.DContactSource
import com.ismartcoding.plain.data.DGroup

/**
 * Returns all contact groups. Empty list on platforms without a contacts provider.
 */
expect fun getContactGroups(): List<DGroup>

/**
 * Create a new contact group with [name] under the account identified by
 * [accountName]/[accountType]. Returns the newly created group.
 */
expect fun createContactGroup(name: String, accountName: String, accountType: String): DGroup

/**
 * Update the contact group identified by [id], changing its name to [name].
 * Returns the updated group, or null on failure / unsupported platforms.
 */
expect fun updateContactGroup(id: String, name: String): DGroup?

/**
 * Delete the contact group identified by [id]. No-op on unsupported platforms.
 */
expect fun deleteContactGroup(id: String)

/**
 * Returns all contact sources (accounts). Empty list on unsupported platforms.
 */
expect fun getContactSources(): List<DContactSource>
