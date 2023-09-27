package com.ismartcoding.plain.features.contact

import com.ismartcoding.lib.extensions.containsChinese
import com.ismartcoding.plain.data.IData
import kotlinx.datetime.Instant

data class DContactSource(var name: String, var type: String)

data class DGroup(
    var id: Long,
    var name: String,
)

data class DContact(
    override var id: String,
    var prefix: String,
    var givenName: String,
    var middleName: String,
    var familyName: String,
    var suffix: String,
    var nickname: String,
    var photoUri: String,
    var phoneNumbers: List<DPhoneNumber>,
    var emails: List<DContentItem>,
    var addresses: List<DContentItem>,
    var events: List<DContentItem>,
    var source: String,
    var starred: Int,
    var contactId: String,
    var thumbnailUri: String,
    var notes: String,
    var groups: List<DGroup>,
    var organization: DOrganization?,
    var websites: List<DContentItem>,
    var ims: List<DContentItem>,
    var ringtone: String,
    var updatedAt: Instant,
) : IData {
    fun fullName(): String {
        val name =
            if (givenName.containsChinese() || familyName.containsChinese()) {
                "$familyName$middleName$givenName"
            } else {
                setOf(givenName, middleName, familyName).filter { it.isNotEmpty() }.joinToString(" ")
            }

        val suffixComma = if (suffix.isEmpty()) "" else ", $suffix"
        val fullName = "$prefix $name $suffixComma".trim()
        return fullName.ifEmpty {
            emails.firstOrNull()?.value?.trim() ?: ""
        }
    }
}
