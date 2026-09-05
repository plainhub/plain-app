package com.ismartcoding.plain.features.feed

// Discrete article text scale options for FeedEntryPage; the selected index
// is persisted in FeedFontScalePreference. Values multiply the density
// fontScale so only sp-based text sizes change.
object FeedFontScale {
    val values = listOf(0.85f, 1.0f, 1.25f, 1.5f, 2.0f)

    fun value(index: Int): Float = values.getOrElse(index) { 1.0f }
}
