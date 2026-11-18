package com.ismartcoding.plain.features.images.editor

import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.models.ImageEditorProjectSummary
import com.ismartcoding.plain.models.ImageEditorProject
import com.ismartcoding.plain.rust.graphql.GraphqlClient

class ImageEditorProjectsClient(private val api: GraphqlClient) {
    suspend fun listAsync(): List<ImageEditorProjectSummary> = api.execute(Query.imageEditorProjects())

    suspend fun getByIdAsync(id: String): ImageEditorProject? =
        api.execute(Query.imageEditorProject(id))

    suspend fun addOrUpdateAsync(id: String, updateItem: ImageEditorProject.() -> Unit): ImageEditorProject {
        val project = getByIdAsync(id) ?: ImageEditorProject(id)
        project.updateItem()
        return api.execute(Mutation.saveImageEditorProject(id, project))
    }

    suspend fun deleteAsync(id: String) {
        api.execute(Mutation.deleteImageEditorProject(id))
    }
}
