package com.ismartcoding.plain.features.feed

import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.withIO
import kotlinx.serialization.Serializable

@Serializable
data class CatalogFeed(
    val name: String,
    val url: String,
    val site: String,
)

@Serializable
data class CatalogCategory(
    val id: String,
    val feeds: List<CatalogFeed>,
)

// Category ids used by the bundled catalog file; UI titles are string resources
// keyed by these ids so they translate.
object CatalogCategoryIds {
    const val TECH_CN = "tech_cn"
    const val DEV_CN = "dev_cn"
    const val TECH_EN = "tech_en"
    const val NEWS_EN = "news_en"
    const val READS = "reads"
}

// Parses the bundled catalog JSON. Duplicate urls across categories are
// dropped defensively so a bad catalog file can never double-subscribe.
fun parseCatalog(json: String): List<CatalogCategory> =
    JsonHelper.jsonDecode<List<CatalogCategory>>(json).map {
        it.copy(feeds = it.feeds.distinctBy { feed -> feed.url })
    }

object FeedsCatalog {
    private const val FILE = "files/feeds_catalog.json"

    suspend fun loadAsync(): List<CatalogCategory> =
        withIO { parseCatalog(Res.readBytes(FILE).decodeToString()) }
}
