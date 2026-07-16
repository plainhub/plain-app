package com.ismartcoding.plain.web.schemas

import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.lib.kgraphql.schema.execution.Executor
import com.ismartcoding.plain.lib.kgraphql.schema.execution.Execution
import com.ismartcoding.plain.lib.kgraphql.helpers.getFields
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.data.DContact
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.platform.checkEnabledAsync
import com.ismartcoding.plain.features.checkEnabledAsync
import com.ismartcoding.plain.platform.enabledAndIsGrantedAsync
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.platform.countMedia
import com.ismartcoding.plain.platform.createContact
import com.ismartcoding.plain.platform.createContactGroup
import com.ismartcoding.plain.platform.deleteContactGroup
import com.ismartcoding.plain.platform.deleteContacts
import com.ismartcoding.plain.platform.getContactById
import com.ismartcoding.plain.platform.getContactGroups
import com.ismartcoding.plain.platform.getContactSources
import com.ismartcoding.plain.platform.getMediaIds
import com.ismartcoding.plain.platform.searchMedia
import com.ismartcoding.plain.platform.updateContact
import com.ismartcoding.plain.platform.updateContactGroup
import com.ismartcoding.plain.web.loaders.TagsLoader
import com.ismartcoding.plain.web.models.Contact
import com.ismartcoding.plain.web.models.ContactGroup
import com.ismartcoding.plain.web.models.ContactInput
import com.ismartcoding.plain.web.models.ID
import com.ismartcoding.plain.web.models.toModel

fun SchemaBuilder.addContactSchema() {
    query("contacts") {
        configure {
            executor = Executor.DataLoaderPrepared
        }
        resolver("offset", "limit", "query") { offset: Int, limit: Int, query: String ->
            checkEnabledAsync(setOf(Permission.READ_CONTACTS))
            try {
                searchMedia(DataType.CONTACT, query, limit, offset, FileSortBy.DATE_DESC)
                    .filterIsInstance<DContact>()
                    .map { it.toModel() }
            } catch (ex: Exception) {
                LogCat.e(ex)
                emptyList()
            }
        }
        type<Contact> {
            dataProperty("tags") {
                prepare { item -> item.id.value }
                loader { ids ->
                    TagsLoader.load(ids, DataType.CONTACT)
                }
            }
        }
    }
    query("contactCount") {
        resolver("query") { query: String ->
            if (Permission.WRITE_CONTACTS.enabledAndIsGrantedAsync()) {
                countMedia(DataType.CONTACT, query)
            } else {
                0
            }
        }
    }
    query("contactSources") {
        resolver { ->
            checkEnabledAsync(setOf(Permission.READ_CONTACTS))
            getContactSources().map { it.toModel() }
        }
    }
    query("contactGroups") {
        resolver("node") { node: Execution.Node ->
            checkEnabledAsync(setOf(Permission.READ_CONTACTS))
            val groups = getContactGroups().map { it.toModel() }
            val fields = node.getFields()
            if (fields.contains(ContactGroup::contactCount.name)) {
                // TODO support contactsCount
            }
            groups
        }
    }
    mutation("deleteContacts") {
        resolver("query") { query: String ->
            Permission.WRITE_CONTACTS.checkEnabledAsync()
            val newIds = getMediaIds(DataType.CONTACT, query)
            TagHelper.deleteTagRelationByKeys(newIds, DataType.CONTACT)
            deleteContacts(newIds)
            true
        }
    }
    mutation("updateContact") {
        resolver("id", "input") { id: ID, input: ContactInput ->
            Permission.WRITE_CONTACTS.checkEnabledAsync()
            updateContact(id.value, input)
            getContactById(id.value)?.toModel()
        }
    }
    mutation("createContact") {
        resolver("input") { input: ContactInput ->
            Permission.WRITE_CONTACTS.checkEnabledAsync()
            val id = createContact(input)
            if (id.isEmpty()) null else getContactById(id)?.toModel()
        }
    }
    mutation("createContactGroup") {
        resolver("name", "accountName", "accountType") { name: String, accountName: String, accountType: String ->
            Permission.WRITE_CONTACTS.checkEnabledAsync()
            createContactGroup(name, accountName, accountType).toModel()
        }
    }
    mutation("updateContactGroup") {
        resolver("id", "name") { id: ID, name: String ->
            Permission.WRITE_CONTACTS.checkEnabledAsync()
            updateContactGroup(id.value, name)
            ContactGroup(id, name)
        }
    }
    mutation("deleteContactGroup") {
        resolver("id") { id: ID ->
            Permission.WRITE_CONTACTS.checkEnabledAsync()
            deleteContactGroup(id.value)
            true
        }
    }
}
