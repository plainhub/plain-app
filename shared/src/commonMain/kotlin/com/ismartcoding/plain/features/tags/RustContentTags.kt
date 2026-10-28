package com.ismartcoding.plain.features.tags

import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.models.Tag
import com.ismartcoding.plain.models.TagRelation
import com.ismartcoding.plain.platform.system.DataType
import com.ismartcoding.plain.rust.graphql.GraphqlClient
import com.ismartcoding.plain.rust.graphql.GraphqlApi

internal object RustContentTags : TagsApi()
internal open class TagsApi(
    private val api: GraphqlClient = GraphqlApi,
) {

    suspend fun all(type: DataType): List<Tag> =
        api.execute(Query.tags(type))

    suspend fun create(type: DataType, name: String): Tag =
        api.execute(Mutation.createTag(type, name))

    suspend fun rename(id: String, name: String): Tag =
        api.execute(Mutation.updateTag(id, name))

    suspend fun delete(id: String) {
        api.execute(Mutation.deleteTag(id))
    }

    suspend fun keys(id: String): List<String> =
        api.execute(Query.tagKeys(id))

    suspend fun relations(keys: Set<String>, type: DataType): List<TagRelation> =
        api.execute(Query.tagRelations(type, keys))

    suspend fun edit(type: DataType, keys: Collection<String>, add: Collection<String>, remove: Collection<String>) {
        if (keys.isEmpty() || (add.isEmpty() && remove.isEmpty())) return
        api.execute(Mutation.updateTagRelations(type, keys, add, remove))
    }
}
