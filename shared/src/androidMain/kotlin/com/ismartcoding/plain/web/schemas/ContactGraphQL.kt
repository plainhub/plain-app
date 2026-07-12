package com.ismartcoding.plain.web.schemas
import com.ismartcoding.plain.appContext

import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.lib.kgraphql.schema.execution.Executor
import com.ismartcoding.plain.lib.kgraphql.schema.execution.Execution
import com.ismartcoding.plain.lib.kgraphql.helpers.getFields
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.platform.checkEnabledAsync
import com.ismartcoding.plain.features.checkEnabledAsync
import com.ismartcoding.plain.platform.enabledAndIsGrantedAsync
import com.ismartcoding.plain.features.contact.GroupHelper
import com.ismartcoding.plain.features.contact.SourceHelper
import com.ismartcoding.plain.features.media.ContactMediaStoreHelper
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
            val context = appContext
            checkEnabledAsync(setOf(Permission.READ_CONTACTS))
            try {
                ContactMediaStoreHelper.searchAsync(context, query, limit, offset).map { it.toModel() }
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
            val context = appContext
            if (Permission.WRITE_CONTACTS.enabledAndIsGrantedAsync()) {
                ContactMediaStoreHelper.countAsync(context, query)
            } else {
                0
            }
        }
    }
    query("contactSources") {
        resolver { ->
            checkEnabledAsync(setOf(Permission.READ_CONTACTS))
            SourceHelper.getAll().map { it.toModel() }
        }
    }
    query("contactGroups") {
        resolver("node") { node: Execution.Node ->
            checkEnabledAsync(setOf(Permission.READ_CONTACTS))
            val groups = GroupHelper.getAll().map { it.toModel() }
            val fields = node.getFields()
            if (fields.contains(ContactGroup::contactCount.name)) {
                // TODO support contactsCount
            }
            groups
        }
    }
    mutation("deleteContacts") {
        resolver("query") { query: String ->
            val context = appContext
            Permission.WRITE_CONTACTS.checkEnabledAsync()
            val newIds = ContactMediaStoreHelper.getIdsAsync(context, query)
            TagHelper.deleteTagRelationByKeys(newIds, DataType.CONTACT)
            ContactMediaStoreHelper.deleteByIdsAsync(context, newIds)
            true
        }
    }
    mutation("updateContact") {
        resolver("id", "input") { id: ID, input: ContactInput ->
            Permission.WRITE_CONTACTS.checkEnabledAsync()
            ContactMediaStoreHelper.updateAsync(id.value, input)
            ContactMediaStoreHelper.getByIdAsync(appContext, id.value)?.toModel()
        }
    }
    mutation("createContact") {
        resolver("input") { input: ContactInput ->
            Permission.WRITE_CONTACTS.checkEnabledAsync()
            val id = ContactMediaStoreHelper.createAsync(input)
            if (id.isEmpty()) null else ContactMediaStoreHelper.getByIdAsync(appContext, id)?.toModel()
        }
    }
    mutation("createContactGroup") {
        resolver("name", "accountName", "accountType") { name: String, accountName: String, accountType: String ->
            Permission.WRITE_CONTACTS.checkEnabledAsync()
            GroupHelper.create(name, accountName, accountType).toModel()
        }
    }
    mutation("updateContactGroup") {
        resolver("id", "name") { id: ID, name: String ->
            Permission.WRITE_CONTACTS.checkEnabledAsync()
            GroupHelper.update(id.value, name)
            ContactGroup(id, name)
        }
    }
    mutation("deleteContactGroup") {
        resolver("id") { id: ID ->
            Permission.WRITE_CONTACTS.checkEnabledAsync()
            GroupHelper.delete(id.value)
            true
        }
    }
}
