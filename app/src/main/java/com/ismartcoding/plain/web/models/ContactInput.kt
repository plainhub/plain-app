package com.ismartcoding.plain.web.models

data class ContentItemInput(var value: String, var type: Int, var label: String)

data class OrganizationInput(var company: String, var title: String)

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
