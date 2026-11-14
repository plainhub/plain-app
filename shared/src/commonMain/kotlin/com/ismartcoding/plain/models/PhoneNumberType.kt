package com.ismartcoding.plain.models

import kotlinx.serialization.Serializable

@Serializable
enum class PhoneNumberType {
    FIXED_LINE,
    MOBILE,
    FIXED_LINE_OR_MOBILE,
    TOLL_FREE,
    PREMIUM_RATE,
    SHARED_COST,
    PERSONAL_NUMBER,
    VOIP,
    PAGER,
    UAN,
    VOICEMAIL,
}
