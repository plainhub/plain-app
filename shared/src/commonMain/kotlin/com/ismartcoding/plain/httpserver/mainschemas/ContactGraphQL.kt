package com.ismartcoding.plain.httpserver.mainschemas

import com.ismartcoding.plain.lib.kgraphql.GraphQLError
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLMutation
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.lib.kgraphql.schema.execution.Execution
import com.ismartcoding.plain.lib.kgraphql.helpers.getFields
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.data.DContact
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.features.system.RustContactWriter
import com.ismartcoding.plain.platform.checkEnabledAsync
import com.ismartcoding.plain.features.checkEnabledAsync
import com.ismartcoding.plain.platform.enabledAndIsGrantedAsync
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.helpers.QueryHelper
import com.ismartcoding.plain.platform.countMedia
import com.ismartcoding.plain.platform.getContactById
import com.ismartcoding.plain.platform.getContactGroups
import com.ismartcoding.plain.platform.getContactSources
import com.ismartcoding.plain.platform.getMediaIds
import com.ismartcoding.plain.platform.searchMedia
import com.ismartcoding.plain.httpserver.loaders.TagsLoader
import com.ismartcoding.plain.httpserver.models.ActionResult
import com.ismartcoding.plain.httpserver.models.Contact
import com.ismartcoding.plain.httpserver.models.ContactGroup
import com.ismartcoding.plain.httpserver.models.ContactInput
import com.ismartcoding.plain.httpserver.models.ContactSource
import com.ismartcoding.plain.httpserver.models.ID
import com.ismartcoding.plain.httpserver.models.toModel

@GraphQLQuery
suspend fun contactCount(query: String): Int {
    return if (Permission.WRITE_CONTACTS.enabledAndIsGrantedAsync()) {
        countMedia(DataType.CONTACT, query)
    } else {
        0
    }
}

@GraphQLQuery
suspend fun contactSources(): List<ContactSource> {
    checkEnabledAsync(setOf(Permission.READ_CONTACTS))
    return getContactSources().map { it.toModel() }
}

@GraphQLQuery
suspend fun contactGroups(node: Execution.Node): List<ContactGroup> {
    checkEnabledAsync(setOf(Permission.READ_CONTACTS))
    val groups = getContactGroups().map { it.toModel() }
    val fields = node.getFields()
    if (fields.contains(ContactGroup::contactCount.name)) {
        // TODO support contactsCount
    }
    return groups
}

@GraphQLMutation
suspend fun deleteContacts(query: String): ActionResult {
    QueryHelper.requireExplicitBulkQuery(query)
    Permission.WRITE_CONTACTS.checkEnabledAsync()
    val newIds = getMediaIds(DataType.CONTACT, query)
    return ActionResult(com.ismartcoding.plain.features.system.RustSystemProviders.deleteRecords(DataType.CONTACT, newIds))
}

@GraphQLMutation
suspend fun updateContact(id: ID, input: ContactInput): Contact {
    RustContactWriter.update(id.value, input)
    return getContactById(id.value)?.toModel()
        ?: throw GraphQLError("Contact ${id.value} not found after update")
}

@GraphQLMutation
suspend fun createContact(input: ContactInput): Contact {
    val id = RustContactWriter.create(input)
    return getContactById(id)?.toModel() ?: throw GraphQLError("Contact $id not found after create")
}

@GraphQLMutation
suspend fun createContactGroup(name: String, accountName: String, accountType: String): ContactGroup {
    return ContactGroup(ID(RustContactWriter.createGroup(name, accountName, accountType)), name)
}

@GraphQLMutation
suspend fun updateContactGroup(id: ID, name: String): ContactGroup {
    RustContactWriter.updateGroup(id.value, name)
    return ContactGroup(id, name)
}

@GraphQLMutation
suspend fun deleteContactGroup(id: ID): Boolean {
    RustContactWriter.deleteGroup(id.value)
    return true
}

@GraphQLQuery
suspend fun contacts(offset: Int, limit: Int, query: String): List<Contact> {
    checkEnabledAsync(setOf(Permission.READ_CONTACTS))
    return try {
        searchMedia(DataType.CONTACT, query, limit, offset, FileSortBy.DATE_DESC)
            .filterIsInstance<DContact>()
            .map { it.toModel() }
    } catch (ex: Exception) {
        LogCat.e(ex)
        emptyList()
    }
}

fun SchemaBuilder.addContactSchema() {
    type<Contact> {
        dataProperty("tags") {
            prepare { item -> item.id.value }
            loader { ids ->
                TagsLoader.load(ids, DataType.CONTACT)
            }
        }
    }
}
