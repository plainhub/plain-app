package com.ismartcoding.plain.enums

/** Call log direction, mirroring Android's CallLog.Calls.TYPE codes. */
enum class CallType(val androidValue: Int) {
    INCOMING(1),
    OUTGOING(2),
    MISSED(3),
    VOICEMAIL(4),
    REJECTED(5),
    BLOCKED(6),
    ANSWERED_EXTERNALLY(7),
    UNKNOWN(0),
    ;

    companion object {
        fun fromInt(value: Int): CallType = entries.find { it.androidValue == value } ?: UNKNOWN
    }
}
