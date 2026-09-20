package com.ismartcoding.plain.enums

/** SMS box/direction, mirroring Android's Telephony.Sms.MESSAGE_TYPE_* codes. */
enum class SmsType(val androidValue: Int) {
    INBOX(1),
    SENT(2),
    DRAFT(3),
    OUTBOX(4),
    FAILED(5),
    QUEUED(6),
    UNKNOWN(0),
    ;

    companion object {
        fun fromInt(value: Int): SmsType = entries.find { it.androidValue == value } ?: UNKNOWN
    }
}
