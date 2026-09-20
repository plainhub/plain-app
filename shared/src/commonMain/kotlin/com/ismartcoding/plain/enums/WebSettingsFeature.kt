package com.ismartcoding.plain.enums

/**
 * Highlightable options on the Desktop Access Settings page. The desktop app
 * passes one via the openWebSettings mutation; the phone then opens the page,
 * scrolls to the option and shows a bubble pointing at it.
 */
enum class WebSettingsFeature {
    FILES,
    CONTACTS,
    SMS,
    CALL_LOGS,
    CALL_PHONE,
    PHONE_NUMBER,
    APPS,
    NOTIFICATIONS,
    CLIPBOARD,
}
