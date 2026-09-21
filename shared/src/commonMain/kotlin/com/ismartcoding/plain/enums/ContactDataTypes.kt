package com.ismartcoding.plain.enums

/**
 * Contact detail kinds, one enum per ContactsContract CommonDataKinds data row.
 * Each member carries its Android DATA2 code so the GraphQL layer can map
 * both directions without leaking raw ints into the API contract. Unknown
 * codes fall back to CUSTOM (the label/customProtocol string then carries
 * the user-visible value, matching Android's own convention).
 */

enum class PhoneType(val androidValue: Int) {
    CUSTOM(0),
    HOME(1),
    MOBILE(2),
    WORK(3),
    FAX_WORK(4),
    FAX_HOME(5),
    PAGER(6),
    OTHER(7),
    CALLBACK(8),
    CAR(9),
    COMPANY_MAIN(10),
    ISDN(11),
    MAIN(12),
    OTHER_FAX(13),
    RADIO(14),
    TELEX(15),
    TTY_TDD(16),
    WORK_MOBILE(17),
    WORK_PAGER(18),
    ASSISTANT(19),
    ;

    companion object {
        fun fromInt(value: Int): PhoneType = entries.find { it.androidValue == value } ?: CUSTOM
    }
}

enum class EmailType(val androidValue: Int) {
    CUSTOM(0),
    HOME(1),
    WORK(2),
    OTHER(3),
    MOBILE(4),
    ;

    companion object {
        fun fromInt(value: Int): EmailType = entries.find { it.androidValue == value } ?: CUSTOM
    }
}

enum class PostalType(val androidValue: Int) {
    CUSTOM(0),
    HOME(1),
    WORK(2),
    OTHER(3),
    ;

    companion object {
        fun fromInt(value: Int): PostalType = entries.find { it.androidValue == value } ?: CUSTOM
    }
}

enum class EventType(val androidValue: Int) {
    CUSTOM(0),
    ANNIVERSARY(1),
    BIRTHDAY(2),
    OTHER(3),
    ;

    companion object {
        fun fromInt(value: Int): EventType = entries.find { it.androidValue == value } ?: CUSTOM
    }
}

enum class WebsiteType(val androidValue: Int) {
    CUSTOM(0),
    HOMEPAGE(1),
    BLOG(2),
    FTP(3),
    HOME(4),
    WORK(5),
    OTHER(6),
    ;

    companion object {
        fun fromInt(value: Int): WebsiteType = entries.find { it.androidValue == value } ?: CUSTOM
    }
}

/** Instant-message network, mirroring ContactsContract.CommonDataKinds.Im.PROTOCOL codes. */
enum class ImProtocol(val androidValue: Int) {
    CUSTOM(0),
    AIM(1),
    MSN(2),
    YAHOO(3),
    SKYPE(4),
    QQ(5),
    GOOGLE_TALK(6),
    ICQ(7),
    JABBER(8),
    NETMEETING(9),
    ;

    companion object {
        fun fromInt(value: Int): ImProtocol = entries.find { it.androidValue == value } ?: CUSTOM
    }
}
