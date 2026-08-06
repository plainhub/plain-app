package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLInput
import kotlinx.serialization.Serializable

@GraphQLInput
@Serializable
data class ContentItemInput(var value: String, var type: Int, var label: String)
@GraphQLInput
@Serializable
data class OrganizationInput(var company: String, var title: String)

@GraphQLInput
@Serializable
data class ContactInput(
    var prefix: String,
    var firstName: String,
    var middleName: String,
    var lastName: String,
    var suffix: String,
    var nickname: String,
    var phoneNumbers: List<ContentItemInput>,
    var emails: List<ContentItemInput>,
    var addresses: List<ContentItemInput>,
    var events: List<ContentItemInput>,
    var source: String,
    var starred: Boolean,
    var notes: String,
    var groupIds: List<ID>,
    var organization: OrganizationInput?,
    var websites: List<ContentItemInput>,
    var ims: List<ContentItemInput>,
)
