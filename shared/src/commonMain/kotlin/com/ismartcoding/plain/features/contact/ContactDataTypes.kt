package com.ismartcoding.plain.features.contact

data class DContentItem(var value: String, var type: Int, var label: String)

data class DOrganization(var company: String, var title: String)

data class DContactPhoneNumber(var value: String, var type: Int, var label: String, var normalizedNumber: String)
