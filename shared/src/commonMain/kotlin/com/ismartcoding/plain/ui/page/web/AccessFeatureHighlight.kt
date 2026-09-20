package com.ismartcoding.plain.ui.page.web

import com.ismartcoding.plain.enums.WebSettingsFeature
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Handoff for the openWebSettings(feature) mutation: producers (channel event
 * handler / intent handler) set the pending feature before navigating to the
 * page; the page consumes it to scroll to the option and show a bubble.
 * Works even when the page is already open, since it is route-independent.
 */
object AccessFeatureHighlight {
    val pending = MutableStateFlow<WebSettingsFeature?>(null)
}
