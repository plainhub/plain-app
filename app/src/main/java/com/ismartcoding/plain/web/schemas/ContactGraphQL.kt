package com.ismartcoding.plain.web.schemas

import com.ismartcoding.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.lib.kgraphql.schema.execution.Executor
import com.ismartcoding.lib.kgraphql.schema.execution.Execution
import com.ismartcoding.lib.kgraphql.helpers.getFields
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.Permissions
import com.ismartcoding.plain.features.TagHelper
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
        resolver { offset: Int, limit: Int, query: String ->
            val context = MainApp.instance
            Permissions.checkAsync(context, setOf(Permission.READ_CONTACTS))
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
        resolver { query: String ->
            val context = MainApp.instance
            if (Permission.WRITE_CONTACTS.enabledAndCanAsync(context)) {
                ContactMediaStoreHelper.countAsync(context, query)
            } else {
                0
            }
        }
    }
    query("contactSources") {
        resolver { ->
            Permissions.checkAsync(MainApp.instance, setOf(Permission.READ_CONTACTS))
            SourceHelper.getAll().map { it.toModel() }
        }
    }
    query("contactGroups") {
        resolver { node: Execution.Node ->
            Permissions.checkAsync(MainApp.instance, setOf(Permission.READ_CONTACTS))
            val groups = GroupHelper.getAll().map { it.toModel() }
            val fields = node.getFields()
            if (fields.contains(ContactGroup::contactCount.name)) {
                // TODO support contactsCount
            }
            groups
        }
    }
    mutation("deleteContacts") {
        resolver { query: String ->
            val context = MainApp.instance
            Permission.WRITE_CONTACTS.checkAsync(context)
            val newIds = ContactMediaStoreHelper.getIdsAsync(context, query)
            TagHelper.deleteTagRelationByKeys(newIds, DataType.CONTACT)
            ContactMediaStoreHelper.deleteByIdsAsync(context, newIds)
            true
        }
    }
    mutation("updateContact") {
        resolver { id: ID, input: ContactInput ->
            Permission.WRITE_CONTACTS.checkAsync(MainApp.instance)
            ContactMediaStoreHelper.updateAsync(id.value, input)
            ContactMediaStoreHelper.getByIdAsync(MainApp.instance, id.value)?.toModel()
        }
    }
    mutation("createContact") {
        resolver { input: ContactInput ->
            Permission.WRITE_CONTACTS.checkAsync(MainApp.instance)
            val id = ContactMediaStoreHelper.createAsync(input)
            if (id.isEmpty()) null else ContactMediaStoreHelper.getByIdAsync(MainApp.instance, id)?.toModel()
        }
    }
    mutation("createContactGroup") {
        resolver { name: String, accountName: String, accountType: String ->
            Permission.WRITE_CONTACTS.checkAsync(MainApp.instance)
            GroupHelper.create(name, accountName, accountType).toModel()
        }
    }
    mutation("updateContactGroup") {
        resolver { id: ID, name: String ->
            Permission.WRITE_CONTACTS.checkAsync(MainApp.instance)
            GroupHelper.update(id.value, name)
            ContactGroup(id, name)
        }
    }
    mutation("deleteContactGroup") {
        resolver { id: ID ->
            Permission.WRITE_CONTACTS.checkAsync(MainApp.instance)
            GroupHelper.delete(id.value)
            true
        }
    }
}
