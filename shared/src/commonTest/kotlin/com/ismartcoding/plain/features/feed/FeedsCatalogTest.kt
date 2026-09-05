package com.ismartcoding.plain.features.feed

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FeedsCatalogTest {
    private val sampleJson =
        """
        [
          {"id":"tech_cn","feeds":[
            {"name":"少数派","url":"https://sspai.com/feed","site":"sspai.com"},
            {"name":"爱范儿","url":"https://www.ifanr.com/feed","site":"ifanr.com"}
          ]},
          {"id":"news_en","feeds":[
            {"name":"BBC News","url":"https://feeds.bbci.co.uk/news/rss.xml","site":"bbc.com"},
            {"name":"BBC Dup","url":"https://feeds.bbci.co.uk/news/rss.xml","site":"bbc.com"}
          ]}
        ]
        """.trimIndent()

    @Test
    fun parses_categories_and_feeds() {
        val categories = parseCatalog(sampleJson)
        assertEquals(2, categories.size)
        assertEquals("tech_cn", categories[0].id)
        assertEquals(2, categories[0].feeds.size)
        assertEquals("少数派", categories[0].feeds[0].name)
        assertEquals("sspai.com", categories[0].feeds[0].site)
    }

    @Test
    fun drops_duplicate_urls_across_rows() {
        val categories = parseCatalog(sampleJson)
        assertEquals(1, categories[1].feeds.size)
        assertEquals("BBC News", categories[1].feeds[0].name)
    }

    @Test
    fun all_category_ids_are_known() {
        val known =
            setOf(
                CatalogCategoryIds.TECH_CN,
                CatalogCategoryIds.DEV_CN,
                CatalogCategoryIds.TECH_EN,
                CatalogCategoryIds.NEWS_EN,
                CatalogCategoryIds.READS,
            )
        parseCatalog(sampleJson).forEach {
            assertTrue(it.id in known, "unknown category id: " + it.id)
        }
    }
}
